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
import com.tcc.pjb.backend.controller.processual.peticionamento.PeticionamentoController;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDecision;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import com.tcc.pjb.backend.service.processual.peticionamento.journey.PeticionamentoJourneyIntelligenceService;
import com.tcc.pjb.backend.service.processual.peticionamento.PeticionamentoSessaoFacadeService;
import com.tcc.pjb.backend.service.processual.peticionamento.journey.PeticionamentoSimpleProtocolWizardService;
import com.tcc.pjb.backend.service.processual.peticionamento.studio.PeticionamentoStudioWorkspaceService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Provider("PjbPeticionamentoProvider")
@PactFolder("src/test/resources/pacts/provider")
class PeticionamentoControllerProviderContractTest {

    private final PeticionamentoSessaoFacadeService facadeService = mock(PeticionamentoSessaoFacadeService.class);
    private final LaianePeticaoInicialDraftService draftService = mock(LaianePeticaoInicialDraftService.class);
    private final PeticionamentoStudioWorkspaceService studioWorkspaceService = mock(PeticionamentoStudioWorkspaceService.class);
    private final PeticionamentoSimpleProtocolWizardService simpleProtocolWizardService = mock(PeticionamentoSimpleProtocolWizardService.class);
    private final PeticionamentoJourneyIntelligenceService journeyIntelligenceService = mock(PeticionamentoJourneyIntelligenceService.class);
    private final CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
    private final PeticionamentoController controller = new PeticionamentoController(
            facadeService,
            draftService,
            studioWorkspaceService,
            simpleProtocolWizardService,
            journeyIntelligenceService,
            rateLimiter
    );

    @BeforeEach
    void setUp(PactVerificationContext context) {
        reset(facadeService, draftService, studioWorkspaceService, simpleProtocolWizardService, journeyIntelligenceService, rateLimiter);
        PactProviderSpring6Support.configure(context, controller);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpring6Provider.class)
    void verify(PactVerificationContext context, MockHttpServletRequestBuilder request) {
        PactProviderSpring6Support.applyJsonBody(context, request);
        context.verifyInteraction();
    }

    @State("initial petition session can be opened")
    void initialPetitionSessionCanBeOpened() {
        when(rateLimiter.enforce(any(), any(), any(), any())).thenReturn(new CapabilityRateLimitDecision(true, 100L, 99L, 0L, 60, 1));
        when(facadeService.abrirSessaoInicial(any(PeticionamentoSessaoRequest.class)))
                .thenReturn(PeticionamentoSessaoResponse.builder()
                        .modoSolicitado("INICIAL")
                        .modoResolvido("INICIAL_ASSISTIDO")
                        .papelArquitetural("PETICIONAMENTO_STUDIO")
                        .status("READY")
                        .sessionKey("sessao-ini-001")
                        .passosSugeridos(List.of("validar documentos", "revisar competencia"))
                        .workspace(Map.of("template", "peticao-inicial"))
                        .build());
    }
}
