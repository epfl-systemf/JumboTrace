package com.epfl.systemf.jumbotrace;

public final class Formatting {

    private Formatting() {
        throw new AssertionError("not instantiable");
    }

    public static String lastNameOnly(String namesSeq) {
        return lastNamesOnly(namesSeq, 1);
    }

    public static String lastNamesOnly(String namesSeq, int nNames) {
        if (namesSeq == null) {
            return "?";
        }
        var idx = namesSeq.length() - 1;
        var namesFound = 0;
        while (idx >= 0) {
            char c = namesSeq.charAt(idx);
            if (c == '/' || c == '\\' || c == '.') {
                namesFound += 1;
            }
            if (namesFound >= nNames) {
                return namesSeq.substring(idx + 1);
            }
            idx--;
        }
        return namesSeq;
    }

    public static String insertNewlineWhenTooLong(String s, int maxLength, int indent) {
        var words = s.split("\\s");
        var sb = new StringBuilder();
        var width = indent;
        for (var word : words) {
            if (width + word.length() >= maxLength) {
                sb.append("\n");
                width = indent;
            }
            sb.append(word);
            sb.append(" ");
            width += word.length() + 1;
        }
        return sb.toString().indent(indent);
    }

}
