package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public record NationalCommunicationInstitutionalResolveRequest(
        Long processoId,
        @NotNull DestinatarioInstitucionalKind destinatarioKind,
        @NotNull PapelProcessualInstitucional papelProcessual,
        RamoDireito ramoDireito,
        GrauJurisdicao grauJurisdicao,
        String uf,
        String comarca,
        String foro,
        String unidadeSugerida,
        String nucleoSugerido,
        String fundamentoLegal,
        Boolean exigeCienciaPessoal) {
}
