package com.epfl.systemf.jumbotrace.injected.raw;

import com.epfl.systemf.jumbotrace.Config;
import com.epfl.systemf.jumbotrace.events.Event;
import com.epfl.systemf.jumbotrace.events.NonStatementEvent.*;
import com.epfl.systemf.jumbotrace.events.StatementEvent;
import com.epfl.systemf.jumbotrace.events.StatementEvent.*;
import com.epfl.systemf.jumbotrace.events.Value;
import com.epfl.systemf.jumbotrace.injected.annot.Specialize;
import com.epfl.systemf.jumbotrace.util.ReversedArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private static final class Frame {
        String className;
        String methodName;
        String signature;
        int depth;
        Long callId;
        Long enterId;
        long parentId;
        boolean enterIsInstrumented;
        Long currentStat;

        public Frame(@NotNull String className, @NotNull String methodName, @Nullable String signature,
                     int depth,
                     @Nullable Long callId, @Nullable Long enterId, long parentId, boolean enterIsInstrumented,
                     @Nullable Long currentStat) {
            this.className = className;
            this.methodName = methodName;
            this.signature = signature;
            this.depth = depth;
            this.callId = callId;
            this.enterId = enterId;
            this.parentId = parentId;
            this.enterIsInstrumented = enterIsInstrumented;
            this.currentStat = currentStat;
        }

        @Override
        public String toString() {
            return "Frame{" +
                    "className='" + className + '\'' +
                    ", methodName='" + methodName + '\'' +
                    ", signature='" + signature + '\'' +
                    ", depth=" + depth +
                    ", callId=" + callId +
                    ", enterId=" + enterId +
                    ", parentId=" + parentId +
                    ", enterIsInstrumented=" + enterIsInstrumented +
                    ", currentStat=" + currentStat +
                    '}';
        }
    }

    private static final Deque<Frame> stack = new LinkedList<>();
    private static Frame pendingCall = null;

    private static void updateStackForCall(MethodCallEvent event) {
        pendingCall = new Frame(
                event.className(), event.methodName(), event.methodSig(),
                stack.size() + 1,
                event.id(), null, event.parentId(), false,null);
    }

    /**
     * WARNING this method is sensitive to where it is called from
     */
    private static void updateStackForEnter(InstrumentedMethodEnter event) {
        if (stack.isEmpty()){   // happens during initial call to main
            pendingCall = new Frame(event.className(), event.methodName(), event.methodSig(),
                    0, Config.NO_PARENT_EVENT_CODE, null, event.parentId(), false, null);
        }
        var stackTrace = new ReversedArray<>((new Exception()).getStackTrace());
        var interestingStackTraceLength = stackTrace.length() - 2;  // subtract nesting depth inside this class
//        System.out.println("Stack:");   // TODO remove
//        System.out.println(stack);
//        System.out.println("Stacktrace:");
//        System.out.println(stackTrace);
//        System.out.println("----------------------------");
        if (stack.size() + 1 == interestingStackTraceLength) {
            // called from an instrumented location, no layer missing
            checkAssertion(pendingCall != null);
            var frame = pendingCall;
            pendingCall = null;
            frame.enterId = event.id();
            frame.enterIsInstrumented = true;
            stack.addFirst(frame);
        } else {  // called from a non-instrumented location, so at least 1 layer is missing
            // FIRST handle the last method called from an instrumented location
            var lastInstrumentedCallFrame = pendingCall;
            pendingCall = null;
            var firstNonInstrEnterStackTraceElem = stackTrace.get(stack.size());
            checkAssertion(lastInstrumentedCallFrame.methodName.equals(firstNonInstrEnterStackTraceElem.getMethodName()));
            log("[non-instrumented] ENTER ", firstNonInstrEnterStackTraceElem.getClassName(), ".",
                    firstNonInstrEnterStackTraceElem.getMethodName());
            var firstNonInstrEnterEvent = new NonInstrumentedMethodEnter(
                    genEventId(), lastInstrumentedCallFrame.callId,
                    firstNonInstrEnterStackTraceElem.getClassName(), firstNonInstrEnterStackTraceElem.getMethodName(),
                    firstNonInstrEnterStackTraceElem.getFileName()
            );
            writeEvent(firstNonInstrEnterEvent);
            lastInstrumentedCallFrame.enterId = firstNonInstrEnterEvent.id();
            lastInstrumentedCallFrame.enterIsInstrumented = false;
            stack.addFirst(lastInstrumentedCallFrame);
            // THEN handle the intermediate enter events (the ones for which neither the call nor the enter are instrumented)
            var parentId = firstNonInstrEnterEvent.id();
            for (var depth = stack.size(); depth < interestingStackTraceLength-1; depth++){
                var stackTraceElem = stackTrace.get(depth);
                var className = stackTraceElem.getClassName();
                var methodName = stackTraceElem.getMethodName();
                var filename = stackTraceElem.getFileName();
                log("[non-instrumented] ENTER ", className, ".", methodName);
                var methEnter = new NonInstrumentedMethodEnter(genEventId(), parentId, className, methodName, filename);
                writeEvent(methEnter);
                stack.addFirst(new Frame(className, methodName, null, depth, null, methEnter.id(),
                        parentId, false, null));
                parentId = methEnter.id();
            }
            // FINALLY handle the "come-back" to an instrumented location
            var stackTraceElem = stackTrace.get(interestingStackTraceLength-1);
            checkAssertion(event.methodName().equals(stackTraceElem.getMethodName()));
            var firstReInstrCallFrame = new Frame(event.className(), event.methodName(), event.methodSig(),
                    interestingStackTraceLength, null, event.id(), parentId, true, null);
            stack.addFirst(firstReInstrCallFrame);
        }
    }

    private static void updateStackForMethodExit(){
        Frame frame = stack.removeFirst();
        while (frame.callId == null){
            log("[non-instrumented] EXIT ", frame.methodName);
            writeEvent(new NonInstrumentedMethodExit(
                    genEventId(),
                    frame.parentId,
                    frame.methodName
            ));
            frame = stack.removeFirst();
        }
    }

    private static void updateStackForStatement(StatementEvent event) {
        stack.getFirst().currentStat = event.id();
    }

    private static long getEnclosingCallId(){
        return stack.isEmpty() ? Config.NO_PARENT_EVENT_CODE : stack.getFirst().callId;
    }

    private static long getEnclosingEnterId(){
        return stack.getFirst().enterId;
    }

    private static long getEnclosingStatIdOrElseEnter(){
        // stat can be null in a call to super-constructor (or to other constructor of same class)
        Long stat = stack.getFirst().currentStat;
        return stat == null ? getEnclosingEnterId() : stat;
    }

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
            if (event instanceof StatementEvent statementEvent){
                updateStackForStatement(statementEvent);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void staticMethodCall(String className, String methodName, String methodSig, Object[] args,
                                        String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("CALL: ", className, ".", methodName, methodSig, " args=", Arrays.toString(args),
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            var event = new StaticMethodCall(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
                    className,
                    methodName,
                    methodSig,
                    makeArgsValuesArray(args),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            );
            writeEvent(event);
            updateStackForCall(event);
            enableLogging();
        }
    }

    public static void nonStaticMethodCall(String className, String methodName, String methodSig, Object receiver, Object[] args,
                                           String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("CALL: ", className, ".", methodName, methodSig, " receiver='", receiver, "' args=", Arrays.toString(args),
                    " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            var event = new NonStaticMethodCall(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            );
            writeEvent(event);
            updateStackForCall(event);
            enableLogging();
        }
    }

    public static void methodEnter(String className, String methodName, String methodSig, String filename, int line, int col) {
        if (loggingEnabled) {
            disableLogging();
            indent += 1;
            log("ENTER: ", className, ".", methodName, methodSig, " at ", formatPosition(filename, line, col));
            // TODO check stacktrace using an exception
            var event = new InstrumentedMethodEnter(
                    genEventId(),
                    getEnclosingCallId(),
                    className,
                    methodName,
                    methodSig,
                    filename,
                    line,
                    col
            );
            writeEvent(event);
            updateStackForEnter(event);
            enableLogging();
        }
    }

    public static void methodExit(String methodName, String filename, int line, int col) {
        if (loggingEnabled) {
            disableLogging();
            updateStackForMethodExit();
            log("METHOD EXIT ", methodName, " at ", formatPosition(filename, line, col));
            writeEvent(new InstrumentedMethodExit(
                    genEventId(),
                    getEnclosingCallId(),
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
            writeEvent(new MethodReturnVal(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new MethodReturnVoid(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new ReturnStat(
                    genEventId(),
                    getEnclosingEnterId(),
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
            writeEvent(new ImplicitReturn(
                    genEventId(),
                    getEnclosingEnterId(),
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
            writeEvent(new BreakStat(
                    genEventId(),
                    getEnclosingEnterId(),
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
            writeEvent(new ContinueStat(
                    genEventId(),
                    getEnclosingEnterId(),
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
            writeEvent(new YieldStat(
                    genEventId(),
                    getEnclosingEnterId(),
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
            writeEvent(new SwitchExpr(
                    genEventId(),
                    isExpr ? getEnclosingStatIdOrElseEnter() : getEnclosingEnterId(),
                    Value.valueFor(selector),
                    filename,
                    startLine,
                    startCol,
                    endLine,
                    endCol
            ));
            enableLogging();
        }
        return selector;
    }

    public static void loopEnter(String loopType, String filename, int startLine, int startCol, int endLine, int endCol) {
        if (loggingEnabled) {
            disableLogging();
            log("ENTER LOOP (", loopType, ") at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            writeEvent(new LoopEnter(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new LoopExit(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new LoopCond(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new ForEachLoopNextIter(
                    genEventId(),
                    getEnclosingEnterId(),
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
            writeEvent(new IfCond(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new LocalVarAssignment(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new LocalVarAssignOp(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new LocalVarIncDecOp(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new StaticFieldAssignment(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new StaticFieldAssignOp(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new StaticFieldIncDecOp(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new InstanceFieldAssignment(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new InstanceFieldAssignOp(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new InstanceFieldIncDecOp(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new ArrayElemSet(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new ArrayElemAssignOp(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new ArrayElemIncDecOp(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new VarDeclStat(
                    genEventId(),
                    getEnclosingEnterId(),
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
            writeEvent(new Caught(
                    genEventId(),
                    getEnclosingEnterId(),
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
            writeEvent(new CastAttempt(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new ThrowStat(
                    genEventId(),
                    getEnclosingEnterId(),
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
            writeEvent(new AssertionStat(
                    genEventId(),
                    getEnclosingEnterId(),
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
            writeEvent(new UnaryOp(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new BinaryOp(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new LocalVarRead(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new StaticFieldRead(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new InstanceFieldRead(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new ArrayAccess(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new TernaryCondition(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            if (testedObject == null) {
                log("TYPE TEST null is not of type ", targetTypeName,
                        " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            } else {
                var actualType = testedObject.getClass().getTypeName();
                var possiblyNegate = result ? "" : "not ";
                log("TYPE TEST ", actualType, " is ", possiblyNegate, "of type ", targetTypeName,
                        " at ", formatPositionInterval(filename, startLine, startCol, endLine, endCol));
            }
            writeEvent(new TypeTest(
                    genEventId(),
                    getEnclosingStatIdOrElseEnter(),
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
            writeEvent(new Exec(
                    genEventId(),
                    getEnclosingEnterId(),
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

    private static Value[] makeArgsValuesArray(Object[] obj) {
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
        var idx = filename.length() - 1;
        while (idx >= 0 && filename.charAt(idx) != '/') {
            idx--;
        }
        return filename.substring(idx);
    }

    // TODO disable once tested
    private static void checkAssertion(boolean assertion){
        if (!assertion){
            throw new AssertionError();
        }
    }

}

