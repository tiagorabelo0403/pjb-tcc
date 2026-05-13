package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPosColetivaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPosColetivaCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPosColetivaTribunal;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTutelaColetivaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTutelaColetivaCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTutelaColetivaTribunal;
import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoLote;
import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoTrilha;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.transito.PostJudgmentOperationalProfile;
import com.tcc.pjb.backend.core.transito.PostJudgmentOperationalResolver;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbSubstituicaoFederativaPosColetivaApplicationService {

    private static final int NATIONAL_SAMPLE_SIZE = 720;
    private static final int TRIBUNAL_SAMPLE_SIZE = 120;
    private static final int COMPETENCIAS_LIMITE = 10;

    private final PjbSubstituicaoFederativaTutelaColetivaApplicationService tutelaColetivaApplicationService;
    private final ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    private final PostJudgmentOperationalResolver postJudgmentOperationalResolver;
    private final ProcessoExecucaoApplicationService processoExecucaoApplicationService;
    private final ProcessoRepository processoRepository;

    public PjbSubstituicaoFederativaPosColetivaApplicationService(
            PjbSubstituicaoFederativaTutelaColetivaApplicationService tutelaColetivaApplicationService,
            ProcessoDocumentoApplicationService processoDocumentoApplicationService,
            PostJudgmentOperationalResolver postJudgmentOperationalResolver,
            ProcessoExecucaoApplicationService processoExecucaoApplicationService,
            ProcessoRepository processoRepository) {
        this.tutelaColetivaApplicationService = Objects.requireNonNull(tutelaColetivaApplicationService);
        this.processoDocumentoApplicationService = Objects.requireNonNull(processoDocumentoApplicationService);
        this.postJudgmentOperationalResolver = Objects.requireNonNull(postJudgmentOperationalResolver);
        this.processoExecucaoApplicationService = Objects.requireNonNull(processoExecucaoApplicationService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaPosColetivaAggregate avaliar() {
        PjbSubstituicaoFederativaTutelaColetivaAggregate tutela = tutelaColetivaApplicationService.avaliar();
        Map<String, PjbSubstituicaoFederativaTutelaColetivaTribunal> baseline = tutela.tribunais().stream()
                .collect(Collectors.toMap(PjbSubstituicaoFederativaTutelaColetivaTribunal::tribunalCodigo, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<Processo>> processosPorTribunal = carregarProcessosRecentes(NATIONAL_SAMPLE_SIZE).stream()
                .collect(Collectors.groupingBy(this::resolverCodigoTribunal, LinkedHashMap::new, Collectors.toList()));

        List<PjbSubstituicaoFederativaPosColetivaTribunal> tribunais = baseline.values().stream()
                .map(tribunal -> buildTribunal(tribunal, processosPorTribunal.getOrDefault(tribunal.tribunalCodigo(), List.of())))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaPosColetivaTribunal::malhaPosColetivaPronta).reversed()
                        .thenComparing(PjbSubstituicaoFederativaPosColetivaTribunal::scoreGeral, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaPosColetivaTribunal::tribunalCodigo))
                .toList();

        int tribunaisProntos = (int) tribunais.stream().filter(PjbSubstituicaoFederativaPosColetivaTribunal::malhaPosColetivaPronta).count();
        int scoreNacional = clamp((int) Math.round(tribunais.stream().mapToInt(PjbSubstituicaoFederativaPosColetivaTribunal::scoreGeral).average().orElse(0d)));
        boolean coisaJulgadaColetivaGovernada = tribunais.stream().filter(item -> item.scoreCoisaJulgadaColetiva() >= 70).count() >= Math.max(3, tribunais.size() / 5);
        boolean liquidacaoColetivaGovernada = tribunais.stream().filter(item -> item.scoreLiquidacaoColetiva() >= 68).count() >= Math.max(3, tribunais.size() / 5);
        boolean habilitacaoIndividualGovernada = tribunais.stream().filter(item -> item.scoreHabilitacaoIndividual() >= 66).count() >= Math.max(3, tribunais.size() / 5);
        boolean cumprimentoPulverizadoLotesGovernado = tribunais.stream().filter(item -> item.scoreCumprimentoPulverizadoLotes() >= 68).count() >= Math.max(3, tribunais.size() / 5);
        boolean pronta = tribunaisProntos >= Math.max(4, tribunais.size() / 6)
                && coisaJulgadaColetivaGovernada
                && liquidacaoColetivaGovernada
                && habilitacaoIndividualGovernada
                && cumprimentoPulverizadoLotesGovernado;

        LinkedHashSet<String> bloqueadoresCriticos = new LinkedHashSet<>(tutela.bloqueadoresCriticos());
        tribunais.stream().flatMap(tribunal -> tribunal.bloqueadores().stream()).limit(40).forEach(bloqueadoresCriticos::add);

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(tutela.fundamentos());
        fundamentos.add("posColetiva.scoreNacional=" + scoreNacional);
        fundamentos.add("posColetiva.tribunaisProntos=" + tribunaisProntos);
        fundamentos.add("posColetiva.coisaJulgada=" + coisaJulgadaColetivaGovernada);
        fundamentos.add("posColetiva.liquidacao=" + liquidacaoColetivaGovernada);
        fundamentos.add("posColetiva.habilitacao=" + habilitacaoIndividualGovernada);
        fundamentos.add("posColetiva.cumprimentoLotes=" + cumprimentoPulverizadoLotesGovernado);
        fundamentos.add("posColetiva.malhaPronta=" + pronta);

        return new PjbSubstituicaoFederativaPosColetivaAggregate(
                scoreNacional,
                pronta,
                coisaJulgadaColetivaGovernada,
                liquidacaoColetivaGovernada,
                habilitacaoIndividualGovernada,
                cumprimentoPulverizadoLotesGovernado,
                tribunaisProntos,
                tribunais,
                List.copyOf(bloqueadoresCriticos.stream().limit(60).toList()),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaPosColetivaTribunal avaliarTribunal(String tribunalCodigo) {
        PjbSubstituicaoFederativaTutelaColetivaTribunal baseline = tutelaColetivaApplicationService.avaliarTribunal(tribunalCodigo);
        List<Processo> processos = carregarProcessosRecentes(TRIBUNAL_SAMPLE_SIZE * 8).stream()
                .filter(processo -> baseline.tribunalCodigo().equals(resolverCodigoTribunal(processo)))
                .limit(TRIBUNAL_SAMPLE_SIZE)
                .toList();
        return buildTribunal(baseline, processos);
    }

    private PjbSubstituicaoFederativaPosColetivaTribunal buildTribunal(PjbSubstituicaoFederativaTutelaColetivaTribunal baseline,
                                                                       List<Processo> processos) {
        Map<String, PjbSubstituicaoFederativaTutelaColetivaCompetencia> baselineCompetencias = baseline.competencias().stream()
                .collect(Collectors.toMap(PjbSubstituicaoFederativaTutelaColetivaCompetencia::competenciaCodigo, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<PosColetivoSnapshot> snapshots = processos.stream()
                .map(processo -> avaliarProcesso(processo, baselineCompetencias))
                .filter(PosColetivoSnapshot::relevantePosColetiva)
                .toList();

        Map<String, List<PosColetivoSnapshot>> porCompetencia = snapshots.stream()
                .collect(Collectors.groupingBy(snapshot -> snapshot.competencia().codigo(), LinkedHashMap::new, Collectors.toList()));

        List<PjbSubstituicaoFederativaPosColetivaCompetencia> competencias = porCompetencia.entrySet().stream()
                .map(entry -> buildCompetencia(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaPosColetivaCompetencia::malhaPosColetivaPronta).reversed()
                        .thenComparing(this::scoreCompetencia, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaPosColetivaCompetencia::competenciaCodigo))
                .limit(COMPETENCIAS_LIMITE)
                .toList();

        int scoreCoisaJulgadaColetiva = competencias.isEmpty()
                ? clamp((baseline.scoreTutelaColetiva() + baseline.scoreDemandasEstruturais()) / 2)
                : clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaPosColetivaCompetencia::scoreCoisaJulgadaColetiva).average().orElse(0d)));
        int scoreLiquidacaoColetiva = competencias.isEmpty()
                ? clamp((baseline.scoreExecucaoColetiva() + baseline.scoreCumprimentoMassa()) / 2)
                : clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaPosColetivaCompetencia::scoreLiquidacaoColetiva).average().orElse(0d)));
        int scoreHabilitacaoIndividual = competencias.isEmpty()
                ? clamp((baseline.scoreTutelaColetiva() + baseline.scoreCumprimentoMassa()) / 2)
                : clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaPosColetivaCompetencia::scoreHabilitacaoIndividual).average().orElse(0d)));
        int scoreCumprimentoPulverizadoLotes = competencias.isEmpty()
                ? clamp((baseline.scoreExecucaoColetiva() + baseline.scoreCumprimentoMassa()) / 2)
                : clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaPosColetivaCompetencia::scoreCumprimentoPulverizadoLotes).average().orElse(0d)));
        int scoreGeral = clamp((int) Math.round((scoreCoisaJulgadaColetiva + scoreLiquidacaoColetiva + scoreHabilitacaoIndividual + scoreCumprimentoPulverizadoLotes) / 4.0d));
        boolean pronta = baseline.malhaTutelaColetivaPronta()
                && scoreCoisaJulgadaColetiva >= 70
                && scoreLiquidacaoColetiva >= 68
                && scoreHabilitacaoIndividual >= 66
                && scoreCumprimentoPulverizadoLotes >= 68;

        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>(baseline.bloqueadores());
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>(baseline.proximasAcoes());
        if (scoreCoisaJulgadaColetiva < 70) {
            bloqueadores.add("Coisa julgada coletiva ainda não está governada de forma uniforme no tribunal.");
            proximasAcoes.add("Ligar trânsito coletivo, publicação de mérito e filtros de res judicata à malha nacional.");
        }
        if (scoreLiquidacaoColetiva < 68) {
            bloqueadores.add("Liquidação coletiva ainda não convergiu com trilhas técnicas e documentais do tribunal.");
            proximasAcoes.add("Conectar cálculo, contadoria e publicação de lotes ao pós-julgamento coletivo.");
        }
        if (scoreHabilitacaoIndividual < 66) {
            bloqueadores.add("Habilitação individual de beneficiários ainda não está previsível por competência material.");
            proximasAcoes.add("Abrir esteira de habilitação individual vinculada ao título coletivo e à competência de origem.");
        }
        if (scoreCumprimentoPulverizadoLotes < 68) {
            bloqueadores.add("Cumprimento pulverizado por lotes ainda não possui trilha uniforme de monitoramento e retomada.");
            proximasAcoes.add("Costurar publicação, lotes documentais, execução e retomada pós-falha por tribunal.");
        }
        if (!pronta) {
            proximasAcoes.add("Manter operação assistida do pós-coletivo até convergir res judicata, liquidação, habilitação e cumprimento por lotes.");
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(baseline.fundamentos());
        fundamentos.add("posColetiva.scoreGeral=" + scoreGeral);
        fundamentos.add("posColetiva.coisaJulgada=" + scoreCoisaJulgadaColetiva);
        fundamentos.add("posColetiva.liquidacao=" + scoreLiquidacaoColetiva);
        fundamentos.add("posColetiva.habilitacao=" + scoreHabilitacaoIndividual);
        fundamentos.add("posColetiva.cumprimentoLotes=" + scoreCumprimentoPulverizadoLotes);
        fundamentos.add("posColetiva.competencias=" + competencias.size());
        fundamentos.add("posColetiva.pronta=" + pronta);

        return new PjbSubstituicaoFederativaPosColetivaTribunal(
                baseline.tribunalCodigo(),
                baseline.tribunalNome(),
                baseline.ramoJustica(),
                baseline.legadoPrincipal(),
                baseline.ondaAtual(),
                scoreGeral,
                scoreCoisaJulgadaColetiva,
                scoreLiquidacaoColetiva,
                scoreHabilitacaoIndividual,
                scoreCumprimentoPulverizadoLotes,
                baseline.malhaTutelaColetivaPronta(),
                pronta,
                competencias.size(),
                competencias,
                List.copyOf(bloqueadores.stream().limit(30).toList()),
                List.copyOf(proximasAcoes.stream().limit(20).toList()),
                List.copyOf(fundamentos)
        );
    }

    private PjbSubstituicaoFederativaPosColetivaCompetencia buildCompetencia(String competenciaCodigo,
                                                                             List<PosColetivoSnapshot> snapshots) {
        PosColetivoSnapshot referencia = snapshots.stream()
                .max(Comparator.comparing(PosColetivoSnapshot::scoreGeral)
                        .thenComparing(snapshot -> Objects.requireNonNullElse(snapshot.processoId(), 0L)))
                .orElseThrow();
        int total = snapshots.size();
        int scoreCoisaJulgadaColetiva = clamp((int) Math.round(snapshots.stream().mapToInt(PosColetivoSnapshot::scoreCoisaJulgadaColetiva).average().orElse(0d)));
        int scoreLiquidacaoColetiva = clamp((int) Math.round(snapshots.stream().mapToInt(PosColetivoSnapshot::scoreLiquidacaoColetiva).average().orElse(0d)));
        int scoreHabilitacaoIndividual = clamp((int) Math.round(snapshots.stream().mapToInt(PosColetivoSnapshot::scoreHabilitacaoIndividual).average().orElse(0d)));
        int scoreCumprimentoPulverizadoLotes = clamp((int) Math.round(snapshots.stream().mapToInt(PosColetivoSnapshot::scoreCumprimentoPulverizadoLotes).average().orElse(0d)));
        boolean coisaJulgadaColetivaAtiva = snapshots.stream().anyMatch(PosColetivoSnapshot::coisaJulgadaColetivaAtiva);
        boolean liquidacaoColetivaAtiva = snapshots.stream().anyMatch(PosColetivoSnapshot::liquidacaoColetivaAtiva);
        boolean habilitacaoIndividualAtiva = snapshots.stream().anyMatch(PosColetivoSnapshot::habilitacaoIndividualAtiva);
        boolean cumprimentoPulverizadoLotesAtivo = snapshots.stream().anyMatch(PosColetivoSnapshot::cumprimentoPulverizadoLotesAtivo);
        boolean pronta = scoreCoisaJulgadaColetiva >= 70
                && scoreLiquidacaoColetiva >= 68
                && scoreHabilitacaoIndividual >= 66
                && scoreCumprimentoPulverizadoLotes >= 68;

        LinkedHashSet<String> guardrails = new LinkedHashSet<>();
        snapshots.stream().flatMap(snapshot -> snapshot.guardrails().stream()).limit(12).forEach(guardrails::add);
        if (!coisaJulgadaColetivaAtiva) {
            guardrails.add("Sem prova consistente de coisa julgada coletiva na amostra da competência.");
        }
        if (!liquidacaoColetivaAtiva) {
            guardrails.add("Liquidação coletiva ainda não apareceu com trilha técnica suficiente nesta competência.");
        }
        if (!habilitacaoIndividualAtiva) {
            guardrails.add("Habilitação individual ainda não está previsível na amostra da competência.");
        }
        if (!cumprimentoPulverizadoLotesAtivo) {
            guardrails.add("Cumprimento pulverizado por lotes ainda não apareceu com governança uniforme nesta competência.");
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        snapshots.stream().flatMap(snapshot -> snapshot.fundamentos().stream()).limit(18).forEach(fundamentos::add);
        fundamentos.add("competencia.total=" + total);
        fundamentos.add("competencia.coisaJulgada=" + thingPercentage(snapshots, PosColetivoSnapshot::coisaJulgadaColetivaAtiva));
        fundamentos.add("competencia.liquidacao=" + thingPercentage(snapshots, PosColetivoSnapshot::liquidacaoColetivaAtiva));
        fundamentos.add("competencia.habilitacao=" + thingPercentage(snapshots, PosColetivoSnapshot::habilitacaoIndividualAtiva));
        fundamentos.add("competencia.cumprimentoLotes=" + thingPercentage(snapshots, PosColetivoSnapshot::cumprimentoPulverizadoLotesAtivo));
        fundamentos.add("competencia.pronta=" + pronta);

        return new PjbSubstituicaoFederativaPosColetivaCompetencia(
                competenciaCodigo,
                referencia.competencia().ramoCodigo(),
                referencia.competencia().ramoNome(),
                referencia.competencia().ritoCodigo(),
                total,
                scoreCoisaJulgadaColetiva,
                scoreLiquidacaoColetiva,
                scoreHabilitacaoIndividual,
                scoreCumprimentoPulverizadoLotes,
                pronta,
                coisaJulgadaColetivaAtiva,
                liquidacaoColetivaAtiva,
                habilitacaoIndividualAtiva,
                cumprimentoPulverizadoLotesAtivo,
                pronta ? "janela-pos-coletiva-governada" : "janela-pos-coletiva-assistida",
                List.copyOf(guardrails),
                List.copyOf(fundamentos),
                referencia.processoId(),
                referencia.numeroReferencia()
        );
    }

    private PosColetivoSnapshot avaliarProcesso(Processo processo,
                                                Map<String, PjbSubstituicaoFederativaTutelaColetivaCompetencia> baselineCompetencias) {
        CompetenciaKey competencia = resolverCompetencia(processo);
        PjbSubstituicaoFederativaTutelaColetivaCompetencia baseline = baselineCompetencias.get(competencia.codigo());
        boolean relevantePosColetiva = isRelevantPosColetiva(processo, baseline);
        ProcessoDocumentoAggregate documental = safeDocumental(processo.getId());
        ProcessoExecucaoAggregate execucao = safeExecucao(processo.getId());
        String qualifier = qualifierPosColetivo(processo);
        PostJudgmentOperationalProfile perfilTransito = safePostJudgment(processo, ProcessoLifecycleAction.CERTIFICAR_TRANSITO, qualifier);
        PostJudgmentOperationalProfile perfilCumprimento = safePostJudgment(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, qualifier);

        boolean coisaJulgadaColetivaAtiva = relevantePosColetiva && isCoisaJulgadaColetivaAtiva(processo, documental, perfilTransito);
        boolean liquidacaoColetivaAtiva = relevantePosColetiva && isLiquidacaoColetivaAtiva(processo, documental, perfilCumprimento, execucao);
        boolean habilitacaoIndividualAtiva = relevantePosColetiva && isHabilitacaoIndividualAtiva(processo, documental, perfilCumprimento);
        boolean cumprimentoPulverizadoLotesAtivo = relevantePosColetiva && isCumprimentoPulverizadoLotesAtivo(processo, documental, perfilCumprimento, execucao);

        int scoreBase = baseline == null ? 34 : clamp((baseline.scoreTutelaColetiva() + baseline.scoreCumprimentoMassa()) / 2);
        int scoreCoisaJulgadaColetiva = scoreCoisaJulgadaColetiva(processo, scoreBase, documental, perfilTransito, coisaJulgadaColetivaAtiva);
        int scoreLiquidacaoColetiva = scoreLiquidacaoColetiva(processo, scoreBase, documental, perfilCumprimento, execucao, liquidacaoColetivaAtiva);
        int scoreHabilitacaoIndividual = scoreHabilitacaoIndividual(processo, scoreBase, documental, perfilCumprimento, habilitacaoIndividualAtiva);
        int scoreCumprimentoPulverizadoLotes = scoreCumprimentoPulverizadoLotes(processo, scoreBase, documental, perfilCumprimento, execucao, cumprimentoPulverizadoLotesAtivo);

        LinkedHashSet<String> guardrails = new LinkedHashSet<>();
        if (documental == null) {
            guardrails.add("Acervo documental ainda não foi materializado com nitidez suficiente para o pós-coletivo.");
        }
        if (perfilTransito == null) {
            guardrails.add("Perfil de trânsito coletivo ainda não ficou disponível nesta amostra.");
        }
        if (perfilCumprimento == null) {
            guardrails.add("Perfil de cumprimento coletivo ainda não ficou disponível nesta amostra.");
        }
        if (execucao == null) {
            guardrails.add("Malha executiva do processo não respondeu com dados suficientes nesta amostra.");
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("relevante=" + relevantePosColetiva);
        fundamentos.add("coisaJulgada=" + coisaJulgadaColetivaAtiva);
        fundamentos.add("liquidacao=" + liquidacaoColetivaAtiva);
        fundamentos.add("habilitacao=" + habilitacaoIndividualAtiva);
        fundamentos.add("cumprimentoLotes=" + cumprimentoPulverizadoLotesAtivo);
        if (perfilTransito != null) {
            fundamentos.add("transito.scope=" + perfilTransito.resJudicataScope());
            fundamentos.add("transito.track=" + perfilTransito.executionTrack());
        }
        if (perfilCumprimento != null) {
            fundamentos.add("cumprimento.mode=" + perfilCumprimento.operationMode());
            fundamentos.add("cumprimento.track=" + perfilCumprimento.executionTrack());
        }
        if (documental != null) {
            fundamentos.add("documental.lotes=" + documental.lotes());
            fundamentos.add("documental.publicados=" + documental.publicados());
            fundamentos.add("documental.minutas=" + documental.minutas());
        }
        if (execucao != null) {
            fundamentos.add("execucao.trilhas=" + execucao.totalTrilhas());
            fundamentos.add("execucao.bloqueantes=" + execucao.totalBloqueantes());
            fundamentos.add("execucao.mandados=" + execucao.totalMandados());
        }

        return new PosColetivoSnapshot(
                processo.getId(),
                numeroReferencia(processo),
                resolverCodigoTribunal(processo),
                competencia,
                relevantePosColetiva,
                coisaJulgadaColetivaAtiva,
                liquidacaoColetivaAtiva,
                habilitacaoIndividualAtiva,
                cumprimentoPulverizadoLotesAtivo,
                scoreCoisaJulgadaColetiva,
                scoreLiquidacaoColetiva,
                scoreHabilitacaoIndividual,
                scoreCumprimentoPulverizadoLotes,
                List.copyOf(guardrails),
                List.copyOf(fundamentos)
        );
    }

    private List<Processo> carregarProcessosRecentes(int limit) {
        Sort sort = Sort.by(Sort.Order.desc("id"));
        return processoRepository.findAll(PageRequest.of(0, Math.max(1, limit), sort)).getContent();
    }

    private ProcessoDocumentoAggregate safeDocumental(Long processoId) {
        try {
            return processoDocumentoApplicationService.detalhar(processoId);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private ProcessoExecucaoAggregate safeExecucao(Long processoId) {
        try {
            return processoExecucaoApplicationService.detalhar(processoId);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private PostJudgmentOperationalProfile safePostJudgment(Processo processo,
                                                            ProcessoLifecycleAction action,
                                                            String qualifier) {
        try {
            return postJudgmentOperationalResolver.resolve(processo, action, qualifier, valorBase(processo));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private CompetenciaKey resolverCompetencia(Processo processo) {
        String ramoCodigo = processo.getRamoDireito() != null ? processo.getRamoDireito().name() : "NAO_CLASSIFICADO";
        RamoDireito ramo = RamoDireito.fromString(ramoCodigo);
        String ramoNome = ramo != null ? ramo.getDescricao() : ramoCodigo;
        String ritoCodigo = processo.getRito() != null ? processo.getRito().name() : "NAO_CLASSIFICADO";
        return new CompetenciaKey(ramoCodigo, ramoNome, ritoCodigo);
    }

    private String resolverCodigoTribunal(Processo processo) {
        return firstNonBlank(processo.getTribunal(), "TRIBUNAL_NACIONAL");
    }

    private String numeroReferencia(Processo processo) {
        return firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero());
    }

    private boolean isRelevantPosColetiva(Processo processo,
                                          PjbSubstituicaoFederativaTutelaColetivaCompetencia baseline) {
        String corpus = normalize(String.join(" ", List.of(
                Objects.toString(processo.getClasseProcessual(), ""),
                Objects.toString(processo.getAssunto(), ""),
                Objects.toString(processo.getObjetoProcessual(), ""),
                processo.getRito() != null ? processo.getRito().name() : "",
                processo.getFaseAtual() != null ? processo.getFaseAtual().name() : ""
        )));
        return baseline != null
                || containsAny(corpus,
                        "acao civil publica",
                        "acp",
                        "coletiv",
                        "demanda estrutural",
                        "execucao coletiva",
                        "cumprimento coletivo",
                        "beneficiari",
                        "substituid",
                        "liquidacao coletiva",
                        "habilitacao individual");
    }

    private boolean isCoisaJulgadaColetivaAtiva(Processo processo,
                                                ProcessoDocumentoAggregate documental,
                                                PostJudgmentOperationalProfile perfilTransito) {
        String corpus = normalize(buildCorpus(processo));
        boolean meritoPublicado = documental != null && documental.grupos().stream()
                .anyMatch(lote -> "MERITO".equalsIgnoreCase(lote.eixoDocumental()) && "PUBLICADO".equalsIgnoreCase(lote.ultimaVersaoEstado()));
        boolean scopeColetivo = perfilTransito != null && containsAny(normalize(perfilTransito.resJudicataScope()), "coletiv", "erga omnes", "ultra partes");
        return containsAny(corpus, "coisa julgada", "transito em julgado", "titulo coletivo", "acordao coletivo", "sentenca coletiva")
                || meritoPublicado
                || scopeColetivo;
    }

    private boolean isLiquidacaoColetivaAtiva(Processo processo,
                                              ProcessoDocumentoAggregate documental,
                                              PostJudgmentOperationalProfile perfilCumprimento,
                                              ProcessoExecucaoAggregate execucao) {
        String corpus = normalize(buildCorpus(processo));
        boolean documentalLiquida = documental != null && documental.grupos().stream().anyMatch(this::isLoteLiquidacao);
        boolean perfilLiquida = perfilCumprimento != null && containsAny(normalize(String.join(" ", List.of(
                Objects.toString(perfilCumprimento.executionTrack(), ""),
                Objects.toString(perfilCumprimento.operationMode(), ""),
                Objects.toString(perfilCumprimento.reviewDesk(), "")
        ))), "liquid", "contadoria", "calculo", "cumprimento");
        boolean execucaoLiquida = execucao != null && execucao.trilhas().stream().anyMatch(this::isTrilhaLiquidacao);
        return containsAny(corpus, "liquidacao", "calculo", "contadoria", "cumprimento de sentenca", "cumprimento sentença")
                || documentalLiquida
                || perfilLiquida
                || execucaoLiquida;
    }

    private boolean isHabilitacaoIndividualAtiva(Processo processo,
                                                 ProcessoDocumentoAggregate documental,
                                                 PostJudgmentOperationalProfile perfilCumprimento) {
        String corpus = normalize(buildCorpus(processo));
        boolean documentalHabilita = documental != null && documental.grupos().stream().anyMatch(this::isLoteHabilitacao);
        boolean perfilHabilita = perfilCumprimento != null && containsAny(normalize(String.join(" ", List.of(
                Objects.toString(perfilCumprimento.operationMode(), ""),
                String.join(" ", perfilCumprimento.reviewChecklist())
        ))), "habilit", "beneficiari", "individual", "lista");
        return containsAny(corpus,
                "habilit",
                "beneficiari",
                "substituid",
                "cumprimento individual",
                "liquidacao individual",
                "execucao individual",
                "lista de credores")
                || documentalHabilita
                || perfilHabilita;
    }

    private boolean isCumprimentoPulverizadoLotesAtivo(Processo processo,
                                                       ProcessoDocumentoAggregate documental,
                                                       PostJudgmentOperationalProfile perfilCumprimento,
                                                       ProcessoExecucaoAggregate execucao) {
        String corpus = normalize(buildCorpus(processo));
        boolean lotesDocumentais = documental != null && documental.lotes() >= 3 && documental.publicados() >= 1;
        boolean lotesComTitulos = documental != null && documental.grupos().stream().anyMatch(this::isLoteCumprimentoLote);
        boolean perfilLote = perfilCumprimento != null && containsAny(normalize(String.join(" ", List.of(
                Objects.toString(perfilCumprimento.operationMode(), ""),
                Objects.toString(perfilCumprimento.executionTrack(), ""),
                String.join(" ", perfilCumprimento.reviewChecklist())
        ))), "lote", "massa", "beneficiari", "cumprimento", "execucao");
        boolean execucaoLote = execucao != null && (execucao.totalTrilhas() >= 3 || execucao.totalMandados() >= 2);
        return containsAny(corpus,
                "cumprimento em massa",
                "cumprimento pulverizado",
                "por lotes",
                "lista de beneficiarios",
                "beneficiarios",
                "execucao pulverizada")
                || (lotesDocumentais && perfilLote)
                || lotesComTitulos
                || execucaoLote;
    }

    private int scoreCoisaJulgadaColetiva(Processo processo,
                                          int scoreBase,
                                          ProcessoDocumentoAggregate documental,
                                          PostJudgmentOperationalProfile perfilTransito,
                                          boolean ativa) {
        int score = clamp((int) Math.round(scoreBase * 0.45d));
        if (ativa) {
            score += 22;
        }
        if (perfilTransito != null && perfilTransito.resJudicataScope() != null && !perfilTransito.resJudicataScope().isBlank()) {
            score += 14;
        }
        if (documental != null && documental.publicados() > 0) {
            score += 12;
        }
        if (containsAny(normalize(buildCorpus(processo)), "transito", "coisa julgada", "sentenca coletiva")) {
            score += 10;
        }
        return clamp(score);
    }

    private int scoreLiquidacaoColetiva(Processo processo,
                                        int scoreBase,
                                        ProcessoDocumentoAggregate documental,
                                        PostJudgmentOperationalProfile perfilCumprimento,
                                        ProcessoExecucaoAggregate execucao,
                                        boolean ativa) {
        int score = clamp((int) Math.round(scoreBase * 0.40d));
        if (ativa) {
            score += 20;
        }
        if (documental != null && documental.grupos().stream().anyMatch(this::isLoteLiquidacao)) {
            score += 16;
        }
        if (perfilCumprimento != null && perfilCumprimento.reviewChecklist().size() >= 2) {
            score += 10;
        }
        if (execucao != null && execucao.totalTrilhas() >= 2) {
            score += 10;
        }
        if (containsAny(normalize(buildCorpus(processo)), "liquidacao", "contadoria", "calculo")) {
            score += 8;
        }
        return clamp(score);
    }

    private int scoreHabilitacaoIndividual(Processo processo,
                                           int scoreBase,
                                           ProcessoDocumentoAggregate documental,
                                           PostJudgmentOperationalProfile perfilCumprimento,
                                           boolean ativa) {
        int score = clamp((int) Math.round(scoreBase * 0.38d));
        if (ativa) {
            score += 22;
        }
        if (documental != null && documental.grupos().stream().anyMatch(this::isLoteHabilitacao)) {
            score += 16;
        }
        if (perfilCumprimento != null && containsAny(normalize(String.join(" ", perfilCumprimento.reviewChecklist())), "habilit", "beneficiari", "individual")) {
            score += 12;
        }
        if (containsAny(normalize(buildCorpus(processo)), "habilit", "beneficiari", "substituid", "individual")) {
            score += 10;
        }
        return clamp(score);
    }

    private int scoreCumprimentoPulverizadoLotes(Processo processo,
                                                 int scoreBase,
                                                 ProcessoDocumentoAggregate documental,
                                                 PostJudgmentOperationalProfile perfilCumprimento,
                                                 ProcessoExecucaoAggregate execucao,
                                                 boolean ativa) {
        int score = clamp((int) Math.round(scoreBase * 0.40d));
        if (ativa) {
            score += 20;
        }
        if (documental != null && documental.lotes() >= 3) {
            score += 14;
        }
        if (perfilCumprimento != null && containsAny(normalize(Objects.toString(perfilCumprimento.executionTrack(), "")), "exec", "cumpr")) {
            score += 10;
        }
        if (execucao != null && (execucao.totalMandados() >= 2 || execucao.trilhas().stream().anyMatch(this::isTrilhaLote))) {
            score += 12;
        }
        if (containsAny(normalize(buildCorpus(processo)), "lote", "massa", "beneficiari", "cumprimento")) {
            score += 8;
        }
        return clamp(score);
    }

    private boolean isLoteLiquidacao(ProcessoDocumentoLote lote) {
        String corpus = normalize(String.join(" ", List.of(
                Objects.toString(lote.tituloBase(), ""),
                Objects.toString(lote.eixoDocumental(), ""),
                Objects.toString(lote.papelAssinante(), "")
        )));
        return containsAny(corpus, "liquid", "calculo", "contadoria", "memoria", "planilha");
    }

    private boolean isLoteHabilitacao(ProcessoDocumentoLote lote) {
        String corpus = normalize(String.join(" ", List.of(
                Objects.toString(lote.tituloBase(), ""),
                Objects.toString(lote.eixoDocumental(), "")
        )));
        return containsAny(corpus, "habilit", "beneficiari", "substituid", "listagem", "cadastro");
    }

    private boolean isLoteCumprimentoLote(ProcessoDocumentoLote lote) {
        String corpus = normalize(String.join(" ", List.of(
                Objects.toString(lote.tituloBase(), ""),
                Objects.toString(lote.eixoDocumental(), "")
        )));
        return containsAny(corpus, "lote", "massa", "beneficiari", "cumprimento", "execucao");
    }

    private boolean isTrilhaLiquidacao(ProcessoExecucaoTrilha trilha) {
        String corpus = normalize(String.join(" ", List.of(
                Objects.toString(trilha.titulo(), ""),
                Objects.toString(trilha.impacto(), ""),
                String.join(" ", trilha.fundamentos())
        )));
        return containsAny(corpus, "liquid", "calculo", "contadoria", "saldo", "satisfacao");
    }

    private boolean isTrilhaLote(ProcessoExecucaoTrilha trilha) {
        String corpus = normalize(String.join(" ", List.of(
                Objects.toString(trilha.titulo(), ""),
                Objects.toString(trilha.impacto(), ""),
                String.join(" ", trilha.guardas())
        )));
        return containsAny(corpus, "lote", "massa", "beneficiari", "cumprimento", "execucao");
    }

    private String qualifierPosColetivo(Processo processo) {
        return String.join(" ", List.of(
                Objects.toString(processo.getClasseProcessual(), ""),
                Objects.toString(processo.getAssunto(), ""),
                Objects.toString(processo.getObjetoProcessual(), ""),
                Objects.toString(processo.getParteAutoraNome(), ""),
                Objects.toString(processo.getParteReuNome(), ""),
                processo.getRito() != null ? processo.getRito().name() : "",
                processo.getFaseAtual() != null ? processo.getFaseAtual().name() : ""
        ));
    }

    private String buildCorpus(Processo processo) {
        return String.join(" ", List.of(
                Objects.toString(processo.getClasseProcessual(), ""),
                Objects.toString(processo.getAssunto(), ""),
                Objects.toString(processo.getObjetoProcessual(), ""),
                Objects.toString(processo.getMaterialProbatorioResumo(), ""),
                Objects.toString(processo.getParteAutoraNome(), ""),
                Objects.toString(processo.getParteReuNome(), ""),
                processo.getRito() != null ? processo.getRito().name() : "",
                processo.getFaseAtual() != null ? processo.getFaseAtual().name() : ""
        ));
    }

    private double valorBase(Processo processo) {
        BigDecimal valorCausa = processo.getValorCausa();
        return valorCausa == null ? 0d : valorCausa.doubleValue();
    }

    private int scoreCompetencia(PjbSubstituicaoFederativaPosColetivaCompetencia competencia) {
        return clamp((int) Math.round((competencia.scoreCoisaJulgadaColetiva()
                + competencia.scoreLiquidacaoColetiva()
                + competencia.scoreHabilitacaoIndividual()
                + competencia.scoreCumprimentoPulverizadoLotes()) / 4.0d));
    }

    private int thingPercentage(List<PosColetivoSnapshot> snapshots,
                                java.util.function.Predicate<PosColetivoSnapshot> predicate) {
        return snapshots.isEmpty() ? 0 : clamp((int) Math.round((snapshots.stream().filter(predicate).count() * 100.0d) / snapshots.size()));
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private boolean containsAny(String corpus, String... tokens) {
        if (corpus == null || corpus.isBlank()) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && corpus.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record CompetenciaKey(String ramoCodigo, String ramoNome, String ritoCodigo) {
        String codigo() {
            return ramoCodigo + ':' + ritoCodigo;
        }
    }

    private record PosColetivoSnapshot(
            Long processoId,
            String numeroReferencia,
            String tribunalCodigo,
            CompetenciaKey competencia,
            boolean relevantePosColetiva,
            boolean coisaJulgadaColetivaAtiva,
            boolean liquidacaoColetivaAtiva,
            boolean habilitacaoIndividualAtiva,
            boolean cumprimentoPulverizadoLotesAtivo,
            int scoreCoisaJulgadaColetiva,
            int scoreLiquidacaoColetiva,
            int scoreHabilitacaoIndividual,
            int scoreCumprimentoPulverizadoLotes,
            List<String> guardrails,
            List<String> fundamentos
    ) {
        int scoreGeral() {
            return Math.max(0, Math.min(100, (int) Math.round((scoreCoisaJulgadaColetiva + scoreLiquidacaoColetiva + scoreHabilitacaoIndividual + scoreCumprimentoPulverizadoLotes) / 4.0d)));
        }
    }
}
