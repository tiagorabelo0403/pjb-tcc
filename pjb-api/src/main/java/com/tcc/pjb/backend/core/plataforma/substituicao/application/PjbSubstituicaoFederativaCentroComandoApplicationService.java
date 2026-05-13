package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCentroComandoAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTribunal;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalProgramaAggregate;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorObservabilitySystemReport;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbSubstituicaoFederativaCentroComandoApplicationService {

    private static final Duration HORIZON = Duration.ofHours(24);

    private final PjbSubstituicaoNacionalProgramaApplicationService programaApplicationService;
    private final ObjectProvider<JudicialConnectorCommandCenterService> commandCenterServiceProvider;

    public PjbSubstituicaoFederativaCentroComandoApplicationService(
            PjbSubstituicaoNacionalProgramaApplicationService programaApplicationService,
            ObjectProvider<JudicialConnectorCommandCenterService> commandCenterServiceProvider) {
        this.programaApplicationService = Objects.requireNonNull(programaApplicationService);
        this.commandCenterServiceProvider = Objects.requireNonNull(commandCenterServiceProvider);
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaCentroComandoAggregate avaliar() {
        PjbSubstituicaoNacionalProgramaAggregate programa = programaApplicationService.avaliar();
        JudicialConnectorCommandCenterService commandCenterService = commandCenterServiceProvider.getIfAvailable();

        List<PjbSubstituicaoFederativaTribunal> tribunais = Arrays.stream(NationalCompetenceMatrix.values())
                .map(tribunal -> buildTribunal(programa, tribunal, commandCenterService))
                .sorted(Comparator
                        .comparing(PjbSubstituicaoFederativaTribunal::prontoRollout).reversed()
                        .thenComparing(PjbSubstituicaoFederativaTribunal::scoreProntidao, Comparator.reverseOrder())
                        .thenComparing(PjbSubstituicaoFederativaTribunal::tribunalCodigo))
                .toList();

        int prontosCorteAssistido = (int) tribunais.stream().filter(PjbSubstituicaoFederativaTribunal::prontoRollout).count();
        int tribunaisComBloqueio = (int) tribunais.stream().filter(tribunal -> !tribunal.bloqueadores().isEmpty()).count();
        int mediaTribunais = clamp((int) Math.round(tribunais.stream().mapToInt(PjbSubstituicaoFederativaTribunal::scoreProntidao).average().orElse(0)));
        int scoreNacional = clamp((int) Math.round(programa.scoreGeral() * 0.52d + mediaTribunais * 0.48d));

        boolean prontoRolloutFederativo = programa.prontoOperacaoAssistida() && prontosCorteAssistido >= Math.max(4, tribunais.size() / 6);
        boolean prontoRollbackGovernado = programa.buildGateAprovado() && tribunais.stream().filter(PjbSubstituicaoFederativaTribunal::prontoRollback).count() >= Math.max(6, tribunais.size() / 8);

        LinkedHashSet<String> pendenciasCriticas = new LinkedHashSet<>(programa.pendenciasCriticas());
        tribunais.stream()
                .flatMap(tribunal -> tribunal.bloqueadores().stream().limit(2))
                .limit(24)
                .forEach(pendenciasCriticas::add);

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("programa.scoreGeral=" + programa.scoreGeral());
        fundamentos.add("programa.prontoOperacaoAssistida=" + programa.prontoOperacaoAssistida());
        fundamentos.add("programa.prontoCutoverNacional=" + programa.prontoCutoverNacional());
        fundamentos.add("centroComando.mediaTribunais=" + mediaTribunais);
        fundamentos.add("centroComando.tribunaisMonitorados=" + tribunais.size());
        fundamentos.add("centroComando.tribunaisProntosCorteAssistido=" + prontosCorteAssistido);
        fundamentos.add("centroComando.tribunaisComBloqueio=" + tribunaisComBloqueio);
        fundamentos.add("centroComando.prontoRolloutFederativo=" + prontoRolloutFederativo);
        fundamentos.add("centroComando.prontoRollbackGovernado=" + prontoRollbackGovernado);

        return new PjbSubstituicaoFederativaCentroComandoAggregate(
                scoreNacional,
                prontoRolloutFederativo,
                prontoRollbackGovernado,
                tribunais.size(),
                prontosCorteAssistido,
                tribunaisComBloqueio,
                List.copyOf(pendenciasCriticas.stream().limit(30).toList()),
                tribunais,
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoFederativaTribunal avaliarTribunal(String tribunalCodigo) {
        String normalized = normalizeCode(tribunalCodigo);
        NationalCompetenceMatrix tribunal = NationalCompetenceMatrix.porCodigo(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Tribunal não mapeado para rollout federativo: " + normalized));
        return buildTribunal(programaApplicationService.avaliar(), tribunal, commandCenterServiceProvider.getIfAvailable());
    }

    private PjbSubstituicaoFederativaTribunal buildTribunal(PjbSubstituicaoNacionalProgramaAggregate programa,
                                                            NationalCompetenceMatrix tribunal,
                                                            JudicialConnectorCommandCenterService commandCenterService) {
        JudicialConnectorCommandCenterReport report = commandCenterService == null ? null : commandCenterService.tribunalReport(tribunal.codigo(), HORIZON);
        int tribunalReady = report != null && report.controlPlane() != null ? report.controlPlane().tribunalReadySystems().size() : 0;
        int productionReady = report != null && report.controlPlane() != null ? report.controlPlane().productionReadySystems().size() : 0;
        int healthySystems = report != null && report.observability() != null ? report.observability().healthySystems() : 0;
        int degradedSystems = report != null && report.observability() != null ? report.observability().degradedSystems() : 0;
        int blockedSystems = report != null && report.observability() != null ? report.observability().blockedSystems() : 0;
        int dataReadySystems = report != null && report.dataPlane() != null ? report.dataPlane().readySystems().size() : 0;
        int certificateReady = report != null && report.cryptography() != null ? report.cryptography().certificateReadyCount() : 0;
        int strongAuthentication = report != null && report.cryptography() != null ? report.cryptography().strongAuthenticationCount() : 0;
        int cryptoBlocked = report != null && report.cryptography() != null ? report.cryptography().blockedCount() : 0;

        int connectorScore = clamp((tribunalReady * 18)
                + (productionReady * 16)
                + (healthySystems * 8)
                + (dataReadySystems * 7)
                + (certificateReady * 4)
                + (strongAuthentication * 3)
                - (blockedSystems * 18)
                - (cryptoBlocked * 10)
                - (degradedSystems * 4));
        int score = clamp((int) Math.round(programa.scoreGeral() * 0.42d + connectorScore * 0.58d));

        boolean prontoRollout = programa.prontoOperacaoAssistida()
                && tribunalReady > 0
                && blockedSystems == 0
                && cryptoBlocked == 0
                && score >= 82;
        boolean prontoRollback = programa.buildGateAprovado()
                && tribunalReady > 0
                && blockedSystems <= 1
                && score >= 74;

        String ondaAtual = prontoRollout && programa.prontoCutoverNacional()
                ? "cutover-nacional"
                : prontoRollout
                ? "operacao-assistida"
                : "shadow-mode-governado";

        LinkedHashSet<String> bloqueadores = new LinkedHashSet<>();
        if (report != null && report.alerts() != null) {
            report.alerts().stream()
                    .filter(alert -> alert != null && !alert.isBlank())
                    .limit(10)
                    .forEach(bloqueadores::add);
        }
        if (tribunalReady == 0) {
            bloqueadores.add("ROLL_OUT_NO_TRIBUNAL_READY_SYSTEM");
        }
        if (blockedSystems > 0) {
            bloqueadores.add("ROLL_OUT_TRIBUNAL_BLOCKED_SYSTEMS=" + blockedSystems);
        }
        if (cryptoBlocked > 0) {
            bloqueadores.add("ROLL_OUT_TRIBUNAL_CRYPTO_BLOCKED=" + cryptoBlocked);
        }
        if (!programa.buildGateAprovado()) {
            bloqueadores.add("ROLL_OUT_BUILD_GATE_NOT_APPROVED");
        }

        List<String> guardrails = List.of(
                "Corte por rito e unidade com aprovação federativa explícita",
                "Overlay reversível para retorno imediato ao legado " + tribunal.connectorPreferido().name(),
                "Reconciliação de eventos e metadados antes de ampliar a onda",
                "Trilha probatória nacional preservada por tribunal e janela operacional"
        );
        List<String> rollback = List.of(
                "Reabrir roteamento para o legado " + tribunal.connectorPreferido().name() + " sem perda de protocolo",
                "Reprocessar fila idempotente a partir do ledger institucional do tribunal",
                "Suspender apenas a onda local sem contaminar outros tribunais",
                "Comparar snapshots PJB x legado até restabelecer divergência zero"
        );
        List<String> proximasAcoes = prontoRollout
                ? List.of(
                        "Agendar janela assistida para " + tribunal.codigo(),
                        "Ampliar corte por ramo " + tribunal.ramo().name(),
                        "Manter war room federativo com prova de reversibilidade"
                )
                : List.of(
                        "Elevar sistemas prontos para submissão real em " + tribunal.codigo(),
                        "Remover bloqueadores de observabilidade e criptografia do tribunal",
                        "Validar rollback overlay antes de qualquer corte assistido"
                );

        List<String> sistemasSaudaveis = report != null && report.observability() != null && report.observability().systems() != null
                ? report.observability().systems().stream()
                .filter(system -> "HEALTHY".equals(system.observabilityStatus()))
                .map(system -> system.system() != null ? system.system().name() : "OUTRO")
                .toList()
                : List.of();

        return new PjbSubstituicaoFederativaTribunal(
                tribunal.codigo(),
                tribunal.nome(),
                tribunal.ramo().name(),
                tribunal.connectorPreferido().name(),
                tribunal.sistemaJudicialFallback().name(),
                ondaAtual,
                resolveStatus(score, prontoRollout, bloqueadores.size()),
                score,
                prontoRollout,
                prontoRollback,
                report != null && report.controlPlane() != null ? report.controlPlane().tribunalReadySystems() : List.of(),
                sistemasSaudaveis,
                guardrails,
                rollback,
                List.copyOf(bloqueadores),
                proximasAcoes
        );
    }

    private PjbFechamentoStatus resolveStatus(int score, boolean pronta, int bloqueadores) {
        if (pronta) {
            return PjbFechamentoStatus.CONCLUIDA;
        }
        if (score >= 78 && bloqueadores <= 2) {
            return PjbFechamentoStatus.PARCIAL;
        }
        return PjbFechamentoStatus.PENDENTE;
    }

    private String normalizeCode(String tribunalCodigo) {
        return Objects.toString(tribunalCodigo, "").trim().toUpperCase(Locale.ROOT);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
