package com.tcc.pjb.backend.core.dje;

public final class DjeTestFactory {

    private DjeTestFactory() {
    }

    public static DjeProperties propertiesForTest(boolean enabled, long schedulerFixedRateMs, int maxBatchSize) {
        return new DjeProperties(enabled, false, schedulerFixedRateMs, maxBatchSize);
    }
}
