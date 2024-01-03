package com.epfl.systemf.jumbotrace.events;

import java.io.Serializable;

public sealed interface Event extends Serializable permits StatementEvent, NonStatementEvent {

    long id();

    long parentId();

    String descr();

}
