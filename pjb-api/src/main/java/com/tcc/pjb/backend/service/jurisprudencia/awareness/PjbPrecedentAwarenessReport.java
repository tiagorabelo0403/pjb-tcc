package com.tcc.pjb.backend.service.jurisprudencia.awareness;

import java.util.List;

public record PjbPrecedentAwarenessReport(String status,
                                          boolean bindingPrecedentDetected,
                                          boolean suspensionRecommended,
                                          List<PjbPrecedentSignal> signals,
                                          List<String> requiredActions) {
}
