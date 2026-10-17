package com.tcc.pjb.backend.service.processo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import com.tcc.pjb.backend.service.territorial.TerritorialProcessualService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessoPostAjuizamentoOrchestratorServiceTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final ProcessoDistribuicaoInicialReforcoService reforco = mock(ProcessoDistribuicaoInicialReforcoService.class);
    private final AuditLedgerService auditoria = mock(AuditLedgerService.class);
    private final ProcessoPostAjuizamentoOrchestratorService orquestrador = new ProcessoPostAjuizamentoOrchestratorService(
            processoRepository,
            mock(WorkItemRepository.class),
            mock(ProcessoLifecycleMachine.class),
            reforco,
            mock(TetoProcessualService.class),
            mock(TerritorialProcessualService.class),
            mock(ProcessoSlaJudicialService.class),
            auditoria);

    @Test
    void distribuicaoAplicadaQueExigeConferenciaDeJuizadoLevaOProcessoParaASecretaria() {
        Processo processo = processoComSnapshotCompleto();
        when(reforco.ensureSnapshot(processo)).thenReturn(distribuicaoAplicadaParaConferencia());

        orquestrador.onProcessoAjuizado(ProcessoAjuizadoEvent.builder().processoId(5L).build());

        verify(auditoria).appendSafely(eq("PROCESSO_POS_AJUIZAMENTO_SECRETARIA"), eq("PROCESSO_POS_AJUIZAMENTO"),
                eq("5"), anyString(), anyString());
    }

    @Test
    void semDistribuicaoParaConferirOProcessoSegueParaOGabinete() {
        processoComSnapshotCompleto();

        orquestrador.onProcessoAjuizado(ProcessoAjuizadoEvent.builder().processoId(5L).build());

        verify(auditoria).appendSafely(eq("PROCESSO_POS_AJUIZAMENTO_GABINETE"), eq("PROCESSO_POS_AJUIZAMENTO"),
                eq("5"), anyString(), anyString());
    }

    private Processo processoComSnapshotCompleto() {
        Processo processo = new Processo();
        processo.setId(5L);
        processo.setUnidadeJudiciariaCodigo("TJCE-CIVEL-CE-CAP");
        processo.setTribunalCodigoRoteado("TJCE");
        processo.setPreProtocoloStatus("APTA");
        processo.setParteReuNome("Empresa Re");
        when(processoRepository.findById(5L)).thenReturn(Optional.of(processo));
        when(processoRepository.save(any(Processo.class))).thenAnswer(inv -> inv.getArgument(0));
        return processo;
    }

    private static DynamicCompetenceDistributionResponse distribuicaoAplicadaParaConferencia() {
        return new DynamicCompetenceDistributionResponse(
                "req", Instant.now(), "0001234-55.2026.8.06.0001", "TJCE-CIVEL-CE-CAP", "TJCE", "Fortaleza", "CE", "CIVEL_GERAL",
                0d, false, "conferir", List.of(),
                List.of("Juizado especial nao instalado ao alcance do processo; unidade comum de mesma natureza exige conferencia da competencia"),
                List.of(), new DynamicCompetenceDistributionResponse.ScoreBreakdown(0d, 0d, 0d, 0d, 0d));
    }
}
