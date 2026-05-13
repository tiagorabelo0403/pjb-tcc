package com.tcc.pjb.backend.legal.skills.v1;

import java.util.Map;

public interface LegalSkillV1 {

    
    String id();

    
    default boolean supports(LegalSkillRequestV1 request) {
        return request != null && id().equalsIgnoreCase(request.getSkill());
    }

    
    LegalSkillResponseV1 execute(LegalSkillRequestV1 request, Map<String, Object> context);
}
