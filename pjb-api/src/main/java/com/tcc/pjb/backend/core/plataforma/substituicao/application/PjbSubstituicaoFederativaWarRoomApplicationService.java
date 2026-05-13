package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix.RamoJusticaNacional;
import com.tcc.pjb.backend.core.governance.institucional.application.PjbGovernancaInstitucionalNormativaApplicationService;
import com.tcc.pjb.backend.core.governance.institucional.domain.PjbGovernancaInstitucionalNormativaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTribunal;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomRamo;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomRito;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomTribunal;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalProgramaAggregate;
import com.tcc.pjb.backend.core.processo.operacao.application.ProcessoOperacaoApplicationService;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoProducaoPesadaApplicationService;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoProducaoPesadaAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
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
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbSubstituicaoFederativaWarRoomApplicationService {

    private static final int NATIONAL_SAMPLE_SIZE = 640;
    private static final int TRIBUNAL_SAMPLE_SIZE = 72;
    private static final int RAMO_SAMPLE_SIZE = 12;
    private static final int RITO_SAMPLE_SIZE = 3;

    private final PjbSubstituicaoFederativaCentroComandoApplicationService centroComandoApplicationService;
    private final PjbSubstituicaoNacionalProgramaApplicationService programaApplicationService;
    private final ProcessoRepository processoRepository;
    private final ProcessoOperacaoApplicationService processoOperacaoApplicationService;
    private final ProcessoProducaoPesadaApplicationService processoProducaoPesadaApplicationService;
    private final ObjectProvider<PjbGovernancaInstitucionalNormativaApplicationService> governancaInstitucionalApplicationServiceProvider;

    public PjbSubstituicaoFederativaWarRoomApplicationService(
            PjbSubstituicaoFederativaCentroComandoApplicationService centroComandoApplicationService,
            PjbSubstituicaoNacionalProgramaApplicationService programaApplicationService,
            ProcessoRepository processoRepository,
            ProcessoOperacaoApplicationService processoOperacaoApplicationService,
            ProcessoProducaoPesadaApplicationService processoProducaoPesadaApplicationService,
            ObjectProvider<PjbGovernancaInstitucionalNormativaApplicationService> governancaInstitucionalApplicationServiceProvider) {
        this.centroComandoApplicationService = Objects.requireNonNull(centroComandoApplicationService);
        this.programaApplicationService = Objects.requireNonNull(programaApplicationService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoOperacaoApplicationService = Objects.requireNonNull(processoOperacaoApplicationService);
        this.processoProducaoPesadaApplicationService = Objects.requireNonNull(processoProducaoPesadaApplicationService);
        this.governancaInstitucionalApplicationServiceProvider = Objects.requireNonNull(governancaInstitucionalApplicationServiceProvider);
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaWarRoomAggregate avaliar() {
        PjbSubstituicaoNacionalProgramaAggregate programa = programaApplicationService.avaliar();
        List<PjbSubstituicaoFederativaTribunal> baseTribunais = centroComandoApplicationService.avaliar().tribunais();
        Map<String, List<Processo>> processosPorTribunal = carregarProcessosRecentes(NATIONAL_SAMPLE_SIZE).stream()
                .collect(Collectors.groupingBy(this::resolverCodigoTribunal, LinkedHashMap::new, Collectors.toList()));

        List<PjbSubstituicaoFederativaWarRoomTribunal> tribunais = baseTribunais.stream()
                .map(tribunal -> buildTribunal(programa, tribunal, processosPorTribunal.getOrDefault(tribunal.tribunalCodigo(), List.of())))
                .sorted(Comparator
                        .comparing(PjbSubstituicaoFederativaWarRoomTribunal::janelaAberta).reversed()
                        .thenComparing(PjbSubstituicaoFederativaWarRoomTribunal::corteLiberado).reversed()
                        .thenComparing(PjbSubstituicaoFederativaWarRoomTribunal::scoreProntidao, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaWarRoomTribunal::tribunalCodigo))
                .toList();

        int scoreMedioTribunais = clamp((int) Math.round(tribunais.stream().mapToInt(PjbSubstituicaoFederativaWarRoomTribunal::scoreProntidao).average().orElse(0d)));
        int tribunaisComJanelaAberta = (int) tribunais.stream().filter(PjbSubstituicaoFederativaWarRoomTribunal::janelaAberta).count();
        int tribunaisEmFreeze = (int) tribunais.stream().filter(PjbSubstituicaoFederativaWarRoomTribunal::freezeAtivo).count();
        boolean freezeNacionalAtivo = !programa.buildGateAprovado() || tribunaisEmFreeze >= Math.max(4, tribunais.size() / 5);
        boolean prontoCorteControlado = !freezeNacionalAtivo
                && programa.prontoOperacaoAssistida()
                && tribunaisComJanelaAberta >= Math.max(3, tribunais.size() / 7);
        int scoreGeral = clamp((int) Math.round(programa.scoreGeral() * 0.38d + scoreMedioTribunais * 0.62d));

        LinkedHashSet<String> bloqueadoresCriticos = new LinkedHashSet<>(programa.pendenciasCriticas());
        tribunais.stream()
                .flatMap(tribunal -> tribunal.bloqueadores().stream().limit(3))
                .limit(36)
                .forEach(bloqueadoresCriticos::add);

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("warRoom.programa.scoreGeral=" + programa.scoreGeral());
        fundamentos.add("warRoom.programa.prontoOperacaoAssistida=" + programa.prontoOperacaoAssistida());
        fundamentos.add("warRoom.scoreMedioTribunais=" + scoreMedioTribunais);
        fundamentos.add("warRoom.tribunaisComJanelaAberta=" + tribunaisComJanelaAberta);
        fundamentos.add("warRoom.tribunaisEmFreeze=" + tribunaisEmFreeze);
        fundamentos.add("warRoom.freezeNacionalAtivo=" + freezeNacionalAtivo);
        fundamentos.add("warRoom.prontoCorteControlado=" + prontoCorteControlado);
        fundamentos.add("warRoom.processosAmostrados=" + processosPorTribunal.values().stream().mapToInt(List::size).sum());

        return new PjbSubstituicaoFederativaWarRoomAggregate(
                scoreGeral,
                freezeNacionalAtivo,
                prontoCorteControlado,
                tribunaisComJanelaAberta,
                tribunaisEmFreeze,
                List.copyOf(bloqueadoresCriticos.stream().limit(40).toList()),
                tribunais,
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaWarRoomTribunal avaliarTribunal(String tribunalCodigo) {
        PjbSubstituicaoNacionalProgramaAggregate programa = programaApplicationService.avaliar();
        PjbSubstituicaoFederativaTribunal tribunal = centroComandoApplicationService.avaliarTribunal(tribunalCodigo);
        List<Processo> processos = carregarProcessosRecentes(TRIBUNAL_SAMPLE_SIZE * 4).stream()
                .filter(processo -> tribunal.tribunalCodigo().equals(resolverCodigoTribunal(processo)))
                .limit(TRIBUNAL_SAMPLE_SIZE)
                .toList();
        return buildTribunal(programa, tribunal, processos);
    }

    private PjbSubstituicaoFederativaWarRoomTribunal buildTribunal(PjbSubstituicaoNacionalProgramaAggregate programa,
                                                                   PjbSubstituicaoFederativaTribunal tribunal,
                                                                   List<Processo> processos) {
        List<Processo> amostraTribunal = limitarProcessos(processos, TRIBUNAL_SAMPLE_SIZE);
        Map<RamoDireito, List<Processo>> processosPorRamo = amostraTribunal.stream()
                .filter(processo -> processo.getRamoDireito() != null)
                .collect(Collectors.groupingBy(Processo::getRamoDireito, LinkedHashMap::new, Collectors.toList()));

        List<PjbSubstituicaoFederativaWarRoomRamo> ramos = processosPorRamo.entrySet().stream()
                .sorted(Comparator.<Map.Entry<RamoDireito, List<Processo>>>comparingInt(entry -> entry.getValue().size()).reversed()
                        .thenComparing(entry -> entry.getKey().name()))
                .limit(6)
                .map(entry -> buildRamo(programa, tribunal, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaWarRoomRamo::freezeAtivo)
                        .thenComparing(PjbSubstituicaoFederativaWarRoomRamo::score, Comparator.reverseOrder())
                        .reversed())
                .toList();

        int scoreRamos = clamp((int) Math.round(ramos.stream().mapToInt(PjbSubstituicaoFederativaWarRoomRamo::score).average().orElse(tribunal.scoreProntidao())));
        int scoreProntidao = clamp((int) Math.round(tribunal.scoreProntidao() * 0.52d + scoreRamos * 0.48d));
        boolean freezeAtivo = !programa.buildGateAprovado()
                || !tribunal.prontoRollback()
                || ramos.stream().filter(PjbSubstituicaoFederativaWarRoomRamo::freezeAtivo).count() >= Math.max(1, ramos.size() / 2);
        boolean corteLiberado = tribunal.prontoRollout()
                && !freezeAtivo
                && ramos.stream().anyMatch(PjbSubstituicaoFederativaWarRoomRamo::corteLiberado);
        boolean janelaAberta = corteLiberado || tribunal.prontoRollout() && !freezeAtivo;
        String janelaAtual = freezeAtivo
                ? "freeze-federativo"
                : corteLiberado
                ? "janela-corte-controlado"
                : tribunal.prontoRollout()
                ? "janela-operacao-assistida"
                : "janela-shadow-governado";

        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>(tribunal.bloqueadores());
        if (ramos.isEmpty()) {
            bloqueadores.add("WAR_ROOM_SEM_AMOSTRA_PROCESSUAL_TRIBUNAL");
        }
        ramos.stream()
                .filter(PjbSubstituicaoFederativaWarRoomRamo::freezeAtivo)
                .flatMap(ramo -> ramo.evidencias().stream().limit(2))
                .limit(16)
                .forEach(bloqueadores::add);

        LinkedHashSet<String> guardrails = new LinkedHashSet<>(tribunal.guardrails());
        guardrails.add("Janela por ramo e rito somente com observabilidade processual viva");
        guardrails.add("Corte condicionado a prontidão de produção pesada do processo de referência");
        guardrails.add("Freeze automático ao detectar bloqueio recorrente em rito sensível");
        LinkedHashSet<String> rollback = new LinkedHashSet<>(tribunal.rollback());
        rollback.add("Reabrir overlay por rito e reidratar fila processual do tribunal");
        rollback.add("Isolar apenas o ramo degradado, preservando ondas saudáveis do mesmo tribunal");

        List<String> proximasAcoes = corteLiberado
                ? List.of(
                        "Executar corte controlado por ramo prioritário em " + tribunal.tribunalCodigo(),
                        "Monitorar ritos com maior score durante a janela atual",
                        "Preparar reabertura graduada do backlog após estabilidade da janela"
                )
                : List.of(
                        "Elevar amostra processual com produção pesada aprovada em " + tribunal.tribunalCodigo(),
                        "Corrigir ramos em freeze antes de abrir nova janela",
                        "Validar corte e rollback por rito com mais processos de referência"
                );

        return new PjbSubstituicaoFederativaWarRoomTribunal(
                tribunal.tribunalCodigo(),
                tribunal.tribunalNome(),
                tribunal.ramoJustica(),
                tribunal.ondaAtual(),
                tribunal.status().name(),
                scoreProntidao,
                janelaAberta,
                freezeAtivo,
                corteLiberado,
                janelaAtual,
                ramos,
                List.copyOf(guardrails),
                List.copyOf(rollback),
                List.copyOf(bloqueadores.stream().limit(24).toList()),
                proximasAcoes
        );
    }

    private PjbSubstituicaoFederativaWarRoomRamo buildRamo(PjbSubstituicaoNacionalProgramaAggregate programa,
                                                           PjbSubstituicaoFederativaTribunal tribunal,
                                                           RamoDireito ramo,
                                                           List<Processo> processos) {
        List<Processo> amostraRamo = limitarProcessos(processos, RAMO_SAMPLE_SIZE);
        Map<RitoProcessual, List<Processo>> processosPorRito = amostraRamo.stream()
                .filter(processo -> processo.getRito() != null)
                .collect(Collectors.groupingBy(Processo::getRito, LinkedHashMap::new, Collectors.toList()));

        List<PjbSubstituicaoFederativaWarRoomRito> ritos = processosPorRito.entrySet().stream()
                .sorted(Comparator.<Map.Entry<RitoProcessual, List<Processo>>>comparingInt(entry -> entry.getValue().size()).reversed()
                        .thenComparing(entry -> entry.getKey().name()))
                .limit(5)
                .map(entry -> buildRito(programa, tribunal, ramo, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PjbSubstituicaoFederativaWarRoomRito::freezeAtivo)
                        .thenComparing(PjbSubstituicaoFederativaWarRoomRito::score, Comparator.reverseOrder())
                        .reversed())
                .toList();

        int score = clamp((int) Math.round(ritos.stream().mapToInt(PjbSubstituicaoFederativaWarRoomRito::score).average().orElse(tribunal.scoreProntidao())));
        boolean freezeAtivo = !programa.buildGateAprovado()
                || ritos.stream().filter(PjbSubstituicaoFederativaWarRoomRito::freezeAtivo).count() >= Math.max(1, ritos.size() / 2)
                || ramo.isPenalLike() && !tribunal.prontoRollback();
        boolean corteLiberado = tribunal.prontoRollout()
                && !freezeAtivo
                && ritos.stream().anyMatch(PjbSubstituicaoFederativaWarRoomRito::corteLiberado);
        String janelaAtual = resolveJanelaRamo(ramo, freezeAtivo, corteLiberado, tribunal.prontoRollout());

        LinkedHashSet<String> evidencias = new LinkedHashSet<>();
        evidencias.add("ramo.amostra=" + amostraRamo.size());
        evidencias.add("ramo.ritosMaterializados=" + ritos.size());
        evidencias.add("ramo.vertical=" + ramo.verticalPrincipal());
        if (ramo.exigeAtuacaoMP()) {
            evidencias.add("ramo.exigeAtuacaoMP=true");
        }
        if (ramo.geraSigiloAutomatico()) {
            evidencias.add("ramo.sigiloAutomatico=true");
        }
        ritos.stream().flatMap(rito -> rito.alertas().stream().limit(1)).limit(8).forEach(evidencias::add);

        LinkedHashSet<String> acoes = new LinkedHashSet<>();
        ritos.stream().flatMap(rito -> rito.acoesImediatas().stream().limit(2)).limit(10).forEach(acoes::add);
        if (acoes.isEmpty()) {
            acoes.add("AMPLIAR_AMOSTRA_OPERACIONAL_DO_RAMO");
        }

        return new PjbSubstituicaoFederativaWarRoomRamo(
                ramo.name(),
                ramo.getDescricao(),
                score,
                corteLiberado,
                freezeAtivo,
                janelaAtual,
                ritos,
                List.copyOf(evidencias),
                List.copyOf(acoes)
        );
    }

    private PjbSubstituicaoFederativaWarRoomRito buildRito(PjbSubstituicaoNacionalProgramaAggregate programa,
                                                           PjbSubstituicaoFederativaTribunal tribunal,
                                                           RamoDireito ramo,
                                                           RitoProcessual rito,
                                                           List<Processo> processos) {
        List<Processo> amostraRito = limitarProcessos(processos, RITO_SAMPLE_SIZE);
        Processo referencia = amostraRito.isEmpty() ? null : amostraRito.getFirst();
        ProcessoOperacaoAggregate operacao = referencia == null ? null : safeOperacao(referencia.getId());
        ProcessoProducaoPesadaAggregate producao = referencia == null ? null : safeProducao(referencia.getId());
        PjbGovernancaInstitucionalNormativaAggregate governanca = referencia == null ? null : safeGovernanca(referencia.getId());

        int scoreOperacao = operacao == null ? tribunal.scoreProntidao() : scoreOperacao(operacao);
        int scoreProducao = producao == null ? tribunal.scoreProntidao() : producao.scoreGeral();
        int scoreGovernanca = governanca == null ? tribunal.scoreProntidao() : governanca.scoreGeral();
        int score = clamp((int) Math.round(tribunal.scoreProntidao() * 0.22d
                + scoreOperacao * 0.34d
                + scoreProducao * 0.30d
                + scoreGovernanca * 0.14d));

        boolean freezeAtivo = !programa.buildGateAprovado()
                || operacao == null
                || producao == null
                || operacao.totalBloqueios() > 0
                || !producao.prontoProducaoPesada()
                || governanca != null && !governanca.prontoGovernanca();
        boolean corteLiberado = tribunal.prontoRollout()
                && !freezeAtivo
                && "READY".equalsIgnoreCase(operacao == null ? "NOT_READY" : operacao.readiness())
                && producao != null
                && producao.prontoProducaoPesada()
                && score >= (ramo.isPenalLike() ? 88 : 80);
        String janelaAtual = resolveJanelaRito(rito, ramo, freezeAtivo, corteLiberado, tribunal.prontoRollout());

        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (operacao != null) {
            alertas.addAll(operacao.alertas());
        }
        if (producao != null) {
            alertas.addAll(producao.bloqueios());
        }
        if (governanca != null) {
            alertas.addAll(governanca.pendencias());
        }
        if (alertas.isEmpty() && referencia == null) {
            alertas.add("WAR_ROOM_SEM_PROCESSO_REFERENCIA_RITO");
        }

        LinkedHashSet<String> acoes = new LinkedHashSet<>();
        if (operacao != null) {
            acoes.addAll(operacao.acoesImediatas());
        }
        if (producao != null && !producao.prontoProducaoPesada()) {
            acoes.addAll(producao.bloqueios());
        }
        if (governanca != null && !governanca.prontoGovernanca()) {
            acoes.addAll(governanca.pendencias());
        }
        if (acoes.isEmpty()) {
            acoes.add(corteLiberado ? "MANTER_JANELA_VIVA_DO_RITO" : "ESTABILIZAR_REFERENCIA_DO_RITO");
        }

        return new PjbSubstituicaoFederativaWarRoomRito(
                rito.name(),
                score,
                operacao == null ? "NOT_READY" : operacao.readiness(),
                operacao == null ? "FRAGIL" : operacao.resilienceState(),
                operacao == null ? "CRITICAL" : operacao.observabilityState(),
                janelaAtual,
                corteLiberado,
                freezeAtivo,
                List.copyOf(alertas.stream().limit(12).toList()),
                List.copyOf(acoes.stream().limit(12).toList()),
                referencia == null ? null : referencia.getId(),
                referencia == null ? "" : Objects.toString(referencia.getNumeroCNJ(), Objects.toString(referencia.getNumeroUnificado(), ""))
        );
    }

    private int scoreOperacao(ProcessoOperacaoAggregate operacao) {
        int readiness = switch (operacao.readiness()) {
            case "READY" -> 92;
            case "PARTIAL_READY" -> 74;
            default -> 46;
        };
        int resilience = switch (operacao.resilienceState()) {
            case "FORTE" -> 90;
            case "OBSERVAR" -> 72;
            default -> 52;
        };
        int observability = switch (operacao.observabilityState()) {
            case "STABLE" -> 90;
            case "ATTENTION" -> 68;
            default -> 42;
        };
        int migration = switch (operacao.migrationState()) {
            case "READY" -> 88;
            case "PARTIAL_READY" -> 70;
            default -> 48;
        };
        int penalty = clamp((int) Math.round(operacao.totalBloqueios() * 9d + operacao.saturacaoMaxima() * 0.18d));
        return clamp((int) Math.round(readiness * 0.34d + resilience * 0.24d + observability * 0.22d + migration * 0.20d - penalty));
    }

    private ProcessoOperacaoAggregate safeOperacao(Long processoId) {
        try {
            return processoOperacaoApplicationService.detalhar(processoId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private ProcessoProducaoPesadaAggregate safeProducao(Long processoId) {
        try {
            return processoProducaoPesadaApplicationService.avaliar(processoId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private PjbGovernancaInstitucionalNormativaAggregate safeGovernanca(Long processoId) {
        PjbGovernancaInstitucionalNormativaApplicationService service = governancaInstitucionalApplicationServiceProvider.getIfAvailable();
        if (service == null) {
            return null;
        }
        try {
            return service.avaliar(processoId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<Processo> carregarProcessosRecentes(int limit) {
        return processoRepository.findAll(PageRequest.of(0, Math.max(1, limit), Sort.by(
                Sort.Order.desc("dataUltimaMovimentacao"),
                Sort.Order.desc("id")
        ))).getContent();
    }

    private List<Processo> limitarProcessos(List<Processo> processos, int limit) {
        if (processos == null || processos.isEmpty()) {
            return List.of();
        }
        return processos.stream().limit(limit).toList();
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

    private String resolveJanelaRamo(RamoDireito ramo, boolean freezeAtivo, boolean corteLiberado, boolean tribunalProntoRollout) {
        if (freezeAtivo) {
            if (ramo.isPenalLike() || ramo.geraSigiloAutomatico()) {
                return "freeze-rigor-penal";
            }
            return "freeze-controlado";
        }
        if (corteLiberado) {
            if (ramo == RamoDireito.TRABALHISTA) {
                return "janela-corte-lote-trabalhista";
            }
            if (ramo.isFazendaLike()) {
                return "janela-corte-fazenda-controlada";
            }
            return "janela-corte-controlado";
        }
        return tribunalProntoRollout ? "janela-validacao-assistida" : "janela-shadow-ramo";
    }

    private String resolveJanelaRito(RitoProcessual rito,
                                     RamoDireito ramo,
                                     boolean freezeAtivo,
                                     boolean corteLiberado,
                                     boolean tribunalProntoRollout) {
        if (freezeAtivo) {
            if (rito.isPenal() || ramo.isPenalLike() || ramo.geraSigiloAutomatico()) {
                return "freeze-rito-sensivel";
            }
            if (rito.isEleitoral()) {
                return "freeze-janela-eleitoral";
            }
            if (rito.isMilitar()) {
                return "freeze-janela-militar";
            }
            return "freeze-rito-controlado";
        }
        if (corteLiberado) {
            if (rito.isPenal()) {
                return "janela-corte-penal-supervisionado";
            }
            if (rito.isEleitoral()) {
                return "janela-corte-eleitoral-controlado";
            }
            if (rito.isTrabalhista()) {
                return "janela-corte-trabalhista-assistido";
            }
            if (rito.isTribFazenda() || rito.isPrevidenciario()) {
                return "janela-corte-fazenda-assistido";
            }
            return "janela-corte-controlado";
        }
        return tribunalProntoRollout ? "janela-observacao-rito" : "janela-shadow-rito";
    }

    private String normalize(String value) {
        return Objects.toString(value, "").trim().toUpperCase(Locale.ROOT);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
