package com.tcc.pjb.backend.core.comunicacao.institucional.canonico.domain;

import java.util.List;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;

public record InstitutionalCanonicalCatalogEntry(
        AtoCanonicoProcessual atoCanonico,
        DestinatarioInstitucionalKind destinatarioKind,
        PapelProcessualInstitucional papelProcessual,
        TipoComunicacaoJudicial tipoComunicacao,
        boolean exigeCienciaPessoal,
        boolean bloqueiaMarcoProcessual,
        String gateCode,
        CanalComunicacaoInstitucional canalPrincipalSugerido,
        List<CanalComunicacaoInstitucional> fallbacksSugeridos,
        String fundamentoLegal,
        List<String> justificativas
) {
}
