package com.tcc.pjb.backend.core.modularity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public enum PjbModuleId {

    IDENTIDADE_SEGURANCA("identidade-seguranca", "Identidade e Segurança", List.of(
            "com.tcc.pjb.backend.core.identity",
            "com.tcc.pjb.backend.core.identidade",
            "com.tcc.pjb.backend.configs.security",
            "com.tcc.pjb.backend.controller.auth",
            "com.tcc.pjb.backend.controller.security",
            "com.tcc.pjb.backend.service.security.surface"
    )),
    AJUIZAMENTO("ajuizamento", "Ajuizamento", List.of(
            "com.tcc.pjb.backend.command",
            "com.tcc.pjb.backend.core.compiler",
            "com.tcc.pjb.backend.controller.processo",
            "com.tcc.pjb.backend.service"
    )),
    TRIAGEM_CANONIZACAO("triagem-canonizacao", "Triagem e Canonização", List.of(
            "com.tcc.pjb.backend.core.catalog",
            "com.tcc.pjb.backend.core.engine",
            "com.tcc.pjb.backend.service.intelligence",
            "com.tcc.pjb.backend.service.triagem",
            "com.tcc.pjb.backend.ai"
    )),
    COMPETENCIA_ROTEAMENTO("competencia-roteamento", "Competência e Roteamento", List.of(
            "com.tcc.pjb.backend.core.competence",
            "com.tcc.pjb.backend.core.distribuicao",
            "com.tcc.pjb.backend.core.forum.routing",
            "com.tcc.pjb.backend.core.procedural",
            "com.tcc.pjb.backend.core.processual.routing",
            "com.tcc.pjb.backend.controller.distribuicao"
    )),
    PROCESSO_LIFECYCLE("processo-lifecycle", "Processo e Lifecycle", List.of(
            "com.tcc.pjb.backend.core.engine.lifecycle",
            "com.tcc.pjb.backend.service.processo",
            "com.tcc.pjb.backend.model.entity",
            "com.tcc.pjb.backend.model.repository",
            "com.tcc.pjb.backend.service.processual.surface"
    )),
    DOCUMENTOS("documentos", "Documentos", List.of(
            "com.tcc.pjb.backend.core.engine.document",
            "com.tcc.pjb.backend.controller.processual",
            "com.tcc.pjb.backend.service.documento"
    )),
    COMUNICACOES("comunicacoes", "Comunicações", List.of(
            "com.tcc.pjb.backend.core.comunicacao",
            "com.tcc.pjb.backend.controller.notification",
            "com.tcc.pjb.backend.controller.secretariat",
            "com.tcc.pjb.backend.modules.atendimento",
            "com.tcc.pjb.backend.modules.balcao",
            "com.tcc.pjb.backend.modules.suporte"
    )),
    PRAZOS_AGENDA("prazos-agenda", "Prazos e Agenda", List.of(
            "com.tcc.pjb.backend.controller.calendar",
            "com.tcc.pjb.backend.service.calendar",
            "com.tcc.pjb.backend.controller.audiencia",
            "com.tcc.pjb.backend.service.audiencia",
            "com.tcc.pjb.backend.core.kernel.recursal"
    )),
    INTEGRACOES_EXTERNAS("integracoes-externas", "Integrações Externas", List.of(
            "com.tcc.pjb.backend.integration",
            "com.tcc.pjb.backend.adapter",
            "com.tcc.pjb.backend.configs.kafka"
    )),
    AUDITORIA_OBSERVABILIDADE("auditoria-observabilidade", "Auditoria e Observabilidade", List.of(
            "com.tcc.pjb.backend.core.audit",
            "com.tcc.pjb.backend.core.observability",
            "com.tcc.pjb.backend.controller.admin"
    )),
    INTELIGENCIA_APLICADA("inteligencia-aplicada", "Inteligência Aplicada", List.of(
            "com.tcc.pjb.backend.ai",
            "com.tcc.pjb.backend.legal.skills",
            "com.tcc.pjb.backend.financial.ai"
    ));

    private static final Map<String, PjbModuleId> LOOKUP = buildLookup();

    private final String code;
    private final String displayName;
    private final List<String> ownedPackageRoots;

    PjbModuleId(String code, String displayName, List<String> ownedPackageRoots) {
        this.code = requireText(code, "code");
        this.displayName = requireText(displayName, "displayName");
        this.ownedPackageRoots = normalizePackageRoots(ownedPackageRoots);
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public List<String> ownedPackageRoots() {
        return ownedPackageRoots;
    }

    public boolean ownsPackage(String packageName) {
        String normalizedPackage = normalizePackageName(packageName);
        return normalizedPackage != null && ownsNormalizedPackage(normalizedPackage);
    }

    public static Optional<PjbModuleId> fromCode(String code) {
        String normalizedLookupKey = normalizeLookupKey(code);
        return normalizedLookupKey == null
                ? Optional.empty()
                : Optional.ofNullable(LOOKUP.get(normalizedLookupKey));
    }

    boolean ownsNormalizedPackage(String normalizedPackage) {
        for (int i = 0; i < ownedPackageRoots.size(); i++) {
            String root = ownedPackageRoots.get(i);
            if (normalizedPackage.equals(root) || normalizedPackage.startsWith(root + ".")) {
                return true;
            }
        }
        return false;
    }

    static String normalizePackageName(String packageName) {
        if (packageName == null) {
            return null;
        }
        String normalized = packageName.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Map<String, PjbModuleId> buildLookup() {
        LinkedHashMap<String, PjbModuleId> lookup = new LinkedHashMap<>();
        for (PjbModuleId moduleId : values()) {
            registerLookupAlias(lookup, moduleId.code, moduleId);
            registerLookupAlias(lookup, moduleId.name(), moduleId);
        }
        return Map.copyOf(lookup);
    }

    private static void registerLookupAlias(Map<String, PjbModuleId> lookup, String alias, PjbModuleId moduleId) {
        String normalizedAlias = normalizeLookupKey(alias);
        if (normalizedAlias == null) {
            throw new IllegalArgumentException("Lookup alias must not be blank");
        }
        PjbModuleId existing = lookup.putIfAbsent(normalizedAlias, moduleId);
        if (existing != null && existing != moduleId) {
            throw new IllegalStateException("Duplicate module lookup alias: " + alias);
        }
    }

    private static List<String> normalizePackageRoots(List<String> ownedPackageRoots) {
        Objects.requireNonNull(ownedPackageRoots, "ownedPackageRoots");
        LinkedHashMap<String, String> uniqueRoots = new LinkedHashMap<>();
        for (int i = 0; i < ownedPackageRoots.size(); i++) {
            String root = normalizePackageName(ownedPackageRoots.get(i));
            if (root == null) {
                throw new IllegalArgumentException("ownedPackageRoots must not contain blank values");
            }
            uniqueRoots.putIfAbsent(root, root);
        }
        if (uniqueRoots.isEmpty()) {
            throw new IllegalArgumentException("ownedPackageRoots must not be empty");
        }
        ArrayList<String> normalizedRoots = new ArrayList<>(uniqueRoots.values());
        normalizedRoots.sort((left, right) -> {
            int lengthComparison = Integer.compare(right.length(), left.length());
            return lengthComparison != 0 ? lengthComparison : left.compareTo(right);
        });
        return List.copyOf(normalizedRoots);
    }

    private static String normalizeLookupKey(String value) {
        String normalized = normalizePackageName(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
