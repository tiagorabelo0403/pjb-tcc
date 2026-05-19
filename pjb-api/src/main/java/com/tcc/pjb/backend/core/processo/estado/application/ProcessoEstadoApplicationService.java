package com.tcc.pjb.backend.core.processo.estado.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.estado.domain.ProcessoEstadoMaquina;
import com.tcc.pjb.backend.core.processo.estado.domain.TransicaoEstadoResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.processo.ProcessoEstadoLog;
import com.tcc.pjb.backend.model.repository.ProcessoEstadoLogRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoEstadoApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoEstadoLogRepository logRepository;
    private final AuditLedgerService auditLedgerService;
    private final ProcessoEstadoMaquina maquina;

    @Inject
    public ProcessoEstadoApplicationService(ProcessoRepository processoRepository,
                                             ProcessoEstadoLogRepository logRepository,
                                             AuditLedgerService auditLedgerService) {
        this.processoRepository = processoRepository;
        this.logRepository = logRepository;
        this.auditLedgerService = auditLedgerService;
        this.maquina = new ProcessoEstadoMaquina();
    }

    @Transactional
    public void transitar(Long processoId, StatusProcesso novoEstado,
                          Long operadorId, String motivo) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new EntityNotFoundException("Processo não encontrado: " + processoId));
        StatusProcesso estadoAtual = processo.getStatusProcesso();
        TransicaoEstadoResult resultado = maquina.avaliar(estadoAtual, novoEstado);
        if (resultado instanceof TransicaoEstadoResult.Negada negada) {
            throw new IllegalStateException(negada.motivo());
        }
        processo.setStatusProcesso(novoEstado);
        processoRepository.save(processo);
        ProcessoEstadoLog log = new ProcessoEstadoLog(processoId, estadoAtual, novoEstado, operadorId, motivo);
        logRepository.save(log);
        auditLedgerService.appendSafely("PROCESSO_ESTADO_TRANSICAO",
                "Processo", String.valueOf(processoId), null);
    }

    @Transactional(readOnly = true)
    public Set<StatusProcesso> transicoesPossiveis(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new EntityNotFoundException("Processo não encontrado: " + processoId));
        return maquina.transicoesPossiveis(processo.getStatusProcesso());
    }

    @Transactional(readOnly = true)
    public List<ProcessoEstadoLog> historico(Long processoId) {
        return logRepository.findByProcessoIdOrderByCreatedAtAsc(processoId);
    }
}
