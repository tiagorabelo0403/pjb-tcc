package com.tcc.pjb.backend.core.processo.producao.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoProducaoPesadaAggregate;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoProducaoPesadaGate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimePreparationApplicationService;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimePreparationAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoMalhaObservabilidadeApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaObservabilidadeAggregate;
import com.tcc.pjb.backend.core.quality.certificacao.application.PjbCertificacaoOperacionalApplicationService;
import com.tcc.pjb.backend.core.quality.certificacao.domain.PjbCertificacaoOperacionalAggregate;
import com.tcc.pjb.backend.core.util.Hashes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoProducaoPesadaApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService;
    private final ProcessoMalhaObservabilidadeApplicationService processoMalhaObservabilidadeApplicationService;
    private final PjbCertificacaoOperacionalApplicationService pjbCertificacaoOperacionalApplicationService;
    private final ProcessoMalhaSupportBridge processoMalhaSupportBridge;

    public ProcessoProducaoPesadaApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                    ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService,
                                                    ProcessoMalhaObservabilidadeApplicationService processoMalhaObservabilidadeApplicationService,
                                                    PjbCertificacaoOperacionalApplicationService pjbCertificacaoOperacionalApplicationService,
                                                    ProcessoMalhaSupportBridge processoMalhaSupportBridge) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoRuntimePreparationApplicationService = Objects.requireNonNull(processoRuntimePreparationApplicationService);
        this.processoMalhaObservabilidadeApplicationService = Objects.requireNonNull(processoMalhaObservabilidadeApplicationService);
        this.pjbCertificacaoOperacionalApplicationService = Objects.requireNonNull(pjbCertificacaoOperacionalApplicationService);
        this.processoMalhaSupportBridge = Objects.requireNonNull(processoMalhaSupportBridge);
    }

    @Transactional(readOnly = true)
    public ProcessoProducaoPesadaAggregate avaliar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        ProcessoRuntimePreparationAggregate runtime = processoRuntimePreparationApplicationService.avaliar(contexto);
        ProcessoMalhaObservabilidadeAggregate observabilidade = processoMalhaObservabilidadeApplicationService.detalhar(processoId);
        PjbCertificacaoOperacionalAggregate certificacao = pjbCertificacaoOperacionalApplicationService.certificar(processoId);
        List<ProcessoProducaoPesadaGate> gates = new ArrayList<>();
        gates.add(gate("build.integral", "Build integral validado", runtime.integrationStatus().prontoMinimo() && certificacao.percentualCobertura() >= 85, runtime.integrationStatus().prontoMinimo() ? 78 : 38, true, "Prontidão do runtime=" + runtime.integrationStatus().percentualProntidao(), "Executar build integral no pipeline nacional"));
        gates.add(gate("teste.e2e", "Testes ponta a ponta", certificacao.percentualCobertura() >= 90 && observabilidade.alertas().isEmpty(), Math.min(100, certificacao.percentualCobertura()), true, "Cobertura operacional=" + certificacao.percentualCobertura(), "Materializar suíte ponta a ponta por ramo e tribunal"));
        gates.add(gate("carga", "Carga e capacidade", observabilidade.workItemsPendentesNacionais() < 5_000 && observabilidade.filasCriticas().size() <= 1, observabilidade.filasCriticas().isEmpty() ? 82 : 56, true, "Filas críticas=" + observabilidade.filasCriticas().size(), "Executar benchmark e soak test nacional"));
        gates.add(gate("resiliencia", "Resiliência e failover", runtime.integrationStatus().outboxDisponivel() && runtime.integrationStatus().observabilidadeNacionalDisponivel(), runtime.integrationStatus().outboxDisponivel() ? 72 : 40, true, "Outbox=" + runtime.integrationStatus().outboxDisponivel() + ";observabilidade=" + runtime.integrationStatus().observabilidadeNacionalDisponivel(), "Amarrar failover, replay e tolerância a backlog"));
        gates.add(gate("rollback", "Rollback e reversibilidade", runtime.integrationStatus().outboxDisponivel() && runtime.integrationStatus().auditLedgerDisponivel(), runtime.integrationStatus().auditLedgerDisponivel() ? 70 : 32, false, "Audit=" + runtime.integrationStatus().auditLedgerDisponivel(), "Operacionalizar rollback transacional por tribunal"));
        gates.add(gate("observabilidade", "Observabilidade contínua", !"DESACOPLADA".equalsIgnoreCase(observabilidade.saudeInstitucional()), "ASSISTIDA".equalsIgnoreCase(observabilidade.saudeInstitucional()) ? 68 : 88, false, "Saúde institucional=" + observabilidade.saudeInstitucional(), "Fechar métricas, alertas e tracing em produção"));
        gates.add(gate("operacao.continua", "Operação contínua", runtime.integrationStatus().segurancaDisponivel() && runtime.integrationStatus().usuarioRepositoryDisponivel(), runtime.integrationStatus().segurancaDisponivel() ? 74 : 44, true, "Segurança=" + runtime.integrationStatus().segurancaDisponivel(), "Ligar runbooks, plantão e resposta operacional"));
        int scoreGeral = gates.isEmpty() ? 0 : (int) Math.round(gates.stream().mapToInt(ProcessoProducaoPesadaGate::score).average().orElse(0));
        List<String> bloqueios = gates.stream().filter(item -> item.bloqueante() && item.status() != PjbFechamentoStatus.CONCLUIDA).map(ProcessoProducaoPesadaGate::codigo).toList();
        PjbFechamentoStatus statusGeral = scoreGeral >= 85 && bloqueios.isEmpty() ? PjbFechamentoStatus.CONCLUIDA : scoreGeral >= 60 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.BLOQUEADA;
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("runtime.prontidao=" + runtime.integrationStatus().percentualProntidao());
        fundamentos.add("certificacao.cobertura=" + certificacao.percentualCobertura());
        fundamentos.add("observabilidade.saude=" + observabilidade.saudeInstitucional());
        fundamentos.addAll(runtime.alertas());
        fundamentos.addAll(observabilidade.alertas());
        fundamentos.addAll(certificacao.modulosCriticos());
        DecisionTraceService trace = processoMalhaSupportBridge.decisionTraceService();
        if (trace != null) {
            trace.record("plataforma.producao.pesada", "Processo", String.valueOf(processoId), BigDecimal.valueOf(scoreGeral), gates.toString(), bloqueios.toString(), Hashes.sha256Hex(contexto.numeroReferencia()), Hashes.sha256Hex(gates.toString()), "PJB-PROD", statusGeral.name());
        }
        AuditLedgerService audit = processoMalhaSupportBridge.auditLedgerService();
        if (audit != null) {
            audit.appendSafely("PJB_PRODUCAO_PESADA_AVALIADA", "Processo", String.valueOf(processoId), Hashes.sha256Hex(gates.toString()), "status=" + statusGeral.name() + ";score=" + scoreGeral);
        }
        return new ProcessoProducaoPesadaAggregate(
                processoId,
                contexto.numeroReferencia(),
                List.copyOf(gates),
                scoreGeral,
                statusGeral,
                statusGeral == PjbFechamentoStatus.CONCLUIDA,
                bloqueios,
                List.copyOf(fundamentos.stream().limit(120).toList()),
                Instant.now()
        );
    }

    private ProcessoProducaoPesadaGate gate(String codigo,
                                            String titulo,
                                            boolean concluida,
                                            int scoreBase,
                                            boolean bloqueante,
                                            String diagnostico,
                                            String proximaAcao) {
        PjbFechamentoStatus status = concluida ? PjbFechamentoStatus.CONCLUIDA : scoreBase >= 60 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.PENDENTE;
        return new ProcessoProducaoPesadaGate(codigo, titulo, status, concluida ? Math.max(scoreBase, 85) : scoreBase, bloqueante, diagnostico, proximaAcao, List.of(diagnostico, proximaAcao));
    }
}
