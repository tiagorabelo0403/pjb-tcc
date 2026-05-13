package com.tcc.pjb.backend.service.secretariat.operational;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAttendanceRequest;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueCompletionEventRequest;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueOperationalActionResponse;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueParticipantNotificationRequest;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueProcessReturnRequest;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueVenueConfirmationRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecretariatQueueOperationalActionServiceTest {

    private SecretariatQueueItemRepository repository;
    private SecretariatInstitutionalVisibilityService visibilityService;
    private WorkItemRepository workItemRepository;
    private ProcessoRepository processoRepository;
    private InstitutionalActorRoutingService institutionalActorRoutingService;
    private OutboxPublisher outboxPublisher;
    private OperationalNotificationProofService notificationProofService;
    private SecretariatQueueOperationalActionService service;
    private ObjectMapper mapper;
    private SecretariatQueueItem item;

    @BeforeEach
    void setUp() throws Exception {
        repository = mock(SecretariatQueueItemRepository.class);
        visibilityService = mock(SecretariatInstitutionalVisibilityService.class);
        workItemRepository = mock(WorkItemRepository.class);
        processoRepository = mock(ProcessoRepository.class);
        institutionalActorRoutingService = mock(InstitutionalActorRoutingService.class);
        outboxPublisher = mock(OutboxPublisher.class);
        notificationProofService = mock(OperationalNotificationProofService.class);
        mapper = new ObjectMapper();
        service = new SecretariatQueueOperationalActionService(
            repository,
            visibilityService,
            mapper,
            workItemRepository,
            processoRepository,
            institutionalActorRoutingService,
            outboxPublisher,
            notificationProofService
        );
        item = SecretariatQueueItem.builder()
            .workItemId(5001L)
            .processoId(7001L)
            .inboxKey("inbox.secretaria")
            .queueCode("SEC_AUDIENCIA")
            .status("PENDENTE")
            .prioridade(1)
            .score(90)
            .titulo("Audiência de instrução")
            .metadataJson(mapper.writeValueAsString(new LinkedHashMap<>(Map.of(
                "eventTrack", "AUDIENCIA_PROCESSUAL",
                "authorityClass", "JUIZ_ESTADUAL",
                "authorityTribunalAxis", "TJCE",
                "authorityUnitLabel", "Vara 1",
                "processReturnRoute", "/api/v1/juiz/gabinete-decisoes/processos/7001/retorno",
                "contactEnvelope", Map.of(
                    "autor", Map.of("nome", "Maria", "contactReady", true),
                    "reu", Map.of("nome", "João", "contactReady", true),
                    "advogados", List.of(Map.of("nome", "Dra. Ana", "contactReady", true))
                )
            ))))
            .createdAt(Instant.parse("2026-04-15T10:00:00Z"))
            .updatedAt(Instant.parse("2026-04-15T10:00:00Z"))
            .build();

        Processo processo = Processo.builder()
            .id(7001L)
            .numeroProcesso("7001-11.2026.8.06.0001")
            .faseAtual(FaseProcessual.CONHECIMENTO)
            .build();
        WorkItem rootWorkItem = WorkItem.builder()
            .id(5001L)
            .processo(processo)
            .templateCode("SEC_AUDIENCIA_5001")
            .titulo("Audiência de instrução")
            .descricao("item raiz")
            .queueCode("SEC_AUDIENCIA")
            .inboxKey("inbox.secretaria")
            .assignedRole(TipoUsuario.SERVIDOR_JUDICIARIO)
            .status(WorkItemStatus.EM_EXECUCAO)
            .prioridade(1)
            .build();
        InstitutionalActorRoutingService.InstitutionalRoute route = new InstitutionalActorRoutingService.InstitutionalRoute(
            "GABINETE:RETORNO_PROCESSO:PRIMEIRO_GRAU",
            "inbox.gabinete.vara1",
            TipoUsuario.JUIZ_ESTADUAL,
            "RETORNO_PROCESSO",
            "TOPO:VARA1",
            "Retorno ao gabinete da Vara 1",
            Map.of("institutionalLandingPath", "/api/v1/juiz/gabinete-decisoes?justica=ESTADUAL&tribunal=TJCE&unidade=VARA1")
        );

        when(repository.findLockedByWorkItemId(5001L)).thenReturn(Optional.of(item));
        when(visibilityService.requireInboxAccess("inbox.secretaria")).thenReturn("inbox.secretaria");
        when(repository.save(any(SecretariatQueueItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(processoRepository.findContextoArquiteturalById(7001L)).thenReturn(Optional.of(processo));
        when(workItemRepository.findLockedDetailedById(5001L)).thenReturn(Optional.of(rootWorkItem));
        when(workItemRepository.findLatestByProcessoIdAndTemplateCode(eq(7001L), any())).thenReturn(Optional.empty());
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem candidate = invocation.getArgument(0);
            if (candidate.getId() == null) {
                candidate.setId(9001L);
            }
            return candidate;
        });
        when(institutionalActorRoutingService.gabineteDecision(7001L, "RETORNO_PROCESSO")).thenReturn(route);
        when(notificationProofService.materializeProof(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new OperationalNotificationProofService.GeneratedNotificationProof(9901L, "conteudo", Map.of("noteId", 9901L, "contentHash", "hash-123", "title", "Carta")));
    }

    @Test
    void shouldPersistVenueNotificationAttendanceAndReturnFlow() {
        SecretariatQueueOperationalActionResponse venue = service.confirmVenue(5001L,
            new SecretariatQueueVenueConfirmationRequest("VIRTUAL", "Secretaria Vara 1", "Sala Virtual 5", "https://pjb.local/sala-5", null, Instant.parse("2026-04-20T09:00:00Z"), "link validado"),
            "servidor.a"
        );

        assertThat(venue.queueStatus()).isEqualTo("EM_EXECUCAO");
        assertThat(venue.venueConfirmationStatus()).isEqualTo("LOCAL_CONFIRMADO");
        assertThat(venue.metadata()).containsKey("operationalAuditTrail");
                assertThat(venue.panelRoute()).contains("/api/v1/secretariat/queue/panel");
        assertThat(venue.agendaRoute()).contains("/api/v1/secretariat/queue/agenda");

        SecretariatQueueOperationalActionResponse notification = service.confirmParticipantNotification(5001L,
            new SecretariatQueueParticipantNotificationRequest("PARTICIPANTES_INTIMADOS", 3, 0, 0, "WHATSAPP", Instant.parse("2026-04-20T10:00:00Z"), "todos comunicados", 1001L, "123456", "WHATSAPP", "captura do envio", List.of("msg:1")),
            "servidor.a"
        );

        assertThat(notification.participantNotificationStatus()).isEqualTo("PARTICIPANTES_INTIMADOS");
        assertThat(notification.metadata()).containsEntry("contactReadyCount", 3L);
        assertThat(notification.generatedDocument()).containsEntry("noteId", 9901L);

        SecretariatQueueOperationalActionResponse attendance = service.registerAttendance(5001L,
            new SecretariatQueueAttendanceRequest("PRESENTE", "AUTOR", "Maria", Instant.parse("2026-04-22T14:02:00Z"), "compareceu"),
            "servidor.a"
        );

        assertThat(attendance.attendanceStatus()).isEqualTo("PRESENTE");
        assertThat(attendance.confirmationStatus()).isEqualTo("CUMPRIDO");

        SecretariatQueueOperationalActionResponse completion = service.registerCompletionEvent(5001L,
            new SecretariatQueueCompletionEventRequest("AUDIENCIA_REALIZADA", null, Instant.parse("2026-04-22T15:10:00Z"), "ato concluído"),
            "servidor.a"
        );

        assertThat(completion.completionEventStatus()).isEqualTo("EVENTO_REAL_REGISTRADO");
        assertThat(completion.processReturnStatus()).isEqualTo("PRONTO_PARA_RETORNO");
        assertThat(completion.autoReturnReady()).isTrue();

        SecretariatQueueOperationalActionResponse processReturn = service.executeProcessReturn(5001L,
            new SecretariatQueueProcessReturnRequest(null, Instant.parse("2026-04-22T15:11:00Z"), "retorno executado"),
            "servidor.a"
        );

        assertThat(processReturn.queueStatus()).isEqualTo("CONCLUIDO");
        assertThat(processReturn.processReturnStatus()).isEqualTo("RETORNO_EXECUTADO");
        assertThat(processReturn.autoReturnReady()).isFalse();
        assertThat(processReturn.checkpoint()).containsEntry("actionCode", "PROCESS_RETURN");
        assertThat(processReturn.reentryWorkItemId()).isEqualTo(9001L);
        assertThat(processReturn.targetPanelRoute()).contains("/api/v1/juiz/gabinete-decisoes");
        assertThat(processReturn.metadata()).containsEntry("rootWorkItemStatus", "CONCLUIDO");
        assertThat(processReturn.metadata()).containsEntry("reentryInboxKey", "inbox.gabinete.vara1");
    }

    @Test
    void shouldDeriveParticipantNotificationStatusFromCounts() {
        SecretariatQueueOperationalActionResponse response = service.confirmParticipantNotification(5001L,
            new SecretariatQueueParticipantNotificationRequest(null, 1, 2, 0, null, Instant.parse("2026-04-20T10:00:00Z"), null, 1002L, "654321", "EMAIL", "registro parcial", List.of()),
            "servidor.b"
        );

        assertThat(response.participantNotificationStatus()).isEqualTo("PENDENTE_INTIMACAO");
        assertThat(response.metadata()).containsEntry("contactReadyCount", 1L);
    }
}
