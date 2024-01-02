package com.epfl.systemf.jumbotrace.frontend;

import com.epfl.systemf.jumbotrace.events.Event;
import com.epfl.systemf.jumbotrace.events.NonStatementEvent;
import com.epfl.systemf.jumbotrace.events.StatementEvent;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static com.epfl.systemf.jumbotrace.Config.LOG_FILE;
import static com.epfl.systemf.jumbotrace.Formatting.lastNameOnly;
import static com.epfl.systemf.jumbotrace.frontend.Colors.*;

// TODO rename jumbotrace-injected into jumbotrace-logging

public final class Frontend {
    private static final String INDENT = "   ";
    private static final int LEFTMOST_COL_FOR_CODE_POSITION = 120;

    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Frontend takes exactly 1 argument: the path to the source directory");
            System.exit(0);
        }

        var srcDirPath = args[0];
        var srcFiles = new HashMap<String, List<String>>();
        readJavaFiles(srcFiles, new File(srcDirPath));

        var events = readEvents();
        printEvents(events, srcFiles);
    }

    private static void printEvents(List<Event> events, Map<String, List<String>> srcFiles){
        var indentLevel = 0;
        for (var event: events){
            if (event instanceof StatementEvent statementEvent && statementEvent.endLine() > 0){
                System.out.println(INDENT.repeat(indentLevel) + ANSI_CYAN + "// " + statementEvent.descr() + ANSI_RESET);
                System.out.println(codeFor(statementEvent, srcFiles, indentLevel));
            } else {
                System.out.println(INDENT.repeat(indentLevel) + ANSI_YELLOW + " > " + event.descr() + ANSI_RESET);
            }
            if (event instanceof NonStatementEvent.MethodEnter){
                indentLevel += 1;
            } else if (event instanceof NonStatementEvent.MethodExit){
                indentLevel -= 1;
            }
        }
    }

    private static String codeFor(StatementEvent statementEvent, Map<String, List<String>> srcFiles, int indentLevel) {
        var filename = statementEvent.filename();
        var fileLines = Objects.requireNonNull(srcFiles.get(filename));
        var startLine = statementEvent.startLine();
        var startCol = statementEvent.startCol();
        var endLine = statementEvent.endLine();
        var endCol = statementEvent.endCol();
        require(endLine > 0);
        require(endCol > 0);
        var sb = new StringBuilder();
        var nCharsInCol = 0;
        for (int oneBasedLineIdx = startLine; oneBasedLineIdx <= endLine; oneBasedLineIdx++) {
            sb.append(INDENT.repeat(indentLevel));
            var line = fileLines.get(oneBasedLineIdx - 1);
            var nLeadingSpaces = nLeadingSpaces(line);
            nCharsInCol = INDENT.length() * indentLevel;
            for (int oneBasedColIdx = 1 + nLeadingSpaces; oneBasedColIdx <= line.length(); oneBasedColIdx++){
                if (oneBasedLineIdx == startLine && oneBasedColIdx == startCol){
                    sb.append(ANSI_WHITE_BACKGROUND);
                    sb.append(ANSI_BLACK);
                }
                if (oneBasedLineIdx == endLine && oneBasedColIdx == endCol){
                    sb.append(ANSI_BLACK_BACKGROUND);
                    sb.append(ANSI_RESET);
                }
                sb.append(line.charAt(oneBasedColIdx-1));
                nCharsInCol += 1;
            }
            if (oneBasedLineIdx < endLine){
                sb.append("\n");
            }
        }
        sb.append(ANSI_BLACK_BACKGROUND);
        sb.append(ANSI_RESET);
        sb.append(" ");
        sb.append(".".repeat(Math.max(0, LEFTMOST_COL_FOR_CODE_POSITION - nCharsInCol)));
        sb.append(" ");
        sb.append(lastNameOnly(statementEvent.filename()));
        sb.append(":");
        sb.append(statementEvent.startLine());
        sb.append(":");
        sb.append(statementEvent.startCol());
        return sb.toString();
    }

    private static int nLeadingSpaces(String s){
        var cnt = 0;
        while (cnt < s.length() && s.charAt(cnt) == ' '){
            cnt++;
        }
        return cnt;
    }

    private static void readJavaFiles(Map<String, List<String>> filesMap, File directory) {
        for (var file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                readJavaFiles(filesMap, file);
            } else if (file.getName().endsWith(".java")) {
                try {
                    filesMap.put(file.toURI().normalize().toString(), Files.readAllLines(file.toPath()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static ArrayList<Event> readEvents() {
        var events = new ArrayList<Event>();
        try (
                var fis = new FileInputStream(LOG_FILE);
                var ois = new ObjectInputStream(fis)
        ) {
            for (; ; ) {
                events.add((Event) ois.readObject());
            }
        } catch (EOFException e) {
            // expected at the end of the stream
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return events;
    }

    private static void require(boolean cond){
        if (!cond){
            throw new IllegalArgumentException();
        }
    }

}
