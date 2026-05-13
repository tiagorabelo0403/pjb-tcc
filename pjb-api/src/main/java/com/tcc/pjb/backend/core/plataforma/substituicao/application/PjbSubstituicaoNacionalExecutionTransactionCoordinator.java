package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoFase;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEventoEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoEventoRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoRepository;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.features", name = "substituicao-nacional", havingValue = "true", matchIfMissing = true)
public class PjbSubstituicaoNacionalExecutionTransactionCoordinator {

    private final PjbSubstituicaoNacionalExecucaoRepository repository;
    private final PjbSubstituicaoNacionalExecucaoEventoRepository eventoRepository;
    private final AuditLedgerService auditLedgerService;
    private final ObjectMapper objectMapper;

    public PjbSubstituicaoNacionalExecutionTransactionCoordinator(PjbSubstituicaoNacionalExecucaoRepository repository,
                                                                  PjbSubstituicaoNacionalExecucaoEventoRepository eventoRepository,
                                                                  AuditLedgerService auditLedgerService,
                                                                  ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository);
        this.eventoRepository = Objects.requireNonNull(eventoRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional(readOnly = true)
    public ExecutionSnapshot carregar(Long execucaoId) {
        PjbSubstituicaoNacionalExecucaoEntity execucao = repository.findById(execucaoId)
                .orElseThrow(() -> new IllegalArgumentException("Execução de substituição nacional não encontrada: " + execucaoId));
        return new ExecutionSnapshot(execucao.getId(), execucao.getTribunalCodigo(), execucao.getAcao(), execucao.getFaseAtual(), execucao.isDryRun(), execucao.getPayloadJson(), execucao.getRequestHash());
    }

    @Transactional
    public void iniciar(Long execucaoId, PjbSubstituicaoGateSnapshot gate) {
        PjbSubstituicaoNacionalExecucaoEntity execucao = locked(execucaoId);
        execucao.iniciar(PjbSubstituicaoExecucaoFase.PRECHECK, gate.gateScore(), gate.gateAprovado(), gate.rollbackReversivel());
        repository.save(execucao);
        registrar(execucao, "PRECHECK_CONCLUIDO", "INFO", PjbSubstituicaoExecucaoFase.PRECHECK, "Pré-check institucional consolidado.", gate.toMap());
    }

    @Transactional
    public void atualizarFase(Long execucaoId, PjbSubstituicaoExecucaoFase fase, String resultadoJson, String codigoEvento, String severidade, String descricao, Map<String, Object> detalhes) {
        PjbSubstituicaoNacionalExecucaoEntity execucao = locked(execucaoId);
        execucao.atualizarFase(fase, resultadoJson);
        repository.save(execucao);
        registrar(execucao, codigoEvento, severidade, fase, descricao, detalhes);
    }

    @Transactional
    public void concluir(Long execucaoId, PjbSubstituicaoExecucaoFase fase, PjbSubstituicaoGateSnapshot gate, String descricao, Map<String, Object> resultado) {
        PjbSubstituicaoNacionalExecucaoEntity execucao = locked(execucaoId);
        execucao.concluir(fase, toJson(resultado), gate.gateAprovado(), gate.rollbackReversivel(), gate.gateScore());
        repository.save(execucao);
        registrar(execucao, "EXECUCAO_CONCLUIDA", "INFO", fase, descricao, resultado);
        auditLedgerService.appendSafely("PJB_SUBSTITUICAO_NACIONAL_EXECUCAO_CONCLUIDA", "PJB_SUBSTITUICAO_EXECUCAO", String.valueOf(execucao.getId()), execucao.getRequestHash(), descricao);
    }

    @Transactional
    public void bloquear(Long execucaoId, PjbSubstituicaoExecucaoFase fase, PjbSubstituicaoGateSnapshot gate, String descricao) {
        PjbSubstituicaoNacionalExecucaoEntity execucao = locked(execucaoId);
        Map<String, Object> resultado = new java.util.LinkedHashMap<>(gate.resultadoBase());
        resultado.put("verdict", "BLOQUEADA");
        resultado.put("bloqueadores", gate.blockers());
        execucao.bloquear(fase, toJson(resultado), gate.gateAprovado(), gate.rollbackReversivel(), gate.gateScore());
        repository.save(execucao);
        registrar(execucao, "EXECUCAO_BLOQUEADA", "WARN", fase, descricao, resultado);
        auditLedgerService.appendSafely("PJB_SUBSTITUICAO_NACIONAL_EXECUCAO_BLOQUEADA", "PJB_SUBSTITUICAO_EXECUCAO", String.valueOf(execucao.getId()), execucao.getRequestHash(), descricao);
    }

    @Transactional
    public void falhar(Long execucaoId, String descricao, Throwable throwable) {
        PjbSubstituicaoNacionalExecucaoEntity execucao = locked(execucaoId);
        Map<String, Object> resultado = new java.util.LinkedHashMap<>();
        resultado.put("verdict", "FALHA");
        resultado.put("erro", descricao);
        if (throwable != null) {
            resultado.put("tipoErro", throwable.getClass().getName());
        }
        execucao.falhar(execucao.getFaseAtual(), toJson(resultado));
        repository.save(execucao);
        registrar(execucao, "EXECUCAO_FALHOU", "ERROR", execucao.getFaseAtual(), descricao, resultado);
        auditLedgerService.appendSafely("PJB_SUBSTITUICAO_NACIONAL_EXECUCAO_FALHOU", "PJB_SUBSTITUICAO_EXECUCAO", String.valueOf(execucao.getId()), execucao.getRequestHash(), descricao);
    }

    private void registrar(PjbSubstituicaoNacionalExecucaoEntity execucao,
                           String codigo,
                           String severidade,
                           PjbSubstituicaoExecucaoFase fase,
                           String descricao,
                           Map<String, Object> detalhes) {
        eventoRepository.save(new PjbSubstituicaoNacionalExecucaoEventoEntity(execucao, codigo, severidade, fase, descricao, toJson(detalhes)));
    }

    private PjbSubstituicaoNacionalExecucaoEntity locked(Long execucaoId) {
        return repository.findLockedById(execucaoId)
                .orElseThrow(() -> new IllegalArgumentException("Execução de substituição nacional não encontrada: " + execucaoId));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    public record ExecutionSnapshot(Long execucaoId,
                                    String tribunalCodigo,
                                    com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao acao,
                                    PjbSubstituicaoExecucaoFase faseAtual,
                                    boolean dryRun,
                                    String payloadJson,
                                    String requestHash) {
    }
}
