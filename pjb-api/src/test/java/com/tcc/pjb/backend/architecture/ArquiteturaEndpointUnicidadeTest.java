package com.tcc.pjb.backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class ArquiteturaEndpointUnicidadeTest {

    @Test
    void naoDeveExistirEndpointDuplicadoEntreControllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        Map<String, List<String>> endpoints = new LinkedHashMap<>();
        for (var candidate : scanner.findCandidateComponents("com.tcc.pjb.backend")) {
            Class<?> controllerClass = Class.forName(candidate.getBeanClassName());
            List<String> classPaths = resolveClassPaths(controllerClass);
            for (Method method : controllerClass.getDeclaredMethods()) {
                List<RouteDescriptor> routes = resolveMethodRoutes(method);
                if (routes.isEmpty()) {
                    continue;
                }
                for (RouteDescriptor route : routes) {
                    for (String classPath : classPaths) {
                        for (String methodPath : route.paths()) {
                            String key = route.httpMethod() + " " + joinPaths(classPath, methodPath);
                            endpoints.computeIfAbsent(key, ignored -> new ArrayList<>())
                                    .add(controllerClass.getName() + "#" + method.getName());
                        }
                    }
                }
            }
        }

        Map<String, List<String>> duplicated = endpoints.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().sorted().toList(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        assertThat(duplicated).isEmpty();
    }

    private List<String> resolveClassPaths(Class<?> controllerClass) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controllerClass, RequestMapping.class);
        return expandPaths(mapping == null ? new String[0] : pickPaths(mapping.path(), mapping.value()));
    }

    private List<RouteDescriptor> resolveMethodRoutes(Method method) {
        ArrayList<RouteDescriptor> routes = new ArrayList<>();
        GetMapping getMapping = AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class);
        if (getMapping != null) {
            routes.add(new RouteDescriptor("GET", expandPaths(pickPaths(getMapping.path(), getMapping.value()))));
        }
        PostMapping postMapping = AnnotatedElementUtils.findMergedAnnotation(method, PostMapping.class);
        if (postMapping != null) {
            routes.add(new RouteDescriptor("POST", expandPaths(pickPaths(postMapping.path(), postMapping.value()))));
        }
        PutMapping putMapping = AnnotatedElementUtils.findMergedAnnotation(method, PutMapping.class);
        if (putMapping != null) {
            routes.add(new RouteDescriptor("PUT", expandPaths(pickPaths(putMapping.path(), putMapping.value()))));
        }
        DeleteMapping deleteMapping = AnnotatedElementUtils.findMergedAnnotation(method, DeleteMapping.class);
        if (deleteMapping != null) {
            routes.add(new RouteDescriptor("DELETE", expandPaths(pickPaths(deleteMapping.path(), deleteMapping.value()))));
        }
        PatchMapping patchMapping = AnnotatedElementUtils.findMergedAnnotation(method, PatchMapping.class);
        if (patchMapping != null) {
            routes.add(new RouteDescriptor("PATCH", expandPaths(pickPaths(patchMapping.path(), patchMapping.value()))));
        }
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (requestMapping != null && routes.isEmpty()) {
            List<String> methods = requestMapping.method().length == 0
                    ? List.of("REQUEST")
                    : Arrays.stream(requestMapping.method()).map(Enum::name).toList();
            List<String> paths = expandPaths(pickPaths(requestMapping.path(), requestMapping.value()));
            for (String httpMethod : methods) {
                routes.add(new RouteDescriptor(httpMethod, paths));
            }
        }
        return List.copyOf(routes);
    }

    private String[] pickPaths(String[] path, String[] value) {
        return path != null && path.length > 0 ? path : value;
    }

    private List<String> expandPaths(String[] paths) {
        if (paths == null || paths.length == 0) {
            return List.of("");
        }
        return Arrays.stream(paths)
                .map(path -> path == null ? "" : path.trim())
                .toList();
    }

    private String joinPaths(String classPath, String methodPath) {
        String base = classPath == null ? "" : classPath.trim();
        String method = methodPath == null ? "" : methodPath.trim();
        if (base.isEmpty()) {
            return normalize(method);
        }
        if (method.isEmpty()) {
            return normalize(base);
        }
        return normalize(base + "/" + method);
    }

    private String normalize(String path) {
        String normalized = path.replaceAll("/+", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private record RouteDescriptor(String httpMethod, List<String> paths) {
    }
}
