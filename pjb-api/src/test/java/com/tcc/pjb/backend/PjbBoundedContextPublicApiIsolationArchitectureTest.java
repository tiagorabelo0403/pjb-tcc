package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.modularity.PjbModuleCatalog;
import com.tcc.pjb.backend.core.modularity.PjbModuleDescriptor;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.modularity.PjbPublicApi;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PjbBoundedContextPublicApiIsolationArchitectureTest {

    private static final String BASE_PACKAGE = "com.tcc.pjb.backend";
    private static JavaClasses classes;

    @BeforeAll
    static void load() {
        classes = new ClassFileImporter().importPackages(BASE_PACKAGE);
    }

    @Test
    void publicApisNaoDevemAcoplarEntrypointsPublicosDeOutrosBoundedContexts() {
        List<String> violations = new ArrayList<>();
        for (JavaClass source : classes) {
            PjbPublicApi publicApi = readPublicApi(source);
            if (publicApi == null) {
                continue;
            }
            PjbModuleId sourceModule = publicApi.module();
            for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                if (!target.getPackageName().startsWith(BASE_PACKAGE) || target.equals(source)) {
                    continue;
                }
                if (isSharedDependency(target.getPackageName())) {
                    continue;
                }
                Optional<PjbModuleDescriptor> targetDescriptor = PjbModuleCatalog.resolveByPackage(target.getPackageName());
                if (targetDescriptor.isEmpty() || targetDescriptor.get().id() == sourceModule) {
                    continue;
                }
                if (touchesPublicEntrypoint(target.getPackageName(), targetDescriptor.get())) {
                    violations.add(source.getFullName() + " -> " + target.getFullName());
                }
            }
        }
        assertThat(violations)
                .as("As public APIs devem expor fronteiras estáveis sem acoplar entrypoints públicos de outros módulos")
                .isEmpty();
    }

    private boolean touchesPublicEntrypoint(String packageName, PjbModuleDescriptor descriptor) {
        return descriptor.publicEntryPoints().stream().anyMatch(packageName::contains);
    }

    private boolean isSharedDependency(String packageName) {
        return packageName.startsWith("com.tcc.pjb.backend.model.")
                || packageName.startsWith("com.tcc.pjb.backend.configs.")
                || packageName.startsWith("com.tcc.pjb.backend.core.modularity")
                || packageName.startsWith("com.tcc.pjb.backend.service.exception")
                || packageName.startsWith("com.tcc.pjb.backend.service.outbox")
                || packageName.startsWith("com.tcc.pjb.backend.service.triagem")
                || packageName.startsWith("com.tcc.pjb.backend.core.audit")
                || packageName.startsWith("com.tcc.pjb.backend.core.util")
                || packageName.startsWith("com.tcc.pjb.backend.core.ownership");
    }

    private PjbPublicApi readPublicApi(JavaClass source) {
        return source.tryGetAnnotationOfType(PjbPublicApi.class).orElse(null);
    }
}
