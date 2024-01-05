package ch.epfl.systemf.jumbotrace.events;

import java.io.Serializable;

/**
 * An event as part of a trace
 */
public sealed interface Event extends Serializable permits StatementEvent, NonStatementEvent {

    long id();

    long parentId();

    String descr();

}
