package com.tcc.pjb.backend.model.dto.profile;

import java.util.List;

public record CapabilityExtensionResponse(
        String role,
        List<String> capabilities
) {
}
