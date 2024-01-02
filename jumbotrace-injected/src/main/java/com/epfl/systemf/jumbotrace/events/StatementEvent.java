package com.epfl.systemf.jumbotrace.events;

import com.epfl.systemf.jumbotrace.Formatting;

import static com.epfl.systemf.jumbotrace.Formatting.lastNameOnly;

public sealed interface StatementEvent extends Event {

    String filename();

    int startLine();

    int startCol();

    int endLine();

    int endCol();

    record ReturnStat(long id, long parentId, String methodName, String filename, int startLine, int startCol,
                      int endLine, int endCol) implements StatementEvent {
        @Override
        public String descr() {
            return "return statement targeting method " + methodName;
        }
    }

    record BreakStat(long id, long parentId, String targetDescr, int targetLine, int targetCol,
                     String filename, int startLine, int startCol, int endLine, int endCol) implements StatementEvent {
        @Override
        public String descr() {
            return "break statement targeting " + targetDescr + "(" + targetLine + ":" + targetCol + ")";
        }
    }

    record ContinueStat(long id, long parentId, String targetDescr, int targetLine, int targetCol,
                        String filename, int startLine, int startCol, int endLine,
                        int endCol) implements StatementEvent {
        @Override
        public String descr() {
            return "continue statement targeting " + targetDescr + "(" + targetLine + ":" + targetCol + ")";
        }
    }

    record YieldStat(long id, long parentId, Value yieldedVal, String targetDescr, int targetLine, int targetCol,
                     String filename, int startLine, int startCol, int endLine, int endCol) implements StatementEvent {
        @Override
        public String descr() {
            return "yield statement with value " + yieldedVal + " targeting " + targetDescr + "(" + targetLine + ":" + targetCol + ")";
        }
    }

    record SwitchStat(long id, long parentId, Value selector, String filename, int startLine, int startCol,
                      int endLine,
                      int endCol) implements StatementEvent {
        @Override
        public String descr() {
            return "switch statement with selector value " + selector;
        }
    }

    record VarDeclStat(long id, long parentId, String varName, String typeDescr,
                       String filename, int startLine, int startCol, int endLine, int endCol
    ) implements StatementEvent {
        @Override
        public String descr() {
            return "variable declarator statement for variable " + varName + " of type " + typeDescr;
        }
    }

    record InitializedFieldDeclStat(long id, long parentId, String className, String fieldName, String typeDescr,
                                    Object value,
                                    String filename, int startLine, int startCol, int endLine,
                                    int endCol) implements StatementEvent {
        @Override
        public String descr() {
            return "field " + fieldName + " of class " + lastNameOnly(className) + " with type " + typeDescr +
                    " initialized with value " + value;
        }
    }

    record ThrowStat(long id, long parentId, Value throwable, String filename, int startLine, int startCol,
                     int endLine, int endCol) implements StatementEvent {
        @Override
        public String descr() {
            return "throw statement throwing " + throwable;
        }
    }

    record Caught(long id, long parentId, Value throwable, String filename, int startLine, int startCol,
                  int endLine, int endCol) implements StatementEvent {
        @Override
        public String descr() {
            return "catch statement catching " + throwable;
        }
    }

    record AssertionStat(long id, long parentId, Value asserted, String assertionDescr,
                         String filename, int startLine, int startCol, int endLine,
                         int endCol) implements StatementEvent {
        @Override
        public String descr() {
            return "assertion statement for assertion " + assertionDescr + "(asserted expression yielded " + asserted + ")";
        }
    }

    record Exec(long id, long parentId,
                String filename, int startLine, int startCol, int endLine, int endCol) implements StatementEvent {
        @Override
        public String descr() {
            return "expression statement";
        }
    }

    record IfCond(long id, long parentId, Value evalRes, String filename, int startLine, int startCol, int endLine,
                  int endCol) implements StatementEvent {
        @Override
        public String descr() {
            return "condition of if evaluates to " + evalRes;
        }
    }

    record LoopCond(long id, long parentId, Value evalRes, String loopType, String filename, int startLine,
                    int startCol, int endLine, int endCol) implements StatementEvent {
        @Override
        public String descr() {
            return "condition of " + loopType + " evaluates to " + evalRes;
        }
    }

}
