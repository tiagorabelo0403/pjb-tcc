package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix.RamoJusticaNacional;
import com.tcc.pjb.backend.core.comunicacao.judicial.ExpedicaoJudicial;
import com.tcc.pjb.backend.core.comunicacao.judicial.ExpedicaoJudicialRepository;
import com.tcc.pjb.backend.core.governance.institucional.application.PjbGovernancaInstitucionalNormativaApplicationService;
import com.tcc.pjb.backend.core.governance.institucional.domain.PjbGovernancaInstitucionalNormativaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCutoverCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCutoverMatrixAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCutoverTribunal;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTribunal;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomRamo;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomRito;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomTribunal;
import com.tcc.pjb.backend.core.processo.operacao.application.ProcessoOperacaoApplicationService;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoProducaoPesadaApplicationService;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoProducaoPesadaAggregate;
import com.tcc.pjb.backend.core.security.sigilo.SigiloAccessStatus;
import com.tcc.pjb.backend.core.security.sigilo.repository.SigiloAccessRequestRepository;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
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
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbSubstituicaoFederativaCutoverMatrixApplicationService {

    private static final int NATIONAL_SAMPLE_SIZE = 720;
    private static final int TRIBUNAL_SAMPLE_SIZE = 72;
    private static final int COMPETENCIA_SAMPLE_SIZE = 10;

    private final PjbSubstituicaoFederativaWarRoomApplicationService warRoomApplicationService;
    private final PjbSubstituicaoFederativaCentroComandoApplicationService centroComandoApplicationService;
    private final ProcessoRepository processoRepository;
    private final ProcessoOperacaoApplicationService processoOperacaoApplicationService;
    private final ProcessoProducaoPesadaApplicationService processoProducaoPesadaApplicationService;
    private final ExpedicaoJudicialRepository expedicaoJudicialRepository;
    private final ObjectProvider<SigiloAccessRequestRepository> sigiloAccessRequestRepositoryProvider;
    private final ObjectProvider<PjbGovernancaInstitucionalNormativaApplicationService> governancaNormativaApplicationServiceProvider;

    public PjbSubstituicaoFederativaCutoverMatrixApplicationService(
            PjbSubstituicaoFederativaWarRoomApplicationService warRoomApplicationService,
            PjbSubstituicaoFederativaCentroComandoApplicationService centroComandoApplicationService,
            ProcessoRepository processoRepository,
            ProcessoOperacaoApplicationService processoOperacaoApplicationService,
            ProcessoProducaoPesadaApplicationService processoProducaoPesadaApplicationService,
            ExpedicaoJudicialRepository expedicaoJudicialRepository,
            ObjectProvider<SigiloAccessRequestRepository> sigiloAccessRequestRepositoryProvider,
            ObjectProvider<PjbGovernancaInstitucionalNormativaApplicationService> governancaNormativaApplicationServiceProvider) {
        this.warRoomApplicationService = Objects.requireNonNull(warRoomApplicationService);
        this.centroComandoApplicationService = Objects.requireNonNull(centroComandoApplicationService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoOperacaoApplicationService = Objects.requireNonNull(processoOperacaoApplicationService);
        this.processoProducaoPesadaApplicationService = Objects.requireNonNull(processoProducaoPesadaApplicationService);
        this.expedicaoJudicialRepository = Objects.requireNonNull(expedicaoJudicialRepository);
        this.sigiloAccessRequestRepositoryProvider = Objects.requireNonNull(sigiloAccessRequestRepositoryProvider);
        this.governancaNormativaApplicationServiceProvider = Objects.requireNonNull(governancaNormativaApplicationServiceProvider);
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaCutoverMatrixAggregate avaliar() {
        PjbSubstituicaoFederativaWarRoomAggregate warRoom = warRoomApplicationService.avaliar();
        Map<String, PjbSubstituicaoFederativaTribunal> centroPorTribunal = centroComandoApplicationService.avaliar().tribunais().stream()
                .collect(Collectors.toMap(PjbSubstituicaoFederativaTribunal::tribunalCodigo, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<Processo>> processosPorTribunal = carregarProcessosRecentes(NATIONAL_SAMPLE_SIZE).stream()
                .collect(Collectors.groupingBy(this::resolverCodigoTribunal, LinkedHashMap::new, Collectors.toList()));

        List<PjbSubstituicaoFederativaCutoverTribunal> tribunais = warRoom.tribunais().stream()
                .map(tribunal -> buildTribunal(
                        tribunal,
                        centroPorTribunal.get(tribunal.tribunalCodigo()),
                        processosPorTribunal.getOrDefault(tribunal.tribunalCodigo(), List.of())
                ))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaCutoverTribunal::corteLiberado).reversed()
                        .thenComparing(PjbSubstituicaoFederativaCutoverTribunal::freezeAtivo)
                        .thenComparing(PjbSubstituicaoFederativaCutoverTribunal::scoreGeral, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaCutoverTribunal::tribunalCodigo))
                .toList();

        int tribunaisLiberados = (int) tribunais.stream().filter(PjbSubstituicaoFederativaCutoverTribunal::corteLiberado).count();
        int competenciasLiberadas = tribunais.stream().mapToInt(tribunal -> (int) tribunal.competencias().stream().filter(PjbSubstituicaoFederativaCutoverCompetencia::corteLiberado).count()).sum();
        int scoreGeral = clamp((int) Math.round(tribunais.stream().mapToInt(PjbSubstituicaoFederativaCutoverTribunal::scoreGeral).average().orElse(0d)));
        boolean freezeNacionalAtivo = warRoom.freezeNacionalAtivo() || tribunais.stream().filter(PjbSubstituicaoFederativaCutoverTribunal::freezeAtivo).count() >= Math.max(4, tribunais.size() / 5);
        boolean prontoJanelaMaterial = !freezeNacionalAtivo && tribunaisLiberados >= Math.max(3, tribunais.size() / 8) && competenciasLiberadas >= Math.max(6, tribunais.size());

        LinkedHashSet<String> bloqueadoresCriticos = new LinkedHashSet<>(warRoom.bloqueadoresCriticos());
        tribunais.stream().flatMap(tribunal -> tribunal.bloqueadores().stream()).limit(40).forEach(bloqueadoresCriticos::add);

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(warRoom.fundamentos());
        fundamentos.add("cutoverMatrix.scoreGeral=" + scoreGeral);
        fundamentos.add("cutoverMatrix.tribunaisLiberados=" + tribunaisLiberados);
        fundamentos.add("cutoverMatrix.competenciasLiberadas=" + competenciasLiberadas);
        fundamentos.add("cutoverMatrix.freezeNacionalAtivo=" + freezeNacionalAtivo);
        fundamentos.add("cutoverMatrix.prontoJanelaMaterial=" + prontoJanelaMaterial);

        return new PjbSubstituicaoFederativaCutoverMatrixAggregate(
                scoreGeral,
                freezeNacionalAtivo,
                prontoJanelaMaterial,
                tribunaisLiberados,
                competenciasLiberadas,
                tribunais,
                List.copyOf(bloqueadoresCriticos.stream().limit(50).toList()),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaCutoverTribunal avaliarTribunal(String tribunalCodigo) {
        PjbSubstituicaoFederativaWarRoomTribunal warRoomTribunal = warRoomApplicationService.avaliarTribunal(tribunalCodigo);
        PjbSubstituicaoFederativaTribunal centroTribunal = centroComandoApplicationService.avaliarTribunal(tribunalCodigo);
        List<Processo> processos = carregarProcessosRecentes(TRIBUNAL_SAMPLE_SIZE * 4).stream()
                .filter(processo -> tribunalCodigo.equalsIgnoreCase(resolverCodigoTribunal(processo)))
                .limit(TRIBUNAL_SAMPLE_SIZE)
                .toList();
        return buildTribunal(warRoomTribunal, centroTribunal, processos);
    }

    private PjbSubstituicaoFederativaCutoverTribunal buildTribunal(PjbSubstituicaoFederativaWarRoomTribunal warRoomTribunal,
                                                                   PjbSubstituicaoFederativaTribunal centroTribunal,
                                                                   List<Processo> processos) {
        List<Processo> amostraTribunal = limitar(processos, TRIBUNAL_SAMPLE_SIZE);
        Map<String, List<Processo>> processosPorCompetencia = amostraTribunal.stream()
                .filter(processo -> processo.getRamoDireito() != null)
                .collect(Collectors.groupingBy(this::competenciaKey, LinkedHashMap::new, Collectors.toList()));

        List<PjbSubstituicaoFederativaCutoverCompetencia> competencias = processosPorCompetencia.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, List<Processo>>>comparingInt(entry -> entry.getValue().size()).reversed())
                .limit(8)
                .map(entry -> buildCompetencia(warRoomTribunal, entry.getValue()))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaCutoverCompetencia::corteLiberado).reversed()
                        .thenComparing(PjbSubstituicaoFederativaCutoverCompetencia::scoreMaterial, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaCutoverCompetencia::ramoCodigo)
                        .thenComparing(PjbSubstituicaoFederativaCutoverCompetencia::ritoCodigo))
                .toList();

        int scoreMaterial = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaCutoverCompetencia::scoreMaterial).average().orElse(warRoomTribunal.scoreProntidao())));
        int scoreComunicacao = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaCutoverCompetencia::scoreComunicacao).average().orElse(scoreMaterial)));
        int scoreSigilo = clamp((int) Math.round(competencias.stream().mapToInt(PjbSubstituicaoFederativaCutoverCompetencia::scoreSigilo).average().orElse(scoreMaterial)));
        int scoreGovernanca = clamp(warRoomTribunal.scoreProntidao() + (centroTribunal != null && centroTribunal.prontoRollback() ? 6 : 0) + (centroTribunal != null && centroTribunal.prontoRollout() ? 4 : 0));
        int scoreGeral = clamp((int) Math.round(scoreMaterial * 0.34d + scoreComunicacao * 0.22d + scoreSigilo * 0.22d + scoreGovernanca * 0.22d));
        boolean corteLiberado = !warRoomTribunal.freezeAtivo() && warRoomTribunal.corteLiberado() && competencias.stream().filter(PjbSubstituicaoFederativaCutoverCompetencia::corteLiberado).count() >= Math.max(1, competencias.size() / 2);

        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>(warRoomTribunal.bloqueadores());
        competencias.stream()
                .filter(item -> !item.corteLiberado())
                .flatMap(item -> item.proximasAcoes().stream().limit(2))
                .limit(16)
                .forEach(bloqueadores::add);
        if (scoreComunicacao < 75) {
            bloqueadores.add("comunicacao.judicial.baixa-prontidao");
        }
        if (scoreSigilo < 72) {
            bloqueadores.add("sigilo.material.sensivel-sem-jogo-de-guardrails");
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(warRoomTribunal.guardrails());
        fundamentos.add("tribunal.scoreMaterial=" + scoreMaterial);
        fundamentos.add("tribunal.scoreComunicacao=" + scoreComunicacao);
        fundamentos.add("tribunal.scoreSigilo=" + scoreSigilo);
        fundamentos.add("tribunal.scoreGovernanca=" + scoreGovernanca);
        fundamentos.add("tribunal.totalCompetencias=" + competencias.size());
        fundamentos.add("tribunal.legadoPrincipal=" + (centroTribunal == null ? "NAO_IDENTIFICADO" : centroTribunal.legadoPrincipal()));

        return new PjbSubstituicaoFederativaCutoverTribunal(
                warRoomTribunal.tribunalCodigo(),
                warRoomTribunal.tribunalNome(),
                warRoomTribunal.ramoJustica(),
                centroTribunal == null ? "" : centroTribunal.legadoPrincipal(),
                warRoomTribunal.ondaAtual(),
                scoreGeral,
                scoreMaterial,
                scoreComunicacao,
                scoreSigilo,
                scoreGovernanca,
                corteLiberado,
                warRoomTribunal.freezeAtivo(),
                warRoomTribunal.janelaAtual(),
                competencias.size(),
                competencias,
                List.copyOf(bloqueadores),
                List.copyOf(fundamentos)
        );
    }

    private PjbSubstituicaoFederativaCutoverCompetencia buildCompetencia(PjbSubstituicaoFederativaWarRoomTribunal warRoomTribunal,
                                                                         List<Processo> processosCompetencia) {
        List<Processo> amostraCompetencia = limitar(processosCompetencia, COMPETENCIA_SAMPLE_SIZE);
        Processo referencia = amostraCompetencia.isEmpty() ? null : amostraCompetencia.getFirst();
        RamoDireito ramo = referencia == null ? null : referencia.getRamoDireito();
        RitoProcessual rito = referencia == null ? null : referencia.getRito();
        PjbSubstituicaoFederativaWarRoomRamo warRoomRamo = localizarRamo(warRoomTribunal, ramo);
        PjbSubstituicaoFederativaWarRoomRito warRoomRito = localizarRito(warRoomRamo, rito);

        ProcessoOperacaoAggregate operacao = referencia == null ? null : avaliarOperacao(referencia.getId());
        ProcessoProducaoPesadaAggregate producao = referencia == null ? null : avaliarProducao(referencia.getId());
        PjbGovernancaInstitucionalNormativaAggregate governanca = referencia == null ? null : avaliarGovernanca(referencia.getId());

        List<Long> processoIds = amostraCompetencia.stream().map(Processo::getId).filter(Objects::nonNull).toList();
        List<ExpedicaoJudicial> expedicoes = processoIds.isEmpty() ? List.of() : expedicaoJudicialRepository.findTop200ByProcessoIdInOrderByExpedidaEmDesc(processoIds);
        SigiloStats sigiloStats = avaliarSigilo(amostraCompetencia, processoIds);

        int scoreMaterial = scoreMaterial(warRoomTribunal, warRoomRamo, warRoomRito, operacao, producao, governanca, amostraCompetencia.size());
        int scoreComunicacao = scoreComunicacao(warRoomTribunal, warRoomRito, expedicoes);
        int scoreSigilo = scoreSigilo(ramo, warRoomRito, sigiloStats, amostraCompetencia);
        boolean corteLiberado = !warRoomTribunal.freezeAtivo()
                && warRoomTribunal.corteLiberado()
                && scoreMaterial >= 80
                && scoreComunicacao >= 76
                && scoreSigilo >= 72;

        LinkedHashSet<String> guardrails = new LinkedHashSet<>();
        if (warRoomRito != null) {
            guardrails.addAll(warRoomRito.alertas());
        }
        if (ramo != null && ramo.geraSigiloAutomatico()) {
            guardrails.add("sigilo.credencial-forte-para-ramo-sensivel");
        }
        if (!expedicoes.isEmpty()) {
            guardrails.add("comunicacao.judicial.trilha-entrega-auditavel");
        }
        if (sigiloStats.totalProcessosSigilosos() > 0) {
            guardrails.add("sigilo.zero-knowledge-ou-credencial-institucional");
        }

        LinkedHashSet<String> proximasAcoes = new LinkedHashSet<>();
        if (scoreMaterial < 80) {
            proximasAcoes.add("elevar.readiness.material." + codigoRamo(ramo));
        }
        if (scoreComunicacao < 76) {
            proximasAcoes.add("reforcar.comunicacao.judicial." + codigoRamo(ramo));
        }
        if (scoreSigilo < 72) {
            proximasAcoes.add("fechar.guardrails.sigilo." + codigoRamo(ramo));
        }
        if (warRoomRito != null) {
            proximasAcoes.addAll(warRoomRito.acoesImediatas());
        }
        if (operacao != null) {
            proximasAcoes.addAll(operacao.acoesImediatas());
        }

        return new PjbSubstituicaoFederativaCutoverCompetencia(
                codigoRamo(ramo),
                descricaoRamo(ramo),
                rito == null ? "SEM_RITO" : rito.name(),
                amostraCompetencia.size(),
                scoreMaterial,
                scoreComunicacao,
                scoreSigilo,
                corteLiberado,
                warRoomRito == null ? warRoomTribunal.janelaAtual() : warRoomRito.janelaAtual(),
                List.copyOf(guardrails.stream().limit(16).toList()),
                List.copyOf(proximasAcoes.stream().limit(16).toList()),
                referencia == null ? null : referencia.getId(),
                referencia == null ? null : referencia.getNumeroProcesso()
        );
    }

    private int scoreMaterial(PjbSubstituicaoFederativaWarRoomTribunal warRoomTribunal,
                              PjbSubstituicaoFederativaWarRoomRamo warRoomRamo,
                              PjbSubstituicaoFederativaWarRoomRito warRoomRito,
                              ProcessoOperacaoAggregate operacao,
                              ProcessoProducaoPesadaAggregate producao,
                              PjbGovernancaInstitucionalNormativaAggregate governanca,
                              int tamanhoAmostra) {
        int base = warRoomRito != null ? warRoomRito.score() : warRoomRamo != null ? warRoomRamo.score() : warRoomTribunal.scoreProntidao();
        int operacaoScore = operacao == null ? base : switch (operacao.readiness()) {
            case "READY" -> 90;
            case "PARTIAL_READY" -> 74;
            default -> 58;
        };
        int producaoScore = producao == null ? base : producao.scoreGeral();
        int governancaScore = governanca == null ? warRoomTribunal.scoreProntidao() : governanca.scoreGeral();
        int sampleBoost = Math.min(5, Math.max(0, tamanhoAmostra - 2));
        return clamp((int) Math.round(base * 0.42d + operacaoScore * 0.2d + producaoScore * 0.22d + governancaScore * 0.16d + sampleBoost));
    }

    private int scoreComunicacao(PjbSubstituicaoFederativaWarRoomTribunal warRoomTribunal,
                                 PjbSubstituicaoFederativaWarRoomRito warRoomRito,
                                 List<ExpedicaoJudicial> expedicoes) {
        if (expedicoes == null || expedicoes.isEmpty()) {
            return clamp((warRoomRito == null ? warRoomTribunal.scoreProntidao() : warRoomRito.score()) - 4);
        }
        long entregues = expedicoes.stream().filter(this::isEntregue).count();
        long falhas = expedicoes.stream().filter(this::isFalha).count();
        long pendentesOficial = expedicoes.stream().filter(item -> item.getStatus() == ExpedicaoJudicial.StatusExpedicao.PENDENTE_OFICIAL).count();
        long editais = expedicoes.stream().filter(item -> item.getStatus() == ExpedicaoJudicial.StatusExpedicao.PUBLICADA_EDITAL).count();
        long evasoes = expedicoes.stream().filter(ExpedicaoJudicial::isEvasaoDetectada).count();
        long escalonados = expedicoes.stream().filter(ExpedicaoJudicial::isEscalonadoParaJuiz).count();
        int base = warRoomRito == null ? warRoomTribunal.scoreProntidao() : warRoomRito.score();
        double ratioEntregue = (double) entregues / expedicoes.size();
        int score = (int) Math.round(base * 0.45d + ratioEntregue * 45d - falhas * 6d - pendentesOficial * 3d - evasoes * 4d - escalonados * 3d + editais * 1.5d);
        return clamp(score);
    }

    private int scoreSigilo(RamoDireito ramo,
                            PjbSubstituicaoFederativaWarRoomRito warRoomRito,
                            SigiloStats sigiloStats,
                            List<Processo> processos) {
        long sigilosos = sigiloStats.totalProcessosSigilosos();
        boolean ramoSensivel = ramo != null && ramo.geraSigiloAutomatico();
        int base = warRoomRito == null ? 78 : warRoomRito.score();
        if (sigilosos == 0 && !ramoSensivel) {
            return clamp(base + 8);
        }
        long maxNivel = processos.stream()
                .map(Processo::getNivelSigilo)
                .filter(Objects::nonNull)
                .mapToLong(NivelSigilo::getNivel)
                .max()
                .orElse(0L);
        int score = (int) Math.round(base * 0.4d
                + (sigiloStats.aprovadas() > 0 ? 24d : 12d)
                + (sigiloStats.pendentes() == 0 ? 12d : Math.max(0, 12d - sigiloStats.pendentes() * 4d))
                + (ramoSensivel ? 8d : 0d)
                - maxNivel * 3d);
        return clamp(score);
    }

    private SigiloStats avaliarSigilo(List<Processo> processos, List<Long> processoIds) {
        SigiloAccessRequestRepository repository = sigiloAccessRequestRepositoryProvider.getIfAvailable();
        long totalProcessosSigilosos = processos.stream()
                .map(Processo::getNivelSigilo)
                .filter(Objects::nonNull)
                .filter(NivelSigilo::exigeCredencial)
                .count();
        if (repository == null || processoIds.isEmpty()) {
            return new SigiloStats(totalProcessosSigilosos, 0, 0);
        }
        int aprovadas = 0;
        int pendentes = 0;
        for (Long processoId : processoIds) {
            aprovadas += repository.findByProcessoIdAndStatus(processoId, SigiloAccessStatus.APROVADA).size();
            pendentes += repository.findByProcessoIdAndStatus(processoId, SigiloAccessStatus.PENDENTE).size();
        }
        return new SigiloStats(totalProcessosSigilosos, aprovadas, pendentes);
    }

    private ProcessoOperacaoAggregate avaliarOperacao(Long processoId) {
        try {
            return processoOperacaoApplicationService.detalhar(processoId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private ProcessoProducaoPesadaAggregate avaliarProducao(Long processoId) {
        try {
            return processoProducaoPesadaApplicationService.avaliar(processoId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private PjbGovernancaInstitucionalNormativaAggregate avaliarGovernanca(Long processoId) {
        PjbGovernancaInstitucionalNormativaApplicationService service = governancaNormativaApplicationServiceProvider.getIfAvailable();
        if (service == null) {
            return null;
        }
        try {
            return service.avaliar(processoId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private PjbSubstituicaoFederativaWarRoomRamo localizarRamo(PjbSubstituicaoFederativaWarRoomTribunal tribunal, RamoDireito ramo) {
        if (tribunal == null || ramo == null) {
            return null;
        }
        return tribunal.ramos().stream()
                .filter(item -> ramo.name().equalsIgnoreCase(item.ramoCodigo()))
                .findFirst()
                .orElse(null);
    }

    private PjbSubstituicaoFederativaWarRoomRito localizarRito(PjbSubstituicaoFederativaWarRoomRamo ramo, RitoProcessual rito) {
        if (ramo == null || rito == null) {
            return null;
        }
        return ramo.ritos().stream()
                .filter(item -> rito.name().equalsIgnoreCase(item.ritoCodigo()))
                .findFirst()
                .orElse(null);
    }

    private boolean isEntregue(ExpedicaoJudicial expedicao) {
        return expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.ENTREGUE_CONFIRMADA
                || expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.LIDA_CONFIRMADA
                || expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.PRESUMIDA_ENTREGUE;
    }

    private boolean isFalha(ExpedicaoJudicial expedicao) {
        return expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.FRUSTRADA_DEFINITIVA
                || expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.DEVOLVIDA
                || expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.CANCELADA;
    }

    private List<Processo> carregarProcessosRecentes(int limit) {
        return processoRepository.findAll(PageRequest.of(0, Math.max(1, limit), Sort.by(
                Sort.Order.desc("dataUltimaMovimentacao"),
                Sort.Order.desc("id")
        ))).getContent();
    }

    private List<Processo> limitar(List<Processo> processos, int limit) {
        if (processos == null || processos.isEmpty()) {
            return List.of();
        }
        return processos.stream().limit(limit).toList();
    }

    private String competenciaKey(Processo processo) {
        return codigoRamo(processo.getRamoDireito()) + ':' + (processo.getRito() == null ? "SEM_RITO" : processo.getRito().name());
    }

    private String resolverCodigoTribunal(Processo processo) {
        String tribunal = normalize(Objects.toString(processo.getTribunal(), ""));
        if (!tribunal.isBlank() && NationalCompetenceMatrix.porCodigo(tribunal).isPresent()) {
            return tribunal;
        }
        RamoJusticaNacional ramo = mapRamoNacional(processo.getRamoDireito());
        return NationalCompetenceMatrix.resolver(processo.getUf(), ramo)
                .map(NationalCompetenceMatrix::codigo)
                .orElseGet(() -> tribunal.isBlank() ? "SEM_TRIBUNAL" : tribunal);
    }

    private RamoJusticaNacional mapRamoNacional(RamoDireito ramoDireito) {
        if (ramoDireito == null) {
            return RamoJusticaNacional.ESTADUAL;
        }
        return switch (ramoDireito.verticalPrincipal()) {
            case "ELEITORAL" -> RamoJusticaNacional.ELEITORAL;
            case "TRABALHISTA" -> RamoJusticaNacional.TRABALHO;
            case "FAZENDA" -> RamoJusticaNacional.FEDERAL;
            case "PENAL" -> ramoDireito == RamoDireito.MILITAR
                    ? RamoJusticaNacional.MILITAR_ESTADUAL
                    : RamoJusticaNacional.ESTADUAL;
            case "DIFUSO", "CIVEL" -> RamoJusticaNacional.ESTADUAL;
            default -> ramoDireito == RamoDireito.MILITAR
                    ? RamoJusticaNacional.MILITAR_ESTADUAL
                    : RamoJusticaNacional.ESTADUAL;
        };
    }

    private String codigoRamo(RamoDireito ramo) {
        return ramo == null ? "SEM_RAMO" : ramo.name();
    }

    private String descricaoRamo(RamoDireito ramo) {
        return ramo == null ? "Ramo não informado" : ramo.getDescricao();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record SigiloStats(long totalProcessosSigilosos, int aprovadas, int pendentes) {
    }
}
