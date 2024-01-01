package com.epfl.systemf.jumbotrace.frontend;

import com.epfl.systemf.jumbotrace.Config;
import com.epfl.systemf.jumbotrace.events.Event;
import com.epfl.systemf.jumbotrace.events.StatementEvent;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static com.epfl.systemf.jumbotrace.Config.HTML_FILE;
import static com.epfl.systemf.jumbotrace.Config.LOG_FILE;
import static com.epfl.systemf.jumbotrace.frontend.HtmlBuilder.Tag.*;

// TODO rename jumbotrace-injected into jumbotrace-logging

public final class Frontend {

    private static final String INDENT = "\u00A0";

    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Frontend takes arguments: the paths to the source directories");
            System.exit(0);
        }

        var srcFiles = new HashMap<String, List<String>>();
        readJavaFiles(srcFiles, Arrays.stream(args).map(File::new).toList());

        var rawEvents = readEvents();
        var eventsByParentId = computeParentToChildEventsMap(rawEvents);

        var html = new HtmlBuilder();
        //@formatter:off
        html.open(HTML, "style=\"font-family: Arial;font-color:gray;\"");

            html.open(HEADER);
                html.open(TITLE);
                    html.text("JumboTrace");
                html.close(TITLE);
            html.close(HEADER);

            html.open(BODY);
                generateHtmlTrace(Config.NO_PARENT_EVENT_CODE, html, eventsByParentId, srcFiles, 0);
            html.close(BODY);

        html.close(HTML);
        //@formatter:on
        writeHtml(html);
    }

    private static void generateHtmlTrace(
            long rootId,
            HtmlBuilder html,
            Map<Long, List<Event>> eventsByParentId,
            Map<String, List<String>> srcFiles,
            int indentLevel
    ) {
        for (var event : eventsByParentId.getOrDefault(rootId, List.of())) {
            var indent = INDENT.repeat(indentLevel);
            if (event instanceof StatementEvent statementEvent) {
                //@formatter:off
                html.open(DETAILS);
                    html.open(SUMMARY);
                        html.open(DIV, "style=\"font-family: Consolas;font-color:black;\"");
                            html.text(indent + codeFor(statementEvent, srcFiles));
                        html.close(DIV);
                    html.close(SUMMARY);
                    html.open(DIV);
                        html.text(indent + event.descr());
                        generateHtmlTrace(event.id(), html, eventsByParentId, srcFiles, indentLevel + 1);
                    html.close(DIV);
                html.close(DETAILS);
                //@formatter:on
            } else {
                html.open(DIV);
                html.text(indent + event.descr());
                generateHtmlTrace(event.id(), html, eventsByParentId, srcFiles, indentLevel + 1);
                html.close(DIV);
            }
        }
    }

    private static String codeFor(StatementEvent statementEvent, Map<String, List<String>> srcFiles) {
        var filename = statementEvent.filename();
        var fileLines = Objects.requireNonNull(srcFiles.get(filename));
        var startLine = statementEvent.startLine();
        var endLine = statementEvent.endLine();
        var sb = new StringBuilder();
        for (int oneBasedLineIdx = startLine; oneBasedLineIdx <= endLine; oneBasedLineIdx++) {
            var line = fileLines.get(oneBasedLineIdx - 1);
            var startCol = (oneBasedLineIdx == startLine) ? statementEvent.startCol() - 1 : 0;
            var endCol = Math.min((oneBasedLineIdx == endLine) ? statementEvent.endCol() - 1 : line.length(), line.length());
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(line, startCol, endCol);
        }
        return sb.toString();
    }

    private static void readJavaFiles(Map<String, List<String>> filesMap, List<File> directories) {
        for (var directory : directories) {
            for (var file : Objects.requireNonNull(directory.listFiles())) {
                if (file.isDirectory()) {
                    readJavaFiles(filesMap, List.of(file));
                } else if (file.getName().endsWith(".java")) {
                    try {
                        filesMap.put(file.toURI().normalize().toString(), Files.readAllLines(file.toPath()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private static Map<Long, List<Event>> computeParentToChildEventsMap(ArrayList<Event> events) {
        Map<Long, List<Event>> eventsByParentId = new HashMap<>();
        for (var event : events) {
            var parentId = event.parentId();
            if (!eventsByParentId.containsKey(parentId)) {
                eventsByParentId.put(parentId, new ArrayList<>());
            }
            eventsByParentId.get(parentId).add(event);
        }
        return eventsByParentId;
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

    private static void writeHtml(HtmlBuilder html) {
        try (var wr = new FileWriter(HTML_FILE)) {
            wr.write(html.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
