package com.tcc.pjb.backend.model.dto.processual.comunicacao.routing;

import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public record NationalCommunicationRoutingResolveRequest(
        Long processoId,
        @NotNull DestinatarioInstitucionalKind destinatarioKind,
        @NotNull PapelProcessualInstitucional papelProcessual,
        @NotNull TipoComunicacaoJudicial tipoComunicacao,
        AtoCanonicoProcessual atoCanonico,
        RamoDireito ramoDireito,
        GrauJurisdicao grauJurisdicao,
        String uf,
        String comarca,
        String foro,
        String unidadeSugerida,
        String nucleoSugerido,
        String fundamentoLegal,
        Boolean exigeCienciaPessoal,
        CanalComunicacaoInstitucional canalPreferencial,
        Boolean urgente,
        Boolean bloqueioFluxoSensivel) {
}
