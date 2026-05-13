package com.tcc.pjb.backend.modules.advocacia.office.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeDelegatedActionOpsDto;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeSignerDashboardRowDto;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeDelegationMode;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import com.tcc.pjb.backend.modules.advocacia.office.repository.OfficeDelegatedActionRepository;

@Service
public class OfficeOpsService {

    private final OfficeDelegatedActionRepository delegatedActionRepository;

    public OfficeOpsService(OfficeDelegatedActionRepository delegatedActionRepository) {
        this.delegatedActionRepository = delegatedActionRepository;
    }

    @Transactional(readOnly = true)
    public Page<OfficeDelegatedActionOpsDto> delegatedActions(Long equipeId,
                                                             Long executorUserId,
                                                             Long signerUserId,
                                                             OfficeActionType actionType,
                                                             OfficeDelegationMode mode,
                                                             OfficeQueueStatus queueStatus,
                                                             String resourceType,
                                                             String resourceId,
                                                             LocalDateTime from,
                                                             LocalDateTime to,
                                                             Pageable pageable) {
        return delegatedActionRepository.searchOps(equipeId, executorUserId, signerUserId, actionType, mode, queueStatus, resourceType, resourceId, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public List<OfficeSignerDashboardRowDto> dashboard(Long equipeId, Long signerUserId, LocalDateTime from, LocalDateTime to) {
        return delegatedActionRepository.dashboard(equipeId, signerUserId, from, to)
                .stream()
                .map(r -> new OfficeSignerDashboardRowDto(
                        r.getDay(),
                        r.getExecutorUserId(),
                        OfficeDelegationMode.valueOf(r.getMode()),
                        r.getTotal() != null ? r.getTotal() : 0L
                ))
                .toList();
    }
}
