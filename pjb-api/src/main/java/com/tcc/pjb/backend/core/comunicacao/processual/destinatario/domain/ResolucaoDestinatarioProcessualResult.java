package com.tcc.pjb.backend.core.comunicacao.processual.destinatario.domain;

import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.enums.TrilhoComunicacaoProcessual;

public record ResolucaoDestinatarioProcessualResult(
        DestinatarioProcessual destinatario,
        TrilhoComunicacaoProcessual trilho,
        boolean usaFluxoPessoal,
        boolean usaFluxoInstitucional,
        boolean admiteCitacao,
        boolean admiteIntimacao,
        List<String> justificativas,
        String hashResolucao) {

    public ResolucaoDestinatarioProcessualResult {
        destinatario = Objects.requireNonNull(destinatario);
        trilho = Objects.requireNonNull(trilho);
        justificativas = PayloadMaps.copyTrimmedStrings(justificativas);
        hashResolucao = hashResolucao == null || hashResolucao.isBlank() ? destinatario.hashResolucao() : hashResolucao;
    }
}
