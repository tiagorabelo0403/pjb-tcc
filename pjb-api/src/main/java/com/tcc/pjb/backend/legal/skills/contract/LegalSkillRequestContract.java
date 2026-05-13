package com.tcc.pjb.backend.legal.skills.contract;

import java.time.Instant;
import java.util.Map;

public interface LegalSkillRequestContract {

    String getRequestId();

    String getCorrelationId();

    String getTenantId();

    String getUsuarioId();

    String getSkill();

    String getContractVersion();

    Instant getTimestamp();

    Map<String, Object> getPayload();
}
