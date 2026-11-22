package org.siddharth.ssmp.moderation;
import java.util.*;
public final class ModerationFilter {
    record Match(String term, String category, int start, int end) {}
    record Result(String message, Set<String> categories, boolean matched) {}
    private static final class Node {
        final Map<Integer, Node> next = new HashMap<>();
        Node failure;
        final List<Entry> outputs = new ArrayList<>();
    }

    private record Entry(String term, String category, int length) {}
    private final Node root = new Node();
    private int termCount;
    public ModerationFilter(Map<String, String> terms) {
        if (terms == null) return;
        for (Map.Entry<String, String> entry : terms.entrySet()) {
            String normalized = normalize(entry.getKey());
            if (normalized.isEmpty()) continue;
            String category = entry.getValue() == null ? "UNKNOWN" : entry.getValue().toUpperCase(Locale.ROOT);
            Node current = root;
            for (int cp : normalized.codePoints().toArray()) {
                current = current.next.computeIfAbsent(cp, k -> new Node());
            }

            current.outputs.add(new Entry(entry.getKey(), category, normalized.codePointCount(0, normalized.length())));
            termCount++;
        }

        buildFailureLinks();
    }

    private void buildFailureLinks() {
        Queue<Node> queue = new ArrayDeque<>();
        root.failure = root;
        for (Node child : root.next.values()) {
            child.failure = root;
            queue.add(child);
        }

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            for (Map.Entry<Integer, Node> edge : current.next.entrySet()) {
                int cp = edge.getKey();
                Node child = edge.getValue();
                Node fallback = current.failure;
                while (fallback != root && !fallback.next.containsKey(cp)) {
                    fallback = fallback.failure;
                }

                if (fallback.next.containsKey(cp) && fallback.next.get(cp) != child) {
                    child.failure = fallback.next.get(cp);
                } else {
                    child.failure = root;
                }

                child.outputs.addAll(child.failure.outputs);
                queue.add(child);
            }
        }
    }

    Result apply(String input, String maskCharacter, int maskLength) {
        if (input == null || input.isEmpty() || termCount == 0) {
            return new Result(input, Set.of(), false);
        }

        StringBuilder normalized = new StringBuilder();
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        for (int i = 0; i < input.length();) {
            int cp = input.codePointAt(i);
            int width = Character.charCount(cp);
            if (Character.isLetterOrDigit(cp)) {
                int lower = Character.toLowerCase(cp);
                normalized.appendCodePoint(lower);
                for (int j = 0; j < Character.charCount(lower); j++) {
                    starts.add(i);
                    ends.add(i + width);
                }
            }

            i += width;
        }

        if (normalized.isEmpty()) {
            return new Result(input, Set.of(), false);
        }

        boolean[] masked = new boolean[input.length()];
        Set<String> categories = new LinkedHashSet<>();
        Node current = root;
        boolean matched = false;
        int[] chars = normalized.toString().codePoints().toArray();
        for (int i = 0; i < chars.length; i++) {
            int cp = chars[i];
            while (current != root && !current.next.containsKey(cp)) {
                current = current.failure;
            }

            current = current.next.getOrDefault(cp, root);
            for (Entry entry : current.outputs) {
                int startIndex = i - entry.length() + 1;

                if (startIndex < 0) continue;

                int originalStart = starts.get(startIndex);
                int originalEnd = ends.get(i);

                for (int j = originalStart; j < originalEnd; j++) {
                    masked[j] = true;
                }

                categories.add(entry.category());
                matched = true;
            }
        }

        if (!matched) {
            return new Result(input, Set.of(), false);
        }

        String replacement = maskCharacter == null || maskCharacter.isEmpty() ? "*" : new String(Character.toChars(maskCharacter.codePointAt(0)));
        int count = Math.max(1, Math.min(16, maskLength));
        String mask = replacement.repeat(count);
        StringBuilder output = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            if (masked[i]) {
                output.append(mask);

                while (i + 1 < input.length() && masked[i + 1]) {
                    i++;
                }
            } else {
                output.append(input.charAt(i));
            }
        }

        return new Result(
                output.toString(),
                Collections.unmodifiableSet(categories),
                true
        );
    }

    private static String normalize(String value) {
        if (value == null || value.isEmpty()) return "";

        StringBuilder result = new StringBuilder();

        value.codePoints()
                .filter(Character::isLetterOrDigit)
                .map(Character::toLowerCase)
                .forEach(result::appendCodePoint);

        return result.toString();
    }
}
