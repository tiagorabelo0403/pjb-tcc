package com.tcc.pjb.backend.core.processo.recursal.domain;

public record ProcessoRecursalDecisionLayer(
        Integer ordemSequencial,
        String stageLabel,
        String decisionType,
        String tribunalOrigem,
        String unidadeOrigem,
        String numeroCnjOrigem,
        ProcessoRecursalDecisionSourceDocument documentoOriginal
) {
    public ProcessoRecursalDecisionLayer {
        ordemSequencial = ordemSequencial == null || ordemSequencial < 1 ? 1 : ordemSequencial;
        stageLabel = normalize(stageLabel);
        decisionType = normalize(decisionType);
        tribunalOrigem = normalize(tribunalOrigem);
        unidadeOrigem = normalize(unidadeOrigem);
        numeroCnjOrigem = normalize(numeroCnjOrigem);
    }

    public boolean available() {
        return documentoOriginal != null && documentoOriginal.available()
                || !blank(stageLabel)
                || !blank(decisionType)
                || !blank(numeroCnjOrigem)
                || !blank(tribunalOrigem)
                || !blank(unidadeOrigem);
    }

    public String organLabel() {
        if (!blank(unidadeOrigem) && !blank(tribunalOrigem)) {
            return unidadeOrigem + " / " + tribunalOrigem;
        }
        if (!blank(unidadeOrigem)) {
            return unidadeOrigem;
        }
        return tribunalOrigem;
    }

    public String headline() {
        if (documentoOriginal != null && documentoOriginal.available()) {
            return documentoOriginal.displayTitle();
        }
        return !blank(decisionType) ? decisionType : stageLabel;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String compact = value.trim().replaceAll("\\s+", " ");
        return compact.isBlank() ? null : compact;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
