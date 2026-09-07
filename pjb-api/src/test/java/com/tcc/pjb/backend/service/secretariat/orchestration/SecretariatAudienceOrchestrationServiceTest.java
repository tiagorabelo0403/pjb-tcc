package com.tcc.pjb.backend.service.secretariat.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.processual.pauta.PautaAudienciaNacionalService;
import com.tcc.pjb.backend.service.rito.RitoUrgenciaPriorityPolicy;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalAttendanceService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalHearingResourceService;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SecretariatAudienceOrchestrationServiceTest {

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final SecretariatQueueProjectionService secretariatQueueProjectionService = mock(SecretariatQueueProjectionService.class);
    private final SecretariatOperationalRoutingResolver routingResolver = mock(SecretariatOperationalRoutingResolver.class);
    private final PautaAudienciaNacionalService pautaAudienciaNacionalService = mock(PautaAudienciaNacionalService.class);
    private final SecretariatOperationalHearingResourceService hearingResourceService = mock(SecretariatOperationalHearingResourceService.class);
    private final SecretariatOperationalAttendanceService attendanceService = mock(SecretariatOperationalAttendanceService.class);
    private final RitoUrgenciaPriorityPolicy ritoUrgenciaPriorityPolicy = mock(RitoUrgenciaPriorityPolicy.class);

    private final SecretariatAudienceOrchestrationService service = new SecretariatAudienceOrchestrationService(
            currentUserService,
            processoRepository,
            workItemRepository,
            secretariatQueueProjectionService,
            routingResolver,
            pautaAudienciaNacionalService,
            hearingResourceService,
            attendanceService,
            ritoUrgenciaPriorityPolicy
    );

    private Usuario servidor() {
        return Usuario.builder().id(9L).nome("Servidor").uf("CE").comarca("Fortaleza").tipoUsuario(TipoUsuario.SERVIDOR_FORUM).build();
    }

    private Processo processo(Long id) {
        Processo processo = new Processo();
        processo.setId(id);
        return processo;
    }

    private SecretariatOperationalRoutingProfile profile() {
        return new SecretariatOperationalRoutingProfile(
                "ROUTE-1", "ESTADUAL", "TRIB1", "PRIMEIRO_GRAU", "COMUM", "CIVIL", "DESK1",
                "SEC-CIVEL", "Q_RECEB", "INBOX_RECEB", "Q_SANE", "INBOX_SANE",
                "Q_AUD", "INBOX_AUD", "Q_EXEC", "INBOX_EXEC", "SALA",
                "ORG/PATH", Duration.ofHours(24), Duration.ofHours(48), Duration.ofHours(12),
                60, true, true, false, false,
                List.of("checklist-item"), List.of(), null, null, Map.of()
        );
    }

    private PautaAudienciaNacionalService.PautaAudienciaDecision decisao() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(3);
        return new PautaAudienciaNacionalService.PautaAudienciaDecision(
                true, inicio, inicio.plusMinutes(60), 60, true, null,
                List.of(), null, 30, false, List.of("fundamento"), 100L, "PAUTA-KEY-1"
        );
    }

    private SecretariatOperationalHearingResourceService.HearingResourceSnapshot recursoSnapshot() {
        var candidato = new SecretariatOperationalHearingResourceService.ResourceCandidate("SALA-1", 5L, "FISICA", List.of());
        return new SecretariatOperationalHearingResourceService.HearingResourceSnapshot(candidato, List.of(candidato), List.of(), Map.of());
    }

    @Test
    void registrarPautaCriaWorkItemEProjetaNaFila() {
        Usuario usuario = servidor();
        when(currentUserService.getRequired()).thenReturn(usuario);
        Processo processo = processo(50L);
        when(processoRepository.findById(50L)).thenReturn(Optional.of(processo));
        SecretariatOperationalRoutingProfile profile = profile();
        when(routingResolver.resolve(processo)).thenReturn(profile);
        PautaAudienciaNacionalService.PautaAudienciaDecision decision = decisao();
        when(pautaAudienciaNacionalService.registrar(any())).thenReturn(decision);
        when(workItemRepository.findLatestByProcessoIdAndTemplateCode(eq(50L), any())).thenReturn(Optional.empty());
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            item.setId(200L);
            return item;
        });
        var recursos = recursoSnapshot();
        when(hearingResourceService.reservar(processo, usuario, profile, decision, "SALA_FISICA")).thenReturn(recursos);

        var resultado = service.registrarPauta(50L, decision.inicio(), 60, "AUDIENCIA_UNA", "SALA_FISICA");

        assertThat(resultado.registrada()).isTrue();
        assertThat(resultado.recursos()).isSameAs(recursos);
        assertThat(resultado.pauta()).isSameAs(decision);
        verify(workItemRepository).save(argThat((WorkItem item) ->
                item.getStatus() == WorkItemStatus.PENDENTE
                        && item.getQueueCode().equals("Q_AUD")
                        && item.getInboxKey().equals("INBOX_AUD")
        ));
        verify(secretariatQueueProjectionService).upsert(any(WorkItem.class), anyInt(), any());
    }

    @Test
    void avaliarPautaNaoGravaWorkItem() {
        Usuario usuario = servidor();
        when(currentUserService.getRequired()).thenReturn(usuario);
        Processo processo = processo(51L);
        when(processoRepository.findById(51L)).thenReturn(Optional.of(processo));
        SecretariatOperationalRoutingProfile profile = profile();
        when(routingResolver.resolve(processo)).thenReturn(profile);
        PautaAudienciaNacionalService.PautaAudienciaDecision decision = decisao();
        when(pautaAudienciaNacionalService.avaliar(any())).thenReturn(decision);
        var recursos = recursoSnapshot();
        when(hearingResourceService.avaliar(processo, usuario, profile, decision, "SALA_FISICA")).thenReturn(recursos);

        var resultado = service.avaliarPauta(51L, decision.inicio(), 60, "AUDIENCIA_UNA", "SALA_FISICA");

        assertThat(resultado.registrada()).isFalse();
        verify(workItemRepository, never()).save(any());
    }

    @Test
    void avaliarPresencaAudienciaDelegaParaAttendanceService() {
        Usuario usuario = servidor();
        when(currentUserService.getRequired()).thenReturn(usuario);
        Processo processo = processo(52L);
        when(processoRepository.findById(52L)).thenReturn(Optional.of(processo));
        SecretariatOperationalRoutingProfile profile = profile();
        when(routingResolver.resolve(processo)).thenReturn(profile);
        LocalDateTime inicio = LocalDateTime.now();
        var attendance = new SecretariatOperationalAttendanceService.AttendanceSnapshot(null, List.of(), List.of(), Map.of());
        when(attendanceService.avaliar(processo, usuario, profile, inicio, 30, "CIVIL", "SALA_1")).thenReturn(attendance);

        var resultado = service.avaliarPresencaAudiencia(52L, inicio, 30, "CIVIL", "SALA_1");

        assertThat(resultado.audiencia()).isSameAs(attendance);
    }

    @Test
    void registrarPresencaAudienciaDelegaParaAttendanceService() {
        Usuario usuario = servidor();
        when(currentUserService.getRequired()).thenReturn(usuario);
        Processo processo = processo(53L);
        when(processoRepository.findById(53L)).thenReturn(Optional.of(processo));
        SecretariatOperationalRoutingProfile profile = profile();
        when(routingResolver.resolve(processo)).thenReturn(profile);
        LocalDateTime inicio = LocalDateTime.now();
        var attendance = new SecretariatOperationalAttendanceService.AttendanceSnapshot(null, List.of(), List.of(), Map.of());
        when(attendanceService.registrar(processo, usuario, profile, inicio, 30, "CIVIL", "SALA_1", "TESTEMUNHA", "Fulano", "PRESENTE")).thenReturn(attendance);

        var resultado = service.registrarPresencaAudiencia(53L, inicio, 30, "CIVIL", "SALA_1", "TESTEMUNHA", "Fulano", "PRESENTE");

        assertThat(resultado.audiencia()).isSameAs(attendance);
    }

}
