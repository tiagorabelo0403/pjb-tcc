package com.tcc.pjb.backend.legal.skills.v2;

import java.util.Map;

public interface LegalSkillV2 {

    
    String id();

    
    default boolean supports(LegalSkillRequestV2 request) {
        return request != null && id().equalsIgnoreCase(request.getSkill());
    }

    
    LegalSkillResponseV2 execute(LegalSkillRequestV2 request, Map<String, Object> context);
}
