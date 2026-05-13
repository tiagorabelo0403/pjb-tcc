package com.tcc.pjb.backend.service.api;

import java.util.Set;

public record PjbJudicialServiceOffering(String providerCode,
                                         PjbJudicialServiceCategory category,
                                         Set<String> supportedTribunals,
                                         boolean homologated,
                                         boolean audited,
                                         boolean lgpdReady,
                                         boolean humanSupervisionRequired) {
}
