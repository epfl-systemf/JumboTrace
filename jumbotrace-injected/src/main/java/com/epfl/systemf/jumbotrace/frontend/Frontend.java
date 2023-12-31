package com.epfl.systemf.jumbotrace.frontend;

import com.epfl.systemf.jumbotrace.events.Event;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import static com.epfl.systemf.jumbotrace.Config.LOG_FILE;

// TODO rename jumbotrace-injected into jumbotrace-logging

public final class Frontend {

    public static void main(String[] args) {
        var events = new ArrayList<Event>();
        try (
                var fis = new FileInputStream(LOG_FILE);
                var ois = new ObjectInputStream(fis)
        ) {
            for (;;){
                events.add((Event) ois.readObject());
            }
        } catch (EOFException e) {
            // expected at the end of the stream
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        System.out.println(events.size() + " events");
        for (var event : events) {
            System.out.println(event);
        }
    }

}
