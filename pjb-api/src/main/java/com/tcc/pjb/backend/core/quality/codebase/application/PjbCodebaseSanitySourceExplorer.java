package com.tcc.pjb.backend.core.quality.codebase.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class PjbCodebaseSanitySourceExplorer {

    private static final Pattern PACKAGE = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    private static final Pattern TYPE = Pattern.compile("\\b(?:public\\s+)?(?:record|class|interface|enum)\\s+(\\w+)");
    private static final Pattern IMPORT = Pattern.compile("^import\\s+([\\w.]+);", Pattern.MULTILINE);
    private static final Pattern REPOSITORY_GENERIC = Pattern.compile("(?:JpaRepository|CrudRepository|PagingAndSortingRepository)\\s*<\\s*(\\w+)\\s*,");

    List<Path> listJavaFiles(List<Path> roots) {
        ArrayList<Path> files = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .forEach(files::add);
            } catch (IOException ignored) {
            }
        }
        return List.copyOf(files);
    }

    String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    String packageName(String source) {
        return match(PACKAGE, source);
    }

    String topLevelType(String source) {
        return match(TYPE, source);
    }

    List<String> imports(String source) {
        ArrayList<String> imports = new ArrayList<>();
        Matcher matcher = IMPORT.matcher(source);
        while (matcher.find()) {
            imports.add(Objects.toString(matcher.group(1), "").trim());
        }
        return List.copyOf(imports);
    }

    String repositoryEntity(String source) {
        Matcher matcher = REPOSITORY_GENERIC.matcher(source);
        return matcher.find() ? Objects.toString(matcher.group(1), "").trim() : "";
    }

    int count(String source, String token) {
        int index = 0;
        int hits = 0;
        while ((index = source.indexOf(token, index)) >= 0) {
            hits++;
            index += token.length();
        }
        return hits;
    }

    String relativize(Path projectRoot, Path file) {
        try {
            return projectRoot.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return file.toString().replace('\\', '/');
        }
    }

    private String match(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? Objects.toString(matcher.group(1), "").trim() : "";
    }
}
