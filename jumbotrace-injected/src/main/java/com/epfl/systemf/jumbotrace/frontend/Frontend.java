package com.epfl.systemf.jumbotrace.frontend;

import com.epfl.systemf.jumbotrace.Config;
import com.epfl.systemf.jumbotrace.events.Event;
import com.epfl.systemf.jumbotrace.events.NonStatementEvent;
import com.epfl.systemf.jumbotrace.events.StatementEvent;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.epfl.systemf.jumbotrace.Config.HTML_FILE;
import static com.epfl.systemf.jumbotrace.Config.LOG_FILE;
import static com.epfl.systemf.jumbotrace.frontend.HtmlBuilder.Tag.*;

// TODO rename jumbotrace-injected into jumbotrace-logging

public final class Frontend {

    public static void main(String[] args) {

        if (args.length == 0){
            System.err.println("Frontend takes arguments: the paths to the source directories");
            System.exit(0);
        }

        var srcFiles = new HashMap<String, List<String>>();
        readJavaFiles(srcFiles, Arrays.stream(args).map(File::new).toList());

        var rawEvents = readEvents();
        var eventsByParentId = computeParentToChildEventsMap(rawEvents);

        var html = new HtmlBuilder();
        //@formatter:off
        html.open(HTML);

            html.open(HEADER);
                html.open(TITLE);
                    html.text("JumboTrace");
                html.close(TITLE);
            html.close(HEADER);

            html.open(BODY);
                generateHtmlTrace(Config.NO_PARENT_EVENT_CODE, html, eventsByParentId, srcFiles);
            html.close(BODY);

        html.close(HTML);
        //@formatter:on
        writeHtml(html);
    }

    private static void generateHtmlTrace(
            long rootId,
            HtmlBuilder html,
            Map<Long, List<Event>> eventsByParentId,
            Map<String, List<String>> srcFiles
    ) {
        for (var event : eventsByParentId.getOrDefault(rootId, List.of())) {
            if (event instanceof StatementEvent statementEvent) {
                html.open(DIV);
                html.open(DIV);
                html.text(codeFor(statementEvent, srcFiles));
                html.open(DIV);
                generateHtmlTrace(event.id(), html, eventsByParentId, srcFiles);
                html.open(DIV);
            } else if (event instanceof NonStatementEvent.InstrumentedMethodEnter){
                generateHtmlTrace(event.id(), html, eventsByParentId, srcFiles);
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
            var line = fileLines.get(oneBasedLineIdx-1);
            var startCol = (oneBasedLineIdx == startLine) ? statementEvent.startCol()-1 : 0;
            var endCol = Math.min((oneBasedLineIdx == endLine) ? statementEvent.endCol()-1 : line.length(), line.length());
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(line, startCol, endCol);
        }
        return sb.toString();
    }

    private static void readJavaFiles(Map<String, List<String>> filesMap, List<File> directories) {
        for (var directory: directories){
            for (var file: Objects.requireNonNull(directory.listFiles())){
                if (file.isDirectory()){
                    readJavaFiles(filesMap, List.of(file));
                } else if (file.getName().endsWith(".java")){
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
            var id = event.parentId();
            if (!eventsByParentId.containsKey(id)) {
                eventsByParentId.put(id, new ArrayList<>());
            }
            eventsByParentId.get(id).add(event);
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
            wr.write(html.result());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
