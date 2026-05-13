package com.tcc.pjb.backend.contracts.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.spring.spring6.PactVerificationSpring6Provider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.tcc.pjb.backend.ai.juridica.api.LegalAiController;
import com.tcc.pjb.backend.ai.juridica.api.LegalAiConversationController;
import com.tcc.pjb.backend.ai.juridica.conversation.JuridicaLegalAiConversationService;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalDraftResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationResponse;
import com.tcc.pjb.backend.service.intelligence.surface.LegalAiSurfaceFacadeService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Provider("PjbLegalAiProvider")
@PactFolder("src/test/resources/pacts/provider")
class LegalAiControllerProviderContractTest {

    private final LegalAiSurfaceFacadeService surfaceFacadeService = mock(LegalAiSurfaceFacadeService.class);
    private final JuridicaLegalAiConversationService conversationService = mock(JuridicaLegalAiConversationService.class);
    private final LegalAiController controller = new LegalAiController(surfaceFacadeService);
    private final LegalAiConversationController conversationController = new LegalAiConversationController(conversationService);

    @BeforeEach
    void setUp(PactVerificationContext context) {
        reset(surfaceFacadeService, conversationService);
        PactProviderSpring6Support.configure(context, controller, conversationController);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpring6Provider.class)
    void verify(PactVerificationContext context, MockHttpServletRequestBuilder request) {
        PactProviderSpring6Support.applyJsonBody(context, request);
        context.verifyInteraction();
    }

    @State("legal ai minuta promoted response is available")
    void legalAiMinutaPromotedResponseIsAvailable() {
        when(surfaceFacadeService.minuta(any())).thenReturn(new LegalDraftResponse(
                "RELATÓRIO\nMinuta promovida por evidência oficial.",
                "PROMOTED",
                List.of("Evidência oficial soberana confirmada."),
                Map.of(
                        "surfaceStatus", "NOT_REQUIRED",
                        "draftPromotionStatus", "PROMOTED",
                        "promotedDraftEvidenceIds", List.of("CNJ-OK"),
                        "evidenceProvenanceTier", "OFFICIAL_DOCUMENT"
                )
        ));
    }

    @State("legal ai minuta step up response is available")
    void legalAiMinutaStepUpResponseIsAvailable() {
        when(surfaceFacadeService.minuta(any())).thenReturn(new LegalDraftResponse(
                "RELATÓRIO\n[PENDENTE_CONFIRMACAO_SOBERANA]",
                "STEP_UP_REQUIRED",
                List.of("Confirmar cadeia oficial antes da redação material."),
                Map.of(
                        "surfaceStatus", "ESCALATED",
                        "draftPromotionStatus", "STEP_UP_REQUIRED",
                        "promotedDraftEvidenceIds", List.of(),
                        "evidenceProvenanceTier", "DERIVED_DOCUMENT"
                )
        ));
    }

    @State("legal ai minuta blocked response is available")
    void legalAiMinutaBlockedResponseIsAvailable() {
        when(surfaceFacadeService.minuta(any())).thenReturn(new LegalDraftResponse(
                "RELATÓRIO\n[PROMOCAO_DOCUMENTAL_BLOQUEADA]",
                "BLOCKED",
                List.of("Remover anexo em quarentena e reenviar origem oficial."),
                Map.of(
                        "surfaceStatus", "LOCKED",
                        "draftPromotionStatus", "BLOCKED",
                        "promotedDraftEvidenceIds", List.of(),
                        "evidenceProvenanceTier", "UNTRUSTED_DOCUMENT"
                )
        ));
    }

    @State("legal ai grounding promoted response is available")
    void legalAiGroundingPromotedResponseIsAvailable() {
        when(surfaceFacadeService.hallucinationGuard(any())).thenReturn(new LegalHallucinationGuardResponse(
                "LEGAL_GROUNDING_CHECK_V3",
                "V3",
                "LEGAL_GROUNDING_CHECK_V3",
                "ALIGNED",
                true,
                true,
                true,
                "STRICT_OFFICIAL_ONLY",
                "[CITACAO_PENDENTE_VALIDACAO]",
                "NOT_REQUIRED",
                "OFFICIAL_DOCUMENT",
                "SOVEREIGN_PROMOTED",
                "PROMOTED",
                List.of(),
                List.of(),
                Map.of(
                        "surfaceStatus", "NOT_REQUIRED",
                        "promotedGroundingEvidenceIds", List.of("CNJ-OK")
                )
        ));
    }

    @State("legal ai grounding review required response is available")
    void legalAiGroundingReviewRequiredResponseIsAvailable() {
        when(surfaceFacadeService.hallucinationGuard(any())).thenReturn(new LegalHallucinationGuardResponse(
                "LEGAL_GROUNDING_CHECK_V3",
                "V3",
                "LEGAL_GROUNDING_CHECK_V3",
                "REVIEW_REQUIRED",
                true,
                true,
                true,
                "STRICT_OFFICIAL_ONLY",
                "[CITACAO_PENDENTE_VALIDACAO]",
                "ESCALATED",
                "DERIVED_DOCUMENT",
                "SOVEREIGN_STEP_UP",
                "STEP_UP_REQUIRED",
                List.of("A cadeia soberana exige confirmação oficial adicional."),
                List.of(),
                Map.of(
                        "surfaceStatus", "ESCALATED",
                        "promotedGroundingEvidenceIds", List.of()
                )
        ));
    }

