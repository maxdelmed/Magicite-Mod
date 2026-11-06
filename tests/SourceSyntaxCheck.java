package tests;

import com.sun.source.util.JavacTask;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public final class SourceSyntaxCheck {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK required");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            List<File> files;
            try (Stream<Path> paths = Files.walk(Path.of("src/main/java"))) {
                files = paths.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).toList();
            }
            JavacTask parser = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("--release", "17", "-proc:none"), null, manager.getJavaFileObjectsFromFiles(files));
            parser.parse();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    throw new AssertionError(diagnostic.toString());
                }
            }
            System.out.println("Java syntax: " + files.size() + " files PASS");
        }
    }
}
