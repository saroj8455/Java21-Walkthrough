package com.walkthrough.api.transaction.importing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Minimal RFC 4180 CSV support for ONE line: split into cells, and join cells back into a line.
 *
 * <p>Why hand-written instead of a library? Statement files are one record per line, so this ~50 line
 * class is enough, has no dependency, and is a good exercise in plain Java string handling.
 * (For multi-line quoted values you would use Apache Commons CSV or Jackson CSV.)
 *
 * <p>Rules: cells are separated by commas; a cell wrapped in double quotes may contain commas;
 * a doubled quote {@code ""} inside a quoted cell means one literal quote.
 */
public final class CsvLine {

    private CsvLine() {
    }

    /**
     * Splits one CSV line into cells.
     *
     * @throws IllegalArgumentException if a quoted cell is not closed
     */
    public static List<String> parse(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();   // mutable string, like building an array of chars
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    boolean escapedQuote = i + 1 < line.length() && line.charAt(i + 1) == '"';
                    if (escapedQuote) {
                        current.append('"');
                        i++;                            // skip the second quote
                    } else {
                        inQuotes = false;               // closing quote
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                cells.add(current.toString());
                current.setLength(0);                   // reset the builder for the next cell
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("Malformed CSV: a quoted value is not closed");
        }
        cells.add(current.toString());
        return cells;
    }

    /** Joins cells into one CSV line, quoting only where needed. */
    public static String format(List<String> cells) {
        return cells.stream()
                .map(CsvLine::escape)                   // method reference == cell -> escape(cell)
                .collect(Collectors.joining(","));
    }

    static String escape(String cell) {
        if (cell == null) {
            return "";
        }
        boolean needsQuotes = cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r");
        return needsQuotes ? '"' + cell.replace("\"", "\"\"") + '"' : cell;
    }
}
