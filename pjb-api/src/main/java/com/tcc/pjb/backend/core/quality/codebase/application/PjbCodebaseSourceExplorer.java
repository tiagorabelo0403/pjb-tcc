package com.tcc.pjb.backend.core.quality.codebase.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class PjbCodebaseSourceExplorer {

    private static final Pattern IMPORT = Pattern.compile("^import\\s+([\\w.]+);", Pattern.MULTILINE);

    List<Path> listJavaFiles(Path root) {
        ArrayList<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(files::add);
        } catch (IOException ignored) {
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

    Set<String> importedSlices(String source, String basePackage) {
        LinkedHashSet<String> slices = new LinkedHashSet<>();
        Matcher matcher = IMPORT.matcher(source);
        while (matcher.find()) {
            String imported = Objects.toString(matcher.group(1), "").trim();
            if (!imported.startsWith(basePackage)) {
                continue;
            }
            String tail = imported.substring(basePackage.length());
            String[] parts = tail.split("\\.");
            if (parts.length < 2) {
                continue;
            }
            slices.add(parts[0] + '/' + parts[1]);
        }
        return Set.copyOf(slices);
    }

    String sliceFromRelative(Path relative) {
        if (relative.getNameCount() == 0) {
            return "";
        }
        if (relative.getNameCount() == 1) {
            return relative.getName(0).toString();
        }
        return relative.getName(0) + "/" + relative.getName(1);
    }

    String laneFromRelative(Path relative) {
        if (relative.getNameCount() < 3) {
            return "canonico";
        }
        return Objects.toString(relative.getName(2), "canonico").trim();
    }
}
