package com.tcc.pjb.backend.governance.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TopLevelJavaTypeGovernanceTest {

    private static final Path ROOT = Path.of("");
    private static final Path MAIN_JAVA = ROOT.resolve("src/main/java");
    private static final Path TEST_JAVA = ROOT.resolve("src/test/java");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern TOP_LEVEL_TYPE_PATTERN = Pattern.compile("(?:public\\s+)?(?:final\\s+|sealed\\s+|non-sealed\\s+|abstract\\s+)?(?:class|record|enum|interface|@interface)\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern TOP_LEVEL_PUBLIC_TYPE_PATTERN = Pattern.compile("public\\s+(?:final\\s+|sealed\\s+|non-sealed\\s+|abstract\\s+)?(?:class|record|enum|interface|@interface)\\s+([A-Za-z_][A-Za-z0-9_]*)");

    @Test
    void tiposTopLevelDevemTerPacoteCompativelNomePublicoCompativelEIdentidadeUnica() throws IOException {
        List<String> packageViolations = new ArrayList<>();
        List<String> publicTypeViolations = new ArrayList<>();
        Map<String, List<String>> fqcnToPaths = new LinkedHashMap<>();
        for (Path root : List.of(MAIN_JAVA, TEST_JAVA)) {
            try (Stream<Path> stream = Files.walk(root)) {
                for (Path file : stream.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    String stripped = stripCommentsAndStrings(source);
                    String packageName = packageName(stripped);
                    String expectedPackage = expectedPackage(root, file);
                    if (!expectedPackage.equals(packageName)) {
                        packageViolations.add(file + " -> package=" + packageName + " esperado=" + expectedPackage);
                    }
                    List<String> topLevelTypes = topLevelTypes(stripped);
                    for (String typeName : topLevelTypes) {
                        String fqcn = packageName.isBlank() ? typeName : packageName + "." + typeName;
                        fqcnToPaths.computeIfAbsent(fqcn, ignored -> new ArrayList<>()).add(file.toString());
                    }
                    List<String> publicTypes = topLevelPublicTypes(stripped);
                    if (publicTypes.size() > 1) {
                        publicTypeViolations.add(file + " -> múltiplos tipos públicos top-level " + publicTypes);
                    } else if (publicTypes.size() == 1 && !file.getFileName().toString().equals(publicTypes.get(0) + ".java")) {
                        publicTypeViolations.add(file + " -> tipo público " + publicTypes.get(0) + " não corresponde ao arquivo");
                    }
                }
            }
        }
        List<String> duplicateFqcnViolations = fqcnToPaths.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + " -> " + entry.getValue())
                .toList();
        assertTrue(packageViolations.isEmpty(), () -> "Packages incompatíveis com o root: " + packageViolations);
        assertTrue(publicTypeViolations.isEmpty(), () -> "Tipos públicos top-level inválidos: " + publicTypeViolations);
        assertTrue(duplicateFqcnViolations.isEmpty(), () -> "FQCN duplicado entre main/test: " + duplicateFqcnViolations);
    }

    private static String expectedPackage(Path root, Path file) {
        Path relative = root.relativize(file.getParent());
        String normalized = relative.toString().replace('\\', '.').replace('/', '.');
        return normalized;
    }

    private static String packageName(String source) {
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static List<String> topLevelTypes(String source) {
        return topLevelMatches(source, TOP_LEVEL_TYPE_PATTERN);
    }

    private static List<String> topLevelPublicTypes(String source) {
        return topLevelMatches(source, TOP_LEVEL_PUBLIC_TYPE_PATTERN);
    }

    private static List<String> topLevelMatches(String source, Pattern pattern) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        for (int index = 0; index < source.length(); index++) {
            char ch = source.charAt(index);
            if (ch == '{') {
                depth++;
                continue;
            }
            if (ch == '}') {
                depth = Math.max(0, depth - 1);
                continue;
            }
            if (depth != 0) {
                continue;
            }
            Matcher matcher = pattern.matcher(source);
            matcher.region(index, source.length());
            if (matcher.lookingAt()) {
                out.add(matcher.group(1));
                index = matcher.end() - 1;
            }
        }
        return out;
    }

    private static String stripCommentsAndStrings(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int index = 0;
        while (index < source.length()) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';
            if (current == '/' && next == '/') {
                out.append(' ');
                out.append(' ');
                index += 2;
                while (index < source.length() && source.charAt(index) != '\n') {
                    out.append(' ');
                    index++;
                }
                continue;
            }
            if (current == '/' && next == '*') {
                out.append(' ');
                out.append(' ');
                index += 2;
                while (index < source.length()) {
                    char c = source.charAt(index);
                    char n = index + 1 < source.length() ? source.charAt(index + 1) : '\0';
                    if (c == '*' && n == '/') {
                        out.append(' ');
                        out.append(' ');
                        index += 2;
                        break;
                    }
                    out.append(c == '\n' ? '\n' : ' ');
                    index++;
                }
                continue;
            }
            if (current == '"' || current == '\'') {
                char quote = current;
                out.append(' ');
                index++;
                while (index < source.length()) {
                    char c = source.charAt(index);
                    if (c == '\\') {
                        out.append(' ');
                        if (index + 1 < source.length()) {
                            out.append(' ');
                        }
                        index += 2;
                        continue;
                    }
                    out.append(c == '\n' ? '\n' : ' ');
                    index++;
                    if (c == quote) {
                        break;
                    }
                }
                continue;
            }
            out.append(current);
            index++;
        }
        return out.toString();
    }
}
