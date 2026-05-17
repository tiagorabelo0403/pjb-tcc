package com.tcc.pjb.backend.workflow.consumer;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.workflow.adapter.AjuizamentoWorkflowAdapter;
import com.tcc.pjb.backend.workflow.zeebe.ZeebeCompat;
import io.camunda.zeebe.client.ZeebeClient;
import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnBean(ZeebeClient.class)
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public class ComandoAjuizamentoConsumer {

    private final ZeebeClient zeebeClient;
    private final AjuizamentoWorkflowAdapter adapter;

    public void startAjuizamento(Map<String, Object> vars) {
        Map<String, Object> commandVars = vars == null ? Map.of() : new LinkedHashMap<>(vars);

        Object evalCmd = zeebeClient
                .newEvaluateDecisionCommand()
                .decisionId("ajui-rules");

        evalCmd = ZeebeCompat.latestVersionIfPossible(evalCmd);
        ZeebeCompat.withVariables(evalCmd, commandVars);

        Object evalFuture = ZeebeCompat.send(evalCmd);
        Object evalResponse = ZeebeCompat.await(evalFuture);

        Map<String, Object> dmnOutputs = ZeebeCompat.decisionOutputAsMap(evalResponse);
        Map<String, Object> workflowVars = adapter.applyDecisionOutputs(commandVars, dmnOutputs);

        Object createCmd = zeebeClient
                .newCreateInstanceCommand()
                .bpmnProcessId("process-ajui")
                .latestVersion();
        ZeebeCompat.withVariables(createCmd, workflowVars);

        Object createFuture = ZeebeCompat.send(createCmd);
        ZeebeCompat.await(createFuture);
    }
}
