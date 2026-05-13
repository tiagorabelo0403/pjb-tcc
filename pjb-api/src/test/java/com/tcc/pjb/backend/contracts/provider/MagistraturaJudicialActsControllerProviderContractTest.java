package com.tcc.pjb.backend.contracts.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.spring.spring6.PactVerificationSpring6Provider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.tcc.pjb.backend.controller.magistratura.MagistraturaJudicialActsController;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActAvailabilityResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActFieldResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActPreviewResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceDispatchResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceResponse;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDecision;
import com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialActWorkbenchService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Provider("PjbMagistraturaActsProvider")
@PactFolder("src/test/resources/pacts/provider")
class MagistraturaJudicialActsControllerProviderContractTest {

    private final MagistraturaJudicialActWorkbenchService service = mock(MagistraturaJudicialActWorkbenchService.class);
    private final CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
    private final MagistraturaJudicialActsController controller = new MagistraturaJudicialActsController(service, rateLimiter);

    @BeforeEach
    void setUp(PactVerificationContext context) {
        reset(service, rateLimiter);
        when(rateLimiter.enforce(any(), any(), any(), any())).thenReturn(new CapabilityRateLimitDecision(true, 100L, 99L, 0L, 60, 1));
        PactProviderSpring6Support.configure(context, controller);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpring6Provider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("magistratura acts workspace can be loaded")
    void magistraturaActsWorkspaceCanBeLoaded() {
        when(service.workspace(eq(9001L))).thenReturn(new MagistraturaJudicialActWorkspaceResponse(
                77L,
                "Juiz Teste",
                TipoUsuario.JUIZ_ESTADUAL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                EsferaJurisdicao.JUSTICA_ESTADUAL,
                "PRIMEIRO_GRAU",
                9001L,
                "0009001-11.2026.8.06.0001",
                List.of("/api/v1/magistratura/atos", "/api/v1/magistratura/processos/9001/atos"),
                List.of(new MagistraturaJudicialActAvailabilityResponse(
                        MagistraturaJudicialActCode.DESPACHO,
                        "Despacho",
                        "PRIMEIRO_GRAU",
                        true,
                        "ALLOW",
                        "/api/v1/magistratura/processos/9001/atos",
                        "DESPACHO",
                        List.of("Trilha singular apta para despacho."),
                        List.of(),
                        List.of(new MagistraturaJudicialActFieldResponse("conteudo", "Conteúdo", "TEXTAREA", true, "Intime-se."))
                )),
                Map.of("lane", "PRIMEIRO_GRAU", "processoNumero", "0009001-11.2026.8.06.0001")
        ));
    }

    @State("magistratura act preview can be loaded")
    void magistraturaActPreviewCanBeLoaded() {
        when(service.preview(eq(9001L), eq("DESPACHO"))).thenReturn(new MagistraturaJudicialActPreviewResponse(
                9001L,
                "0009001-11.2026.8.06.0001",
                MagistraturaJudicialActCode.DESPACHO,
                true,
                "ALLOW",
                "PRIMEIRO_GRAU",
                "Despacho — JUIZO_SINGULAR — 0009001-11.2026.8.06.0001",
                "/api/v1/magistratura/processos/9001/atos",
                "DESPACHO",
                List.of("Fluxo singular compatível com o processo."),
                List.of(),
                List.of(new MagistraturaJudicialActFieldResponse("conteudo", "Conteúdo", "TEXTAREA", true, "Intime-se.")),
                List.of(new MagistraturaJudicialProvidenceResponse(
                        MagistraturaJudicialProvidenceCode.PROVIDENCIAR_PUBLICACAO,
                        "Publicação",
                        true,
                        "PROVIDENCIAR_PUBLICACAO",
                        "SECRETARIA:PUBLICACAO",
                        "PUB:DESPACHO",
                        "/api/v1/secretaria/publicacoes",
                        Instant.parse("2026-04-17T12:00:00Z"),
                        null,
                        null,
                        null,
                        "Publicação automática do despacho.",
                        List.of(),
                        List.of("Fluxo ordinário de secretaria."),
                        List.of(),
                        Map.of("priority", 1)
                )),
                Map.of("lane", "PRIMEIRO_GRAU", "nativeRoute", "/api/v1/magistratura/processos/9001/atos")
        ));
    }

    @State("magistratura blocked preview can be loaded")
    void magistraturaBlockedPreviewCanBeLoaded() {
        when(service.preview(eq(9001L), eq("SENTENCA"))).thenReturn(new MagistraturaJudicialActPreviewResponse(
                9001L,
                "0009001-11.2026.8.06.0001",
                MagistraturaJudicialActCode.SENTENCA,
                false,
                "BLOCK",
                "PRIMEIRO_GRAU",
                "Sentença — JUIZO_SINGULAR — 0009001-11.2026.8.06.0001",
                "/api/v1/magistratura/processos/9001/atos",
                "SENTENCA",
                List.of("Fluxo singular projetado para sentença."),
                List.of("Ato bloqueado pela malha jurisdicional/material do magistrado."),
                List.of(new MagistraturaJudicialActFieldResponse("dispositivo", "Dispositivo", "TEXTAREA", true, "Julgo procedente.")),
                List.of(),
                Map.of("lane", "PRIMEIRO_GRAU", "nativeRoute", "/api/v1/magistratura/processos/9001/atos")
        ));
    }

    @State("magistratura automation preview can be loaded")
    void magistraturaAutomationPreviewCanBeLoaded() {
        when(service.preview(eq(9001L), org.mockito.ArgumentMatchers.<com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest>any())).thenReturn(new MagistraturaJudicialActPreviewResponse(
                9001L,
                "0009001-11.2026.8.06.0001",
                MagistraturaJudicialActCode.DESPACHO,
                true,
                "ALLOW",
                "PRIMEIRO_GRAU",
                "Despacho — JUIZO_SINGULAR — 0009001-11.2026.8.06.0001",
                "/api/v1/magistratura/processos/9001/atos",
                "DESPACHO",
                List.of("Fluxo singular compatível com o processo."),
                List.of(),
                List.of(new MagistraturaJudicialActFieldResponse("conteudo", "Conteúdo", "TEXTAREA", true, "Intime-se.")),
                List.of(new MagistraturaJudicialProvidenceResponse(
                        MagistraturaJudicialProvidenceCode.EXPEDIR_INTIMACOES,
                        "Intimação",
                        true,
                        "EXPEDIR_INTIMACOES",
                        "SECRETARIA:INTIMACAO",
                        "INT:DESPACHO",
                        "/api/v1/secretaria/intimacoes",
                        Instant.parse("2026-04-17T14:00:00Z"),
                        null,
                        null,
                        null,
                        "Intimação automática das partes.",
                        List.of(),
                        List.of("Providência deduzida do conteúdo do ato."),
                        List.of(),
                        Map.of("priority", 2)
                )),
                Map.of("lane", "PRIMEIRO_GRAU", "automation", true)
        ));
    }

    @State("magistratura act can be executed")
    void magistraturaActCanBeExecuted() {
        when(service.execute(eq(9001L), any())).thenReturn(new MagistraturaJudicialActCommandResponse(
                MagistraturaJudicialActCode.DESPACHO,
                "PRIMEIRO_GRAU",
                "ASSINADO",
                9001L,
                List.of("Fluxo singular compatível com o processo."),
                List.of(new MagistraturaJudicialProvidenceDispatchResponse(
                        MagistraturaJudicialProvidenceCode.PROVIDENCIAR_PUBLICACAO,
                        "DISPATCHED",
                        "PROVIDENCIAR_PUBLICACAO",
                        501L,
                        false,
                        "SECRETARIA:PUBLICACAO",
                        "PUB:DESPACHO",
                        "/api/v1/secretaria/publicacoes",
                        Instant.parse("2026-04-17T12:00:00Z"),
                        null,
                        null,
                        null,
                        "Publicação criada para o despacho.",
                        List.of(),
                        List.of("Fluxo ordinário de secretaria."),
                        Map.of("priority", 1)
                )),
                Map.of("status", "ASSINADO", "documentoId", "DOC-9001")
        ));
    }

    @State("magistratura colegiado act can be executed")
    void magistraturaColegiadoActCanBeExecuted() {
        when(service.execute(eq(9001L), any())).thenReturn(new MagistraturaJudicialActCommandResponse(
                MagistraturaJudicialActCode.VOTO_COLEGIADO,
                "SEGUNDO_GRAU",
                "VOTO_REGISTRADO",
                9001L,
                List.of("Trilha colegiada compatível com o processo."),
                List.of(new MagistraturaJudicialProvidenceDispatchResponse(
                        MagistraturaJudicialProvidenceCode.REMETER_COLEGIADO_OU_PLENARIO,
                        "DISPATCHED",
                        "REMETER_COLEGIADO_OU_PLENARIO",
                        601L,
                        false,
                        "COLEGIADO:PUBLICACAO",
                        "COL:VOTO",
                        "/api/v1/colegiado/publicacoes",
                        Instant.parse("2026-04-18T12:00:00Z"),
                        null,
                        null,
                        null,
                        "Publicação colegiada preparada.",
                        List.of(),
                        List.of("Trilha colegiada."),
                        Map.of("priority", 2)
                )),
                Map.of("status", "VOTO_REGISTRADO", "sessaoId", 808L)
        ));
    }

    @State("magistratura superior act can be executed")
    void magistraturaSuperiorActCanBeExecuted() {
        when(service.execute(eq(9001L), any())).thenReturn(new MagistraturaJudicialActCommandResponse(
                MagistraturaJudicialActCode.DECISAO_PLENARIA,
                "SUPERIOR",
                "DECISAO_PLENARIA_REGISTRADA",
                9001L,
                List.of("Trilha superior compatível com o processo."),
                List.of(new MagistraturaJudicialProvidenceDispatchResponse(
                        MagistraturaJudicialProvidenceCode.PROVIDENCIAR_PUBLICACAO,
                        "DISPATCHED",
                        "PROVIDENCIAR_PUBLICACAO",
                        1001L,
                        false,
                        "SUPERIOR:PUBLICACAO",
                        "SUP:PLENARIO",
                        "/api/v1/superior/publicacoes",
                        Instant.parse("2026-04-19T12:00:00Z"),
                        null,
                        null,
                        null,
                        "Publicação da decisão plenária preparada.",
                        List.of(),
                        List.of("Trilha superior."),
                        Map.of("priority", 1)
                )),
                Map.of("status", "DECISAO_PLENARIA_REGISTRADA", "sessaoId", 1201L)
        ));
    }
}
