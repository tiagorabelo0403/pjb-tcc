package com.tcc.pjb.backend.service.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCheckpointTipo;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacao;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacaoStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCheckpointEventoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class CalendarNativeOperationalEventAssemblerServiceTest {

    @Test
    void assemblesMandadoAndPrazoFromOperationalWorkItems() {
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        PeritoNomeacaoRepository peritoNomeacaoRepository = mock(PeritoNomeacaoRepository.class);
        DiligenciaOperadorCheckpointEventoRepository checkpointRepository = mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = mock(DiligenciaOperadorEncerramentoRepository.class);
        SecretariatQueueItemRepository secretariatQueueItemRepository = mock(SecretariatQueueItemRepository.class);
        CalendarNativeOperationalEventAssemblerService service = new CalendarNativeOperationalEventAssemblerService(workItemRepository, peritoNomeacaoRepository, checkpointRepository, encerramentoRepository, secretariatQueueItemRepository);

        Usuario usuario = new Usuario();
        usuario.setId(77L);
        usuario.setTipoUsuario(TipoUsuario.OFICIAL_JUSTICA);
        usuario.setUf("CE");
        usuario.setComarca("Morada Nova");

        Processo processo = Processo.builder().id(100L).build();
        Instant due = LocalDateTime.of(2026, 4, 8, 10, 0).atZone(ZoneId.of("America/Fortaleza")).toInstant();
        WorkItem mandado = WorkItem.builder()
                .id(1L)
                .processo(processo)
                .type(WorkItemType.DILIGENCIA)
                .titulo("Cumprir mandado de citação")
                .descricao("Diligência externa com certidão de cumprimento.")
                .queueCode("MANDADO_CUMPRIMENTO")
                .inboxKey("CENTRAL_MANDADOS")
                .assignedRole(TipoUsuario.OFICIAL_JUSTICA)
                .status(WorkItemStatus.PENDENTE)
                .blocking(true)
                .prioridade(1)
                .dueAt(due)
                .build();
        WorkItem prazo = WorkItem.builder()
                .id(2L)
                .processo(processo)
                .type(WorkItemType.RECURSO)
                .titulo("Apresentar contrarrazões")
                .descricao("Prazo recursal institucional.")
                .queueCode("RECURSAL")
                .inboxKey("EQUIPE_RECURSAL")
                .assignedRole(TipoUsuario.OFICIAL_JUSTICA)
                .status(WorkItemStatus.PENDENTE)
                .blocking(false)
                .prioridade(2)
                .dueAt(due.plusSeconds(3600))
                .build();

        when(workItemRepository.findCalendarWindowByAssignedUser(eq(77L), any(), any(), any(Pageable.class))).thenReturn(List.of(mandado, prazo));
        when(workItemRepository.findCalendarWindowByRoleAndTerritory(eq(TipoUsuario.OFICIAL_JUSTICA), eq("CE"), eq("Morada Nova"), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(checkpointRepository.findByOperatorUserIdAndCanalAndWorkItemIdInOrderByOccurredAtDesc(eq(77L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(List.of());
        when(encerramentoRepository.findByOperatorUserIdAndCanalAndWorkItemIdInOrderByCreatedAtDesc(eq(77L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(List.of());

        List<CalendarEventDto> events = service.assembleForUser(usuario, LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 9), Map.of(100L, "0001000-00.2026.8.06.0001"));

        assertEquals(2, events.size());
        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("MANDADO_DILIGENCIA") && event.title().contains("Mandado e diligência")));
        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("PRAZO_RECURSAL_OPERACIONAL") && event.body().contains("categoria=PRAZO")));
    }

    @Test
    void classifiesOperationalSubtypesForGabineteSecretariaAndMandado() {
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        PeritoNomeacaoRepository peritoNomeacaoRepository = mock(PeritoNomeacaoRepository.class);
        DiligenciaOperadorCheckpointEventoRepository checkpointRepository = mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = mock(DiligenciaOperadorEncerramentoRepository.class);
        SecretariatQueueItemRepository secretariatQueueItemRepository = mock(SecretariatQueueItemRepository.class);
        CalendarNativeOperationalEventAssemblerService service = new CalendarNativeOperationalEventAssemblerService(workItemRepository, peritoNomeacaoRepository, checkpointRepository, encerramentoRepository, secretariatQueueItemRepository);

        Usuario usuario = new Usuario();
        usuario.setId(91L);
        usuario.setTipoUsuario(TipoUsuario.ASSESSOR_JUDICIAL);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");

        Processo processo = Processo.builder().id(300L).build();
        Instant base = LocalDateTime.of(2026, 4, 15, 14, 0).atZone(ZoneId.of("America/Fortaleza")).toInstant();
        WorkItem mandadoRetorno = WorkItem.builder()
                .id(11L)
                .processo(processo)
                .type(WorkItemType.DILIGENCIA)
                .titulo("Retorno de mandado para reexpedição")
                .descricao("Oficial devolveu o mandado sem cumprimento.")
                .queueCode("CENTRAL_MANDADOS_RETORNO")
                .inboxKey("CENTRAL_MANDADOS")
                .status(WorkItemStatus.PENDENTE)
                .blocking(true)
                .dueAt(base)
                .build();
        WorkItem gabineteVoto = WorkItem.builder()
                .id(12L)
                .processo(processo)
                .type(WorkItemType.MANIFESTACAO)
                .titulo("Minuta de voto para sessão colegiada")
                .descricao("Voto pendente para julgamento colegiado.")
                .queueCode("GABINETE_VOTO")
                .inboxKey("GABINETE")
                .status(WorkItemStatus.PENDENTE)
                .blocking(false)
                .dueAt(base.plusSeconds(3600))
                .build();
        WorkItem secretariaAudiencia = WorkItem.builder()
                .id(13L)
                .processo(processo)
                .type(WorkItemType.EXPEDICAO)
                .titulo("Preparar audiência e intimar partes")
                .descricao("Secretaria deve reservar sala e intimar testemunhas.")
                .queueCode("SECRETARIA_AUDIENCIA")
                .inboxKey("SECRETARIA")
                .status(WorkItemStatus.PENDENTE)
                .blocking(false)
                .dueAt(base.plusSeconds(7200))
                .build();

        when(workItemRepository.findCalendarWindowByAssignedUser(eq(91L), any(), any(), any(Pageable.class))).thenReturn(List.of(mandadoRetorno, gabineteVoto, secretariaAudiencia));
        when(workItemRepository.findCalendarWindowByRoleAndTerritory(eq(TipoUsuario.ASSESSOR_JUDICIAL), eq("CE"), eq("Fortaleza"), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(secretariatQueueItemRepository.findCalendarWindowByInboxKeys(anyCollection(), anyCollection(), any(), any(), any(Pageable.class))).thenReturn(List.of());

        List<CalendarEventDto> events = service.assembleForUser(usuario, LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 16), Map.of(300L, "0003000-00.2026.8.06.0001"));

        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("MANDADO_RETORNO") && event.color().equals("RED")));
        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("GABINETE_VOTO") && event.title().contains("Voto e sessão")));
        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("SECRETARIA_AUDIENCIA") && event.body().contains("operacao=SECRETARIA_AUDIENCIA")));
    }

    @Test
    void assemblesPericiaForPeritoProfile() {
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        PeritoNomeacaoRepository peritoNomeacaoRepository = mock(PeritoNomeacaoRepository.class);
        DiligenciaOperadorCheckpointEventoRepository checkpointRepository = mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = mock(DiligenciaOperadorEncerramentoRepository.class);
        SecretariatQueueItemRepository secretariatQueueItemRepository = mock(SecretariatQueueItemRepository.class);
        CalendarNativeOperationalEventAssemblerService service = new CalendarNativeOperationalEventAssemblerService(workItemRepository, peritoNomeacaoRepository, checkpointRepository, encerramentoRepository, secretariatQueueItemRepository);

        Usuario usuario = new Usuario();
        usuario.setId(88L);
        usuario.setTipoUsuario(TipoUsuario.PERITO_CONTABIL);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");

        Processo processo = Processo.builder().id(200L).build();
        PeritoNomeacao nomeacao = PeritoNomeacao.builder()
                .id(55L)
                .processo(processo)
                .perito(usuario)
                .status(PeritoNomeacaoStatus.NOMEADO)
                .nomeadoEm(LocalDateTime.of(2026, 4, 10, 9, 30))
                .observacao("Responder quesitos e apresentar proposta honorária.")
                .build();

        when(workItemRepository.findCalendarWindowByAssignedUser(eq(88L), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(workItemRepository.findCalendarWindowByRoleAndTerritory(eq(TipoUsuario.PERITO_CONTABIL), eq("CE"), eq("Fortaleza"), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(peritoNomeacaoRepository.findByPerito_IdAndStatusInAndNomeadoEmBetweenOrderByNomeadoEmAsc(eq(88L), anyCollection(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(nomeacao));

        List<CalendarEventDto> events = service.assembleForUser(usuario, LocalDate.of(2026, 4, 9), LocalDate.of(2026, 4, 11), Map.of(200L, "0002000-00.2026.8.06.0001"));

        assertEquals(1, events.size());
        CalendarEventDto event = events.getFirst();
        assertEquals("PERICIA_ACEITE", event.eventType());
        assertEquals("PERICIA", event.sourceCode());
        assertTrue(event.body().contains("nomeacaoId=55"));
    }

    @Test
    void usesResponseDateAndLaudoSubtypeForPericiaWhenAvailable() {
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        PeritoNomeacaoRepository peritoNomeacaoRepository = mock(PeritoNomeacaoRepository.class);
        DiligenciaOperadorCheckpointEventoRepository checkpointRepository = mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = mock(DiligenciaOperadorEncerramentoRepository.class);
        SecretariatQueueItemRepository secretariatQueueItemRepository = mock(SecretariatQueueItemRepository.class);
        CalendarNativeOperationalEventAssemblerService service = new CalendarNativeOperationalEventAssemblerService(workItemRepository, peritoNomeacaoRepository, checkpointRepository, encerramentoRepository, secretariatQueueItemRepository);

        Usuario usuario = new Usuario();
        usuario.setId(102L);
        usuario.setTipoUsuario(TipoUsuario.PERITO_ENGENHARIA);
        usuario.setUf("CE");
        usuario.setComarca("Sobral");

        Processo processo = Processo.builder().id(400L).build();
        LocalDateTime respondidoEm = LocalDateTime.of(2026, 4, 20, 16, 45);
        PeritoNomeacao nomeacao = PeritoNomeacao.builder()
                .id(77L)
                .processo(processo)
                .perito(usuario)
                .status(PeritoNomeacaoStatus.ACEITO)
                .nomeadoEm(LocalDateTime.of(2026, 4, 18, 10, 0))
                .respondidoEm(respondidoEm)
                .observacao("Laudo técnico liberado para entrega e resposta aos quesitos.")
                .build();

        when(workItemRepository.findCalendarWindowByAssignedUser(eq(102L), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(workItemRepository.findCalendarWindowByRoleAndTerritory(eq(TipoUsuario.PERITO_ENGENHARIA), eq("CE"), eq("Sobral"), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(peritoNomeacaoRepository.findByPerito_IdAndStatusInAndNomeadoEmBetweenOrderByNomeadoEmAsc(eq(102L), anyCollection(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(nomeacao));

        List<CalendarEventDto> events = service.assembleForUser(usuario, LocalDate.of(2026, 4, 18), LocalDate.of(2026, 4, 21), Map.of(400L, "0004000-00.2026.8.06.0001"));

        assertEquals(1, events.size());
        CalendarEventDto event = events.getFirst();
        assertEquals("PERICIA_LAUDO", event.eventType());
        assertEquals(respondidoEm, event.at());
        assertTrue(event.body().contains("respondidoEm=2026-04-20T16:45"));
    }

    @Test
    void assemblesOfficialTelemetryAndSecretariatQueueEvents() {
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        PeritoNomeacaoRepository peritoNomeacaoRepository = mock(PeritoNomeacaoRepository.class);
        DiligenciaOperadorCheckpointEventoRepository checkpointRepository = mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = mock(DiligenciaOperadorEncerramentoRepository.class);
        SecretariatQueueItemRepository secretariatQueueItemRepository = mock(SecretariatQueueItemRepository.class);
        CalendarNativeOperationalEventAssemblerService service = new CalendarNativeOperationalEventAssemblerService(workItemRepository, peritoNomeacaoRepository, checkpointRepository, encerramentoRepository, secretariatQueueItemRepository);

        Usuario oficial = new Usuario();
        oficial.setId(501L);
        oficial.setTipoUsuario(TipoUsuario.OFICIAL_JUSTICA);
        oficial.setUf("CE");
        oficial.setComarca("Fortaleza");

        Processo processo = Processo.builder().id(900L).build();
        Instant base = LocalDateTime.of(2026, 4, 23, 9, 0).atZone(ZoneId.of("America/Fortaleza")).toInstant();
        WorkItem mandado = WorkItem.builder()
                .id(70L)
                .processo(processo)
                .type(WorkItemType.DILIGENCIA)
                .titulo("Mandado externo")
                .inboxKey("CENTRAL_MANDADOS")
                .queueCode("MANDADO_CAMPO")
                .status(WorkItemStatus.PENDENTE)
                .dueAt(base)
                .build();
        DiligenciaOperadorCheckpointEvento checkpoint = DiligenciaOperadorCheckpointEvento.builder()
                .id(11L)
                .operatorUserId(501L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("DIL-70")
                .checkpointTipo(DiligenciaCheckpointTipo.TENTATIVA)
                .classification("TENTATIVA_CAMPO")
                .source("GPS")
                .tentativaSequencia(2)
                .distanceMeters(37.5)
                .insideGeofence(false)
                .workItemId(70L)
                .processoId(900L)
                .processoNumero("0009000-00.2026.8.06.0001")
                .occurredAt(base.plusSeconds(1800))
                .createdAt(base.plusSeconds(1800))
                .targetLatitude(0)
                .targetLongitude(0)
                .observedLatitude(0)
                .observedLongitude(0)
                .geofenceRadiusMeters(100)
                .build();
        DiligenciaOperadorEncerramento encerramento = DiligenciaOperadorEncerramento.builder()
                .id(19L)
                .operatorUserId(501L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("DIL-70")
                .outcome(DiligenciaEncerramentoTipo.CUMPRIMENTO_FRUSTRADO)
                .workItemId(70L)
                .processoId(900L)
                .processoNumero("0009000-00.2026.8.06.0001")
                .createdAt(base.plusSeconds(5400))
                .idempotencyKey("abc")
                .executionDigestSha256("def")
                .build();

        when(workItemRepository.findCalendarWindowByAssignedUser(eq(501L), any(), any(), any(Pageable.class))).thenReturn(List.of(mandado));
        when(workItemRepository.findCalendarWindowByRoleAndTerritory(eq(TipoUsuario.OFICIAL_JUSTICA), eq("CE"), eq("Fortaleza"), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(checkpointRepository.findByOperatorUserIdAndCanalAndWorkItemIdInOrderByOccurredAtDesc(eq(501L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(List.of(checkpoint));
        when(encerramentoRepository.findByOperatorUserIdAndCanalAndWorkItemIdInOrderByCreatedAtDesc(eq(501L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(List.of(encerramento));

        List<CalendarEventDto> events = service.assembleForUser(oficial, LocalDate.of(2026, 4, 23), LocalDate.of(2026, 4, 24), Map.of(900L, "0009000-00.2026.8.06.0001"));

        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("MANDADO_TENTATIVA") && event.sourceCode().equals("OFICIAL_TELEMETRIA")));
        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("MANDADO_RETORNO") && event.sourceCode().equals("OFICIAL_TELEMETRIA")));
        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("MANDADO_JANELA_RETORNO") && event.body().contains("followupWorkItemId=")));
    }

    @Test
    void derivesOfficialAttemptWindowWhenMultipleAttemptsExist() {
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        PeritoNomeacaoRepository peritoNomeacaoRepository = mock(PeritoNomeacaoRepository.class);
        DiligenciaOperadorCheckpointEventoRepository checkpointRepository = mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = mock(DiligenciaOperadorEncerramentoRepository.class);
        SecretariatQueueItemRepository secretariatQueueItemRepository = mock(SecretariatQueueItemRepository.class);
        CalendarNativeOperationalEventAssemblerService service = new CalendarNativeOperationalEventAssemblerService(workItemRepository, peritoNomeacaoRepository, checkpointRepository, encerramentoRepository, secretariatQueueItemRepository);

        Usuario oficial = new Usuario();
        oficial.setId(333L);
        oficial.setTipoUsuario(TipoUsuario.OFICIAL_JUSTICA);
        oficial.setUf("CE");
        oficial.setComarca("Fortaleza");

        Processo processo = Processo.builder().id(990L).build();
        Instant base = LocalDateTime.of(2026, 4, 26, 8, 0).atZone(ZoneId.of("America/Fortaleza")).toInstant();
        WorkItem mandado = WorkItem.builder()
                .id(101L)
                .processo(processo)
                .type(WorkItemType.DILIGENCIA)
                .titulo("Mandado com múltiplas tentativas")
                .inboxKey("CENTRAL_MANDADOS")
                .queueCode("MANDADO_CUMPRIMENTO")
                .status(WorkItemStatus.PENDENTE)
                .dueAt(base)
                .build();
        DiligenciaOperadorCheckpointEvento tentativa1 = DiligenciaOperadorCheckpointEvento.builder()
                .id(41L)
                .operatorUserId(333L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("DIL-101")
                .checkpointTipo(DiligenciaCheckpointTipo.TENTATIVA)
                .classification("TENTATIVA_1")
                .source("GPS")
                .tentativaSequencia(1)
                .distanceMeters(90)
                .insideGeofence(false)
                .workItemId(101L)
                .processoId(990L)
                .processoNumero("0009900-00.2026.8.06.0001")
                .occurredAt(base.plusSeconds(1800))
                .createdAt(base.plusSeconds(1800))
                .targetLatitude(0)
                .targetLongitude(0)
                .observedLatitude(0)
                .observedLongitude(0)
                .geofenceRadiusMeters(100)
                .build();
        DiligenciaOperadorCheckpointEvento tentativa2 = DiligenciaOperadorCheckpointEvento.builder()
                .id(42L)
                .operatorUserId(333L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .diligenceReference("DIL-101")
                .checkpointTipo(DiligenciaCheckpointTipo.TENTATIVA)
                .classification("TENTATIVA_2")
                .source("GPS")
                .tentativaSequencia(2)
                .distanceMeters(45)
                .insideGeofence(false)
                .workItemId(101L)
                .processoId(990L)
                .processoNumero("0009900-00.2026.8.06.0001")
                .occurredAt(base.plusSeconds(5400))
                .createdAt(base.plusSeconds(5400))
                .targetLatitude(0)
                .targetLongitude(0)
                .observedLatitude(0)
                .observedLongitude(0)
                .geofenceRadiusMeters(100)
                .build();

        when(workItemRepository.findCalendarWindowByAssignedUser(eq(333L), any(), any(), any(Pageable.class))).thenReturn(List.of(mandado));
        when(workItemRepository.findCalendarWindowByRoleAndTerritory(eq(TipoUsuario.OFICIAL_JUSTICA), eq("CE"), eq("Fortaleza"), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(checkpointRepository.findByOperatorUserIdAndCanalAndWorkItemIdInOrderByOccurredAtDesc(eq(333L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(List.of(tentativa2, tentativa1));
        when(encerramentoRepository.findByOperatorUserIdAndCanalAndWorkItemIdInOrderByCreatedAtDesc(eq(333L), eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), any())).thenReturn(List.of());

        List<CalendarEventDto> events = service.assembleForUser(oficial, LocalDate.of(2026, 4, 26), LocalDate.of(2026, 4, 27), Map.of(990L, "0009900-00.2026.8.06.0001"));

        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("MANDADO_MULTI_TENTATIVA") && event.body().contains("maiorSequencia=2")));
    }


    @Test
    void assemblesInstitutionalQueueEventsForSecretariatAndGabinete() {
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        PeritoNomeacaoRepository peritoNomeacaoRepository = mock(PeritoNomeacaoRepository.class);
        DiligenciaOperadorCheckpointEventoRepository checkpointRepository = mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = mock(DiligenciaOperadorEncerramentoRepository.class);
        SecretariatQueueItemRepository secretariatQueueItemRepository = mock(SecretariatQueueItemRepository.class);
        CalendarNativeOperationalEventAssemblerService service = new CalendarNativeOperationalEventAssemblerService(workItemRepository, peritoNomeacaoRepository, checkpointRepository, encerramentoRepository, secretariatQueueItemRepository);

        Usuario assessor = new Usuario();
        assessor.setId(777L);
        assessor.setTipoUsuario(TipoUsuario.ASSESSOR_JUDICIAL);
        assessor.setUf("CE");
        assessor.setComarca("Fortaleza");

        Processo processo = Processo.builder().id(901L).build();
        Instant base = LocalDateTime.of(2026, 4, 24, 8, 0).atZone(ZoneId.of("America/Fortaleza")).toInstant();
        WorkItem gatilho = WorkItem.builder()
                .id(88L)
                .processo(processo)
                .type(WorkItemType.MANIFESTACAO)
                .titulo("Inbox institucional")
                .inboxKey("SEC:CE:FORTALEZA:GAB")
                .queueCode("GABINETE")
                .status(WorkItemStatus.PENDENTE)
                .dueAt(base)
                .build();
        SecretariatQueueItem filaAudiencia = SecretariatQueueItem.builder()
                .workItemId(5001L)
                .processoId(901L)
                .inboxKey("SEC:CE:FORTALEZA:AUD")
                .queueCode("AUDIENCIA_PREPARATORIA")
                .status("PENDENTE")
                .prioridade(1)
                .score(90)
                .titulo("Preparar audiência una")
                .hearingSensitive(true)
                .dueAt(base.plusSeconds(3600))
                .updatedAt(base.plusSeconds(3600))
                .createdAt(base)
                .build();
        SecretariatQueueItem filaGabinete = SecretariatQueueItem.builder()
                .workItemId(5002L)
                .processoId(901L)
                .inboxKey("SEC:CE:FORTALEZA:GAB")
                .queueCode("GABINETE_VOTO")
                .status("EM_EXECUCAO")
                .prioridade(2)
                .score(84)
                .titulo("Voto pendente do gabinete")
                .blocking(true)
                .dueAt(base.plusSeconds(7200))
                .updatedAt(base.plusSeconds(7200))
                .createdAt(base)
                .build();

        when(workItemRepository.findCalendarWindowByAssignedUser(eq(777L), any(), any(), any(Pageable.class))).thenReturn(List.of(gatilho));
        when(workItemRepository.findCalendarWindowByRoleAndTerritory(eq(TipoUsuario.ASSESSOR_JUDICIAL), eq("CE"), eq("Fortaleza"), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(secretariatQueueItemRepository.findCalendarWindowByInboxKeys(anyCollection(), anyCollection(), any(), any(), any(Pageable.class))).thenReturn(List.of(filaAudiencia, filaGabinete));

        List<CalendarEventDto> events = service.assembleForUser(assessor, LocalDate.of(2026, 4, 24), LocalDate.of(2026, 4, 25), Map.of(901L, "0009010-00.2026.8.06.0001"));

        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("SECRETARIA_FILA_AUDIENCIA") && event.sourceCode().equals("SECRETARIA_QUEUE")));
        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("GABINETE_VOTO") && event.sourceCode().equals("SECRETARIA_QUEUE")));
    }

    @Test
    void classifiesPautaAndPendingLaudoAcrossInstitutionalQueuesAndPericia() {
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        PeritoNomeacaoRepository peritoNomeacaoRepository = mock(PeritoNomeacaoRepository.class);
        DiligenciaOperadorCheckpointEventoRepository checkpointRepository = mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenciaOperadorEncerramentoRepository encerramentoRepository = mock(DiligenciaOperadorEncerramentoRepository.class);
        SecretariatQueueItemRepository secretariatQueueItemRepository = mock(SecretariatQueueItemRepository.class);
        CalendarNativeOperationalEventAssemblerService service = new CalendarNativeOperationalEventAssemblerService(workItemRepository, peritoNomeacaoRepository, checkpointRepository, encerramentoRepository, secretariatQueueItemRepository);

        Usuario assessor = new Usuario();
        assessor.setId(818L);
        assessor.setTipoUsuario(TipoUsuario.ASSESSOR_JUDICIAL);
        assessor.setUf("CE");
        assessor.setComarca("Fortaleza");

        Processo processo = Processo.builder().id(999L).build();
        Instant base = LocalDateTime.of(2026, 4, 27, 10, 0).atZone(ZoneId.of("America/Fortaleza")).toInstant();
        WorkItem laudoPendente = WorkItem.builder()
                .id(120L)
                .processo(processo)
                .type(WorkItemType.LAUDO)
                .titulo("Laudo pendente de entrega")
                .descricao("Aguardando laudo técnico complementar")
                .queueCode("PERICIA_LAUDO")
                .inboxKey("PERICIA")
                .status(WorkItemStatus.PENDENTE)
                .dueAt(base)
                .build();
        WorkItem gatilho = WorkItem.builder()
                .id(121L)
                .processo(processo)
                .type(WorkItemType.MANIFESTACAO)
                .titulo("Pauta colegiada no gabinete")
                .inboxKey("SEC:CE:FORTALEZA:GAB")
                .queueCode("GABINETE")
                .status(WorkItemStatus.PENDENTE)
                .dueAt(base.plusSeconds(1800))
                .build();
        SecretariatQueueItem pauta = SecretariatQueueItem.builder()
                .workItemId(8001L)
                .processoId(999L)
                .inboxKey("SEC:CE:FORTALEZA:GAB")
                .queueCode("PAUTA_COLEGIADA")
                .titulo("Pauta de sessão da câmara")
                .status("EM_EXECUCAO")
                .prioridade(1)
                .score(99)
                .blocking(false)
                .dueAt(base.plusSeconds(3600))
                .updatedAt(base.plusSeconds(3600))
                .createdAt(base)
                .build();
        SecretariatQueueItem pautaInterna = SecretariatQueueItem.builder()
                .workItemId(8002L)
                .processoId(999L)
                .inboxKey("SEC:CE:FORTALEZA:AUD")
                .queueCode("RESERVA_SALA_AUDIENCIA")
                .titulo("Reserva de sala e pauta interna")
                .status("PENDENTE")
                .prioridade(2)
                .score(70)
                .hearingSensitive(true)
                .blocking(false)
                .dueAt(base.plusSeconds(7200))
                .updatedAt(base.plusSeconds(7200))
                .createdAt(base)
                .build();

        when(workItemRepository.findCalendarWindowByAssignedUser(eq(818L), any(), any(), any(Pageable.class))).thenReturn(List.of(laudoPendente, gatilho));
        when(workItemRepository.findCalendarWindowByRoleAndTerritory(eq(TipoUsuario.ASSESSOR_JUDICIAL), eq("CE"), eq("Fortaleza"), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(secretariatQueueItemRepository.findCalendarWindowByInboxKeys(anyCollection(), anyCollection(), any(), any(), any(Pageable.class))).thenReturn(List.of(pauta, pautaInterna));

        List<CalendarEventDto> events = service.assembleForUser(assessor, LocalDate.of(2026, 4, 27), LocalDate.of(2026, 4, 28), Map.of(999L, "0009990-00.2026.8.06.0001"));

        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("PERICIA_LAUDO_PENDENTE")));
        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("GABINETE_PAUTA") && event.sourceCode().equals("SECRETARIA_QUEUE")));
        assertTrue(events.stream().anyMatch(event -> event.eventType().equals("SECRETARIA_PAUTA_INTERNA") && event.sourceCode().equals("SECRETARIA_QUEUE")));
    }

}
