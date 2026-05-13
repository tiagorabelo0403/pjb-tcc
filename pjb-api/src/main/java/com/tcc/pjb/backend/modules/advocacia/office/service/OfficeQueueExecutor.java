package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem;

public interface OfficeQueueExecutor {

    String resourceType();

    void onApproved(OfficeSignatureQueueItem item, Long decidedByUserId, String reason);

    void onRejected(OfficeSignatureQueueItem item, Long decidedByUserId, String reason);
}
