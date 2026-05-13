package com.tcc.pjb.backend.service.magistratura.acts;

import com.tcc.pjb.backend.model.entity.workflow.WorkItem;

public record ReusedWorkItem(WorkItem workItem, boolean reused) {
    public static ReusedWorkItem empty() {
        return new ReusedWorkItem(null, false);
    }
}
