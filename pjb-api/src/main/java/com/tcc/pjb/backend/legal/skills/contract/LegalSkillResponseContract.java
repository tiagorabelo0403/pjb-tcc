package com.tcc.pjb.backend.legal.skills.contract;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface LegalSkillResponseContract {

    String getRequestId();

    String getCorrelationId();

    String getSkill();

    double getConfidence();

    String getMessage();

    Instant getTimestamp();

    Map<String, Object> getOutputs();

    List<String> getWarnings();
}
