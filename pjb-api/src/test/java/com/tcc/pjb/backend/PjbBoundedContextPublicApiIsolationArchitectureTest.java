package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.modularity.PjbModuleCatalog;
import com.tcc.pjb.backend.core.modularity.PjbModuleDescriptor;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.modularity.PjbPublicApi;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class PjbBoundedContextPublicApiIsolationArchitectureTest {

    private static final String BASE_PACKAGE = "com.tcc.pjb.backend";

    @ArchTest
    static void publicApisNaoDevemAcoplarEntrypointsPublicosDeOutrosBoundedContexts(JavaClasses classes) {
        List<String> violations = new ArrayList<>();
        int publicApisInspecionadas = 0;
        for (JavaClass source : classes) {
            PjbPublicApi publicApi = readPublicApi(source);
            if (publicApi == null) {
                continue;
            }
            publicApisInspecionadas++;
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
        assertThat(publicApisInspecionadas)
                .as("nenhuma classe anotada com @PjbPublicApi foi encontrada: a regra abaixo passaria sem verificar nada")
                .isPositive();
        assertThat(violations)
                .as("As public APIs devem expor fronteiras estáveis sem acoplar entrypoints públicos de outros módulos")
                .isEmpty();
    }

    private static boolean touchesPublicEntrypoint(String packageName, PjbModuleDescriptor descriptor) {
        return descriptor.publicEntryPoints().stream().anyMatch(packageName::contains);
    }

    private static boolean isSharedDependency(String packageName) {
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

    private static PjbPublicApi readPublicApi(JavaClass source) {
        return source.tryGetAnnotationOfType(PjbPublicApi.class).orElse(null);
    }
}
