package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaMalhaJulgadoraAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaMalhaJulgadoraTribunal;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaMalhaJulgadoraUnidade;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaNucleoDuroAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaNucleoDuroTribunal;
import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoTrilha;
import com.tcc.pjb.backend.core.processo.prevencao.application.ProcessoPrevencaoApplicationService;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoPrevencaoAggregate;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculacaoAnaliseConsulta;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.enums.StatusJulgamentoColegiado;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.colegiado.NationalColegiadoEngine;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoRedistribuicaoService;
import java.time.Instant;
import java.util.ArrayList;
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
public class PjbSubstituicaoFederativaMalhaJulgadoraApplicationService {

    private static final int NATIONAL_SAMPLE_SIZE = 520;
    private static final int TRIBUNAL_SAMPLE_SIZE = 72;
    private static final int UNIDADE_SAMPLE_SIZE = 4;
    private static final int UNIDADES_LIMITE = 10;

    private final PjbSubstituicaoFederativaNucleoDuroApplicationService nucleoDuroApplicationService;
    private final ProcessoRepository processoRepository;
    private final ProcessoExecucaoApplicationService processoExecucaoApplicationService;
    private final ProcessoPrevencaoApplicationService processoPrevencaoApplicationService;
    private final ProcessoRecursalApplicationService processoRecursalApplicationService;
    private final FederalismoRedistribuicaoService federalismoRedistribuicaoService;
    private final NationalColegiadoEngine nationalColegiadoEngine;
    private final JulgamentoColegiadoRepository julgamentoColegiadoRepository;

