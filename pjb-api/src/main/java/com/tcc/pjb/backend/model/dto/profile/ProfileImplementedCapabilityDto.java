package com.tcc.pjb.backend.model.dto.profile;

import java.util.List;

public record ProfileImplementedCapabilityDto(
        String role,
        String capabilityCode,
        String title,
        String status,
        String activationPath,
        String summary,
        List<String> tags
) {
}
