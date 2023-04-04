# JavaTracer

Tracing debugger for the Java language, using the [Java debug interface](https://docs.oracle.com/javase/7/docs/jdk/api/jpda/jdi/)

Usage:

- First argument is the name of the main class (i.e. the class containing the `main` method)
- Second argument is the name of the main file (i.e. the file containing the `main` method)
- Subsequent arguments are the names of other source files, if any

E.g. to run the example program whose source and class files are located in the [`examples`](./examples) folder, program arguments are:
```
Debuggee Debuggee.java Fib.java Algo.java
```
(`Fib.java` and `Algo.java` may be swapped, but not the others)

All the `.class` files needed to execute the program are expected to be present in the working directory.

Each time a line is executed, it is displayed by the debugger <b>only if the compilation of the source file containing that line included the generation of debug information 
and the source file has been given as an argument to the tracer</b>.
