package com.tcc.pjb.backend.legal.skills.v3;

import java.util.Map;

public interface LegalSkillV3 {

    String id();

    default boolean supports(LegalSkillRequestV3 request) {
        return request != null && id().equalsIgnoreCase(request.getSkill());
    }

    LegalSkillResponseV3 execute(LegalSkillRequestV3 request, Map<String, Object> context);
}
