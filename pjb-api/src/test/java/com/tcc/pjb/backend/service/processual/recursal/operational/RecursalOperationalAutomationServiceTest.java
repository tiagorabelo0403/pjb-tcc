package com.tcc.pjb.backend.service.processual.recursal.operational;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalOperationalAutomationService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecursalOperationalAutomationServiceTest {

    @Mock private SecretariatOperationalRoutingResolver routingResolver;
    @Mock private SecretariatOperationalAssignmentService assignmentService;
    @Mock private SecretariatQueueProjectionService projectionService;
    @Mock private WorkItemRepository workItemRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SecretariatProcessContactEnvelopeResolver contactEnvelopeResolver;

    private RecursalOperationalAutomationService service;

    @BeforeEach
    void setUp() {
        service = new RecursalOperationalAutomationService(routingResolver, assignmentService, projectionService, workItemRepository, usuarioRepository, contactEnvelopeResolver);
        when(contactEnvelopeResolver.participantSnapshots(any(Processo.class))).thenReturn(List.of(Map.of("role", "AUTOR", "nome", "Autor")));
        when(contactEnvelopeResolver.buildEnvelope(any(Processo.class))).thenReturn(Map.of("contactReadyCount", 1L, "contactMissingCount", 0L, "advogados", List.of()));
    }

    @Test
    void materializeDeveProjetarEmbargosParaPainelColegiadoComServidorResponsavel() {
        Processo processo = processo();
        Usuario ator = ator();
        Usuario servidor = servidor();
        WorkItem peticao = workItem(11L, WorkItemType.PETICAO, "PETICAO");
        WorkItem recurso = workItem(22L, WorkItemType.RECURSO, "RECURSO");
        SecretariatOperationalRoutingProfile routing = profile();
        SecretariatOperationalAssignmentService.AssignmentSnapshot snapshot = new SecretariatOperationalAssignmentService.AssignmentSnapshot(
                "EMBARGOS", "CELL_EMBARGOS", List.of(), servidor, null, List.of(), List.of(), Map.of()
        );

        when(routingResolver.resolve(processo)).thenReturn(routing);
        when(assignmentService.avaliar(any(Processo.class), any(SecretariatOperationalRoutingProfile.class), any(String.class)))
                .thenReturn(snapshot);
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, Object> result = service.materialize(processo, ator, "EMBARGOS DE DECLARACAO", peticao, recurso);

        Map<String, Object> recursoPayload = cast(result.get("recurso"));
        assertThat(recursoPayload.get("stage")).isEqualTo("EMBARGOS");
        assertThat(recursoPayload.get("panelRoute")).asString().contains("/processos/77/")
                .contains("stage=EMBARGOS")
                .contains("unidadeCodigo=TJCE-2G-CAMARA-CIVEL");
        assertThat(recursoPayload.get("assignedUserId")).isEqualTo(91L);
        assertThat(recursoPayload.get("cellCode")).isEqualTo("SECRETARIA_CAMARA_CIVEL_EMBARGOS_DECLARACAO");
        assertThat(recursoPayload.get("completionEvent")).isEqualTo("EMBARGOS_ENCAMINHADOS");
        assertThat(cast(result.get("peticao")).get("stage")).isEqualTo("RECEBIMENTO");
        assertThat(result).containsEntry("authorityScope", "SEGUNDO_GRAU");
        assertThat(result).containsEntry("authorityAxis", "DESEMBARGADOR_RELATOR");
        assertThat(result).containsEntry("authorityPanelRoute", "/api/v1/desembargador/colegiado/processos/77/malha");
        assertThat(result).containsEntry("authorityJusticeAxis", "ESTADUAL");
        assertThat(result).containsEntry("authorityInstitutionalPanelCode", "MAGISTRATURA_DESEMBARGADOR_ESTADUAL_TJCE");
        assertThat(result).containsEntry("authorityUnitCode", "TJCE-2G-CAMARA-CIVEL");
        assertThat(result).containsEntry("routingUnitCode", "TJCE-2G-CAMARA-CIVEL");
        verify(projectionService, atLeastOnce()).upsert(any(WorkItem.class), anyInt(), any(List.class), any(Map.class));
    }

    @Test
    void materializeDeveProjetarApelacaoParaAdmissibilidadeSemNovaMalha() {
        Processo processo = processo();
        Usuario ator = ator();
        Usuario servidor = servidor();
        WorkItem peticao = workItem(31L, WorkItemType.PETICAO, "PETICAO");
        WorkItem recurso = workItem(32L, WorkItemType.RECURSO, "RECURSO");

        when(routingResolver.resolve(processo)).thenReturn(profile());
        when(assignmentService.avaliar(any(Processo.class), any(SecretariatOperationalRoutingProfile.class), any(String.class)))
                .thenReturn(new SecretariatOperationalAssignmentService.AssignmentSnapshot("ADMISSIBILIDADE", "CELL_ADM", List.of(), servidor, null, List.of(), List.of(), Map.of()));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = service.materialize(processo, ator, "APELACAO", peticao, recurso);

        Map<String, Object> recursoPayload = cast(result.get("recurso"));
        assertThat(recursoPayload.get("stage")).isEqualTo("ADMISSIBILIDADE");
        assertThat(recursoPayload.get("panelRoute")).asString().contains("/api/v1/processual/recursal/admissibilidade")
                .contains("stage=ADMISSIBILIDADE")
                .contains("unidadeCodigo=TJCE-2G-CAMARA-CIVEL");
        assertThat(((List<?>) recursoPayload.get("nextStages")).stream().map(String::valueOf).toList()).contains("contrarrazões", "colegiado", "acórdão");
        assertThat(result).containsEntry("expectedReturn", "recurso qualificado para admissibilidade, contrarrazões e remessa ao colegiado");
        assertThat(result).containsEntry("authorityScope", "SEGUNDO_GRAU");
        assertThat(result).containsEntry("judgmentAxis", "COLEGIADO");
        assertThat(result).containsEntry("sessionRoute", "/api/v1/secretaria/operacional/colegiado/processos/77/pauta");
        assertThat(result).containsEntry("routingUnitCode", "TJCE-2G-CAMARA-CIVEL");
    }


    @Test
    void materializeDeveInferirPainelDeMinistroQuandoRecursoVaiParaCorteSuperior() {
        Processo processo = processo();
        processo.setTribunalCodigoRoteado("STJ");
        Usuario ator = ator();
        WorkItem peticao = workItem(41L, WorkItemType.PETICAO, "PETICAO");
        WorkItem recurso = workItem(42L, WorkItemType.RECURSO, "RECURSO");

        when(routingResolver.resolve(processo)).thenReturn(profileSuperior());
        when(assignmentService.avaliar(any(Processo.class), any(SecretariatOperationalRoutingProfile.class), any(String.class)))
                .thenReturn(new SecretariatOperationalAssignmentService.AssignmentSnapshot("ADMISSIBILIDADE", "CELL_STJ", List.of(), servidor(), null, List.of(), List.of(), Map.of()));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = service.materialize(processo, ator, "RECURSO ESPECIAL", peticao, recurso);

        assertThat(result).containsEntry("authorityScope", "SUPERIOR");
        assertThat(result).containsEntry("authorityAxis", "MINISTRO_RELATOR");
        assertThat(result).containsEntry("judgmentAxis", "PLENARIO");
        assertThat(result).containsEntry("authorityPanelRoute", "/api/v1/ministro/plenario/processos/77/malha");
        assertThat(result).containsEntry("authorityInstitutionalPanelCode", "MAGISTRATURA_MINISTRO_STJ");
        assertThat(result).containsEntry("authorityUnitCode", "STJ-PLENARIO-01");
        assertThat(result.get("authorityInstitutionalLandingPath")).asString().contains("unidadeCodigo=STJ-PLENARIO-01");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }

    private Processo processo() {
        Processo processo = Processo.builder().id(77L).numeroProcesso("0001234-56.2026.8.06.0001").build();
        processo.setNumeroUnificado("0001234-56.2026.8.06.0001");
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setTribunalCodigoRoteado("TJCE");
        processo.setUnidadeJudiciariaCodigo("TJCE-2G-CAMARA-CIVEL");
        processo.setVara("CAMARA CIVEL");
        processo.setParteAutoraNome("Maria da Silva");
        processo.setParteAutoraCpf("12345678900");
        processo.setParteReuNome("João Souza");
        processo.setParteReuCpf("99888777666");
        return processo;
    }

    private Usuario ator() {
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setNome("Advogado Recorrente");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        return usuario;
    }

    private Usuario servidor() {
        Usuario usuario = new Usuario();
        usuario.setId(91L);
        usuario.setNome("Servidor Colegiado");
        usuario.setEmail("colegiado@tribunal.jus.br");
        usuario.setTipoUsuario(TipoUsuario.SERVIDOR_FORUM);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        return usuario;
    }

    private Usuario parte(String nome, String email, TipoUsuario tipoUsuario) {
        Usuario usuario = new Usuario();
        usuario.setId((long) nome.hashCode());
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setTipoUsuario(tipoUsuario);
        return usuario;
    }

    private WorkItem workItem(Long id, WorkItemType type, String code) {
        return WorkItem.builder()
                .id(id)
                .processo(processo())
                .type(type)
                .status(WorkItemStatus.PENDENTE)
                .titulo(code)
                .inboxKey("LEGADO")
                .queueCode("LEGADO")
                .build();
    }


    private SecretariatOperationalRoutingProfile profileSuperior() {
        return new SecretariatOperationalRoutingProfile(
                "STJ:SUP:CIVEL",
                "JUSTICA_SUPERIOR",
                "STJ",
                "SUPERIOR",
                "PLENARIO",
                "CIVEL",
                "PLENARIO",
                "SECRETARIA_STJ_PLENARIO",
                "SECRETARIA_RECEBIMENTO_STJ",
                "SECRETARIA_RECEBIMENTO_STJ",
                "SECRETARIA_SANEAMENTO_STJ",
                "SECRETARIA_SANEAMENTO_STJ",
                "SECRETARIA_PAUTA_STJ",
                "SECRETARIA_PAUTA_STJ",
                "SECRETARIA_EXECUCAO_STJ",
                "SECRETARIA_EXECUCAO_STJ",
                "SALA_STJ",
                "STJ/BRASILIA/PLENARIO",
                Duration.ofHours(4),
                Duration.ofHours(8),
                Duration.ofHours(24),
                90,
                true,
                true,
                false,
                true,
                List.of("Conferir admissibilidade superior"),
                List.of("SUPERIOR"),
                null,
                null,
                Map.of(
                        "unidadeJudiciariaCodigo", "STJ-PLENARIO-01",
                        "vara", "PLENARIO",
                        "comarca", "Brasília",
                        "tribunalFlow", Map.of("queueCodes", Map.of(
                                "sessao", "STJ:SESSAO",
                                "gabineteRelator", "STJ:RELATOR",
                                "admissibilidade", "STJ:ADMISSIBILIDADE",
                                "acordao", "STJ:ACORDAO"
                        ))
                )
        );
    }

    private SecretariatOperationalRoutingProfile profile() {
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
                List.of("Conferir admissibilidade"),
                List.of("CIVEL", "RECURSAL"),
                null,
                null,
                Map.of(
                        "unidadeJudiciariaCodigo", "TJCE-2G-CAMARA-CIVEL",
                        "vara", "CAMARA CIVEL",
                        "comarca", "Fortaleza",
                        "tribunalFlow", Map.of("queueCodes", Map.of(
                                "sessao", "CAMARA:SESSAO",
                                "gabineteRelator", "CAMARA:RELATOR",
                                "embargos", "CAMARA:EMBARGOS",
                                "acordao", "CAMARA:ACORDAO",
                                "admissibilidade", "CAMARA:ADMISSIBILIDADE"
                        ))
                )
        );
    }
}
