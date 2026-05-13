package com.tcc.pjb.backend.ai.agentic.core;

import com.tcc.pjb.backend.ai.scope.MateriaDecision;
import com.tcc.pjb.backend.service.SigiloService;

public record AgenticRoutingDecision(
        AgenticDomain domain,
        MateriaDecision materia,
        SigiloService.SigiloDecision sigilo,
        String effectiveQuery
) {
}
