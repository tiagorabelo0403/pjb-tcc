package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class AdvProcessOperationOfficeQueueExecutor implements OfficeQueueExecutor {

    private final OfficeGovernedProcessOperationService officeGovernedProcessOperationService;

    public AdvProcessOperationOfficeQueueExecutor(OfficeGovernedProcessOperationService officeGovernedProcessOperationService) {
        this.officeGovernedProcessOperationService = Objects.requireNonNull(officeGovernedProcessOperationService);
    }

    @Override
    public String resourceType() {
        return OfficeGovernedProcessOperationService.RESOURCE_TYPE;
    }

    @Override
    public void onApproved(OfficeSignatureQueueItem item, Long decidedByUserId, String reason) {
        Long operationId = parseLong(item == null ? null : item.getResourceId());
        if (operationId == null) {
            return;
        }
        officeGovernedProcessOperationService.approveQueuedOperation(operationId, item == null ? null : item.getId(), decidedByUserId, reason);
    }

    @Override
    public void onRejected(OfficeSignatureQueueItem item, Long decidedByUserId, String reason) {
        Long operationId = parseLong(item == null ? null : item.getResourceId());
        if (operationId == null) {
            return;
        }
        officeGovernedProcessOperationService.rejectQueuedOperation(operationId, item == null ? null : item.getId(), decidedByUserId, reason);
    }

    private Long parseLong(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
