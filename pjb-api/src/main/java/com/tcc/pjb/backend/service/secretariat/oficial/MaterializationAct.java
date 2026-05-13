package com.tcc.pjb.backend.service.secretariat.oficial;

import java.util.Optional;

enum MaterializationAct {
    JUNTADA_FINAL_PROCESSUAL("Juntada final processual", "PROCESSUAL"),
    NOVA_EXPEDICAO_AO_OFICIAL("Nova expedição ao Oficial", "CUMPRIMENTO"),
    CONCLUSAO_AUTOMATICA_AO_GABINETE("Conclusão automática ao gabinete", "GABINETE"),
    ORDEM_JUDICIAL_SUGERIDA_AO_GABINETE("Ordem judicial sugerida ao gabinete", "GABINETE");

    private final String label;
    private final String category;

    MaterializationAct(String label, String category) {
        this.label = label;
        this.category = category;
    }

    static Optional<MaterializationAct> fromExternal(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase();
        for (MaterializationAct act : values()) {
            if (act.name().equals(normalized)) {
                return Optional.of(act);
            }
        }
        return Optional.empty();
    }

    String label() { return label; }
    String category() { return category; }
}
