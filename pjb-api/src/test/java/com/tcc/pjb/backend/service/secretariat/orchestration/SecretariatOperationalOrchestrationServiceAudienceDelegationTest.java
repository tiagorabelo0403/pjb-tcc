package com.tcc.pjb.backend.service.secretariat.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;
import com.tcc.pjb.backend.service.rito.RitoUrgenciaPriorityPolicy;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalActLineService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalAssignmentService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalBottleneckRadarService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalBulkReassignmentService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalChecklistEngine;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalExpeditionBatchService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalRedistributionService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalSlaService;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import com.tcc.pjb.backend.service.secretariat.rules.SecretariatRulePackFactory;
import com.tcc.pjb.backend.service.secretariat.stability.SecretariatOperationalStabilityService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Prova que os 6 métodos de audiência em {@link SecretariatOperationalOrchestrationService} são
 * delegates puros para {@link SecretariatAudienceOrchestrationService} -- extraído em F6 para
 * tirar 3 dependências exclusivas do bean raiz (20 -> 18 deps de construtor).
 */
class SecretariatOperationalOrchestrationServiceAudienceDelegationTest {

    private final SecretariatAudienceOrchestrationService audienceOrchestrationService = mock(SecretariatAudienceOrchestrationService.class);

    private final SecretariatOperationalOrchestrationService service = new SecretariatOperationalOrchestrationService(
            mock(CurrentUserService.class),
            mock(ProcessoRepository.class),
            mock(WorkItemRepository.class),
            mock(SecretariatQueueItemRepository.class),
            mock(SecretariatQueueProjectionService.class),
            mock(SecretariatRulePackFactory.class),
            mock(SecretariatOperationalRoutingResolver.class),
            mock(SecretariatOperationalChecklistEngine.class),
            mock(SecretariatOperationalAssignmentService.class),
            mock(SecretariatOperationalSlaService.class),
            mock(SecretariatOperationalActLineService.class),
            mock(SecretariatOperationalExpeditionBatchService.class),
            mock(SecretariatOperationalRedistributionService.class),
            mock(SecretariatOperationalBottleneckRadarService.class),
            mock(SecretariatOperationalStabilityService.class),
            mock(SecretariatOperationalBulkReassignmentService.class),
            mock(RitoUrgenciaPriorityPolicy.class),
            audienceOrchestrationService
    );

    private SecretariatOperationalOrchestrationService.HearingSnapshot hearingSnapshot() {
        return new SecretariatOperationalOrchestrationService.HearingSnapshot(1L, "ator", 2L, "0001", null, null, null, false, List.of());
    }

    private SecretariatOperationalOrchestrationService.HearingResourcesSnapshot hearingResourcesSnapshot() {
        return new SecretariatOperationalOrchestrationService.HearingResourcesSnapshot(2L, "0001", null, null);
    }

    private SecretariatOperationalOrchestrationService.AttendanceSnapshot attendanceSnapshot() {
        return new SecretariatOperationalOrchestrationService.AttendanceSnapshot(2L, "0001", null, null);
    }

    @Test
    void avaliarPautaDelegaComOsMesmosArgumentos() {
        LocalDateTime inicio = LocalDateTime.now();
        var esperado = hearingSnapshot();
        when(audienceOrchestrationService.avaliarPauta(eq(10L), eq(inicio), eq(30), eq("CIVIL"), eq("SALA_1"))).thenReturn(esperado);

        var resultado = service.avaliarPauta(10L, inicio, 30, "CIVIL", "SALA_1");

        assertThat(resultado).isSameAs(esperado);
    }

    @Test
    void registrarPautaDelegaComOsMesmosArgumentos() {
        LocalDateTime inicio = LocalDateTime.now();
        var esperado = hearingSnapshot();
        when(audienceOrchestrationService.registrarPauta(eq(11L), eq(inicio), eq(45), eq("PENAL"), eq("SALA_2"))).thenReturn(esperado);

        var resultado = service.registrarPauta(11L, inicio, 45, "PENAL", "SALA_2");

        assertThat(resultado).isSameAs(esperado);
        verify(audienceOrchestrationService).registrarPauta(11L, inicio, 45, "PENAL", "SALA_2");
    }

    @Test
    void avaliarRecursosAudienciaDelegaComOsMesmosArgumentos() {
        LocalDateTime inicio = LocalDateTime.now();
        var esperado = hearingResourcesSnapshot();
        when(audienceOrchestrationService.avaliarRecursosAudiencia(eq(12L), eq(inicio), eq(20), eq("CIVIL"), eq("SALA_3"))).thenReturn(esperado);

        var resultado = service.avaliarRecursosAudiencia(12L, inicio, 20, "CIVIL", "SALA_3");

        assertThat(resultado).isSameAs(esperado);
    }

    @Test
    void reservarRecursosAudienciaDelegaComOsMesmosArgumentos() {
        LocalDateTime inicio = LocalDateTime.now();
        var esperado = hearingResourcesSnapshot();
        when(audienceOrchestrationService.reservarRecursosAudiencia(eq(13L), eq(inicio), eq(15), eq("TRABALHISTA"), eq("SALA_4"))).thenReturn(esperado);

        var resultado = service.reservarRecursosAudiencia(13L, inicio, 15, "TRABALHISTA", "SALA_4");

        assertThat(resultado).isSameAs(esperado);
    }

    @Test
    void avaliarPresencaAudienciaDelegaComOsMesmosArgumentos() {
        LocalDateTime inicio = LocalDateTime.now();
        var esperado = attendanceSnapshot();
        when(audienceOrchestrationService.avaliarPresencaAudiencia(eq(14L), eq(inicio), eq(60), eq("CIVIL"), eq("SALA_5"))).thenReturn(esperado);

        var resultado = service.avaliarPresencaAudiencia(14L, inicio, 60, "CIVIL", "SALA_5");

        assertThat(resultado).isSameAs(esperado);
    }

    @Test
    void registrarPresencaAudienciaDelegaComOsMesmosArgumentos() {
        LocalDateTime inicio = LocalDateTime.now();
        var esperado = attendanceSnapshot();
        when(audienceOrchestrationService.registrarPresencaAudiencia(eq(15L), eq(inicio), eq(30), eq("CIVIL"), eq("SALA_6"), eq("TESTEMUNHA"), eq("Fulano"), eq("PRESENTE"))).thenReturn(esperado);

        var resultado = service.registrarPresencaAudiencia(15L, inicio, 30, "CIVIL", "SALA_6", "TESTEMUNHA", "Fulano", "PRESENTE");

        assertThat(resultado).isSameAs(esperado);
        verify(audienceOrchestrationService).registrarPresencaAudiencia(15L, inicio, 30, "CIVIL", "SALA_6", "TESTEMUNHA", "Fulano", "PRESENTE");
    }
}
