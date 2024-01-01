package com.epfl.systemf.jumbotrace.events;

public sealed interface NonStatementEvent extends Event {

    record InitializationEvent(long id, long parentId, String timestamp) implements NonStatementEvent {
    }

    record StaticMethodCall(long id, long parentId, String className, String methodName, String methodSig,
                            Value[] args,
                            String filename, int startLine, int startCol, int endLine,
                            int endCol) implements NonStatementEvent {

    }

    record NonStaticMethodCall(long id, long parentId, String className, String methodName, String methodSig,
                               Value receiver, Value[] args,
                               String filename, int startLine, int startCol, int endLine,
                               int endCol) implements NonStatementEvent {
    }

    sealed interface MethodEnterEvent extends NonStatementEvent {
    }

    record InstrumentedMethodEnter(long id, long parentId, String className, String methodName, String methodSig,
                                   String filename, int startLine, int startCol) implements MethodEnterEvent {
    }

    record NonInstrumentedMethodEnter(long id, long parentId, String className,
                                      String methodName) implements MethodEnterEvent {
    }

    record InstrumentedMethodExit(long id, long parentId, String methodName, String filename, int startLine,
                                  int startCol) implements NonStatementEvent {
    }

    record MethodReturnVal(long id, long parentId, String className, String methodName, Value retValue,
                           String filename, int startLine, int startCol, int endLine,
                           int endCol) implements NonStatementEvent {
    }

    record MethodReturnVoid(long id, long parentId, String className, String methodName,
                            String filename, int startLine, int startCol, int endLine,
                            int endCol) implements NonStatementEvent {
    }

    record ImplicitReturn(long id, long parentId, String methodName, String filename, int startLine,
                          int startCol) implements NonStatementEvent {
    }

    record SwitchExpr(long id, long parentId, Value selector, String filename, int startLine, int startCol,
                      int endLine,
                      int endCol) implements NonStatementEvent {
    }

    record LoopEnter(long id, long parentId, String loopType, String filename, int startLine, int startCol, int endLine,
                     int endCol) implements NonStatementEvent {
    }

    record LoopExit(long id, long parentId, String loopType, String filename, int startLine, int startCol, int endLine,
                    int endCol) implements NonStatementEvent {
    }

    record LoopCond(long id, long parentId, Value evalRes, String loopType, String filename, int startLine,
                    int startCol, int endLine,
                    int endCol) implements NonStatementEvent {
    }

    record ForEachLoopNextIter(long id, long parentId, Value newElem, String filename, int startLine, int startCol,
                               int endLine,
                               int endCol) implements NonStatementEvent {
    }

    record IfCond(long id, long parentId, Value evalRes, String filename, int startLine, int startCol, int endLine,
                  int endCol) implements NonStatementEvent {
    }

    record LocalVarAssignment(long id, long parentId, String varName, Value assignedValue,
                              String filename, int startLine, int startCol, int endLine,
                              int endCol) implements NonStatementEvent {
    }

    record LocalVarAssignOp(long id, long parentId, String varName, Value newValue,
                            Value oldValue, String operator, Value rhs,
                            String filename, int startLine, int startCol, int endLine,
                            int endCol) implements NonStatementEvent {
    }

    record LocalVarIncDecOp(long id, long parentId, String varName, Value result,
                            Value newValue, Value oldValue,
                            String filename, int startLine, int startCol, int endLine,
                            int endCol) implements NonStatementEvent {
    }

    record StaticFieldAssignment(long id, long parentId, String className, String fieldName, Value assignedValue,
                                 String filename, int startLine, int startCol, int endLine,
                                 int endCol) implements NonStatementEvent {
    }

    record StaticFieldAssignOp(long id, long parentId, String className, String fieldName, Value newValue,
                               Value oldValue, String operator, Value rhs,
                               String filename, int startLine, int startCol, int endLine,
                               int endCol) implements NonStatementEvent {
    }

    record StaticFieldIncDecOp(long id, long parentId, String className, String fieldName, Value result,
                               Value newValue, Value oldValue,
                               String filename, int startLine, int startCol, int endLine,
                               int endCol) implements NonStatementEvent {
    }

    record InstanceFieldAssignment(long id, long parentId, String className, Value instance, String fieldName,
                                   Value assignedValue,
                                   String filename, int startLine, int startCol, int endLine,
                                   int endCol) implements NonStatementEvent {
    }

    record InstanceFieldAssignOp(long id, long parentId, String className, Value instance, String fieldName,
                                 Value newValue,
                                 Value oldValue, String operator, Value rhs,
                                 String filename, int startLine, int startCol, int endLine,
                                 int endCol) implements NonStatementEvent {
    }

    record InstanceFieldIncDecOp(long id, long parentId, String className, Value instance, String fieldName,
                                 Value result,
                                 Value newValue, Value oldValue,
                                 String filename, int startLine, int startCol, int endLine,
                                 int endCol) implements NonStatementEvent {
    }

    record ArrayElemSet(long id, long parentId, Value array, Value index, Value assignedValue,
                        String filename, int startLine, int startCol, int endLine,
                        int endCol) implements NonStatementEvent {
    }

    record ArrayElemAssignOp(long id, long parentId, Value array, Value index, Value newValue,
                             Value oldValue, String operator, Value rhs,
                             String filename, int startLine, int startCol, int endLine,
                             int endCol) implements NonStatementEvent {
    }

    record ArrayElemIncDecOp(long id, long parentId, Value array, Value index, Value result,
                             Value newValue, Value oldValue,
                             String filename, int startLine, int startCol, int endLine,
                             int endCol) implements NonStatementEvent {
    }

    record CastAttempt(long id, long parentId, Value value, String targetTypeDescr, boolean willSucceed,
                       String filename, int startLine, int startCol, int endLine,
                       int endCol) implements NonStatementEvent {
    }

    record UnaryOp(long id, long parentId, Value res, Value arg, String operator,
                   String filename, int startLine, int startCol, int endLine, int endCol) implements NonStatementEvent {
    }

    record BinaryOp(long id, long parentId, Value lhs, Value rhs, String operator, Value result,
                    String filename, int startLine, int startCol, int endLine,
                    int endCol) implements NonStatementEvent {
    }

    record LocalVarRead(long id, long parentId, Value value, String varName,
                        String filename, int startLine, int startCol, int endLine,
                        int endCol) implements NonStatementEvent {
    }

    record StaticFieldRead(long id, long parentId, Value value, String className, String fieldName,
                           String filename, int startLine, int startCol, int endLine,
                           int endCol) implements NonStatementEvent {
    }

    record InstanceFieldRead(long id, long parentId, Value value, Value owner, String className, String fieldName,
                             String filename, int startLine, int startCol, int endLine,
                             int endCol) implements NonStatementEvent {
    }

    record ArrayAccess(long id, long parentId, Value value, Value array, Value index,
                       String filename, int startLine, int startCol, int endLine,
                       int endCol) implements NonStatementEvent {
    }

    record TernaryCondition(long id, long parentId, Value cond, String filename, int startLine, int startCol,
                            int endLine, int endCol) implements NonStatementEvent {
    }

    record TypeTest(long id, long parentId, Value result, Value testedObject, String targetTypeName, String filename,
                    int startLine,
                    int startCol, int endLine, int endCol) implements NonStatementEvent {
    }

}
