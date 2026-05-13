package com.tcc.pjb.backend.core.quality.apisurface.application;

import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceIssue;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbProjectPathResolver;
import org.springframework.stereotype.Service;

@Service
public class PjbApiSurfaceSanityApplicationService {

    private static final Pattern PACKAGE = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    private static final Pattern TYPE = Pattern.compile("\\b(?:public\\s+)?(?:record|class|interface|enum)\\s+(\\w+)");
    private static final Pattern REQUEST_MAPPING = Pattern.compile("@RequestMapping\\((?:value\\s*=\\s*)?\"([^\"]*)\"");
    private static final Pattern HTTP_MAPPING = Pattern.compile("@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\((?:value\\s*=\\s*)?\"([^\"]*)\"");
    private static final Pattern RESPONSE_ENTITY = Pattern.compile("ResponseEntity\\s*<\\s*([\\w.$]+)\\s*>");
    private static final Pattern SERVICE_NESTED_RESPONSE = Pattern.compile("ResponseEntity\\s*<\\s*([A-Za-z0-9_$.]+Service\\.[A-Za-z0-9_$.]+)\\s*>");
    private static final Pattern RAW_MAP_RESPONSE = Pattern.compile("ResponseEntity\\s*<\\s*Map\\s*<\\s*String\\s*,\\s*Object\\s*>\\s*>");
    private static final Pattern WILDCARD_RESPONSE = Pattern.compile("ResponseEntity\\s*<\\s*\\?\\s*>");
    private static final Pattern INTERNAL_DOMAIN_IMPORT = Pattern.compile("import\\s+com\\.tcc\\.pjb\\.backend\\.core\\..*\\.domain\\..*;");
    private static final Pattern SERVICE_NESTED_CONTRACT = Pattern.compile("\\b[A-Z][A-Za-z0-9_$.]*Service\\.[A-Za-z0-9_$.]+\\b");
    private static final Set<String> ENTITY_NAMES = Set.of("Processo", "Usuario", "Jurisdicao", "DocumentoProcessual");

    private final Path projectRoot;

    public PjbApiSurfaceSanityApplicationService() {
        this(Path.of("").toAbsolutePath().normalize());
    }

    public PjbApiSurfaceSanityApplicationService(Path projectRoot) {
        this.projectRoot = PjbProjectPathResolver.apiModuleRoot(projectRoot);
    }

