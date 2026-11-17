
/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */

package org.siddharth.ssmp.cpanel;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ssmpWebHandler implements HttpHandler {
    private final JavaPlugin plugin;
    private final ssmpWebAPI api;
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    private static final long LOCK_DURATION = 300000;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_REQUEST_SIZE = 16384;

    private static final class LoginAttempt {
        private int failures;
        private long lockedUntil;
    }

    ssmpWebHandler(JavaPlugin plugin, ssmpWebAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            securityHeaders(exchange.getResponseHeaders());

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (path.equals("/api/login")) {
                handleLogin(exchange, method);
                return;
            }

            if (path.equals("/api/logout")) {
                handleLogout(exchange, method);
                return;
            }

            if (path.equals("/api/console/stream")) {
                if (!method.equals("GET")) {
                    send(exchange, 405, "{\"error\":\"GET required.\"}");
                    return;
                }

                if (!api.validSession(cookie(exchange, "SSMPSESSION"))) {
                    send(exchange, 401, "{\"error\":\"Authentication required.\"}");
                    return;
                }

                api.streamConsole(exchange);
                return;
            }

            if (path.startsWith("/api/")) {
                handleAPI(exchange, method, path);
                return;
            }

            if (path.equals("/") || path.equals("/index.html")) {
                if (!method.equals("GET")) {
                    send(exchange, 405, "{\"error\":\"GET required.\"}");
                    return;
                }

                if (api.validSession(cookie(exchange, "SSMPSESSION"))) {
                    serveResource(exchange, "/web/index.html", "text/html; charset=utf-8");
                } else {
                    serveResource(exchange, "/web/login.html", "text/html; charset=utf-8");
                }

                return;
            }

            if (path.startsWith("/assets/")
                    || path.startsWith("/css/")
                    || path.startsWith("/js/")) {

                if (!method.equals("GET")) {
                    send(exchange, 405, "{\"error\":\"GET required.\"}");
                    return;
                }

                if (path.contains("..") || path.contains("\\")) {
                    send(exchange, 400, "{\"error\":\"Invalid resource path.\"}");
                    return;
                }

                String resource = "/web" + path;
                String type = contentType(path);

                serveResource(exchange, resource, type);
                return;
            }

            send(exchange, 404, "{\"error\":\"Not found.\"}");

        } catch (SecurityException exception) {
            sendError(exchange, 403, exception.getMessage());

        } catch (IllegalArgumentException exception) {
            sendError(exchange, 400, exception.getMessage());

        } catch (Exception exception) {
            plugin.getLogger().warning(
                    "[SSMP Control Panel] Request failed: " + exception.getMessage()
            );

            sendError(exchange, 500, "The request could not be completed.");

        } finally {
            exchange.close();
        }
    }

    private void handleLogin(HttpExchange exchange, String method) throws IOException {
        if (!method.equals("POST")) {
            send(exchange, 405, "{\"error\":\"POST required.\"}");
            return;
        }

        if (!sameOrigin(exchange)) {
            send(exchange, 403, "{\"error\":\"Origin check failed.\"}");
            return;
        }

        String address = exchange.getRemoteAddress().getAddress() == null
                ? "unknown"
                : exchange.getRemoteAddress().getAddress().getHostAddress();

        LoginAttempt attempt = loginAttempts.computeIfAbsent(
                address,
                ignored -> new LoginAttempt()
        );

        synchronized (attempt) {
            long now = System.currentTimeMillis();

            if (attempt.lockedUntil > now) {
                send(exchange, 429, "{\"error\":\"Too many attempts. Try again later.\"}");
                return;
            }

            if (attempt.lockedUntil != 0) {
                attempt.lockedUntil = 0;
                attempt.failures = 0;
            }
        }

        Map<String, String> form = readForm(exchange);

        if (!api.login(form.getOrDefault("password", ""))) {
            synchronized (attempt) {
                attempt.failures++;

                if (attempt.failures >= MAX_LOGIN_ATTEMPTS) {
                    attempt.lockedUntil = System.currentTimeMillis() + LOCK_DURATION;
                    attempt.failures = 0;
                }
            }

            send(exchange, 401, "{\"error\":\"Invalid password.\"}");
            return;
        }

        loginAttempts.remove(address);

        String token = api.newSession();

        exchange.getResponseHeaders().add(
                "Set-Cookie",
                "SSMPSESSION=" + token + "; HttpOnly; SameSite=Strict; Path=/"
        );

        send(exchange, 200, "{\"authenticated\":true}");
    }

    private void handleLogout(HttpExchange exchange, String method) throws IOException {
        if (!method.equals("POST")) {
            send(exchange, 405, "{\"error\":\"POST required.\"}");
            return;
        }

        if (!sameOrigin(exchange)) {
            send(exchange, 403, "{\"error\":\"Origin check failed.\"}");
            return;
        }

        api.logout(cookie(exchange, "SSMPSESSION"));

        exchange.getResponseHeaders().add(
                "Set-Cookie",
                "SSMPSESSION=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0"
        );

        send(exchange, 200, "{\"loggedOut\":true}");
    }

    private void handleAPI(
            HttpExchange exchange,
            String method,
            String path
    ) throws Exception {
        if (!api.validSession(cookie(exchange, "SSMPSESSION"))) {
            send(exchange, 401, "{\"error\":\"Authentication required.\"}");
            return;
        }

        if (!method.equals("GET") && !method.equals("POST")) {
            send(exchange, 405, "{\"error\":\"Unsupported HTTP method.\"}");
            return;
        }

        if (method.equals("POST") && !sameOrigin(exchange)) {
            send(exchange, 403, "{\"error\":\"Origin check failed.\"}");
            return;
        }

        Map<String, String> params = method.equals("GET")
                ? ssmpWebAPI.decodeForm(exchange.getRequestURI().getRawQuery())
                : readForm(exchange);

        String response = api.handle(method, path, params);
        send(exchange, 200, response);
    }

    private String contentType(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);

        if (lower.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }

        if (lower.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }

        if (lower.endsWith(".png")) {
            return "image/png";
        }

        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }

        if (lower.endsWith(".ico")) {
            return "image/x-icon";
        }

        if (lower.endsWith(".woff")) {
            return "font/woff";
        }

        if (lower.endsWith(".woff2")) {
            return "font/woff2";
        }

        return "application/octet-stream";
    }

    private void serveResource(
            HttpExchange exchange,
            String resource,
            String contentType
    ) throws IOException {
        try (InputStream input = plugin.getResource(resource.substring(1))) {
            if (input == null) {
                send(exchange, 404, "{\"error\":\"Missing bundled resource.\"}");
                return;
            }

            byte[] bytes = input.readAllBytes();

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");

            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private Map<String, String> readForm(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_REQUEST_SIZE + 1);

        if (bytes.length > MAX_REQUEST_SIZE) {
            throw new IllegalArgumentException("Request too large.");
        }

        return ssmpWebAPI.decodeForm(
                new String(bytes, StandardCharsets.UTF_8)
        );
    }

    private String cookie(HttpExchange exchange, String name) {
        String header = exchange.getRequestHeaders().getFirst("Cookie");

        if (header == null) {
            return null;
        }

        for (String part : header.split(";")) {
            String[] pair = part.trim().split("=", 2);

            if (pair.length == 2 && pair[0].equals(name)) {
                return pair[1];
            }
        }

        return null;
    }

    private boolean sameOrigin(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin == null) return false;

        String host = exchange.getRequestHeaders().getFirst("Host");
        if (host == null || host.isBlank()) return false;

        try {
            URI uri = URI.create(origin);

            if (uri.getScheme() == null
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getPath() != null && !uri.getPath().isEmpty()
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {
                return false;
            }

            String originHost = uri.getRawAuthority();

            return originHost != null
                    && originHost.equalsIgnoreCase(host)
                    && (uri.getScheme().equalsIgnoreCase("http")
                    || uri.getScheme().equalsIgnoreCase("https"));

        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void securityHeaders(Headers headers) {
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        headers.set(
                "Content-Security-Policy",
                "default-src 'self'; " +
                        "style-src 'self'; " +
                        "script-src 'self'; " +
                        "img-src 'self' data:; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'none'; " +
                        "base-uri 'none'; " +
                        "form-action 'self'"
        );

        headers.set("Cache-Control", "no-store");
    }

    private void sendError(
            HttpExchange exchange,
            int status,
            String message
    ) throws IOException {
        if (exchange.getResponseCode() != -1) {
            return;
        }

        String safeMessage = ssmpWebAPI.escape(
                message == null ? "Unknown error." : message
        );

        send(
                exchange,
                status,
                "{\"error\":\"" + safeMessage + "\"}"
        );
    }

    private void send(
            HttpExchange exchange,
            int status,
            String body
    ) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=utf-8"
        );

        exchange.getResponseHeaders().set("Cache-Control", "no-store");

        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
