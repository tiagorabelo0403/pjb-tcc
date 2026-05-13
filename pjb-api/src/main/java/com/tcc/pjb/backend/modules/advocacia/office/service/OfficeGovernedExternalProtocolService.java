package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedProtocolSubmissionView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedProtocolSubmitRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.laiane.dto.protocol.LaianeProtocolPackageDto;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProtocolPackage;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProtocolPackageRepository;
import com.tcc.pjb.backend.modules.laiane.service.LaianeProtocolSubmissionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeGovernedExternalProtocolService {

    private final CurrentUserService currentUserService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final LaianeProtocolPackageRepository laianeProtocolPackageRepository;
    private final LaianeProtocolSubmissionService laianeProtocolSubmissionService;

    public OfficeGovernedExternalProtocolService(CurrentUserService currentUserService,
                                                 OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService,
                                                 LaianeProtocolPackageRepository laianeProtocolPackageRepository,
                                                 LaianeProtocolSubmissionService laianeProtocolSubmissionService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officeProcessWorkspaceScopeService = Objects.requireNonNull(officeProcessWorkspaceScopeService);
        this.laianeProtocolPackageRepository = Objects.requireNonNull(laianeProtocolPackageRepository);
        this.laianeProtocolSubmissionService = Objects.requireNonNull(laianeProtocolSubmissionService);
    }

    @Transactional
    public PjbFrontendOfficeGovernedProtocolSubmissionView submit(Long processoId,
                                                                  Long protocolPackageId,
                                                                  FrontendOfficeGovernedProtocolSubmitRequest request,
                                                                  HttpServletRequest httpServletRequest) {
        Usuario actor = currentUserService.getRequired();
        PjbFrontendOfficeProcessAccessView access = officeProcessWorkspaceScopeService.access(processoId, OfficeActionType.PROTOCOL_SUBMIT_PJE, httpServletRequest);
        if (!access.allowed()) {
            throw new IllegalStateException("Protocolo externo fora do escopo operacional do workspace: " + String.join(", ", access.blockers()));
        }
        LaianeProtocolPackage protocolPackage = laianeProtocolPackageRepository.findById(protocolPackageId)
                .orElseThrow(() -> new EntityNotFoundException("Pacote de protocolo nao encontrado."));
        if (protocolPackage.getUsuario() == null || protocolPackage.getUsuario().getId() == null || !Objects.equals(protocolPackage.getUsuario().getId(), actor.getId())) {
            throw new EntityNotFoundException("Pacote de protocolo nao encontrado.");
        }
        if (request != null && request.expectedIntegrityHash() != null && !request.expectedIntegrityHash().isBlank()
                && !Objects.equals(protocolPackage.getIntegrityHash(), request.expectedIntegrityHash().trim())) {
            throw new IllegalStateException("Pacote de protocolo alterado desde a revisao do frontend.");
        }
        LaianeProtocolPackageDto result = laianeProtocolSubmissionService.submit(protocolPackageId);
        return new PjbFrontendOfficeGovernedProtocolSubmissionView(
                processoId,
                result.getId(),
                result.getIntegrityHash(),
                result.getStatus(),
                result.getOfficeQueueItemId(),
                result.getSubmissionJobId(),
                result.getSignerUserId(),
                access.queueRequired(),
                result.getExternalProtocolRef(),
                result.getSubmittedAt(),
                result.getLastError(),
                result.getGuardrailStatus(),
                Boolean.TRUE.equals(result.getReadyForSubmission()),
                result.getGuardrailBlockers() == null ? List.of() : result.getGuardrailBlockers(),
                access.warnings() == null ? List.of() : access.warnings()
        );
    }
}
