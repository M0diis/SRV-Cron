package me.m0dii.srvcron.utils;

import java.util.Map;

/** Converts legacy color codes in plain text to MiniMessage tags. */
public final class Kyorifier {
    private static final Map<Character, String> COLORS = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white")
    );

    private static final Map<Character, String> DECORATIONS = Map.of(
            'k', "obfuscated",
            'l', "bold",
            'm', "strikethrough",
            'n', "underlined",
            'o', "italic"
    );

    private Kyorifier() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Translates legacy {@code &} and {@code §} codes while leaving MiniMessage tags,
     * including quoted tag arguments, intact.
     */
    public static String kyorify(String input) {
        StringBuilder result = new StringBuilder(input.length() + 16);
        boolean inTag = false;
        char quote = 0;

        for (int i = 0; i < input.length();) {
            char current = input.charAt(i);

            if (inTag) {
                result.append(current);
                if (quote != 0) {
                    if (current == '\\' && i + 1 < input.length()) {
                        result.append(input.charAt(i + 1));
                        i += 2;
                        continue;
                    }
                    if (current == quote) {
                        quote = 0;
                    }
                } else if (current == '\'' || current == '"') {
                    quote = current;
                } else if (current == '>') {
                    inTag = false;
                }
                i++;
                continue;
            }

            if (current == '\\' && i + 1 < input.length() && input.charAt(i + 1) == '<') {
                result.append("\\<");
                i += 2;
                continue;
            }

            if (current == '<') {
                result.append(current);
                inTag = true;
                i++;
                continue;
            }

            if (current == '&' || current == '\u00a7') {
                int rgbEnd = appendRgbCode(input, i, result);
                if (rgbEnd > i) {
                    i = rgbEnd;
                    continue;
                }

                if (i + 1 < input.length()) {
                    char code = Character.toLowerCase(input.charAt(i + 1));
                    String color = COLORS.get(code);
                    if (color != null) {
                        result.append("<reset><").append(color).append('>');
                        i += 2;
                        continue;
                    }

                    String decoration = DECORATIONS.get(code);
                    if (decoration != null) {
                        result.append('<').append(decoration).append('>');
                        i += 2;
                        continue;
                    }

                    if (code == 'r') {
                        result.append("<reset>");
                        i += 2;
                        continue;
                    }
                }
            }

            result.append(current);
            i++;
        }

        return result.toString();
    }

    private static int appendRgbCode(String input, int start, StringBuilder output) {
        char marker = input.charAt(start);

        if (start + 8 <= input.length() && input.charAt(start + 1) == '#') {
            String hex = input.substring(start + 2, start + 8);
            if (isHex(hex)) {
                output.append("<reset><#").append(hex).append('>');
                return start + 8;
            }
        }

        if (start + 14 <= input.length() && Character.toLowerCase(input.charAt(start + 1)) == 'x') {
            StringBuilder hex = new StringBuilder(6);
            for (int digit = 0; digit < 6; digit++) {
                int markerIndex = start + 2 + digit * 2;
                int digitIndex = markerIndex + 1;
                if (input.charAt(markerIndex) != marker || !isHexDigit(input.charAt(digitIndex))) {
                    return start;
                }
                hex.append(input.charAt(digitIndex));
            }
            output.append("<reset><#").append(hex).append('>');
            return start + 14;
        }

        return start;
    }

    private static boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!isHexDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexDigit(char value) {
        return value >= '0' && value <= '9'
                || value >= 'a' && value <= 'f'
                || value >= 'A' && value <= 'F';
    }
}
