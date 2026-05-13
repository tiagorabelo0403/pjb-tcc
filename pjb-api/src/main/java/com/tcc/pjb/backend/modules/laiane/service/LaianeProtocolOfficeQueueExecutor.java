package com.tcc.pjb.backend.modules.laiane.service;

import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeQueueExecutor;
import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem;

@Component
public class LaianeProtocolOfficeQueueExecutor implements OfficeQueueExecutor {

    private final LaianeProtocolSubmissionService submissionService;

    public LaianeProtocolOfficeQueueExecutor(LaianeProtocolSubmissionService submissionService) {
        this.submissionService = Objects.requireNonNull(submissionService);
    }

    @Override
    public String resourceType() {
        return LaianeProtocolSubmissionService.RESOURCE_TYPE;
    }

    @Override
    public void onApproved(OfficeSignatureQueueItem item, Long decidedByUserId, String reason) {
        if (item == null) return;
        Long protocolId = parseLong(item.getResourceId());
        if (protocolId == null) return;
        Long equipeId = item.getEquipe() != null ? item.getEquipe().getId() : null;
        Long executorId = item.getExecutor() != null ? item.getExecutor().getId() : null;
        Long signerId = item.getSigner() != null ? item.getSigner().getId() : null;
        submissionService.enqueueFromQueue(protocolId, equipeId, executorId, signerId, item.getId(), decidedByUserId, reason);
    }

    @Override
    public void onRejected(OfficeSignatureQueueItem item, Long decidedByUserId, String reason) {
        if (item == null) return;
        Long protocolId = parseLong(item.getResourceId());
        if (protocolId == null) return;
        Long signerId = item.getSigner() != null ? item.getSigner().getId() : null;
        submissionService.rejectFromQueue(protocolId, signerId, item.getId(), decidedByUserId, reason);
    }

    private Long parseLong(String value) {
        try {
            if (value == null) return null;
            String v = value.trim();
            if (v.isEmpty()) return null;
            return Long.parseLong(v);
        } catch (Exception e) {
            return null;
        }
    }
}
