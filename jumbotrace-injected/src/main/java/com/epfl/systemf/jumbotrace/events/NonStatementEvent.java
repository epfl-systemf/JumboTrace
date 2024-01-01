package com.epfl.systemf.jumbotrace.events;

import java.util.StringJoiner;

public sealed interface NonStatementEvent extends Event {

    record InitializationEvent(long id, long parentId, String timestamp) implements NonStatementEvent {
        @Override
        public String descr() {
            return "initialization of the tracing class happened at " + timestamp;
        }
    }

    sealed interface MethodCallEvent extends NonStatementEvent {
        String className();
        String methodName();
        String methodSig();
    }

    record StaticMethodCall(long id, long parentId, String className, String methodName, String methodSig,
                            Value[] args,
                            String filename, int startLine, int startCol, int endLine,
                            int endCol) implements MethodCallEvent {
        @Override
        public String descr() {
            return "invocation of static method " + className + "." + methodName + methodSig + " with arguments " +
                   argsListToString(args);
        }
    }

    record NonStaticMethodCall(long id, long parentId, String className, String methodName, String methodSig,
                               Value receiver, Value[] args,
                               String filename, int startLine, int startCol, int endLine,
                               int endCol) implements MethodCallEvent {
        @Override
        public String descr() {
            return "invocation of non-static method " + className + "." + methodName + methodSig + " with receiver " +
                    receiver + " and arguments " + argsListToString(args);
        }
    }

    sealed interface MethodEnterEvent extends NonStatementEvent {
        String className();
        String methodName();
        String filename();
    }

    record InstrumentedMethodEnter(long id, long parentId, String className, String methodName, String methodSig,
                                   String filename, int startLine, int startCol) implements MethodEnterEvent {
        @Override
        public String descr() {
            return "entering instrumented method " + className + "." + methodName + methodSig;
        }
    }

    record NonInstrumentedMethodEnter(long id, long parentId, String className, String methodName,
                                      String filename) implements MethodEnterEvent {
        @Override
        public String descr() {
            return "entering non-instrumented method " + className + "." + methodName + " [/!\\ logging not available]";
        }
    }

