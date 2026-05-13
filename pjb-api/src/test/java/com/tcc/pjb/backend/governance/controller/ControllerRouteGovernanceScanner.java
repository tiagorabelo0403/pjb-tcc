package com.tcc.pjb.backend.governance.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class ControllerRouteGovernanceScanner {

    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Path ROUTES_FILE = MAIN_JAVA.resolve("com/tcc/pjb/backend/core/operational/OperationalApiRoutes.java");
    private static final Pattern MAPPING_PATTERN = Pattern.compile("@(?<ann>RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\s*(\\((?<args>.*?)\\))?", Pattern.DOTALL);
    private static final Pattern CONSTANT_PATTERN = Pattern.compile("public\\s+static\\s+final\\s+String\\s+(\\w+)\\s*=\\s*([^;]+);");
    private static final Pattern PATH_VARIABLE_VALUE_PATTERN = Pattern.compile("@PathVariable(?:\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"\\s*\\))?");
    private static final Pattern REQUEST_METHOD_PATTERN = Pattern.compile("RequestMethod\\.([A-Z]+)");

    private ControllerRouteGovernanceScanner() {
    }

    static List<String> duplicateControllerRouteMappings() {
        Map<String, String> constants = operationalRouteConstants();
        Map<RouteKey, Set<String>> index = new LinkedHashMap<>();
        for (ControllerModel controller : controllers(constants)) {
            for (MethodModel method : controller.methods()) {
                for (String httpMethod : method.httpMethods(controller.classHttpMethods())) {
                    for (String route : method.resolvedRoutes(controller.basePaths())) {
                        RouteKey key = new RouteKey(httpMethod, route);
                        index.computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                                .add(controller.path() + "#" + method.methodName());
                    }
                }
            }
        }
        List<String> offenders = new ArrayList<>();
        index.forEach((key, owners) -> {
            if (owners.size() > 1) {
                offenders.add(key.httpMethod() + " " + key.route() + " -> " + owners);
            }
        });
        return offenders;
    }

    static List<String> pathVariableBindingViolations() {
        Map<String, String> constants = operationalRouteConstants();
        List<String> offenders = new ArrayList<>();
        for (ControllerModel controller : controllers(constants)) {
            for (MethodModel method : controller.methods()) {
                Set<String> placeholders = new LinkedHashSet<>();
                for (String route : method.resolvedRoutes(controller.basePaths())) {
                    placeholders.addAll(extractPlaceholders(route));
                }
                if (placeholders.isEmpty()) {
                    continue;
                }
                Set<String> boundNames = method.pathVariableNames();
                Set<String> missing = new LinkedHashSet<>(placeholders);
                missing.removeAll(boundNames);
                Set<String> extra = new LinkedHashSet<>(boundNames);
                extra.removeAll(placeholders);
                if (!missing.isEmpty() || !extra.isEmpty()) {
                    offenders.add(controller.path() + "#" + method.methodName()
                            + " -> placeholders=" + placeholders
                            + ", pathVariables=" + boundNames
                            + ", missing=" + missing
                            + ", extra=" + extra);
                }
            }
        }
        return offenders;
    }

    static List<String> duplicateOperationalBases() {
        Path path = ROUTES_FILE;
        String source = read(path);
        Matcher baseMatcher = Pattern.compile("private\\s+static\\s+final\\s+List<String>\\s+OPERATIONAL_BASES\\s*=\\s*List\\.of\\((.*?)\\);", Pattern.DOTALL)
                .matcher(source);
        if (!baseMatcher.find()) {
            return List.of("OperationalApiRoutes.OPERATIONAL_BASES ausente");
        }
        List<String> bases = resolveArrayExpressions(baseMatcher.group(1), operationalRouteConstants());
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (String base : bases) {
            grouped.computeIfAbsent(base, ignored -> new ArrayList<>()).add(base);
        }
        List<String> offenders = new ArrayList<>();
        grouped.forEach((base, values) -> {
            if (base == null || base.isBlank()) {
                offenders.add("Base operacional vazia ou nula encontrada");
            } else if (values.size() > 1) {
                offenders.add("Base operacional duplicada: " + base);
            }
        });
        return offenders;
    }

    private static List<ControllerModel> controllers(Map<String, String> constants) {
        try (Stream<Path> stream = Files.walk(MAIN_JAVA)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .sorted()
                    .map(path -> parseController(path, read(path), constants))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao varrer controllers", ex);
        }
    }

    private static ControllerModel parseController(Path path, String source, Map<String, String> constants) {
        int classIndex = source.indexOf("class ");
        if (classIndex < 0) {
            return null;
        }
        List<String> basePaths = List.of("/");
        Set<String> classHttpMethods = Set.of("ANY");
        List<MethodModel> methods = new ArrayList<>();
        Matcher matcher = MAPPING_PATTERN.matcher(source);
        while (matcher.find()) {
            String annotation = matcher.group("ann");
            String args = matcher.group("args");
            if (matcher.start() < classIndex) {
                basePaths = resolveMappings(args, constants);
                classHttpMethods = resolveHttpMethods(annotation, args);
                continue;
            }
            int signatureStart = skipWhitespace(source, matcher.end());
            MethodSignature signature = findMethodSignature(source, signatureStart);
            if (signature == null) {
                continue;
            }
            methods.add(new MethodModel(
                    signature.methodName(),
                    resolveMappings(args, constants),
                    resolveHttpMethods(annotation, args),
                    extractPathVariableNames(signature.parameters())));
        }
        return new ControllerModel(path.toString(), basePaths, classHttpMethods, methods);
    }

    private static MethodSignature findMethodSignature(String source, int fromIndex) {
        int searchLimit = Math.min(source.length(), fromIndex + 2400);
        int methodParen = -1;
        int nameStart = -1;
        for (int index = fromIndex; index < searchLimit; index++) {
            char ch = source.charAt(index);
            if (ch == '(') {
                methodParen = index;
                nameStart = previousJavaIdentifierStart(source, index - 1);
                break;
            }
            if (ch == ';' || ch == '=') {
                return null;
            }
        }
        if (methodParen < 0 || nameStart < 0) {
            return null;
        }
        String methodName = source.substring(nameStart, previousJavaIdentifierEnd(source, methodParen - 1) + 1).trim();
        if (methodName.isEmpty() || methodName.equals("if") || methodName.equals("for") || methodName.equals("while") || methodName.equals("switch")) {
            return null;
        }
        int parenEnd = findMatching(source, methodParen, '(', ')');
        if (parenEnd < 0) {
            return null;
        }
        int bodyStart = skipWhitespace(source, parenEnd + 1);
        if (bodyStart >= source.length() || source.charAt(bodyStart) != '{') {
            return null;
        }
        return new MethodSignature(methodName, source.substring(methodParen + 1, parenEnd));
    }

    private static int previousJavaIdentifierStart(String source, int index) {
        int current = previousJavaIdentifierEnd(source, index);
        if (current < 0) {
            return -1;
        }
        while (current >= 0 && Character.isJavaIdentifierPart(source.charAt(current))) {
            current--;
        }
        return current + 1;
    }

    private static int previousJavaIdentifierEnd(String source, int index) {
        int current = index;
        while (current >= 0 && Character.isWhitespace(source.charAt(current))) {
            current--;
        }
        return current;
    }

    private static int skipWhitespace(String source, int index) {
        int current = index;
        while (current < source.length() && Character.isWhitespace(source.charAt(current))) {
            current++;
        }
        return current;
    }

    private static List<String> resolveMappings(String args, Map<String, String> constants) {
        if (args == null || args.isBlank()) {
            return List.of("/");
        }
        List<String> expressions = new ArrayList<>();
        expressions.addAll(extractNamedExpressions(args, "value"));
        expressions.addAll(extractNamedExpressions(args, "path"));
        if (expressions.isEmpty()) {
            List<String> tokens = splitTopLevel(args, ',');
            if (!tokens.isEmpty()) {
                expressions.add(tokens.getFirst());
            }
        }
        if (expressions.isEmpty()) {
            return List.of("/");
        }
        List<String> resolved = new ArrayList<>();
        for (String expression : expressions) {
            if (expression == null || expression.isBlank()) {
                continue;
            }
            if (expression.startsWith("{")) {
                resolved.addAll(resolveArrayExpressions(expression.substring(1, expression.length() - 1), constants));
                continue;
            }
            resolveExpression(expression, constants).ifPresent(resolved::add);
        }
        if (resolved.isEmpty()) {
            return List.of("/");
        }
        return resolved.stream().map(ControllerRouteGovernanceScanner::normalizePath).distinct().toList();
    }

    private static List<String> extractNamedExpressions(String args, String key) {
        List<String> expressions = new ArrayList<>();
        Matcher matcher = Pattern.compile(key + "\\s*=\\s*(\\{.*?\\}|\"[^\"]*\"|[\\w.]+(?:\\s*\\+\\s*[\\w.\"]+)*)", Pattern.DOTALL)
                .matcher(args);
        while (matcher.find()) {
            expressions.add(matcher.group(1).trim());
        }
        return expressions;
    }

    private static List<String> resolveArrayExpressions(String body, Map<String, String> constants) {
        List<String> resolved = new ArrayList<>();
        for (String token : splitTopLevel(body, ',')) {
            resolveExpression(token, constants).ifPresent(resolved::add);
        }
        return resolved;
    }

    private static Optional<String> resolveExpression(String expression, Map<String, String> constants) {
        if (expression == null) {
            return Optional.empty();
        }
        String trimmed = trimOuterParentheses(expression.trim());
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder builder = new StringBuilder();
        for (String token : splitTopLevel(trimmed, '+')) {
            String part = token.trim();
            if (part.isEmpty()) {
                continue;
            }
            if (part.startsWith("\"") && part.endsWith("\"")) {
                builder.append(part, 1, part.length() - 1);
                continue;
            }
            String key = part.startsWith("OperationalApiRoutes.") ? part.substring("OperationalApiRoutes.".length()) : part;
            String resolved = constants.get(key);
            if (resolved == null) {
                return Optional.empty();
            }
            builder.append(resolved);
        }
        return Optional.of(normalizePath(builder.toString()));
    }

    private static String trimOuterParentheses(String expression) {
        String value = expression;
        while (value.startsWith("(") && value.endsWith(")")) {
            int end = findMatching(value, 0, '(', ')');
            if (end != value.length() - 1) {
                break;
            }
            value = value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private static Set<String> resolveHttpMethods(String annotation, String args) {
        if (!"RequestMapping".equals(annotation)) {
            return Set.of(annotation.replace("Mapping", "").toUpperCase(Locale.ROOT));
        }
        if (args == null || args.isBlank()) {
            return Set.of("ANY");
        }
        Matcher matcher = REQUEST_METHOD_PATTERN.matcher(args);
        Set<String> methods = new LinkedHashSet<>();
        while (matcher.find()) {
            methods.add(matcher.group(1));
        }
        return methods.isEmpty() ? Set.of("ANY") : methods;
    }

    private static Set<String> extractPathVariableNames(String parameters) {
        Set<String> names = new LinkedHashSet<>();
        for (String parameter : splitTopLevel(parameters, ',')) {
            if (!parameter.contains("PathVariable")) {
                continue;
            }
            Matcher matcher = PATH_VARIABLE_VALUE_PATTERN.matcher(parameter);
            String explicit = matcher.find() ? matcher.group(1) : null;
            String inferred = inferParameterName(parameter);
            if (explicit != null && !explicit.isBlank()) {
                names.add(explicit.trim());
            } else if (inferred != null && !inferred.isBlank()) {
                names.add(inferred.trim());
            }
        }
        return names;
    }

    private static String inferParameterName(String parameter) {
        String cleaned = parameter
                .replaceAll("@[\\w.]+(?:\\([^)]*\\))?", " ")
                .replace("final", " ")
                .trim();
        Matcher matcher = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*$").matcher(cleaned);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Set<String> extractPlaceholders(String path) {
        Matcher matcher = Pattern.compile("\\{([^}/]+)}").matcher(path);
        Set<String> placeholders = new LinkedHashSet<>();
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }

    private static List<String> splitTopLevel(String value, char separator) {
        List<String> out = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return out;
        }
        StringBuilder current = new StringBuilder();
        int parentheses = 0;
        int brackets = 0;
        int braces = 0;
        int angles = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (inString) {
                current.append(ch);
                if (escaping) {
                    escaping = false;
                } else if (ch == '\\') {
                    escaping = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            switch (ch) {
                case '"' -> {
                    inString = true;
                    current.append(ch);
                }
                case '(' -> {
                    parentheses++;
                    current.append(ch);
                }
                case ')' -> {
                    parentheses = Math.max(0, parentheses - 1);
                    current.append(ch);
                }
                case '[' -> {
                    brackets++;
                    current.append(ch);
                }
                case ']' -> {
                    brackets = Math.max(0, brackets - 1);
                    current.append(ch);
                }
                case '{' -> {
                    braces++;
                    current.append(ch);
                }
                case '}' -> {
                    braces = Math.max(0, braces - 1);
                    current.append(ch);
                }
                case '<' -> {
                    angles++;
                    current.append(ch);
                }
                case '>' -> {
                    angles = Math.max(0, angles - 1);
                    current.append(ch);
                }
                default -> {
                    if (ch == separator && parentheses == 0 && brackets == 0 && braces == 0 && angles == 0) {
                        out.add(current.toString().trim());
                        current.setLength(0);
                    } else {
                        current.append(ch);
                    }
                }
            }
        }
        if (!current.isEmpty()) {
            out.add(current.toString().trim());
        }
        return out;
    }

    private static int findMatching(String value, int start, char open, char close) {
        ArrayDeque<Character> stack = new ArrayDeque<>();
        boolean inString = false;
        boolean escaping = false;
        for (int index = start; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (ch == '\\') {
                    escaping = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
                continue;
            }
            if (ch == open) {
                stack.push(ch);
                continue;
            }
            if (ch == close) {
                if (stack.isEmpty()) {
                    return -1;
                }
                stack.pop();
                if (stack.isEmpty()) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static Map<String, String> operationalRouteConstants() {
        Map<String, String> raw = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.walk(MAIN_JAVA)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .forEach(path -> collectStringConstants(path, raw));
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao varrer constantes de rotas", ex);
        }
        Map<String, String> resolved = new LinkedHashMap<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<String, String> entry : raw.entrySet()) {
                if (resolved.containsKey(entry.getKey())) {
                    continue;
                }
                Optional<String> value = resolveExpression(entry.getValue(), resolved);
                if (value.isPresent()) {
                    resolved.put(entry.getKey(), value.get());
                    changed = true;
                }
            }
        }
        return resolved;
    }

    private static void collectStringConstants(Path path, Map<String, String> raw) {
        String source = read(path);
        String simpleClassName = path.getFileName().toString().replaceFirst("\\.java$", "");
        String packageName = packageName(source);
        Matcher matcher = CONSTANT_PATTERN.matcher(source);
        while (matcher.find()) {
            String name = matcher.group(1);
            String expression = matcher.group(2).trim();
            raw.putIfAbsent(name, expression);
            raw.put(simpleClassName + "." + name, expression);
            if (!packageName.isBlank()) {
                raw.put(packageName + "." + simpleClassName + "." + name, expression);
            }
        }
    }

    private static String packageName(String source) {
        Matcher matcher = Pattern.compile("package\\s+([A-Za-z0-9_.]+)\\s*;").matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.trim().replaceAll("/+", "/");
        if (!normalized.startsWith("/")) {
            normalized = '/' + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler arquivo: " + path, ex);
        }
    }

    private record ControllerModel(String path,
                                   List<String> basePaths,
                                   Set<String> classHttpMethods,
                                   List<MethodModel> methods) {
    }

    private record MethodModel(String methodName,
                               List<String> methodPaths,
                               Set<String> methodHttpMethods,
                               Set<String> pathVariableNames) {

        Collection<String> httpMethods(Set<String> classHttpMethods) {
            if (methodHttpMethods.size() == 1 && methodHttpMethods.contains("ANY")) {
                return classHttpMethods;
            }
            return methodHttpMethods;
        }

        Collection<String> resolvedRoutes(List<String> basePaths) {
            Set<String> routes = new LinkedHashSet<>();
            for (String basePath : basePaths) {
                for (String methodPath : methodPaths) {
                    routes.add(normalizePath(basePath + "/" + methodPath));
                }
            }
            return routes;
        }
    }

    private record MethodSignature(String methodName, String parameters) {
    }

    private record RouteKey(String httpMethod, String route) {
    }
}
