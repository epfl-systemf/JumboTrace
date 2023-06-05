# JumboTrace 🐘

A tracer for Java, to record the evolution of the state of a Java program during its execution and use it to build a trace
(sequence of events and lines executed in the program).


## Warning/disclaimer

This is experimental software. While it has been successfully tested on a few examples, there is no guarantee about the
absence of bugs.


## Important general observations

- All the class files provided as input to the instrumenter must be compiler with the debug flag (`javac -g`). Otherwise
the instrumeter will not find enough information in the program to work properly).
- The program for which we want to generate a trace should never exit by calling `System.exit`, otherwise the instructions
that write the JSON trace will not be executed.
- The `toString` method of the objects manipulated by the program should not have side effects. If they do, the behaviour
of the program may be modified by the calls to `toString` performed by the instrumented bytecode.
- The program should not contain a class named `___JumboTracer___`.


## Setup

In order to be able to run all the functionalities of this repository, you need:
 - Java (both `javac` and `java`, https://www.oracle.com/java/technologies/downloads/)
 - sbt (https://www.scala-sbt.org/1.x/docs/Setup.html)
 - make (Installation instructions: [Ubuntu](https://linuxhint.com/install-make-ubuntu/), [Windows](https://www.technewstoday.com/install-and-use-make-in-windows/))
 - Python (https://www.python.org/downloads/)
 - gcc (Installation instructions: [Ubuntu](https://linuxconfig.org/how-to-install-gcc-the-c-compiler-on-ubuntu-22-04-lts-jammy-jellyfish-linux), [Windows](https://dev.to/gamegods3/how-to-install-gcc-in-windows-10-the-easier-way-422j)), or any other tool that is able to expand C macros


## Modules

### Instrumenter

The [instrumenter](./instrumenter) module, written in Scala 3, takes class files as input and outputs copies of these class files, augmented
with instructions that save the events happening during the execution of the program. These instructions call methods of
the [injected](###Injected) directory to perform the recording of the events.

Executable: [Instrumenter](./instrumenter/src/main/scala/Instrumenter.scala)


### Injected

The [injected](./injected) module, written in Java, contains the [`___JumboTracer___`](./injected/___JumboTracer___raw.java) class
which maintains static fields that record the events happening in a Java program. Once compiled, this class must be added
to the instrumented class files of the program being executed, and the instructions added by the instrumentation will
call the methods of `___JumboTracer___` to save the relevant events. The `___JumboTracer___` class also contains methods
to write the recorded events to a JSON file.


### TraceElements

The [traceElements](./traceElements) module, written in Scala 2 (because of issues with the JSON library), contains the
[`TraceElement`](./traceElements/TraceElement.scala) algebraic data type, which represents the events saved by the tracer.
It also contains a parser that handles the JSON file written by the `___JumboTracer___` class.


### DebugCmdlineFrontend

The [DebugCmdlineFrontend](./debugCmdlineFrontend) module, written in Scala 3, is a very simple formatter for a trace,
mostly dedicated to testing and debugging.

Executable: [DebugCmdlineFrontend](./debugCmdlineFrontend/src/main/scala/DebugCmdlineFrontend.scala)


### JavaHtmlFrontend

The [JavaHtmlFrontend](./javaHtmlFrontend) module, written in Scala 3, generated an HTML version of the trace of a Java
program. It does so by parsing the JSON trace file and the source files of the Java program.

Executable: [JavaHtmlFrontend](./javaHtmlFrontend/src/main/scala/JavaHtmlFrontend.scala)


### Commander

The [Commander](./commander) module, written in Scala 3, is a very simple command-line program to run the other modules.
Run it with the `help` command to see which commands it supports.

Executable: [JavaCommander](./commander/src/main/scala/JavaCommander.scala)


## Examples

The [examples](./examples) directory contains example programs that can be used to test and demonstrate the program. Note
that one of the programs is in Scala, despite no frontend being available to properly display the traces of a Scala program.


## Intellij support

The [`.run`](./.run) directory contains Intellij run configurations for some modules.

