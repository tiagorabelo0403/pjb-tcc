package com.tcc.pjb.backend.ai.common;

import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.HsmTestFactory;

public final class VectorSearchTestFactory {

    private VectorSearchTestFactory() {
    }

    public static VectorSearchServiceMock forTest() {
        return new VectorSearchServiceMock(new HsmTestFactory.NoOpMockGuardEnvironmentQuery(), event -> {});
    }
}
