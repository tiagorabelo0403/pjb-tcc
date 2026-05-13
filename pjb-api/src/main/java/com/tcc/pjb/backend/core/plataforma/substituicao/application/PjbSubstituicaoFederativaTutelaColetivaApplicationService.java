package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.core.governance.institucional.application.PjbGovernancaInstitucionalNormativaApplicationService;
import com.tcc.pjb.backend.core.governance.institucional.domain.PjbGovernancaInstitucionalNormativaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPrecedentesQualificadosAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPrecedentesQualificadosTribunal;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTutelaColetivaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTutelaColetivaCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTutelaColetivaTribunal;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoProducaoPesadaApplicationService;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoProducaoPesadaAggregate;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationCanonicalActResolveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationCanonicalActResolveResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.routing.NationalCommunicationRoutingResolveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.routing.NationalCommunicationRoutingResolveResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.processual.comunicacao.flow.NationalCommunicationFlowService;
import java.time.Instant;
import java.util.ArrayList;
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
public class PjbSubstituicaoFederativaTutelaColetivaApplicationService {

    private static final int NATIONAL_SAMPLE_SIZE = 720;
    private static final int TRIBUNAL_SAMPLE_SIZE = 120;
    private static final int COMPETENCIAS_LIMITE = 10;
    private static final int COMPETENCIA_SAMPLE_SIZE = 5;

    private final PjbSubstituicaoFederativaPrecedentesQualificadosApplicationService precedentesApplicationService;
    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final NationalCommunicationFlowService nationalCommunicationFlowService;
    private final ProcessoExecucaoApplicationService processoExecucaoApplicationService;
    private final ProcessoProducaoPesadaApplicationService processoProducaoPesadaApplicationService;
    private final PjbGovernancaInstitucionalNormativaApplicationService governancaInstitucionalNormativaApplicationService;
    private final ProcessoRepository processoRepository;

