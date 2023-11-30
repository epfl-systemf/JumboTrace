package com.epf.systemf.jumbotrace;

import com.github.javaparser.StaticJavaParser;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.javaparser.ParserConfiguration.*;

public final class Main {

    private static final String SRC_DIR_NAME = "raw";
    private static final String TARGET_DIR_NAME = "processed";
    private static final LanguageLevel LANGUAGE_LEVEL = LanguageLevel.JAVA_17;


    public static void main(String[] filenames) throws IOException {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(LANGUAGE_LEVEL);
        for (var filename : filenames) {
            var rootPath = Paths.get(".");
            var codePath = rootPath.resolve(SRC_DIR_NAME).resolve(filename);
            var targetDir = rootPath.resolve(TARGET_DIR_NAME);
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
