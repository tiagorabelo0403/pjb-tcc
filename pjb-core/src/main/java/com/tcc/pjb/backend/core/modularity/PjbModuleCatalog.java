package com.tcc.pjb.backend.core.modularity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class PjbModuleCatalog {

    private static final CatalogState CATALOG = buildCatalog();
    private static final Map<PjbModuleId, PjbModuleDescriptor> DESCRIPTORS = CATALOG.descriptors();
    private static final List<PjbModuleDescriptor> ORDERED_DESCRIPTORS = CATALOG.orderedDescriptors();
    private static final List<PackageOwnership> PACKAGE_OWNERSHIP_INDEX = CATALOG.packageOwnershipIndex();

    private PjbModuleCatalog() {
    }

    public static Collection<PjbModuleDescriptor> all() {
        return ORDERED_DESCRIPTORS;
    }

    public static PjbModuleDescriptor descriptor(PjbModuleId id) {
        PjbModuleDescriptor descriptor = DESCRIPTORS.get(Objects.requireNonNull(id, "id"));
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown module id: " + id);
        }
        return descriptor;
    }

    public static Optional<PjbModuleDescriptor> resolveByPackage(String packageName) {
        String normalizedPackage = PjbModuleId.normalizePackageName(packageName);
        if (normalizedPackage == null) {
            return Optional.empty();
        }
        for (int i = 0; i < PACKAGE_OWNERSHIP_INDEX.size(); i++) {
            PackageOwnership ownership = PACKAGE_OWNERSHIP_INDEX.get(i);
            if (ownership.matches(normalizedPackage)) {
                return Optional.of(ownership.descriptor());
            }
        }
        return Optional.empty();
    }

    public static Set<String> publicEntryPoints(PjbModuleId id) {
        return descriptor(id).publicEntryPoints();
    }

    private static CatalogState buildCatalog() {
        EnumMap<PjbModuleId, PjbModuleDescriptor> descriptors = new EnumMap<>(PjbModuleId.class);
        ArrayList<PjbModuleDescriptor> orderedDescriptors = new ArrayList<>(PjbModuleId.values().length);

        orderedDescriptors.add(register(descriptors, define(
                PjbModuleId.IDENTIDADE_SEGURANCA,
                Set.of("controller.auth", "controller.security", "core.identity", "service.security.surface"),
                Set.of("auth", "security", "identity")
        )));
        orderedDescriptors.add(register(descriptors, define(
                PjbModuleId.AJUIZAMENTO,
                Set.of("command", "controller.processo", "core.compiler", "service"),
                Set.of("ajuizamento", "entrada", "peticao")
        )));
        orderedDescriptors.add(register(descriptors, define(
                PjbModuleId.TRIAGEM_CANONIZACAO,
                Set.of("core.catalog", "core.engine", "service.triagem", "ai"),
                Set.of("triagem", "canonizacao", "classificacao")
        )));
        orderedDescriptors.add(register(descriptors, define(
                PjbModuleId.COMPETENCIA_ROTEAMENTO,
                Set.of("core.competence", "core.distribuicao", "core.procedural", "core.processual.routing"),
                Set.of("competencia", "roteamento", "distribuicao")
        )));
        orderedDescriptors.add(register(descriptors, define(
                PjbModuleId.PROCESSO_LIFECYCLE,
                Set.of("service.processo", "service.processual.surface", "core.engine.lifecycle", "model.entity", "model.repository"),
                Set.of("processo", "lifecycle", "estado")
        )));
        orderedDescriptors.add(register(descriptors, define(
                PjbModuleId.DOCUMENTOS,
                Set.of("core.engine.document", "controller.processual"),
                Set.of("documentos", "arquivo", "juntada")
        )));
        orderedDescriptors.add(register(descriptors, define(
                PjbModuleId.COMUNICACOES,
                Set.of("core.comunicacao", "controller.notification", "modules.atendimento"),
                Set.of("notificacao", "chat", "comunicacao")
        )));
        orderedDescriptors.add(register(descriptors, define(
                PjbModuleId.PRAZOS_AGENDA,
                Set.of("controller.calendar", "service.calendar", "core.kernel.recursal"),
                Set.of("prazos", "agenda", "recursal")
        )));
        orderedDescriptors.add(register(descriptors, define(
                PjbModuleId.INTEGRACOES_EXTERNAS,
                Set.of("integration", "adapter", "configs.kafka"),
                Set.of("integracao", "outbox", "mensageria")
        )));
        orderedDescriptors.add(register(descriptors, define(
                PjbModuleId.AUDITORIA_OBSERVABILIDADE,
                Set.of("core.audit", "core.observability", "controller.admin"),
                Set.of("auditoria", "observabilidade", "ledger")
        )));
        orderedDescriptors.add(register(descriptors, define(
                PjbModuleId.INTELIGENCIA_APLICADA,
                Set.of("ai", "legal.skills", "financial.ai"),
                Set.of("ai", "jurimetria", "explicabilidade")
        )));

        return new CatalogState(
                Map.copyOf(descriptors),
                List.copyOf(orderedDescriptors),
                buildPackageOwnershipIndex(orderedDescriptors)
        );
    }

    private static List<PackageOwnership> buildPackageOwnershipIndex(List<PjbModuleDescriptor> descriptors) {
        ArrayList<PackageOwnership> ownerships = new ArrayList<>();
        for (int descriptorIndex = 0; descriptorIndex < descriptors.size(); descriptorIndex++) {
            PjbModuleDescriptor descriptor = descriptors.get(descriptorIndex);
            for (int rootIndex = 0; rootIndex < descriptor.ownedPackageRoots().size(); rootIndex++) {
                ownerships.add(new PackageOwnership(descriptor.ownedPackageRoots().get(rootIndex), descriptor));
            }
        }
        ownerships.sort((left, right) -> {
            int lengthComparison = Integer.compare(right.root().length(), left.root().length());
            if (lengthComparison != 0) {
                return lengthComparison;
            }
            int moduleComparison = Integer.compare(left.descriptor().id().ordinal(), right.descriptor().id().ordinal());
            return moduleComparison != 0 ? moduleComparison : left.root().compareTo(right.root());
        });
        return List.copyOf(ownerships);
    }

    private static PjbModuleDescriptor register(Map<PjbModuleId, PjbModuleDescriptor> descriptors, PjbModuleDescriptor descriptor) {
        PjbModuleDescriptor previous = descriptors.putIfAbsent(descriptor.id(), descriptor);
        if (previous != null) {
            throw new IllegalStateException("Duplicate descriptor for module: " + descriptor.id());
        }
        return descriptor;
    }

    private static PjbModuleDescriptor define(PjbModuleId id, Set<String> entryPoints, Set<String> tags) {
        return new PjbModuleDescriptor(
                id,
                id.displayName(),
                id.ownedPackageRoots(),
                entryPoints,
                tags
        );
    }

    private record CatalogState(
            Map<PjbModuleId, PjbModuleDescriptor> descriptors,
            List<PjbModuleDescriptor> orderedDescriptors,
            List<PackageOwnership> packageOwnershipIndex
    ) {
    }

    private record PackageOwnership(String root, PjbModuleDescriptor descriptor) {

        private boolean matches(String packageName) {
            return packageName.equals(root) || packageName.startsWith(root + ".");
        }
    }
}