    public PjbApiSurfaceSanityAggregate auditar() {
        Path mainJava = projectRoot.resolve("src/main/java");
        if (!Files.isDirectory(mainJava)) {
            return new PjbApiSurfaceSanityAggregate(false, true, 0, 0, 0, 0, 0, List.of(), Instant.now());
        }
        List<Path> controllers = collect(mainJava, "/controller/");
        List<Path> dtos = collect(mainJava, "/model/dto/");
        ArrayList<PjbApiSurfaceIssue> issues = new ArrayList<>();
        LinkedHashMap<String, List<String>> routes = new LinkedHashMap<>();
        int entidadesExpostasDiretamente = 0;
        int dtoInlineEmController = 0;
        int rawMapEmController = 0;
        int internalDomainImports = 0;
        int nestedContracts = 0;
        int wildcardResponses = 0;
        for (Path controller : controllers) {
            String source = read(controller);
            String classPrefix = normalizePath(match(REQUEST_MAPPING, source));
            for (Route route : routes(source, classPrefix)) {
                String key = route.method + ' ' + route.path;
                routes.computeIfAbsent(key, ignored -> new ArrayList<>()).add(relativize(controller));
            }
            if (controllerDeclaresInlineDto(source)) {
                dtoInlineEmController++;
                issues.add(new PjbApiSurfaceIssue(
                        "controller.inline.dto",
                        "MEDIO",
                        relativize(controller),
                        "CONTROLLER",
                        controller.getFileName().toString().replace(".java", ""),
                        List.of("Controller declara record ou classe auxiliar inline; mover para model.dto")
                ));
            }
            for (String type : responseEntityTypes(source)) {
                String simpleName = simpleName(type);
                if (ENTITY_NAMES.contains(simpleName)) {
                    entidadesExpostasDiretamente++;
                    issues.add(new PjbApiSurfaceIssue(
                            "controller.entity.exposure",
                            "MEDIO",
                            relativize(controller),
                            "RESPONSE",
                            simpleName,
                            List.of("Controller expondo entidade diretamente na superfície HTTP")
                    ));
                    continue;
                }
                if (simpleName.endsWith("Aggregate") || type.contains(".core.") && type.contains(".domain.")) {
                    entidadesExpostasDiretamente++;
                    issues.add(new PjbApiSurfaceIssue(
                            "controller.aggregate.exposure",
                            "ALTO",
                            relativize(controller),
                            "RESPONSE",
                            simpleName,
                            List.of("Controller expondo aggregate de domínio diretamente na superfície HTTP")
                    ));
                }
            }
            for (String type : serviceNestedResponseTypes(source)) {
                entidadesExpostasDiretamente++;
                issues.add(new PjbApiSurfaceIssue(
                        "service.nested.exposure",
                        "ALTO",
                        relativize(controller),
                        "RESPONSE",
                        type,
                        List.of("Controller expõe tipo aninhado de service na superfície HTTP")
                ));
            }
            if (controllerUsesRawMapResponse(source)) {
                rawMapEmController++;
                issues.add(new PjbApiSurfaceIssue(
                        "controller.raw.map.response",
                        "MEDIO",
                        relativize(controller),
                        "RESPONSE",
                        "Map<String,Object>",
                        List.of("Controller expõe Map<String,Object> cru na superfície HTTP")
                ));
            }
            if (controllerImportsInternalDomain(source)) {
                internalDomainImports++;
                issues.add(new PjbApiSurfaceIssue(
                        "controller.internal.domain.import",
                        "MEDIO",
                        relativize(controller),
                        "CONTROLLER",
                        controller.getFileName().toString().replace(".java", ""),
                        List.of("Controller importa domínio interno diretamente; preferir facade/mapper de surface")
                ));
            }
            if (controllerUsesWildcardResponse(source)) {
                wildcardResponses++;
                issues.add(new PjbApiSurfaceIssue(
                        "controller.wildcard.response",
                        "MEDIO",
                        relativize(controller),
                        "RESPONSE",
                        "ResponseEntity<?>",
                        List.of("Controller expõe wildcard na superfície HTTP; preferir DTO ou contrato explícito")
                ));
            }
            if (controllerUsesNestedServiceContract(source)) {
                nestedContracts++;
                issues.add(new PjbApiSurfaceIssue(
                        "controller.nested.service.contract",
                        "ALTO",
                        relativize(controller),
                        "CONTROLLER",
                        controller.getFileName().toString().replace(".java", ""),
                        List.of("Controller expõe contrato aninhado de service; preferir DTO próprio de surface")
                ));
            }
        }
        int rotasDuplicadas = 0;
        for (Map.Entry<String, List<String>> entry : routes.entrySet()) {
            if (entry.getValue().size() > 1) {
                rotasDuplicadas++;
                String[] parts = entry.getKey().split(" ", 2);
                issues.add(new PjbApiSurfaceIssue(
                        "api.route.duplicada",
                        "CRITICO",
                        String.join(" | ", entry.getValue()),
                        parts[0],
                        parts.length > 1 ? parts[1] : "",
                        List.copyOf(entry.getValue())
                ));
            }
        }
        int dtoForaDoPadrao = 0;
        for (Path dto : dtos) {
            String source = read(dto);
            String pkg = match(PACKAGE, source);
            String type = match(TYPE, source);
            String fileName = dto.getFileName().toString().replace(".java", "");
            LinkedHashSet<String> detalhes = new LinkedHashSet<>();
            if (!pkg.startsWith("com.tcc.pjb.backend.model.dto.")) {
                detalhes.add("pacote.dto.fora.do.padrao=" + pkg);
            }
            if (!type.equals(fileName)) {
                detalhes.add("nome.tipo.diverge.do.arquivo=" + type + " vs " + fileName);
            }
            if (!(fileName.endsWith("Request") || fileName.endsWith("Response") || fileName.endsWith("Dto") || fileName.endsWith("DTO") || fileName.endsWith("Payload"))) {
                detalhes.add("nome.dto.sem.sufixo.convencional=" + fileName);
            }
            if (!detalhes.isEmpty()) {
                dtoForaDoPadrao++;
                issues.add(new PjbApiSurfaceIssue(
                        "dto.padronizacao",
                        "ALTO",
                        relativize(dto),
                        "DTO",
                        fileName,
                        List.copyOf(detalhes)
                ));
            }
        }
        boolean limpo = rotasDuplicadas == 0 && dtoForaDoPadrao == 0 && entidadesExpostasDiretamente == 0 && dtoInlineEmController == 0 && rawMapEmController == 0 && internalDomainImports == 0 && nestedContracts == 0 && wildcardResponses == 0;
        return new PjbApiSurfaceSanityAggregate(
                true,
                limpo,
                controllers.size(),
                dtos.size(),
                rotasDuplicadas,
                dtoForaDoPadrao + dtoInlineEmController + rawMapEmController + internalDomainImports + nestedContracts + wildcardResponses,
                entidadesExpostasDiretamente,
                List.copyOf(issues),
                Instant.now()
        );
    }

