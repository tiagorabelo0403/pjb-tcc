package com.tcc.pjb.backend.workflow.adapter;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.workflow.service.WorkflowVariableAggregator;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultAjuizamentoWorkflowAdapter implements AjuizamentoWorkflowAdapter {

    private final ObjectProvider<WorkflowVariableAggregator> aggregatorProvider;

    @Override
    public Map<String, Object> applyDecisionOutputs(Map<String, Object> command, Map<String, Object> decisionOutputs) {
        final Map<String, Object> merged = new HashMap<>();
        if (command != null) {
            merged.putAll(command);
        }
        if (decisionOutputs != null) {
            merged.putAll(decisionOutputs);
        }

        final WorkflowVariableAggregator aggregator = aggregatorProvider.getIfAvailable();
        if (aggregator == null) {
            return merged;
        }

        try {
            
            return new HashMap<>(aggregator.aggregateVariables(merged));
        } catch (Exception ex) {
            
            return merged;
        }
    }
}
