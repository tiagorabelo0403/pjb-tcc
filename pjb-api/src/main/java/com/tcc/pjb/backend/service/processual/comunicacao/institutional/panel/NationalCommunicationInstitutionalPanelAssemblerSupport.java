package com.tcc.pjb.backend.service.processual.comunicacao.institutional.panel;

import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalExecutivePanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelCard;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelChart;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelChartPoint;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelNotification;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelProgressStage;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalOrgPanelSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalUnitQueueSummary;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalExecutiveDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelCardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelChartPointResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelChartResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelNotificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelProgressStageResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelSummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalUnitQueueResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.access.InstitutionalRequestAccessContextFacadeService;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class NationalCommunicationInstitutionalPanelAssemblerSupport {

    private final InstitutionalRequestAccessContextFacadeService accessContextFacadeService;

    public NationalCommunicationInstitutionalPanelAssemblerSupport(InstitutionalRequestAccessContextFacadeService accessContextFacadeService) {
        this.accessContextFacadeService = Objects.requireNonNull(accessContextFacadeService);
    }

    public NationalCommunicationInstitutionalExecutiveDashboardResponse toResponse(InstitutionalExecutivePanel panel) {
        return new NationalCommunicationInstitutionalExecutiveDashboardResponse(
                panel.cards().stream().map(this::toResponse).toList(),
                panel.notifications().stream().map(this::toResponse).toList(),
                panel.progressStages().stream().map(this::toResponse).toList(),
                panel.charts().stream().map(this::toResponse).toList(),
                panel.orgaos().stream().map(this::toResponse).toList(),
                panel.filas().stream().map(this::toResponse).toList(),
                accessContextFacadeService.atual(),
                panel.generatedAt());
    }

    public NationalCommunicationInstitutionalPanelCardResponse toResponse(InstitutionalPanelCard item) {
        return new NationalCommunicationInstitutionalPanelCardResponse(item.code(), item.title(), item.value(), item.subtitle(), item.accentColor(), item.trend(), item.icon(), item.navigationPath());
    }

    public NationalCommunicationInstitutionalPanelNotificationResponse toResponse(InstitutionalPanelNotification item) {
        return new NationalCommunicationInstitutionalPanelNotificationResponse(item.notificationId(), item.severity(), item.title(), item.message(), item.accentColor(), item.actionLabel(), item.actionPath(), item.createdAt());
    }

    public NationalCommunicationInstitutionalPanelProgressStageResponse toResponse(InstitutionalPanelProgressStage item) {
        return new NationalCommunicationInstitutionalPanelProgressStageResponse(item.code(), item.title(), item.total(), item.percentual(), item.accentColor(), item.semanticStatus());
    }

    public NationalCommunicationInstitutionalPanelChartResponse toResponse(InstitutionalPanelChart item) {
        return new NationalCommunicationInstitutionalPanelChartResponse(
                item.chartId(),
                item.title(),
                item.chartType(),
                item.accentColor(),
                item.points().stream().map(this::toResponse).toList());
    }

    public NationalCommunicationInstitutionalPanelChartPointResponse toResponse(InstitutionalPanelChartPoint point) {
        return new NationalCommunicationInstitutionalPanelChartPointResponse(point.label(), point.value(), point.accentColor(), point.tooltip());
    }

    public NationalCommunicationInstitutionalPanelSummaryResponse toResponse(InstitutionalOrgPanelSummary item) {
        InstitutionalRequestAccessContextFacadeService.InstitutionalAccessDigest digest = accessContextFacadeService.digest();
        return new NationalCommunicationInstitutionalPanelSummaryResponse(item.unidadeCodigo(), item.unidadeSigla(), item.destinatarioKind(), item.totalExpedientes(), item.pendentesRecebimento(), item.pendentesCiencia(), item.pendentesCumprimento(), item.atrasados(), item.caixasVisiveis(), digest.horizontalDataPlaneKey(), digest.rlsScopeKey(), digest.coverageMode(), digest.readOnly(), item.generatedAt());
    }

    public NationalCommunicationInstitutionalUnitQueueResponse toResponse(InstitutionalUnitQueueSummary item) {
        InstitutionalRequestAccessContextFacadeService.InstitutionalAccessDigest digest = accessContextFacadeService.digest();
        return new NationalCommunicationInstitutionalUnitQueueResponse(item.unidadeCodigo(), item.unidadeSigla(), item.caixaCodigo(), item.total(), item.disponibilizadas(), item.recebidas(), item.cientificadas(), item.cumpridas(), item.atrasadas(), digest.horizontalDataPlaneKey(), digest.rlsScopeKey(), digest.coverageMode(), digest.readOnly(), item.prazoMaisProximo(), item.generatedAt());
    }
}