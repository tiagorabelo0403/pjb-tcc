package com.tcc.pjb.backend.core.comunicacao.institucional.canonico;

import java.util.List;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;

public record PoliticaAtoCanonicoProcessual(
        AtoCanonicoProcessual atoCanonico,
        DestinatarioInstitucionalKind destinatarioKind,
        PapelProcessualInstitucional papelProcessual,
        TipoComunicacaoJudicial tipoComunicacao,
        boolean exigeCienciaPessoal,
        boolean bloqueiaFluxo,
        String gateCode,
        String fundamentoLegal,
        List<String> justificativasPadrao
) {
    public PoliticaAtoCanonicoProcessual {
        if (atoCanonico == null) {
            throw new IllegalArgumentException("atoCanonico é obrigatório");
        }
        if (destinatarioKind == null) {
            throw new IllegalArgumentException("destinatarioKind é obrigatório");
        }
        if (papelProcessual == null) {
            throw new IllegalArgumentException("papelProcessual é obrigatório");
        }
        if (tipoComunicacao == null) {
            throw new IllegalArgumentException("tipoComunicacao é obrigatório");
        }
        if (fundamentoLegal == null || fundamentoLegal.isBlank()) {
            throw new IllegalArgumentException("fundamentoLegal é obrigatório");
        }
        justificativasPadrao = normalizeJustificativas(justificativasPadrao);
        gateCode = gateCode == null || gateCode.isBlank() ? null : gateCode.trim();
        fundamentoLegal = fundamentoLegal.trim();
    }

    private static List<String> normalizeJustificativas(List<String> justificativasPadrao) {
        if (justificativasPadrao == null || justificativasPadrao.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<String> normalized = new java.util.ArrayList<>(justificativasPadrao.size());
        for (String justificativa : justificativasPadrao) {
            if (justificativa != null && !justificativa.isBlank()) {
                normalized.add(justificativa.trim());
            }
        }
        return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }
}
