package com.tcc.pjb.backend.contracts.provider;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.spring.spring6.PactVerificationSpring6Provider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.tcc.pjb.backend.ai.juridica.api.AjuizamentoIntentController;
import com.tcc.pjb.backend.ai.juridica.v3.core.AjuizamentoIntent;
import com.tcc.pjb.backend.ai.juridica.v3.core.RamoDescriptor;
import com.tcc.pjb.backend.ai.juridica.v3.core.UnifiedProcessoIntentRouter;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceFieldResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceListItemResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.intelligence.surface.AjuizamentoIntentSurfaceFacadeService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Provider("PjbAjuizamentoIntentProvider")
@PactFolder("src/test/resources/pacts/provider")
class AjuizamentoIntentControllerProviderContractTest {

    private final AjuizamentoIntentSurfaceFacadeService surfaceFacadeService = mock(AjuizamentoIntentSurfaceFacadeService.class);
    private final AjuizamentoIntentController controller = new AjuizamentoIntentController(surfaceFacadeService);

    @BeforeEach
    void setUp(PactVerificationContext context) {
        reset(surfaceFacadeService);
        PactProviderSpring6Support.configure(context, controller);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpring6Provider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("ajuizamento branches can be listed")
    void ajuizamentoBranchesCanBeListed() {
        when(surfaceFacadeService.listarRamos())
                .thenReturn(new SurfaceCollectionResponse(
                        "AJUIZAMENTO_RAMOS",
                        List.of(
                                new SurfaceListItemResponse("CIVIL", List.of(
                                        new SurfaceFieldResponse("label", "Cível"),
                                        new SurfaceFieldResponse("macroArea", "Direito Privado")
                                )),
                                new SurfaceListItemResponse("PENAL", List.of(
                                        new SurfaceFieldResponse("label", "Penal"),
                                        new SurfaceFieldResponse("macroArea", "Direito Público")
                                ))
                        )
                ));
    }

    @State("ajuizamento branch detail can be loaded")
    void ajuizamentoBranchDetailCanBeLoaded() {
        when(surfaceFacadeService.detalharRamo(eq("CIVIL")))
                .thenReturn(new RamoDescriptor(
                        "CIVIL",
                        "Cível",
                        "Tutela de pretensões patrimoniais e obrigacionais",
                        "CPC",
                        List.of("COBRANCA", "RESPONSABILIDADE_CIVIL"),
                        List.of("TJCE", "TRF5"),
                        List.of("Ação de cobrança", "Cumprimento de sentença"),
                        List.of("Tema 988", "Tema 123"),
                        List.of("15 dias úteis", "30 dias"),
                        false,
                        false,
                        true
                ));
    }

    @State("ajuizamento route can be executed")
    void ajuizamentoRouteCanBeExecuted() {
        when(surfaceFacadeService.route(anyMap()))
                .thenReturn(new UnifiedProcessoIntentRouter.RouterDecision(
                        "route-2026-01",
                        Instant.parse("2026-04-16T12:00:00Z"),
                        new AjuizamentoIntent(
                                "COMUM_ORDINARIO",
                                "CIVIL",
                                "COBRANCA",
                                "ESTADUAL",
                                "2_VARA_CIVEL",
                                "AÇÃO DE COBRANÇA",
                                "inadimplemento contratual",
                                0.94,
                                List.of("parteAutoraNome", "pedidoPrincipal"),
                                List.of("Validar competência territorial"),
                                List.of("Contrato", "Notificação extrajudicial"),
                                List.of("Conferir planilha de débito"),
                                false,
                                false,
                                true,
                                null
                        ),
                        Map.of("status", "OK", "rito", "COMUM_ORDINARIO"),
                        new RamoDescriptor(
                                "CIVIL",
                                "Cível",
                                "Tutela de pretensões patrimoniais e obrigacionais",
                                "CPC",
                                List.of("COBRANCA"),
                                List.of("TJCE"),
                                List.of("Ação de cobrança"),
                                List.of("Tema 988"),
                                List.of("15 dias úteis"),
                                false,
                                false,
                                true
                        ),
                        "CIVIL_ESTRATEGICO",
                        "CIVIL",
                        Map.of("program", "CONTENCIOSO_CIVEL"),
                        Map.of("base", "BRASIL_2026"),
                        Map.of("tribunalCodigo", "TJCE", "judicialSystem", "PJE"),
                        Map.of("status", "READY", "reviewChecklist", List.of("competência territorial")),
                        Map.of("rito", "COMUM_ORDINARIO", "tribunal", "TJCE"),
                        Map.of("status", "GREEN"),
                        Map.of("status", "GENERATED"),
                        "Ajuizamento apto para protocolo assistido.",
                        List.of("Confirmar endereço da parte ré"),
                        List.of("parteAutoraNome", "pedidoPrincipal"),
                        List.of("Contrato", "Notificação extrajudicial"),
                        List.of("Gerar petição inicial"),
                        0.95,
                        "PJE",
                        "READY",
                        false,
                        false,
                        true
                ));
    }

    @State("ajuizamento infer map can be generated")
    void ajuizamentoInferMapCanBeGenerated() {
        when(surfaceFacadeService.inferIntentMap(anyMap()))
                .thenReturn(new SurfaceSnapshotResponse(
                        "AJUIZAMENTO_INTENT_MAP",
                        List.of(
                                new SurfaceFieldResponse("ramoDireito", "CIVIL"),
                                new SurfaceFieldResponse("tipoAcao", "AÇÃO DE COBRANÇA"),
                                new SurfaceFieldResponse("confianca", 0.93),
                                new SurfaceFieldResponse("requiresHumanReview", false)
                        )
                ));
    }

    @State("ajuizamento tribunal capabilities can be loaded")
    void ajuizamentoTribunalCapabilitiesCanBeLoaded() {
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("tribunalCodigo", "TJCE");
        capabilities.put("supportsDigitalProtocol", true);
        capabilities.put("supportsBulkUpload", false);
        capabilities.put("supportsJuizo100Digital", true);
        when(surfaceFacadeService.capabilitiesTribunal(anyMap()))
                .thenReturn(new SurfaceSnapshotResponse(
                        "TRIBUNAL_CAPABILITIES",
                        List.of(
                                new SurfaceFieldResponse("tribunalCodigo", "TJCE"),
                                new SurfaceFieldResponse("capabilities", capabilities),
                                new SurfaceFieldResponse("status", "READY")
                        )
                ));
    }
}
