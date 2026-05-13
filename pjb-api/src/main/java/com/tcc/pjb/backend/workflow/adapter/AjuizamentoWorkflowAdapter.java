package com.tcc.pjb.backend.workflow.adapter;

import java.util.Map;

public interface AjuizamentoWorkflowAdapter {

    Map<String, Object> applyDecisionOutputs(Map<String, Object> command, Map<String, Object> decisionOutputs);
}
