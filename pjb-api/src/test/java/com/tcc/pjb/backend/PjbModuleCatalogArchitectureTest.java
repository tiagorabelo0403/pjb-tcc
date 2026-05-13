package com.tcc.pjb.backend;

import com.tcc.pjb.backend.core.modularity.PjbModuleCatalog;
import com.tcc.pjb.backend.core.modularity.PjbModuleDescriptor;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.modularity.PjbPublicApi;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PjbModuleCatalogArchitectureTest {

    @Test
    void publicApisDevemResidirNasRaizesDoModuloDeclarado() {
        List<Class<?>> publicApis = List.of(
                com.tcc.pjb.backend.service.security.surface.SecuritySurfaceFacadeService.class,
                com.tcc.pjb.backend.service.AjuizamentoService.class,
                com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine.class,
                com.tcc.pjb.backend.service.processual.surface.ProcessoSurfaceFacadeService.class,
                com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService.class
        );

        for (Class<?> type : publicApis) {
            PjbPublicApi annotation = type.getAnnotation(PjbPublicApi.class);
            assertThat(annotation).as(type.getName()).isNotNull();
            PjbModuleDescriptor descriptor = PjbModuleCatalog.descriptor(annotation.module());
            assertThat(annotation.module().ownsPackage(type.getPackageName()))
                    .as(type.getName() + " deve estar dentro de " + annotation.module())
                    .isTrue();
            assertThat(descriptor.publicEntryPoints())
                    .anyMatch(entry -> type.getPackageName().contains(entry) || type.getSimpleName().toLowerCase().contains(entry.replace('.', '-')));
        }
    }

    @Test
    void catalogoDeModulosDeveExporRaizesEEntrypointsConsistentes() {
        for (PjbModuleId moduleId : PjbModuleId.values()) {
            PjbModuleDescriptor descriptor = PjbModuleCatalog.descriptor(moduleId);
            assertThat(descriptor).isNotNull();
            assertThat(descriptor.ownedPackageRoots()).isNotEmpty();
            assertThat(descriptor.publicEntryPoints()).isNotEmpty();
            assertThat(descriptor.tags()).isNotEmpty();
        }
    }
}
