package com.tcc.pjb.backend.core.processo.anomalia.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaMalhaAggregate;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaMalhaItem;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoMalhaNacionalApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalAggregate;
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
public class ProcessoAnomaliaMalhaApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoMalhaNacionalApplicationService processoMalhaNacionalApplicationService;
    private final OutboxPublisher outboxPublisher;
    private final DecisionTraceService decisionTraceService;
    private final AuditLedgerService auditLedgerService;

    public ProcessoAnomaliaMalhaApplicationService(ProcessoRepository processoRepository,
                                                   ProcessoMalhaNacionalApplicationService processoMalhaNacionalApplicationService,
                                                   OutboxPublisher outboxPublisher,
                                                   ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                                   ObjectProvider<AuditLedgerService> auditLedgerServiceProvider) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoMalhaNacionalApplicationService = Objects.requireNonNull(processoMalhaNacionalApplicationService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.auditLedgerService = auditLedgerServiceProvider.getIfAvailable();
    }

    @Transactional(readOnly = true)
    public ProcessoAnomaliaMalhaAggregate detalhar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoMalhaNacionalAggregate malha = processoMalhaNacionalApplicationService.detalhar(processoId);
        ArrayList<ProcessoAnomaliaMalhaItem> itens = new ArrayList<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(malha.fundamentos());

        if (mesmaParteNosDoisPolos(processo)) {
            itens.add(new ProcessoAnomaliaMalhaItem(
                    "POLOS_COM_DOCUMENTO_IGUAL",
                    "IDENTIDADE",
                    98,
                    "CRITICO",
                    true,
                    "Documento repetido em polos opostos",
                    "O mesmo identificador apareceu no polo ativo e no polo passivo do processo.",
                    List.of(firstNonBlank(processo.getParteAutoraCpf(), "SEM_DOCUMENTO"))
            ));
        }
        if (representacaoSobreposta(processo)) {
            itens.add(new ProcessoAnomaliaMalhaItem(
                    "REPRESENTACAO_SOBREPOSTA",
                    "CONFLITO_INTERESSE",
                    92,
                    "CRITICO",
                    true,
                    "Representação potencialmente conflitante",
                    "O CPF do usuário responsável coincide com uma das partes materiais do feito.",
                    List.of(firstNonBlank(processo.getUsuario() != null ? processo.getUsuario().getCpf() : null, "SEM_USUARIO"))
            ));
        }
        if (malha.totalVerticesIdentidade() >= 10) {
            itens.add(new ProcessoAnomaliaMalhaItem(
                    "DENSIDADE_IDENTIDADE_ELEVADA",
                    "GRAFO",
                    Math.min(99, 55 + malha.totalVerticesIdentidade() * 3),
                    malha.totalVerticesIdentidade() >= 14 ? "ALTO" : "ATENCAO",
                    malha.totalVerticesIdentidade() >= 14,
                    "Densidade relacional acima do esperado",
                    malha.totalVerticesIdentidade() + " vértices identitários orbitam o mesmo feito.",
                    malha.hotspots()
            ));
        }
        if (malha.totalProcessosCorrelatos() >= 4) {
            itens.add(new ProcessoAnomaliaMalhaItem(
                    "LITIGANCIA_REPETITIVA_CLUSTER",
                    "RECORRENCIA",
                    Math.min(99, 48 + malha.totalProcessosCorrelatos() * 6),
                    malha.totalProcessosCorrelatos() >= 7 ? "ALTO" : "ATENCAO",
                    malha.totalProcessosCorrelatos() >= 7,
                    "Cluster processual recorrente",
                    malha.totalProcessosCorrelatos() + " feitos correlatos foram projetados pela malha nacional.",
                    malha.fundamentos()
            ));
        }
        if (malha.nivelSigiloRecomendado().nivel() > malha.nivelSigiloAtual().nivel()) {
            itens.add(new ProcessoAnomaliaMalhaItem(
                    "SIGILO_SUBDIMENSIONADO",
                    "SIGILO",
                    88,
                    "ALTO",
                    true,
                    "Patamar de sigilo abaixo do recomendado",
                    malha.nivelSigiloAtual().name() + " -> " + malha.nivelSigiloRecomendado().name(),
                    malha.riscos().stream().filter(risco -> Objects.equals(risco.dominio(), "SIGILO") || Objects.equals(risco.dominio(), "EVIDENCIA")).flatMap(risco -> risco.fundamentos().stream()).toList()
            ));
        }
        if (malha.travaDistribuicaoOuFluxo() && malha.totalBloqueios() >= 2) {
            itens.add(new ProcessoAnomaliaMalhaItem(
                    "TRAVAS_MULTIPLAS",
                    "OPERACAO",
                    90,
                    "ALTO",
                    true,
                    "Bloqueios múltiplos no mesmo recorte",
                    malha.totalBloqueios() + " bloqueios foram empilhados pela malha nacional.",
                    malha.hotspots()
            ));
        }

        itens.forEach(item -> fundamentos.addAll(item.fundamentos()));
        int scoreGlobal = itens.stream().mapToInt(ProcessoAnomaliaMalhaItem::score).max().orElse(malha.riscos().isEmpty() ? 8 : 42);
        boolean exigeEscalonamento = itens.stream().anyMatch(ProcessoAnomaliaMalhaItem::exigeEscalonamento);
        String nivelGlobal;
        if (scoreGlobal >= 90) {
            nivelGlobal = "CRITICO";
        } else if (scoreGlobal >= 75) {
            nivelGlobal = "ALTO";
        } else if (scoreGlobal >= 50) {
            nivelGlobal = "ATENCAO";
        } else {
            nivelGlobal = "NORMAL";
        }
        return new ProcessoAnomaliaMalhaAggregate(
                processoId,
                processo.getNumero(),
                nivelGlobal,
                scoreGlobal,
                exigeEscalonamento,
                List.copyOf(itens),
                List.copyOf(fundamentos.stream().limit(60).toList()),
                Instant.now()
        );
    }

    @Transactional
    public ProcessoAnomaliaMalhaAggregate escalar(Long processoId) {
        ProcessoAnomaliaMalhaAggregate aggregate = detalhar(processoId);
        outboxPublisher.enqueue(
                "processo.malha.anomalia." + aggregate.processoId(),
                "PROCESSO_ANOMALIA_MALHA_DETECTADA",
                Map.of(
                        "processoId", aggregate.processoId(),
                        "numeroProcesso", aggregate.numeroProcesso(),
                        "nivelGlobal", aggregate.nivelGlobal(),
                        "scoreGlobal", aggregate.scoreGlobal(),
                        "exigeEscalonamento", aggregate.exigeEscalonamento(),
                        "totalItens", aggregate.itens().size()
                ),
                Map.of(
                        "boundedContext", "processo.anomalia",
                        "eventVersion", "1",
                        "nivelGlobal", aggregate.nivelGlobal()
                ),
                "processo-anomalia-malha:" + aggregate.processoId() + ':' + aggregate.nivelGlobal() + ':' + aggregate.scoreGlobal(),
                "PROCESSO",
                String.valueOf(aggregate.processoId())
        );
        if (decisionTraceService != null) {
            decisionTraceService.record(
                    "PROCESSO_ANOMALIA_MALHA",
                    "PROCESSO",
                    String.valueOf(aggregate.processoId()),
                    BigDecimal.valueOf(Math.max(0.51d, aggregate.scoreGlobal() / 100d)),
                    jsonList(aggregate.fundamentos()),
                    jsonList(aggregate.itens().stream().map(item -> item.codigo() + ':' + item.nivel()).toList()),
                    aggregate.numeroProcesso(),
                    aggregate.nivelGlobal() + ':' + aggregate.scoreGlobal(),
                    "PJB_ANOMALIA_MALHA_V1",
                    jsonList(List.of(String.valueOf(aggregate.exigeEscalonamento()), String.valueOf(aggregate.itens().size())))
            );
        }
        if (auditLedgerService != null) {
            auditLedgerService.appendSafely(
                    "PROCESSO_ANOMALIA_MALHA",
                    "PROCESSO",
                    String.valueOf(aggregate.processoId()),
                    aggregate.nivelGlobal() + ':' + aggregate.scoreGlobal(),
                    "Anomalia consolidada pela malha nacional"
            );
        }
        return aggregate;
    }

    private boolean mesmaParteNosDoisPolos(Processo processo) {
        String ativo = digits(processo.getParteAutoraCpf());
        String passivo = digits(processo.getParteReuCpf());
        return !ativo.isBlank() && ativo.equals(passivo);
    }

    private boolean representacaoSobreposta(Processo processo) {
        String advogado = digits(processo.getUsuario() != null ? processo.getUsuario().getCpf() : null);
        if (advogado.isBlank()) {
            return false;
        }
        return advogado.equals(digits(processo.getParteAutoraCpf())) || advogado.equals(digits(processo.getParteReuCpf()));
    }

    private String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String jsonList(List<String> values) {
        return values == null || values.isEmpty()
                ? "[]"
                : '[' + values.stream().filter(Objects::nonNull).map(this::escape).map(value -> '"' + value + '"').reduce((left, right) -> left + ',' + right).orElse("") + ']';
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
