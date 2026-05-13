package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaMalhaJulgadoraAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaMalhaJulgadoraTribunal;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPrecedentesQualificadosAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPrecedentesQualificadosTribunal;
import com.tcc.pjb.backend.core.processo.painel.application.ProcessoPainelFonteOficialApplicationService;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelFonteOficialAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.judicial.TemaPrecedenteVinculante;
import com.tcc.pjb.backend.model.entity.judicial.TemaRecursoRepetitivo;
import com.tcc.pjb.backend.model.entity.judicial.TemaRepercussaoGeral;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.TemaPrecedenteVinculanteRepository;
import com.tcc.pjb.backend.model.repository.TemaRecursoRepetitivoRepository;
import com.tcc.pjb.backend.model.repository.TemaRepercussaoGeralRepository;
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
public class PjbSubstituicaoFederativaPrecedentesQualificadosApplicationService {

    private static final int NATIONAL_SAMPLE_SIZE = 640;
    private static final int TRIBUNAL_SAMPLE_SIZE = 104;
    private static final int COMPETENCIAS_LIMITE = 10;
    private static final int COMPETENCIA_SAMPLE_SIZE = 5;

    private final PjbSubstituicaoFederativaMalhaJulgadoraApplicationService malhaJulgadoraApplicationService;
    private final ProcessoPainelFonteOficialApplicationService processoPainelFonteOficialApplicationService;
    private final ProcessoRepository processoRepository;
    private final TemaRecursoRepetitivoRepository temaRecursoRepetitivoRepository;
    private final TemaRepercussaoGeralRepository temaRepercussaoGeralRepository;
    private final TemaPrecedenteVinculanteRepository temaPrecedenteVinculanteRepository;