    record InstrumentedMethodExit(long id, long parentId, String methodName, String filename, int startLine,
                                  int startCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "method " + methodName + " exits";
        }
    }

    record NonInstrumentedMethodExit(long id, long parentId, String methodName) implements NonStatementEvent {
        @Override
        public String descr() {
            return "method " + methodName + " exits";
        }
    }

    record MethodReturnVal(long id, long parentId, String className, String methodName, Value retValue,
                           String filename, int startLine, int startCol, int endLine,
                           int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "method " + className + "." + methodName + " returned " + retValue;
        }
    }

    record MethodReturnVoid(long id, long parentId, String className, String methodName,
                            String filename, int startLine, int startCol, int endLine,
                            int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "method " + className + "." + methodName + " returned void";
        }
    }

    record ImplicitReturn(long id, long parentId, String methodName, String filename, int startLine,
                          int startCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "method " + methodName + " returns void after reaching the end of its body";
        }
    }

    record SwitchExpr(long id, long parentId, Value selector, String filename, int startLine, int startCol,
                      int endLine, int endCol) implements NonStatementEvent {
        // TODO maybe add result value?
        @Override
        public String descr() {
            return "switch expression with selector value " + selector;
        }
    }

    record LoopEnter(long id, long parentId, String loopType, String filename, int startLine, int startCol, int endLine,
                     int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "entering " + loopType;
        }
    }

    record LoopExit(long id, long parentId, String loopType, String filename, int startLine, int startCol, int endLine,
                    int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "exiting " + loopType;
        }
    }

    record LoopCond(long id, long parentId, Value evalRes, String loopType, String filename, int startLine,
                    int startCol, int endLine, int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "condition of " + loopType + " evaluates to " + evalRes;
        }
    }

    record ForEachLoopNextIter(long id, long parentId, Value newElem, String filename, int startLine, int startCol,
                               int endLine, int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "starting a new iteration of for-each loop, new value is " + newElem;
        }
    }

    record IfCond(long id, long parentId, Value evalRes, String filename, int startLine, int startCol, int endLine,
                  int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "condition of if evaluates to " + evalRes;
        }
    }

    record LocalVarAssignment(long id, long parentId, String varName, Value assignedValue,
                              String filename, int startLine, int startCol, int endLine,
                              int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "assigning value " + assignedValue + " to local variable " + varName;
        }
    }

    record LocalVarAssignOp(long id, long parentId, String varName, Value newValue,
                            Value oldValue, String operator, Value rhs,
                            String filename, int startLine, int startCol, int endLine,
                            int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "updating local variable " + varName + " from value " + oldValue + " to value " + newValue +
                    " using operator " + operator + " (right-hand side was " + rhs + ")";
        }
    }

    record LocalVarIncDecOp(long id, long parentId, String varName, Value result,
                            Value newValue, Value oldValue,
                            String filename, int startLine, int startCol, int endLine,
                            int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            // TODO maybe save operator to be more precise?
            return "updating local variable " + varName + " from value " + oldValue + " to value " + newValue +
                    " (result is " + result + ")";
        }
    }

    record StaticFieldAssignment(long id, long parentId, String className, String fieldName, Value assignedValue,
                                 String filename, int startLine, int startCol, int endLine,
                                 int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "assigning value " + assignedValue + " to static field " + fieldName + " of class " + className;
        }
    }

    record StaticFieldAssignOp(long id, long parentId, String className, String fieldName, Value newValue,
                               Value oldValue, String operator, Value rhs,
                               String filename, int startLine, int startCol, int endLine,
                               int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "updating static field " + fieldName + " of class " + className + " from value " + oldValue +
                    " to value " + newValue + " using operator " + operator + " (right-hand side was " + rhs + ")";
        }
    }

    record StaticFieldIncDecOp(long id, long parentId, String className, String fieldName, Value result,
                               Value newValue, Value oldValue,
                               String filename, int startLine, int startCol, int endLine,
                               int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            // TODO maybe save operator to be more precise?
            return "updating static field " + fieldName + " of class " + className + " from value " + oldValue +
                    " to value " + newValue + " (result is " + result + ")";
        }
    }

    record InstanceFieldAssignment(long id, long parentId, String className, Value instance, String fieldName,
                                   Value assignedValue, String filename, int startLine, int startCol, int endLine,
                                   int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "assigning value " + assignedValue + " to field " + fieldName + " of instance " + instance +
                    " (field is defined in class " + className + ")";
        }
    }

    record InstanceFieldAssignOp(long id, long parentId, String className, Value instance, String fieldName,
                                 Value newValue,
                                 Value oldValue, String operator, Value rhs,
                                 String filename, int startLine, int startCol, int endLine,
                                 int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "updating field " + fieldName + " of instance " + instance + " from value " + oldValue +
                    " to value " + newValue + " using operator " + operator + " (right-hand side was " + rhs +
                    " and field is defined in class " + className + ")";
        }
    }

    record InstanceFieldIncDecOp(long id, long parentId, String className, Value instance, String fieldName,
                                 Value result,
                                 Value newValue, Value oldValue,
                                 String filename, int startLine, int startCol, int endLine,
                                 int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            // TODO maybe save operator to be more precise?
            return "updating field " + fieldName + " of instance " + instance + " from value " + oldValue +
                    " to value " + newValue + " (field is defined in class " + className + " and result is " + result + ")";
        }
    }

    record ArrayElemSet(long id, long parentId, Value array, Value index, Value assignedValue,
                        String filename, int startLine, int startCol, int endLine,
                        int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "writing value " + assignedValue + " to index " + index + " of array " + array;
        }
    }

    record ArrayElemAssignOp(long id, long parentId, Value array, Value index, Value newValue,
                             Value oldValue, String operator, Value rhs,
                             String filename, int startLine, int startCol, int endLine,
                             int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "updating slot of index " + index + " in array " + array + " from value " + oldValue +" to value " +
                    newValue + " using operator " + operator + " (right-hand side was " + rhs + ")";
        }
    }

    record ArrayElemIncDecOp(long id, long parentId, Value array, Value index, Value result,
                             Value newValue, Value oldValue,
                             String filename, int startLine, int startCol, int endLine,
                             int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            // TODO maybe save operator to be more precise?
            return "updating slot of index " + index + " in array " + array + " from value " + oldValue + " to value " + newValue;
        }
    }

    record CastAttempt(long id, long parentId, Value value, String targetTypeDescr, boolean willSucceed,
                       String filename, int startLine, int startCol, int endLine,
                       int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "cast of " + value + " to type " + targetTypeDescr + (willSucceed ? " succeeds" : " fails with ClassCastException");
        }
    }

    record UnaryOp(long id, long parentId, Value res, Value arg, String operator,
                   String filename, int startLine, int startCol, int endLine, int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "application of unary operator " + operator + " to value " + arg + " yields " + res;
        }
    }

    record BinaryOp(long id, long parentId, Value lhs, Value rhs, String operator, Value result,
                    String filename, int startLine, int startCol, int endLine,
                    int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "application of binary operator " + operator + " to values " + lhs + " (left) and " + rhs +
                    " (right) yields " + result;
        }
    }

    record LocalVarRead(long id, long parentId, Value value, String varName,
                        String filename, int startLine, int startCol, int endLine,
                        int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "access to local variable " + varName + " yields value " + value;
        }
    }

    record StaticFieldRead(long id, long parentId, Value value, String className, String fieldName,
                           String filename, int startLine, int startCol, int endLine,
                           int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "access to static field " + fieldName + " of class " + className + " yields value " + value;
        }
    }

    record InstanceFieldRead(long id, long parentId, Value value, Value owner, String className, String fieldName,
                             String filename, int startLine, int startCol, int endLine,
                             int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "access to field " + fieldName + " of instance " + owner + " yields value " + value +
                    "(field is defined in class " + className + ")";
        }
    }

    record ArrayAccess(long id, long parentId, Value value, Value array, Value index,
                       String filename, int startLine, int startCol, int endLine,
                       int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "access to slot of index " + index + " of array " + array + " yields value " + value;
        }
    }

    record TernaryCondition(long id, long parentId, Value cond, String filename, int startLine, int startCol,
                            int endLine, int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "condition of ternary operator evaluates to " + cond;
        }
    }

    record TypeTest(long id, long parentId, Value result, Value testedObject, String targetTypeName, String filename,
                    int startLine, int startCol, int endLine, int endCol) implements NonStatementEvent {
        @Override
        public String descr() {
            return "type test of " + testedObject + " against type " + targetTypeName + " yields " + result;
        }
    }

    private static String argsListToString(Value[] args){
        var sj = new StringJoiner(", ", "[", "]");
        for (Value arg : args) {
            sj.add(arg.toString());
        }
        return sj.toString();
    }

}
