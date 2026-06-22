package com.tcc.pjb.backend.controller.magistratura;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.persona.PersonaKey;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.core.security.persona.UserPersonaService;
import com.tcc.pjb.backend.core.security.stepup.FaceReauthTokenPayload;
import com.tcc.pjb.backend.core.security.stepup.FaceReauthTokenService;
import com.tcc.pjb.backend.core.security.stepup.web.MinisterStepUpFilter;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceDispatchResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceResponse;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDecision;
import com.tcc.pjb.backend.service.casefile.CaseContinuityDecisionGateService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.desembargador.DesembargadorColegialdoPainelService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.juiz.CertidaoTransitoJulgadoService;
import com.tcc.pjb.backend.service.juiz.decision.JuizGabineteDecisionalService;
import com.tcc.pjb.backend.service.juiz.decision.JuizOficialCumprimentoOrderService;
import com.tcc.pjb.backend.service.juiz.guardrails.JuizProcessoGuardRailService;
import com.tcc.pjb.backend.service.julgamento.safety.DecisionSafetyService;
import com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialProvidenceAutomationService;
import com.tcc.pjb.backend.service.ministro.MinistroPlenarioService;
import com.tcc.pjb.backend.service.pericia.PeritoNomeacaoService;
import com.tcc.pjb.backend.service.processual.document.template.RecursalQualifiedDocumentMaterializerService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false"
})
class MagistraturaJudicialActsControllerIT extends PjbIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private TrustedDeviceRepository trustedDeviceRepository;

    @MockitoBean
    private UserPersonaService personaService;

    @MockitoBean
    private PjbAuthorizationService authorizationService;

    @MockitoBean
    private CapabilityRateLimiter capabilityRateLimiter;

    @MockitoBean
    private JuizProcessoGuardRailService guardRailService;

    @MockitoBean
    private JuizGabineteDecisionalService juizGabineteDecisionalService;

    @MockitoBean
    private JuizOficialCumprimentoOrderService juizOficialCumprimentoOrderService;

    @MockitoBean
    private CertidaoTransitoJulgadoService certidaoTransitoJulgadoService;

    @MockitoBean
    private DesembargadorColegialdoPainelService desembargadorColegialdoPainelService;

    @MockitoBean
    private MinistroPlenarioService ministroPlenarioService;

    @MockitoBean
    private PeritoNomeacaoService peritoNomeacaoService;

    @MockitoBean
    private InstitutionalActorRoutingService institutionalActorRoutingService;

    @MockitoBean
    private RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService;

    @MockitoBean
    private CaseContinuityDecisionGateService caseContinuityDecisionGateService;

    @MockitoBean
    private DecisionSafetyService decisionSafetyService;

    @MockitoBean
    private PainelServiceCommons commons;

    @MockitoBean
    private MagistraturaJudicialProvidenceAutomationService providenceAutomationService;

    @MockitoBean
    private FaceReauthTokenService faceReauthTokenService;

    private Processo processo;
    private Usuario ministro;

    @BeforeEach
    void setup() {
        processoRepository.deleteAll();
        trustedDeviceRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario juiz = usuarioRepository.save(novoJuiz());
        Usuario desembargador = usuarioRepository.save(novoDesembargador());
        this.ministro = usuarioRepository.save(novoMinistro());

        registrarPasskey(juiz, "cred-juiz-atos-it");
        registrarPasskey(desembargador, "cred-desemb-atos-it");
        registrarPasskey(this.ministro, "cred-ministro-atos-it");
        processo = processoRepository.save(Processo.builder()
                .numeroProcesso("MAG-ATOS-2026-01")
                .numeroUnificado("0009001-11.2026.8.06.0001")
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.CIVIL)
                .classeProcessual("Procedimento comum cível")
                .assunto("Obrigação de fazer")
                .parteAutoraNome("Maria da Silva")
                .parteReuNome("Município de Fortaleza")
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .uf("CE")
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .dataUltimaMovimentacao(LocalDateTime.of(2026, 4, 16, 10, 0))
                .usuario(juiz)
                .build());

        when(personaService.getRequiredPersona()).thenReturn(personaJuiz());
        doNothing().when(authorizationService).requireReadProcesso(any());
        when(capabilityRateLimiter.enforce(any(), any(), any(), any())).thenReturn(new CapabilityRateLimitDecision(true, 100L, 99L, 0L, 60, 1));
        JuizProcessoGuardRailService.GuardRailSnapshot allowSnapshot = guardAllow();
        when(guardRailService.avaliar(any(), any(), any(), any())).thenReturn(allowSnapshot);
        when(juizGabineteDecisionalService.assinarDespacho(eq(processo.getId()), eq("Intime-se."), eq("CPC")))
                .thenReturn(Map.of("status", "ASSINADO", "processoId", processo.getId(), "documentoId", "DOC-9001"));
        when(providenceAutomationService.preview(any(), any(), any(), any())).thenReturn(List.of());
        when(providenceAutomationService.dispatch(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(providenceAutomationService.preview(any(), any(), eq(MagistraturaJudicialActCode.DESPACHO), any()))
                .thenReturn(List.of(new MagistraturaJudicialProvidenceResponse(
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
                )));
        when(providenceAutomationService.dispatch(any(), any(), eq(MagistraturaJudicialActCode.DESPACHO), any(), any()))
                .thenReturn(List.of(new MagistraturaJudicialProvidenceDispatchResponse(
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
                )));
    }

    @Test
    @WithMockUser(username = "juiz@test.local", roles = "JUIZ_ESTADUAL")
    void deveProjetarWorkspaceMagistraturaComAtosDisponiveis() throws Exception {
        mockMvc.perform(get("/api/v1/magistratura/atos")
                        .param("processoId", String.valueOf(processo.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lane").value("PRIMEIRO_GRAU"))
                .andExpect(jsonPath("$.processoId").value(processo.getId()))
                .andExpect(jsonPath("$.processoNumero").value("MAG-ATOS-2026-01"))
                .andExpect(jsonPath("$.acts[0].code").value("DESPACHO"))
                .andExpect(jsonPath("$.acts[0].enabled").value(true))
                .andExpect(jsonPath("$.acts[0].nativeRoute").value("/api/v1/juiz/gabinete-decisoes/processos/" + processo.getId() + "/despacho"));
    }

    @Test
    @WithMockUser(username = "juiz@test.local", roles = "JUIZ_ESTADUAL")
    void deveProjetarPreviewDeAtoMagistradoComProvidenciaAutomatica() throws Exception {
        mockMvc.perform(get("/api/v1/magistratura/processos/{processoId}/atos/preview", processo.getId())
                        .param("action", "DESPACHO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processoId").value(processo.getId()))
                .andExpect(jsonPath("$.action").value("DESPACHO"))
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.nativeRoute").value("/api/v1/juiz/gabinete-decisoes/processos/" + processo.getId() + "/despacho"))
                .andExpect(jsonPath("$.providences[0].code").value("PROVIDENCIAR_PUBLICACAO"))
                .andExpect(jsonPath("$.providences[0].targetQueueCode").value("PUB:DESPACHO"));
    }

    @Test
    @WithMockUser(username = "juiz@test.local", roles = "JUIZ_ESTADUAL")
    void deveProjetarPreviewBloqueadoQuandoGuardRailNegarAto() throws Exception {
        JuizProcessoGuardRailService.GuardRailSnapshot blockSnapshot = guardBlock();
        when(guardRailService.avaliar(any(), any(), any(), any())).thenReturn(blockSnapshot);

        mockMvc.perform(get("/api/v1/magistratura/processos/{processoId}/atos/preview", processo.getId())
                        .param("action", "SENTENCA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processoId").value(processo.getId()))
                .andExpect(jsonPath("$.action").value("SENTENCA"))
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.verdict").value("BLOCK"))
                .andExpect(jsonPath("$.warnings[0]").value("Ato bloqueado pela malha jurisdicional/material do magistrado."));
    }

    @Test
    @WithMockUser(username = "juiz@test.local", roles = "JUIZ_ESTADUAL")
    void deveProjetarAutomationPreviewDaMagistratura() throws Exception {
        MagistraturaJudicialActCommandRequest request = new MagistraturaJudicialActCommandRequest(
                "DESPACHO",
                "Intime-se.",
                "CPC",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of("PROVIDENCIAR_PUBLICACAO"),
                Boolean.TRUE
        );

        mockMvc.perform(post("/api/v1/magistratura/processos/{processoId}/atos/automation-preview", processo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processoId").value(processo.getId()))
                .andExpect(jsonPath("$.action").value("DESPACHO"))
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.providences[0].code").value("PROVIDENCIAR_PUBLICACAO"));
    }

    @Test
    @WithMockUser(username = "juiz@test.local", roles = "JUIZ_ESTADUAL")
    void deveExecutarDespachoDaMagistraturaSemPerderProvidenciaProjetada() throws Exception {
        MagistraturaJudicialActCommandRequest request = new MagistraturaJudicialActCommandRequest(
                "DESPACHO",
                "Intime-se.",
                "CPC",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of("PROVIDENCIAR_PUBLICACAO"),
                Boolean.TRUE
        );

        mockMvc.perform(post("/api/v1/magistratura/processos/{processoId}/atos", processo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.action").value("DESPACHO"))
                .andExpect(jsonPath("$.lane").value("PRIMEIRO_GRAU"))
                .andExpect(jsonPath("$.status").value("ASSINADO"))
                .andExpect(jsonPath("$.processoId").value(processo.getId()))
                .andExpect(jsonPath("$.providences[0].code").value("PROVIDENCIAR_PUBLICACAO"))
                .andExpect(jsonPath("$.payload.status").value("ASSINADO"));
    }

    @Test
    @WithMockUser(username = "juiz@test.local", roles = "JUIZ_ESTADUAL")
    void deveNegarExecucaoQuandoGuardRailBloquearAto() throws Exception {
        JuizProcessoGuardRailService.GuardRailSnapshot blockSnapshot = guardBlock();
        when(guardRailService.avaliar(any(), any(), any(), any())).thenReturn(blockSnapshot);
        MagistraturaJudicialActCommandRequest request = new MagistraturaJudicialActCommandRequest(
                "DESPACHO",
                "Intime-se.",
                "CPC",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/magistratura/processos/{processoId}/atos", processo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Acesso negado."));
    }

    @Test
    @WithMockUser(username = "desembargador@test.local", roles = "DESEMBARGADOR")
    void deveExecutarVotoColegialEmSegundoGrau() throws Exception {
        when(personaService.getRequiredPersona()).thenReturn(personaDesembargador());
        when(desembargadorColegialdoPainelService.proferirVoto(eq(processo.getId()), eq("Acompanho o relator."), eq("fundamentacao colegiada"), eq("NEGAR_PROVIMENTO")))
                .thenReturn(Map.of("status", "VOTO_REGISTRADO", "processoId", processo.getId(), "sessaoId", 808L));
        when(providenceAutomationService.dispatch(any(), any(), eq(MagistraturaJudicialActCode.VOTO_COLEGIADO), any(), any()))
                .thenReturn(List.of(new MagistraturaJudicialProvidenceDispatchResponse(
                        MagistraturaJudicialProvidenceCode.REMETER_COLEGIADO_OU_PLENARIO,
                        "DISPATCHED",
                        "REMETER_COLEGIADO_OU_PLENARIO",
                        901L,
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
                )));
        MagistraturaJudicialActCommandRequest request = new MagistraturaJudicialActCommandRequest(
                "VOTO_COLEGIADO",
                null,
                "fundamentacao colegiada",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Acompanho o relator.",
                "NEGAR_PROVIMENTO",
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/magistratura/processos/{processoId}/atos", processo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.action").value("VOTO_COLEGIADO"))
                .andExpect(jsonPath("$.lane").value("SEGUNDO_GRAU"))
                .andExpect(jsonPath("$.status").value("VOTO_REGISTRADO"))
                .andExpect(jsonPath("$.providences[0].code").value("REMETER_COLEGIADO_OU_PLENARIO"));
    }

    @Test
    @WithMockUser(username = "ministro@test.local", roles = "MINISTRO")
    void deveExecutarDecisaoPlenariaEmTrilhaSuperior() throws Exception {
        long nowSec = Instant.now().getEpochSecond();
        when(faceReauthTokenService.verifyAndDecode(any()))
                .thenReturn(new FaceReauthTokenPayload("jti-it-ministro", ministro.getId(), nowSec - 100L, nowSec + 7200L, "FACE_HIGH"));
        when(personaService.getRequiredPersona()).thenReturn(personaMinistro());
        when(ministroPlenarioService.registrarDecisaoPlenaria(eq(processo.getId()), eq("MAIORIA"), eq("Tema constitucional fixado."), eq("Fixada a tese.")))
                .thenReturn(Map.of("status", "DECISAO_PLENARIA_REGISTRADA", "processoId", processo.getId(), "sessaoId", 1201L));
        when(providenceAutomationService.dispatch(any(), any(), eq(MagistraturaJudicialActCode.DECISAO_PLENARIA), any(), any()))
                .thenReturn(List.of(new MagistraturaJudicialProvidenceDispatchResponse(
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
                )));
        MagistraturaJudicialActCommandRequest request = new MagistraturaJudicialActCommandRequest(
                "DECISAO_PLENARIA",
                null,
                null,
                "Fixada a tese.",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Tema constitucional fixado.",
                null,
                "MAIORIA",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/magistratura/processos/{processoId}/atos", processo.getId())
                        .header(MinisterStepUpFilter.HEADER_FACE_TOKEN, "test-face-token-ministro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.action").value("DECISAO_PLENARIA"))
                .andExpect(jsonPath("$.lane").value("SUPERIOR"))
                .andExpect(jsonPath("$.status").value("DECISAO_PLENARIA_REGISTRADA"))
                .andExpect(jsonPath("$.providences[0].code").value("PROVIDENCIAR_PUBLICACAO"));
    }

    private Usuario novoJuiz() {
        Usuario usuario = new Usuario();
        usuario.setNome("Juiz Estadual Teste");
        usuario.setEmail("juiz@test.local");
        usuario.setSenha("x");
        usuario.setCpf("12345678901");
        usuario.setTipoUsuario(TipoUsuario.JUIZ_ESTADUAL);
        usuario.setPerfil(TipoUsuario.JUIZ_ESTADUAL.name());
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setAtivo(true);
        return usuario;
    }

    private Usuario novoDesembargador() {
        Usuario usuario = new Usuario();
        usuario.setNome("Desembargador Teste");
        usuario.setEmail("desembargador@test.local");
        usuario.setSenha("x");
        usuario.setCpf("22345678901");
        usuario.setTipoUsuario(TipoUsuario.DESEMBARGADOR);
        usuario.setPerfil(TipoUsuario.DESEMBARGADOR.name());
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setAtivo(true);
        return usuario;
    }

    private Usuario novoMinistro() {
        Usuario usuario = new Usuario();
        usuario.setNome("Ministro Teste");
        usuario.setEmail("ministro@test.local");
        usuario.setSenha("x");
        usuario.setCpf("32345678901");
        usuario.setTipoUsuario(TipoUsuario.MINISTRO);
        usuario.setPerfil(TipoUsuario.MINISTRO.name());
        usuario.setUf("DF");
        usuario.setComarca("Brasília");
        usuario.setAtivo(true);
        return usuario;
    }

    private UserPersona personaJuiz() {
        return new UserPersona(
                TipoUsuario.JUIZ_ESTADUAL,
                PersonaKey.JUIZ_ESTADUAL,
                "Juiz Estadual",
                "Excelência",
                GrauJurisdicao.PRIMEIRO_GRAU,
                EsferaJurisdicao.JUSTICA_ESTADUAL,
                false
        );
    }

    private UserPersona personaDesembargador() {
        return new UserPersona(
                TipoUsuario.DESEMBARGADOR,
                PersonaKey.DESEMBARGADOR,
                "Desembargador",
                "Excelência",
                GrauJurisdicao.SEGUNDO_GRAU,
                EsferaJurisdicao.JUSTICA_ESTADUAL,
                false
        );
    }

    private UserPersona personaMinistro() {
        return new UserPersona(
                TipoUsuario.MINISTRO,
                PersonaKey.MINISTRO,
                "Ministro",
                "Excelência",
                GrauJurisdicao.SUPERIOR,
                EsferaJurisdicao.JUSTICA_ESTADUAL,
                false
        );
    }

    private void registrarPasskey(Usuario usuario, String credentialId) {
        TrustedDevice passkey = new TrustedDevice();
        passkey.setUsuario(usuario);
        passkey.setCredentialId(credentialId);
        passkey.setPublicKey("pub-key-" + credentialId);
        passkey.setAlias("passkey-" + credentialId);
        passkey.setAttestationTrusted(false);
        passkey.setEnrollSuspectNetwork(false);
        passkey.setRiskScoreEnroll(0);
        trustedDeviceRepository.save(passkey);
    }

    private JuizProcessoGuardRailService.GuardRailSnapshot guardAllow() {
        JuizProcessoGuardRailService.GuardRailSnapshot snapshot = mock(JuizProcessoGuardRailService.GuardRailSnapshot.class);
        when(snapshot.allowed()).thenReturn(true);
        when(snapshot.fundamentos()).thenReturn(List.of("Competência material e territorial confirmada."));
        when(snapshot.metrics()).thenReturn(Map.of("allowed", true, "territorial", true));
        return snapshot;
    }

    private JuizProcessoGuardRailService.GuardRailSnapshot guardBlock() {
        JuizProcessoGuardRailService.GuardRailSnapshot snapshot = mock(JuizProcessoGuardRailService.GuardRailSnapshot.class);
        when(snapshot.allowed()).thenReturn(false);
        when(snapshot.fundamentos()).thenReturn(List.of("Competência material não aderente à trilha atual."));
        when(snapshot.metrics()).thenReturn(Map.of("allowed", false, "territorial", false));
        return snapshot;
    }
}
