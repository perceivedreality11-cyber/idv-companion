package io.github.manorcompanion;

public final class Csv {
    private Csv() {}
    /** Quote every field; neutralize spreadsheet formula prefixes even after leading whitespace. */
    public static String cell(String value) {
        if (value == null) value = "";
        int start = 0;
        while (start < value.length() && Character.isWhitespace(value.charAt(start))) start++;
        String trimmed = value.substring(start);
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
