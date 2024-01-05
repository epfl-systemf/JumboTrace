package ch.epfl.systemf.jumbotrace;

public final class Formatting {

    private Formatting() {
        throw new AssertionError("not instantiable");
    }

    /**
     * Takes a sequence of names concatenated using '/', '\' or '.' and returns the last name of this sequence
     */
    public static String lastNameOnly(String namesSeq) {
        return lastNamesOnly(namesSeq, 1);
    }

    /**
     * Takes a sequence of names concatenated using '/', '\' or '.' and returns the nNames last names of this sequence
     */
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

    /**
     * Moves words to a newline when the current line becomes longer than maxLength
     * <p>
     * Also applies the given indent on the lines
     */
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
