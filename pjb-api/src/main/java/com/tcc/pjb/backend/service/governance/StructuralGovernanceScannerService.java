package com.tcc.pjb.backend.service.governance;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import com.tcc.pjb.backend.model.dto.governance.StructuralAutoRemediationReportResponse;
import com.tcc.pjb.backend.model.dto.governance.StructuralGovernanceReportResponse;
import jakarta.validation.Valid;

@Service
public class StructuralGovernanceScannerService {

    private final ApplicationContext applicationContext;
    private final ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider;

    public StructuralGovernanceScannerService(ApplicationContext applicationContext,
                                              ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider) {
        this.applicationContext = Objects.requireNonNull(applicationContext);
        this.handlerMappingProvider = Objects.requireNonNull(handlerMappingProvider);
    }

    public StructuralGovernanceReportResponse scan() {
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);
        Map<String, Object> services = applicationContext.getBeansWithAnnotation(Service.class);
        Map<String, Object> repositories = applicationContext.getBeansWithAnnotation(Repository.class);
        List<String> controllersSemPreAuthorize = new ArrayList<>();
        List<String> controllersSemRequestMapping = new ArrayList<>();
        controllers.forEach((beanName, bean) -> {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            if (targetClass == null) {
                return;
            }
            boolean preAuthorize = targetClass.isAnnotationPresent(PreAuthorize.class)
                    || hasMethodAnnotation(targetClass, PreAuthorize.class);
            boolean requestMapping = targetClass.isAnnotationPresent(RequestMapping.class)
                    || hasMethodRequestMapping(targetClass);
            if (!preAuthorize) {
                controllersSemPreAuthorize.add(targetClass.getName());
            }
            if (!requestMapping) {
                controllersSemRequestMapping.add(targetClass.getName());
            }
        });
        List<String> destaques = List.of(
                "Controllers auditados com foco em segurança declarativa e surface HTTP.",
                "Serviços auditados por presença de bean operacional no contexto Spring.",
                "Repositórios auditados por exposição institucional no contexto da aplicação."
        );
        return new StructuralGovernanceReportResponse(
                controllers.size(),
                services.size(),
                repositories.size(),
                controllersSemPreAuthorize.size(),
                controllersSemRequestMapping.size(),
                List.copyOf(controllersSemPreAuthorize),
                List.copyOf(controllersSemRequestMapping),
                destaques
        );
    }

    public StructuralAutoRemediationReportResponse scanDetailed() {
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);
        Map<String, Object> services = applicationContext.getBeansWithAnnotation(Service.class);
        RequestMappingHandlerMapping handlerMapping = handlerMappingProvider.getIfAvailable();
        if (handlerMapping == null) {
            return new StructuralAutoRemediationReportResponse(
                    controllers.size(),
                    services.size(),
                    0,
                    0,
                    0,
                    0,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of("Contexto sem MVC detectado; auditoria de rotas HTTP adiada sem impedir bootstrap não-web.")
            );
        }
        Map<String, List<String>> pathOwners = new LinkedHashMap<>();
        List<String> rawResponseEndpointOwners = new ArrayList<>();
        List<String> requestBodiesMissingValidation = new ArrayList<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> inspectHandler(mapping, handler, pathOwners, rawResponseEndpointOwners, requestBodiesMissingValidation));
        List<String> duplicatePathMappings = pathOwners.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> entry.getKey() + " -> " + String.join(" | ", new LinkedHashSet<>(entry.getValue())))
                .toList();
        List<String> servicesWithoutController = resolveProcessualServicesWithoutController(services, controllers);
        List<String> remediationPriorities = buildPriorities(duplicatePathMappings, rawResponseEndpointOwners, requestBodiesMissingValidation, servicesWithoutController);
        return new StructuralAutoRemediationReportResponse(
                controllers.size(),
                services.size(),
                duplicatePathMappings.size(),
                rawResponseEndpointOwners.size(),
                requestBodiesMissingValidation.size(),
                servicesWithoutController.size(),
                duplicatePathMappings,
                List.copyOf(rawResponseEndpointOwners),
                List.copyOf(requestBodiesMissingValidation),
                List.copyOf(servicesWithoutController),
                remediationPriorities
        );
    }

    private void inspectHandler(RequestMappingInfo mapping,
                                HandlerMethod handler,
                                Map<String, List<String>> pathOwners,
                                List<String> rawResponseEndpointOwners,
                                List<String> requestBodiesMissingValidation) {
        Class<?> beanType = handler.getBeanType();
        Set<String> patterns = mapping.getPatternValues();
        if (patterns.isEmpty()) {
            patterns = Set.of("[NO_PATTERN]");
        }
        String owner = beanType.getName() + "#" + handler.getMethod().getName();
        for (String pattern : patterns) {
            pathOwners.computeIfAbsent(pattern, key -> new ArrayList<>()).add(owner);
        }
        if (isRawResponseType(handler.getMethod().getReturnType())) {
            rawResponseEndpointOwners.add(owner + " -> " + handler.getMethod().getReturnType().getSimpleName());
        }
        for (Parameter parameter : handler.getMethod().getParameters()) {
            if (parameter.isAnnotationPresent(RequestBody.class)
                    && !parameter.isAnnotationPresent(Valid.class)
                    && !parameter.isAnnotationPresent(Validated.class)) {
                requestBodiesMissingValidation.add(owner + " -> " + parameter.getType().getName());
            }
        }
    }

    private List<String> resolveProcessualServicesWithoutController(Map<String, Object> services,
                                                                    Map<String, Object> controllers) {
        Set<String> controllerBases = new LinkedHashSet<>();
        controllers.values().forEach(bean -> {
            Class<?> type = AopUtils.getTargetClass(bean);
            if (type != null) {
                controllerBases.add(baseName(type.getSimpleName(), "Controller"));
            }
        });
        List<String> missing = new ArrayList<>();
        services.values().forEach(bean -> {
            Class<?> type = AopUtils.getTargetClass(bean);
            if (type == null) {
                return;
            }
            Package servicePackage = type.getPackage();
            String packageName = servicePackage == null ? "" : servicePackage.getName();
            if (!packageName.contains(".service.processual.")) {
                return;
            }
            String base = baseName(type.getSimpleName(), "Service");
            if (!controllerBases.contains(base)) {
                missing.add(type.getName());
            }
        });
        return List.copyOf(missing);
    }

    private List<String> buildPriorities(List<String> duplicatePathMappings,
                                         List<String> rawResponseEndpointOwners,
                                         List<String> requestBodiesMissingValidation,
                                         List<String> servicesWithoutController) {
        List<String> out = new ArrayList<>();
        if (!duplicatePathMappings.isEmpty()) {
            out.add("Sanear rotas HTTP duplicadas antes de novas expansões de controllers.");
        }
        if (!requestBodiesMissingValidation.isEmpty()) {
            out.add("Tipar e validar todos os request bodies expostos sem @Valid ou @Validated.");
        }
        if (!rawResponseEndpointOwners.isEmpty()) {
            out.add("Padronizar endpoints que retornam tipos crus em envelopes institucionais.");
        }
        if (!servicesWithoutController.isEmpty()) {
            out.add("Revisar serviços processuais sem surface HTTP para decidir exposição controlada ou uso interno explícito.");
        }
        if (out.isEmpty()) {
            out.add("Estrutura HTTP e governança declarativa sem achados críticos no snapshot atual.");
        }
        return List.copyOf(out);
    }

    private boolean isRawResponseType(Class<?> type) {
        return type == String.class
                || Map.class.isAssignableFrom(type)
                || List.class.isAssignableFrom(type);
    }

    private String baseName(String simpleName, String suffix) {
        if (simpleName == null || simpleName.isBlank()) {
            return "";
        }
        return simpleName.endsWith(suffix)
                ? simpleName.substring(0, simpleName.length() - suffix.length())
                : simpleName;
    }

    private boolean hasMethodAnnotation(Class<?> targetClass, Class<PreAuthorize> annotationClass) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationClass)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMethodRequestMapping(Class<?> targetClass) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(RequestMapping.class)
                    || method.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class)
                    || method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)
                    || method.isAnnotationPresent(org.springframework.web.bind.annotation.PutMapping.class)
                    || method.isAnnotationPresent(org.springframework.web.bind.annotation.DeleteMapping.class)
                    || method.isAnnotationPresent(org.springframework.web.bind.annotation.PatchMapping.class)) {
                return true;
            }
        }
        return false;
    }
}