    public PjbSubstituicaoFederativaTutelaColetivaApplicationService(
            PjbSubstituicaoFederativaPrecedentesQualificadosApplicationService precedentesApplicationService,
            NationalProceduralRoutingService nationalProceduralRoutingService,
            NationalCommunicationFlowService nationalCommunicationFlowService,
            ProcessoExecucaoApplicationService processoExecucaoApplicationService,
            ProcessoProducaoPesadaApplicationService processoProducaoPesadaApplicationService,
            PjbGovernancaInstitucionalNormativaApplicationService governancaInstitucionalNormativaApplicationService,
            ProcessoRepository processoRepository) {
        this.precedentesApplicationService = Objects.requireNonNull(precedentesApplicationService);
        this.nationalProceduralRoutingService = Objects.requireNonNull(nationalProceduralRoutingService);
        this.nationalCommunicationFlowService = Objects.requireNonNull(nationalCommunicationFlowService);
        this.processoExecucaoApplicationService = Objects.requireNonNull(processoExecucaoApplicationService);
        this.processoProducaoPesadaApplicationService = Objects.requireNonNull(processoProducaoPesadaApplicationService);
        this.governancaInstitucionalNormativaApplicationService = Objects.requireNonNull(governancaInstitucionalNormativaApplicationService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaTutelaColetivaAggregate avaliar() {
        PjbSubstituicaoFederativaPrecedentesQualificadosAggregate precedentes = precedentesApplicationService.avaliar();
        Map<String, PjbSubstituicaoFederativaPrecedentesQualificadosTribunal> baseline = precedentes.tribunais().stream()
                .collect(Collectors.toMap(PjbSubstituicaoFederativaPrecedentesQualificadosTribunal::tribunalCodigo, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<Processo>> processosPorTribunal = carregarProcessosRecentes(NATIONAL_SAMPLE_SIZE).stream()
                .collect(Collectors.groupingBy(this::resolverCodigoTribunal, LinkedHashMap::new, Collectors.toList()));

        List<PjbSubstituicaoFederativaTutelaColetivaTribunal> tribunais = baseline.values().stream()
                .map(tribunal -> buildTribunal(tribunal, processosPorTribunal.getOrDefault(tribunal.tribunalCodigo(), List.of())))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaTutelaColetivaTribunal::malhaTutelaColetivaPronta).reversed()
                        .thenComparing(PjbSubstituicaoFederativaTutelaColetivaTribunal::scoreGeral, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaTutelaColetivaTribunal::tribunalCodigo))
                .toList();

        int tribunaisProntos = (int) tribunais.stream().filter(PjbSubstituicaoFederativaTutelaColetivaTribunal::malhaTutelaColetivaPronta).count();
        int scoreNacional = clamp((int) Math.round(tribunais.stream().mapToInt(PjbSubstituicaoFederativaTutelaColetivaTribunal::scoreGeral).average().orElse(0d)));
        boolean tutelaColetivaConectada = tribunais.stream().filter(item -> item.scoreTutelaColetiva() >= 70).count() >= Math.max(3, tribunais.size() / 5);
        boolean demandasEstruturaisGovernadas = tribunais.stream().filter(item -> item.scoreDemandasEstruturais() >= 68).count() >= Math.max(3, tribunais.size() / 5);
        boolean execucaoColetivaGovernada = tribunais.stream().filter(item -> item.scoreExecucaoColetiva() >= 68).count() >= Math.max(3, tribunais.size() / 5);
        boolean cumprimentoMassaGovernado = tribunais.stream().filter(item -> item.scoreCumprimentoMassa() >= 68).count() >= Math.max(3, tribunais.size() / 5);
        boolean pronta = tribunaisProntos >= Math.max(4, tribunais.size() / 6)
                && tutelaColetivaConectada
                && demandasEstruturaisGovernadas
                && execucaoColetivaGovernada
                && cumprimentoMassaGovernado;

        LinkedHashSet<String> bloqueadoresCriticos = new LinkedHashSet<>(precedentes.bloqueadoresCriticos());
        tribunais.stream().flatMap(tribunal -> tribunal.bloqueadores().stream()).limit(40).forEach(bloqueadoresCriticos::add);

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(precedentes.fundamentos());
        fundamentos.add("tutelaColetiva.scoreNacional=" + scoreNacional);
        fundamentos.add("tutelaColetiva.tribunaisProntos=" + tribunaisProntos);
        fundamentos.add("tutelaColetiva.conectada=" + tutelaColetivaConectada);
        fundamentos.add("tutelaColetiva.demandasEstruturais=" + demandasEstruturaisGovernadas);
        fundamentos.add("tutelaColetiva.execucaoColetiva=" + execucaoColetivaGovernada);
        fundamentos.add("tutelaColetiva.cumprimentoMassa=" + cumprimentoMassaGovernado);
        fundamentos.add("tutelaColetiva.malhaPronta=" + pronta);

        return new PjbSubstituicaoFederativaTutelaColetivaAggregate(
                scoreNacional,
                pronta,
                tutelaColetivaConectada,
                demandasEstruturaisGovernadas,
                execucaoColetivaGovernada,
                cumprimentoMassaGovernado,
                tribunaisProntos,
                tribunais,
                List.copyOf(bloqueadoresCriticos.stream().limit(60).toList()),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaTutelaColetivaTribunal avaliarTribunal(String tribunalCodigo) {
        PjbSubstituicaoFederativaPrecedentesQualificadosTribunal baseline = precedentesApplicationService.avaliarTribunal(tribunalCodigo);
        List<Processo> processos = carregarProcessosRecentes(TRIBUNAL_SAMPLE_SIZE * 8).stream()
                .filter(processo -> baseline.tribunalCodigo().equals(resolverCodigoTribunal(processo)))
                .limit(TRIBUNAL_SAMPLE_SIZE)
                .toList();
        return buildTribunal(baseline, processos);
    }

    private PjbSubstituicaoFederativaTutelaColetivaTribunal buildTribunal(PjbSubstituicaoFederativaPrecedentesQualificadosTribunal baseline,
                                                                          List<Processo> processos) {
        Map<CompetenciaKey, List<CollectiveSnapshot>> snapshotsPorCompetencia = processos.stream()
                .map(this::toSnapshot)
                .filter(CollectiveSnapshot::relevanteColetivo)
                .collect(Collectors.groupingBy(CollectiveSnapshot::competencia, LinkedHashMap::new, Collectors.toList()));
        if (snapshotsPorCompetencia.isEmpty()) {
            processos.stream().limit(COMPETENCIA_SAMPLE_SIZE).map(this::toSnapshot).forEach(snapshot -> snapshotsPorCompetencia.put(snapshot.competencia(), List.of(snapshot)));
        }
        List<PjbSubstituicaoFederativaTutelaColetivaCompetencia> competencias = snapshotsPorCompetencia.entrySet().stream()
                .sorted(Map.Entry.<CompetenciaKey, List<CollectiveSnapshot>>comparingByValue(Comparator.comparingInt(List::size)).reversed())
                .limit(COMPETENCIAS_LIMITE)
                .map(entry -> buildCompetencia(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaTutelaColetivaCompetencia::malhaTutelaColetivaPronta).reversed()
                        .thenComparing(this::scoreCompetencia, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaTutelaColetivaCompetencia::competenciaCodigo))
                .toList();

        int scoreTutelaColetiva = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaTutelaColetivaCompetencia::scoreTutelaColetiva).average().orElse(0d)));
        int scoreDemandasEstruturais = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaTutelaColetivaCompetencia::scoreDemandasEstruturais).average().orElse(0d)));
        int scoreExecucaoColetiva = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaTutelaColetivaCompetencia::scoreExecucaoColetiva).average().orElse(0d)));
        int scoreCumprimentoMassa = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaTutelaColetivaCompetencia::scoreCumprimentoMassa).average().orElse(0d)));
        int scoreGeral = clamp((int) Math.round((scoreTutelaColetiva + scoreDemandasEstruturais + scoreExecucaoColetiva + scoreCumprimentoMassa) / 4.0d));
        boolean pronta = baseline.malhaPrecedentesPronta()
                && scoreTutelaColetiva >= 68
                && scoreDemandasEstruturais >= 66
                && scoreExecucaoColetiva >= 66
                && scoreCumprimentoMassa >= 66
                && competencias.stream().anyMatch(PjbSubstituicaoFederativaTutelaColetivaCompetencia::malhaTutelaColetivaPronta);

        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>(baseline.bloqueadores());
        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(baseline.fundamentos());
        fundamentos.add("tutelaColetiva.tribunal.scoreGeral=" + scoreGeral);
        fundamentos.add("tutelaColetiva.tribunal.scoreTutela=" + scoreTutelaColetiva);
        fundamentos.add("tutelaColetiva.tribunal.scoreEstrutural=" + scoreDemandasEstruturais);
        fundamentos.add("tutelaColetiva.tribunal.scoreExecucao=" + scoreExecucaoColetiva);
        fundamentos.add("tutelaColetiva.tribunal.scoreCumprimento=" + scoreCumprimentoMassa);
        fundamentos.add("tutelaColetiva.tribunal.competencias=" + competencias.size());

        if (scoreTutelaColetiva < 68) {
            bloqueadores.add("Tutela coletiva ainda não está roteada com governança uniforme no tribunal.");
            proximasAcoes.add("Ligar ACP, dissídio coletivo e demanda estrutural à malha oficial de comunicação e vistas obrigatórias.");
        }
        if (scoreDemandasEstruturais < 66) {
            bloqueadores.add("Demandas estruturais ainda não estão estabilizadas por competência material.");
            proximasAcoes.add("Cruzar precedentes qualificados, prova oficial e radar estrutural por competência e rito.");
        }
        if (scoreExecucaoColetiva < 66) {
            bloqueadores.add("Execução coletiva ainda não está convergindo com o eixo executivo institucional.");
            proximasAcoes.add("Conectar título coletivo, trilha executiva e janelas de cumprimento por tribunal.");
        }
        if (scoreCumprimentoMassa < 66) {
            bloqueadores.add("Cumprimento em massa ainda não possui trilha previsível de monitoramento e retomada.");
            proximasAcoes.add("Abrir fila institucional de cumprimento em massa com gates de reversão e governança.");
        }
        if (!pronta) {
            proximasAcoes.add("Manter operação assistida da tutela coletiva até convergir comunicação, execução e governança por competência.");
        }
        competencias.stream().flatMap(item -> item.fundamentos().stream()).limit(20).forEach(fundamentos::add);

        return new PjbSubstituicaoFederativaTutelaColetivaTribunal(
                baseline.tribunalCodigo(),
                baseline.tribunalNome(),
                baseline.ramoJustica(),
                baseline.legadoPrincipal(),
                baseline.ondaAtual(),
                scoreGeral,
                scoreTutelaColetiva,
                scoreDemandasEstruturais,
                scoreExecucaoColetiva,
                scoreCumprimentoMassa,
                baseline.malhaPrecedentesPronta(),
                pronta,
                competencias.size(),
                competencias,
                List.copyOf(bloqueadores.stream().limit(24).toList()),
                List.copyOf(proximasAcoes.stream().limit(16).toList()),
                List.copyOf(fundamentos.stream().limit(40).toList())
        );
    }

    private PjbSubstituicaoFederativaTutelaColetivaCompetencia buildCompetencia(CompetenciaKey competenciaKey,
                                                                                 List<CollectiveSnapshot> snapshots) {
        List<CollectiveSnapshot> amostra = snapshots.stream().limit(COMPETENCIA_SAMPLE_SIZE).toList();
        CollectiveSnapshot referencia = amostra.isEmpty() ? null : amostra.getFirst();
        int total = snapshots.size();
        long tutelaAtiva = snapshots.stream().filter(CollectiveSnapshot::tutelaColetivaAtiva).count();
        long estruturalAtiva = snapshots.stream().filter(CollectiveSnapshot::demandaEstruturalAtiva).count();
        long execucaoAtiva = snapshots.stream().filter(CollectiveSnapshot::execucaoColetivaAtiva).count();
        long cumprimentoAtivo = snapshots.stream().filter(CollectiveSnapshot::cumprimentoMassaAtivo).count();
        long roteamentoAtivo = snapshots.stream().filter(CollectiveSnapshot::roteamentoColetivoAtivo).count();

        int scoreTutela = clamp((int) Math.round((snapshots.stream().mapToInt(CollectiveSnapshot::scoreTutelaColetiva).average().orElse(0d) * 0.75d)
                + percentage(tutelaAtiva, total) * 0.25d));
        int scoreEstrutural = clamp((int) Math.round((snapshots.stream().mapToInt(CollectiveSnapshot::scoreDemandaEstrutural).average().orElse(0d) * 0.70d)
                + percentage(estruturalAtiva, total) * 0.30d));
        int scoreExecucao = clamp((int) Math.round((snapshots.stream().mapToInt(CollectiveSnapshot::scoreExecucaoColetiva).average().orElse(0d) * 0.70d)
                + percentage(execucaoAtiva, total) * 0.30d));
        int scoreCumprimento = clamp((int) Math.round((snapshots.stream().mapToInt(CollectiveSnapshot::scoreCumprimentoMassa).average().orElse(0d) * 0.70d)
                + percentage(cumprimentoAtivo, total) * 0.30d));
        boolean pronta = scoreTutela >= 66
                && scoreEstrutural >= 64
                && scoreExecucao >= 64
                && scoreCumprimento >= 64
                && roteamentoAtivo > 0;

        LinkedHashSet<String> guardrails = new LinkedHashSet<>();
        snapshots.stream().flatMap(item -> item.guardrails().stream()).limit(18).forEach(guardrails::add);
        if (roteamentoAtivo == 0) {
            guardrails.add("Sem roteamento institucional coletivo confiável na amostra da competência.");
        }
        if (cumprimentoAtivo == 0) {
            guardrails.add("Cumprimento em massa ainda não apareceu com trilha executiva consistente nesta competência.");
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("tutelaColetiva.competencia.total=" + total);
        fundamentos.add("tutelaColetiva.competencia.scoreTutela=" + scoreTutela);
        fundamentos.add("tutelaColetiva.competencia.scoreEstrutural=" + scoreEstrutural);
        fundamentos.add("tutelaColetiva.competencia.scoreExecucao=" + scoreExecucao);
        fundamentos.add("tutelaColetiva.competencia.scoreCumprimento=" + scoreCumprimento);
        fundamentos.add("tutelaColetiva.competencia.roteamento=" + roteamentoAtivo);
        snapshots.stream().flatMap(item -> item.fundamentos().stream()).limit(20).forEach(fundamentos::add);

        return new PjbSubstituicaoFederativaTutelaColetivaCompetencia(
                competenciaKey.codigo(),
                competenciaKey.ramoCodigo(),
                competenciaKey.ramoNome(),
                competenciaKey.ritoCodigo(),
                total,
                scoreTutela,
                scoreEstrutural,
                scoreExecucao,
                scoreCumprimento,
                pronta,
                tutelaAtiva > 0,
                estruturalAtiva > 0,
                execucaoAtiva > 0,
                cumprimentoAtivo > 0,
                roteamentoAtivo > 0,
                pronta ? "janela-coletiva-governada" : "janela-coletiva-assistida",
                List.copyOf(guardrails.stream().limit(18).toList()),
                List.copyOf(fundamentos.stream().limit(32).toList()),
                referencia != null ? referencia.processoId() : null,
                referencia != null ? referencia.numeroReferencia() : null
        );
    }

    private CollectiveSnapshot toSnapshot(Processo processo) {
        ProceduralRoutingReport routing = safeRouting(processo);
        boolean relevanteColetivo = isCollective(processo, routing);
        boolean demandaEstrutural = isStructuralDemand(processo, routing);
        NationalCommunicationCanonicalActResolveResponse atoCanonico = relevanteColetivo ? safeAtoCanonico(processo) : null;
        NationalCommunicationRoutingResolveResponse roteamento = relevanteColetivo ? safeRoteamentoColetivo(processo, atoCanonico) : null;
        ProcessoExecucaoAggregate execucao = processo.getId() != null ? safeExecucao(processo.getId()) : null;
        ProcessoProducaoPesadaAggregate producao = processo.getId() != null ? safeProducao(processo.getId()) : null;
        PjbGovernancaInstitucionalNormativaAggregate governanca = processo.getId() != null ? safeGovernanca(processo.getId()) : null;

        boolean tutelaColetivaAtiva = relevanteColetivo && atoCanonico != null
                && "ABRIR_VISTA_MP_ACAO_COLETIVA".equalsIgnoreCase(atoCanonico.atoCanonico());
        boolean roteamentoColetivoAtivo = relevanteColetivo && roteamento != null
                && roteamento.destinatarioKind() != null
                && roteamento.canalPrincipal() != null;
        boolean execucaoColetivaAtiva = relevanteColetivo && execucao != null && (execucao.processoExecutivo() || execucao.totalTrilhas() > 0);
        boolean cumprimentoMassaAtivo = relevanteColetivo && execucao != null && (execucao.totalMandados() > 0 || execucao.totalBloqueantes() > 0);

        int scoreTutela = clamp((int) Math.round(baseComunicacao(atoCanonico, roteamento, routing) * 0.55d
                + scoreGovernanca(governanca) * 0.20d
                + scoreProducao(producao) * 0.10d
                + (relevanteColetivo ? 15d : 0d)));
        int scoreEstrutural = clamp((int) Math.round((demandaEstrutural ? 52d : 28d)
                + scoreGovernanca(governanca) * 0.20d
                + scoreProducao(producao) * 0.15d
                + (routing != null && "CRITICO".equalsIgnoreCase(routing.riskLevel()) ? 8d : 0d)));
        int scoreExecucao = clamp((int) Math.round((execucao != null ? Math.min(55d, execucao.totalTrilhas() * 11d + execucao.totalMandados() * 7d) : 18d)
                + scoreGovernanca(governanca) * 0.20d
                + scoreProducao(producao) * 0.15d
                + (execucaoColetivaAtiva ? 12d : 0d)));
        int scoreCumprimento = clamp((int) Math.round((execucao != null ? Math.min(52d, execucao.totalBloqueantes() * 10d + execucao.totalMandados() * 6d) : 18d)
                + scoreGovernanca(governanca) * 0.18d
                + scoreProducao(producao) * 0.18d
                + (cumprimentoMassaAtivo ? 10d : 0d)));

        LinkedHashSet<String> guardrails = new LinkedHashSet<>();
        if (atoCanonico != null && atoCanonico.gateCode() != null) {
            guardrails.add(atoCanonico.gateCode());
        }
        if (roteamento != null && roteamento.gateCode() != null) {
            guardrails.add(roteamento.gateCode());
        }
        if (execucao != null) {
            guardrails.addAll(execucao.alertas().stream().limit(4).toList());
        }
        if (governanca != null) {
            guardrails.addAll(governanca.pendencias().stream().limit(4).toList());
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("processo=" + Objects.toString(numeroReferencia(processo), "SEM_NUMERO"));
        fundamentos.add("coletivo=" + relevanteColetivo);
        fundamentos.add("estrutural=" + demandaEstrutural);
        if (routing != null) {
            fundamentos.add("routing.actionNature=" + routing.actionNature());
            fundamentos.add("routing.actionFamily=" + routing.actionFamily());
            fundamentos.add("routing.proceduralTrack=" + routing.proceduralTrack());
        }
        if (atoCanonico != null) {
            fundamentos.add("comunicacao.atoCanonico=" + atoCanonico.atoCanonico());
            fundamentos.add("comunicacao.score=" + Objects.toString(atoCanonico.score(), "0"));
        }
        if (roteamento != null) {
            fundamentos.add("comunicacao.canal=" + roteamento.canalPrincipal());
            fundamentos.add("comunicacao.unidade=" + roteamento.unidadeCodigo());
        }
        if (execucao != null) {
            fundamentos.add("execucao.trilhas=" + execucao.totalTrilhas());
            fundamentos.add("execucao.mandados=" + execucao.totalMandados());
        }
        if (governanca != null) {
            fundamentos.add("governanca.score=" + governanca.scoreGeral());
        }
        if (producao != null) {
            fundamentos.add("producao.score=" + producao.scoreGeral());
        }

        return new CollectiveSnapshot(
                processo.getId(),
                numeroReferencia(processo),
                resolverCodigoTribunal(processo),
                resolverCompetencia(processo, routing),
                relevanteColetivo,
                tutelaColetivaAtiva,
                demandaEstrutural,
                execucaoColetivaAtiva,
                cumprimentoMassaAtivo,
                roteamentoColetivoAtivo,
                scoreTutela,
                scoreEstrutural,
                scoreExecucao,
                scoreCumprimento,
                List.copyOf(guardrails),
                List.copyOf(fundamentos)
        );
    }

    private List<Processo> carregarProcessosRecentes(int limit) {
        Sort sort = Sort.by(Sort.Order.desc("id"));
        return processoRepository.findAll(PageRequest.of(0, Math.max(1, limit), sort)).getContent();
    }

    private ProceduralRoutingReport safeRouting(Processo processo) {
        try {
            return nationalProceduralRoutingService.analyzeProcess(processo);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private NationalCommunicationCanonicalActResolveResponse safeAtoCanonico(Processo processo) {
        try {
            return nationalCommunicationFlowService.resolverAtoCanonico(new NationalCommunicationCanonicalActResolveRequest(
                    processo.getId(),
                    processo.getRamoDireito(),
                    GrauJurisdicao.PRIMEIRO_GRAU,
                    processo.getFaseAtual(),
                    processo.getClasseProcessual(),
                    processo.getAssunto(),
                    processo.getObjetoProcessual(),
                    processo.getObjetoProcessual(),
                    processo.getUf(),
                    processo.getComarca(),
                    processo.getVara(),
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    isFazendaPublica(processo),
                    true,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
            ));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private NationalCommunicationRoutingResolveResponse safeRoteamentoColetivo(Processo processo,
                                                                               NationalCommunicationCanonicalActResolveResponse atoCanonico) {
        try {
            AtoCanonicoProcessual ato = atoCanonico != null && atoCanonico.atoCanonico() != null
                    ? AtoCanonicoProcessual.valueOf(atoCanonico.atoCanonico())
                    : AtoCanonicoProcessual.ABRIR_VISTA_MP_ACAO_COLETIVA;
            return nationalCommunicationFlowService.resolverRoteamentoInstitucional(new NationalCommunicationRoutingResolveRequest(
                    processo.getId(),
                    DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                    PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                    TipoComunicacaoJudicial.VISTA_MP_FISCAL_ORDEM_JURIDICA,
                    ato,
                    processo.getRamoDireito(),
                    GrauJurisdicao.PRIMEIRO_GRAU,
                    processo.getUf(),
                    processo.getComarca(),
                    processo.getVara(),
                    processo.getUnidadeJudiciariaCodigo(),
                    processo.getTribunal(),
                    atoCanonico != null ? atoCanonico.fundamentoLegal() : null,
                    true,
                    null,
                    true,
                    true
            ));
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

    private ProcessoProducaoPesadaAggregate safeProducao(Long processoId) {
        try {
            return processoProducaoPesadaApplicationService.avaliar(processoId);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private PjbGovernancaInstitucionalNormativaAggregate safeGovernanca(Long processoId) {
        try {
            return governancaInstitucionalNormativaApplicationService.avaliar(processoId);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private CompetenciaKey resolverCompetencia(Processo processo, ProceduralRoutingReport routing) {
        String ramoCodigo = processo.getRamoDireito() != null ? processo.getRamoDireito().name() : extrairRamo(routing);
        String ritoCodigo = processo.getRito() != null ? processo.getRito().name() : extrairRito(routing);
        RamoDireito ramo = RamoDireito.fromString(ramoCodigo);
        return new CompetenciaKey(
                firstNonBlank(ramoCodigo, "NAO_CLASSIFICADO"),
                ramo != null ? ramo.getDescricao() : firstNonBlank(ramoCodigo, "Não classificado"),
                firstNonBlank(ritoCodigo, "NAO_CLASSIFICADO")
        );
    }

    private String resolverCodigoTribunal(Processo processo) {
        return firstNonBlank(processo.getTribunal(), "TRIBUNAL_NACIONAL");
    }

    private String numeroReferencia(Processo processo) {
        return firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero());
    }

    private boolean isCollective(Processo processo, ProceduralRoutingReport routing) {
        String corpus = normalize(String.join(" ", List.of(
                Objects.toString(processo.getClasseProcessual(), ""),
                Objects.toString(processo.getAssunto(), ""),
                Objects.toString(processo.getObjetoProcessual(), ""),
                processo.getRito() != null ? processo.getRito().name() : "",
                routing != null ? Objects.toString(routing.actionNature(), "") : "",
                routing != null ? Objects.toString(routing.actionFamily(), "") : "",
                routing != null ? Objects.toString(routing.proceduralTrack(), "") : ""
        )));
        return containsAny(corpus,
                "acao civil publica",
                "acp",
                "coletiv",
                "interesse difuso",
                "interesse coletivo",
                "dissidio coletivo",
                "dano coletivo",
                "tutela coletiva",
                "demanda estrutural",
                "cumprimento coletivo",
                "execucao coletiva");
    }

    private boolean isStructuralDemand(Processo processo, ProceduralRoutingReport routing) {
        String corpus = normalize(String.join(" ", List.of(
                Objects.toString(processo.getAssunto(), ""),
                Objects.toString(processo.getObjetoProcessual(), ""),
                routing != null ? Objects.toString(routing.actionNature(), "") : "",
                routing != null ? Objects.toString(routing.riskLevel(), "") : "",
                routing != null ? String.join(" ", routing.alerts()) : ""
        )));
        return containsAny(corpus,
                "estrutural",
                "coletiv",
                "saude publica",
                "politica publica",
                "moradia",
                "ambiental",
                "saneamento",
                "sistema prisional",
                "conflito fundiario",
                "agrario");
    }

    private boolean isFazendaPublica(Processo processo) {
        String corpus = normalize(String.join(" ", List.of(
                Objects.toString(processo.getParteReuNome(), ""),
                Objects.toString(processo.getParteAutoraNome(), ""),
                Objects.toString(processo.getAssunto(), "")
        )));
        return containsAny(corpus, "municipio", "estado", "uniao", "fazenda publica", "autarquia", "inss", "ibama", "fundacao publica");
    }

    private int baseComunicacao(NationalCommunicationCanonicalActResolveResponse atoCanonico,
                                NationalCommunicationRoutingResolveResponse roteamento,
                                ProceduralRoutingReport routing) {
        double score = 26d;
        if (atoCanonico != null) {
            score += Objects.requireNonNullElse(atoCanonico.score(), 0);
            if (atoCanonico.bloqueiaFluxo()) {
                score += 8d;
            }
        }
        if (roteamento != null) {
            score += roteamento.forcarDigital() ? 8d : 0d;
            score += roteamento.bloqueiaFluxo() ? 6d : 0d;
            score += roteamento.slaRespostaHoras() <= 24 ? 10d : 4d;
        }
        if (routing != null) {
            score += routing.exigeRevisaoHumana() ? 4d : 8d;
            score += "CRITICO".equalsIgnoreCase(routing.riskLevel()) ? 4d : 8d;
        }
        return clamp((int) Math.round(score / 1.65d));
    }

    private int scoreGovernanca(PjbGovernancaInstitucionalNormativaAggregate aggregate) {
        return aggregate == null ? 35 : aggregate.scoreGeral();
    }

    private int scoreProducao(ProcessoProducaoPesadaAggregate aggregate) {
        return aggregate == null ? 35 : aggregate.scoreGeral();
    }

    private String extrairRamo(ProceduralRoutingReport routing) {
        return routing == null ? null : normalizeEnumToken(firstNonBlank(routing.actionFamily(), routing.tipoJusticaSugerida()));
    }

    private String extrairRito(ProceduralRoutingReport routing) {
        return routing == null ? null : normalizeEnumToken(routing.ritoSugerido());
    }

    private int scoreCompetencia(PjbSubstituicaoFederativaTutelaColetivaCompetencia competencia) {
        return clamp((int) Math.round((competencia.scoreTutelaColetiva()
                + competencia.scoreDemandasEstruturais()
                + competencia.scoreExecucaoColetiva()
                + competencia.scoreCumprimentoMassa()) / 4.0d));
    }

    private int percentage(long value, int total) {
        return total <= 0 ? 0 : clamp((int) Math.round((value * 100.0d) / total));
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
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private String normalizeEnumToken(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private record CompetenciaKey(String ramoCodigo, String ramoNome, String ritoCodigo) {
        String codigo() {
            return ramoCodigo + ':' + ritoCodigo;
        }
    }

    private record CollectiveSnapshot(
            Long processoId,
            String numeroReferencia,
            String tribunalCodigo,
            CompetenciaKey competencia,
            boolean relevanteColetivo,
            boolean tutelaColetivaAtiva,
            boolean demandaEstruturalAtiva,
            boolean execucaoColetivaAtiva,
            boolean cumprimentoMassaAtivo,
            boolean roteamentoColetivoAtivo,
            int scoreTutelaColetiva,
            int scoreDemandaEstrutural,
            int scoreExecucaoColetiva,
            int scoreCumprimentoMassa,
            List<String> guardrails,
            List<String> fundamentos
    ) {
    }
}
