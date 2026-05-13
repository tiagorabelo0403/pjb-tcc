package com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain;

import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioProcessualKind;
import com.tcc.pjb.backend.model.entity.enums.NationalCommunicationRecipientKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;

public record ResolucaoDestinatarioProcessualRequest(
        Long processoId,
        String numeroProcesso,
        TipoComunicacaoJudicial tipoComunicacao,
        NationalCommunicationRecipientKind legacyKind,
        DestinatarioProcessualKind destinatarioProcessualKind,
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
        Boolean intimacaoPessoalInstitucional,
        Boolean citacao,
        Boolean intimacao) {
}
