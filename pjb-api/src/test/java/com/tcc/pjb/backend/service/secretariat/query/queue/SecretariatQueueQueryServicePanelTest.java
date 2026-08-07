package com.tcc.pjb.backend.service.secretariat.query.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioResolver;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAgendaSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelSnapshotDto;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import com.tcc.pjb.backend.service.secretariat.governance.SecretariatGovernanceService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalActionModelService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalDeskModelService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalTransactionModelService;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatDeskLoadProfile;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatDeskLoadResolver;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatInstitutionalAlignmentService;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatJudicialReferenceModelService;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatFlowBridgeResolver;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatHearingMediaLaneService;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatJudicialIntegrationResolver;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatMigrationLaneService;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver.SecretariatSpecializationProfile;
import com.tcc.pjb.backend.service.ui.UiHintService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecretariatQueueQueryServicePanelTest {

    private SecretariatQueueItemRepository repo;
    private SecretariatQueueQueryService service;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        repo = mock(SecretariatQueueItemRepository.class);
        mapper = new ObjectMapper();
        UiHintService uiHintService = mock(UiHintService.class);
        ForumDeskPortfolioResolver portfolioResolver = mock(ForumDeskPortfolioResolver.class);
        SecretariatQueueLoadResolver loadResolver = mock(SecretariatQueueLoadResolver.class);
        SecretariatDeskLoadResolver deskLoadResolver = mock(SecretariatDeskLoadResolver.class);
        SecretariatFlowBridgeResolver flowBridgeResolver = mock(SecretariatFlowBridgeResolver.class);
        SecretariatJudicialIntegrationResolver integrationResolver = mock(SecretariatJudicialIntegrationResolver.class);
        SecretariatInstitutionalVisibilityService visibilityService = mock(SecretariatInstitutionalVisibilityService.class);
        SecretariatJudicialReferenceModelService referenceModelService = mock(SecretariatJudicialReferenceModelService.class);
        SecretariatInstitutionalAlignmentService institutionalAlignmentService = mock(SecretariatInstitutionalAlignmentService.class);
        SecretariatOperationalDeskModelService operationalDeskModelService = mock(SecretariatOperationalDeskModelService.class);
        SecretariatOperationalActionModelService operationalActionModelService = mock(SecretariatOperationalActionModelService.class);
        SecretariatOperationalTransactionModelService operationalTransactionModelService = mock(SecretariatOperationalTransactionModelService.class);
        SecretariatMigrationLaneService migrationLaneService = mock(SecretariatMigrationLaneService.class);
        SecretariatHearingMediaLaneService hearingMediaLaneService = mock(SecretariatHearingMediaLaneService.class);

        when(portfolioResolver.resolve(any())).thenReturn(new ForumDeskPortfolioProfile(
            "TRIAGE",
            "GABINETE",
            "AUDIENCIA",
            "COMPLIANCE",
            "ESCALATION",
            "ASSISTANT",
            "COORD",
            "REDIST",
            "FORUM_OPERATIONS",
            List.of("FORUM"),
            new LinkedHashMap<>()
        ));
        when(loadResolver.resolve(eq("inbox.secretaria"), any())).thenReturn(new SecretariatQueueLoadProfile(
            "inbox.secretaria", 3, 0, 1, 1, "HIGH", "FLOW_STANDARD", false, List.of("PRESSURE"), new LinkedHashMap<>()
        ));
        when(deskLoadResolver.resolve(eq("inbox.secretaria"), any(), any())).thenReturn(new SecretariatDeskLoadProfile(
            "inbox.secretaria", "AUDIENCIA", 3, 0, 0, 0, 1, "HIGH", "REDIST", "GABINETE", "COORD", false, false, true, List.of("AUDIENCIA"), new LinkedHashMap<>()
        ));
        when(visibilityService.describeAuthorizedInbox("inbox.secretaria")).thenReturn(
            new SecretariatInstitutionalVisibilityService.SecretariatInboxInstitutionalProfile(
                "inbox.secretaria",
                new SecretariatInstitutionalVisibilityService.ActorSecretariatScope("TJCE", "PRIMEIRO_GRAU", "ESTADUAL", "CE", "Fortaleza", "VARA_1", "SEC_1", true),
                new SecretariatSpecializationProfile("SECRETARIA_FORUM", "PRIMEIRO_GRAU", "ESTADUAL", "forum.ce", "painel-forum", "painel-forum", "SEC_1", "Secretaria Vara 1", "inbox.secretaria", List.of("AUDIENCIA"), Map.of())
            )
        );

        SecretariatQueueInboxContextResolver inboxContextResolver = new SecretariatQueueInboxContextResolver(
            visibilityService,
            portfolioResolver,
            loadResolver,
            deskLoadResolver
        );
        SecretariatGovernanceService governanceService = mock(SecretariatGovernanceService.class);
        SecretariatQueueSummaryAssembler summaryAssembler = mock(SecretariatQueueSummaryAssembler.class);

        service = new SecretariatQueueQueryService(
            repo,
            mock(ProcessoRepository.class),
            mapper,
            uiHintService,
            inboxContextResolver,
            flowBridgeResolver,
            integrationResolver,
            governanceService,
            summaryAssembler,
            referenceModelService,
            institutionalAlignmentService,
            operationalDeskModelService,
            operationalActionModelService,
            operationalTransactionModelService,
            migrationLaneService,
            hearingMediaLaneService
        );
    }

    @Test
    void shouldBuildPanelGroupedByProcessRitoVaraAndDate() throws Exception {
        SecretariatQueueItem one = item(1L, 101L, "Pauta audiência", 1, Instant.parse("2026-04-20T14:00:00Z"), mapOf(
            "processoNumero", "0001-11.2026.8.06.0001",
            "ritoProcessual", "CIVEL_COMUM",
            "routingVaraLabel", "Vara 1",
            "panelDateBucket", "2026-04-20",
            "stage", "AUDIENCIA"
        ));
        SecretariatQueueItem two = item(2L, 101L, "Intimação de partes", 2, Instant.parse("2026-04-20T16:00:00Z"), mapOf(
            "processoNumero", "0001-11.2026.8.06.0001",
            "ritoProcessual", "CIVEL_COMUM",
            "routingVaraLabel", "Vara 1",
            "panelDateBucket", "2026-04-20",
            "stage", "INTIMACAO"
        ));
        SecretariatQueueItem three = item(3L, 202L, "Citação inicial", 1, Instant.parse("2026-04-21T10:00:00Z"), mapOf(
            "processoNumero", "0002-22.2026.8.06.0001",
            "ritoProcessual", "JUIZADO_ESPECIAL_CIVEL",
            "routingVaraLabel", "Juizado 2",
            "panelDateBucket", "2026-04-21",
            "stage", "CITACAO"
        ));

        when(repo.listInbox(eq("inbox.secretaria"), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(one, two, three)));

        SecretariatQueuePanelSnapshotDto snapshot = service.panel("inbox.secretaria", List.of("PENDENTE", "EM_EXECUCAO"), 120);

        assertThat(snapshot.byProcesso()).hasSize(2);
        assertThat(snapshot.byProcesso().get(0).groupLabel()).isEqualTo("0001-11.2026.8.06.0001");
        assertThat(snapshot.byProcesso().get(0).itemCount()).isEqualTo(2);
        assertThat(snapshot.byRito()).extracting(group -> group.groupLabel()).containsExactly("CIVEL_COMUM", "JUIZADO_ESPECIAL_CIVEL");
        assertThat(snapshot.byVara()).extracting(group -> group.groupLabel()).containsExactly("Vara 1", "Juizado 2");
        assertThat(snapshot.byData()).extracting(group -> group.groupLabel()).containsExactly("2026-04-20", "2026-04-21");
        assertThat(snapshot.metadata()).containsEntry("totalItems", 3);
        assertThat(snapshot.byProcesso().get(0).items().get(0).actionContracts()).extracting(contract -> contract.actionCode())
            .contains("CONFIRMAR_LOCAL_AGENDA", "CONFIRMAR_INTIMACAO_PARTICIPANTES", "REGISTRAR_COMPARECIMENTO", "REGISTRAR_EVENTO_REAL", "EXECUTAR_RETORNO_PROCESSO");
    }


    @Test
    void shouldBuildAgendaWithContactsAndRitoCoverage() throws Exception {
        SecretariatQueueItem one = item(10L, 301L, "Audiência de instrução", 1, Instant.parse("2026-04-22T14:00:00Z"), mapOf(
            "processoNumero", "0301-11.2026.8.06.0001",
            "ritoProcessual", "JUIZADO_ESPECIAL_CIVEL",
            "routingVaraLabel", "Juizado 1",
            "routingSecretariatLabel", "Secretaria Juizado 1",
            "routingCellCode", "SEC_JEC_AUDIENCIA",
            "routingComarcaLabel", "Fortaleza",
            "tribunalCodigo", "TJCE",
            "stage", "AUDIENCIA",
            "assignedUserId", 9001,
            "assignedUserName", "Servidor João",
            "panelDateBucket", "2026-04-22",
            "venue", Map.of(
                "modality", "VIRTUAL",
                "locationLabel", "Secretaria Juizado 1",
                "roomLabel", "Sala Virtual 3",
                "virtualLink", "https://audiencia.pjb.local/sala-3",
                "confirmationStatus", "LOCAL_CONFIRMADO",
                "confirmedAt", "2026-04-21T10:00:00Z"
            ),
            "participantNotification", Map.of(
                "status", "PARTICIPANTES_INTIMADOS",
                "readyCount", 2,
                "pendingCount", 0,
                "missingCount", 1
            ),
            "completionEvent", "AUDIENCIA_REALIZADA",
            "completionEventStatus", "PENDENTE_EVENTO_REAL",
            "operationalChecklist", List.of(
                Map.of("code", "LOCAL_LINK", "label", "Confirmação de sala, local ou link", "status", "LOCAL_CONFIRMADO", "blocking", true, "confirmedAt", "2026-04-21T10:00:00Z"),
                Map.of("code", "PARTICIPANTES_INTIMADOS", "label", "Confirmação de intimação dos participantes", "status", "PARTICIPANTES_INTIMADOS", "blocking", true),
                Map.of("code", "COMPARECIMENTO", "label", "Registro de presença ou ausência", "status", "AGUARDANDO_REALIZACAO", "blocking", false),
                Map.of("code", "EVENTO_REAL", "label", "Registro do evento real para retorno automático", "status", "PENDENTE_EVENTO_REAL", "blocking", true)
            ),
            "contactEnvelope", Map.of(
                "autor", Map.of("role", "AUTOR", "nome", "Maria", "email", "maria@exemplo.com", "contactReady", true),
                "reu", Map.of("role", "REU", "nome", "João", "contactReady", false),
                "advogados", List.of(
                    Map.of("role", "ADVOGADO", "side", "AUTOR", "nome", "Dra. Ana", "email", "ana@oab.com", "numeroOab", "OABCE123", "contactReady", true)
                )
            )
        ));
        when(repo.findCalendarWindowByInboxKeys(eq(List.of("inbox.secretaria")), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(one));

        SecretariatQueueAgendaSnapshotDto snapshot = service.agenda("inbox.secretaria", List.of("PENDENTE", "EM_EXECUCAO"), 2, 30, 180);

        assertThat(snapshot.items()).hasSize(1);
        assertThat(snapshot.items().get(0).contacts()).hasSize(3);
        assertThat(snapshot.byRito()).extracting(group -> group.groupLabel()).containsExactly("JUIZADO_ESPECIAL_CIVEL");
        assertThat(snapshot.byCell()).extracting(group -> group.groupLabel()).containsExactly("SEC_JEC_AUDIENCIA");
        assertThat(snapshot.byResponsible()).extracting(group -> group.groupLabel()).containsExactly("Servidor João");
        assertThat(snapshot.byCategory()).extracting(group -> group.groupLabel()).containsExactly("AUDIENCIA");
        assertThat(snapshot.byTrack()).extracting(group -> group.groupLabel()).containsExactly("AUDIENCIA_PROCESSUAL");
        assertThat(snapshot.byConfirmation()).extracting(group -> group.groupLabel()).containsExactly("PENDENTE_CONFIRMACAO");
        assertThat(snapshot.byVenueConfirmation()).extracting(group -> group.groupLabel()).containsExactly("LOCAL_CONFIRMADO");
        assertThat(snapshot.byParticipantNotification()).extracting(group -> group.groupLabel()).containsExactly("PARTICIPANTES_INTIMADOS");
        assertThat(snapshot.byAttendance()).extracting(group -> group.groupLabel()).containsExactly("AGUARDANDO_REALIZACAO");
        assertThat(snapshot.byReturnStatus()).extracting(group -> group.groupLabel()).containsExactly("AGUARDANDO_EVENTO");
        assertThat(snapshot.deadlineBuckets()).isNotEmpty();
        assertThat(snapshot.filters()).extracting(group -> group.filterCode()).contains("tribunal", "foro", "vara", "secretaria", "cellCode", "responsavel", "categoria", "trilha", "confirmacao", "local", "intimacao", "comparecimento", "retorno");
        assertThat(snapshot.items().get(0).responsibleName()).isEqualTo("Servidor João");
        assertThat(snapshot.items().get(0).secretariatLabel()).isEqualTo("Secretaria Juizado 1");
        assertThat(snapshot.items().get(0).cellCode()).isEqualTo("SEC_JEC_AUDIENCIA");
        assertThat(snapshot.items().get(0).eventTrack()).isEqualTo("AUDIENCIA_PROCESSUAL");
        assertThat(snapshot.items().get(0).confirmationStatus()).isEqualTo("PENDENTE_CONFIRMACAO");
        assertThat(snapshot.items().get(0).venueConfirmationStatus()).isEqualTo("LOCAL_CONFIRMADO");
        assertThat(snapshot.items().get(0).participantNotificationStatus()).isEqualTo("PARTICIPANTES_INTIMADOS");
        assertThat(snapshot.items().get(0).completionEventStatus()).isEqualTo("PENDENTE_EVENTO_REAL");
        assertThat(snapshot.items().get(0).attendanceStatus()).isEqualTo("AGUARDANDO_REALIZACAO");
        assertThat(snapshot.items().get(0).processReturnStatus()).isEqualTo("AGUARDANDO_EVENTO");
        assertThat(snapshot.items().get(0).autoReturnReady()).isFalse();
        assertThat(snapshot.items().get(0).venue().roomLabel()).isEqualTo("Sala Virtual 3");
        assertThat(snapshot.items().get(0).notification().status()).isEqualTo("PARTICIPANTES_INTIMADOS");
        assertThat(snapshot.items().get(0).completion().eventCode()).isEqualTo("AUDIENCIA_REALIZADA");
        assertThat(snapshot.items().get(0).checklist()).hasSize(4);
        assertThat(snapshot.items().get(0).targetPanelRoute()).contains("/api/v1/juiz/gabinete-decisoes");
        assertThat(snapshot.items().get(0).actionContracts()).extracting(contract -> contract.actionCode())
            .contains("CONFIRMAR_LOCAL_AGENDA", "CONFIRMAR_INTIMACAO_PARTICIPANTES", "REGISTRAR_COMPARECIMENTO", "REGISTRAR_EVENTO_REAL", "EXECUTAR_RETORNO_PROCESSO");
        assertThat(snapshot.items().get(0).actionContracts().stream()
            .filter(contract -> "EXECUTAR_RETORNO_PROCESSO".equals(contract.actionCode()))
            .findFirst()
            .orElseThrow()
            .enabled()).isFalse();
        assertThat(snapshot.metadata()).containsEntry("totalItems", 1);
        assertThat(snapshot.metadata()).containsKey("availableRitos");
        assertThat(((Map<?, ?>) snapshot.metadata().get("ritoCoverage")).get("JUIZADO_ESPECIAL_CIVEL")).isEqualTo(1L);
    }

    @Test
    void shouldApplyOperationalAgendaFilters() throws Exception {
        SecretariatQueueItem one = item(11L, 401L, "Intimação", 1, Instant.parse("2026-04-23T11:00:00Z"), mapOf(
            "processoNumero", "0401-11.2026.8.06.0001",
            "ritoProcessual", "CIVEL_COMUM",
            "routingVaraLabel", "Vara 1",
            "routingSecretariatLabel", "Secretaria Vara 1",
            "routingCellCode", "SEC_VARA1_INTIMACAO",
            "routingComarcaLabel", "Fortaleza",
            "tribunalCodigo", "TJCE",
            "stage", "INTIMACAO",
            "assignedUserName", "Servidor A",
            "panelDateBucket", "2026-04-23"
        ));
        SecretariatQueueItem two = item(12L, 402L, "Sessão colegiada", 1, Instant.parse("2026-04-24T15:00:00Z"), mapOf(
            "processoNumero", "0402-11.2026.4.05.0001",
            "ritoProcessual", "CIVEL_COMUM",
            "routingVaraLabel", "2ª Turma",
            "routingOrgaoLabel", "2ª Turma",
            "routingSecretariatLabel", "Secretaria TRF5",
            "routingCellCode", "SEC_TRF5_COLEGIADO",
            "routingComarcaLabel", "Recife",
            "tribunalCodigo", "TRF5",
            "stage", "COLEGIADO",
            "assignedUserName", "Servidor B",
            "panelDateBucket", "2026-04-24"
        ));
        when(repo.findCalendarWindowByInboxKeys(eq(List.of("inbox.secretaria")), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(one, two));

        SecretariatQueueAgendaSnapshotDto snapshot = service.agenda(
            "inbox.secretaria",
            List.of("PENDENTE", "EM_EXECUCAO"),
            2,
            30,
            180,
            new SecretariatQueueAgendaFilter("TRF5", null, null, "Turma", null, null, null, null, "SESSAO")
        );

        assertThat(snapshot.items()).hasSize(1);
        assertThat(snapshot.items().get(0).tribunalCodigo()).isEqualTo("TRF5");
        assertThat(snapshot.items().get(0).category()).isEqualTo("SESSAO_COLEGIADA");
    }

    @Test
    void shouldBuildAgendaOperationalReadinessForSessionAndCompletedReturn() throws Exception {
        SecretariatQueueItem one = item(13L, 501L, "Sessão colegiada confirmada", 1, Instant.parse("2026-04-25T13:00:00Z"), mapOf(
            "processoNumero", "0501-11.2026.4.05.0001",
            "ritoProcessual", "CIVEL_COMUM",
            "routingVaraLabel", "2ª Turma",
            "routingOrgaoLabel", "2ª Turma",
            "routingSecretariatLabel", "Secretaria TRF5",
            "routingCellCode", "SEC_TRF5_PAUTA",
            "routingComarcaLabel", "Recife",
            "tribunalCodigo", "TRF5",
            "stage", "COLEGIADO",
            "eventTrack", "SESSAO_COLEGIADA",
            "operationalConfirmationStatus", "CUMPRIDO",
            "venue", Map.of("confirmationStatus", "LOCAL_CONFIRMADO", "roomLabel", "Plenário 2", "locationLabel", "TRF5", "confirmedAt", "2026-04-24T10:00:00Z"),
            "participantNotification", Map.of("status", "PARTICIPANTES_INTIMADOS", "readyCount", 2, "pendingCount", 0, "missingCount", 0),
            "attendanceStatus", "PRESENTE",
            "completionEvent", "SESSAO_REALIZADA",
            "completionEventStatus", "EVENTO_REAL_REGISTRADO",
            "completionEventOccurredAt", "2026-04-25T16:30:00Z",
            "processReturnStatus", "PRONTO_PARA_RETORNO",
            "processReturnRoute", "/api/v1/secretariat/operacional/colegiado/julgamentos/77/baixa-origem",
            "autoReturnReady", true,
            "assignedUserName", "Servidor Sessão",
            "panelDateBucket", "2026-04-25"
        ));
        when(repo.findCalendarWindowByInboxKeys(eq(List.of("inbox.secretaria")), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(one));

        SecretariatQueueAgendaSnapshotDto snapshot = service.agenda("inbox.secretaria", List.of("PENDENTE", "EM_EXECUCAO"), 2, 30, 180);

        assertThat(snapshot.items()).hasSize(1);
        assertThat(snapshot.byTrack()).extracting(group -> group.groupLabel()).containsExactly("SESSAO_COLEGIADA");
        assertThat(snapshot.byConfirmation()).extracting(group -> group.groupLabel()).containsExactly("CUMPRIDO");
        assertThat(snapshot.byVenueConfirmation()).extracting(group -> group.groupLabel()).containsExactly("LOCAL_CONFIRMADO");
        assertThat(snapshot.byParticipantNotification()).extracting(group -> group.groupLabel()).containsExactly("PARTICIPANTES_INTIMADOS");
        assertThat(snapshot.byAttendance()).extracting(group -> group.groupLabel()).containsExactly("PRESENTE");
        assertThat(snapshot.byReturnStatus()).extracting(group -> group.groupLabel()).containsExactly("PRONTO_PARA_RETORNO");
        assertThat(snapshot.items().get(0).processReturnRoute()).contains("baixa-origem");
        assertThat(snapshot.items().get(0).venueConfirmationStatus()).isEqualTo("LOCAL_CONFIRMADO");
        assertThat(snapshot.items().get(0).participantNotificationStatus()).isEqualTo("PARTICIPANTES_INTIMADOS");
        assertThat(snapshot.items().get(0).completionEventStatus()).isEqualTo("EVENTO_REAL_REGISTRADO");
        assertThat(snapshot.items().get(0).completion().occurredAt()).isEqualTo(Instant.parse("2026-04-25T16:30:00Z"));
        assertThat(snapshot.items().get(0).autoReturnReady()).isTrue();
        assertThat(snapshot.items().get(0).actionContracts().stream()
            .filter(contract -> "EXECUTAR_RETORNO_PROCESSO".equals(contract.actionCode()))
            .findFirst()
            .orElseThrow()
            .enabled()).isTrue();
        assertThat(snapshot.metadata()).containsEntry("processReturnReadyCount", 1L);
        assertThat(snapshot.metadata()).containsEntry("completionEventReadyCount", 1L);
    }

    private SecretariatQueueItem item(Long workItemId,
                                      Long processoId,
                                      String titulo,
                                      Integer prioridade,
                                      Instant dueAt,
                                      Map<String, Object> metadata) throws Exception {
        String stage = String.valueOf(metadata.getOrDefault("stage", ""));
        boolean hearingSensitive = "AUDIENCIA".equalsIgnoreCase(stage);
        String queueCode = hearingSensitive ? "QUEUE_AUDIENCIA" : stage != null && !stage.isBlank() ? "QUEUE_" + stage.toUpperCase() : "QUEUE_BASE";
        return SecretariatQueueItem.builder()
            .workItemId(workItemId)
            .processoId(processoId)
            .inboxKey("inbox.secretaria")
            .queueCode(queueCode)
            .status("PENDENTE")
            .prioridade(prioridade)
            .dueAt(dueAt)
            .score(100)
            .tagsJson(mapper.writeValueAsString(List.of("URGENTE")))
            .metadataJson(mapper.writeValueAsString(metadata))
            .titulo(titulo)
            .escalationRequired(false)
            .secrecyReviewRequired(false)
            .hearingSensitive(hearingSensitive)
            .blocking(false)
            .createdAt(Instant.parse("2026-04-15T10:00:00Z"))
            .updatedAt(Instant.parse("2026-04-15T10:00:00Z"))
            .build();
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOf(Object... items) {
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < items.length; i += 2) {
            map.put(String.valueOf(items[i]), items[i + 1]);
        }
        return map;
    }

}
