package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralForumAllocationReport;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class JudicialProtocolReplayServiceTest {

    @Test
    void replaysEligibleProcesses() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        NationalProceduralRoutingService routingService = Mockito.mock(NationalProceduralRoutingService.class);
        ProceduralSubmissionBlueprintService blueprintService = Mockito.mock(ProceduralSubmissionBlueprintService.class);
        ProceduralConnectorExecutionService executionService = Mockito.mock(ProceduralConnectorExecutionService.class);
        JudicialConnectorLifecycleService lifecycleService = Mockito.mock(JudicialConnectorLifecycleService.class);

        Processo processo = new Processo();
        processo.setId(91L);
        processo.setNumeroUnificado("0000001-00.2026.8.06.0001");
        processo.setConnectorSystem("PJE");
        processo.setConnectorSubmissionStatus("CONNECTOR_ERROR");

        when(processoRepository.findConnectorReplayCandidates(any(), eq(4), eq(PageRequest.of(0, 12))))
                .thenReturn(new PageImpl<>(List.of(processo)));

        ProceduralForumAllocationReport forum = new ProceduralForumAllocationReport(
                Instant.now(),
                "PROCEDIMENTO_COMUM_CIVEL",
                "Procedimento Comum Cível",
                "DOMICILIO_REU",
                "Fortaleza",
                "CE",
                "Art. 46 CPC",
                "NONE",
                "NONE",
                List.of(),
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "TJCE-CIVEL-CE-CAP",
                "1ª Vara Cível",
                "CIVEL_GERAL",
                true,
                true,
                0.97d,
                "PJE",
                true,
                false,
                false,
                true,
                true,
                "READY",
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );
        ProceduralRoutingReport routing = new ProceduralRoutingReport(
                Instant.now(),
                "OBRIGACAO_DE_FAZER",
                "CIVIL",
                "COMUM",
                "CONTENCIOSO",
                "ESTADUAL",
                "COMUM_ORDINARIO",
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "PJE",
                "Foro de Fortaleza",
                "Fortaleza",
                "CE",
                "1ª Vara Cível",
                "CIVEL_GERAL",
                "BAIXA",
                "DOCUMENTAL_SIMPLES",
                false,
                true,
                false,
                0.92d,
                "BAIXO",
                List.of(),
                null,
                forum,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );
        when(routingService.analyzeProcess(processo)).thenReturn(routing);

        ProceduralSubmissionBlueprintReport blueprint = new ProceduralSubmissionBlueprintReport(
                Instant.now(),
                "REQ-91",
                "READY_REAL_CONNECTOR",
                true,
                true,
                true,
                JudicialSystem.PJE,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "PROCEDIMENTO_COMUM_CIVEL",
                "Procedimento Comum Cível",
                "TJCE-CIVEL-CE-CAP",
                "1ª Vara Cível",
                "COMUM_ORDINARIO",
                "OBRIGACAO_DE_FAZER",
                "DOMICILIO_REU",
                "NONE",
                "NONE",
                "LOCAL_HASH",
                List.of(),
                true,
                false,
                false,
                "DRY_RUN_OK",
                "DRY-PJE",
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of()
        );
        when(blueprintService.analyzeProcess(processo, routing)).thenReturn(blueprint);

        ProceduralConnectorExecutionReport execution = new ProceduralConnectorExecutionReport(
                Instant.now(),
                "REAL_CONNECTOR",
                "DIRECT_PROTOCOL",
                "PJE:TJCE:TJCE-CIVEL-CE-CAP:PROCEDIMENTO_COMUM_CIVEL",
                JudicialSystem.PJE,
                "TJCE",
                "TJCE-CIVEL-CE-CAP",
                "PROCEDIMENTO_COMUM_CIVEL",
                "IDEMPOTENCY-91",
                "PASSWORD",
                "FAST_RETRY",
                true,
                false,
                false,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );
        when(executionService.analyzeProcess(processo, routing, blueprint)).thenReturn(execution);
        when(lifecycleService.submitAndSynchronize(processo, blueprint, execution, true))
                .thenReturn(Optional.of(new ProtocolSubmissionResult(true, JudicialSystem.PJE, "PJE-91", "SUBMITTED", "ok", Instant.now(), Map.of())));

        JudicialProtocolReplayService replayService = new JudicialProtocolReplayService(
                processoRepository,
                routingService,
                blueprintService,
                executionService,
                lifecycleService
        );

        int accepted = replayService.replayPendingProtocols(12, 4);

        assertThat(accepted).isEqualTo(1);
        verify(processoRepository).save(processo);
    }
}
