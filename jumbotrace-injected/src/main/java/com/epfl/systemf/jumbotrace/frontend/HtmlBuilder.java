package com.epfl.systemf.jumbotrace.frontend;

public final class HtmlBuilder {

    private static final String INDENT = "\u00A0";

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

    public void br(){
        addln("<br>");
    }

    public String toString(){
        return sb.toString();
    }

    private void addln(String s){
        sb.append(INDENT.repeat(indentLevel));
        sb.append(s);
        sb.append("\n");
    }

    public enum Tag {
        HTML, HEADER, BODY, DIV, DETAILS, SUMMARY, I, B, TITLE;

        public String open(String... options){
            return "<" + name().toLowerCase() + formatOptions(options) + ">";
        }

        public String close(String... options){
            return "</" + name().toLowerCase() + " " + formatOptions(options) + ">";
        }
    }

    private static String formatOptions(String... strings){
        var sb = new StringBuilder();
        if (strings.length > 0){
            sb.append(" ");
        }
        for (int i = 0; i < strings.length; i++) {
            sb.append(strings[i]);
            if (i < strings.length - 1){
                sb.append(" ");
            }
        }
        return sb.toString();
    }

}
