
window.SSMPApi = {
    async request(path, data = null, method = "GET") {
        const options = {
            method,
            credentials: "same-origin",
            headers: {}
        };

        if (data !== null) {
            options.headers["Content-Type"] = "application/x-www-form-urlencoded;charset=UTF-8";
            options.body = new URLSearchParams(data).toString();
        }

        const response = await fetch(path, options);
        const result = await response.json().catch(() => null);

        if (!response.ok) {
            throw new Error(result?.error || `Request failed (${response.status})`);
        }

        if (result === null) {
            throw new Error("Invalid API response");
        }

        return result;
    },

    login(password) {
        return this.request("/api/login", { password }, "POST");
    },

    logout() {
        return this.request("/api/logout", {}, "POST");
    },

    files() {
        return this.request("/api/files");
    },

    file(path) {
        return this.request("/api/file?path=" + encodeURIComponent(path));
    },

    save(path, key, value, type) {
        return this.request("/api/save", { path, key, value, type }, "POST");
    },

    add(path, key, value, type) {
        return this.request("/api/add", { path, key, value, type }, "POST");
    },

    changePassword(current, next) {
        return this.request("/api/password", { current, next }, "POST");
    },

    server() {
        return this.request("/api/server");
    },

    monitor() {
        return this.request("/api/monitor");
    },

    resources() {
        return this.request("/api/resources");
    },

    checkUpdates() {
        return this.request("/api/update-check", {}, "POST");
    },

    consoleCommand(command) {
        return this.request("/api/console/command", { command }, "POST");
    },

    consoleStatus() {
        return this.request("/api/console/status");
    },

    consoleStream() {
        return new EventSource("/api/console/stream", {
            withCredentials: true
        });
    },
    consolePoll(since) {
        return this.request("/api/console/poll?since=" + since);
    },
};
