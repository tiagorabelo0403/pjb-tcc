package com.tcc.pjb.backend.service.jobs;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.runtime.JobExecutionContext;
import com.tcc.pjb.backend.core.jobs.runtime.JobHandler;
import com.tcc.pjb.backend.core.jobs.runtime.JobPauseException;

@Component
public class NotImplementedJobHandlers {

    @Component
    static class SecretariatTriageReindex implements JobHandler {
        @Override
        public JobType type() {
            return JobType.SECRETARIAT_TRIAGE_REINDEX;
        }

        @Override
        public void execute(JobExecutionContext ctx) {
            throw new JobPauseException("SECRETARIAT_TRIAGE_REINDEX ainda não foi conectado ao projetor");
        }
    }

    @Component
    static class BulkNotify implements JobHandler {
        @Override
        public JobType type() {
            return JobType.BULK_NOTIFY;
        }

        @Override
        public void execute(JobExecutionContext ctx) {
            throw new JobPauseException("BULK_NOTIFY ainda não foi implementado");
        }
    }

    @Component
    static class BulkSign implements JobHandler {
        @Override
        public JobType type() {
            return JobType.BULK_SIGN;
        }

        @Override
        public void execute(JobExecutionContext ctx) {
            throw new JobPauseException("BULK_SIGN ainda não foi implementado");
        }
    }

    @Component
    static class BulkExport implements JobHandler {
        @Override
        public JobType type() {
            return JobType.BULK_EXPORT;
        }

        @Override
        public void execute(JobExecutionContext ctx) {
            throw new JobPauseException("BULK_EXPORT ainda não foi implementado");
        }
    }
}
