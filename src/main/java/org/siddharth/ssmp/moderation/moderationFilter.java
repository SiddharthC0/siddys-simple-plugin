package org.siddharth.ssmp.moderation;

import java.util.*;

final class moderationFilter {
    record Match(String term, String category, int start, int end) {}
    record Result(String message, Set<String> categories, boolean matched) {}

    private final Map<String, String> terms;

    moderationFilter(Map<String, String> terms) {
        this.terms = new LinkedHashMap<>(terms);
    }

    Result apply(String input, String maskCharacter, int maskLength) {
        if (input == null || input.isEmpty() || terms.isEmpty()) return new Result(input, Set.of(), false);

        StringBuilder normalized = new StringBuilder();
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();

        for (int i = 0; i < input.length();) {
            int cp = input.codePointAt(i);
            int width = Character.charCount(cp);
            if (Character.isLetterOrDigit(cp)) {
                String lower = new String(Character.toChars(cp)).toLowerCase(Locale.ROOT);
                for (int j = 0; j < lower.length(); j++) {
                    normalized.append(lower.charAt(j));
                    starts.add(i);
                    ends.add(i + width);
                }
            }
            i += width;
        }

        String text = normalized.toString();
        if (text.isEmpty()) return new Result(input, Set.of(), false);

        boolean[] masked = new boolean[input.length()];
        Set<String> categories = new LinkedHashSet<>();
        boolean matched = false;

        for (Map.Entry<String, String> entry : terms.entrySet()) {
            String term = normalize(entry.getKey());
            if (term.isEmpty()) continue;

            int from = 0;
            while ((from = text.indexOf(term, from)) >= 0) {
                int to = from + term.length();
                int originalStart = starts.get(from);
                int originalEnd = ends.get(to - 1);
                boolean leftBoundary = originalStart == 0 || !Character.isLetterOrDigit(input.codePointBefore(originalStart));
                boolean rightBoundary = originalEnd >= input.length() || !Character.isLetterOrDigit(input.codePointAt(originalEnd));
                if (leftBoundary && rightBoundary) {
                    matched = true;
                    categories.add(entry.getValue().toUpperCase(Locale.ROOT));
                    for (int i = originalStart; i < originalEnd; i++) masked[i] = true;
                }
                from = Math.max(to, from + 1);
            }
        }

        if (!matched) return new Result(input, Set.of(), false);

        String replacement = maskCharacter == null || maskCharacter.isEmpty() ? "*" : maskCharacter.substring(0, 1);
        int count = Math.max(1, Math.min(16, maskLength));
        String mask = replacement.repeat(count);
        StringBuilder output = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            if (masked[i]) {
                output.append(mask);
                while (i + 1 < input.length() && masked[i + 1]) i++;
            } else {
                output.append(input.charAt(i));
            }
        }
        return new Result(output.toString(), Collections.unmodifiableSet(categories), true);
    }

    private static String normalize(String value) {
        StringBuilder result = new StringBuilder();
        value.toLowerCase(Locale.ROOT).codePoints().filter(Character::isLetterOrDigit).forEach(result::appendCodePoint);
        return result.toString();
    }
}
