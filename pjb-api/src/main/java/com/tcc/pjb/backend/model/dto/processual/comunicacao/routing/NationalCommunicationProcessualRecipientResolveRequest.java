package com.tcc.pjb.backend.model.dto.processual.comunicacao.routing;

import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioProcessualKind;
import com.tcc.pjb.backend.model.entity.enums.NationalCommunicationRecipientKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;

public record NationalCommunicationProcessualRecipientResolveRequest(
        Long processoId,
        TipoComunicacaoJudicial tipoComunicacao,
        NationalCommunicationRecipientKind destinatarioTipo,
        DestinatarioProcessualKind destinatarioProcessualTipo,
        DestinatarioInstitucionalKind destinatarioInstitucionalKind,
        PapelProcessualInstitucional papelProcessualInstitucional,
        String unidadeInstitucionalCodigo,
        String documento,
        String nome,
        String email,
        String telefone,
        String oabNumero,
        String govbrAccountId,
        String uf,
        String comarca,
        String foro,
        Boolean possuiContaGovBr,
        Boolean possuiAdvogado,
        Boolean fazendaPublica,
        Boolean intimacaoPessoalInstitucional) {
}
