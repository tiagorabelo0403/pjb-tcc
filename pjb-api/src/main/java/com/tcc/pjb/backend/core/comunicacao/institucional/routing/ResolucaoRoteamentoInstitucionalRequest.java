package com.tcc.pjb.backend.core.comunicacao.institucional.routing;

import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public record ResolucaoRoteamentoInstitucionalRequest(
        Long processoId,
        String processoNumero,
        DestinatarioInstitucionalKind destinatarioKind,
        PapelProcessualInstitucional papelProcessual,
        TipoComunicacaoJudicial tipoComunicacaoSolicitada,
        AtoCanonicoProcessual atoCanonico,
        RamoDireito ramoDireito,
        GrauJurisdicao grauJurisdicao,
        String uf,
        String comarca,
        String foro,
        String unidadeSugerida,
        String nucleoSugerido,
        String fundamentoLegal,
        boolean exigeCienciaPessoal,
        CanalComunicacaoInstitucional canalPreferencial,
        boolean urgente,
        boolean bloqueioFluxoSensivel
) {
    public ResolucaoRoteamentoInstitucionalRequest {
        if (destinatarioKind == null) {
            throw new IllegalArgumentException("destinatarioKind é obrigatório");
        }
        if (papelProcessual == null) {
            throw new IllegalArgumentException("papelProcessual é obrigatório");
        }
        if (tipoComunicacaoSolicitada == null) {
            throw new IllegalArgumentException("tipoComunicacaoSolicitada é obrigatória");
        }
        processoNumero = normalize(processoNumero);
        uf = normalize(uf);
        comarca = normalize(comarca);
        foro = normalize(foro);
        unidadeSugerida = normalize(unidadeSugerida);
        nucleoSugerido = normalize(nucleoSugerido);
        fundamentoLegal = normalize(fundamentoLegal);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
