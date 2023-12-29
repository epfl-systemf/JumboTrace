package com.epfl.systemf.jumbotrace;

// TODO rename com.epfl into ch.epfl everywhere

import java.lang.instrument.Instrumentation;

public final class InstrumentationAgent {

    public static void premain(@SuppressWarnings("unused") String agentArgs, Instrumentation inst){
        inst.addTransformer(new InstrumentationTransformer());
    }

}
