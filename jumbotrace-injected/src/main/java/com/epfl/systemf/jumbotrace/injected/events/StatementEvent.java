package com.epfl.systemf.jumbotrace.injected.events;

public interface StatementEvent extends Event {

    String filename();

    int startLine();

    int startCol();

    int endLine();

    int endCol();

    record ReturnStat(long id, long parentId, String methodName, String filename, int startLine, int startCol,
                      int endLine,
                      int endCol) implements StatementEvent {
    }

    record BreakStat(long id, long parentId, String targetDescr, int targetLine, int targetCol,
                     String filename, int startLine, int startCol, int endLine, int endCol) implements StatementEvent {
    }

    record ContinueStat(long id, long parentId, String targetDescr, int targetLine, int targetCol,
                        String filename, int startLine, int startCol, int endLine,
                        int endCol) implements StatementEvent {
    }

    record YieldStat(long id, long parentId, Value yieldedVal, String targetDescr, int targetLine, int targetCol,
                     String filename, int startLine, int startCol, int endLine, int endCol) implements StatementEvent {
    }

    record SwitchStat(long id, long parentId, Value selector, String filename, int startLine, int startCol,
                      int endLine,
                      int endCol) implements StatementEvent {
    }

    record VarDeclStat(long id, long parentId, String varName, String typeDescr,
                       String filename, int startLine, int startCol, int endLine, int endCol
    ) implements StatementEvent {
    }

    record ThrowStat(long id, long parentId, Value throwable, String filename, int startLine, int startCol,
                     int endLine, int endCol) implements StatementEvent {
    }

    record Caught(long id, long parentId, Value throwable, String filename, int startLine, int startCol,
                  int endLine, int endCol) implements StatementEvent {
    }

    record AssertionStat(long id, long parentId, Value asserted, String assertionDescr,
                         String filename, int startLine, int startCol, int endLine,
                         int endCol) implements StatementEvent {
    }

    record Exec(long id, long parentId,
                String filename, int startLine, int startCol, int endLine, int endCol) implements StatementEvent {
    }

}
