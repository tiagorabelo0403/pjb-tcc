package com.tcc.pjb.backend.core.procedural;

import java.util.Map;

record NationalProceduralActionProfileContext(
        Map<String, Object> payload,
        ProceduralCanonicalResolver.CanonicalContext canonical,
        String corpus,
        NationalProceduralPartyProfile partyProfile
) {
}
