package com.tcc.pjb.backend.integration.judicial.financeiro;

import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudReconciliationCommand;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialRetrySummary;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudReconciliationCommand;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudReconciliationCommand;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoSnapshot;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialConsultaResumo;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialFailureSnapshot;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaView;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoView;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoView;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialHealthSnapshot;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialTimelineEntry;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialTimelineQuery;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialTimelineResult;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.financeiro.InfojudConsulta;
import com.tcc.pjb.backend.model.entity.financeiro.RenajudRestricao;
import com.tcc.pjb.backend.model.entity.financeiro.SisbajudOperacao;
import com.tcc.pjb.backend.model.repository.InfojudConsultaRepository;
import com.tcc.pjb.backend.model.repository.RenajudRestricaoRepository;
import com.tcc.pjb.backend.model.repository.SisbajudOperacaoRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaSnapshot;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoSnapshot;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudRetrySnapshot;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaAuditSnapshot;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialExecutionCommand;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialExecutionResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudConsultaCommand;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudConsultaResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoAuditSnapshot;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudConsultaCommand;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudConsultaResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoAuditSnapshot;

@Service
public class IntegracaoJudicialFinanceiraLifecycleService {

    private final SisbajudOperacaoRepository sisbajudOperacaoRepository;
    private final RenajudRestricaoRepository renajudRestricaoRepository;
    private final InfojudConsultaRepository infojudConsultaRepository;
    private final SisbajudHttpClient sisbajudHttpClient;
    private final RenajudHttpClient renajudHttpClient;
    private final InfojudHttpClient infojudHttpClient;
    private final IntegracaoJudicialFinanceiraProperties properties;
    private final ReadAfterWriteConsistencyPolicy rawPolicy;
    private final AuditLedgerService auditLedger;

