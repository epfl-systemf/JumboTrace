package ch.epfl.systemf.jumbotrace.injectedgen;

import com.github.javaparser.StaticJavaParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import static com.github.javaparser.ParserConfiguration.LanguageLevel;

public final class Main {

    private static final String TEST_MODE_MARKER = "#test";
    private static final String INJECTION_DIR_NAME = "jumbotrace-injected";
    private static final String SRC_DIR_NAME = "raw";
    private static final String TARGET_DIR_NAME = "processed";
    private static final LanguageLevel LANGUAGE_LEVEL = LanguageLevel.JAVA_17;


    public static void main(String[] args) throws IOException {
        if (args.length == 0){
            System.err.println("No arguments provided to injected-gen. Exiting.");
            System.exit(-1);
        }
        var testMode = Objects.equals(args[0], TEST_MODE_MARKER);
        var filenames = testMode ?
                Arrays.asList(args).subList(1, args.length) :
                Arrays.asList(args);
        StaticJavaParser.getParserConfiguration().setLanguageLevel(LANGUAGE_LEVEL);
        for (var filename : filenames) {
            var injectedDir = Paths.get(new File("").getAbsolutePath()).getParent()
                    .resolve(INJECTION_DIR_NAME)
                    .resolve("src")
                    .resolve("main")
                    .resolve("java")
                    .resolve("ch")
                    .resolve("epfl")
                    .resolve("systemf")
                    .resolve("jumbotrace")
                    .resolve("injected");
            var codePath = injectedDir.resolve(SRC_DIR_NAME).resolve(filename);
            var targetDir = injectedDir.resolve(TARGET_DIR_NAME);
            var code = Files.readString(codePath);
            var cu = StaticJavaParser.parse(code);
            cu.accept(new Transformer(testMode), null);
            Files.createDirectories(targetDir);
            try (var writer = new FileWriter(targetDir.resolve(filename).toFile())) {
                writer.write(cu.toString());
            }
        }
    }

}
