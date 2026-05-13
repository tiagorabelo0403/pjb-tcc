package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import java.util.List;

record InstitutionalOperationalDeskUnitFingerprint(
        String varaCluster,
        String specializationCluster,
        String groupingKey,
        String isolationMode,
        List<String> topology
) {
}
