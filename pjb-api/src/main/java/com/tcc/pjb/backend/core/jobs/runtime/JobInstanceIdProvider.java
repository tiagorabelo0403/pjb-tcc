package com.tcc.pjb.backend.core.jobs.runtime;

import java.util.Objects;

public final class JobInstanceIdProvider {

    private static final String UNKNOWN_INSTANCE = "unknown-instance";

    private final String instanceId;

    public JobInstanceIdProvider(String instanceId) {
        this.instanceId = normalize(instanceId);
    }

    public String get() {
        return instanceId;
    }

    public boolean isUnknown() {
        return UNKNOWN_INSTANCE.equals(instanceId);
    }

    private static String normalize(String instanceId) {
        String value = Objects.requireNonNullElse(instanceId, "").trim();
        return value.isEmpty() ? UNKNOWN_INSTANCE : value;
    }
}
