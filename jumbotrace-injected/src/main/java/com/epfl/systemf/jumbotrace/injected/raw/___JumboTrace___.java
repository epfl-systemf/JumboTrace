package com.epfl.systemf.jumbotrace.injected.raw;

import com.epfl.systemf.jumbotrace.Config;
import com.epfl.systemf.jumbotrace.events.Event;
import com.epfl.systemf.jumbotrace.events.NonStatementEvent;
import com.epfl.systemf.jumbotrace.events.NonStatementEvent.*;
import com.epfl.systemf.jumbotrace.events.StatementEvent;
import com.epfl.systemf.jumbotrace.events.StatementEvent.*;
import com.epfl.systemf.jumbotrace.events.Value;
import com.epfl.systemf.jumbotrace.injected.annot.Specialize;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import static com.epfl.systemf.jumbotrace.Config.LOG_FILE;

@SuppressWarnings("unused")
public class ___JumboTrace___ {

    // May be set to null by the code generation system
    private static final PrintStream PRINT_STREAM = System.out;

    private static final int NO_POS = -1;

    private static final String COLOR_YELLOW_CODE = "\u001B[33m";
    private static final String COLOR_RED_CODE = "\u001B[31m";
    private static final String COLOR_RESET_CODE = "\u001B[0m";

    private static void log(Object... objects) {
        if (PRINT_STREAM != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(COLOR_YELLOW_CODE);
            sb.append("[jbt] ");
            sb.append(" ".repeat(indent));
            for (var obj : objects) {
                try {
                    sb.append(obj);
                } catch (Throwable e) {  // relies on user-defined toString, so we need to recover from exceptions
                    sb.append("<??>");
                }
            }
            sb.append(COLOR_RESET_CODE);
            PRINT_STREAM.println(sb);
        }
    }

    private static final ObjectOutputStream outputStream;
    private static final Deque<StatementEvent> statementEventsStack = new LinkedList<>();
    private static final Deque<MethodEnterEvent> callsStack = new LinkedList<>();

