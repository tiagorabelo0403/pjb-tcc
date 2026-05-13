package com.tcc.pjb.backend.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class ArquiteturaSourceScanSupport {

    private ArquiteturaSourceScanSupport() {
    }

    static Path detectRoot() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (Files.exists(cwd.resolve("pom.xml")) && Files.exists(cwd.resolve("src"))) {
            return cwd;
        }
        if (Files.exists(cwd.resolve("pom.xml")) && Files.exists(cwd.resolve("pjb-api"))) {
            return cwd;
        }
        Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("pom.xml")) && Files.exists(parent.resolve("pjb-api"))) {
            return parent;
        }
        throw new IllegalStateException("Repository root not found from " + cwd);
    }

    static Path moduleSourceRoot(String... segments) {
        Path root = detectRoot();
        Path moduleRoot = Files.exists(root.resolve("pjb-api")) ? root.resolve("pjb-api") : root;
        Path out = moduleRoot;
        for (String segment : segments) {
            out = out.resolve(segment);
        }
        return out;
    }

    static List<Path> javaFiles(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !"package-info.java".equals(path.getFileName().toString()))
                    .sorted()
                    .toList();
        }
    }

    static List<Path> scanJavaSourcesUnder(Path root) throws IOException {
        return javaFiles(root);
    }

    static Map<String, List<String>> duplicateFileNames(Path root) throws IOException {
        return duplicateBy(root, path -> path.getFileName().toString(), path -> unixPath(root.relativize(path)));
    }

    static Map<String, List<String>> duplicateSimpleClassNames(Path root) throws IOException {
        return duplicateBy(root,
                path -> stripJavaExtension(path.getFileName().toString()),
                path -> unixPath(root.relativize(path)));
    }

    static Map<String, List<String>> duplicateSimpleClassNames(Path root, Predicate<Path> filter) throws IOException {
        return duplicateBy(root,
                path -> stripJavaExtension(path.getFileName().toString()),
                path -> unixPath(root.relativize(path)),
                filter);
    }

    private static Map<String, List<String>> duplicateBy(Path root,
                                                         java.util.function.Function<Path, String> keyMapper,
                                                         java.util.function.Function<Path, String> valueMapper) throws IOException {
        return duplicateBy(root, keyMapper, valueMapper, path -> true);
    }

    private static Map<String, List<String>> duplicateBy(Path root,
                                                         java.util.function.Function<Path, String> keyMapper,
                                                         java.util.function.Function<Path, String> valueMapper,
                                                         Predicate<Path> filter) throws IOException {
        Map<String, List<String>> grouped = javaFiles(root).stream()
                .filter(filter)
                .collect(Collectors.groupingBy(
                        keyMapper,
                        LinkedHashMap::new,
                        Collectors.mapping(valueMapper, Collectors.toList())
                ));

        LinkedHashMap<String, List<String>> duplicates = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            List<String> values = entry.getValue().stream().sorted().toList();
            if (values.size() > 1) {
                duplicates.put(entry.getKey(), values);
            }
        }
        return Map.copyOf(duplicates.isEmpty() ? Map.of() : duplicates);
    }

    static String unixPath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String stripJavaExtension(String fileName) {
        return fileName.endsWith(".java")
                ? fileName.substring(0, fileName.length() - 5)
                : fileName;
    }
}
