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
import com.tcc.pjb.backend.controller.publico.ConsultasPublicasController;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaFilterOptionDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaHitDTO;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaProcessoViewResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPublicActDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaSearchConfigDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaSearchResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceAccessibilityDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceActionDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceDatasetDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceRoutesDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceSectionDto;
import com.tcc.pjb.backend.model.dto.consultapublica.PublicPageResolveResponse;
import com.tcc.pjb.backend.model.dto.publico.PublicMovimentacaoDTO;
import com.tcc.pjb.backend.model.dto.publico.PublicProcessoResumoCardDto;
import com.tcc.pjb.backend.model.dto.publico.SigiloUiDTO;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDecision;
import com.tcc.pjb.backend.service.consultapublica.ConsultaPublicaSearchService;
import com.tcc.pjb.backend.service.consultapublica.ConsultaPublicaWorkspaceService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Provider("PjbConsultaPublicaProvider")
@PactFolder("src/test/resources/pacts/provider")
class ConsultasPublicasControllerProviderContractTest {

    private final ConsultaPublicaSearchService consultaPublicaSearchService = mock(ConsultaPublicaSearchService.class);
    private final ConsultaPublicaWorkspaceService consultaPublicaWorkspaceService = mock(ConsultaPublicaWorkspaceService.class);
    private final CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
    private final ConsultasPublicasController controller = new ConsultasPublicasController(
            consultaPublicaSearchService,
            consultaPublicaWorkspaceService,
            rateLimiter
    );

