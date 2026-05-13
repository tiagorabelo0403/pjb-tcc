package com.tcc.pjb.backend.core.events;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;

public interface PjbIntegrationEvent {

    PjbModuleId moduleId();

    String eventType();

    default int schemaVersion() {
        return 1;
    }
}