    public PjbApiSurfaceSanityAggregate scan() {
        return auditar();
    }

    private static List<Path> collect(Path root, String token) {
        String normalizedToken = token.replace('\\', '/');
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().replace('\\', '/').contains(normalizedToken))
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private static List<Route> routes(String source, String prefix) {
        ArrayList<Route> routes = new ArrayList<>();
        Matcher matcher = HTTP_MAPPING.matcher(source);
        while (matcher.find()) {
            String method = httpVerb(matcher.group(1));
            String route = normalizePath(prefix + '/' + Objects.toString(matcher.group(2), ""));
            routes.add(new Route(method, route));
        }
        return routes;
    }

    private static boolean controllerDeclaresInlineDto(String source) {
        String normalized = source == null ? "" : source;
        return normalized.contains("\n    record ")
                || normalized.contains("\n    class ")
                || normalized.contains("\n    static class ");
    }

    private static boolean controllerUsesRawMapResponse(String source) {
        return RAW_MAP_RESPONSE.matcher(source == null ? "" : source).find();
    }

    private static boolean controllerImportsInternalDomain(String source) {
        return INTERNAL_DOMAIN_IMPORT.matcher(source == null ? "" : source).find();
    }

    private static boolean controllerUsesWildcardResponse(String source) {
        return WILDCARD_RESPONSE.matcher(source == null ? "" : source).find();
    }

    private static boolean controllerUsesNestedServiceContract(String source) {
        String normalized = source == null ? "" : source;
        String[] lines = normalized.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (!trimmed.startsWith("public ")
                    && !trimmed.startsWith("protected ")
                    && !trimmed.startsWith("private ")
                    && !trimmed.contains("@RequestBody")
                    && !trimmed.contains("@ModelAttribute")
                    && !trimmed.contains("ResponseEntity<")) {
                continue;
            }
            Matcher matcher = SERVICE_NESTED_CONTRACT.matcher(trimmed);
            while (matcher.find()) {
                String contract = matcher.group();
                if (contract.contains("FacadeService.")) {
                    continue;
                }
                if (contract.contains("ProjectionSupport.")) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private static String httpVerb(String annotation) {
        return annotation.replace("Mapping", "").toUpperCase(Locale.ROOT);
    }

    private static List<String> responseEntityTypes(String source) {
        ArrayList<String> types = new ArrayList<>();
        Matcher matcher = RESPONSE_ENTITY.matcher(source);
        while (matcher.find()) {
            types.add(matcher.group(1).trim());
        }
        return types;
    }

    private static List<String> serviceNestedResponseTypes(String source) {
        ArrayList<String> types = new ArrayList<>();
        Matcher matcher = SERVICE_NESTED_RESPONSE.matcher(source);
        while (matcher.find()) {
            types.add(matcher.group(1).trim());
        }
        return types;
    }

    private static String normalizePath(String path) {
        String normalized = Objects.toString(path, "").trim();
        if (normalized.isBlank()) {
            return "/";
        }
        normalized = normalized.replaceAll("//+", "/");
        if (!normalized.startsWith("/")) {
            normalized = '/' + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.replaceAll("//+", "/");
    }

    private static String match(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? Objects.toString(matcher.group(1), "").trim() : "";
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    private String relativize(Path file) {
        try {
            return projectRoot.relativize(file).toString().replace('\\', '/');
        } catch (Exception ex) {
            return file.toString().replace('\\', '/');
        }
    }

    private static String simpleName(String type) {
        int index = type.lastIndexOf('.');
        return index >= 0 ? type.substring(index + 1) : type;
    }

    private record Route(String method, String path) {}
}