    @BeforeEach
    void setUp(PactVerificationContext context) {
        reset(consultaPublicaSearchService, consultaPublicaWorkspaceService, rateLimiter);
        when(rateLimiter.enforce(any(), any(), any(), any())).thenReturn(new CapabilityRateLimitDecision(true, 100L, 99L, 0L, 60, 1));
        PactProviderSpring6Support.configure(context, controller);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpring6Provider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("public consultation workspace can be loaded")
    void publicConsultationWorkspaceCanBeLoaded() {
        when(consultaPublicaWorkspaceService.workspace())
                .thenReturn(new ConsultaPublicaWorkspaceResponse(
                        "W/\"consulta-publica-2026-01\"",
                        LocalDateTime.of(2026, 4, 16, 11, 0),
                        "PUBLIC_ONLY",
                        "Consulta processual clara, rápida e governada",
                        "Busca pública orientada para terceiros sem expor superfícies autenticadas.",
                        new ConsultaPublicaSearchConfigDto(
                                3,
                                20,
                                50,
                                List.of(new ConsultaPublicaFilterOptionDto("ESTADUAL", "Justiça Estadual", "TIPO_JUSTICA")),
                                List.of(new ConsultaPublicaFilterOptionDto("CIVIL", "Cível", "RAMO_DIREITO")),
                                List.of("q", "tipoJustica", "ramoDireito"),
                                "Busque por número, nome ou assunto",
                                "RELEVANCE"
                        ),
                        new ConsultaPublicaWorkspaceRoutesDto(
                                "/api/v1/public/consultas-publicas/workspace",
                                "/api/v1/public/consultas-publicas/search",
                                "/api/v1/public/consultas-publicas/processos/{numero}",
                                "/api/v1/processos/pessoais/meus-processos",
                                "/api/v1/processos/pessoais/cockpit",
                                "/api/v1/processos/pessoais/{processoId}/overview",
                                "/api/v1/calendar/workspace?from={from}&to={to}",
                                "/api/v1/public/consultas-publicas/pages/{pageId}",
                                "/api/v1/public/persons/candidates",
                                "/api/v1/public/persons/candidates/{candidateId}/processos",
                                "/api/v1/public/cpf/{cpf}/processos",
                                "/api/v1/calendar/workspace",
                                "/api/v1/calendar/panel",
                                "/api/v1/processos/{processoId}/prazo-real",
                                "/api/v1/processual/calculos/workspace",
                                "/api/v1/processual/calculos/domain-help",
                                "/api/v1/chat/processo/{processoId}",
                                "/api/v1/processos/{processoId}/notes",
                                "/api/v1/workspace/tags",
                                "/api/v1/processos/{processoId}/tags",
                                "/api/v1/professional/forensic-panel/workspace",
                                "/api/v1/professional/forensic-panel/search",
                                "/api/v1/professional/institutional/overview",
                                "/api/v1/professional/organization/dashboard",
                                "/api/v1/professional/client-360/{clienteId}",
                                "/api/v1/professional/processos/{processoId}",
                                "/api/v1/professional/grants/workspace",
                                "/api/v1/professional/grants/processos/{processoId}/timeline",
                                "/api/v1/professional/grants/governance/dashboard",
                                "/api/v1/professional/grants/batch-requests",
                                "/api/v1/professional/grants/operational/dashboard",
                                "/api/v1/professional/grants/template-catalog",
                                "/api/v1/professional/grants/template-batch-requests"
                        ),
                        new ConsultaPublicaWorkspaceAccessibilityDto(
                                "WCAG_2_2_AA",
                                List.of("contraste reforçado", "navegação por teclado"),
                                List.of("atalhos de foco", "descrição semântica"),
                                "/api/v1/ui/legend",
                                "/api/v1/ui/presentation-bundle",
                                "/api/v1/ui/accessibility/preferences"
                        ),
                        new ConsultaPublicaWorkspaceDatasetDto(true, 0, 15, 25, 20, false, false, true, true, true,
                                "TERCEIROS_APENAS_RESUMO_PUBLICO;ATOS_PUBLICOS_LIMITADOS"),
                        List.of(),
                        List.of(new ConsultaPublicaPublicActDto("ACORDAO", "Acórdão público", "Leitura textual governada", "/api/v1/public/consultas-publicas/pages/{pageId}", true)),
                        List.of(new ConsultaPublicaWorkspaceSectionDto("BUSCA_PUBLICA", "Busca pública", "Busca orientada para terceiros", "PRIMARY", "/api/v1/public/consultas-publicas/search", true)),
                        null,
                        List.of(),
                        List.of("A trilha pública não expõe documentos integrais nem metadados sensíveis.")
                ));
    }

    @State("public consultation search can be executed")
    void publicConsultationSearchCanBeExecuted() {
        when(consultaPublicaSearchService.searchPublic(eq("João Pereira"), eq("ESTADUAL"), eq("CIVIL"), eq(0), eq(20)))
                .thenReturn(ConsultaPublicaSearchResponse.builder()
                        .query("João Pereira")
                        .page(0)
                        .size(20)
                        .total(1)
                        .hits(List.of(ConsultaPublicaHitDTO.builder()
                                .processoId(9001L)
                                .numeroUnificado("0001234-56.2026.8.06.0001")
                                .tipoJustica("ESTADUAL")
                                .ramoDireito("CIVIL")
                                .classeProcessual("Cumprimento de sentença")
                                .assunto("Cobrança contratual")
                                .documentoId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                                .documentoTitulo("Acórdão público")
                                .pageId("ato-2026-0001")
                                .pageNumber(3)
                                .snippet("Partes públicas: João Pereira x Município de Quixadá")
                                .score(0.98d)
                                .build()))
                        .build());
    }

    @State("public process detail can be loaded")
    void publicProcessDetailCanBeLoaded() {
        when(consultaPublicaWorkspaceService.detail(eq("0001234-56.2026.8.06.0001")))
                .thenReturn(new ConsultaPublicaProcessoViewResponse(
                        "W/\"processo-publico-2026-01\"",
                        LocalDateTime.of(2026, 4, 16, 11, 5),
                        25,
                        new PublicProcessoResumoCardDto(
                                9001L,
                                "0001234-56.2026.8.06.0001",
                                "TJCE",
                                "CE",
                                "Quixadá",
                                "Fórum Des. Avelar Rocha",
                                "ESTADUAL",
                                "CIVIL",
                                "Cumprimento de sentença",
                                "Cobrança contratual",
                                LocalDateTime.of(2026, 4, 15, 9, 0),
                                LocalDateTime.of(2026, 4, 16, 10, 0),
                                new SigiloUiDTO(false, 0, "Público", "lock-open", "green", "Processo público"),
                                false,
                                "Resumo público institucional do processo.",
                                List.of(new PublicMovimentacaoDTO(1L, LocalDateTime.of(2026, 4, 16, 10, 0), "CONHECIMENTO", "INSTRUTORIA", "Despacho de impulso")),
                                null
                        ),
                        new ConsultaPublicaWorkspaceAccessibilityDto(
                                "WCAG_2_2_AA",
                                List.of("contraste reforçado"),
                                List.of("navegação por teclado"),
                                "/api/v1/ui/legend",
                                "/api/v1/ui/presentation-bundle",
                                "/api/v1/ui/accessibility/preferences"
                        ),
                        List.of(new ConsultaPublicaWorkspaceActionDto("NOVA_BUSCA", "Nova busca pública", "/api/v1/public/consultas-publicas/search", "NEUTRAL")),
                        List.of("A trilha pública não expõe documentos integrais nem metadados sensíveis.")
                ));
    }

    @State("public page can be resolved")
    void publicPageCanBeResolved() {
        when(consultaPublicaSearchService.resolvePublicPage(eq("ato-2026-0001")))
                .thenReturn(PublicPageResolveResponse.builder()
                        .pageId("ato-2026-0001")
                        .documentoId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .documentoTitulo("Acórdão público")
                        .publicActKind("ACORDAO")
                        .processoId(9001L)
                        .numeroUnificado("0001234-56.2026.8.06.0001")
                        .pageNumber(3)
                        .fingerprint("sha256:abc123")
                        .texto("trecho publico consolidado")
                        .build());
    }
}