    public PjbSubstituicaoFederativaMalhaJulgadoraApplicationService(
            PjbSubstituicaoFederativaNucleoDuroApplicationService nucleoDuroApplicationService,
            ProcessoRepository processoRepository,
            ProcessoExecucaoApplicationService processoExecucaoApplicationService,
            ProcessoPrevencaoApplicationService processoPrevencaoApplicationService,
            ProcessoRecursalApplicationService processoRecursalApplicationService,
            FederalismoRedistribuicaoService federalismoRedistribuicaoService,
            NationalColegiadoEngine nationalColegiadoEngine,
            JulgamentoColegiadoRepository julgamentoColegiadoRepository) {
        this.nucleoDuroApplicationService = Objects.requireNonNull(nucleoDuroApplicationService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoExecucaoApplicationService = Objects.requireNonNull(processoExecucaoApplicationService);
        this.processoPrevencaoApplicationService = Objects.requireNonNull(processoPrevencaoApplicationService);
        this.processoRecursalApplicationService = Objects.requireNonNull(processoRecursalApplicationService);
        this.federalismoRedistribuicaoService = Objects.requireNonNull(federalismoRedistribuicaoService);
        this.nationalColegiadoEngine = Objects.requireNonNull(nationalColegiadoEngine);
        this.julgamentoColegiadoRepository = Objects.requireNonNull(julgamentoColegiadoRepository);
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaMalhaJulgadoraAggregate avaliar() {
        PjbSubstituicaoFederativaNucleoDuroAggregate nucleo = nucleoDuroApplicationService.avaliar();
        Map<String, PjbSubstituicaoFederativaNucleoDuroTribunal> baseline = nucleo.tribunais().stream()
                .collect(Collectors.toMap(PjbSubstituicaoFederativaNucleoDuroTribunal::tribunalCodigo, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<Processo>> processosPorTribunal = carregarProcessosRecentes(NATIONAL_SAMPLE_SIZE).stream()
                .collect(Collectors.groupingBy(this::resolverCodigoTribunal, LinkedHashMap::new, Collectors.toList()));
        List<FederalismoRedistribuicaoService.VaraAnalise> redistribuicoes = federalismoRedistribuicaoService.sugerir(0.82d).varasCriticas();

        List<PjbSubstituicaoFederativaMalhaJulgadoraTribunal> tribunais = baseline.values().stream()
                .map(tribunal -> buildTribunal(tribunal, processosPorTribunal.getOrDefault(tribunal.tribunalCodigo(), List.of()), redistribuicoes))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaMalhaJulgadoraTribunal::malhaJulgadoraPronta).reversed()
                        .thenComparing(PjbSubstituicaoFederativaMalhaJulgadoraTribunal::scoreGeral, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaMalhaJulgadoraTribunal::tribunalCodigo))
                .toList();

        int tribunaisProntos = (int) tribunais.stream().filter(PjbSubstituicaoFederativaMalhaJulgadoraTribunal::malhaJulgadoraPronta).count();
        int scoreNacional = clamp((int) Math.round(tribunais.stream().mapToInt(PjbSubstituicaoFederativaMalhaJulgadoraTribunal::scoreGeral).average().orElse(0d)));
        boolean incidentesConectados = tribunais.stream().filter(item -> item.scoreIncidentes() >= 70).count() >= Math.max(3, tribunais.size() / 5);
        boolean colegiadosConectados = tribunais.stream().filter(item -> item.scoreColegiados() >= 70).count() >= Math.max(3, tribunais.size() / 5);
        boolean unidadesJulgadorasConectadas = tribunais.stream().filter(item -> item.scoreUnidadesJulgadoras() >= 68).count() >= Math.max(3, tribunais.size() / 5);
        boolean malhaJulgadoraPronta = tribunaisProntos >= Math.max(4, tribunais.size() / 6)
                && incidentesConectados
                && colegiadosConectados
                && unidadesJulgadorasConectadas;

        LinkedHashSet<String> bloqueadoresCriticos = new LinkedHashSet<>(nucleo.bloqueadoresCriticos());
        tribunais.stream().flatMap(tribunal -> tribunal.bloqueadores().stream()).limit(40).forEach(bloqueadoresCriticos::add);

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(nucleo.fundamentos());
        fundamentos.add("malhaJulgadora.scoreNacional=" + scoreNacional);
        fundamentos.add("malhaJulgadora.tribunaisProntos=" + tribunaisProntos);
        fundamentos.add("malhaJulgadora.incidentesConectados=" + incidentesConectados);
        fundamentos.add("malhaJulgadora.colegiadosConectados=" + colegiadosConectados);
        fundamentos.add("malhaJulgadora.unidadesJulgadorasConectadas=" + unidadesJulgadorasConectadas);
        fundamentos.add("malhaJulgadora.pronta=" + malhaJulgadoraPronta);

        return new PjbSubstituicaoFederativaMalhaJulgadoraAggregate(
                scoreNacional,
                malhaJulgadoraPronta,
                incidentesConectados,
                colegiadosConectados,
                unidadesJulgadorasConectadas,
                tribunaisProntos,
                tribunais,
                List.copyOf(bloqueadoresCriticos.stream().limit(60).toList()),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaMalhaJulgadoraTribunal avaliarTribunal(String tribunalCodigo) {
        PjbSubstituicaoFederativaNucleoDuroTribunal baseline = nucleoDuroApplicationService.avaliarTribunal(tribunalCodigo);
        List<Processo> processos = carregarProcessosRecentes(TRIBUNAL_SAMPLE_SIZE * 8).stream()
                .filter(processo -> baseline.tribunalCodigo().equals(resolverCodigoTribunal(processo)))
                .limit(TRIBUNAL_SAMPLE_SIZE)
                .toList();
        return buildTribunal(baseline, processos, federalismoRedistribuicaoService.sugerir(0.82d).varasCriticas());
    }

    private PjbSubstituicaoFederativaMalhaJulgadoraTribunal buildTribunal(
            PjbSubstituicaoFederativaNucleoDuroTribunal baseline,
            List<Processo> processos,
            List<FederalismoRedistribuicaoService.VaraAnalise> redistribuicoes) {
        Map<String, List<Processo>> processosPorUnidade = processos.stream()
                .collect(Collectors.groupingBy(this::resolverCodigoUnidade, LinkedHashMap::new, Collectors.toList()));
        List<PjbSubstituicaoFederativaMalhaJulgadoraUnidade> unidades = processosPorUnidade.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, List<Processo>>>comparingInt(entry -> entry.getValue().size()).reversed())
                .limit(UNIDADES_LIMITE)
                .map(entry -> buildUnidade(baseline.tribunalCodigo(), entry.getKey(), entry.getValue(), redistribuicoes))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaMalhaJulgadoraUnidade::malhaJulgadoraPronta).reversed()
                        .thenComparing(this::scoreUnidade, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaMalhaJulgadoraUnidade::unidadeCodigo))
                .toList();

        ColegiadoTribunalSnapshot colegiadoTribunal = avaliarColegiadoTribunal(baseline.tribunalCodigo());
        int scoreIncidentes = clamp((int) Math.round(unidades.stream().mapToInt(PjbSubstituicaoFederativaMalhaJulgadoraUnidade::scoreIncidentes).average().orElse(0d)));
        int scoreColegiados = clamp((int) Math.round((colegiadoTribunal.score() + unidades.stream().mapToInt(PjbSubstituicaoFederativaMalhaJulgadoraUnidade::scoreColegiado).average().orElse(0d)) / 2.0d));
        int scoreUnidadesJulgadoras = clamp((int) Math.round(unidades.stream().mapToInt(this::scoreUnidade).average().orElse(0d)));
        int scoreGeral = clamp((int) Math.round((baseline.scoreGeral() + scoreIncidentes + scoreColegiados + scoreUnidadesJulgadoras) / 4.0d));
        boolean pronta = baseline.prontoNucleoDuro()
                && scoreIncidentes >= 70
                && scoreColegiados >= 70
                && scoreUnidadesJulgadoras >= 68;

        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>(baseline.bloqueadores());
        if (scoreIncidentes < 70) {
            bloqueadores.add("MALHA_JULGADORA_INCIDENTES_INSUFICIENTES");
        }
        if (scoreColegiados < 70) {
            bloqueadores.add("MALHA_JULGADORA_COLEGIADOS_INSUFICIENTES");
        }
        if (scoreUnidadesJulgadoras < 68) {
            bloqueadores.add("MALHA_JULGADORA_UNIDADES_JULGADORAS_INSUFICIENTES");
        }
        if (unidades.isEmpty()) {
            bloqueadores.add("MALHA_JULGADORA_SEM_AMOSTRA_DE_UNIDADES");
        }

        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        if (pronta) {
            proximasAcoes.add("EXPANDIR_CORTE_POR_UNIDADE_JULGADORA_COM_MONITORAMENTO_DE_INCIDENTES");
            proximasAcoes.add("MANTER_COLEGIADO_E_RECURRENCIAS_RECURSAIS_INDEXADOS_POR_TRIBUNAL");
            proximasAcoes.add("PRESERVAR_PREVENCAO_E_REDISTRIBUICAO_GOVERNADA_EM_ONDAS_LOCAIS");
        } else {
            proximasAcoes.add("FECHAR_GAPS_DE_INCIDENTES_PROCESSUAIS_POR_UNIDADE");
            proximasAcoes.add("REDUZIR_PENDENCIAS_DE_COLEGIADO_E_PUBLICACAO_DE_ACORDAO");
            proximasAcoes.add("AMARRAR_PREVENCAO_E_REDISTRIBUICAO_A_UNIDADE_JULGADORA_REAL");
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(baseline.fundamentos());
        fundamentos.add("malhaJulgadora.scoreIncidentes=" + scoreIncidentes);
        fundamentos.add("malhaJulgadora.scoreColegiados=" + scoreColegiados);
        fundamentos.add("malhaJulgadora.scoreUnidades=" + scoreUnidadesJulgadoras);
        fundamentos.add("malhaJulgadora.painelColegiado.abertos=" + colegiadoTribunal.abertos());
        fundamentos.add("malhaJulgadora.painelColegiado.pendentesPublicacao=" + colegiadoTribunal.pendentesPublicacao());
        fundamentos.add("malhaJulgadora.pronta=" + pronta);

        return new PjbSubstituicaoFederativaMalhaJulgadoraTribunal(
                baseline.tribunalCodigo(),
                baseline.tribunalNome(),
                baseline.ramoJustica(),
                baseline.legadoPrincipal(),
                baseline.ondaAtual(),
                scoreGeral,
                scoreIncidentes,
                scoreColegiados,
                scoreUnidadesJulgadoras,
                baseline.prontoNucleoDuro(),
                pronta,
                unidades.size(),
                unidades,
                List.copyOf(bloqueadores),
                List.copyOf(proximasAcoes),
                List.copyOf(fundamentos)
        );
    }

    private PjbSubstituicaoFederativaMalhaJulgadoraUnidade buildUnidade(
            String tribunalCodigo,
            String unidadeCodigo,
            List<Processo> processos,
            List<FederalismoRedistribuicaoService.VaraAnalise> redistribuicoes) {
        List<Processo> amostra = processos.stream().limit(UNIDADE_SAMPLE_SIZE).toList();
        Processo processoReferencia = amostra.isEmpty() ? null : amostra.getFirst();
        IncidenteSnapshot incidentes = avaliarIncidentes(amostra);
        ColegiadoUnidadeSnapshot colegiado = avaliarColegiadoUnidade(tribunalCodigo, amostra);
        PrevencaoRedistribuicaoSnapshot prevencaoRedistribuicao = avaliarPrevencaoRedistribuicao(unidadeCodigo, amostra, redistribuicoes);
        int scoreGeral = clamp((int) Math.round((incidentes.score() + colegiado.score() + prevencaoRedistribuicao.score()) / 3.0d));
        boolean pronta = scoreGeral >= 70 && colegiado.ativo();

        LinkedHashSet<String> guardrails = new LinkedHashSet<>();
        guardrails.add("UNIDADE_JULGADORA_INDEXADA");
        guardrails.add("INCIDENTE_PROCESSUAL_MONITORADO");
        if (colegiado.ativo()) {
            guardrails.add("COLEGIADO_VINCULADO_A_PROCESSOS_DA_UNIDADE");
        }
        if (prevencaoRedistribuicao.unidadePreventa() != null && !prevencaoRedistribuicao.unidadePreventa().isBlank()) {
            guardrails.add("PREVENCAO_MAPEADA_PARA_UNIDADE_PREVENTA");
        }
        if (prevencaoRedistribuicao.possuiRedistribuicaoAssistida()) {
            guardrails.add("REDISTRIBUICAO_ASSISTIDA_INDEXADA");
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.addAll(incidentes.fundamentos());
        fundamentos.addAll(colegiado.fundamentos());
        fundamentos.addAll(prevencaoRedistribuicao.fundamentos());
        fundamentos.add("malhaJulgadora.unidade.score=" + scoreGeral);
        fundamentos.add("malhaJulgadora.unidade.processos=" + processos.size());
        if (processoReferencia != null) {
            fundamentos.add("malhaJulgadora.unidade.processoReferencia=" + numeroProcesso(processoReferencia));
        }

        return new PjbSubstituicaoFederativaMalhaJulgadoraUnidade(
                unidadeCodigo,
                resolverNomeUnidade(processoReferencia, unidadeCodigo),
                processoReferencia == null || processoReferencia.getRamoDireito() == null ? RamoDireito.CIVIL.name() : processoReferencia.getRamoDireito().name(),
                processoReferencia == null || processoReferencia.getRito() == null ? RitoProcessual.COMUM_ORDINARIO.name() : processoReferencia.getRito().name(),
                processos.size(),
                incidentes.score(),
                colegiado.score(),
                prevencaoRedistribuicao.score(),
                pronta,
                incidentes.ativo(),
                colegiado.ativo(),
                pronta ? "janela-unidade-controlada" : "janela-unidade-assistida",
                List.copyOf(guardrails),
                List.copyOf(fundamentos.stream().limit(30).toList()),
                processoReferencia == null ? null : processoReferencia.getId(),
                processoReferencia == null ? null : numeroProcesso(processoReferencia)
        );
    }

    private IncidenteSnapshot avaliarIncidentes(List<Processo> processos) {
        if (processos.isEmpty()) {
            return new IncidenteSnapshot(0, false, List.of("incidente.semAmostra=true"));
        }
        int score = 0;
        boolean ativo = false;
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        for (Processo processo : processos) {
            try {
                ProcessoExecucaoAggregate aggregate = processoExecucaoApplicationService.detalhar(processo.getId());
                int parcial = 35;
                parcial += Math.min(20, (int) aggregate.totalTrilhas() * 4);
                parcial += aggregate.trilhas().stream().anyMatch(this::isTrilhaIncidente) ? 20 : 0;
                parcial += aggregate.totalBloqueantes() == 0 ? 10 : 0;
                parcial += aggregate.processoExecutivo() ? 10 : 5;
                score += clamp(parcial);
                if (aggregate.trilhas().stream().anyMatch(this::isTrilhaIncidente)) {
                    ativo = true;
                }
                fundamentos.add("incidente.processo=" + numeroProcesso(processo) + ":trilhas=" + aggregate.totalTrilhas());
                fundamentos.add("incidente.processo=" + numeroProcesso(processo) + ":bloqueantes=" + aggregate.totalBloqueantes());
            } catch (RuntimeException ex) {
                score += 30;
                fundamentos.add("incidente.processo=" + numeroProcesso(processo) + ":fallback=" + ex.getClass().getSimpleName());
            }
        }
        return new IncidenteSnapshot(clamp((int) Math.round(score / (double) processos.size())), ativo, List.copyOf(fundamentos));
    }

    private ColegiadoTribunalSnapshot avaliarColegiadoTribunal(String tribunalCodigo) {
        Map<String, Object> painel = nationalColegiadoEngine.gerarPainelColegiado(tribunalCodigo);
        long totais = longValue(painel.get("julgamentosTotais"));
        long abertos = longValue(painel.get("abertos"));
        long pendentesPublicacao = longValue(painel.get("pendentesPublicacaoAcordao"));
        long urgentesPendentes = longValue(painel.get("urgentesPendentes"));
        long temas = longValue(painel.get("temasRepetitivos"));
        int score = 46;
        score += totais > 0 ? 10 : 0;
        score += Math.min(12, (int) temas * 2);
        score += Math.min(14, (int) abertos * 2);
        score -= Math.min(20, (int) pendentesPublicacao * 3);
        score -= Math.min(14, (int) urgentesPendentes * 2);
        return new ColegiadoTribunalSnapshot(clamp(score), abertos, pendentesPublicacao);
    }

    private ColegiadoUnidadeSnapshot avaliarColegiadoUnidade(String tribunalCodigo, List<Processo> processos) {
        if (processos.isEmpty()) {
            return new ColegiadoUnidadeSnapshot(0, false, List.of("colegiado.semAmostra=true"));
        }
        ArrayList<JulgamentoColegiado> julgamentos = new ArrayList<>();
        for (Processo processo : processos) {
            julgamentos.addAll(julgamentoColegiadoRepository.findByProcessoId(processo.getId()));
        }
        boolean ativo = julgamentos.stream().anyMatch(this::isColegiadoAtivo);
        boolean publicado = julgamentos.stream().anyMatch(j -> Boolean.TRUE.equals(j.getAcordaoPublicado()));
        int score = 40;
        score += julgamentos.isEmpty() ? 0 : Math.min(18, julgamentos.size() * 4);
        score += ativo ? 18 : 0;
        score += publicado ? 12 : 0;
        score += avaliarRecursal(processos).score();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("colegiado.tribunal=" + normalize(tribunalCodigo));
        fundamentos.add("colegiado.julgamentos=" + julgamentos.size());
        fundamentos.add("colegiado.ativo=" + ativo);
        fundamentos.add("colegiado.publicado=" + publicado);
        return new ColegiadoUnidadeSnapshot(clamp(score), ativo || !julgamentos.isEmpty(), List.copyOf(fundamentos));
    }

    private RecursalSnapshot avaliarRecursal(List<Processo> processos) {
        if (processos.isEmpty()) {
            return new RecursalSnapshot(0, List.of("recursal.semAmostra=true"));
        }
        int score = 0;
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        for (Processo processo : processos) {
            try {
                ProcessoRecursalAggregate aggregate = processoRecursalApplicationService.detalhar(processo.getId());
                int parcial = 0;
                parcial += aggregate.totalCabiveis() > 0 ? 10 : 0;
                parcial += aggregate.janelas().stream().anyMatch(item -> item.julgamentoColegiado() && item.cabivel()) ? 10 : 0;
                parcial += aggregate.travas().isEmpty() ? 8 : 2;
                parcial += aggregate.totalExterno() > 0 ? 6 : 0;
                score += parcial;
                fundamentos.add("recursal.processo=" + numeroProcesso(processo) + ":cabiveis=" + aggregate.totalCabiveis());
                fundamentos.add("recursal.processo=" + numeroProcesso(processo) + ":travas=" + aggregate.travas().size());
            } catch (RuntimeException ex) {
                fundamentos.add("recursal.processo=" + numeroProcesso(processo) + ":fallback=" + ex.getClass().getSimpleName());
            }
        }
        return new RecursalSnapshot(Math.min(20, clamp((int) Math.round(score / (double) processos.size()))), List.copyOf(fundamentos));
    }

    private PrevencaoRedistribuicaoSnapshot avaliarPrevencaoRedistribuicao(
            String unidadeCodigo,
            List<Processo> processos,
            List<FederalismoRedistribuicaoService.VaraAnalise> redistribuicoes) {
        if (processos.isEmpty()) {
            return new PrevencaoRedistribuicaoSnapshot(0, null, false, List.of("prevencaoRedistribuicao.semAmostra=true"));
        }
        int prevencaoScore = 0;
        String unidadePreventa = null;
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        for (Processo processo : processos) {
            try {
                ProcessoPrevencaoAggregate aggregate = processoPrevencaoApplicationService.analisar(new ProcessoVinculacaoAnaliseConsulta(
                        processo.getId(),
                        numeroProcesso(processo),
                        "malha-julgadora",
                        "substituicao-federativa"
                ));
                prevencaoScore += aggregate.temPrevencao() ? 74 : 46;
                if (unidadePreventa == null && aggregate.unidadePreventa() != null && !aggregate.unidadePreventa().isBlank()) {
                    unidadePreventa = aggregate.unidadePreventa();
                }
                fundamentos.add("prevencao.processo=" + numeroProcesso(processo) + ":temPrevencao=" + aggregate.temPrevencao());
            } catch (RuntimeException ex) {
                prevencaoScore += 42;
                fundamentos.add("prevencao.processo=" + numeroProcesso(processo) + ":fallback=" + ex.getClass().getSimpleName());
            }
        }
        boolean redistribuicaoAssistida = redistribuicoes.stream().anyMatch(item -> matchesRedistribuicao(item, unidadeCodigo, processos));
        int redistribuicaoScore = redistribuicaoAssistida ? 76 : 58;
        fundamentos.add("redistribuicao.assistida=" + redistribuicaoAssistida);
        if (unidadePreventa != null) {
            fundamentos.add("prevencao.unidadePreventa=" + unidadePreventa);
        }
        return new PrevencaoRedistribuicaoSnapshot(
                clamp((int) Math.round((prevencaoScore / (double) processos.size() + redistribuicaoScore) / 2.0d)),
                unidadePreventa,
                redistribuicaoAssistida,
                List.copyOf(fundamentos)
        );
    }

    private boolean isTrilhaIncidente(ProcessoExecucaoTrilha trilha) {
        return normalize(trilha.eixo()).contains("INCIDENTE") || normalize(trilha.codigo()).contains("INCIDENTE");
    }

    private boolean isColegiadoAtivo(JulgamentoColegiado julgamento) {
        return julgamento != null && julgamento.getStatus() != null && julgamento.getStatus() != StatusJulgamentoColegiado.ENCERRADO;
    }

    private boolean matchesRedistribuicao(FederalismoRedistribuicaoService.VaraAnalise analise, String unidadeCodigo, List<Processo> processos) {
        if (analise == null) {
            return false;
        }
        String unidade = normalize(unidadeCodigo);
        if (!unidade.isBlank()) {
            if (normalize(analise.sigla()).contains(unidade) || normalize(analise.nome()).contains(unidade)) {
                return true;
            }
        }
        return processos.stream().anyMatch(processo -> normalize(analise.comarca()).equals(normalize(processo.getComarca())));
    }

    private List<Processo> carregarProcessosRecentes(int size) {
        return processoRepository.findAll(PageRequest.of(0, Math.max(1, size), Sort.by(Sort.Order.desc("id")))).getContent();
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

    private String resolverCodigoUnidade(Processo processo) {
        String codigo = normalize(processo.getUnidadeJudiciariaCodigo());
        if (!codigo.isBlank()) {
            return codigo;
        }
        String vara = normalize(processo.getVara());
        if (!vara.isBlank()) {
            return vara;
        }
        return resolverCodigoTribunal(processo) + ':' + Optional.ofNullable(processo.getRamoDireito()).orElse(RamoDireito.CIVIL).name();
    }

    private String resolverNomeUnidade(Processo processo, String unidadeCodigo) {
        if (processo == null) {
            return unidadeCodigo;
        }
        String vara = Objects.toString(processo.getVara(), "").trim();
        if (!vara.isBlank()) {
            return vara;
        }
        return unidadeCodigo;
    }

    private int scoreUnidade(PjbSubstituicaoFederativaMalhaJulgadoraUnidade unidade) {
        return clamp((int) Math.round((unidade.scoreIncidentes() + unidade.scoreColegiado() + unidade.scorePrevencaoRedistribuicao()) / 3.0d));
    }

    private String numeroProcesso(Processo processo) {
        String numero = Objects.toString(processo.getNumeroUnificado(), "").trim();
        if (!numero.isBlank()) {
            return numero;
        }
        return Objects.toString(processo.getNumeroProcesso(), "").trim();
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = Objects.toString(value, "").trim();
        if (text.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(text.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String normalize(String value) {
        return Objects.toString(value, "").trim().toUpperCase(Locale.ROOT);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record IncidenteSnapshot(int score, boolean ativo, List<String> fundamentos) {
    }

    private record ColegiadoTribunalSnapshot(int score, long abertos, long pendentesPublicacao) {
    }

    private record ColegiadoUnidadeSnapshot(int score, boolean ativo, List<String> fundamentos) {
    }

    private record RecursalSnapshot(int score, List<String> fundamentos) {
    }

    private record PrevencaoRedistribuicaoSnapshot(int score,
                                                   String unidadePreventa,
                                                   boolean possuiRedistribuicaoAssistida,
                                                   List<String> fundamentos) {
    }
}
