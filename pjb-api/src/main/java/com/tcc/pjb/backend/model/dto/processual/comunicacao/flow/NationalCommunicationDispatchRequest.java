package com.tcc.pjb.backend.model.dto.processual.comunicacao.flow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioProcessualKind;
import com.tcc.pjb.backend.model.entity.enums.NationalCommunicationRecipientKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;

public record NationalCommunicationDispatchRequest(
        @NotNull Long processoId,
        @NotNull TipoComunicacaoJudicial tipoComunicacao,
        @NotNull NationalCommunicationRecipientKind destinatarioTipo,
        @NotBlank String documento,
        @NotBlank String nome,
        String email,
        String telefone,
        String govbrAccountId,
        String oabNumero,
        String uf,
        String razaoSocial,
        String ente,
        Boolean possuiContaGovBr,
        Boolean possuiAdvogado,
        Boolean grandeEmpresa,
        Boolean banco,
        Boolean fazendaPublica,
        Boolean cnpjAtivo,
        DestinatarioProcessualKind destinatarioProcessualTipo,
        DestinatarioInstitucionalKind destinatarioInstitucionalKind,
        PapelProcessualInstitucional papelProcessualInstitucional,
        String unidadeInstitucionalCodigo,
        String comarca,
        String foro,
        Boolean intimacaoPessoalInstitucional,
        Boolean forcarDigital,
        Boolean forcarOficial,
        @NotBlank String conteudoDoAto,
        String fundamentoAdicional) {
}
