package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.runtime.JobExecutionContext;
import com.tcc.pjb.backend.core.jobs.runtime.JobHandler;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.features", name = "substituicao-nacional", havingValue = "true", matchIfMissing = true)
public class PjbSubstituicaoNacionalExecutionJobHandler implements JobHandler {

    private final PjbSubstituicaoNacionalExecutionOrchestrator orchestrator;

    public PjbSubstituicaoNacionalExecutionJobHandler(PjbSubstituicaoNacionalExecutionOrchestrator orchestrator) {
        this.orchestrator = Objects.requireNonNull(orchestrator);
    }

    @Override
    public JobType type() {
        return JobType.PJB_SUBSTITUICAO_NACIONAL_EXECUCAO;
    }

    @Override
    public void execute(JobExecutionContext ctx) {
        JobInput input = ctx.inputAs(JobInput.class);
        try {
            orchestrator.executar(input.execucaoId(), ctx);
        } catch (RuntimeException ex) {
            orchestrator.falhar(input.execucaoId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    public record JobInput(Long execucaoId) {
    }
}
