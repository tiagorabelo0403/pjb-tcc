package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import java.util.LinkedHashSet;

record InstitutionalPanelProvisioningSnapshot(String initialRoute,
                                              LinkedHashSet<String> primarySections,
                                              LinkedHashSet<String> quickActions,
                                              LinkedHashSet<String> securityGuards,
                                              LinkedHashSet<String> visibilityRules,
                                              LinkedHashSet<String> tabs,
                                              LinkedHashSet<String> fundamentos) {
}
