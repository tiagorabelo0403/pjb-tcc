package com.tcc.pjb.backend.model.dto.institutional;

import java.util.List;

public record InstitutionalWorkbenchProfileResponse(
        String actorClass,
        String institutionalBranch,
        String federativeSphere,
        String headline,
        String materialFocus,
        List<String> justiceMesh,
        List<String> territorialAnchors,
        List<String> specialties,
        List<String> capabilities
) {
    public InstitutionalWorkbenchProfileResponse {
        justiceMesh = justiceMesh == null ? List.of() : List.copyOf(justiceMesh);
        territorialAnchors = territorialAnchors == null ? List.of() : List.copyOf(territorialAnchors);
        specialties = specialties == null ? List.of() : List.copyOf(specialties);
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
    }
}
