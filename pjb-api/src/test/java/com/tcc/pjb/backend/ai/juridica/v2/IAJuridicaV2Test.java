package com.tcc.pjb.backend.ai.juridica.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.core.IAPipelineContext;
import com.tcc.pjb.backend.ai.juridica.v1.IAJuridicaV1;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaLegalAiSpineService;
import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector;
import com.tcc.pjb.backend.service.rito.RitoPackService;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IAJuridicaV2Test {

    @Test
    void writesSelectionMetadataIntoResponse() {
        IAJuridicaV1 juridicaV1 = mock(IAJuridicaV1.class);
        RitoPackService ritoPackService = mock(RitoPackService.class);
        CanonicalRitoSelector selector = mock(CanonicalRitoSelector.class);
        JuridicaUnifiedMeshProfileService meshService = mock(JuridicaUnifiedMeshProfileService.class);
        JuridicaLegalAiSpineService spineService = mock(JuridicaLegalAiSpineService.class);
        IAJuridicaV2 service = new IAJuridicaV2(juridicaV1, ritoPackService, meshService, spineService, selector);

        IAResponse base = IAResponse.builder()
                .origem("JURIDICA_V1")
                .status(IAResponse.StatusIA.SUCESSO)
                .texto("base")
                .metadados(Map.of("materia", "CIVEL"))
                .dataGeracao(Instant.now())
                .build();
        when(juridicaV1.processar(any(IAPipelineContext.class))).thenReturn(base);

        var mesh = new com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiMeshProfileResponse(
                "BALANCED", "V2", "JURIDICA_V2", java.util.List.of(), java.util.List.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.List.of()
        );
        var spine = new com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiSpineProfileResponse(
                "LEGAL_AI_SPINE", "V2", "JURIDICA_V2", java.util.Map.of(), java.util.List.of(), java.util.List.of(),
                new com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiRetrievalDescriptor("PIPE", java.util.List.of(), true, false, java.util.Map.of()),
                new com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiMemoryScopeDescriptor(java.util.List.of(), true, true, java.util.Map.of()),
                new com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiValidationDescriptor(java.util.List.of(), true, true, java.util.Map.of()),
                new com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiGraphDescriptor(true, java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of()),
                new com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiMultimodalDescriptor(java.util.List.of(), false, true, java.util.Map.of()),
                new com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiEvaluationDescriptor(java.util.List.of(), java.util.List.of(), true, java.util.Map.of()),
                new com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiHallucinationGuardDescriptor(false, false, false, false, "STRICT", "", java.util.List.of(), java.util.List.of(), java.util.Map.of()),
                new com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiTraceDescriptor(true, "TRACE", java.util.List.of(), java.util.Map.of()),
                new com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiApprovalDescriptor(false, false, java.util.List.of(), java.util.Map.of())
        );
        when(meshService.resolveForIa(any(), any(), anyString(), any(), any(), any())).thenReturn(mesh);
        when(spineService.resolveForIa(any(), any(), anyString())).thenReturn(spine);

        var selected = new CanonicalRitoSelector.SelectedRito(
                Instant.now(),
                "test",
                null,
                null,
                com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.COMUM_ORDINARIO,
                "CANONICAL_RITO_RESOLVED",
                false,
                false,
                Map.of("effectiveRito", "COMUM_ORDINARIO")
        );
        when(selector.select(any(), anyString(), anyString())).thenReturn(selected);
        when(ritoPackService.get(com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.COMUM_ORDINARIO)).thenReturn(Optional.of(
                RitoDefinition.builder()
                        .rito("COMUM_ORDINARIO")
                        .title("Procedimento Comum")
                        .ramoSugerido("CIVIL")
                        .stages(List.of(RitoStage.builder().fase("POSTULATORIA").allowedNext(List.of("SANEAMENTO")).work(List.of()).build()))
                        .build()
        ));

        IARequest request = IARequest.builder()
                .withOrigem("test")
                .withAcao("analise")
                .withPayload(Map.of("rito", "COMUM_ORDINARIO", "materia", "CIVEL"))
                .build();

        IAResponse response = service.processar(request);

        assertEquals("JURIDICA_V2", response.getOrigem());
        assertNotNull(response.getEssence().get("rito_selection"));
        assertNotNull(response.getMetadados().get("rito_selection"));
    }
}
