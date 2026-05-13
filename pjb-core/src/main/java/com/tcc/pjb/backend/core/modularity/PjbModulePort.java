package com.tcc.pjb.backend.core.modularity;

import java.util.Objects;

public interface PjbModulePort {

    PjbModuleId moduleId();

    default String moduleCode() {
        return Objects.requireNonNull(moduleId(), "moduleId").code();
    }

    default PjbModuleDescriptor moduleDescriptor() {
        return PjbModuleCatalog.descriptor(Objects.requireNonNull(moduleId(), "moduleId"));
    }
}
