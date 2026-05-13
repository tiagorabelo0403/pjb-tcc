package com.tcc.pjb.backend.core.processo.distribuicao.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.competencia.application.ProcessoCompetenciaMalhaApplicationService;
import com.tcc.pjb.backend.core.processo.competencia.domain.ProcessoCompetenciaMalhaAggregate;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaAggregate;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaMotivo;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoMalhaNacionalApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalRisco;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoDistribuicaoMalhaApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoMalhaNacionalApplicationService processoMalhaNacionalApplicationService;
    private final ProcessoCompetenciaMalhaApplicationService processoCompetenciaMalhaApplicationService;
    private final OutboxPublisher outboxPublisher;
    private final DecisionTraceService decisionTraceService;
    private final AuditLedgerService auditLedgerService;

    public ProcessoDistribuicaoMalhaApplicationService(ProcessoRepository processoRepository,
                                                       ProcessoMalhaNacionalApplicationService processoMalhaNacionalApplicationService,
                                                       ProcessoCompetenciaMalhaApplicationService processoCompetenciaMalhaApplicationService,
                                                       OutboxPublisher outboxPublisher,
                                                       ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                                       ObjectProvider<AuditLedgerService> auditLedgerServiceProvider) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoMalhaNacionalApplicationService = Objects.requireNonNull(processoMalhaNacionalApplicationService);
        this.processoCompetenciaMalhaApplicationService = Objects.requireNonNull(processoCompetenciaMalhaApplicationService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.auditLedgerService = auditLedgerServiceProvider.getIfAvailable();
    }

    @Transactional(readOnly = true)
    public ProcessoDistribuicaoMalhaAggregate detalhar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoMalhaNacionalAggregate malha = processoMalhaNacionalApplicationService.detalhar(processoId);
        ProcessoCompetenciaMalhaAggregate competencia = processoCompetenciaMalhaApplicationService.analisar(processoId);
        ArrayList<ProcessoDistribuicaoMalhaMotivo> motivos = new ArrayList<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        String filaSugerida = filaBase(processo, competencia, malha);
        String inboxSugerida = inboxBase(processo, competencia, malha);
        String acaoPrimaria = competencia.acaoPrimaria();
        boolean travaDistribuicao = malha.travaDistribuicaoOuFluxo() || competencia.exigeRemessa() || competencia.travaAtosIncompativeis();
        boolean exigeRemessa = competencia.exigeRemessa();
        boolean exigeReuniao = competencia.exigeRedistribuicao() || malha.hotspots().contains("CONEXAO") || malha.hotspots().contains("DEPENDENCIA");
        boolean exigeSigiloReforcado = malha.nivelSigiloRecomendado().nivel() > malha.nivelSigiloAtual().nivel();

        for (ProcessoMalhaNacionalRisco risco : malha.riscos()) {
            ProcessoDistribuicaoMalhaMotivo motivo = new ProcessoDistribuicaoMalhaMotivo(
                    risco.codigo(),
                    risco.dominio(),
                    risco.severidade(),
                    risco.bloqueante(),
                    risco.titulo(),
                    risco.detalhe(),
                    risco.fundamentos()
            );
            motivos.add(motivo);
            fundamentos.addAll(motivo.fundamentos());
        }
        competencia.itens().forEach(item -> {
            ProcessoDistribuicaoMalhaMotivo motivo = new ProcessoDistribuicaoMalhaMotivo(
                    item.codigo(),
                    item.eixo(),
                    item.bloqueante() ? "CRITICO" : item.score() >= 0.8d ? "ALTO" : "ATENCAO",
                    item.bloqueante(),
                    item.acao(),
                    firstNonBlank(item.destinoUnidade(), item.destinoTribunal()),
                    item.fundamentos()
            );
            motivos.add(motivo);
            fundamentos.addAll(motivo.fundamentos());
        });

        if (exigeRemessa) {
            acaoPrimaria = "TRAVAR_E_REMETER_PREVENTO";
            filaSugerida = filaSugerida + ":PREVENTO";
            inboxSugerida = inboxSugerida + ":REMESSA";
        } else if (competencia.exigeRedistribuicao()) {
            acaoPrimaria = "REDISTRIBUIR_POR_CONEXAO";
            filaSugerida = filaSugerida + ":CONEXAO";
        } else if (exigeSigiloReforcado) {
            acaoPrimaria = Objects.equals(acaoPrimaria, "MANTER_COMPETENCIA") ? "RECLASSIFICAR_SIGILO_E_DISTRIBUIR" : acaoPrimaria;
            filaSugerida = filaSugerida + ":SIGILO";
        } else if (Objects.equals(acaoPrimaria, "MANTER_COMPETENCIA") && malha.totalBloqueios() == 0) {
            acaoPrimaria = "DISTRIBUIR_NORMALMENTE";
        }

        ProcessoDistribuicaoMalhaAggregate aggregate = new ProcessoDistribuicaoMalhaAggregate(
                processoId,
                processo.getNumero(),
                acaoPrimaria,
                filaSugerida,
                inboxSugerida,
                firstNonBlank(competencia.tribunalSugerido(), normalize(processo.getTribunal())),
                firstNonBlank(competencia.unidadeSugerida(), normalize(processo.getVara())),
                prioridade(malha, competencia, exigeSigiloReforcado),
                travaDistribuicao,
                exigeRemessa,
                exigeReuniao,
                exigeSigiloReforcado,
                List.copyOf(motivos),
                List.copyOf(fundamentos.stream().limit(60).toList()),
                Instant.now()
        );
        return aggregate;
    }

    @Transactional
    public ProcessoDistribuicaoMalhaAggregate propagar(Long processoId) {
        ProcessoDistribuicaoMalhaAggregate aggregate = detalhar(processoId);
        outboxPublisher.enqueue(
                "processo.malha.distribuicao." + aggregate.processoId(),
                "PROCESSO_DISTRIBUICAO_MALHA_DECIDIDA",
                Map.ofEntries(
                        Map.entry("processoId", aggregate.processoId()),
                        Map.entry("numeroProcesso", aggregate.numeroProcesso()),
                        Map.entry("acaoPrimaria", aggregate.acaoPrimaria()),
                        Map.entry("filaSugerida", aggregate.filaSugerida()),
                        Map.entry("inboxSugerida", aggregate.inboxSugerida()),
                        Map.entry("destinoTribunal", aggregate.destinoTribunal()),
                        Map.entry("destinoUnidade", aggregate.destinoUnidade()),
                        Map.entry("prioridade", aggregate.prioridade()),
                        Map.entry("travaDistribuicao", aggregate.travaDistribuicao()),
                        Map.entry("exigeRemessa", aggregate.exigeRemessa()),
                        Map.entry("exigeReuniao", aggregate.exigeReuniao()),
                        Map.entry("exigeSigiloReforcado", aggregate.exigeSigiloReforcado())
                ),
                Map.of(
                        "boundedContext", "processo.distribuicao",
                        "eventVersion", "1",
                        "action", aggregate.acaoPrimaria()
                ),
                "processo-distribuicao-malha:" + aggregate.processoId() + ':' + aggregate.acaoPrimaria() + ':' + aggregate.filaSugerida(),
                "PROCESSO",
                String.valueOf(aggregate.processoId())
        );
        if (decisionTraceService != null) {
            decisionTraceService.record(
                    "PROCESSO_DISTRIBUICAO_MALHA",
                    "PROCESSO",
                    String.valueOf(aggregate.processoId()),
                    BigDecimal.valueOf(confianca(aggregate)),
                    jsonList(aggregate.fundamentos()),
                    jsonList(aggregate.motivos().stream().map(motivo -> motivo.codigo() + ':' + motivo.severidade()).toList()),
                    aggregate.numeroProcesso(),
                    aggregate.acaoPrimaria() + ':' + aggregate.destinoUnidade(),
                    "PJB_DISTRIBUICAO_MALHA_V1",
                    jsonList(List.of(aggregate.filaSugerida(), aggregate.inboxSugerida(), String.valueOf(aggregate.prioridade())))
            );
        }
        if (auditLedgerService != null) {
            auditLedgerService.appendSafely(
                    "PROCESSO_DISTRIBUICAO_MALHA",
                    "PROCESSO",
                    String.valueOf(aggregate.processoId()),
                    aggregate.acaoPrimaria() + ':' + aggregate.filaSugerida() + ':' + aggregate.inboxSugerida(),
                    "Distribuição consolidada pela malha nacional"
            );
        }
        return aggregate;
    }

    private int prioridade(ProcessoMalhaNacionalAggregate malha,
                           ProcessoCompetenciaMalhaAggregate competencia,
                           boolean exigeSigiloReforcado) {
        int prioridade = 30;
        prioridade += (int) Math.min(40, malha.totalBloqueios() * 12L);
        prioridade += competencia.exigeRemessa() ? 20 : 0;
        prioridade += competencia.exigeRedistribuicao() ? 12 : 0;
        prioridade += exigeSigiloReforcado ? 8 : 0;
        return Math.max(1, Math.min(99, prioridade));
    }

    private String filaBase(Processo processo,
                            ProcessoCompetenciaMalhaAggregate competencia,
                            ProcessoMalhaNacionalAggregate malha) {
        String ramo = normalize(safeName(processo.getRamoDireito()));
        String tribunal = firstNonBlank(competencia.tribunalSugerido(), normalize(processo.getTribunal()), "NACIONAL");
        String eixo = competencia.eixoCompetencia().isBlank() ? "COMPETENCIA_GERAL" : competencia.eixoCompetencia();
        String risco = malha.totalBloqueios() > 0 ? "CRITICO" : malha.riscos().isEmpty() ? "ESTAVEL" : "ATENCAO";
        return String.join(":", "MALHA_DISTRIBUICAO", tribunal, ramo.isBlank() ? "GERAL" : ramo, eixo, risco);
    }

    private String inboxBase(Processo processo,
                             ProcessoCompetenciaMalhaAggregate competencia,
                             ProcessoMalhaNacionalAggregate malha) {
        String tribunal = firstNonBlank(competencia.tribunalSugerido(), normalize(processo.getTribunal()), "NACIONAL");
        if (malha.totalBloqueios() > 0) {
            return String.join(":", "MALHA", tribunal, "BLOQUEIO_DISTRIBUICAO");
        }
        if (competencia.exigeRedistribuicao()) {
            return String.join(":", "MALHA", tribunal, "REDISTRIBUICAO");
        }
        return String.join(":", "MALHA", tribunal, "DISTRIBUICAO");
    }

    private double confianca(ProcessoDistribuicaoMalhaAggregate aggregate) {
        long criticos = aggregate.motivos().stream().filter(ProcessoDistribuicaoMalhaMotivo::bloqueante).count();
        if (criticos > 0) {
            return 0.95d;
        }
        if (aggregate.exigeReuniao() || aggregate.exigeSigiloReforcado()) {
            return 0.84d;
        }
        return 0.76d;
    }

    private String jsonList(List<String> values) {
        return values == null || values.isEmpty()
                ? "[]"
                : '[' + values.stream().filter(Objects::nonNull).map(this::escape).map(value -> '"' + value + '"').reduce((left, right) -> left + ',' + right).orElse("") + ']';
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "" : normalized;
    }

    private String safeName(Object value) {
        return value == null ? "" : value.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
