package com.epfl.systemf.jumbotrace.javacplugin;

import sun.misc.Unsafe;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/*
* This file is strongly inspired and partially copied from
* https://github.com/Auties00/Optionless/blob/master/src/main/java/it/auties/optional/util/IllegalReflection.java,
* by Alessandro Autiero
*/

public final class DangerousBlackMagicReflection {

    private DangerousBlackMagicReflection(){
        throw new UnsupportedOperationException();
    }

    static void openJavacUnsafe(){
        try {
            var theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            var unsafe = (Unsafe) theUnsafeField.get(null);
            var jdkCompilerModule = ModuleLayer.boot().findModule("jdk.compiler").orElseThrow();
            var addOpensMethod = Module.class.getDeclaredMethod("implAddOpens", String.class, Module.class);
            var addOpensMethodOffset = unsafe.objectFieldOffset(ModulePlaceholder.class.getDeclaredField("first"));
            unsafe.putBooleanVolatile(addOpensMethod, addOpensMethodOffset, true);
            Arrays.stream(Package.getPackages())
                    .map(Package::getName)
                    .filter(pack -> pack.startsWith("com.sun.tools.javac"))
                    .forEach(pack -> {
                        try {
                            addOpensMethod.invoke(jdkCompilerModule, pack, DangerousBlackMagicReflection.class.getModule());
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("all")
    private static class ModulePlaceholder {
        boolean first;
        static final Object staticObj = OutputStream.class;
        volatile Object second;
        private static volatile boolean staticSecond;
        private static volatile boolean staticThird;
    }

}
