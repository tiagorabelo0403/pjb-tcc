package com.tcc.pjb.backend.governance.source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class SourceGovernanceScanner {

    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+([\\w.]+)\\s*;");
    private static final Pattern ENUM_PATTERN = Pattern.compile("\\benum\\s+(\\w+)\\b");
    private static final Pattern METHOD_LOCAL_PATTERN = Pattern.compile("(?m)^\\s*(?:final\\s+)?([\\w$.<>?]+)\\s+(\\w+)\\s*(?==|;)");
    private static final Pattern PARAM_BLOCK_PATTERN = Pattern.compile("\\(([^)]*)\\)");
    private static final Pattern SWITCH_PATTERN = Pattern.compile("switch\\s*\\(\\s*(\\w+)\\s*\\)\\s*\\{");
    private static final Pattern TOP_LEVEL_TYPE_PATTERN = Pattern.compile("\\s*(?:@\\w+(?:\\([^)]*\\))?\\s*)*(public\\s+)?(?:final\\s+|sealed\\s+|non-sealed\\s+|abstract\\s+)?(@interface|class|record|enum|interface)\\s+(\\w+)");

    private SourceGovernanceScanner() {
    }

    static List<Path> mainJavaFiles() {
        try (Stream<Path> stream = Files.walk(MAIN_JAVA)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao varrer árvore de fontes Java", ex);
        }
    }

    static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler arquivo: " + path, ex);
        }
    }


    static List<String> duplicateOperationalRouteConstants() {
        Path path = Path.of("src/main/java/com/tcc/pjb/backend/core/operational/OperationalApiRoutes.java");
        String source = read(path);
        Pattern pattern = Pattern.compile("public\\s+static\\s+final\\s+String\\s+(\\w+)\\s*=\\s*\\\"([^\\\"]*)\\\";");
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            grouped.computeIfAbsent(matcher.group(2), ignored -> new ArrayList<>()).add(matcher.group(1));
        }
        List<String> offenders = new ArrayList<>();
        grouped.forEach((value, names) -> {
            if (names.size() <= 1) {
                return;
            }
            if (allowedOperationalRouteAlias(value, names)) {
                return;
            }
            offenders.add(value + " -> " + names);
        });
        return offenders;
    }

    private static boolean allowedOperationalRouteAlias(String value, List<String> names) {
        if (names == null || names.size() <= 1) {
            return false;
        }
        if (("".equals(value)
                || "/functions/{functionCode}/challenge".equals(value)
                || "/functions/{functionCode}/password".equals(value)
                || "/functions/{functionCode}/unlock".equals(value))
                && names.stream().allMatch(name -> name.startsWith("PATH_SECRETARIAT_CREDENTIAL_") || name.startsWith("PATH_OFICIAL_JUSTICA_CREDENTIAL_"))) {
            return true;
        }
        return false;
    }

    static List<String> forbiddenSpringShortcutAnnotations() {
        List<String> offenders = new ArrayList<>();
        Pattern pattern = Pattern.compile("@(Get|Set|Post|Put|Delete|Patch)\\b");
        for (Path path : mainJavaFiles()) {
            String source = read(path);
            Matcher matcher = pattern.matcher(source);
            while (matcher.find()) {
                offenders.add(path + ":" + lineOf(source, matcher.start()) + " -> " + matcher.group());
            }
        }
        return offenders;
    }

    static List<String> suspiciousEnumFactoryCalls() {
        List<String> offenders = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?:EnumSet\\.of|Set\\.of)\\([^)]*\\.values\\(\\)\\)");
        for (Path path : mainJavaFiles()) {
            String source = read(path);
            Matcher matcher = pattern.matcher(source);
            while (matcher.find()) {
                offenders.add(path + ":" + lineOf(source, matcher.start()) + " -> " + matcher.group());
            }
        }
        return offenders;
    }

    static List<String> controllerMappingImportViolations() {
        List<String> offenders = new ArrayList<>();
        for (Path path : mainJavaFiles()) {
            if (!path.getFileName().toString().endsWith("Controller.java")) {
                continue;
            }
            String source = read(path);
            if (!containsMappingAnnotation(source)) {
                continue;
            }
            boolean ok = source.contains("import org.springframework.web.bind.annotation.")
                    || source.contains("@org.springframework.web.bind.annotation.");
            if (!ok) {
                offenders.add(path.toString());
            }
        }
        return offenders;
    }

    static List<String> inferableEnumSwitchGaps() {
        Map<EnumKey, List<String>> enumConstants = collectEnumConstants();
        List<String> offenders = new ArrayList<>();
        for (Path path : mainJavaFiles()) {
            String source = read(path);
            String stripped = stripCommentsAndStrings(source);
            String pkg = packageName(stripped);
            Map<String, EnumKey> importedEnums = importedEnums(stripped, enumConstants.keySet());
            enumConstants.keySet().stream()
                    .filter(key -> key.packageName().equals(pkg))
                    .forEach(key -> importedEnums.put(key.simpleName(), key));
            Matcher switchMatcher = SWITCH_PATTERN.matcher(stripped);
            while (switchMatcher.find()) {
                String variable = switchMatcher.group(1);
                String variableType = resolveVariableTypeNear(stripped, variable, switchMatcher.start());
                if (variableType == null) {
                    continue;
                }
                EnumKey enumKey = importedEnums.get(simpleType(variableType));
                if (enumKey == null) {
                    continue;
                }
                String body = extractBlockBody(stripped, switchMatcher.end() - 1);
                if (body == null || body.contains("default")) {
                    continue;
                }
                Set<String> labels = caseLabels(body);
                Set<String> missing = new LinkedHashSet<>(enumConstants.get(enumKey));
                missing.removeAll(labels);
                if (!missing.isEmpty()) {
                    offenders.add(path + ":" + lineOf(source, switchMatcher.start()) + " -> " + enumKey.simpleName() + " sem default e sem casos: " + missing);
                }
            }
        }
        return offenders;
    }

    static List<String> nonPublicCrossPackageImports() {
        Map<String, TopLevelType> types = collectTopLevelTypes();
        Set<String> nonPublicTypes = types.entrySet().stream()
                .filter(entry -> !entry.getValue().isPublic())
                .map(Map.Entry::getKey)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        List<String> offenders = new ArrayList<>();
        for (Path path : mainJavaFiles()) {
            String source = read(path);
            String currentPackage = packageName(source);
            Matcher matcher = IMPORT_PATTERN.matcher(source);
            while (matcher.find()) {
                String imported = matcher.group(1);
                if (!nonPublicTypes.contains(imported)) {
                    continue;
                }
                String importedPackage = imported.substring(0, imported.lastIndexOf('.'));
                if (Objects.equals(currentPackage, importedPackage)) {
                    continue;
                }
                offenders.add(path + ":" + lineOf(source, matcher.start()) + " -> importa tipo não público " + imported + " declarado em " + types.get(imported).path());
            }
        }
        return offenders;
    }

    private static boolean containsMappingAnnotation(String source) {
        return source.contains("@GetMapping")
                || source.contains("@PostMapping")
                || source.contains("@PutMapping")
                || source.contains("@DeleteMapping")
                || source.contains("@PatchMapping")
                || source.contains("@RequestMapping");
    }

    private static Map<String, TopLevelType> collectTopLevelTypes() {
        Map<String, TopLevelType> out = new LinkedHashMap<>();
        for (Path path : mainJavaFiles()) {
            String source = stripCommentsAndStrings(read(path));
            String pkg = packageName(source);
            int depth = 0;
            for (int index = 0; index < source.length(); index++) {
                char ch = source.charAt(index);
                if (ch == '{') {
                    depth++;
                } else if (ch == '}') {
                    depth = Math.max(0, depth - 1);
                }
                if (depth != 0) {
                    continue;
                }
                Matcher matcher = TOP_LEVEL_TYPE_PATTERN.matcher(source);
                matcher.region(index, source.length());
                if (matcher.lookingAt()) {
                    String name = matcher.group(3);
                    boolean isPublic = matcher.group(1) != null;
                    String fqcn = pkg == null || pkg.isBlank() ? name : pkg + "." + name;
                    out.put(fqcn, new TopLevelType(path, isPublic));
                    index = matcher.end() - 1;
                }
            }
        }
        return out;
    }

    private static Map<EnumKey, List<String>> collectEnumConstants() {
        Map<EnumKey, List<String>> out = new LinkedHashMap<>();
        for (Path path : mainJavaFiles()) {
            String source = read(path);
            String pkg = packageName(source);
            Matcher matcher = ENUM_PATTERN.matcher(source);
            while (matcher.find()) {
                int bodyStart = source.indexOf('{', matcher.end());
                if (bodyStart < 0) {
                    continue;
                }
                String body = extractBlockBody(source, bodyStart);
                if (body == null) {
                    continue;
                }
                String constantsBlock = enumConstantsBlock(body);
                if (constantsBlock.isBlank()) {
                    continue;
                }
                List<String> constants = new ArrayList<>();
                for (String token : topLevelSplit(constantsBlock, ',')) {
                    String normalized = token.replaceAll("@\\w+(?:\\([^)]*\\))?", " ").trim();
                    String head = normalized.contains("(") ? normalized.substring(0, normalized.indexOf('(')).trim() : normalized;
                    if (head.contains(" ")) {
                        head = head.substring(head.lastIndexOf(' ') + 1);
                    }
                    if (head.matches("[A-Z][A-Z0-9_]*")) {
                        constants.add(head);
                    }
                }
                out.put(new EnumKey(pkg, matcher.group(1)), List.copyOf(constants));
            }
        }
        return out;
    }

    private static String enumConstantsBlock(String enumBody) {
        int depth = 0;
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < enumBody.length(); index++) {
            char ch = enumBody.charAt(index);
            if (ch == '{' || ch == '(' || ch == '[') {
                depth++;
            } else if (ch == '}' || ch == ')' || ch == ']') {
                depth = Math.max(0, depth - 1);
            }
            if (ch == ';' && depth == 0) {
                break;
            }
            builder.append(ch);
        }
        return builder.toString();
    }

    private static Map<String, EnumKey> importedEnums(String source, Collection<EnumKey> enums) {
        Map<String, EnumKey> out = new LinkedHashMap<>();
        Matcher matcher = IMPORT_PATTERN.matcher(source);
        while (matcher.find()) {
            String imported = matcher.group(1);
            int index = imported.lastIndexOf('.');
            if (index < 0) {
                continue;
            }
            EnumKey key = new EnumKey(imported.substring(0, index), imported.substring(index + 1));
            if (enums.contains(key)) {
                out.put(key.simpleName(), key);
            }
        }
        return out;
    }

    private static Map<String, String> visibleVariables(String source) {
        Map<String, String> out = new LinkedHashMap<>();
        Matcher paramMatcher = PARAM_BLOCK_PATTERN.matcher(source);
        while (paramMatcher.find()) {
            String params = paramMatcher.group(1);
            if (params.length() > 300) {
                continue;
            }
            for (String token : topLevelSplit(params, ',')) {
                Matcher matcher = Pattern.compile("([\\w$.<>?]+)\\s+(\\w+)$").matcher(token.trim());
                if (matcher.find()) {
                    out.put(matcher.group(2), matcher.group(1));
                }
            }
        }
        Matcher localMatcher = METHOD_LOCAL_PATTERN.matcher(source);
        while (localMatcher.find()) {
            out.putIfAbsent(localMatcher.group(2), localMatcher.group(1));
        }
        return out;
    }

    private static Set<String> caseLabels(String body) {
        Set<String> labels = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("\\bcase\\s+([^:>{]+?)(?:->|:)").matcher(body);
        while (matcher.find()) {
            for (String token : topLevelSplit(matcher.group(1), ',')) {
                String label = token.trim();
                if (label.matches("[A-Z][A-Z0-9_]*")) {
                    labels.add(label);
                }
            }
        }
        return labels;
    }

    private static String resolveVariableTypeNear(String source, String variable, int offset) {
        if (variable == null || variable.isBlank()) {
            return null;
        }
        int start = Math.max(0, offset - 4000);
        String window = source.substring(start, offset);
        Pattern pattern = Pattern.compile("([\\w$.<>?]+)\\s+" + Pattern.quote(variable) + "\\s*(?==|;|,|\\))");
        Matcher matcher = pattern.matcher(window);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last;
    }

    private static String stripCommentsAndStrings(String source) {
        StringBuilder out = new StringBuilder(source.length());
        boolean lineComment = false;
        boolean blockComment = false;
        boolean stringLiteral = false;
        boolean charLiteral = false;
        boolean escaping = false;
        for (int index = 0; index < source.length(); index++) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';
            if (lineComment) {
                if (current == '\n') {
                    lineComment = false;
                    out.append('\n');
                }
                continue;
            }
            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    index++;
                }
                continue;
            }
            if (stringLiteral) {
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (current == '\\') {
                    escaping = true;
                    continue;
                }
                if (current == '"') {
                    stringLiteral = false;
                    out.append("\"\"");
                }
                continue;
            }
            if (charLiteral) {
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (current == '\\') {
                    escaping = true;
                    continue;
                }
                if (current == '\'') {
                    charLiteral = false;
                    out.append("''");
                }
                continue;
            }
            if (current == '/' && next == '/') {
                lineComment = true;
                index++;
                continue;
            }
            if (current == '/' && next == '*') {
                blockComment = true;
                index++;
                continue;
            }
            if (current == '"') {
                stringLiteral = true;
                continue;
            }
            if (current == '\'') {
                charLiteral = true;
                continue;
            }
            out.append(current);
        }
        return out.toString();
    }

    private static String extractBlockBody(String source, int openBraceIndex) {
        int depth = 0;
        StringBuilder builder = new StringBuilder();
        for (int index = openBraceIndex; index < source.length(); index++) {
            char ch = source.charAt(index);
            if (ch == '{') {
                depth++;
                if (depth == 1) {
                    continue;
                }
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return builder.toString();
                }
            }
            if (depth >= 1) {
                builder.append(ch);
            }
        }
        return null;
    }

    private static List<String> topLevelSplit(String source, char separator) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int index = 0; index < source.length(); index++) {
            char ch = source.charAt(index);
            if (ch == '(' || ch == '{' || ch == '[') {
                depth++;
            } else if (ch == ')' || ch == '}' || ch == ']') {
                depth = Math.max(0, depth - 1);
            }
            if (ch == separator && depth == 0) {
                out.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        out.add(current.toString());
        return out;
    }

    private static String simpleType(String value) {
        String simple = value == null ? null : value.trim();
        if (simple == null || simple.isBlank()) {
            return simple;
        }
        int genericIndex = simple.indexOf('<');
        if (genericIndex >= 0) {
            simple = simple.substring(0, genericIndex);
        }
        int packageIndex = simple.lastIndexOf('.');
        if (packageIndex >= 0) {
            simple = simple.substring(packageIndex + 1);
        }
        return simple;
    }

    private static int lineOf(String source, int offset) {
        int line = 1;
        for (int index = 0; index < offset && index < source.length(); index++) {
            if (source.charAt(index) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static String packageName(String source) {
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    record EnumKey(String packageName, String simpleName) {
    }

    record TopLevelType(Path path, boolean isPublic) {
    }
}