    public PjbSubstituicaoFederativaPrecedentesQualificadosApplicationService(
            PjbSubstituicaoFederativaMalhaJulgadoraApplicationService malhaJulgadoraApplicationService,
            ProcessoPainelFonteOficialApplicationService processoPainelFonteOficialApplicationService,
            ProcessoRepository processoRepository,
            TemaRecursoRepetitivoRepository temaRecursoRepetitivoRepository,
            TemaRepercussaoGeralRepository temaRepercussaoGeralRepository,
            TemaPrecedenteVinculanteRepository temaPrecedenteVinculanteRepository) {
        this.malhaJulgadoraApplicationService = Objects.requireNonNull(malhaJulgadoraApplicationService);
        this.processoPainelFonteOficialApplicationService = Objects.requireNonNull(processoPainelFonteOficialApplicationService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.temaRecursoRepetitivoRepository = Objects.requireNonNull(temaRecursoRepetitivoRepository);
        this.temaRepercussaoGeralRepository = Objects.requireNonNull(temaRepercussaoGeralRepository);
        this.temaPrecedenteVinculanteRepository = Objects.requireNonNull(temaPrecedenteVinculanteRepository);
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaPrecedentesQualificadosAggregate avaliar() {
        PjbSubstituicaoFederativaMalhaJulgadoraAggregate malhaJulgadora = malhaJulgadoraApplicationService.avaliar();
        Map<String, PjbSubstituicaoFederativaMalhaJulgadoraTribunal> baseline = malhaJulgadora.tribunais().stream()
                .collect(Collectors.toMap(PjbSubstituicaoFederativaMalhaJulgadoraTribunal::tribunalCodigo, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<Processo>> processosPorTribunal = carregarProcessosRecentes(NATIONAL_SAMPLE_SIZE).stream()
                .collect(Collectors.groupingBy(this::resolverCodigoTribunal, LinkedHashMap::new, Collectors.toList()));
        TemaPool temaPool = carregarTemas();

        List<PjbSubstituicaoFederativaPrecedentesQualificadosTribunal> tribunais = baseline.values().stream()
                .map(tribunal -> buildTribunal(tribunal, processosPorTribunal.getOrDefault(tribunal.tribunalCodigo(), List.of()), temaPool))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaPrecedentesQualificadosTribunal::malhaPrecedentesPronta).reversed()
                        .thenComparing(PjbSubstituicaoFederativaPrecedentesQualificadosTribunal::scoreGeral, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaPrecedentesQualificadosTribunal::tribunalCodigo))
                .toList();

        int tribunaisProntos = (int) tribunais.stream().filter(PjbSubstituicaoFederativaPrecedentesQualificadosTribunal::malhaPrecedentesPronta).count();
        int scoreNacional = clamp((int) Math.round(tribunais.stream().mapToInt(PjbSubstituicaoFederativaPrecedentesQualificadosTribunal::scoreGeral).average().orElse(0d)));
        boolean incidentesMassaConectados = tribunais.stream().filter(item -> item.scoreIncidentesMassa() >= 70).count() >= Math.max(3, tribunais.size() / 5);
        boolean temasAfetadosGovernados = tribunais.stream().filter(item -> item.scoreTemasAfetados() >= 70).count() >= Math.max(3, tribunais.size() / 5);
        boolean sobrestamentoGovernado = tribunais.stream().filter(item -> item.scoreSobrestamento() >= 70).count() >= Math.max(3, tribunais.size() / 5);
        boolean precedentesVinculantesConectados = tribunais.stream().filter(item -> item.scorePrecedentesVinculantes() >= 70).count() >= Math.max(3, tribunais.size() / 5);
        boolean malhaPrecedentesPronta = tribunaisProntos >= Math.max(4, tribunais.size() / 6)
                && incidentesMassaConectados
                && temasAfetadosGovernados
                && sobrestamentoGovernado
                && precedentesVinculantesConectados;

        LinkedHashSet<String> bloqueadoresCriticos = new LinkedHashSet<>(malhaJulgadora.bloqueadoresCriticos());
        tribunais.stream().flatMap(tribunal -> tribunal.bloqueadores().stream()).limit(40).forEach(bloqueadoresCriticos::add);

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(malhaJulgadora.fundamentos());
        fundamentos.add("precedentesQualificados.scoreNacional=" + scoreNacional);
        fundamentos.add("precedentesQualificados.tribunaisProntos=" + tribunaisProntos);
        fundamentos.add("precedentesQualificados.incidentesMassaConectados=" + incidentesMassaConectados);
        fundamentos.add("precedentesQualificados.temasAfetadosGovernados=" + temasAfetadosGovernados);
        fundamentos.add("precedentesQualificados.sobrestamentoGovernado=" + sobrestamentoGovernado);
        fundamentos.add("precedentesQualificados.precedentesVinculantesConectados=" + precedentesVinculantesConectados);
        fundamentos.add("precedentesQualificados.malhaPronta=" + malhaPrecedentesPronta);
        fundamentos.add("precedentesQualificados.repetitivosCatalogados=" + temaPool.repetitivos().size());
        fundamentos.add("precedentesQualificados.repercussoesCatalogadas=" + temaPool.repercussoes().size());
        fundamentos.add("precedentesQualificados.vinculantesCatalogados=" + temaPool.vinculantes().size());

        return new PjbSubstituicaoFederativaPrecedentesQualificadosAggregate(
                scoreNacional,
                malhaPrecedentesPronta,
                incidentesMassaConectados,
                temasAfetadosGovernados,
                sobrestamentoGovernado,
                precedentesVinculantesConectados,
                tribunaisProntos,
                tribunais,
                List.copyOf(bloqueadoresCriticos.stream().limit(60).toList()),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaPrecedentesQualificadosTribunal avaliarTribunal(String tribunalCodigo) {
        PjbSubstituicaoFederativaMalhaJulgadoraTribunal baseline = malhaJulgadoraApplicationService.avaliarTribunal(tribunalCodigo);
        List<Processo> processos = carregarProcessosRecentes(TRIBUNAL_SAMPLE_SIZE * 8).stream()
                .filter(processo -> baseline.tribunalCodigo().equals(resolverCodigoTribunal(processo)))
                .limit(TRIBUNAL_SAMPLE_SIZE)
                .toList();
        return buildTribunal(baseline, processos, carregarTemas());
    }

    private PjbSubstituicaoFederativaPrecedentesQualificadosTribunal buildTribunal(
            PjbSubstituicaoFederativaMalhaJulgadoraTribunal baseline,
            List<Processo> processos,
            TemaPool temaPool) {
        Map<CompetenciaKey, List<Processo>> processosPorCompetencia = processos.stream()
                .collect(Collectors.groupingBy(this::resolverCompetencia, LinkedHashMap::new, Collectors.toList()));
        List<PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia> competencias = processosPorCompetencia.entrySet().stream()
                .sorted(Map.Entry.<CompetenciaKey, List<Processo>>comparingByValue(Comparator.comparingInt(List::size)).reversed())
                .limit(COMPETENCIAS_LIMITE)
                .map(entry -> buildCompetencia(baseline.tribunalCodigo(), entry.getKey(), entry.getValue(), temaPool))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia::malhaPrecedentesPronta).reversed()
                        .thenComparing(this::scoreCompetencia, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia::competenciaCodigo))
                .toList();

        int scoreIncidentes = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia::scoreIncidentesMassa).average().orElse(0d)));
        int scoreAfetacao = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia::scoreAfetacao).average().orElse(0d)));
        int scoreSobrestamento = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia::scoreSobrestamento).average().orElse(0d)));
        int scorePrecedentes = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia::scorePrecedentesVinculantes).average().orElse(0d)));
        int scoreGeral = clamp((int) Math.round((scoreIncidentes + scoreAfetacao + scoreSobrestamento + scorePrecedentes) / 4.0d));
        boolean pronta = baseline.malhaJulgadoraPronta()
                && scoreIncidentes >= 68
                && scoreAfetacao >= 68
                && scoreSobrestamento >= 68
                && scorePrecedentes >= 70
                && competencias.stream().anyMatch(PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia::malhaPrecedentesPronta);

        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>(baseline.bloqueadores());
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(baseline.fundamentos());
        fundamentos.add("precedentesQualificados.tribunal.scoreGeral=" + scoreGeral);
        fundamentos.add("precedentesQualificados.tribunal.scoreIncidentesMassa=" + scoreIncidentes);
        fundamentos.add("precedentesQualificados.tribunal.scoreAfetacao=" + scoreAfetacao);
        fundamentos.add("precedentesQualificados.tribunal.scoreSobrestamento=" + scoreSobrestamento);
        fundamentos.add("precedentesQualificados.tribunal.scorePrecedentes=" + scorePrecedentes);
        fundamentos.add("precedentesQualificados.tribunal.competencias=" + competencias.size());

        if (scoreIncidentes < 68) {
            bloqueadores.add("Radar de incidentes de massa e litigância repetitiva ainda insuficiente no tribunal.");
            proximasAcoes.add("Conectar demanda repetitiva, classe processual e painel oficial para abertura assistida de IRDR/IAC.");
        }
        if (scoreAfetacao < 68) {
            bloqueadores.add("Afetação qualificada ainda dispersa entre leading cases e recursos representativos.");
            proximasAcoes.add("Indexar afetação por competência material e rito com fila específica por tribunal.");
        }
        if (scoreSobrestamento < 68) {
            bloqueadores.add("Sobrestamento ainda não está governado ponta a ponta em todas as competências.");
            proximasAcoes.add("Vincular rotinas de sobrestamento e retomada às janelas do war room por tribunal.");
        }
        if (scorePrecedentes < 70) {
            bloqueadores.add("Aplicação de precedentes vinculantes ainda não está estabilizada no tribunal.");
            proximasAcoes.add("Cruzar temas qualificados, aplicação automatizada e processo de referência em cada competência.");
        }
        if (!pronta) {
            proximasAcoes.add("Manter operação assistida da malha de precedentes até convergir em afetação, sobrestamento e precedente vinculante.");
        }
        competencias.stream().flatMap(item -> item.fundamentos().stream()).limit(18).forEach(fundamentos::add);

        return new PjbSubstituicaoFederativaPrecedentesQualificadosTribunal(
                baseline.tribunalCodigo(),
                baseline.tribunalNome(),
                baseline.ramoJustica(),
                baseline.legadoPrincipal(),
                baseline.ondaAtual(),
                scoreGeral,
                scoreIncidentes,
                scoreAfetacao,
                scoreSobrestamento,
                scorePrecedentes,
                baseline.malhaJulgadoraPronta(),
                pronta,
                competencias.size(),
                competencias,
                List.copyOf(bloqueadores.stream().limit(24).toList()),
                List.copyOf(proximasAcoes.stream().limit(16).toList()),
                List.copyOf(fundamentos.stream().limit(40).toList())
        );
    }

    private PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia buildCompetencia(
            String tribunalCodigo,
            CompetenciaKey competenciaKey,
            List<Processo> processos,
            TemaPool temaPool) {
        List<Processo> amostra = processos.stream().limit(COMPETENCIA_SAMPLE_SIZE).toList();
        Processo processoReferencia = amostra.isEmpty() ? null : amostra.getFirst();
        boolean painelDemandasRepetitivasAtivo = avaliarPainelDemandasRepetitivas(processoReferencia);
        IncidenteMassaSnapshot incidente = avaliarIncidenteMassa(amostra, painelDemandasRepetitivasAtivo);
        TemaCoverageSnapshot temas = avaliarTemas(tribunalCodigo, competenciaKey, processos, temaPool, painelDemandasRepetitivasAtivo);
        int scoreGeral = clamp((int) Math.round((incidente.score() + temas.scoreAfetacao() + temas.scoreSobrestamento() + temas.scorePrecedentes()) / 4.0d));
        boolean pronta = scoreGeral >= 72
                && incidente.ativo()
                && temas.afetacaoAtiva()
                && temas.sobrestamentoAtiva()
                && temas.precedenteAtivo();

        LinkedHashSet<String> guardrails = new LinkedHashSet<>();
        guardrails.add("AFETACAO_DEVE_REFERENCIAR_LEADING_CASE_OU_RECURSO_REPRESENTATIVO_VALIDO");
        guardrails.add("SOBRESTAMENTO_E_RETOMADA_DEVEM_SER_IDEMPOTENTES_POR_TEMA_E_COMPETENCIA");
        if (painelDemandasRepetitivasAtivo) {
            guardrails.add("PAINEL_DEMANDAS_REPETITIVAS_FONTE_OFICIAL_ATIVO");
        }
        if (temas.precedenteAtivo()) {
            guardrails.add("PRECEDENTE_QUALIFICADO_COM_PROCESSO_DE_REFERENCIA_E_APLICACAO_GUIADA");
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("precedentesQualificados.competencia.scoreGeral=" + scoreGeral);
        fundamentos.add("precedentesQualificados.competencia.scoreIncidentesMassa=" + incidente.score());
        fundamentos.add("precedentesQualificados.competencia.scoreAfetacao=" + temas.scoreAfetacao());
        fundamentos.add("precedentesQualificados.competencia.scoreSobrestamento=" + temas.scoreSobrestamento());
        fundamentos.add("precedentesQualificados.competencia.scorePrecedentes=" + temas.scorePrecedentes());
        fundamentos.addAll(incidente.fundamentos());
        fundamentos.addAll(temas.fundamentos());
        if (processoReferencia != null) {
            fundamentos.add("precedentesQualificados.competencia.referencia=" + numeroProcesso(processoReferencia));
        }

        return new PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia(
                competenciaKey.ramo().name() + ':' + competenciaKey.rito().name(),
                competenciaKey.ramo().name(),
                competenciaKey.ramo().getDescricao(),
                competenciaKey.rito().name(),
                processos.size(),
                incidente.score(),
                temas.scoreAfetacao(),
                temas.scoreSobrestamento(),
                temas.scorePrecedentes(),
                pronta,
                incidente.ativo(),
                temas.afetacaoAtiva(),
                temas.sobrestamentoAtiva(),
                temas.precedenteAtivo(),
                painelDemandasRepetitivasAtivo,
                pronta ? "janela-precedentes-controlada" : "janela-precedentes-assistida",
                List.copyOf(guardrails),
                List.copyOf(fundamentos.stream().limit(30).toList()),
                processoReferencia == null ? null : processoReferencia.getId(),
                processoReferencia == null ? null : numeroProcesso(processoReferencia)
        );
    }

    private IncidenteMassaSnapshot avaliarIncidenteMassa(List<Processo> processos, boolean painelDemandasRepetitivasAtivo) {
        if (processos.isEmpty()) {
            return new IncidenteMassaSnapshot(0, false, List.of("incidentesMassa.semAmostra=true"));
        }
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        int score = 34;
        boolean ativo = false;
        Map<String, Long> chavesRepeticao = processos.stream()
                .map(this::chaveRepeticao)
                .filter(item -> !item.isBlank())
                .collect(Collectors.groupingBy(item -> item, LinkedHashMap::new, Collectors.counting()));
        long picoRepeticao = chavesRepeticao.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        score += Math.min(18, processos.size() * 3);
        score += Math.min(18, (int) picoRepeticao * 5);
        if (painelDemandasRepetitivasAtivo) {
            score += 12;
            ativo = true;
            fundamentos.add("incidentesMassa.painelDemandasRepetitivas=true");
        }
        for (Processo processo : processos) {
            String corpus = normalize(
                    processo.getClasseProcessual(),
                    processo.getAssunto(),
                    processo.getObjetoProcessual(),
                    processo.getMaterialProbatorioResumo(),
                    processo.getFaseAtual(),
                    processo.getPreventionMode(),
                    processo.getLinkageMode(),
                    processo.getRoutingRiskLevel()
            );
            boolean marcadorMassa = containsAny(corpus,
                    "IRDR",
                    "IAC",
                    "INCIDENTE DE RESOLUCAO DE DEMANDAS REPETITIVAS",
                    "INCIDENTE DE ASSUNCAO DE COMPETENCIA",
                    "DEMANDA REPETITIVA",
                    "RECURSO REPETITIVO",
                    "TEMA",
                    "PRECEDENTE",
                    "SOBRESTA",
                    "AFETA");
            if (marcadorMassa) {
                ativo = true;
                score += 8;
                fundamentos.add("incidentesMassa.processo=" + numeroProcesso(processo));
            }
        }
        fundamentos.add("incidentesMassa.picoRepeticao=" + picoRepeticao);
        fundamentos.add("incidentesMassa.totalProcessos=" + processos.size());
        return new IncidenteMassaSnapshot(clamp(score), ativo, List.copyOf(fundamentos));
    }

    private TemaCoverageSnapshot avaliarTemas(
            String tribunalCodigo,
            CompetenciaKey competenciaKey,
            List<Processo> processos,
            TemaPool temaPool,
            boolean painelDemandasRepetitivasAtivo) {
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        List<TemaRecursoRepetitivo> repetitivos = temaPool.repetitivos().stream()
                .filter(tema -> matchesTribunal(tema.getTribunalSigla(), tribunalCodigo) || matchesProcesso(tema.getRecursoRepresentativoProcesso(), tribunalCodigo, competenciaKey))
                .filter(tema -> matchesProcesso(tema.getRecursoRepresentativoProcesso(), tribunalCodigo, competenciaKey) || tema.getRecursoRepresentativoProcesso() == null)
                .toList();
        List<TemaRepercussaoGeral> repercussoes = temaPool.repercussoes().stream()
                .filter(tema -> matchesProcesso(tema.getLeadingCaseProcesso(), tribunalCodigo, competenciaKey) || tema.getLeadingCaseProcesso() == null)
                .toList();
        List<TemaPrecedenteVinculante> vinculantes = temaPool.vinculantes().stream()
                .filter(tema -> matchesProcesso(tema.getLeadingCaseProcesso(), tribunalCodigo, competenciaKey) || tema.getLeadingCaseProcesso() == null)
                .toList();

        long afetados = repetitivos.stream().filter(tema -> containsAny(normalize(tema.getStatus()), "AFETADO", "SOBRESTADO")).count()
                + repercussoes.stream().filter(tema -> containsAny(normalize(tema.getStatus()), "RECONHECIDO", "AFETADO", "SOBRESTADO")).count();
        long precedentesAtivos = vinculantes.stream().filter(tema -> containsAny(normalize(tema.getStatus()), "RECONHECIDO", "APLICADO", "JULGADO")).count();
        int totalSobrestados = repetitivos.stream().mapToInt(tema -> safeInt(tema.getProcessosSobrestados())).sum()
                + repercussoes.stream().mapToInt(tema -> safeInt(tema.getProcessosSobrestados())).sum()
                + vinculantes.stream().mapToInt(tema -> safeInt(tema.getProcessosSobrestados())).sum();
        int totalAplicados = repetitivos.stream().mapToInt(tema -> safeInt(tema.getProcessosAplicados())).sum()
                + repercussoes.stream().mapToInt(tema -> safeInt(tema.getProcessosAplicados())).sum()
                + vinculantes.stream().mapToInt(tema -> safeInt(tema.getProcessosAplicados())).sum();

        boolean afetacaoAtiva = afetados > 0 || processos.stream().anyMatch(processo -> containsAny(normalize(processo.getClasseProcessual(), processo.getAssunto()), "IRDR", "IAC", "AFETA"));
        boolean sobrestamentoAtiva = totalSobrestados > 0 || processos.stream().anyMatch(processo -> containsAny(normalize(processo.getClasseProcessual(), processo.getAssunto(), processo.getObjetoProcessual()), "SOBRESTA", "SUSPENS"));
        boolean precedenteAtivo = precedentesAtivos > 0 || totalAplicados > 0 || painelDemandasRepetitivasAtivo;

        int scoreAfetacao = 42;
        scoreAfetacao += Math.min(18, (int) afetados * 7);
        scoreAfetacao += painelDemandasRepetitivasAtivo ? 8 : 0;
        scoreAfetacao += processos.size() >= 3 ? 8 : 0;

        int scoreSobrestamento = 38;
        scoreSobrestamento += Math.min(26, totalSobrestados / 4);
        scoreSobrestamento += sobrestamentoAtiva ? 10 : 0;
        scoreSobrestamento += afetacaoAtiva ? 6 : 0;

        int scorePrecedentes = 40;
        scorePrecedentes += Math.min(22, (int) precedentesAtivos * 6);
        scorePrecedentes += Math.min(20, totalAplicados / 4);
        scorePrecedentes += painelDemandasRepetitivasAtivo ? 6 : 0;

        fundamentos.add("temas.repetitivos=" + repetitivos.size());
        fundamentos.add("temas.repercussoes=" + repercussoes.size());
        fundamentos.add("temas.vinculantes=" + vinculantes.size());
        fundamentos.add("temas.afetados=" + afetados);
        fundamentos.add("temas.sobrestados=" + totalSobrestados);
        fundamentos.add("temas.aplicados=" + totalAplicados);
        fundamentos.add("temas.precedentesAtivos=" + precedentesAtivos);

        return new TemaCoverageSnapshot(
                clamp(scoreAfetacao),
                clamp(scoreSobrestamento),
                clamp(scorePrecedentes),
                afetacaoAtiva,
                sobrestamentoAtiva,
                precedenteAtivo,
                List.copyOf(fundamentos)
        );
    }

    private boolean avaliarPainelDemandasRepetitivas(Processo processo) {
        if (processo == null || processo.getId() == null) {
            return false;
        }
        try {
            ProcessoPainelFonteOficialAggregate aggregate = processoPainelFonteOficialApplicationService.detalhar(processo.getId());
            return aggregate.itens().stream().anyMatch(item -> "DEMANDAS_REPETITIVAS".equalsIgnoreCase(item.widgetCode()));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private TemaPool carregarTemas() {
        return new TemaPool(
                temaRecursoRepetitivoRepository.findTop100ByOrderByCreatedAtDesc(),
                temaRepercussaoGeralRepository.findTop100ByOrderByCreatedAtDesc(),
                temaPrecedenteVinculanteRepository.findTop100ByOrderByCreatedAtDesc()
        );
    }

    private List<Processo> carregarProcessosRecentes(int size) {
        Sort sort = Sort.by(Sort.Order.desc("dataUltimaMovimentacao"), Sort.Order.desc("id"));
        return processoRepository.findAll(PageRequest.of(0, Math.max(1, size), sort)).getContent();
    }

    private CompetenciaKey resolverCompetencia(Processo processo) {
        RamoDireito ramo = processo.getRamoDireito() == null ? RamoDireito.CIVIL : processo.getRamoDireito();
        RitoProcessual rito = processo.getRito() == null ? RitoProcessual.COMUM_ORDINARIO : processo.getRito();
        return new CompetenciaKey(ramo, rito);
    }

    private boolean matchesTribunal(String raw, String tribunalCodigo) {
        String normalized = normalize(raw);
        String target = normalize(tribunalCodigo);
        if (normalized.isBlank() || target.isBlank()) {
            return false;
        }
        return normalized.equals(target) || normalized.endsWith(target) || target.endsWith(normalized);
    }

    private boolean matchesProcesso(Processo processo, String tribunalCodigo, CompetenciaKey competenciaKey) {
        if (processo == null) {
            return false;
        }
        if (!matchesTribunal(resolverCodigoTribunal(processo), tribunalCodigo)) {
            return false;
        }
        return competenciaKey.ramo() == (processo.getRamoDireito() == null ? RamoDireito.CIVIL : processo.getRamoDireito())
                && competenciaKey.rito() == (processo.getRito() == null ? RitoProcessual.COMUM_ORDINARIO : processo.getRito());
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

    private String chaveRepeticao(Processo processo) {
        return normalize(processo.getClasseProcessual(), processo.getAssunto(), processo.getObjetoProcessual());
    }

    private int scoreCompetencia(PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia competencia) {
        return clamp((int) Math.round((competencia.scoreIncidentesMassa()
                + competencia.scoreAfetacao()
                + competencia.scoreSobrestamento()
                + competencia.scorePrecedentesVinculantes()) / 4.0d));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String numeroProcesso(Processo processo) {
        String numero = Objects.toString(processo.getNumeroUnificado(), "").trim();
        if (!numero.isBlank()) {
            return numero;
        }
        return Objects.toString(processo.getNumeroProcesso(), "").trim();
    }

    private boolean containsAny(String source, String... needles) {
        if (source == null || source.isBlank()) {
            return false;
        }
        String normalized = normalize(source);
        for (String needle : needles) {
            if (normalized.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(Object... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        ArrayList<String> normalized = new ArrayList<>(values.length);
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String token = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
            if (!token.isBlank()) {
                normalized.add(token);
            }
        }
        return String.join(" ", normalized);
    }

    private record CompetenciaKey(RamoDireito ramo, RitoProcessual rito) {
    }

    private record TemaPool(
            List<TemaRecursoRepetitivo> repetitivos,
            List<TemaRepercussaoGeral> repercussoes,
            List<TemaPrecedenteVinculante> vinculantes
    ) {
        private TemaPool {
            repetitivos = repetitivos == null ? List.of() : List.copyOf(repetitivos);
            repercussoes = repercussoes == null ? List.of() : List.copyOf(repercussoes);
            vinculantes = vinculantes == null ? List.of() : List.copyOf(vinculantes);
        }
    }

    private record IncidenteMassaSnapshot(int score, boolean ativo, List<String> fundamentos) {
        private IncidenteMassaSnapshot {
            score = Math.max(0, Math.min(100, score));
            fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        }
    }

    private record TemaCoverageSnapshot(
            int scoreAfetacao,
            int scoreSobrestamento,
            int scorePrecedentes,
            boolean afetacaoAtiva,
            boolean sobrestamentoAtiva,
            boolean precedenteAtivo,
            List<String> fundamentos
    ) {
        private TemaCoverageSnapshot {
            scoreAfetacao = Math.max(0, Math.min(100, scoreAfetacao));
            scoreSobrestamento = Math.max(0, Math.min(100, scoreSobrestamento));
            scorePrecedentes = Math.max(0, Math.min(100, scorePrecedentes));
            fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        }
    }
}
