package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.governance.institucional.application.PjbGovernancaInstitucionalNormativaApplicationService;
import com.tcc.pjb.backend.core.governance.institucional.domain.PjbGovernancaInstitucionalNormativaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCutoverCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCutoverMatrixAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCutoverTribunal;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaNucleoDuroAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaNucleoDuroCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaNucleoDuroTribunal;
import com.tcc.pjb.backend.core.processo.operacao.application.ProcessoOperacaoApplicationService;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import com.tcc.pjb.backend.core.processo.prevencao.application.ProcessoPrevencaoApplicationService;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoPrevencaoAggregate;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculacaoAnaliseConsulta;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoProducaoPesadaApplicationService;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoProducaoPesadaAggregate;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoRedistribuicaoService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbSubstituicaoFederativaNucleoDuroApplicationService {

    private static final int NATIONAL_SAMPLE_SIZE = 420;
    private static final int TRIBUNAL_SAMPLE_SIZE = 42;
    private static final int COMPETENCIA_SAMPLE_SIZE = 4;

    private final PjbSubstituicaoFederativaCutoverMatrixApplicationService cutoverMatrixApplicationService;
    private final ProcessoRepository processoRepository;
    private final ProcessoPrevencaoApplicationService processoPrevencaoApplicationService;
    private final ProcessoRecursalApplicationService processoRecursalApplicationService;
    private final FederalismoRedistribuicaoService federalismoRedistribuicaoService;
    private final ProcessoOperacaoApplicationService processoOperacaoApplicationService;
    private final ProcessoProducaoPesadaApplicationService processoProducaoPesadaApplicationService;
    private final PjbGovernancaInstitucionalNormativaApplicationService governancaInstitucionalNormativaApplicationService;

    public PjbSubstituicaoFederativaNucleoDuroApplicationService(
            PjbSubstituicaoFederativaCutoverMatrixApplicationService cutoverMatrixApplicationService,
            ProcessoRepository processoRepository,
            ProcessoPrevencaoApplicationService processoPrevencaoApplicationService,
            ProcessoRecursalApplicationService processoRecursalApplicationService,
            FederalismoRedistribuicaoService federalismoRedistribuicaoService,
            ProcessoOperacaoApplicationService processoOperacaoApplicationService,
            ProcessoProducaoPesadaApplicationService processoProducaoPesadaApplicationService,
            PjbGovernancaInstitucionalNormativaApplicationService governancaInstitucionalNormativaApplicationService) {
        this.cutoverMatrixApplicationService = Objects.requireNonNull(cutoverMatrixApplicationService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoPrevencaoApplicationService = Objects.requireNonNull(processoPrevencaoApplicationService);
        this.processoRecursalApplicationService = Objects.requireNonNull(processoRecursalApplicationService);
        this.federalismoRedistribuicaoService = Objects.requireNonNull(federalismoRedistribuicaoService);
        this.processoOperacaoApplicationService = Objects.requireNonNull(processoOperacaoApplicationService);
        this.processoProducaoPesadaApplicationService = Objects.requireNonNull(processoProducaoPesadaApplicationService);
        this.governancaInstitucionalNormativaApplicationService = Objects.requireNonNull(governancaInstitucionalNormativaApplicationService);
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaNucleoDuroAggregate avaliar() {
        PjbSubstituicaoFederativaCutoverMatrixAggregate cutover = cutoverMatrixApplicationService.avaliar();
        Map<String, List<Processo>> processosPorTribunal = carregarProcessosRecentes(NATIONAL_SAMPLE_SIZE).stream()
                .collect(Collectors.groupingBy(this::resolverCodigoTribunal, LinkedHashMap::new, Collectors.toList()));
        Map<String, List<FederalismoRedistribuicaoService.VaraAnalise>> redistribuicaoPorTribunal = indexarRedistribuicaoPorTribunal();

        List<PjbSubstituicaoFederativaNucleoDuroTribunal> tribunais = cutover.tribunais().stream()
                .map(tribunal -> buildTribunal(
                        tribunal,
                        processosPorTribunal.getOrDefault(tribunal.tribunalCodigo(), List.of()),
                        redistribuicaoPorTribunal.getOrDefault(tribunal.tribunalCodigo(), List.of())
                ))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaNucleoDuroTribunal::prontoNucleoDuro).reversed()
                        .thenComparing(PjbSubstituicaoFederativaNucleoDuroTribunal::scoreGeral, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaNucleoDuroTribunal::tribunalCodigo))
                .toList();

        int tribunaisProntos = (int) tribunais.stream().filter(PjbSubstituicaoFederativaNucleoDuroTribunal::prontoNucleoDuro).count();
        int scoreNacional = clamp((int) Math.round(tribunais.stream().mapToInt(PjbSubstituicaoFederativaNucleoDuroTribunal::scoreGeral).average().orElse(0d)));
        boolean comunicacaoSigiloConectados = tribunais.stream().filter(item -> item.scoreComunicacaoSigilo() >= 70).count() >= Math.max(3, tribunais.size() / 5);
        boolean prevencaoRedistribuicaoConectadas = tribunais.stream().filter(item -> item.scorePrevencaoRedistribuicao() >= 68).count() >= Math.max(3, tribunais.size() / 5);
        boolean fluxoRecursalConectado = tribunais.stream().filter(item -> item.scoreFluxoRecursal() >= 72).count() >= Math.max(3, tribunais.size() / 5);
        boolean prontoNucleoDuro = tribunaisProntos >= Math.max(4, tribunais.size() / 6)
                && comunicacaoSigiloConectados
                && prevencaoRedistribuicaoConectadas
                && fluxoRecursalConectado;

        LinkedHashSet<String> bloqueadoresCriticos = new LinkedHashSet<>(cutover.bloqueadoresCriticos());
        tribunais.stream().flatMap(tribunal -> tribunal.bloqueadores().stream()).limit(30).forEach(bloqueadoresCriticos::add);

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(cutover.fundamentos());
        fundamentos.add("nucleoDuro.scoreNacional=" + scoreNacional);
        fundamentos.add("nucleoDuro.tribunaisProntos=" + tribunaisProntos);
        fundamentos.add("nucleoDuro.comunicacaoSigiloConectados=" + comunicacaoSigiloConectados);
        fundamentos.add("nucleoDuro.prevencaoRedistribuicaoConectadas=" + prevencaoRedistribuicaoConectadas);
        fundamentos.add("nucleoDuro.fluxoRecursalConectado=" + fluxoRecursalConectado);
        fundamentos.add("nucleoDuro.pronto=" + prontoNucleoDuro);

        return new PjbSubstituicaoFederativaNucleoDuroAggregate(
                scoreNacional,
                prontoNucleoDuro,
                comunicacaoSigiloConectados,
                prevencaoRedistribuicaoConectadas,
                fluxoRecursalConectado,
                tribunaisProntos,
                tribunais,
                List.copyOf(bloqueadoresCriticos.stream().limit(50).toList()),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaNucleoDuroTribunal avaliarTribunal(String tribunalCodigo) {
        PjbSubstituicaoFederativaCutoverTribunal tribunal = cutoverMatrixApplicationService.avaliarTribunal(tribunalCodigo);
        List<Processo> processos = carregarProcessosRecentes(TRIBUNAL_SAMPLE_SIZE * 8).stream()
                .filter(processo -> tribunal.tribunalCodigo().equals(resolverCodigoTribunal(processo)))
                .limit(TRIBUNAL_SAMPLE_SIZE)
                .toList();
        return buildTribunal(tribunal, processos, indexarRedistribuicaoPorTribunal().getOrDefault(tribunal.tribunalCodigo(), List.of()));
    }

    private PjbSubstituicaoFederativaNucleoDuroTribunal buildTribunal(
            PjbSubstituicaoFederativaCutoverTribunal tribunal,
            List<Processo> processos,
            List<FederalismoRedistribuicaoService.VaraAnalise> redistribuicoes) {
        Map<CompetenciaKey, List<Processo>> processosPorCompetencia = processos.stream()
                .collect(Collectors.groupingBy(this::resolverCompetencia, LinkedHashMap::new, Collectors.toList()));
        Map<CompetenciaKey, PjbSubstituicaoFederativaCutoverCompetencia> cutoverPorCompetencia = tribunal.competencias().stream()
                .collect(Collectors.toMap(this::resolverCompetencia, item -> item, (left, right) -> left, LinkedHashMap::new));

        LinkedHashMap<CompetenciaKey, List<Processo>> competenciaBase = new LinkedHashMap<>();
        cutoverPorCompetencia.keySet().forEach(key -> competenciaBase.putIfAbsent(key, processosPorCompetencia.getOrDefault(key, List.of())));
        processosPorCompetencia.forEach(competenciaBase::putIfAbsent);

        List<PjbSubstituicaoFederativaNucleoDuroCompetencia> competencias = competenciaBase.entrySet().stream()
                .map(entry -> buildCompetencia(
                        entry.getKey(),
                        cutoverPorCompetencia.get(entry.getKey()),
                        entry.getValue(),
                        redistribuicoes
                ))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaNucleoDuroCompetencia::prontoNucleoDuro).reversed()
                        .thenComparing(PjbSubstituicaoFederativaNucleoDuroCompetencia::scoreFluxoRecursal, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaNucleoDuroCompetencia::ramoCodigo)
                        .thenComparing(PjbSubstituicaoFederativaNucleoDuroCompetencia::ritoCodigo))
                .toList();

        Processo processoReferenciaTribunal = processos.isEmpty() ? null : processos.get(0);
        int scoreInfraestrutura = computeInfraestruturaScore(processoReferenciaTribunal);
        int scoreComunicacaoSigilo = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaNucleoDuroCompetencia::scoreComunicacaoSigilo).average().orElse((tribunal.scoreComunicacao() + tribunal.scoreSigilo()) / 2.0d)));
        int scorePrevencaoRedistribuicao = clamp((int) Math.round(competencias.stream().mapToInt(item -> (item.scorePrevencao() + item.scoreRedistribuicao()) / 2).average().orElse(64d)));
        int scoreFluxoRecursal = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaNucleoDuroCompetencia::scoreFluxoRecursal).average().orElse(60d)));
        int scoreGeral = clamp((int) Math.round(
                scoreComunicacaoSigilo * 0.28d
                        + scorePrevencaoRedistribuicao * 0.30d
                        + scoreFluxoRecursal * 0.24d
                        + scoreInfraestrutura * 0.18d));

        boolean prevencaoAtiva = competencias.stream().anyMatch(item -> item.scorePrevencao() >= 70);
        boolean redistribuicaoAssistida = competencias.stream().noneMatch(item -> item.scoreRedistribuicao() < 50);
        boolean fluxoRecursalPronto = scoreFluxoRecursal >= 72;
        boolean prontoNucleoDuro = tribunal.corteLiberado()
                && scoreComunicacaoSigilo >= 70
                && scorePrevencaoRedistribuicao >= 68
                && scoreFluxoRecursal >= 72
                && scoreInfraestrutura >= 68;

        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>(tribunal.bloqueadores());
        if (scoreComunicacaoSigilo < 70) {
            bloqueadores.add("NUCLEO_DURO_COMUNICACAO_SIGILO_INSUFICIENTE");
        }
        if (scorePrevencaoRedistribuicao < 68) {
            bloqueadores.add("NUCLEO_DURO_PREVENCAO_REDISTRIBUICAO_INSUFICIENTE");
        }
        if (scoreFluxoRecursal < 72) {
            bloqueadores.add("NUCLEO_DURO_FLUXO_RECURSAL_INSUFICIENTE");
        }
        if (scoreInfraestrutura < 68) {
            bloqueadores.add("NUCLEO_DURO_INFRAESTRUTURA_REFERENCIA_INSUFICIENTE");
        }

        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        if (prontoNucleoDuro) {
            proximasAcoes.add("AMPLIAR_CORTE_POR_COMPETENCIA_MATERIAL_COM_PREVENCAO_E_RECURSAL_ATIVOS");
            proximasAcoes.add("MANTER_RECONCILIACAO_DE_COMUNICACAO_JUDICIAL_E_SIGILO_EM_CADA_ONDA");
            proximasAcoes.add("CONGELAR_APENAS_A_FAIXA_LOCAL_EM_CASO_DE_DIVERGENCIA_RECURSAL");
        } else {
            proximasAcoes.add("ELEVAR_SCORE_PREVENCAO_REDISTRIBUICAO_ANTES_DE_NOVO_CUTOVER");
            proximasAcoes.add("REDUZIR_TRAVAS_RECURSAIS_E_FECHAR_JANELA_DE_MESMOS_AUTOS");
            proximasAcoes.add("VALIDAR_REFERENCIA_OPERACIONAL_DE_PROCESSO_REAL_POR_TRIBUNAL");
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(tribunal.fundamentos());
        fundamentos.add("nucleoDuro.scoreInfraestrutura=" + scoreInfraestrutura);
        fundamentos.add("nucleoDuro.scoreComunicacaoSigilo=" + scoreComunicacaoSigilo);
        fundamentos.add("nucleoDuro.scorePrevencaoRedistribuicao=" + scorePrevencaoRedistribuicao);
        fundamentos.add("nucleoDuro.scoreFluxoRecursal=" + scoreFluxoRecursal);
        fundamentos.add("nucleoDuro.pronto=" + prontoNucleoDuro);
        fundamentos.add("nucleoDuro.prevencaoAtiva=" + prevencaoAtiva);
        fundamentos.add("nucleoDuro.redistribuicaoAssistida=" + redistribuicaoAssistida);
        fundamentos.add("nucleoDuro.fluxoRecursalPronto=" + fluxoRecursalPronto);
        if (processoReferenciaTribunal != null) {
            fundamentos.add("nucleoDuro.processoReferencia=" + numeroProcesso(processoReferenciaTribunal));
        }

        return new PjbSubstituicaoFederativaNucleoDuroTribunal(
                tribunal.tribunalCodigo(),
                tribunal.tribunalNome(),
                tribunal.ramoJustica(),
                tribunal.legadoPrincipal(),
                tribunal.ondaAtual(),
                scoreGeral,
                scoreComunicacaoSigilo,
                scorePrevencaoRedistribuicao,
                scoreFluxoRecursal,
                scoreInfraestrutura,
                tribunal.corteLiberado(),
                prontoNucleoDuro,
                prevencaoAtiva,
                redistribuicaoAssistida,
                fluxoRecursalPronto,
                competencias.size(),
                competencias,
                List.copyOf(bloqueadores),
                List.copyOf(proximasAcoes),
                List.copyOf(fundamentos)
        );
    }

    private PjbSubstituicaoFederativaNucleoDuroCompetencia buildCompetencia(
            CompetenciaKey competenciaKey,
            PjbSubstituicaoFederativaCutoverCompetencia cutoverCompetencia,
            List<Processo> processos,
            List<FederalismoRedistribuicaoService.VaraAnalise> redistribuicoes) {
        List<Processo> amostra = processos.stream().limit(COMPETENCIA_SAMPLE_SIZE).toList();
        Processo processoReferencia = amostra.isEmpty() ? null : amostra.get(0);
        PrevencaoSnapshot prevencao = avaliarPrevencao(amostra);
        RecursalSnapshot recursal = avaliarRecursal(amostra);
        RedistribuicaoSnapshot redistribuicao = avaliarRedistribuicao(redistribuicoes, amostra);
        int scoreComunicacaoSigilo = cutoverCompetencia != null
                ? clamp((int) Math.round((cutoverCompetencia.scoreComunicacao() + cutoverCompetencia.scoreSigilo()) / 2.0d))
                : clamp(60 + (amostra.isEmpty() ? 0 : 10));
        boolean prontoNucleoDuro = (cutoverCompetencia == null || cutoverCompetencia.corteLiberado())
                && scoreComunicacaoSigilo >= 68
                && prevencao.score() >= 65
                && redistribuicao.score() >= 60
                && recursal.score() >= 70;

        LinkedHashSet<String> guardrails = new LinkedHashSet<>();
        if (cutoverCompetencia != null) {
            guardrails.addAll(cutoverCompetencia.guardrails());
        }
        guardrails.add("PREVENCAO_E_DEPENDENCIA_DEVEM_SER_REAVALIADAS_A_CADA_CORTE_DA_COMPETENCIA");
        guardrails.add("REDISTRIBUICAO_FEDERATIVA_SO_PODE_OCORRER_COM_TRILHA_AUDITAVEL_E_ROLLBACK_LOCAL");
        guardrails.add("FLUXO_RECURSAL_DA_COMPETENCIA_NAO_PODE_SER_ABERTO_COM_TRAVAS_DE_TRANSITO_OU_COMPETENCIA");

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("comunicacaoSigilo=" + scoreComunicacaoSigilo);
        fundamentos.add("prevencao.score=" + prevencao.score());
        fundamentos.add("redistribuicao.score=" + redistribuicao.score());
        fundamentos.add("recursal.score=" + recursal.score());
        if (prevencao.unidadePreventa() != null && !prevencao.unidadePreventa().isBlank()) {
            fundamentos.add("prevencao.unidade=" + prevencao.unidadePreventa());
        }
        fundamentos.addAll(prevencao.fundamentos());
        fundamentos.addAll(redistribuicao.fundamentos());
        fundamentos.addAll(recursal.fundamentos());

        return new PjbSubstituicaoFederativaNucleoDuroCompetencia(
                competenciaKey.ramo().name(),
                competenciaKey.ramo().getDescricao(),
                competenciaKey.rito().name(),
                processos.size(),
                scoreComunicacaoSigilo,
                prevencao.score(),
                redistribuicao.score(),
                recursal.score(),
                prontoNucleoDuro,
                prevencao.unidadePreventa(),
                cutoverCompetencia == null ? "janela-em-validacao" : cutoverCompetencia.janelaAtual(),
                List.copyOf(guardrails),
                List.copyOf(fundamentos.stream().limit(30).toList()),
                processoReferencia == null ? null : processoReferencia.getId(),
                processoReferencia == null ? null : numeroProcesso(processoReferencia)
        );
    }

    private PrevencaoSnapshot avaliarPrevencao(List<Processo> processos) {
        if (processos.isEmpty()) {
            return new PrevencaoSnapshot(58, false, null, List.of("prevencao.semAmostra=true"));
        }
        int score = 56;
        boolean ativa = false;
        String unidade = null;
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        int analisados = 0;
        for (Processo processo : processos) {
            try {
                ProcessoPrevencaoAggregate aggregate = processoPrevencaoApplicationService.analisar(
                        new ProcessoVinculacaoAnaliseConsulta(
                                processo.getId(),
                                numeroProcesso(processo),
                                "pjb-substituicao-nucleo-duro",
                                "cutover-matrix"
                        )
                );
                analisados++;
                if (aggregate.haPrevencao()) {
                    ativa = true;
                    score += 10;
                } else {
                    score += 4;
                }
                if (aggregate.unidadeSugerida() != null && !aggregate.unidadeSugerida().isBlank()) {
                    unidade = aggregate.unidadeSugerida();
                    score += 6;
                }
                aggregate.fundamentos().stream().limit(3).forEach(fundamentos::add);
            } catch (RuntimeException ex) {
                fundamentos.add("prevencao.erro=" + ex.getClass().getSimpleName());
                score -= 4;
            }
        }
        fundamentos.add("prevencao.analisados=" + analisados);
        fundamentos.add("prevencao.ativa=" + ativa);
        return new PrevencaoSnapshot(clamp(score), ativa, unidade, List.copyOf(fundamentos));
    }

    private RecursalSnapshot avaliarRecursal(List<Processo> processos) {
        if (processos.isEmpty()) {
            return new RecursalSnapshot(55, false, List.of("recursal.semAmostra=true"));
        }
        int score = 54;
        boolean pronto = true;
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        int analisados = 0;
        for (Processo processo : processos) {
            try {
                ProcessoRecursalAggregate aggregate = processoRecursalApplicationService.detalhar(processo.getId());
                analisados++;
                score += aggregate.totalCabiveis() > 0 ? 9 : 4;
                score += aggregate.totalMesmosAutos() > 0 ? 4 : 1;
                score += aggregate.totalExterno() > 0 ? 3 : 0;
                score -= Math.min(12, aggregate.travas().size() * 4);
                if (!aggregate.travas().isEmpty()) {
                    pronto = false;
                }
                aggregate.proximosPassos().stream().limit(2).forEach(fundamentos::add);
                aggregate.alertas().stream().limit(2).forEach(item -> fundamentos.add("alerta=" + item));
            } catch (RuntimeException ex) {
                score -= 8;
                pronto = false;
                fundamentos.add("recursal.erro=" + ex.getClass().getSimpleName());
            }
        }
        fundamentos.add("recursal.analisados=" + analisados);
        fundamentos.add("recursal.pronto=" + pronto);
        return new RecursalSnapshot(clamp(score), pronto, List.copyOf(fundamentos));
    }

    private RedistribuicaoSnapshot avaliarRedistribuicao(List<FederalismoRedistribuicaoService.VaraAnalise> redistribuicoes,
                                                         List<Processo> processos) {
        if (redistribuicoes.isEmpty()) {
            return new RedistribuicaoSnapshot(82, true, List.of("redistribuicao.varasCriticas=0"));
        }
        long candidatas = redistribuicoes.stream().mapToLong(item -> item.candidatasRedistribuicao() == null ? 0L : item.candidatasRedistribuicao().size()).sum();
        long criticasSemDestino = redistribuicoes.stream().filter(item -> item.candidatasRedistribuicao() == null || item.candidatasRedistribuicao().isEmpty()).count();
        int score = clamp((int) Math.round(70 + Math.min(18, candidatas * 2) - Math.min(24, criticasSemDestino * 12)));
        if (!processos.isEmpty()) {
            long comUnidade = processos.stream().filter(item -> item.getUnidadeJudiciariaCodigo() != null && !item.getUnidadeJudiciariaCodigo().isBlank()).count();
            score = clamp(score + (int) Math.min(8, comUnidade * 2));
        }
        boolean assistida = criticasSemDestino == 0;
        List<String> fundamentos = List.of(
                "redistribuicao.varasCriticas=" + redistribuicoes.size(),
                "redistribuicao.candidatas=" + candidatas,
                "redistribuicao.criticasSemDestino=" + criticasSemDestino,
                "redistribuicao.assistida=" + assistida
        );
        return new RedistribuicaoSnapshot(score, assistida, fundamentos);
    }

    private int computeInfraestruturaScore(Processo processoReferencia) {
        if (processoReferencia == null || processoReferencia.getId() == null) {
            return 60;
        }
        int score = 60;
        try {
            ProcessoOperacaoAggregate operacao = processoOperacaoApplicationService.detalhar(processoReferencia.getId());
            score = clamp(score
                    + readinessScore(operacao.readiness())
                    + stateScore(operacao.resilienceState())
                    + stateScore(operacao.observabilityState())
                    - (int) Math.min(18, Math.round(operacao.saturacaoMaxima() * 10))
                    - (int) Math.min(10, operacao.totalBloqueios() * 2));
        } catch (RuntimeException ignored) {
            score -= 6;
        }
        try {
            ProcessoProducaoPesadaAggregate producao = processoProducaoPesadaApplicationService.avaliar(processoReferencia.getId());
            score = clamp((int) Math.round(score * 0.55d + producao.scoreGeral() * 0.45d));
        } catch (RuntimeException ignored) {
            score -= 4;
        }
        try {
            PjbGovernancaInstitucionalNormativaAggregate governanca = governancaInstitucionalNormativaApplicationService.avaliar(processoReferencia.getId());
            score = clamp((int) Math.round(score * 0.70d + governanca.scoreGeral() * 0.30d));
        } catch (RuntimeException ignored) {
            score -= 4;
        }
        return clamp(score);
    }

    private Map<String, List<FederalismoRedistribuicaoService.VaraAnalise>> indexarRedistribuicaoPorTribunal() {
        FederalismoRedistribuicaoService.RedistribuicaoFederativaReport report = federalismoRedistribuicaoService.sugerir(0.72d);
        LinkedHashMap<String, List<FederalismoRedistribuicaoService.VaraAnalise>> out = new LinkedHashMap<>();
        for (NationalCompetenceMatrix tribunal : NationalCompetenceMatrix.values()) {
            List<FederalismoRedistribuicaoService.VaraAnalise> itens = report.varasCriticas().stream()
                    .filter(item -> tribunal.uf().equalsIgnoreCase(Objects.toString(item.uf(), "")))
                    .toList();
            out.put(tribunal.codigo(), itens);
        }
        return out;
    }

    private List<Processo> carregarProcessosRecentes(int limite) {
        return processoRepository.findAll(PageRequest.of(0, limite, Sort.by(
                Sort.Order.desc("dataUltimaMovimentacao"),
                Sort.Order.desc("id")
        ))).getContent();
    }

    private CompetenciaKey resolverCompetencia(PjbSubstituicaoFederativaCutoverCompetencia competencia) {
        RamoDireito ramo = Optional.ofNullable(RamoDireito.fromString(competencia.ramoCodigo()))
                .orElseGet(() -> Optional.ofNullable(RamoDireito.fromString(competencia.ramoDescricao())).orElse(RamoDireito.CIVIL));
        RitoProcessual rito = Optional.ofNullable(RitoProcessual.tryParse(competencia.ritoCodigo()).orElse(null)).orElse(RitoProcessual.COMUM_ORDINARIO);
        return new CompetenciaKey(ramo, rito);
    }

    private CompetenciaKey resolverCompetencia(Processo processo) {
        RamoDireito ramo = processo.getRamoDireito() == null ? RamoDireito.CIVIL : processo.getRamoDireito();
        RitoProcessual rito = processo.getRito() == null ? RitoProcessual.COMUM_ORDINARIO : processo.getRito();
        return new CompetenciaKey(ramo, rito);
    }

    private String resolverCodigoTribunal(Processo processo) {
        String tribunal = normalize(processo.getTribunal());
        if (!tribunal.isBlank()) {
            return tribunal;
        }
        String uf = normalize(processo.getUf());
        if (!uf.isBlank()) {
            return NationalCompetenceMatrix.resolver(uf, inferirRamoJustica(processo.getRamoDireito()))
                    .map(NationalCompetenceMatrix::codigo)
                    .orElse("NACIONAL");
        }
        return "NACIONAL";
    }

    private NationalCompetenceMatrix.RamoJusticaNacional inferirRamoJustica(RamoDireito ramo) {
        if (ramo == null) {
            return NationalCompetenceMatrix.RamoJusticaNacional.ESTADUAL;
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            return NationalCompetenceMatrix.RamoJusticaNacional.TRABALHO;
        }
        if (ramo == RamoDireito.ELEITORAL) {
            return NationalCompetenceMatrix.RamoJusticaNacional.ELEITORAL;
        }
        if (ramo == RamoDireito.MILITAR) {
            return NationalCompetenceMatrix.RamoJusticaNacional.MILITAR_ESTADUAL;
        }
        if (ramo == RamoDireito.PREVIDENCIARIO || ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.CONSTITUCIONAL) {
            return NationalCompetenceMatrix.RamoJusticaNacional.FEDERAL;
        }
        return NationalCompetenceMatrix.RamoJusticaNacional.ESTADUAL;
    }

    private int readinessScore(String readiness) {
        return switch (normalize(readiness)) {
            case "READY", "PRONTO", "HOT" -> 16;
            case "ASSISTED", "ASSISTIDA", "PARTIAL" -> 8;
            default -> 0;
        };
    }

    private int stateScore(String state) {
        return switch (normalize(state)) {
            case "STABLE", "HEALTHY", "VERDE" -> 12;
            case "ATTENTION", "AMARELO", "ASSISTIDA" -> 5;
            default -> -4;
        };
    }

    private String numeroProcesso(Processo processo) {
        String numero = Objects.toString(processo.getNumeroUnificado(), "").trim();
        if (!numero.isBlank()) {
            return numero;
        }
        return Objects.toString(processo.getNumeroProcesso(), "").trim();
    }

    private String normalize(String value) {
        return Objects.toString(value, "").trim().toUpperCase(Locale.ROOT);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record CompetenciaKey(RamoDireito ramo, RitoProcessual rito) {
        private CompetenciaKey {
            ramo = ramo == null ? RamoDireito.CIVIL : ramo;
            rito = rito == null ? RitoProcessual.COMUM_ORDINARIO : rito;
        }
    }

    private record PrevencaoSnapshot(int score, boolean ativa, String unidadePreventa, Collection<String> fundamentos) {
    }

    private record RecursalSnapshot(int score, boolean pronto, Collection<String> fundamentos) {
    }

    private record RedistribuicaoSnapshot(int score, boolean assistida, Collection<String> fundamentos) {
    }
}
