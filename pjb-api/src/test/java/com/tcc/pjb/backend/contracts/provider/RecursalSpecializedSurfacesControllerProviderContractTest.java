package com.tcc.pjb.backend.contracts.provider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.spring.spring6.PactVerificationSpring6Provider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.tcc.pjb.backend.controller.processual.recursal.surface.RecursalAttorneySurfaceController;
import com.tcc.pjb.backend.controller.processual.recursal.surface.RecursalDocumentalSurfaceController;
import com.tcc.pjb.backend.controller.processual.recursal.surface.RecursalInstitutionalSurfaceController;
import com.tcc.pjb.backend.controller.processual.recursal.surface.RecursalIntelligenceSurfaceController;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalOperationalSurfaceGapView;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalSpecializedSurfaceResponse;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalAttorneySurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalDocumentalSurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalInstitutionalSurfaceService;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalIntelligenceSurfaceService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Provider("PjbRecursalSpecializedSurfacesProvider")
@PactFolder("src/test/resources/pacts/provider")
class RecursalSpecializedSurfacesControllerProviderContractTest {

    private final RecursalAttorneySurfaceService attorneyService = mock(RecursalAttorneySurfaceService.class);
    private final RecursalInstitutionalSurfaceService institutionalService = mock(RecursalInstitutionalSurfaceService.class);
    private final RecursalDocumentalSurfaceService documentalService = mock(RecursalDocumentalSurfaceService.class);
    private final RecursalIntelligenceSurfaceService intelligenceService = mock(RecursalIntelligenceSurfaceService.class);

    private final RecursalAttorneySurfaceController attorneyController = new RecursalAttorneySurfaceController(attorneyService);
    private final RecursalInstitutionalSurfaceController institutionalController = new RecursalInstitutionalSurfaceController(institutionalService);
    private final RecursalDocumentalSurfaceController documentalController = new RecursalDocumentalSurfaceController(documentalService);
    private final RecursalIntelligenceSurfaceController intelligenceController = new RecursalIntelligenceSurfaceController(intelligenceService);

    @BeforeEach
    void setUp(PactVerificationContext context) {
        reset(attorneyService, institutionalService, documentalService, intelligenceService);
        PactProviderSpring6Support.configure(context, attorneyController, institutionalController, documentalController, intelligenceController);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpring6Provider.class)
    void verify(PactVerificationContext context, MockHttpServletRequestBuilder request) {
        PactProviderSpring6Support.applyJsonBody(context, request);
        context.verifyInteraction();
    }

    @State("recursal attorney surface can be loaded")
    void recursalAttorneySurfaceCanBeLoaded() {
        when(attorneyService.buildAttorneySurface(org.mockito.ArgumentMatchers.any())).thenReturn(new RecursalSpecializedSurfaceResponse(
                "SURFACE_ADVOGADO_RECURSAL",
                "Advogado, partes e peticionamento",
                "/surfaces/attorney",
                "/api/v1/processual/recursal/surfaces/attorney",
                "Apelação",
                false,
                null,
                List.of("PAINEL_ADVOGADO_RECURSAL_COMPLETO", "PETICIONAMENTO_LOTE_ASSINATURA_RECURSAL"),
                List.of("painelAdvogado", "peticionamentoEmLote"),
                List.of("Avisos do advogado governados pela malha recursal."),
                List.of(new RecursalOperationalSurfaceGapView(
                        "SPECIALIZED_CONTRACTS_AND_ITS",
                        "CRITICA",
                        "Fechar contracts e ITs por surface especializada",
                        "Blindar o boundary HTTP do eixo do advogado."
                ))
        ));
    }

