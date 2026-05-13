package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.application.InstitutionalCommunicationObservabilityApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.domain.InstitutionalObservabilityBucket;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.domain.InstitutionalObservabilityDashboard;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.*;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalFlowAnalyticsApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalPanelApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalSlaPredictiveApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.*;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.NotificationHistory;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.model.repository.NotificationHistoryRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalExecutivePanelApplicationService {

    private static final String RED = "#DC2626";
    private static final String AMBER = "#D97706";
    private static final String BLUE = "#2563EB";
    private static final String GREEN = "#059669";
    private static final String PURPLE = "#7C3AED";
    private static final String SLATE = "#475569";

    private final InstitutionalPanelApplicationService panelService;
    private final InstitutionalCommunicationObservabilityApplicationService observabilityService;
    private final InstitutionalFlowAnalyticsApplicationService analyticsService;
    private final InstitutionalSlaPredictiveApplicationService predictiveService;
    private final InstitutionalInboxStateRepository inboxRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final CurrentUserService currentUserService;

    public InstitutionalExecutivePanelApplicationService(InstitutionalPanelApplicationService panelService,
                                                         InstitutionalCommunicationObservabilityApplicationService observabilityService,
                                                         InstitutionalFlowAnalyticsApplicationService analyticsService,
                                                         InstitutionalSlaPredictiveApplicationService predictiveService,
                                                         InstitutionalInboxStateRepository inboxRepository,
                                                         NotificationHistoryRepository notificationHistoryRepository,
                                                         CurrentUserService currentUserService) {
        this.panelService = Objects.requireNonNull(panelService);
        this.observabilityService = Objects.requireNonNull(observabilityService);
        this.analyticsService = Objects.requireNonNull(analyticsService);
        this.predictiveService = Objects.requireNonNull(predictiveService);
        this.inboxRepository = Objects.requireNonNull(inboxRepository);
        this.notificationHistoryRepository = Objects.requireNonNull(notificationHistoryRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    public InstitutionalExecutivePanel painelExecutivo(String unidadeCodigo,
                                                       String uf,
                                                       DestinatarioInstitucionalKind destinatarioKind,
                                                       Long processoId,
                                                       String expedicaoUuid) {
        Instant now = Instant.now();
        List<InstitutionalOrgPanelSummary> orgaos = panelService.painelOrgao(unidadeCodigo);
        List<InstitutionalUnitQueueSummary> filas = panelService.filasUnidade(unidadeCodigo);
        InstitutionalObservabilityDashboard observability = observabilityService.dashboard(processoId, uf, destinatarioKind);
        InstitutionalFlowAnalyticsDashboard analytics = analyticsService.dashboard(processoId, expedicaoUuid);
        InstitutionalSlaPredictiveDashboard predictive = predictiveService.dashboard(uf, destinatarioKind);
        List<InstitutionalInboxItem> inbox = filtrarInbox(unidadeCodigo, processoId, expedicaoUuid, destinatarioKind);
        return new InstitutionalExecutivePanel(
                montarCards(observability, analytics, predictive, inbox),
                montarNotifications(observability, analytics, predictive, processoId),
                montarProgressStages(inbox),
                montarCharts(observability, analytics, predictive, inbox),
                orgaos,
                filas,
                now
        );
    }

    public List<InstitutionalPanelNotification> notificacoes(String unidadeCodigo,
                                                             String uf,
                                                             DestinatarioInstitucionalKind destinatarioKind,
                                                             Long processoId,
                                                             String expedicaoUuid) {
        InstitutionalObservabilityDashboard observability = observabilityService.dashboard(processoId, uf, destinatarioKind);
        InstitutionalFlowAnalyticsDashboard analytics = analyticsService.dashboard(processoId, expedicaoUuid);
        InstitutionalSlaPredictiveDashboard predictive = predictiveService.dashboard(uf, destinatarioKind);
        return montarNotifications(observability, analytics, predictive, processoId);
    }

    private List<InstitutionalInboxItem> filtrarInbox(String unidadeCodigo,
                                                      Long processoId,
                                                      String expedicaoUuid,
                                                      DestinatarioInstitucionalKind destinatarioKind) {
        List<InstitutionalInboxItem> source;
        if (expedicaoUuid != null && !expedicaoUuid.isBlank()) {
            source = inboxRepository.findByExpedicaoUuid(expedicaoUuid).stream().toList();
        } else if (processoId != null) {
            source = inboxRepository.findByProcessoId(processoId);
        } else if (unidadeCodigo != null && !unidadeCodigo.isBlank()) {
            source = inboxRepository.findByUnidadeCodigo(unidadeCodigo.trim());
        } else {
            source = inboxRepository.findAll();
        }
        return source.stream()
                .filter(item -> unidadeCodigo == null || unidadeCodigo.isBlank() || item.unidadeCodigo().equalsIgnoreCase(unidadeCodigo.trim()))
                .filter(item -> processoId == null || Objects.equals(item.processoId(), processoId))
                .filter(item -> expedicaoUuid == null || expedicaoUuid.isBlank() || item.expedicaoUuid().equalsIgnoreCase(expedicaoUuid.trim()))
                .filter(item -> destinatarioKind == null || item.destinatarioKind() == destinatarioKind)
                .toList();
    }

    private List<InstitutionalPanelCard> montarCards(InstitutionalObservabilityDashboard observability,
                                                     InstitutionalFlowAnalyticsDashboard analytics,
                                                     InstitutionalSlaPredictiveDashboard predictive,
                                                     List<InstitutionalInboxItem> inbox) {
        long gatesBloqueando = inbox.stream().filter(InstitutionalInboxItem::bloqueiaFluxo).count();
        long pendenciasLeitura = inbox.stream().filter(item -> item.status() == StatusComunicacaoInstitucional.DISPONIBILIZADA).count();
        return List.of(
                new InstitutionalPanelCard("PENDENCIAS", "Pendências institucionais", observability.totalInboxPendentes(), "Caixas com itens sem encerramento", observability.totalInboxPendentes() > 0 ? BLUE : GREEN, trend(observability.totalInboxPendentes()), "inbox", InstitutionalApiRoutes.inbox()),
                new InstitutionalPanelCard("DLQ", "Falhas terminais", observability.totalDlq(), "Entregas que exigem intervenção manual", observability.totalDlq() > 0 ? RED : GREEN, trend(observability.totalDlq()), "shield-alert", InstitutionalApiRoutes.dlq()),
                new InstitutionalPanelCard("SLA", "SLA em risco", observability.totalSlaRisco(), "Unidades com risco preditivo", observability.totalSlaRisco() > 0 ? AMBER : GREEN, trend(observability.totalSlaRisco()), "clock", InstitutionalApiRoutes.slaPreditivo()),
                new InstitutionalPanelCard("GATES", "Gates bloqueando", gatesBloqueando, "Fluxos travados por dependência institucional", gatesBloqueando > 0 ? PURPLE : GREEN, trend(gatesBloqueando), "lock", InstitutionalApiRoutes.gates()),
                new InstitutionalPanelCard("MINUTAS", "Minutas pendentes", analytics.totalMinutasPendentesAprovacao(), "Aguardando revisão ou assinatura", analytics.totalMinutasPendentesAprovacao() > 0 ? BLUE : GREEN, trend(analytics.totalMinutasPendentesAprovacao()), "file-signature", InstitutionalApiRoutes.minutas()),
                new InstitutionalPanelCard("NOVOS", "Novos sem leitura", pendenciasLeitura, "Expedientes ainda não recebidos", pendenciasLeitura > 0 ? AMBER : GREEN, trend(pendenciasLeitura), "bell", InstitutionalApiRoutes.pendentesNaoLeitura())
        );
    }

    private List<InstitutionalPanelNotification> montarNotifications(InstitutionalObservabilityDashboard observability,
                                                                     InstitutionalFlowAnalyticsDashboard analytics,
                                                                     InstitutionalSlaPredictiveDashboard predictive,
                                                                     Long processoId) {
        List<InstitutionalPanelNotification> out = new ArrayList<>();
        if (observability.totalDlq() > 0) {
            out.add(notif("DLQ", "CRITICO", "Entregas institucionais em falha terminal", observability.totalDlq() + " item(ns) aguardam reprocessamento manual ou correção de integração.", RED, "Ver DLQ", InstitutionalApiRoutes.dlq()));
        }
        if (observability.totalGatesBloqueando() > 0) {
            out.add(notif("GATE", "ALTO", "Processos travados por gate institucional", observability.totalGatesBloqueando() + " gate(s) estão impedindo avanço de marco processual.", PURPLE, "Ver gates", InstitutionalApiRoutes.gates()));
        }
        predictive.alertas().stream()
                .filter(alert -> "CRITICO".equalsIgnoreCase(alert.risco()) || "ALTO".equalsIgnoreCase(alert.risco()))
                .sorted(Comparator.comparing(InstitutionalSlaPredictiveAlert::horasRestantesMinimas))
                .limit(4)
                .forEach(alert -> out.add(new InstitutionalPanelNotification(
                        UUID.nameUUIDFromBytes((alert.unidadeCodigo() + alert.risco()).getBytes()).toString(),
                        alert.risco(),
                        "SLA institucional em risco: " + alert.unidadeSigla(),
                        alert.mensagem(),
                        colorForRisk(alert.risco()),
                        "Abrir SLA preditivo",
                        InstitutionalApiRoutes.slaPreditivo(),
                        alert.generatedAt()
                )));
        if (analytics.totalMinutasPendentesAprovacao() > 0) {
            out.add(notif("MINUTA", "MEDIO", "Há minutas aguardando aprovação", analytics.totalMinutasPendentesAprovacao() + " minuta(s) ainda não foram revisadas pelo titular competente.", BLUE, "Abrir minutas", InstitutionalApiRoutes.minutas()));
        }
        long currentUserId = currentUserService.currentUserIdOrZero();
        if (currentUserId > 0) {
            List<NotificationHistory> history = processoId == null
                    ? notificationHistoryRepository.findTop50ByUsuarioIdOrderByEnviadoEmDesc(currentUserId)
                    : notificationHistoryRepository.findTop50ByUsuarioIdAndProcessoIdOrderByEnviadoEmDesc(currentUserId, processoId);
            history.stream().limit(6).forEach(item -> out.add(new InstitutionalPanelNotification(
                    item.getTrackingToken() == null ? UUID.nameUUIDFromBytes((item.getId() + "|notif").getBytes()).toString() : item.getTrackingToken(),
                    normalizeSeverity(item.getStatus()),
                    item.getTitulo(),
                    item.getMensagem(),
                    colorForRisk(normalizeSeverity(item.getStatus())),
                    "Ver detalhes",
                    "/api/v1/processual/comunicacoes/painel",
                    item.getEnviadoEm() == null ? Instant.now() : item.getEnviadoEm().atZone(ZoneId.systemDefault()).toInstant()
            )));
        }
        return out.stream().sorted(Comparator.comparing(InstitutionalPanelNotification::createdAt).reversed()).limit(12).toList();
    }

    private List<InstitutionalPanelProgressStage> montarProgressStages(List<InstitutionalInboxItem> inbox) {
        long total = Math.max(1, inbox.size());
        long disponibilizadas = inbox.stream().filter(item -> item.status() == StatusComunicacaoInstitucional.DISPONIBILIZADA).count();
        long recebidas = inbox.stream().filter(item -> item.status() == StatusComunicacaoInstitucional.RECEBIDA).count();
        long cientificadas = inbox.stream().filter(item -> item.status() == StatusComunicacaoInstitucional.CIENTIFICADA).count();
        long cumpridas = inbox.stream().filter(item -> item.status() == StatusComunicacaoInstitucional.CUMPRIDA).count();
        return List.of(
                stage("DISPONIBILIZADA", "Disponibilizada", disponibilizadas, total, BLUE, "NOVA"),
                stage("RECEBIDA", "Recebida", recebidas, total, AMBER, "EM_TRIAGEM"),
                stage("CIENTIFICADA", "Cientificada", cientificadas, total, PURPLE, "EM_ATUACAO"),
                stage("CUMPRIDA", "Cumprida", cumpridas, total, GREEN, "ENCERRADA")
        );
    }

    private List<InstitutionalPanelChart> montarCharts(InstitutionalObservabilityDashboard observability,
                                                       InstitutionalFlowAnalyticsDashboard analytics,
                                                       InstitutionalSlaPredictiveDashboard predictive,
                                                       List<InstitutionalInboxItem> inbox) {
        List<InstitutionalPanelChart> charts = new ArrayList<>();
        charts.add(new InstitutionalPanelChart(
                "STATUS_ENTREGA",
                "Andamento das comunicações",
                "donut",
                BLUE,
                observability.porStatusEntrega().stream().map(bucket -> point(bucket.key(), bucket.count(), colorForBucket(bucket.key()), bucket.count() + " comunicação(ões)" )).toList()
        ));
        charts.add(new InstitutionalPanelChart(
                "DESTINATARIO",
                "Volume por destinatário institucional",
                "bar",
                PURPLE,
                observability.porDestinatario().stream().map(bucket -> point(bucket.key(), bucket.count(), colorForBucket(bucket.key()), bucket.count() + " expediente(s)" )).toList()
        ));
        charts.add(new InstitutionalPanelChart(
                "MOTIVOS_FALHA",
                "Motivos de fallback e falha",
                "horizontal-bar",
                RED,
                analytics.falhasPorMotivo().values().stream().sorted(Comparator.comparing(InstitutionalFlowAnalyticsBucket::total).reversed()).limit(8).map(bucket -> point(bucket.valor(), bucket.total(), RED, bucket.total() + " ocorrência(s)" )).toList()
        ));
        charts.add(new InstitutionalPanelChart(
                "MINUTAS_STATUS",
                "Fluxo de minutas",
                "stacked-bar",
                BLUE,
                analytics.minutasPorStatus().values().stream().sorted(Comparator.comparing(InstitutionalFlowAnalyticsBucket::valor)).map(bucket -> point(bucket.valor(), bucket.total(), colorForBucket(bucket.valor()), bucket.percentual() + "% do total" )).toList()
        ));
        charts.add(new InstitutionalPanelChart(
                "SLA_RISCO",
                "Risco preditivo de SLA",
                "radial-bar",
                AMBER,
                List.of(
                        point("Crítico", predictive.totalCriticos(), RED, predictive.totalCriticos() + " unidade(s)"),
                        point("Alto", predictive.totalAltos(), AMBER, predictive.totalAltos() + " unidade(s)"),
                        point("Médio", predictive.totalMedios(), BLUE, predictive.totalMedios() + " unidade(s)"),
                        point("Baixo", predictive.totalBaixos(), GREEN, predictive.totalBaixos() + " unidade(s)")
                )
        ));
        charts.add(new InstitutionalPanelChart(
                "CAIXAS_ATIVAS",
                "Fila por caixa institucional",
                "line",
                SLATE,
                inbox.stream()
                        .collect(java.util.stream.Collectors.groupingBy(InstitutionalInboxItem::caixaCodigoAtual, java.util.stream.Collectors.counting()))
                        .entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> point(entry.getKey(), entry.getValue(), SLATE, entry.getValue() + " expediente(s)" ))
                        .toList()
        ));
        return charts;
    }

    private InstitutionalPanelProgressStage stage(String code, String title, long total, long base, String color, String semantic) {
        return new InstitutionalPanelProgressStage(code, title, total, percent(total, base), color, semantic);
    }

    private InstitutionalPanelChartPoint point(String label, Number value, String color, String tooltip) {
        return new InstitutionalPanelChartPoint(label, value.doubleValue(), color, tooltip);
    }

    private InstitutionalPanelNotification notif(String code, String severity, String title, String message, String color, String actionLabel, String actionPath) {
        return new InstitutionalPanelNotification(
                UUID.nameUUIDFromBytes((code + "|" + title).getBytes()).toString(),
                severity,
                title,
                message,
                color,
                actionLabel,
                actionPath,
                Instant.now()
        );
    }

    private double percent(long value, long total) {
        return total <= 0 ? 0D : (value * 100.0D) / total;
    }

    private String trend(long value) {
        if (value <= 0) {
            return "ESTAVEL";
        }
        if (value <= 5) {
            return "ATENCAO";
        }
        return "CRITICO";
    }

    private String colorForRisk(String risk) {
        return switch (risk == null ? "" : risk.trim().toUpperCase()) {
            case "CRITICO", "ERRO", "FALHA" -> RED;
            case "ALTO", "WARNING", "PENDENTE" -> AMBER;
            case "MEDIO", "ENVIADO", "PROCESSANDO" -> BLUE;
            case "BAIXO", "OK", "ENTREGUE", "CONFIRMADO", "SUCESSO" -> GREEN;
            default -> SLATE;
        };
    }

    private String normalizeSeverity(String status) {
        if (status == null || status.isBlank()) {
            return "INFO";
        }
        String normalized = status.trim().toUpperCase();
        if (normalized.contains("FALH") || normalized.contains("ERRO")) {
            return "CRITICO";
        }
        if (normalized.contains("PEND") || normalized.contains("ATRAS")) {
            return "ALTO";
        }
        if (normalized.contains("WARN") || normalized.contains("PROCESS")) {
            return "MEDIO";
        }
        return "INFO";
    }

    private String colorForBucket(String bucket) {
        if (bucket == null) {
            return SLATE;
        }
        String normalized = bucket.trim().toUpperCase();
        if (normalized.contains("FALHA") || normalized.contains("DLQ") || normalized.contains("FRUSTR")) {
            return RED;
        }
        if (normalized.contains("PEND") || normalized.contains("DISPON")) {
            return AMBER;
        }
        if (normalized.contains("CIENT") || normalized.contains("ANALISE") || normalized.contains("MINUTA")) {
            return PURPLE;
        }
        if (normalized.contains("ENTREG") || normalized.contains("CUMPR") || normalized.contains("SUCESS")) {
            return GREEN;
        }
        return BLUE;
    }
}
