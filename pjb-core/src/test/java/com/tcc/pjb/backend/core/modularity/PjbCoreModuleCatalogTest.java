package com.tcc.pjb.backend.core.modularity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PjbCoreModuleCatalogTest {

    @Test
    void catalogoDeveManterRootsCanonicasEEntrypointsCoerentes() {
        for (PjbModuleId moduleId : PjbModuleId.values()) {
            PjbModuleDescriptor descriptor = PjbModuleCatalog.descriptor(moduleId);
            assertThat(descriptor).isNotNull();
            assertThat(descriptor.id()).isEqualTo(moduleId);
            assertThat(descriptor.ownedPackageRoots()).allMatch(moduleId::ownsPackage);
            assertThat(descriptor.publicEntryPoints()).isNotEmpty();
            assertThat(descriptor.tags()).isNotEmpty();
        }
    }

    @Test
    void resolucaoDePacoteDeveSerDeterministicaEPreferirRootMaisEspecifica() {
        assertThat(PjbModuleCatalog.resolveByPackage("com.tcc.pjb.backend.core.processual.routing.queue"))
                .map(PjbModuleDescriptor::id)
                .contains(PjbModuleId.COMPETENCIA_ROTEAMENTO);
        assertThat(PjbModuleCatalog.resolveByPackage("com.tcc.pjb.backend.core.engine.lifecycle.dispatch"))
                .map(PjbModuleDescriptor::id)
                .contains(PjbModuleId.PROCESSO_LIFECYCLE);
        assertThat(PjbModuleCatalog.resolveByPackage("com.tcc.pjb.backend.ai.skills.v1"))
                .map(PjbModuleDescriptor::id)
                .contains(PjbModuleId.TRIAGEM_CANONIZACAO);
    }

    @Test
    void moduloIdDeveResolverPorCodeOuNomeSemCriarBuscaLinear() {
        assertThat(PjbModuleId.fromCode("ajuizamento")).contains(PjbModuleId.AJUIZAMENTO);
        assertThat(PjbModuleId.fromCode("processo_lifecycle")).contains(PjbModuleId.PROCESSO_LIFECYCLE);
        assertThat(PjbModuleId.fromCode("  ")).isEmpty();
    }

    @Test
    void descriptorDeveExporOperacoesSemAmbiguidade() {
        PjbModuleDescriptor descriptor = PjbModuleCatalog.descriptor(PjbModuleId.COMUNICACOES);
        assertThat(descriptor.exposesEntryPoint("core.comunicacao")).isTrue();
        assertThat(descriptor.hasTag("chat")).isTrue();
        assertThat(descriptor.ownedPackageRoots()).isSortedAccordingTo((left, right) -> {
            int lengthComparison = Integer.compare(right.length(), left.length());
            return lengthComparison != 0 ? lengthComparison : left.compareTo(right);
        });
    }

    @Test
    void modulesFaseUmDevemPermanecerPresentesParaExtracaoReal() {
        Set<PjbModuleId> faseUm = Set.of(
                PjbModuleId.IDENTIDADE_SEGURANCA,
                PjbModuleId.PROCESSO_LIFECYCLE,
                PjbModuleId.COMUNICACOES
        );
        assertThat(PjbModuleCatalog.all())
                .extracting(PjbModuleDescriptor::id)
                .containsAll(faseUm);
    }

    @Test
    void catalogoDevePreservarOrdemDeterministicaDosModulos() {
        assertThat(PjbModuleCatalog.all())
                .extracting(PjbModuleDescriptor::id)
                .containsExactlyElementsOf(List.of(PjbModuleId.values()));
    }
}
