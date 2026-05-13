package com.tcc.pjb.backend.integration.mni.domain;

public record MniReprocessamentoSummary(int processadas,
                                        int superseded,
                                        int ignoradas) {
}