    public IntegracaoJudicialFinanceiraLifecycleService(SisbajudOperacaoRepository sisbajudOperacaoRepository,
                                                        RenajudRestricaoRepository renajudRestricaoRepository,
                                                        InfojudConsultaRepository infojudConsultaRepository,
                                                        SisbajudHttpClient sisbajudHttpClient,
                                                        RenajudHttpClient renajudHttpClient,
                                                        InfojudHttpClient infojudHttpClient,
                                                        IntegracaoJudicialFinanceiraProperties properties,
                                                        ReadAfterWriteConsistencyPolicy rawPolicy,
                                                        AuditLedgerService auditLedger) {
        this.sisbajudOperacaoRepository = Objects.requireNonNull(sisbajudOperacaoRepository);
        this.renajudRestricaoRepository = Objects.requireNonNull(renajudRestricaoRepository);
        this.infojudConsultaRepository = Objects.requireNonNull(infojudConsultaRepository);
        this.sisbajudHttpClient = Objects.requireNonNull(sisbajudHttpClient);
        this.renajudHttpClient = Objects.requireNonNull(renajudHttpClient);
        this.infojudHttpClient = Objects.requireNonNull(infojudHttpClient);
        this.properties = Objects.requireNonNull(properties);
        this.rawPolicy = Objects.requireNonNull(rawPolicy);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.reprocessar-falhas", maxMillis = 3000, critical = true)
    public IntegracaoJudicialRetrySummary reprocessarFalhas() {
        if (!properties.enabled()) {
            return new IntegracaoJudicialRetrySummary(0, 0, 0);
        }
        int limite = Math.max(1, properties.reconciliationBatchSize());
        int sisb = retrySisbajud(limite);
        int rena = retryRenajud(limite);
        int info = retryInfojud(limite);
        return new IntegracaoJudicialRetrySummary(sisb, rena, info);
    }


    @Transactional
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.reprocessar-falhas", maxMillis = 3000, critical = true)
    public int reprocessarFalhas(SisbajudReconciliationCommand command) {
        Objects.requireNonNull(command);
        return retrySisbajud(Math.max(1, command.limit()));
    }

    @Transactional
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.reprocessar-falhas", maxMillis = 3000, critical = true)
    public int reprocessarFalhas(RenajudReconciliationCommand command) {
        Objects.requireNonNull(command);
        return retryRenajud(Math.max(1, command.limit()));
    }

    @Transactional
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.reprocessar-falhas", maxMillis = 3000, critical = true)
    public int reprocessarFalhas(InfojudReconciliationCommand command) {
        Objects.requireNonNull(command);
        return retryInfojud(Math.max(1, command.limit()));
    }

    public SisbajudOperacaoSnapshot snapshot(SisbajudOperacao operacao) {
        return new SisbajudOperacaoSnapshot(operacao.getId(), operacao.getProcessoId(), operacao.getStatus(), operacao.getValorSolicitado(), operacao.getProtocoloBacen());
    }

    private int retrySisbajud(int limite) {
        List<SisbajudOperacao> candidatas = sisbajudOperacaoRepository.findRetryCandidates(Instant.now());
        int processadas = 0;
        for (SisbajudOperacao operacao : candidatas) {
            if (processadas >= limite) {
                break;
            }
            if (operacao.getTentativas() >= Math.max(1, properties.retryMaxTentativas())) {
                operacao.setStatus("SUPERSEDED");
                sisbajudOperacaoRepository.save(operacao);
                continue;
            }
            try {
                var response = sisbajudHttpClient.solicitarBloqueio(operacao.getCpfDevedor(), operacao.getValorSolicitado(), operacao.getNumeroOficio());
                operacao.setProtocoloBacen(response.protocolo());
                operacao.setRetornoBacen(response.resumoRetorno());
                operacao.setStatus("CONFIRMED");
                operacao.setConfirmadoEm(Instant.now());
                operacao.setProximoRetryEm(null);
                sisbajudOperacaoRepository.save(operacao);
                rawPolicy.markWrite();
                processadas++;
            } catch (Exception e) {
                operacao.setTentativas(operacao.getTentativas() + 1);
                operacao.setRetornoBacen(e.getMessage());
                operacao.setStatus(operacao.getTentativas() >= Math.max(1, properties.retryMaxTentativas()) ? "SUPERSEDED" : "FAILED");
                operacao.setProximoRetryEm(nextRetry(operacao.getTentativas()));
                sisbajudOperacaoRepository.save(operacao);
                rawPolicy.markWrite();
                auditLedger.appendSafely("SISBAJUD_RETRY_FAIL", "PROCESSO", String.valueOf(operacao.getProcessoId()), "erro=" + e.getMessage());
            }
        }
        return processadas;
    }

    private int retryRenajud(int limite) {
        List<RenajudRestricao> candidatas = renajudRestricaoRepository.findRetryCandidates(Instant.now());
        int processadas = 0;
        for (RenajudRestricao restricao : candidatas) {
            if (processadas >= limite) {
                break;
            }
            if (restricao.getTentativas() >= Math.max(1, properties.retryMaxTentativas())) {
                restricao.setStatus("SUPERSEDED");
                renajudRestricaoRepository.save(restricao);
                continue;
            }
            try {
                var response = renajudHttpClient.solicitarRestricao(restricao.getPlaca(), restricao.getRenavam(), restricao.getTipo());
                restricao.setProtocoloDenatran(response.protocolo());
                restricao.setStatus("CONFIRMED");
                restricao.setConfirmadoEm(Instant.now());
                restricao.setProximoRetryEm(null);
                renajudRestricaoRepository.save(restricao);
                rawPolicy.markWrite();
                processadas++;
            } catch (Exception e) {
                restricao.setTentativas(restricao.getTentativas() + 1);
                restricao.setStatus(restricao.getTentativas() >= Math.max(1, properties.retryMaxTentativas()) ? "SUPERSEDED" : "FAILED");
                restricao.setProximoRetryEm(nextRetry(restricao.getTentativas()));
                renajudRestricaoRepository.save(restricao);
                rawPolicy.markWrite();
                auditLedger.appendSafely("RENAJUD_RETRY_FAIL", "PROCESSO", String.valueOf(restricao.getProcessoId()), "erro=" + e.getMessage());
            }
        }
        return processadas;
    }

    private int retryInfojud(int limite) {
        List<InfojudConsulta> candidatas = infojudConsultaRepository.findRetryCandidates(Instant.now());
        int processadas = 0;
        for (InfojudConsulta consulta : candidatas) {
            if (processadas >= limite) {
                break;
            }
            if (consulta.getTentativas() >= Math.max(1, properties.retryMaxTentativas())) {
                consulta.setStatus("SUPERSEDED");
                infojudConsultaRepository.save(consulta);
                continue;
            }
            try {
                var response = infojudHttpClient.consultar(consulta.getCpfCnpjConsultado());
                consulta.setProtocoloReceita(response.protocolo());
                consulta.setResumoRetorno(response.resumoRetorno());
                consulta.setStatus("CONFIRMED");
                consulta.setConfirmadoEm(Instant.now());
                consulta.setProximoRetryEm(null);
                infojudConsultaRepository.save(consulta);
                rawPolicy.markWrite();
                processadas++;
            } catch (Exception e) {
                consulta.setTentativas(consulta.getTentativas() + 1);
                consulta.setResumoRetorno(e.getMessage());
                consulta.setStatus(consulta.getTentativas() >= Math.max(1, properties.retryMaxTentativas()) ? "SUPERSEDED" : "FAILED");
                consulta.setProximoRetryEm(nextRetry(consulta.getTentativas()));
                infojudConsultaRepository.save(consulta);
                rawPolicy.markWrite();
                auditLedger.appendSafely("INFOJUD_RETRY_FAIL", "PROCESSO", String.valueOf(consulta.getProcessoId()), "erro=" + e.getMessage());
            }
        }
        return processadas;
    }



@Transactional(readOnly = true)
@PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.health.read", maxMillis = 1200, critical = false)
public IntegracaoJudicialHealthSnapshot health() {
    int sisb = sisbajudOperacaoRepository.findRetryCandidates(Instant.now()).size();
    int rena = renajudRestricaoRepository.findRetryCandidates(Instant.now()).size();
    int info = infojudConsultaRepository.findRetryCandidates(Instant.now()).size();
    return new IntegracaoJudicialHealthSnapshot(sisb, rena, info, properties.retryMaxTentativas());
}

@Transactional(readOnly = true)
@PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.timeline.read", maxMillis = 1500, critical = false)
public IntegracaoJudicialTimelineResult timeline(IntegracaoJudicialTimelineQuery query) {
    Objects.requireNonNull(query);
    java.util.ArrayList<IntegracaoJudicialTimelineEntry> entries = new java.util.ArrayList<>();
    switch (query.integracao()) {
        case "SISBAJUD" -> {
            var entity = sisbajudOperacaoRepository.findById(query.id()).orElseThrow(() -> new IllegalArgumentException("Operação SISBAJUD não encontrada: " + query.id()));
            entries.add(new IntegracaoJudicialTimelineEntry("SISBAJUD", entity.getStatus(), entity.getCreatedAt(), entity.getNumeroOficio()));
            if (entity.getConfirmadoEm() != null) entries.add(new IntegracaoJudicialTimelineEntry("SISBAJUD", "CONFIRMED", entity.getConfirmadoEm(), entity.getProtocoloBacen()));
        }
        case "RENAJUD" -> {
            var entity = renajudRestricaoRepository.findById(query.id()).orElseThrow(() -> new IllegalArgumentException("Restrição RENAJUD não encontrada: " + query.id()));
            entries.add(new IntegracaoJudicialTimelineEntry("RENAJUD", entity.getStatus(), entity.getCreatedAt(), entity.getPlaca()));
            if (entity.getConfirmadoEm() != null) entries.add(new IntegracaoJudicialTimelineEntry("RENAJUD", "CONFIRMED", entity.getConfirmadoEm(), entity.getProtocoloDenatran()));
        }
        case "INFOJUD" -> {
            var entity = infojudConsultaRepository.findById(query.id()).orElseThrow(() -> new IllegalArgumentException("Consulta INFOJUD não encontrada: " + query.id()));
            entries.add(new IntegracaoJudicialTimelineEntry("INFOJUD", entity.getStatus(), entity.getCreatedAt(), entity.getCpfCnpjConsultado()));
            if (entity.getConfirmadoEm() != null) entries.add(new IntegracaoJudicialTimelineEntry("INFOJUD", "CONFIRMED", entity.getConfirmadoEm(), entity.getProtocoloReceita()));
        }
        default -> { }
    }
    return new IntegracaoJudicialTimelineResult(query.integracao(), query.id(), java.util.List.copyOf(entries));
}

    private Instant nextRetry(int tentativa) {
        long base = Math.max(1L, properties.retryBackoffMinutes());
        return Instant.now().plus(base * Math.max(1, tentativa), ChronoUnit.MINUTES);
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.sisbajud-retry.read", maxMillis = 1200, critical = false)
    public SisbajudRetrySnapshot sisbajudRetrySnapshot(Long operacaoId) {
        var entity = sisbajudOperacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Operação SISBAJUD não encontrada: " + operacaoId));
        return new SisbajudRetrySnapshot(entity.getId(), entity.getTentativas(), entity.getProximoRetryEm(), entity.getStatus());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.infojud-snapshot.read", maxMillis = 1200, critical = false)
    public InfojudConsultaSnapshot infojudSnapshot(Long consultaId) {
        var entity = infojudConsultaRepository.findById(consultaId)
                .orElseThrow(() -> new IllegalArgumentException("Consulta INFOJUD não encontrada: " + consultaId));
        return new InfojudConsultaSnapshot(entity.getId(), entity.getCpfCnpjConsultado(), entity.getStatus(), entity.getConfirmadoEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.renajud-snapshot.read", maxMillis = 1200, critical = false)
    public RenajudRestricaoSnapshot renajudSnapshot(Long restricaoId) {
        var entity = renajudRestricaoRepository.findById(restricaoId)
                .orElseThrow(() -> new IllegalArgumentException("Restrição RENAJUD não encontrada: " + restricaoId));
        return new RenajudRestricaoSnapshot(entity.getId(), entity.getPlaca(), entity.getStatus(), entity.getConfirmadoEm());
    }


    @Transactional
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.executar.persist", maxMillis = 3000, critical = true)
    public IntegracaoJudicialExecutionResult executar(IntegracaoJudicialExecutionCommand command) {
        Objects.requireNonNull(command);
        return switch (command.integracao()) {
            case "SISBAJUD" -> new IntegracaoJudicialExecutionResult(command.integracao(), retrySisbajud(Math.max(1, command.limit())), "OK");
            case "RENAJUD" -> new IntegracaoJudicialExecutionResult(command.integracao(), retryRenajud(Math.max(1, command.limit())), "OK");
            case "INFOJUD" -> new IntegracaoJudicialExecutionResult(command.integracao(), retryInfojud(Math.max(1, command.limit())), "OK");
            default -> new IntegracaoJudicialExecutionResult(command.integracao(), 0, "IGNORED");
        };
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.sisbajud-consulta.read", maxMillis = 1200, critical = false)
    public SisbajudConsultaResult sisbajudConsulta(SisbajudConsultaCommand command) {
        Objects.requireNonNull(command);
        var entity = sisbajudOperacaoRepository.findById(command.operacaoId())
                .orElseThrow(() -> new IllegalArgumentException("Operação SISBAJUD não encontrada: " + command.operacaoId()));
        return new SisbajudConsultaResult(entity.getId(), entity.getStatus(), entity.getValorSolicitado(), entity.getProtocoloBacen(), entity.getConfirmadoEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.renajud-consulta.read", maxMillis = 1200, critical = false)
    public RenajudConsultaResult renajudConsulta(RenajudConsultaCommand command) {
        Objects.requireNonNull(command);
        var entity = renajudRestricaoRepository.findById(command.restricaoId())
                .orElseThrow(() -> new IllegalArgumentException("Restrição RENAJUD não encontrada: " + command.restricaoId()));
        return new RenajudConsultaResult(entity.getId(), entity.getPlaca(), entity.getStatus(), entity.getProtocoloDenatran(), entity.getConfirmadoEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.sisbajud-audit.read", maxMillis = 1200, critical = false)
    public SisbajudOperacaoAuditSnapshot sisbajudAuditSnapshot(Long operacaoId) {
        var entity = sisbajudOperacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Operação SISBAJUD não encontrada: " + operacaoId));
        return new SisbajudOperacaoAuditSnapshot(entity.getId(), entity.getProcessoId(), entity.getValorSolicitado(), entity.getStatus(), entity.getConfirmadoEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.renajud-audit.read", maxMillis = 1200, critical = false)
    public RenajudRestricaoAuditSnapshot renajudAuditSnapshot(Long restricaoId) {
        var entity = renajudRestricaoRepository.findById(restricaoId)
                .orElseThrow(() -> new IllegalArgumentException("Restrição RENAJUD não encontrada: " + restricaoId));
        return new RenajudRestricaoAuditSnapshot(entity.getId(), entity.getProcessoId(), entity.getPlaca(), entity.getStatus(), entity.getConfirmadoEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.infojud-audit.read", maxMillis = 1200, critical = false)
    public InfojudConsultaAuditSnapshot infojudAuditSnapshot(Long consultaId) {
        var entity = infojudConsultaRepository.findById(consultaId)
                .orElseThrow(() -> new IllegalArgumentException("Consulta INFOJUD não encontrada: " + consultaId));
        return new InfojudConsultaAuditSnapshot(entity.getId(), entity.getProcessoId(), entity.getCpfCnpjConsultado(), entity.getStatus(), entity.getConfirmadoEm());
    }


    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.sisbajud-summary.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialConsultaResumo resumoSisbajud(Long operacaoId) {
        var entity = sisbajudOperacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Operação SISBAJUD não encontrada: " + operacaoId));
        return new IntegracaoJudicialConsultaResumo("SISBAJUD", entity.getId(), entity.getStatus(), entity.getTentativas());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.renajud-summary.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialConsultaResumo resumoRenajud(Long restricaoId) {
        var entity = renajudRestricaoRepository.findById(restricaoId)
                .orElseThrow(() -> new IllegalArgumentException("Restrição RENAJUD não encontrada: " + restricaoId));
        return new IntegracaoJudicialConsultaResumo("RENAJUD", entity.getId(), entity.getStatus(), entity.getTentativas());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.infojud-summary.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialConsultaResumo resumoInfojud(Long consultaId) {
        var entity = infojudConsultaRepository.findById(consultaId)
                .orElseThrow(() -> new IllegalArgumentException("Consulta INFOJUD não encontrada: " + consultaId));
        return new IntegracaoJudicialConsultaResumo("INFOJUD", entity.getId(), entity.getStatus(), entity.getTentativas());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.sisbajud-failure.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialFailureSnapshot failureSisbajud(Long operacaoId) {
        var entity = sisbajudOperacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Operação SISBAJUD não encontrada: " + operacaoId));
        return new IntegracaoJudicialFailureSnapshot("SISBAJUD", entity.getId(), entity.getStatus(), entity.getTentativas(), entity.getProximoRetryEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.renajud-failure.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialFailureSnapshot failureRenajud(Long restricaoId) {
        var entity = renajudRestricaoRepository.findById(restricaoId)
                .orElseThrow(() -> new IllegalArgumentException("Restrição RENAJUD não encontrada: " + restricaoId));
        return new IntegracaoJudicialFailureSnapshot("RENAJUD", entity.getId(), entity.getStatus(), entity.getTentativas(), entity.getProximoRetryEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.infojud-failure.read", maxMillis = 1200, critical = false)
    public IntegracaoJudicialFailureSnapshot failureInfojud(Long consultaId) {
        var entity = infojudConsultaRepository.findById(consultaId)
                .orElseThrow(() -> new IllegalArgumentException("Consulta INFOJUD não encontrada: " + consultaId));
        return new IntegracaoJudicialFailureSnapshot("INFOJUD", entity.getId(), entity.getStatus(), entity.getTentativas(), entity.getProximoRetryEm());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.sisbajud-view.read", maxMillis = 1200, critical = false)
    public SisbajudOperacaoView sisbajudView(Long operacaoId) {
        var entity = sisbajudOperacaoRepository.findById(operacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Operação SISBAJUD não encontrada: " + operacaoId));
        return new SisbajudOperacaoView(entity.getId(), entity.getValorSolicitado(), entity.getStatus(), entity.getProtocoloBacen());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.renajud-view.read", maxMillis = 1200, critical = false)
    public RenajudRestricaoView renajudView(Long restricaoId) {
        var entity = renajudRestricaoRepository.findById(restricaoId)
                .orElseThrow(() -> new IllegalArgumentException("Restrição RENAJUD não encontrada: " + restricaoId));
        return new RenajudRestricaoView(entity.getId(), entity.getPlaca(), entity.getStatus(), entity.getProtocoloDenatran());
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "integracao.financeira.lifecycle.infojud-view.read", maxMillis = 1200, critical = false)
    public InfojudConsultaView infojudView(Long consultaId) {
        var entity = infojudConsultaRepository.findById(consultaId)
                .orElseThrow(() -> new IllegalArgumentException("Consulta INFOJUD não encontrada: " + consultaId));
        return new InfojudConsultaView(entity.getId(), entity.getCpfCnpjConsultado(), entity.getStatus(), entity.getProtocoloReceita());
    }

}