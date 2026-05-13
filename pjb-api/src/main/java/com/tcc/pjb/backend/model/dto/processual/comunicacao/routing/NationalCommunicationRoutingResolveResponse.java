package com.tcc.pjb.backend.model.dto.processual.comunicacao.routing;

import java.util.List;

public record NationalCommunicationRoutingResolveResponse(
        String destinatarioKind,
        String papelProcessual,
        String tipoComunicacaoEfetiva,
        String unidadeCodigo,
        String unidadeNome,
        String caixaCodigo,
        String caixaNome,
        String canalPrincipal,
        List<String> canaisFallback,
        int slaCienciaHoras,
        int slaRespostaHoras,
        boolean forcarDigital,
        boolean forcarOficial,
        boolean bloqueiaFluxo,
        String gateCode,
        String tribunalCodigo,
        String uf,
        String comarca,
        String foro,
        String hashResolucao,
        List<String> justificativas,
        String catalogVersion) {
}
