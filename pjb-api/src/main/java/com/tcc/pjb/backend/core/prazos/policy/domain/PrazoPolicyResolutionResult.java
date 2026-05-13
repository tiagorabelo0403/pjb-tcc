package com.tcc.pjb.backend.core.prazos.policy.domain;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;

public record PrazoPolicyResolutionResult(PrazoRegime regimeAplicado, boolean defensoria, boolean ministerioPublico, boolean fazenda) {}
