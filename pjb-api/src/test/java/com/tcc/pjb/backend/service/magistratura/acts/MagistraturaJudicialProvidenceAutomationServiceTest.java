package com.tcc.pjb.backend.service.magistratura.acts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceCode;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalAssignmentService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatProcessContactEnvelopeResolver;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MagistraturaJudicialProvidenceAutomationServiceTest {

    @Mock private SecretariatOperationalRoutingResolver routingResolver;
    @Mock private WorkItemRepository workItemRepository;
    @Mock private SecretariatQueueProjectionService queueProjectionService;
    @Mock private SecretariatOperationalAssignmentService assignmentService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SecretariatProcessContactEnvelopeResolver contactEnvelopeResolver;

    private MagistraturaJudicialProvidenceAutomationService service;

    @BeforeEach
    void setUp() {
        MagistraturaJudicialProvidencePlanningSupport planningSupport = new MagistraturaJudicialProvidencePlanningSupport(assignmentService, contactEnvelopeResolver);
        MagistraturaJudicialProvidenceDispatchSupport dispatchSupport = new MagistraturaJudicialProvidenceDispatchSupport(workItemRepository, queueProjectionService, assignmentService, contactEnvelopeResolver);
        service = new MagistraturaJudicialProvidenceAutomationService(routingResolver, planningSupport, dispatchSupport);
        when(contactEnvelopeResolver.participantSnapshots(any(Processo.class))).thenReturn(List.of(Map.of("role", "AUTOR", "nome", "Autor")));
        when(contactEnvelopeResolver.buildEnvelope(any(Processo.class))).thenReturn(Map.of("contactReadyCount", 1L, "contactMissingCount", 0L, "advogados", List.of()));
    }

    @Test
    void previewDeveSugerirProvidenciasQuandoDespachoDeterminaAudienciaEIntimacao() {
        Processo processo = processo();
        Usuario usuario = usuario();
        when(routingResolver.resolve(processo)).thenReturn(profile());
        when(assignmentService.avaliar(any(Processo.class), any(SecretariatOperationalRoutingProfile.class), any(String.class)))
                .thenReturn(new SecretariatOperationalAssignmentService.AssignmentSnapshot("AUDIENCIA", "CELL", List.of(), null, null, List.of(), List.of(), Map.of()));

        var request = new MagistraturaJudicialActCommandRequest(
                "DESPACHO",
                "Designo audiência de instrução e intime-se as partes.",
                "CPC",
                null,
                null,
                "Sala 01",
                Instant.parse("2026-05-10T13:00:00Z"),
                null,
                null,
                null,
                null,
                "Urgente",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var response = service.preview(processo, usuario, MagistraturaJudicialActCode.DESPACHO, request);

        assertThat(response).extracting(it -> it.code())
                .contains(MagistraturaJudicialProvidenceCode.PREPARAR_AUDIENCIA,
                        MagistraturaJudicialProvidenceCode.EXPEDIR_INTIMACOES);
        assertThat(response).anySatisfy(item -> {
            if (item.code() == MagistraturaJudicialProvidenceCode.PREPARAR_AUDIENCIA) {
                assertThat(item.targetInboxKey()).isEqualTo("SECRETARIA_AUDIENCIA_CIVEL");
                assertThat(item.targetPanelRoute()).contains("/api/v1/secretariat/operacional/snapshot")
                        .contains("stage=AUDIENCIA")
                        .contains("unidadeCodigo=TJCE-1VC-FOR")
                        .contains("secretariaCodigo=SECRETARIA_1_VARA_CIVEL_FORTALEZA");
                assertThat(item.metrics()).containsEntry("reuseOperationalMesh", true);
                assertThat(item.metrics()).containsEntry("eventoConclusao", "AUDIENCIA_PREPARADA");
                assertThat(item.metrics()).containsEntry("routingUnitCode", "TJCE-1VC-FOR");
                assertThat(item.metrics()).containsEntry("authorityUnitCode", "TJCE-1VC-FOR");
            }
        });
    }

    @Test
    void previewDeveProjetarVistaTecnicaCalculoERedistribuicaoSemCriarNovaMalha() {
        Processo processo = processo();
        Usuario usuario = usuario();
        when(routingResolver.resolve(processo)).thenReturn(profile());
        when(assignmentService.avaliar(any(Processo.class), any(SecretariatOperationalRoutingProfile.class), any(String.class)))
                .thenReturn(new SecretariatOperationalAssignmentService.AssignmentSnapshot("SANEAMENTO", "CELL", List.of(), null, null, List.of(), List.of(), Map.of()));
        when(usuarioRepository.findByCpf("12345678900")).thenReturn(Optional.of(parteAutora()));

        var request = new MagistraturaJudicialActCommandRequest(
                "DESPACHO",
                "Abra-se vista ao Ministério Público, remetam-se os autos à contadoria para cálculo e redistribua-se por prevenção.",
                "CPC e regimento interno",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Providências encadeadas.",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var response = service.preview(processo, usuario, MagistraturaJudicialActCode.DESPACHO, request);

        assertThat(response).extracting(it -> it.code())
                .contains(
                        MagistraturaJudicialProvidenceCode.ABRIR_VISTA_TECNICA,
                        MagistraturaJudicialProvidenceCode.CONTROLAR_CALCULO_LIQUIDACAO,
                        MagistraturaJudicialProvidenceCode.REDISTRIBUIR_OU_PREVENIR
                );
        assertThat(response).anySatisfy(item -> {
            if (item.code() == MagistraturaJudicialProvidenceCode.REDISTRIBUIR_OU_PREVENIR) {
                assertThat(item.targetPanelRoute()).contains("/redistribuicao");
                assertThat(item.metrics()).containsEntry("reuseOperationalMesh", true);
                                @SuppressWarnings("unchecked")
                List<String> dependencias = (List<String>) item.metrics().get("dependencias");
                assertThat(dependencias).contains("fundamento de competência/prevenção");
            }
        });
    }

    @Test
    void dispatchDeveReutilizarWorkItemDeAudienciaQuandoOAtoJaCriouFilaNativa() {
        Processo processo = processo();
        Usuario usuario = usuario();
        WorkItem existing = WorkItem.builder()
                .id(55L)
                .processo(processo)
                .type(WorkItemType.AUDIENCIA)
                .status(WorkItemStatus.PENDENTE)
                .titulo("Audiência")
                .inboxKey("LEGADO")
                .queueCode("LEGADO")
                .build();

        when(routingResolver.resolve(processo)).thenReturn(profile());
        when(assignmentService.avaliar(any(Processo.class), any(SecretariatOperationalRoutingProfile.class), any(String.class)))
                .thenReturn(new SecretariatOperationalAssignmentService.AssignmentSnapshot("AUDIENCIA", "CELL", List.of(), null, null, List.of(), List.of(), Map.of()));
        when(workItemRepository.findById(55L)).thenReturn(Optional.of(existing));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new MagistraturaJudicialActCommandRequest(
                "DESIGNAR_AUDIENCIA",
                "Audiência designada.",
                "CPC",
                null,
                "AUDIENCIA_DE_INSTRUCAO",
                "Sala 03",
                Instant.parse("2026-05-10T13:00:00Z"),
                null,
                null,
                null,
                null,
                "Necessário colher prova oral.",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var response = service.dispatch(
                processo,
                usuario,
                MagistraturaJudicialActCode.DESIGNAR_AUDIENCIA,
                request,
                Map.of("workItemId", 55L, "dataHora", Instant.parse("2026-05-10T13:00:00Z"))
        );

        assertThat(response).anySatisfy(item -> {
            if (item.code() == MagistraturaJudicialProvidenceCode.PREPARAR_AUDIENCIA) {
                assertThat(item.reusedExistingWorkItem()).isTrue();
                assertThat(item.workItemId()).isEqualTo(55L);
                assertThat(item.targetInboxKey()).isEqualTo("SECRETARIA_AUDIENCIA_CIVEL");
                assertThat(item.targetPanelRoute()).contains("/api/v1/secretariat/operacional/snapshot")
                        .contains("stage=AUDIENCIA")
                        .contains("unidadeCodigo=TJCE-1VC-FOR");
                assertThat(item.metrics()).containsEntry("eventoConclusao", "AUDIENCIA_PREPARADA");
                assertThat(item.metrics()).containsEntry("routingCellCode", "SECRETARIA_1_VARA_CIVEL_FORTALEZA_PAUTA_AUDIENCIA");
            }
        });
        verify(queueProjectionService).upsert(any(WorkItem.class), anyInt(), any(List.class), any(Map.class));
    }


    @Test
    void previewDeveProjetarAutoridadeDeDesembargadorSemDocumentoAuxiliar() {
        Processo processo = processo();
        Usuario usuario = usuarioDesembargador();
        when(routingResolver.resolve(processo)).thenReturn(profileSegundoGrau());
        when(assignmentService.avaliar(any(Processo.class), any(SecretariatOperationalRoutingProfile.class), any(String.class)))
                .thenReturn(new SecretariatOperationalAssignmentService.AssignmentSnapshot("COLEGIADO", "CELL_RELATOR", List.of(), null, null, List.of(), List.of(), Map.of()));

        var request = new MagistraturaJudicialActCommandRequest(
                "VOTO_COLEGIADO",
                "Inclua-se em pauta e, após, lavre-se o acórdão.",
                "CPC e regimento interno",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Sessão colegiada.",
                null,
                "Conheço e nego provimento.",
                "Negado provimento.",
                null,
                "CAMARA CIVEL",
                null,
                null,
                null
        );

        var response = service.preview(processo, usuario, MagistraturaJudicialActCode.VOTO_COLEGIADO, request);

        assertThat(response).anySatisfy(item -> {
            if (item.code() == MagistraturaJudicialProvidenceCode.REMETER_COLEGIADO_OU_PLENARIO) {
                assertThat(item.metrics()).containsEntry("authorityScope", "SEGUNDO_GRAU");
                assertThat(item.metrics()).containsEntry("authorityAxis", "DESEMBARGADOR_RELATOR");
                assertThat(item.metrics()).containsEntry("judgmentAxis", "COLEGIADO");
                assertThat(item.metrics()).containsEntry("authorityPanelRoute", "/api/v1/desembargador/colegiado/processos/77/malha");
                assertThat(item.metrics()).containsEntry("authorityOrgao", "CAMARA CIVEL");
                assertThat(item.metrics()).containsEntry("authorityJusticeAxis", "ESTADUAL");
                assertThat(item.metrics()).containsEntry("authorityInstitutionalPanelCode", "MAGISTRATURA_DESEMBARGADOR_ESTADUAL_TJCE");
                assertThat(item.metrics()).containsEntry("authorityUnitCode", "TJCE-2G-CAMARA-CIVEL");
                assertThat(item.metrics()).containsEntry("authorityUnitLabel", "CAMARA CIVEL");
                assertThat(item.metrics().get("authorityInstitutionalLandingPath")).asString().contains("unidadeCodigo=TJCE-2G-CAMARA-CIVEL");
            }
        });
    }

    @Test
    void previewDeveProjetarAutoridadeDeMinistroParaPautaEPlenarioSemArquivoTexto() {
        Processo processo = processo();
        Usuario usuario = usuarioMinistro();
        when(routingResolver.resolve(processo)).thenReturn(profileSuperior());
        when(assignmentService.avaliar(any(Processo.class), any(SecretariatOperationalRoutingProfile.class), any(String.class)))
                .thenReturn(new SecretariatOperationalAssignmentService.AssignmentSnapshot("PAUTA", "CELL_PLENARIO", List.of(), null, null, List.of(), List.of(), Map.of()));

        var request = new MagistraturaJudicialActCommandRequest(
                "INCLUSAO_PAUTA",
                "Inclua-se em pauta para julgamento no Plenário.",
                "RISTF",
                null,
                null,
                null,
                Instant.parse("2026-06-01T17:00:00Z"),
                null,
                null,
                null,
                null,
                "Tema com repercussão geral.",
                null,
                null,
                null,
                null,
                "PLENARIO",
                null,
                null,
                null
        );

        var response = service.preview(processo, usuario, MagistraturaJudicialActCode.INCLUSAO_PAUTA, request);

        assertThat(response).anySatisfy(item -> {
            if (item.code() == MagistraturaJudicialProvidenceCode.REMETER_COLEGIADO_OU_PLENARIO) {
                assertThat(item.metrics()).containsEntry("authorityScope", "SUPERIOR");
                assertThat(item.metrics()).containsEntry("authorityAxis", "MINISTRO_RELATOR");
                assertThat(item.metrics()).containsEntry("judgmentAxis", "PLENARIO");
                assertThat(item.metrics()).containsEntry("authorityPanelRoute", "/api/v1/ministro/plenario/processos/77/pauta");
                assertThat(item.metrics()).containsEntry("authorityReturnRoute", "/api/v1/ministro/plenario/processos/77/malha");
                assertThat(item.metrics()).containsEntry("authorityJusticeAxis", "SUPERIOR");
                assertThat(item.metrics()).containsEntry("authorityInstitutionalPanelCode", "MAGISTRATURA_MINISTRO_STJ");
                assertThat(item.metrics()).containsEntry("authorityUnitCode", "STJ-PLENARIO-01");
                assertThat(item.metrics()).containsEntry("authorityUnitLabel", "PLENARIO");
                assertThat(item.metrics().get("authorityInstitutionalLandingPath")).asString().contains("unidadeCodigo=STJ-PLENARIO-01");
            }
        });
    }

    private Processo processo() {
        Processo processo = Processo.builder().id(77L).numeroProcesso("0001234-56.2026.8.06.0001").build();
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setTribunalCodigoRoteado("TJCE");
        processo.setUnidadeJudiciariaCodigo("TJCE-1VC-FOR");
        processo.setVara("1ª Vara Cível de Fortaleza");
        processo.setParteAutoraNome("Maria da Silva");
        processo.setParteAutoraCpf("12345678900");
        processo.setParteReuNome("João Souza");
        processo.setParteReuCpf("99888777666");
        return processo;
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setTipoUsuario(TipoUsuario.JUIZ_ESTADUAL);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setPerfil("TJCE|TJCE-1VC-FOR|1 VARA CIVEL DE FORTALEZA|SECRETARIA_1_VARA_CIVEL_FORTALEZA");
        return usuario;
    }

    private Usuario parteAutora() {
        Usuario usuario = new Usuario();
        usuario.setId(101L);
        usuario.setNome("Maria da Silva");
        usuario.setEmail("maria@exemplo.com");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        return usuario;
    }

    private Usuario usuarioDesembargador() {
        Usuario usuario = new Usuario();
        usuario.setId(29L);
        usuario.setTipoUsuario(TipoUsuario.DESEMBARGADOR);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setPerfil("TJCE|TJCE-2G-CAMARA-CIVEL|CAMARA CIVEL|SECRETARIA_CAMARA_CIVEL");
        return usuario;
    }

    private Usuario usuarioMinistro() {
        Usuario usuario = new Usuario();
        usuario.setId(39L);
        usuario.setTipoUsuario(TipoUsuario.MINISTRO);
        usuario.setUf("DF");
        usuario.setComarca("Brasília");
        usuario.setPerfil("STJ|STJ-PLENARIO-01|PLENARIO|SECRETARIA_PLENARIO");
        return usuario;
    }

    private SecretariatOperationalRoutingProfile profileSegundoGrau() {
        return new SecretariatOperationalRoutingProfile(
                "TJCE:2G:CIVEL",
                "JUSTICA_ESTADUAL",
                "TJCE",
                "SEGUNDO_GRAU",
                "COLEGIADO",
                "CIVEL",
                "CAMARA_CIVEL",
                "SECRETARIA_CAMARA_CIVEL",
                "SECRETARIA_RECEBIMENTO_RECURSAL",
                "SECRETARIA_RECEBIMENTO_RECURSAL",
                "SECRETARIA_SANEAMENTO_RECURSAL",
                "SECRETARIA_SANEAMENTO_RECURSAL",
                "SECRETARIA_PAUTA_RECURSAL",
                "SECRETARIA_PAUTA_RECURSAL",
                "SECRETARIA_EXECUCAO_RECURSAL",
                "SECRETARIA_EXECUCAO_RECURSAL",
                "SALA_CAMARA",
                "TJCE/FORTALEZA/CAMARA_CIVEL",
                Duration.ofHours(4),
                Duration.ofHours(8),
                Duration.ofHours(24),
                70,
                true,
                true,
                false,
                true,
                List.of("Conferir pauta colegiada"),
                List.of("RECURSAL"),
                null,
                null,
                Map.of(
                        "unidadeJudiciariaCodigo", "TJCE-2G-CAMARA-CIVEL",
                        "vara", "CAMARA CIVEL",
                        "comarca", "Fortaleza",
                        "tribunalFlow", Map.of("queueCodes", Map.of(
                                "sessao", "CAMARA:SESSAO",
                                "gabineteRelator", "CAMARA:RELATOR",
                                "acordao", "CAMARA:ACORDAO",
                                "pauta", "CAMARA:PAUTA"
                        ))
                )
        );
    }

    private SecretariatOperationalRoutingProfile profileSuperior() {
        return new SecretariatOperationalRoutingProfile(
                "STJ:SUP:PUBLICO",
                "JUSTICA_SUPERIOR",
                "STJ",
                "SUPERIOR",
                "PLENARIO",
                "CIVEL",
                "PLENARIO",
                "SECRETARIA_PLENARIO",
                "SECRETARIA_RECEBIMENTO_SUPERIOR",
                "SECRETARIA_RECEBIMENTO_SUPERIOR",
                "SECRETARIA_SANEAMENTO_SUPERIOR",
                "SECRETARIA_SANEAMENTO_SUPERIOR",
                "SECRETARIA_PAUTA_SUPERIOR",
                "SECRETARIA_PAUTA_SUPERIOR",
                "SECRETARIA_EXECUCAO_SUPERIOR",
                "SECRETARIA_EXECUCAO_SUPERIOR",
                "SALA_PLENARIO",
                "STJ/BRASILIA/PLENARIO",
                Duration.ofHours(4),
                Duration.ofHours(8),
                Duration.ofHours(24),
                90,
                true,
                true,
                false,
                true,
                List.of("Conferir pauta plenária"),
                List.of("SUPERIOR"),
                null,
                null,
                Map.of(
                        "unidadeJudiciariaCodigo", "STJ-PLENARIO-01",
                        "vara", "PLENARIO",
                        "comarca", "Brasília",
                        "tribunalFlow", Map.of("queueCodes", Map.of(
                                "sessao", "PLENARIO:SESSAO",
                                "gabineteRelator", "PLENARIO:RELATOR",
                                "acordao", "PLENARIO:ACORDAO",
                                "pauta", "PLENARIO:PAUTA"
                        ))
                )
        );
    }

    private SecretariatOperationalRoutingProfile profile() {
        return new SecretariatOperationalRoutingProfile(
                "TJCE:1G:CIVEL",
                "JUSTICA_ESTADUAL",
                "TJCE",
                "PRIMEIRO_GRAU",
                "COMUM",
                "CIVEL",
                "VARA_CIVEL",
                "SECRETARIA_1_VARA_CIVEL_FORTALEZA",
                "SECRETARIA_RECEBIMENTO_CIVEL",
                "SECRETARIA_RECEBIMENTO_CIVEL",
                "SECRETARIA_SANEAMENTO_CIVEL",
                "SECRETARIA_SANEAMENTO_CIVEL",
                "SECRETARIA_AUDIENCIA_CIVEL",
                "SECRETARIA_AUDIENCIA_CIVEL",
                "SECRETARIA_EXECUCAO_CIVEL",
                "SECRETARIA_EXECUCAO_CIVEL",
                "SALA_AUD",
                "TJCE/FORTALEZA/1VC",
                Duration.ofHours(4),
                Duration.ofHours(6),
                Duration.ofHours(24),
                60,
                true,
                true,
                false,
                true,
                List.of("Conferir pauta"),
                List.of("CIVEL"),
                null,
                null,
                Map.of(
                        "unidadeJudiciariaCodigo", "TJCE-1VC-FOR",
                        "vara", "1ª Vara Cível de Fortaleza",
                        "comarca", "Fortaleza"
                )
        );
    }
}
