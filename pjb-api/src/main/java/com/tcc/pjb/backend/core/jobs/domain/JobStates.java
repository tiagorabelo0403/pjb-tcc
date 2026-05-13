package com.tcc.pjb.backend.core.jobs.domain;

import java.util.Objects;

public final class JobStates {

    private JobStates() {
    }

    public static JobState from(JobStatus status) {
        Objects.requireNonNull(status, "status");
        return switch (status) {
            case PENDING -> new JobState.Pending();
            case RUNNING -> new JobState.Running();
            case PAUSED -> new JobState.Paused();
            case SUCCEEDED -> new JobState.Succeeded();
            case FAILED -> new JobState.Failed();
            case DEAD -> new JobState.Dead();
        };
    }
}