    @State("recursal institutional surface can be loaded")
    void recursalInstitutionalSurfaceCanBeLoaded() {
        when(institutionalService.buildInstitutionalSurface(org.mockito.ArgumentMatchers.any())).thenReturn(new RecursalSpecializedSurfaceResponse(
                "SURFACE_INSTITUCIONAL_RECURSAL",
                "Institucional, caixas e secretaria",
                "/surfaces/institutional",
                "/api/v1/processual/recursal/surfaces/institutional",
                "Apelação",
                false,
                null,
                List.of("ORGANIZACAO_INSTITUCIONAL_RECURSAL", "CAIXAS_HISTORICO_INSTITUCIONAL_RECURSAL"),
                List.of("caixasInstitucionais", "secretariaMultigrau"),
                List.of("Filas e devoluções mantidas no eixo institucional real."),
                List.of(new RecursalOperationalSurfaceGapView(
                        "SPECIALIZED_CONTRACTS_AND_ITS",
                        "CRITICA",
                        "Fechar contracts e ITs por surface especializada",
                        "Blindar o boundary HTTP do eixo institucional."
                ))
        ));
    }

    @State("recursal documental surface can be loaded")
    void recursalDocumentalSurfaceCanBeLoaded() {
        when(documentalService.buildDocumentalSurface(org.mockito.ArgumentMatchers.any())).thenReturn(new RecursalSpecializedSurfaceResponse(
                "SURFACE_DOCUMENTAL_RECURSAL",
                "Autos digitais, certidões e colaboração documental",
                "/surfaces/documental",
                "/api/v1/processual/recursal/surfaces/documental",
                "Apelação",
                false,
                null,
                List.of("AUTOS_DIGITAIS_RECURSAIS_DETALHADOS", "CERTIDOES_EXTERNAS_RECURSAIS", "WIZARD_DISTRIBUICAO_ASSISTIDA_IA"),
                List.of("viewerRecursal", "certidoesExternas", "wizardDistribuicao"),
                List.of("Viewer e colaboração documental recursal em consolidação."),
                List.of(
                        new RecursalOperationalSurfaceGapView(
                                "SPECIALIZED_CONTRACTS_AND_ITS",
                                "CRITICA",
                                "Fechar contracts e ITs por surface especializada",
                                "Blindar o boundary HTTP do eixo documental."
                        ),
                        new RecursalOperationalSurfaceGapView(
                                "DOCUMENT_VIEWER_ASSINATURA_AUTENTICIDADE",
                                "ALTA",
                                "Aprofundar viewer, autenticidade e assinatura",
                                "Fechar suíte soberana de viewer, hash, sigilo por artefato e assinatura."
                        )
                )
        ));
    }

    @State("recursal intelligence surface can be loaded")
    void recursalIntelligenceSurfaceCanBeLoaded() {
        when(intelligenceService.buildIntelligenceSurface(org.mockito.ArgumentMatchers.any())).thenReturn(new RecursalSpecializedSurfaceResponse(
                "SURFACE_INTELIGENCIA_RECURSAL",
                "Observabilidade, indexação e avisos",
                "/surfaces/intelligence",
                "/api/v1/processual/recursal/surfaces/intelligence",
                "Apelação",
                false,
                null,
                List.of("OBSERVABILIDADE_INDEXACAO_INTELIGENTE_RECURSAL", "ESCALONAMENTO_ALERTAS_POR_PERFIL"),
                List.of("observabilidadeRecursal", "avisosGovernados"),
                List.of("Alertas móveis ainda sob governança central."),
                List.of(
                        new RecursalOperationalSurfaceGapView(
                                "SPECIALIZED_CONTRACTS_AND_ITS",
                                "CRITICA",
                                "Fechar contracts e ITs por surface especializada",
                                "Blindar o boundary HTTP do eixo de inteligência."
                        ),
                        new RecursalOperationalSurfaceGapView(
                                "MOBILE_PUSH_GOVERNANCE",
                                "ALTA",
                                "Endurecer avisos móveis sem scheduler paralelo",
                                "Fechar trilho mobile/notificacional governado."
                        )
                )
        ));
    }
}
