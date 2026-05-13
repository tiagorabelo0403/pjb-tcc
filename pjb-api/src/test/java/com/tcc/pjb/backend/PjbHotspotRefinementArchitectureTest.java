package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.ai.juridica.v3.core.LegalDraftingService;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalDraftPreviewAssembler;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalProjectionAssembler;
import com.tcc.pjb.backend.service.processual.recursal.RecursalPeticionamentoFacadeService;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PjbHotspotRefinementArchitectureTest {

    @Test
    void recursalFacadeDeveDelegarDraftECarryOverParaAssemblersDedicados() {
        Constructor<?> constructor = Arrays.stream(RecursalPeticionamentoFacadeService.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();
        Set<Class<?>> parameterTypes = Set.of(constructor.getParameterTypes());

        assertThat(constructor.getParameterCount()).isLessThanOrEqualTo(17);
        assertThat(parameterTypes)
                .contains(RecursalDraftPreviewAssembler.class, RecursalProjectionAssembler.class)
                .doesNotContain(LegalDraftingService.class, DocumentoProcessualRepository.class, DocumentoPaginaRepository.class);
    }

    @Test
    void recursalFacadeNaoDeveReabsorverHelpersDePerfilEClassificacao() {
        Set<String> methodNames = Arrays.stream(RecursalPeticionamentoFacadeService.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertThat(methodNames)
                .doesNotContain(
                        "descriptorOf",
                        "resolveSpeciesType",
                        "resolveEmbargosGrounds",
                        "buildPeticaoDescricao",
                        "buildFundamentacao",
                        "buildHistoryMessage",
                        "inferTipoJustica",
                        "inferTribunalDetalhado",
                        "inferInstanceLevel",
                        "inferOrgaoProlator",
                        "inferDataIntimacao",
                        "inferTribunalCodigo",
                        "stableActorKey",
                        "safeNumeroProcesso"
                );
    }

    @Test
    void assemblersDedicadosDevemConcentrarDependenciasEspecializadas() {
        Constructor<?> draftConstructor = Arrays.stream(RecursalDraftPreviewAssembler.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();
        Constructor<?> projectionConstructor = Arrays.stream(RecursalProjectionAssembler.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        assertThat(toTypeSet(draftConstructor)).containsExactly(LegalDraftingService.class);
        assertThat(toTypeSet(projectionConstructor)).containsExactlyInAnyOrder(DocumentoProcessualRepository.class, DocumentoPaginaRepository.class);
    }

    private Set<Class<?>> toTypeSet(Constructor<?> constructor) {
        return Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());
    }
}
