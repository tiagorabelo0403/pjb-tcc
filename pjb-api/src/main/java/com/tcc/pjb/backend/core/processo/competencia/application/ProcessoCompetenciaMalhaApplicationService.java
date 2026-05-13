package com.tcc.pjb.backend.core.processo.competencia.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.competencia.domain.ProcessoCompetenciaMalhaAggregate;
import com.tcc.pjb.backend.core.processo.competencia.domain.ProcessoCompetenciaMalhaItem;
import com.tcc.pjb.backend.core.processo.conexao.application.ProcessoConexaoApplicationService;
import com.tcc.pjb.backend.core.processo.conexao.domain.ProcessoConexaoAggregate;
import com.tcc.pjb.backend.core.processo.dependencia.application.ProcessoDependenciaApplicationService;
import com.tcc.pjb.backend.core.processo.dependencia.domain.ProcessoDependenciaAggregate;
import com.tcc.pjb.backend.core.processo.prevencao.application.ProcessoPrevencaoApplicationService;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoPrevencaoAggregate;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculacaoAnaliseConsulta;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaParallelExecutor;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoCompetenciaMalhaApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoPrevencaoApplicationService processoPrevencaoApplicationService;
    private final ProcessoConexaoApplicationService processoConexaoApplicationService;
    private final ProcessoDependenciaApplicationService processoDependenciaApplicationService;
    private final DecisionTraceService decisionTraceService;
    private final AuditLedgerService auditLedgerService;
    private final ProcessoMalhaParallelExecutor processoMalhaParallelExecutor;

    public ProcessoCompetenciaMalhaApplicationService(ProcessoRepository processoRepository,
                                                      ProcessoPrevencaoApplicationService processoPrevencaoApplicationService,
                                                      ProcessoConexaoApplicationService processoConexaoApplicationService,
                                                      ProcessoDependenciaApplicationService processoDependenciaApplicationService,
                                                      ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                                      ObjectProvider<AuditLedgerService> auditLedgerServiceProvider,
                                                      ProcessoMalhaParallelExecutor processoMalhaParallelExecutor) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoPrevencaoApplicationService = Objects.requireNonNull(processoPrevencaoApplicationService);
        this.processoConexaoApplicationService = Objects.requireNonNull(processoConexaoApplicationService);
        this.processoDependenciaApplicationService = Objects.requireNonNull(processoDependenciaApplicationService);
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.auditLedgerService = auditLedgerServiceProvider.getIfAvailable();
        this.processoMalhaParallelExecutor = Objects.requireNonNull(processoMalhaParallelExecutor);
    }

    @Transactional(readOnly = true)
    public ProcessoCompetenciaMalhaAggregate analisar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoVinculacaoAnaliseConsulta consulta = new ProcessoVinculacaoAnaliseConsulta(
                processoId,
                processo.getNumero(),
                "MALHA_COMPETENCIA",
                "PROCESSO_COMPETENCIA_MALHA"
        );
        ProcessoMalhaParallelExecutor.Trio<ProcessoPrevencaoAggregate, ProcessoConexaoAggregate, ProcessoDependenciaAggregate> consolidado = processoMalhaParallelExecutor.executar3(
                "PROCESSO_COMPETENCIA_MALHA",
                () -> processoPrevencaoApplicationService.analisar(consulta),
                () -> processoConexaoApplicationService.analisar(consulta),
                () -> processoDependenciaApplicationService.analisar(consulta)
        );
        ProcessoPrevencaoAggregate prevencao = consolidado.primeiro();
        ProcessoConexaoAggregate conexao = consolidado.segundo();
        ProcessoDependenciaAggregate dependencia = consolidado.terceiro();

        ArrayList<ProcessoCompetenciaMalhaItem> itens = new ArrayList<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        String eixoCompetencia = eixoCompetencia(processo);
        String tribunalAtual = normalize(processo.getTribunal());
        String unidadeAtual = normalize(processo.getVara());
        String tribunalSugerido = tribunalAtual;
        String unidadeSugerida = unidadeAtual;
        String acaoPrimaria = "MANTER_COMPETENCIA";
        boolean exigeRemessa = false;
        boolean exigeRedistribuicao = false;
        boolean travaAtosIncompativeis = false;

        fundamentos.add("EIXO_COMPETENCIA=" + eixoCompetencia);
        tribunalSugerido = resolverTribunalSugestivo(processo).orElse(tribunalAtual);
        if (!tribunalSugerido.isBlank()) {
            fundamentos.add("MATRIZ_COMPETENCIA=" + tribunalSugerido);
        }

        if (prevencao.haPrevencao()) {
            ProcessoCompetenciaMalhaItem item = new ProcessoCompetenciaMalhaItem(
                    "REMESSA_PREVENTO",
                    "PREVENCAO",
                    "REMETER_PREVENTO",
                    tribunalAtual,
                    normalize(firstNonBlank(prevencao.unidadeSugerida(), unidadeAtual)),
                    true,
                    scorePrevencao(prevencao),
                    merge(prevencao.fundamentos(), prevencao.itens().stream().flatMap(entry -> entry.fundamentos().stream()).toList())
            );
            itens.add(item);
            fundamentos.addAll(item.fundamentos());
            unidadeSugerida = firstNonBlank(item.destinoUnidade(), unidadeSugerida);
            acaoPrimaria = item.acao();
            exigeRemessa = true;
            travaAtosIncompativeis = true;
        }

        if (dependencia.haDependencia()) {
            ProcessoCompetenciaMalhaItem item = new ProcessoCompetenciaMalhaItem(
                    "DEPENDENCIA_RITUAL",
                    "DEPENDENCIA",
                    dependencia.itens().stream().anyMatch(entry -> entry.bloquearFluxo()) ? "TRAMITAR_POR_DEPENDENCIA" : "SINCRONIZAR_DEPENDENCIA",
                    tribunalAtual,
                    unidadeSugerida,
                    dependencia.itens().stream().anyMatch(entry -> entry.bloquearFluxo()),
                    scoreDependencia(dependencia),
                    merge(dependencia.fundamentos(), dependencia.itens().stream().flatMap(entry -> entry.fundamentos().stream()).toList())
            );
            itens.add(item);
            fundamentos.addAll(item.fundamentos());
            exigeRedistribuicao = exigeRedistribuicao || dependencia.itens().stream().anyMatch(entry -> entry.numeroProcesso().equals(prevencao.processoPrevento()));
            travaAtosIncompativeis = travaAtosIncompativeis || item.bloqueante();
            if (Objects.equals(acaoPrimaria, "MANTER_COMPETENCIA")) {
                acaoPrimaria = item.acao();
            }
        }

        if (conexao.haConexao()) {
            ProcessoCompetenciaMalhaItem item = new ProcessoCompetenciaMalhaItem(
                    "CORRELACAO_CONEXA",
                    "CONEXAO",
                    conexao.totalConexos() >= 3 ? "REDISTRIBUIR_POR_CONEXAO" : "AVALIAR_REUNIAO",
                    tribunalAtual,
                    unidadeSugerida,
                    false,
                    scoreConexao(conexao),
                    merge(conexao.fundamentos(), conexao.itens().stream().flatMap(entry -> entry.fundamentos().stream()).toList())
            );
            itens.add(item);
            fundamentos.addAll(item.fundamentos());
            exigeRedistribuicao = exigeRedistribuicao || Objects.equals(item.acao(), "REDISTRIBUIR_POR_CONEXAO");
            if (Objects.equals(acaoPrimaria, "MANTER_COMPETENCIA")) {
                acaoPrimaria = item.acao();
            }
        }

        ProcessoCompetenciaMalhaAggregate aggregate = new ProcessoCompetenciaMalhaAggregate(
                processoId,
                processo.getNumero(),
                normalize(safeName(processo.getRamoDireito())),
                eixoCompetencia,
                tribunalAtual,
                unidadeAtual,
                tribunalSugerido,
                unidadeSugerida,
                acaoPrimaria,
                exigeRemessa,
                exigeRedistribuicao,
                travaAtosIncompativeis,
                List.copyOf(itens),
                List.copyOf(fundamentos.stream().limit(50).toList()),
                Instant.now()
        );
        registrarRastro(aggregate);
        return aggregate;
    }
    private Optional<String> resolverTribunalSugestivo(Processo processo) {
        String tribunal = normalize(processo.getTribunal());
        if (!tribunal.isBlank()) {
            return Optional.of(tribunal);
        }
        String uf = normalize(processo.getUf());
        if (uf.isBlank()) {
            return Optional.empty();
        }
        return resolverPorUfERamo(processo);
    }


    private Optional<String> resolverPorUfERamo(Processo processo) {
        String uf = normalize(processo.getUf());
        if (uf.isBlank()) {
            return Optional.empty();
        }
        return NationalCompetenceMatrix.resolver(uf, ramoJustica(processo)).map(NationalCompetenceMatrix::codigo);
    }

    private NationalCompetenceMatrix.RamoJusticaNacional ramoJustica(Processo processo) {
        String ramo = normalize(safeName(processo.getRamoDireito()));
        return switch (ramo) {
            case "TRABALHISTA", "TRABALHO" -> NationalCompetenceMatrix.RamoJusticaNacional.TRABALHO;
            case "ELEITORAL" -> NationalCompetenceMatrix.RamoJusticaNacional.ELEITORAL;
            case "MILITAR", "PENAL_MILITAR" -> NationalCompetenceMatrix.RamoJusticaNacional.MILITAR_ESTADUAL;
            case "PREVIDENCIARIO", "TRIBUTARIO" -> NationalCompetenceMatrix.RamoJusticaNacional.FEDERAL;
            default -> NationalCompetenceMatrix.RamoJusticaNacional.ESTADUAL;
        };
    }

    private String eixoCompetencia(Processo processo) {
        String ramo = normalize(safeName(processo.getRamoDireito()));
        return switch (ramo) {
            case "PENAL", "PROCESSO_PENAL", "PENAL_MILITAR" -> "JURISDICAO_E_COMPETENCIA_PENAL";
            case "TRABALHISTA", "TRABALHO" -> "JURISDICAO_E_COMPETENCIA_TRABALHISTA";
            case "FAZENDA_PUBLICA", "PREVIDENCIARIO", "TRIBUTARIO" -> "JURISDICAO_E_COMPETENCIA_PUBLICA";
            case "ELEITORAL" -> "JURISDICAO_E_COMPETENCIA_ELEITORAL";
            case "MILITAR" -> "JURISDICAO_E_COMPETENCIA_MILITAR";
            default -> "JURISDICAO_E_COMPETENCIA_COMUM";
        };
    }

    private double scorePrevencao(ProcessoPrevencaoAggregate aggregate) {
        return aggregate.itens().stream().mapToDouble(item -> item.bloquearDistribuicao() ? Math.max(item.score(), 0.92d) : item.score()).max().orElse(aggregate.haPrevencao() ? 0.82d : 0d);
    }

    private double scoreConexao(ProcessoConexaoAggregate aggregate) {
        double densidade = Math.min(1d, aggregate.totalConexos() / 5d);
        double base = aggregate.itens().stream().mapToDouble(ProcessoConexaoItem -> ProcessoConexaoItem.score()).max().orElse(0d);
        return Math.max(base, densidade);
    }

    private double scoreDependencia(ProcessoDependenciaAggregate aggregate) {
        return aggregate.itens().stream().mapToDouble(item -> item.bloquearFluxo() ? Math.max(item.score(), 0.9d) : item.score()).max().orElse(aggregate.haDependencia() ? 0.75d : 0d);
    }

    private void registrarRastro(ProcessoCompetenciaMalhaAggregate aggregate) {
        if (decisionTraceService != null) {
            decisionTraceService.record(
                    "PROCESSO_COMPETENCIA_MALHA",
                    "PROCESSO",
                    String.valueOf(aggregate.processoId()),
                    BigDecimal.valueOf(confianca(aggregate)),
                    jsonList(aggregate.fundamentos()),
                    jsonList(List.of(aggregate.acaoPrimaria(), aggregate.eixoCompetencia())),
                    aggregate.numeroProcesso(),
                    aggregate.acaoPrimaria() + ':' + aggregate.tribunalSugerido() + ':' + aggregate.unidadeSugerida(),
                    "PJB_COMPETENCIA_MALHA_V1",
                    jsonList(aggregate.itens().stream().map(item -> item.codigo() + ':' + item.acao()).toList())
            );
        }
        if (auditLedgerService != null) {
            auditLedgerService.appendSafely(
                    "PROCESSO_COMPETENCIA_MALHA",
                    "PROCESSO",
                    String.valueOf(aggregate.processoId()),
                    aggregate.acaoPrimaria() + ':' + aggregate.tribunalSugerido() + ':' + aggregate.unidadeSugerida(),
                    "Competência consolidada pela malha nacional"
            );
        }
    }

    private double confianca(ProcessoCompetenciaMalhaAggregate aggregate) {
        return aggregate.itens().stream().mapToDouble(ProcessoCompetenciaMalhaItem::score).max().orElse(aggregate.exigeRemessa() ? 0.94d : aggregate.exigeRedistribuicao() ? 0.84d : 0.71d);
    }

    private String jsonList(List<String> values) {
        return values == null || values.isEmpty()
                ? "[]"
                : '[' + values.stream().filter(Objects::nonNull).map(this::escape).map(value -> '"' + value + '"').reduce((left, right) -> left + ',' + right).orElse("") + ']';
    }

    private List<String> merge(List<String> left, List<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (left != null) {
            left.stream().filter(Objects::nonNull).map(String::trim).filter(text -> !text.isBlank()).forEach(merged::add);
        }
        if (right != null) {
            right.stream().filter(Objects::nonNull).map(String::trim).filter(text -> !text.isBlank()).forEach(merged::add);
        }
        return List.copyOf(merged);
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
        return value == null ? "NAO_INFORMADO" : value.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