    static {
        try {
            var path = Path.of(LOG_FILE);
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            outputStream = new ObjectOutputStream(new FileOutputStream(LOG_FILE));
            var time = LocalDateTime.now();
            outputStream.writeObject(new InitializationEvent(genEventId(), Config.NO_PARENT_EVENT_CODE, time.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private static void updateStacksForNewStat(StatementEvent statementEvent) {
        statementEventsStack.removeFirst();
        statementEventsStack.addFirst(statementEvent);
    }

    private static void updateStacksForCall(NonStatementEvent.MethodEnterEvent methodEnterEvent) {
        callsStack.addFirst(methodEnterEvent);
        statementEventsStack.addFirst(null);
    }

    private static void updateStacksForReturn() {
        callsStack.removeFirst();
        statementEventsStack.removeFirst();
    }

    private static long currentEnclosingCallEventId() {
        return callsStack.isEmpty() ? Config.NO_PARENT_EVENT_CODE : callsStack.getFirst().id();
    }

    private static long currentEnclosingStatementEventId() {
        if (statementEventsStack.isEmpty()){
            return Config.NO_PARENT_EVENT_CODE;
        } else {
            var first = statementEventsStack.getFirst();
            return first == null ? currentEnclosingCallEventId() : first.id();
        }
    }

    private static long nextEventId = 1;

    private static long genEventId() {
        return nextEventId++;
    }

    private static boolean loggingEnabled = true;
    private static int indent = 0;

    private static void enableLogging() {
        loggingEnabled = true;
    }

    private static void disableLogging() {
        loggingEnabled = false;
    }

    private static void writeEvent(Event event) {
        try {
            outputStream.writeObject(event);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void newStatementEvent(StatementEvent statementEvent) {
        writeEvent(statementEvent);
        updateStacksForNewStat(statementEvent);
    }

    private static void newNonStatementEvent(NonStatementEvent nonStatementEvent) {
        writeEvent(nonStatementEvent);
        if (nonStatementEvent instanceof NonStatementEvent.InstrumentedMethodEnter methodEnterEvent){
            updateStacksForCall(methodEnterEvent);
        } else if (nonStatementEvent instanceof NonStatementEvent.InstrumentedMethodExit){
            updateStacksForReturn();
        }
    }

    public static void staticMethodCall(String className, String methodName, String methodSig, Object[] args,
                                        String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("CALL: ", className, ".", methodName, methodSig, " args=", Arrays.toString(args),
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new StaticMethodCall(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    className,
                    methodName,
                    methodSig,
                    makeArgsValuesArray(args),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static void nonStaticMethodCall(String className, String methodName, String methodSig, Object receiver, Object[] args,
                                           String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("CALL: ", className, ".", methodName, methodSig, " receiver='", receiver, "' args=", Arrays.toString(args),
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new NonStaticMethodCall(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    className,
                    methodName,
                    methodSig,
                    Value.valueFor(receiver),
                    makeArgsValuesArray(args),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static void methodEnter(String className, String methodName, String methodSig, String filename, int line, int col) {
        if (loggingEnabled) {
            disableLogging();
            indent += 1;
            log("ENTER: ", className, ".", methodName, methodSig, " at ", formatPosition(filename, line, col));
            // TODO check stacktrace using an exception
            newNonStatementEvent(new InstrumentedMethodEnter(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    className,
                    methodName,
                    methodSig,
                    filename,
                    line,
                    col
            ));
            enableLogging();
        }
    }

    public static void methodExit(String methodName, String filename, int line, int col) {
        if (loggingEnabled) {
            disableLogging();
            log("METHOD EXIT ", methodName, " at ", formatPosition(filename, line, col));
            newNonStatementEvent(new InstrumentedMethodExit(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    methodName,
                    filename,
                    line,
                    col
            ));
            indent -= 1;
            enableLogging();
        }
    }

    public static @Specialize Object methodRet(String className, String methodName, @Specialize Object retValue,
                                               String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log(className, ".", methodName, " RETURNS '", retValue, "' at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new MethodReturnVal(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    className,
                    methodName,
                    Value.valueFor(retValue),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return retValue;
    }

    public static void methodRetVoid(String className, String methodName, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log(className, ".", methodName, " RETURNS void at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new MethodReturnVoid(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    className,
                    methodName,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static void returnStat(String methodName, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("RETURN with target ", methodName, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newStatementEvent(new ReturnStat(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    methodName,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static void implicitReturn(String methodName, String filename, int line, int col) {
        if (loggingEnabled) {
            disableLogging();
            log(methodName, " EXITS at ", formatPosition(filename, line, col));
            newNonStatementEvent(new ImplicitReturn(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    methodName,
                    filename,
                    line,
                    col
            ));
            enableLogging();
        }
    }

    public static void breakStat(String targetDescr, int targetLine, int targetCol,
                                 String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("BREAK with target ", targetDescr, " (", formatPosition(filename, targetLine, targetCol), ") at ",
                    formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newStatementEvent(new BreakStat(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    targetDescr,
                    targetLine,
                    targetCol,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static void continueStat(String targetDescr, int targetLine, int targetCol,
                                    String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("CONTINUE with target ", targetDescr, " (", formatPosition(filename, targetLine, targetCol), ") at ",
                    formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newStatementEvent(new ContinueStat(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    targetDescr,
                    targetLine,
                    targetCol,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static void yieldStat(Object yieldedVal, String targetDescr, int targetLine, int targetCol,
                                 String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("YIELD '", yieldedVal, "' with target ", targetDescr, " (", formatPosition(filename, targetLine, targetCol), ") at ",
                    formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newStatementEvent(new YieldStat(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    Value.valueFor(yieldedVal),
                    targetDescr,
                    targetLine,
                    targetCol,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static @Specialize Object switchConstruct(@Specialize Object selector, boolean isExpr, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            var switchTypeDescr = isExpr ? " (switch expression)" : " (switch statement)";
            log("SWITCH selector='", selector, "' at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol), switchTypeDescr);
            if (isExpr){
                newNonStatementEvent(new SwitchExpr(
                        genEventId(),
                        currentEnclosingStatementEventId(),
                        Value.valueFor(selector),
                        filename,
                        startLine,
                        startCol,
                        endLine,
                        endCol
                ));
            } else {
                newStatementEvent(new SwitchStat(
                        genEventId(),
                        currentEnclosingCallEventId(),
                        Value.valueFor(selector),
                        filename,
                        startLine,
                        startCol,
                        endLine,
                        endCol
                ));
            }
            enableLogging();
        }
        return selector;
    }

    public static void loopEnter(String loopType, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("ENTER LOOP (", loopType, ") at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new LoopEnter(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    loopType,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static void loopExit(String loopType, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("EXIT LOOP (", loopType, ") at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new LoopExit(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    loopType,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static boolean loopCond(boolean evalRes, String loopType, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("LOOP CONDITION evaluates to '", evalRes, "' at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new LoopCond(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(evalRes),
                    loopType,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return evalRes;
    }

    public static void foreachLoopNextIter(@Specialize Object newElem, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("NEXT ITER elem='", newElem, "' at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new ForEachLoopNextIter(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(newElem),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static boolean ifCond(boolean evalRes, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("IF CONDITION evaluates to '", evalRes, "' at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new IfCond(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(evalRes),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return evalRes;
    }

    public static @Specialize Object localVarAssignment(String varName, @Specialize Object assignedValue,
                                                        String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("VAR ASSIGN ", varName, " = ", assignedValue, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new LocalVarAssignment(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    varName,
                    Value.valueFor(assignedValue),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return assignedValue;
    }

    public static void localVarAssignOp(String varName, @Specialize Object newValue,
                                        @Specialize Object oldValue, String operator, @Specialize Object rhs,
                                        String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("VAR UPDATE ", varName, " ", operator, "= ", rhs, " : ", oldValue, " -> ", newValue,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new LocalVarAssignOp(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    varName,
                    Value.valueFor(newValue),
                    Value.valueFor(oldValue),
                    operator,
                    Value.valueFor(rhs),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static @Specialize(numericOnly = true) int localVarIncDecOp(
            String varName, @Specialize(numericOnly = true) int result,
            boolean isPrefixOp, boolean isIncOp,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        if (loggingEnabled) {
            disableLogging();
            var newValue = isPrefixOp ? result :
                    isIncOp ? (result + 1) : (result - 1);
            var oldValue = isIncOp ? (newValue - 1) : (newValue + 1);
            log("VAR ", preOrPost(isPrefixOp), "-", incOrDec(isIncOp), " ", varName, " : ",
                    oldValue, " -> ", newValue, " with result ", result,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new LocalVarIncDecOp(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    varName,
                    Value.valueFor(result),
                    Value.valueFor(newValue),
                    Value.valueFor(oldValue),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return result;
    }

    public static @Specialize Object staticFieldAssignment(String className, String fieldName, @Specialize Object assignedValue,
                                                           String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("STATIC FIELD ASSIGN ", className, ".", fieldName, " = ", assignedValue,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new StaticFieldAssignment(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    className,
                    fieldName,
                    Value.valueFor(assignedValue),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return assignedValue;
    }

    public static void staticFieldAssignOp(String className, String fieldName, @Specialize Object newValue,
                                           @Specialize Object oldValue, String operator, @Specialize Object rhs,
                                           String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("STATIC FIELD UPDATE ", className, ".", fieldName, " ", operator, "= ", rhs, " : ", oldValue, " -> ", newValue,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new StaticFieldAssignOp(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    className,
                    fieldName,
                    Value.valueFor(newValue),
                    Value.valueFor(oldValue),
                    operator,
                    Value.valueFor(rhs),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static @Specialize(numericOnly = true) int staticFieldIncDecOp(
            String className, String fieldName, @Specialize(numericOnly = true) int result,
            boolean isPrefixOp, boolean isIncOp,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        if (loggingEnabled) {
            disableLogging();
            var newValue = isPrefixOp ? result :
                    isIncOp ? (result + 1) : (result - 1);
            var oldValue = isIncOp ? (newValue - 1) : (newValue + 1);
            log("STATIC FIELD ", preOrPost(isPrefixOp), "-", incOrDec(isIncOp), " ", className, ".", fieldName,
                    " : ", oldValue, " -> ", newValue, " with result ", result,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new StaticFieldIncDecOp(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    className,
                    fieldName,
                    Value.valueFor(result),
                    Value.valueFor(newValue),
                    Value.valueFor(oldValue),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return result;
    }

    public static @Specialize Object instanceFieldAssignment(String className, Object instance, String fieldName, @Specialize Object assignedValue,
                                                             String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("INSTANCE FIELD ASSIGN ", instance, ".", className, "::", fieldName, " = ", assignedValue,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new InstanceFieldAssignment(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    className,
                    Value.valueFor(instance),
                    fieldName,
                    Value.valueFor(assignedValue),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return assignedValue;
    }

    public static void instanceFieldAssignOp(String className, Object instance, String fieldName, @Specialize Object newValue,
                                             @Specialize Object oldValue, String operator, @Specialize Object rhs,
                                             String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("INSTANCE FIELD UPDATE ", instance, ".", className, "::", fieldName, " ", operator, "= ", rhs, " : ",
                    oldValue, " -> ", newValue, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new InstanceFieldAssignOp(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    className,
                    Value.valueFor(instance),
                    fieldName,
                    Value.valueFor(newValue),
                    Value.valueFor(oldValue),
                    operator,
                    Value.valueFor(rhs),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static @Specialize(numericOnly = true) int instanceFieldIncDecOp(
            String className, Object instance, String fieldName, @Specialize(numericOnly = true) int result,
            boolean isPrefixOp, boolean isIncOp,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        if (loggingEnabled) {
            disableLogging();
            var newValue = isPrefixOp ? result :
                    isIncOp ? (result + 1) : (result - 1);
            var oldValue = isIncOp ? (newValue - 1) : (newValue + 1);
            log("INSTANCE FIELD ", preOrPost(isPrefixOp), "-", incOrDec(isIncOp), " ", className, "::", fieldName,
                    " : ", oldValue, " -> ", newValue, " with result ", result,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new InstanceFieldIncDecOp(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    className,
                    Value.valueFor(instance),
                    fieldName,
                    Value.valueFor(result),
                    Value.valueFor(newValue),
                    Value.valueFor(oldValue),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return result;
    }

    public static void arrayElemSet(Object array, int index, @Specialize Object assignedValue,
                                    String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("ARRAY SET ", array, "[", index, "] = ", assignedValue,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new ArrayElemSet(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(array),
                    Value.valueFor(index),
                    Value.valueFor(assignedValue),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static void arrayElemAssignOp(Object array, int index, @Specialize Object newValue,
                                         @Specialize Object oldValue, String operator, @Specialize Object rhs,
                                         String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("ARRAY UPDATE ", array, "[", index, "] ", operator, "= ", rhs, " : ", oldValue, " -> ", newValue,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new ArrayElemAssignOp(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(array),
                    Value.valueFor(index),
                    Value.valueFor(newValue),
                    Value.valueFor(oldValue),
                    operator,
                    Value.valueFor(rhs),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static @Specialize(numericOnly = true) int arrayElemIncDecOp(
            Object array, int index, @Specialize(numericOnly = true) int result,
            boolean isPrefixOp, boolean isIncOp,
            String filename, int startLine, int startCol, int endLine, int endCol
    ) {
        if (loggingEnabled) {
            disableLogging();
            var newValue = isPrefixOp ? result :
                    isIncOp ? (result + 1) : (result - 1);
            var oldValue = isIncOp ? (newValue - 1) : (newValue + 1);
            log("ARRAY ", preOrPost(isPrefixOp), "-", incOrDec(isIncOp), " ", array, "[", index, "] ",
                    " : ", oldValue, " -> ", newValue, " with result ", result,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new ArrayElemIncDecOp(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(array),
                    Value.valueFor(index),
                    Value.valueFor(result),
                    Value.valueFor(newValue),
                    Value.valueFor(oldValue),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return result;
    }

    public static void variableDeclared(String varName, String typeDescr,
                                        String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("VAR DECLARED: ", varName, " (of static type ", typeDescr, ") at ",
                    formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newStatementEvent(new VarDeclStat(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    varName,
                    typeDescr,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static void caught(Throwable throwable, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("CATCH ", throwable, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newStatementEvent(new Caught(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    Value.valueFor(throwable),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static @Specialize Object castAttempt(@Specialize Object value, String targetTypeDescr, boolean willSucceed,
                                                 String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            var resultMsg = willSucceed ? "SUCCEEDED" : "FAILED";
            log("CAST ", value, " to type ", targetTypeDescr, " ", resultMsg, " at ",
                    formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new CastAttempt(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(value),
                    targetTypeDescr,
                    willSucceed,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return value;
    }

    public static Throwable throwStat(Throwable throwable, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("THROW ", throwable, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newStatementEvent(new ThrowStat(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    Value.valueFor(throwable),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return throwable;
    }

    public static boolean assertionStat(boolean asserted, String assertionDescr,
                                        String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            var successOrFailDescr = asserted ? " SUCCEEDS" : " FAILS";
            log("ASSERTION ", assertionDescr, successOrFailDescr,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newStatementEvent(new AssertionStat(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    Value.valueFor(asserted),
                    assertionDescr,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return asserted;
    }

    public static @Specialize Object unaryOp(@Specialize Object res, @Specialize Object arg, String operator,
                                             String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("UNARY ", operator, " ", arg, " = ", res,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new UnaryOp(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(res),
                    Value.valueFor(arg),
                    operator,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return res;
    }

    public static void binaryOperator(Object lhs, Object rhs, String operator, Object result,
                                      String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("BINARY ", lhs, " ", operator, " ", rhs, " = ", result,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new BinaryOp(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(lhs),
                    Value.valueFor(rhs),
                    operator,
                    Value.valueFor(result),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    public static @Specialize Object localVarRead(@Specialize Object value, String varName,
                                                  String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("VAR READ ", varName, " : ", value, " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new LocalVarRead(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(value),
                    varName,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return value;
    }

    public static @Specialize Object staticFieldRead(@Specialize Object value, String className, String fieldName,
                                                     String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("STATIC FIELD READ ", className, ".", fieldName, " : ", value,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new StaticFieldRead(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(value),
                    className,
                    fieldName,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return value;
    }

    public static @Specialize Object instanceFieldRead(@Specialize Object value, Object owner, String className, String fieldName,
                                                       String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("INSTANCE FIELD READ ", owner, ".", className, "::", fieldName, " : ", value,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new InstanceFieldRead(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(value),
                    Value.valueFor(owner),
                    className,
                    fieldName,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return value;
    }

    public static @Specialize Object arrayAccess(@Specialize Object value, Object array, int index,
                                                 String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("ARRAY ACCESS ", array, "[", index, "] : ", value,
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new ArrayAccess(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(value),
                    Value.valueFor(array),
                    Value.valueFor(index),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return value;
    }

    public static boolean ternaryCondition(boolean cond, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("TERNARY CONDITION evaluates to '", cond, "' at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newNonStatementEvent(new TernaryCondition(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(cond),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return cond;
    }

    public static boolean typeTest(boolean result, Object testedObject, String targetTypeName, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            if (testedObject == null){
                log("TYPE TEST null is not of type ", targetTypeName,
                        " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            } else {
                var actualType = testedObject.getClass().getTypeName();
                var possiblyNegate = result ? "" : "not ";
                log("TYPE TEST ", actualType, " is ", possiblyNegate, "of type ", targetTypeName,
                        " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            }
            newNonStatementEvent(new TypeTest(
                    genEventId(),
                    currentEnclosingStatementEventId(),
                    Value.valueFor(result),
                    Value.valueFor(testedObject),
                    targetTypeName,
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return result;
    }

    public static void exec(String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("EXEC STAT at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            newStatementEvent(new Exec(
                    genEventId(),
                    currentEnclosingCallEventId(),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
    }

    private static String preOrPost(boolean isPre) {
        return isPre ? "PRE" : "POST";
    }

    private static String incOrDec(boolean isInc) {
        return isInc ? "INCREMENT" : "DECREMENT";
    }

    private static Value[] makeArgsValuesArray(Object[] obj){
        var values = new Value[obj.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Value.valueFor(obj);
        }
        return values;
    }

    private static String formatPositionInterval(String filename, int startLine, int startCol, int endLine, int endCol) {
        if (endLine == NO_POS) {
            return formatPosition(simplifyFilename(filename), startLine, startCol);
        } else {
            return String.format("%s [%d:%d,%d:%d]", simplifyFilename(filename), startLine, startCol, endLine, endCol);
        }
    }

    private static String formatPosition(String filename, int line, int col) {
        var lineStr = (line == NO_POS) ? "?" : String.valueOf(line);
        var colStr = (col == NO_POS) ? "?" : String.valueOf(col);
        return String.format("%s:%s:%s", simplifyFilename(filename), lineStr, colStr);
    }

    private static String simplifyFilename(String filename) {
        var idx = filename.length()-1;
        while (idx >= 0 && filename.charAt(idx) != '/'){
            idx--;
        }
        return filename.substring(idx);
    }

}