    @State("legal ai grounding blocked response is available")
    void legalAiGroundingBlockedResponseIsAvailable() {
        when(surfaceFacadeService.hallucinationGuard(any())).thenReturn(new LegalHallucinationGuardResponse(
                "LEGAL_GROUNDING_CHECK_V3",
                "V3",
                "LEGAL_GROUNDING_CHECK_V3",
                "BLOCKED",
                true,
                true,
                true,
                "STRICT_OFFICIAL_ONLY",
                "[CITACAO_PENDENTE_VALIDACAO]",
                "LOCKED",
                "UNTRUSTED_DOCUMENT",
                "SOVEREIGN_PROVENANCE_HARD_LOCK",
                "BLOCKED",
                List.of(),
                List.of("Grounding estruturado foi bloqueado pela cerca soberana."),
                Map.of(
                        "surfaceStatus", "LOCKED",
                        "promotedGroundingEvidenceIds", List.of()
                )
        ));
    }
    @State("legal ai conversation promoted response is available")
    void legalAiConversationPromotedResponseIsAvailable() {
        when(conversationService.converse(any())).thenReturn(new LegalAiConversationResponse(
                "conv-promoted",
                "V3",
                "LEGAL_GENERAL_ASSIST_V3",
                "Resposta conversacional jurídica V3: analisando o pedido com malha unificada e lastro oficial confirmado.",
                List.of("Manter a conversa em modo read-only soberano enquanto o lastro oficial estiver íntegro."),
                List.of(Map.of("virtualTrend", "VT_OFFICIAL_GROUNDING", "action", "Manter lastro oficial confirmado.")),
                Map.of(
                        "conversationApproval", Map.of("status", "AUTO_READONLY"),
                        "conversationTrustZone", Map.of("status", "NOT_REQUIRED", "trustZone", "PUBLIC"),
                        "conversationEvidenceProvenance", Map.of("status", "NOT_REQUIRED", "tier", "OFFICIAL_DOCUMENT")
                ),
                Map.of(
                        "approvalStatus", "AUTO_READONLY",
                        "trustZoneStatus", "NOT_REQUIRED",
                        "trustZone", "PUBLIC",
                        "evidenceProvenanceStatus", "NOT_REQUIRED",
                        "evidenceProvenanceTier", "OFFICIAL_DOCUMENT"
                )
        ));
    }

    @State("legal ai conversation step up response is available")
    void legalAiConversationStepUpResponseIsAvailable() {
        when(conversationService.converse(any())).thenReturn(new LegalAiConversationResponse(
                "conv-step-up",
                "V3",
                "LEGAL_GENERAL_ASSIST_V3",
                "Resposta conversacional jurídica V3: a consulta exige gate assistido antes de liberar inferência material em zona sigilosa.",
                List.of("Exigir confirmação oficial adicional antes de ampliar o grounding desta conversa."),
                List.of(Map.of("virtualTrend", "VT_SIGILO_STEP_UP", "action", "Exigir confirmação soberana adicional.")),
                Map.of(
                        "conversationApproval", Map.of("status", "STEP_UP_REQUIRED"),
                        "conversationTrustZone", Map.of("status", "ESCALATED", "trustZone", "SIGILOSA"),
                        "conversationEvidenceProvenance", Map.of("status", "ESCALATED", "tier", "DERIVED_DOCUMENT")
                ),
                Map.of(
                        "approvalStatus", "STEP_UP_REQUIRED",
                        "trustZoneStatus", "ESCALATED",
                        "trustZone", "SIGILOSA",
                        "evidenceProvenanceStatus", "ESCALATED",
                        "evidenceProvenanceTier", "DERIVED_DOCUMENT"
                )
        ));
    }

    @State("legal ai conversation blocked response is available")
    void legalAiConversationBlockedResponseIsAvailable() {
        when(conversationService.converse(any())).thenReturn(new LegalAiConversationResponse(
                "conv-blocked",
                "V3",
                "LEGAL_GENERAL_ASSIST_V3",
                "Resposta conversacional jurídica V3: a conversa foi mantida em revisão humana por trust zone crítica e evidência não confiável.",
                List.of("Substituir anexo em quarentena e reenviar apenas origem oficial controlada."),
                List.of(Map.of("virtualTrend", "VT_CRITICAL_LOCK", "action", "Bloquear emissão material até revisão humana.")),
                Map.of(
                        "conversationApproval", Map.of("status", "HUMAN_REVIEW_REQUIRED"),
                        "conversationTrustZone", Map.of("status", "LOCKED", "trustZone", "CRITICAL"),
                        "conversationEvidenceProvenance", Map.of("status", "LOCKED", "tier", "UNTRUSTED_DOCUMENT")
                ),
                Map.of(
                        "approvalStatus", "HUMAN_REVIEW_REQUIRED",
                        "trustZoneStatus", "LOCKED",
                        "trustZone", "CRITICAL",
                        "evidenceProvenanceStatus", "LOCKED",
                        "evidenceProvenanceTier", "UNTRUSTED_DOCUMENT"
                )
        ));
    }
}
