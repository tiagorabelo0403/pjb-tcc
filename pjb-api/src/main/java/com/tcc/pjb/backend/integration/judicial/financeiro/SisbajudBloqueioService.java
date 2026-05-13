package com.tcc.pjb.backend.integration.judicial.financeiro;

import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudBloqueioRequest;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudBloqueioResult;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.financeiro.SisbajudOperacao;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SisbajudOperacaoRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SisbajudBloqueioService {

    private final ProcessoRepository processoRepository;
    private final SisbajudOperacaoRepository operacaoRepository;
    private final SisbajudHttpClient httpClient;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final AuditLedgerService auditLedger;
    private final ReadAfterWriteConsistencyPolicy rawPolicy;

    public SisbajudBloqueioService(ProcessoRepository processoRepository,
                                   SisbajudOperacaoRepository operacaoRepository,
                                   SisbajudHttpClient httpClient,
                                   CurrentUserService currentUserService,
                                   PjbAuthorizationService authorizationService,
                                   AuditLedgerService auditLedger,
                                   ReadAfterWriteConsistencyPolicy rawPolicy) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.operacaoRepository = Objects.requireNonNull(operacaoRepository);
        this.httpClient = Objects.requireNonNull(httpClient);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.rawPolicy = Objects.requireNonNull(rawPolicy);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "sisbajud.bloqueio.persist", maxMillis = 2500, critical = true)
    @CircuitBreaker(name = "sisbajud-bloqueio")
    @Retry(name = "sisbajud-bloqueio")
    @Bulkhead(name = "sisbajud-bloqueio")
    public SisbajudBloqueioResult solicitarBloqueio(SisbajudBloqueioRequest request, String authzTrailId) {
        Objects.requireNonNull(authzTrailId, "authzTrailId obrigatório para SISBAJUD");
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + request.processoId()));
        var operador = currentUserService.getRequired();
        authorizationService.requireRequestSisbajud(operador, request.delegatedOperation());
        SisbajudOperacao entity = SisbajudOperacao.builder()
                .processoId(processo.getId())
                .tipo("BLOQUEIO")
                .cpfDevedor(request.cpfDevedor())
                .valorSolicitado(request.valorSolicitado())
                .numeroOficio(request.numeroOficio())
                .status("PENDING")
                .tentativas(0)
                .createdAt(Instant.now())
                .operadorId(operador.getId())
                .authzTrailId(authzTrailId)
                .build();
        operacaoRepository.save(entity);
        try {
            var response = httpClient.solicitarBloqueio(request.cpfDevedor(), request.valorSolicitado(), request.numeroOficio());
            entity.setProtocoloBacen(response.protocolo());
            entity.setRetornoBacen(response.resumoRetorno());
            entity.setStatus("CONFIRMED");
            entity.setConfirmadoEm(Instant.now());
            entity.setProximoRetryEm(null);
            operacaoRepository.save(entity);
            rawPolicy.markWrite();
            auditLedger.appendSafely("SISBAJUD_BLOQUEIO_OK", "PROCESSO", String.valueOf(processo.getId()),
                    "protocolo=" + response.protocolo() + " valor=" + request.valorSolicitado());
            return SisbajudBloqueioResult.success(entity.getId(), response.protocolo(), response.resumoRetorno());
        } catch (Exception e) {
            entity.setTentativas(entity.getTentativas() + 1);
            entity.setStatus("FAILED");
            entity.setRetornoBacen(e.getMessage());
            entity.setProximoRetryEm(Instant.now().plus(Math.max(1, entity.getTentativas()), ChronoUnit.MINUTES));
            operacaoRepository.save(entity);
            rawPolicy.markWrite();
            auditLedger.appendSafely("SISBAJUD_BLOQUEIO_FAIL", "PROCESSO", String.valueOf(processo.getId()), "erro=" + e.getMessage());
            return SisbajudBloqueioResult.failed(entity.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "sisbajud.consulta.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudConsultaResult consultar(com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudConsultaCommand command) {
        Objects.requireNonNull(command);
        SisbajudOperacao entity = operacaoRepository.findById(command.operacaoId())
                .orElseThrow(() -> new IllegalArgumentException("Operação SISBAJUD não encontrada: " + command.operacaoId()));
        return new com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudConsultaResult(entity.getId(), entity.getStatus(), entity.getValorSolicitado(), entity.getProtocoloBacen(), entity.getConfirmadoEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "sisbajud.snapshot.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoSnapshot snapshot(Long operacaoId) {
        SisbajudOperacao entity = operacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Operação SISBAJUD não encontrada: " + operacaoId));
        return new com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoSnapshot(entity.getId(), entity.getProcessoId(), entity.getStatus(), entity.getValorSolicitado(), entity.getProtocoloBacen());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "sisbajud.retry-snapshot.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudRetrySnapshot retrySnapshot(Long operacaoId) {
        SisbajudOperacao entity = operacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Operação SISBAJUD não encontrada: " + operacaoId));
        return new com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudRetrySnapshot(entity.getId(), entity.getTentativas(), entity.getProximoRetryEm(), entity.getStatus());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "sisbajud.audit-snapshot.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoAuditSnapshot auditSnapshot(Long operacaoId) {
        SisbajudOperacao entity = operacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Operação SISBAJUD não encontrada: " + operacaoId));
        return new com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoAuditSnapshot(entity.getId(), entity.getProcessoId(), entity.getValorSolicitado(), entity.getStatus(), entity.getConfirmadoEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "sisbajud.view.read", maxMillis = 1200, critical = false)
    public com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoView view(Long operacaoId) {
        SisbajudOperacao entity = operacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Operação SISBAJUD não encontrada: " + operacaoId));
        return new com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoView(entity.getId(), entity.getValorSolicitado(), entity.getStatus(), entity.getProtocoloBacen());
    }

}
