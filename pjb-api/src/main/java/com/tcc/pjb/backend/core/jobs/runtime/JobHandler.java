package com.tcc.pjb.backend.core.jobs.runtime;

import com.tcc.pjb.backend.core.jobs.domain.JobType;

public interface JobHandler {

    JobType type();

    void execute(JobExecutionContext ctx) throws Exception;
}
