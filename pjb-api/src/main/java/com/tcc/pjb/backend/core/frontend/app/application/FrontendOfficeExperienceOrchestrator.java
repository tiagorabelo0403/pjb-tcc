package com.tcc.pjb.backend.core.frontend.app.application;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessReadingModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceLegalCockpitView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceMainDashboardView;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeQueueItemDto;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeSignatureQueueService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceExecutiveDashboardService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceLegalCockpitService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceMainDashboardService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceTeamAvatarService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de PjbFrontendAppApplicationService: superfícies de experiência do
 * escritório -- fila de assinaturas, cockpit jurídico, dashboards principal/executivo
 * e avatar de membro da equipe.
 */
@Service
public class FrontendOfficeExperienceOrchestrator {

    private final OfficeSignatureQueueService officeSignatureQueueService;
    private final OfficeWorkspaceLegalCockpitService officeWorkspaceLegalCockpitService;
    private final OfficeWorkspaceMainDashboardService officeWorkspaceMainDashboardService;
    private final OfficeWorkspaceExecutiveDashboardService officeWorkspaceExecutiveDashboardService;
    private final OfficeWorkspaceTeamAvatarService officeWorkspaceTeamAvatarService;

    public FrontendOfficeExperienceOrchestrator(OfficeSignatureQueueService officeSignatureQueueService,
                                                 OfficeWorkspaceLegalCockpitService officeWorkspaceLegalCockpitService,
                                                 OfficeWorkspaceMainDashboardService officeWorkspaceMainDashboardService,
                                                 OfficeWorkspaceExecutiveDashboardService officeWorkspaceExecutiveDashboardService,
                                                 OfficeWorkspaceTeamAvatarService officeWorkspaceTeamAvatarService) {
        this.officeSignatureQueueService = Objects.requireNonNull(officeSignatureQueueService);
        this.officeWorkspaceLegalCockpitService = Objects.requireNonNull(officeWorkspaceLegalCockpitService);
        this.officeWorkspaceMainDashboardService = Objects.requireNonNull(officeWorkspaceMainDashboardService);
        this.officeWorkspaceExecutiveDashboardService = Objects.requireNonNull(officeWorkspaceExecutiveDashboardService);
        this.officeWorkspaceTeamAvatarService = Objects.requireNonNull(officeWorkspaceTeamAvatarService);
    }

    public Page<OfficeQueueItemDto> listSignatureQueue(Long currentUserId, OfficeQueueStatus status, Pageable pageable) {
        return officeSignatureQueueService.listarPorSigner(currentUserId, status, pageable);
    }

    public OfficeQueueItemDto approveQueueItem(Long currentUserId, Long queueItemId, String reason) {
        return officeSignatureQueueService.aprovar(currentUserId, queueItemId, reason);
    }

    public OfficeQueueItemDto rejectQueueItem(Long currentUserId, Long queueItemId, String reason) {
        return officeSignatureQueueService.rejeitar(currentUserId, queueItemId, reason);
    }

    public PjbFrontendOfficeWorkspaceLegalCockpitView legalCockpit(Authentication authentication, HttpServletRequest request, LocalDate from, LocalDate to, Long processoId) {
        return officeWorkspaceLegalCockpitService.cockpit(authentication, request, from, to, processoId);
    }

    public PjbFrontendOfficeProcessReadingModeView readingMode(Long processoId, HttpServletRequest request, LocalDate from, LocalDate to) {
        return officeWorkspaceLegalCockpitService.readingMode(processoId, request, from, to);
    }

    public PjbFrontendOfficeWorkspaceMainDashboardView mainDashboard(Authentication authentication, HttpServletRequest request, LocalDate from, LocalDate to) {
        return officeWorkspaceMainDashboardService.dashboard(authentication, request, from, to);
    }

    public PjbFrontendOfficeWorkspaceExecutiveDashboardView executiveDashboard(Authentication authentication, HttpServletRequest request, LocalDate from, LocalDate to) {
        return officeWorkspaceExecutiveDashboardService.dashboard(authentication, request, from, to);
    }

    public OfficeWorkspaceTeamAvatarService.AvatarReadResult readTeamAvatar(Long userId, HttpServletRequest request) throws IOException {
        return officeWorkspaceTeamAvatarService.read(userId, request);
    }
}
