package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoNacionalAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalOnda;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalProgramaAggregate;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterService;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbSubstituicaoNacionalProgramaApplicationService {

    private final PjbArquiteturaSubstituicaoNacionalApplicationService arquiteturaApplicationService;
    private final ObjectProvider<JudicialConnectorCommandCenterService> judicialConnectorCommandCenterServiceProvider;

    public PjbSubstituicaoNacionalProgramaApplicationService(
            PjbArquiteturaSubstituicaoNacionalApplicationService arquiteturaApplicationService,
            ObjectProvider<JudicialConnectorCommandCenterService> judicialConnectorCommandCenterServiceProvider) {
        this.arquiteturaApplicationService = Objects.requireNonNull(arquiteturaApplicationService);
        this.judicialConnectorCommandCenterServiceProvider = Objects.requireNonNull(judicialConnectorCommandCenterServiceProvider);
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoNacionalProgramaAggregate avaliar() {
        PjbArquiteturaSubstituicaoNacionalAggregate arquitetura = arquiteturaApplicationService.avaliar();
        JudicialConnectorCommandCenterReport commandCenter = resolveCommandCenter();

        int conectoresOperacionais = commandCenter != null && commandCenter.governance() != null
                ? commandCenter.governance().operationalConnectorCount()
                : 0;
        int conectoresSaudaveis = commandCenter != null && commandCenter.observability() != null
                ? commandCenter.observability().healthySystems()
                : 0;
        int conectoresBloqueados = commandCenter != null && commandCenter.observability() != null
                ? commandCenter.observability().blockedSystems()
                : 0;
        int sistemasProntosProducao = commandCenter != null && commandCenter.controlPlane() != null
                ? commandCenter.controlPlane().productionReadySystems().size()
                : 0;

        PjbArquiteturaSubstituicaoPilar interoperabilidade = findPilar(arquitetura, "interoperabilidade-migracao");
        PjbArquiteturaSubstituicaoPilar confiabilidade = findPilar(arquitetura, "confiabilidade-institucional");
        PjbArquiteturaSubstituicaoPilar governanca = findPilar(arquitetura, "governanca-nacional");
        PjbArquiteturaSubstituicaoPilar motor = findPilar(arquitetura, "motor-processual-nacional");

        PjbSubstituicaoNacionalOnda sombra = ondaSombra(interoperabilidade, governanca, commandCenter, arquitetura);
        PjbSubstituicaoNacionalOnda assistida = ondaOperacaoAssistida(arquitetura, confiabilidade, governanca, conectoresOperacionais, conectoresSaudaveis, conectoresBloqueados, sistemasProntosProducao);
        PjbSubstituicaoNacionalOnda cutover = ondaCutover(arquitetura, motor, interoperabilidade, confiabilidade, governanca, conectoresBloqueados, sistemasProntosProducao);
        List<PjbSubstituicaoNacionalOnda> ondas = List.of(sombra, assistida, cutover);

        int scoreGeral = clamp((int) Math.round((arquitetura.scoreGeral() * 0.55d) + (ondas.stream().mapToInt(PjbSubstituicaoNacionalOnda::score).average().orElse(0) * 0.45d)));
        boolean prontoOperacaoAssistida = assistida.pronta();
        boolean prontoCutoverNacional = cutover.pronta();

        LinkedHashSet<String> pendenciasCriticas = new LinkedHashSet<>();
        ondas.stream()
                .filter(onda -> !onda.pronta())
                .flatMap(onda -> onda.proximasAcoes().stream())
                .forEach(pendenciasCriticas::add);
        if (commandCenter != null && commandCenter.alerts() != null) {
            commandCenter.alerts().stream().limit(12).forEach(pendenciasCriticas::add);
        }

        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("arquitetura.scoreGeral=" + arquitetura.scoreGeral());
        fundamentos.add("arquitetura.buildGateAprovado=" + arquitetura.buildGateAprovado());
        fundamentos.add("arquitetura.prontoSubstituicaoImediata=" + arquitetura.prontoParaSubstituicaoImediata());
        fundamentos.add("connectors.operational=" + conectoresOperacionais);
        fundamentos.add("connectors.healthy=" + conectoresSaudaveis);
        fundamentos.add("connectors.blocked=" + conectoresBloqueados);
        fundamentos.add("connectors.productionReady=" + sistemasProntosProducao);
        ondas.forEach(onda -> fundamentos.add("onda." + onda.codigo() + "=" + onda.status().name() + ":" + onda.score()));
        if (commandCenter != null) {
            fundamentos.add("commandCenter.alertCount=" + (commandCenter.alerts() == null ? 0 : commandCenter.alerts().size()));
        }

        String conclusao = prontoCutoverNacional
                ? "O PJB já sustenta programa nacional de cutover governado, com guardrails de reversibilidade, governança e conectores em postura operacional suficiente para ondas nacionais controladas."
                : prontoOperacaoAssistida
                ? "O PJB já comporta operação assistida e shadow mode sério; o próximo fechamento crítico é converter essa maturidade em cutover nacional sem bloqueadores ativos."
                : "O PJB já é candidato real a sistema unificado, mas ainda precisa fechar conectores, governança de rollout, produção observável e critérios de reversibilidade antes do corte nacional.";

        return new PjbSubstituicaoNacionalProgramaAggregate(
                scoreGeral,
                prontoOperacaoAssistida,
                prontoCutoverNacional,
                arquitetura.buildGateAprovado(),
                conectoresOperacionais,
                conectoresBloqueados,
                conectoresSaudaveis,
                sistemasProntosProducao,
                ondas,
                List.copyOf(pendenciasCriticas.stream().limit(20).toList()),
                conclusao,
                List.copyOf(fundamentos),
                Instant.now()
        );
    }

    private JudicialConnectorCommandCenterReport resolveCommandCenter() {
        JudicialConnectorCommandCenterService commandCenterService = judicialConnectorCommandCenterServiceProvider.getIfAvailable();
        return commandCenterService == null ? null : commandCenterService.nationalReport(Duration.ofHours(24));
    }

    private PjbArquiteturaSubstituicaoPilar findPilar(PjbArquiteturaSubstituicaoNacionalAggregate arquitetura, String codigo) {
        return arquitetura.pilares().stream()
                .filter(pilar -> codigo.equalsIgnoreCase(pilar.codigo()))
                .findFirst()
                .orElse(new PjbArquiteturaSubstituicaoPilar(codigo, codigo, PjbFechamentoStatus.PENDENTE, 0, false, List.of(), List.of("Fechar leitura arquitetural do pilar " + codigo)));
    }

    private PjbSubstituicaoNacionalOnda ondaSombra(PjbArquiteturaSubstituicaoPilar interoperabilidade,
                                                   PjbArquiteturaSubstituicaoPilar governanca,
                                                   JudicialConnectorCommandCenterReport commandCenter,
                                                   PjbArquiteturaSubstituicaoNacionalAggregate arquitetura) {
        int score = clamp((int) Math.round(interoperabilidade.score() * 0.6d + governanca.score() * 0.25d + (arquitetura.buildGateAprovado() ? 15 : 0)));
        boolean pronta = interoperabilidade.score() >= 80 && governanca.score() >= 75 && arquitetura.buildGateAprovado();
        List<String> criterios = List.of(
                "Build gate nacional aprovado",
                "Camada de convivência e shadow mode com score >= 80",
                "Governança nacional com trilha de decisão e reversibilidade operacional"
        );
        List<String> blocos = List.of(
                "Espelhamento de fluxos processuais por ramo e rito",
                "Leitura dupla de eventos e reconciliação de divergências",
                "Medição de latência, falha e aderência sem corte do legado"
        );
        LinkedHashSet<String> guardrails = new LinkedHashSet<>();
        guardrails.add("Sem desligamento do legado nesta onda");
        guardrails.add("Comparação de resultado com tolerância auditável por tribunal e rito");
        if (commandCenter != null && commandCenter.alerts() != null) {
            commandCenter.alerts().stream().limit(5).forEach(guardrails::add);
        }
        List<String> rollback = List.of(
                "Preservar legado como sistema de verdade durante a sombra",
                "Interromper roteamento ativo quando divergência crítica for detectada",
                "Reprocessar snapshots a partir de trilha auditável"
        );
        List<String> proximasAcoes = pronta
                ? List.of("Expandir shadow mode para conectores com maior maturidade")
                : List.of(
                        "Fechar convivência transicional e reconciliação de metadados",
                        "Elevar governança operacional da onda de sombra",
                        "Remover bloqueadores estruturais do build gate"
                );
        return new PjbSubstituicaoNacionalOnda(
                "shadow-mode-governado",
                "Onda 0 - sombra governada",
                status(score, pronta),
                score,
                pronta,
                "Provar convivência nacional com legado sem risco de corte prematuro.",
                criterios,
                blocos,
                List.copyOf(guardrails),
                rollback,
                List.of("PJe", "eproc", "e-SAJ", "Projudi", "Creta"),
                proximasAcoes
        );
    }

    private PjbSubstituicaoNacionalOnda ondaOperacaoAssistida(PjbArquiteturaSubstituicaoNacionalAggregate arquitetura,
                                                              PjbArquiteturaSubstituicaoPilar confiabilidade,
                                                              PjbArquiteturaSubstituicaoPilar governanca,
                                                              int conectoresOperacionais,
                                                              int conectoresSaudaveis,
                                                              int conectoresBloqueados,
                                                              int sistemasProntosProducao) {
        int connectorScore = clamp((sistemasProntosProducao * 12) + (conectoresSaudaveis * 8) + (conectoresOperacionais * 6) - (conectoresBloqueados * 10));
        int score = clamp((int) Math.round(arquitetura.scoreGeral() * 0.35d + confiabilidade.score() * 0.35d + governanca.score() * 0.15d + connectorScore * 0.15d));
        boolean pronta = arquitetura.buildGateAprovado()
                && arquitetura.scoreGeral() >= 85
                && confiabilidade.score() >= 80
                && governanca.score() >= 80
                && sistemasProntosProducao > 0
                && conectoresBloqueados <= 1;
        List<String> proximasAcoes = pronta
                ? List.of("Selecionar tribunais pioneiros e habilitar corte assistido por onda")
                : List.of(
                        "Fechar SLO, runbook, idempotência e retomada transacional",
                        "Subir conectores production-ready para operação assistida",
                        "Reduzir bloqueadores de observabilidade e criptografia judicial"
                );
        return new PjbSubstituicaoNacionalOnda(
                "operacao-assistida",
                "Onda 1 - operação assistida",
                status(score, pronta),
                score,
                pronta,
                "Executar cortes controlados em tribunais pioneiros sob dupla observação e reversão imediata.",
                List.of(
                        "Arquitetura nacional >= 85",
                        "Confiabilidade institucional >= 80",
                        "Ao menos um sistema judicial production-ready",
                        "Bloqueadores observáveis próximos de zero"
                ),
                List.of(
                        "Cutover por ramo/rito/unidade em janela governada",
                        "Acompanhamento em tempo real de SLA, backlog e reconciliação",
                        "Confirmação humana institucional nas rotas sensíveis"
                ),
                List.of(
                        "Idempotência obrigatória em todos os comandos externos",
                        "Circuit breaker ativo e métricas por tribunal",
                        "Toda falha crítica com decisão e auditoria registradas"
                ),
                List.of(
                        "Reabrir roteamento para legado por política overlay",
                        "Suspender apenas a onda impactada, sem contaminação nacional",
                        "Reconciliar fila e metadados a partir do ledger"
                ),
                List.of("PJe", "eproc", "e-SAJ"),
                proximasAcoes
        );
    }

    private PjbSubstituicaoNacionalOnda ondaCutover(PjbArquiteturaSubstituicaoNacionalAggregate arquitetura,
                                                    PjbArquiteturaSubstituicaoPilar motor,
                                                    PjbArquiteturaSubstituicaoPilar interoperabilidade,
                                                    PjbArquiteturaSubstituicaoPilar confiabilidade,
                                                    PjbArquiteturaSubstituicaoPilar governanca,
                                                    int conectoresBloqueados,
                                                    int sistemasProntosProducao) {
        int score = clamp((int) Math.round(
                motor.score() * 0.25d
                        + interoperabilidade.score() * 0.2d
                        + confiabilidade.score() * 0.25d
                        + governanca.score() * 0.2d
                        + (arquitetura.prontoParaSubstituicaoImediata() ? 10 : 0)
                        - (conectoresBloqueados * 4)));
        boolean pronta = arquitetura.prontoParaSubstituicaoImediata()
                && arquitetura.buildGateAprovado()
                && conectoresBloqueados == 0
                && sistemasProntosProducao >= 2;
        List<String> proximasAcoes = pronta
                ? List.of("Publicar plano nacional de corte por ondas federativas e ramos")
                : List.of(
                        "Zerar bloqueadores ativos de conectores e observabilidade",
                        "Levar pelo menos dois sistemas a production-ready forte",
                        "Fechar governança de reversibilidade nacional e playbook federativo"
                );
        return new PjbSubstituicaoNacionalOnda(
                "cutover-nacional",
                "Onda 2 - cutover nacional governado",
                status(score, pronta),
                score,
                pronta,
                "Substituir gradualmente os legados com corte federativo, reversível e auditável.",
                List.of(
                        "Quatro pilares estruturais sem pendência crítica",
                        "Zero bloqueador ativo em conectores nacionais",
                        "Matriz federativa de rollout e rollback aprovada",
                        "Maturidade operacional provada em ondas assistidas"
                ),
                List.of(
                        "Corte por família de sistema e por tribunal",
                        "Monitoramento nacional com war room institucional",
                        "Encerramento progressivo das rotas legadas desnecessárias"
                ),
                List.of(
                        "Sem corte em massa sem prova operacional da onda anterior",
                        "Rollback por tribunal, ramo e rito sem reimplantar tudo",
                        "Todo corte dependente de decisão de governança e telemetria saudável"
                ),
                List.of(
                        "Reativar sombra e convivência transicional imediatamente",
                        "Restaurar roteamento anterior por política nacional",
                        "Executar reconciliação corretiva e reidratação de eventos"
                ),
                List.of("PJe", "eproc", "e-SAJ", "Projudi", "Creta"),
                proximasAcoes
        );
    }

    private PjbFechamentoStatus status(int score, boolean pronta) {
        if (pronta) {
            return PjbFechamentoStatus.CONCLUIDA;
        }
        if (score >= 80) {
            return PjbFechamentoStatus.PARCIAL;
        }
        if (score >= 55) {
            return PjbFechamentoStatus.PENDENTE;
        }
        return PjbFechamentoStatus.BLOQUEADA;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
