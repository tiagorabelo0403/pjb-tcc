package com.tcc.pjb.backend.core.jobs.domain;

public sealed interface JobState permits JobState.Pending, JobState.Running, JobState.Paused, JobState.Succeeded, JobState.Failed, JobState.Dead {

    record Pending() implements JobState {}
    record Running() implements JobState {}
    record Paused() implements JobState {}
    record Succeeded() implements JobState {}
    record Failed() implements JobState {}
    record Dead() implements JobState {}
}
