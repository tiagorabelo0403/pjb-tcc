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
import com.tcc.pjb.backend.controller.institutional.InstitutionalWorkbenchController;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchActionPreviewResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchActionResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchExplainabilityResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchMetricResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchOperationalQueueResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchProfileResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchQueueItemResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchQuickActionsResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchRouteResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchWidgetResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchWorkspaceResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.institutional.workbench.InstitutionalWorkbenchProjectionService;
import com.tcc.pjb.backend.service.institutional.workbench.InstitutionalWorkbenchService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Provider("PjbInstitutionalWorkbenchProvider")
@PactFolder("src/test/resources/pacts/provider")
class InstitutionalWorkbenchControllerProviderContractTest {

    private final InstitutionalWorkbenchService service = mock(InstitutionalWorkbenchService.class);
    private final InstitutionalWorkbenchProjectionService projectionService = mock(InstitutionalWorkbenchProjectionService.class);
    private final CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
    private final InstitutionalWorkbenchController controller = new InstitutionalWorkbenchController(service, projectionService);

    @BeforeEach
    void setUp(PactVerificationContext context) {
        reset(service, projectionService, rateLimiter);
        PactProviderSpring6Support.configure(context, controller);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpring6Provider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("institutional workbench workspace can be loaded")
    void institutionalWorkbenchWorkspaceCanBeLoaded() {
        when(service.workspace()).thenReturn(new InstitutionalWorkbenchWorkspaceResponse(
                Instant.parse("2026-04-16T12:00:00Z"),
                new InstitutionalWorkbenchProfileResponse(
                        "PROCURADORIA_FEDERAL",
                        "PROCURADORIA",
                        "FEDERAL",
                        "Procuradoria federal operacional",
                        "Contencioso fazendário e defesa institucional",
                        List.of("FEDERAL", "EXECUCAO_FISCAL"),
                        List.of("TRF5", "CE"),
                        List.of("contencioso", "parecer"),
                        List.of("CONTESTAR", "RECORRER")
                ),
                List.of(new InstitutionalWorkbenchMetricResponse("QUEUE", "Fila ativa", "1", "STEADY", "INFO")),
                List.of(new InstitutionalWorkbenchWidgetResponse(
                        "QUEUE",
                        "Fila operacional",
                        "QUEUE",
                        true,
                        1,
                        "/api/v1/institucional/workbench/operational-queue",
                        "1 item pendente",
                        Map.of("actionableItems", 1),
                        List.of()
                )),
                List.of(new InstitutionalWorkbenchRouteResponse(
                        "OPEN_QUEUE",
                        "Abrir fila",
                        "/api/v1/institucional/workbench/operational-queue",
                        "GET",
                        true
                )),
                quickActionsResponse(null),
                operationalQueueResponse(),
                List.of("Quick actions geradas em modo institucional geral.")
        ));
    }

    @State("institutional workbench quick actions can be projected")
    void institutionalWorkbenchQuickActionsCanBeProjected() {
        when(projectionService.quickActions(9001L)).thenReturn(quickActionsResponse(9001L));
    }

    @State("institutional workbench operational queue can be loaded")
    void institutionalWorkbenchOperationalQueueCanBeLoaded() {
        when(projectionService.operationalQueue(12)).thenReturn(operationalQueueResponse());
    }

    @State("institutional workbench action preview can be loaded")
    void institutionalWorkbenchActionPreviewCanBeLoaded() {
        when(service.actionPreview(9001L, "PROCURADORIA_CONTESTACAO")).thenReturn(new InstitutionalWorkbenchActionPreviewResponse(
                Instant.parse("2026-04-16T12:05:00Z"),
                "PROCURADORIA_FEDERAL",
                9001L,
                "0009001-11.2026.4.05.8100",
                new InstitutionalWorkbenchActionResponse(
                        "PROCURADORIA_CONTESTACAO",
                        "Apresentar contestação",
                        "/api/v1/procuradoria/operacional/processos/9001/contestacao",
                        "POST",
                        true,
                        "ALLOW",
                        "SUCCESS",
                        null,
                        List.of("Fluxo federal compatível com a defesa institucional"),
                        List.of(),
                        Map.of("federalSignal", true)
                ),
                new InstitutionalWorkbenchExplainabilityResponse(
                        "PROCURADORIA_FEDERAL",
                        "FEDERAL",
                        "ALLOW",
                        List.of("Fluxo federal compatível com a defesa institucional"),
                        List.of(),
                        Map.of("federalSignal", true)
                ),
                List.of()
        ));
    }

    private InstitutionalWorkbenchQuickActionsResponse quickActionsResponse(Long processoId) {
        return new InstitutionalWorkbenchQuickActionsResponse(
                Instant.parse("2026-04-16T12:01:00Z"),
                "PROCURADORIA_FEDERAL",
                processoId,
                processoId == null ? null : "0009001-11.2026.4.05.8100",
                List.of(
                        new InstitutionalWorkbenchActionResponse(
                                "PROCURADORIA_CONTESTACAO",
                                "Apresentar contestação",
                                processoId == null
                                        ? "/api/v1/procuradoria/operacional/processos/{processoId}/contestacao"
                                        : "/api/v1/procuradoria/operacional/processos/9001/contestacao",
                                "POST",
                                true,
                                "ALLOW",
                                "SUCCESS",
                                null,
                                List.of("Fluxo federal compatível com a defesa institucional"),
                                List.of(),
                                Map.of("federalSignal", true)
                        )
                ),
                processoId == null
                        ? List.of("Quick actions geradas em modo institucional geral.")
                        : List.of()
        );
    }

    private InstitutionalWorkbenchOperationalQueueResponse operationalQueueResponse() {
        return new InstitutionalWorkbenchOperationalQueueResponse(
                Instant.parse("2026-04-16T12:02:00Z"),
                "PROCURADORIA_FEDERAL",
                12,
                1,
                1,
                0,
                List.of(new InstitutionalWorkbenchQueueItemResponse(
                        77L,
                        9001L,
                        "0009001-11.2026.4.05.8100",
                        "Contestação urgente",
                        "PROC:FED",
                        "PENDENTE",
                        1,
                        Instant.parse("2026-04-17T12:00:00Z"),
                        new InstitutionalWorkbenchActionResponse(
                                "PROCURADORIA_CONTESTACAO",
                                "Apresentar contestação",
                                "/api/v1/procuradoria/operacional/processos/9001/contestacao",
                                "POST",
                                true,
                                "ALLOW",
                                "SUCCESS",
                                null,
                                List.of("Fluxo federal compatível com a defesa institucional"),
                                List.of(),
                                Map.of("federalSignal", true)
                        ),
                        List.of(),
                        List.of(),
                        new InstitutionalWorkbenchExplainabilityResponse(
                                "PROCURADORIA_FEDERAL",
                                "FEDERAL",
                                "ALLOW",
                                List.of("Fluxo federal compatível com a defesa institucional"),
                                List.of(),
                                Map.of("federalSignal", true)
                        )
                )),
                List.of()
        );
    }
}
