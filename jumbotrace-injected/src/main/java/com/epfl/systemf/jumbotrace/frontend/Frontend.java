package com.epfl.systemf.jumbotrace.frontend;

import com.epfl.systemf.jumbotrace.events.Event;
import com.epfl.systemf.jumbotrace.events.NonStatementEvent;
import com.epfl.systemf.jumbotrace.events.StatementEvent;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static com.epfl.systemf.jumbotrace.Config.LOG_FILE;
import static com.epfl.systemf.jumbotrace.Formatting.insertNewlineWhenTooLong;
import static com.epfl.systemf.jumbotrace.Formatting.lastNamesOnly;
import static com.epfl.systemf.jumbotrace.frontend.Colors.*;

// TODO rename jumbotrace-injected into jumbotrace-logging

/**
 * This frontend is meant to be used only for debugging and demo purposes, to show the data collected by the tracing system.
 * Its output should be formatted in a clearer way before using it in real use-cases.
 */
public final class Frontend {
    private static final int INDENT_GRANULARITY = 3;
    private static final int MAX_WIDTH = 165;
    private static final String VERBOSE_OPTION = "-verbose";
    private static final String CONSTRUCTOR_NAME = "<init>";
    private static final String CLS_INIT_NAME = "<clinit>";

    // This method assumes that the logging file is in the current directory
    // It is meant to be used with the automation script (automation.py)
    public static void main(String[] args) {

        if (args.length < 1 || args.length > 2 || (args.length == 2 && !args[1].equals(VERBOSE_OPTION))) {
            System.err.println("Arguments: <path to src> [" + VERBOSE_OPTION + "]");
            System.exit(0);
        }

        var srcDirPath = args[0];
        var verbose = args.length == 2;

        var srcFiles = new HashMap<String, List<String>>();
        readJavaFiles(srcFiles, new File(srcDirPath));

        var events = readEvents();
        printEvents(events, srcFiles, verbose);
    }

    private static void printEvents(List<Event> events, Map<String, List<String>> srcFiles, boolean verbose) {
        var indentLevel = 0;
        for (var event : events) {
            if (event instanceof StatementEvent statementEvent) {
                System.out.print(insertNewlineWhenTooLong(
                        codeFor(statementEvent, srcFiles),
                        MAX_WIDTH, indentLevel * INDENT_GRANULARITY
                ));
            } else if (event instanceof NonStatementEvent.MethodEnter methodEnter) {
                System.out.print(insertNewlineWhenTooLong(
                        ANSI_RED + "method enter " + ANSI_RESET + methodDeclaration(methodEnter, srcFiles),
                        MAX_WIDTH, indentLevel * INDENT_GRANULARITY
                ));
                indentLevel += 1;
            } else if (event instanceof NonStatementEvent.MethodExit methodExit) {
                indentLevel -= 1;
                System.out.print((ANSI_RED + "method exit " + ANSI_RESET + methodExit.descr()).indent(indentLevel * INDENT_GRANULARITY));
            } else if (verbose) {
                System.out.print(insertNewlineWhenTooLong(
                        "> " + event.descr(), MAX_WIDTH,
                        indentLevel * INDENT_GRANULARITY
                ));
            }
        }
    }

    private static String codeFor(StatementEvent statementEvent, Map<String, List<String>> srcFiles) {
        var filename = statementEvent.filename();
        var fileLines = Objects.requireNonNull(srcFiles.get(filename));
        var startLine = statementEvent.startLine();
        var startCol = statementEvent.startCol();
        var endLine = statementEvent.endLine();
        var endCol = statementEvent.endCol();
        var sb = new StringBuilder();
        if (endLine > 0 && endCol > 0) {
            for (int oneBasedLineIdx = startLine; oneBasedLineIdx <= endLine; oneBasedLineIdx++) {
                var line = fileLines.get(oneBasedLineIdx - 1);
                var nLeadingSpaces = nLeadingSpaces(line);
                for (int oneBasedColIdx = 1 + nLeadingSpaces; oneBasedColIdx <= line.length(); oneBasedColIdx++) {
                    if (oneBasedLineIdx == startLine && oneBasedColIdx == startCol) {
                        sb.append(ANSI_YELLOW);
                    }
                    if (oneBasedLineIdx == endLine && oneBasedColIdx == endCol) {
                        sb.append(ANSI_RESET);
                    }
                    sb.append(line.charAt(oneBasedColIdx - 1));
                }
                if (oneBasedLineIdx < endLine) {
                    sb.append("\n");
                }
            }
        } else {
            var line = fileLines.get(startLine - 1);
            var nLeadingSpaces = nLeadingSpaces(line);
            sb.append(line, nLeadingSpaces, startCol - 1);
            sb.append(ANSI_YELLOW);
            sb.append(line.charAt(startCol - 1));
            sb.append(ANSI_RESET);
            if (startCol < line.length()) {
                sb.append(line.substring(startCol));
            }
        }
        sb.append(ANSI_CYAN);
        sb.append("  // ");
        sb.append(statementEvent.descr());
        sb.append(" at ");
        sb.append(lastNamesOnly(filename, 2));
        sb.append(":");
        sb.append(startLine);
        sb.append(":");
        sb.append(startCol);
        sb.append(ANSI_RESET);
        return sb.toString();
    }

    private static String methodDeclaration(NonStatementEvent.MethodEnter methodEnter, Map<String, List<String>> srcFiles) {
        var filename = methodEnter.filename();
        var startLine = methodEnter.startLine();
        var startCol = methodEnter.startCol();
        var sb = new StringBuilder();
        // Initialization methods are not handled correctly by the rest of this method
        // (at least when they are not declared explicitly)
        // For simplicity we special-case them and return a generic description
        if (methodEnter.methodName().equals(CONSTRUCTOR_NAME)) {
            sb.append("constructor of ");
            sb.append(methodEnter.className());
        } else if (methodEnter.methodName().equals(CLS_INIT_NAME)) {
            sb.append("class initialization method of ");
            sb.append(methodEnter.className());
        } else {
            var srcFile = srcFiles.get(filename);
            var foundClosingParenth = false;
            for (int oneBasedLineIdx = startLine; !foundClosingParenth; oneBasedLineIdx++) {
                var line = srcFile.get(oneBasedLineIdx - 1);
                for (int oneBasedColIdx = startCol; oneBasedColIdx <= line.length() && !foundClosingParenth; oneBasedColIdx++) {
                    var c = line.charAt(oneBasedColIdx - 1);
                    sb.append(c);
                    foundClosingParenth = (c == ')');
                }
            }
        }
        sb.append(ANSI_CYAN);
        sb.append("  // ");
        sb.append(methodEnter.descr());
        sb.append(" at ");
        sb.append(lastNamesOnly(filename, 2));
        sb.append(":");
        sb.append(startLine);
        sb.append(":");
        sb.append(startCol);
        sb.append(ANSI_RESET);
        return sb.toString();
    }

    private static int nLeadingSpaces(String s) {
        var cnt = 0;
        while (cnt < s.length() && s.charAt(cnt) == ' ') {
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

    @SuppressWarnings("unused")
    private static void require(boolean cond) {
        if (!cond) {
            throw new IllegalArgumentException();
        }
    }

}
