package com.epfl.systemf.jumbotrace.frontend;

public final class HtmlBuilder {

    private static final String INDENT = "   ";

    private final StringBuilder sb = new StringBuilder();
    private int indentLevel = 0;

    public HtmlBuilder(){
        addln("<!DOCTYPE html>");
    }

    public void open(Tag tag, String... options){
        addln(tag.open(options));
        indentLevel += 1;
    }

    public void close(Tag tag, String... options){
        indentLevel -= 1;
        addln(tag.close(options));
    }

    public void text(String text){
        addln(text);
    }

    public String result(){
        return sb.toString();
    }

    private void addln(String s){
        for (int i = 0; i < indentLevel; i++) {
            sb.append(INDENT);
        }
        sb.append(s);
        sb.append("\n");
    }

    public enum Tag {
        HTML, HEADER, BODY, DIV, DETAILS, SUMMARY, I, B, TITLE;

        public String open(String... options){
            return "<" + name().toLowerCase() + mkString(" ", options) + ">";
        }

        public String close(String... options){
            return "</" + name().toLowerCase() + mkString(" ", options) + ">";
        }
    }

    private static String mkString(String sep, String... strings){
        var sb = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            sb.append(strings[i]);
            if (i < strings.length - 1){
                sb.append(sep);
            }
        }
        return sb.toString();
    }

}
