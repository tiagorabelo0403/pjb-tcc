package com.tcc.pjb.backend.core.processo.lifecycle;

import com.tcc.pjb.backend.core.processual.ato.AtoProcessualCatalogService;

public final class ProcessoLifecycleMachineTestFactory {

    private ProcessoLifecycleMachineTestFactory() {}

    public static ProcessoLifecycleMachine standalone() {
        return new ProcessoLifecycleMachine(new AtoProcessualCatalogService());
    }
}
