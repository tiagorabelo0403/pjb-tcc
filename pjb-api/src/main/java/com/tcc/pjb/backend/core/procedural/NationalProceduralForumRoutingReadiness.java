package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.preflight.ProceduralPreflightEngine;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import java.util.List;

record NationalProceduralForumRoutingReadiness(
        TribunalProtocolRoutingService.RoutingDecision routingDecision,
        JudicialSubmissionCapability capability,
        ProceduralPreflightEngine.PreflightResult preflight,
        boolean connectorOperational,
        boolean protocoloRealDisponivel,
        boolean preProtocoloApto,
        String preProtocoloStatus,
        List<String> incompatibilities,
        List<String> warnings,
        List<String> reviewChecklist
) {
}
