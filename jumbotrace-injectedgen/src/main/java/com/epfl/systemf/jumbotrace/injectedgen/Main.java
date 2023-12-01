package com.epfl.systemf.jumbotrace.injectedgen;

import com.github.javaparser.StaticJavaParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.javaparser.ParserConfiguration.LanguageLevel;

public final class Main {

    private static final String INJECTION_DIR_NAME = "jumbotrace-injected";
    private static final String SRC_DIR_NAME = "raw";
    private static final String TARGET_DIR_NAME = "processed";
    private static final LanguageLevel LANGUAGE_LEVEL = LanguageLevel.JAVA_17;


    public static void main(String[] filenames) throws IOException {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(LANGUAGE_LEVEL);
        for (var filename : filenames) {
            var injectedDir = Paths.get(new File("").getAbsolutePath()).getParent()
                    .resolve(INJECTION_DIR_NAME)
                    .resolve("src")
                    .resolve("main")
                    .resolve("java")
                    .resolve("com")
                    .resolve("epfl")
                    .resolve("systemf")
                    .resolve("jumbotrace")
                    .resolve("injected");
            var codePath = injectedDir.resolve(SRC_DIR_NAME).resolve(filename);
            var targetDir = injectedDir.resolve(TARGET_DIR_NAME);
            var code = Files.readString(codePath);
            var cu = StaticJavaParser.parse(code);
            cu.accept(new Transformer(), null);
            Files.createDirectories(targetDir);
            try (var writer = new FileWriter(targetDir.resolve(filename).toFile())) {
                writer.write(cu.toString());
            }
        }
    }

}
