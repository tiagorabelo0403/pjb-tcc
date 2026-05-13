package com.tcc.pjb.backend.core.kernel.recursal.mesh;

public record RecursalTransitionDetails(
        String protocoloSaidaAutos,
        String canalRemessa,
        String protocoloRecebimentoDestino,
        String motivoDevolucaoRemessa,
        String pautaId,
        String sessaoId,
        String sustentante,
        String motivoAdiamentoSessao,
        String precedenteCodigo,
        String precedenteTribunal,
        String precedenteTema,
        String fundamentoDistincao,
        String orgaoSuscitante,
        String orgaoSuscitado,
        String juizoCompetenteCodigo,
        String tribunalCompetenteCodigo,
        String requisicaoPagamentoId,
        String modalidadePagamento) {

    public RecursalTransitionDetails {
        protocoloSaidaAutos = normalize(protocoloSaidaAutos);
        canalRemessa = normalize(canalRemessa);
        protocoloRecebimentoDestino = normalize(protocoloRecebimentoDestino);
        motivoDevolucaoRemessa = normalize(motivoDevolucaoRemessa);
        pautaId = normalize(pautaId);
        sessaoId = normalize(sessaoId);
        sustentante = normalize(sustentante);
        motivoAdiamentoSessao = normalize(motivoAdiamentoSessao);
        precedenteCodigo = normalize(precedenteCodigo);
        precedenteTribunal = normalize(precedenteTribunal);
        precedenteTema = normalize(precedenteTema);
        fundamentoDistincao = normalize(fundamentoDistincao);
        orgaoSuscitante = normalize(orgaoSuscitante);
        orgaoSuscitado = normalize(orgaoSuscitado);
        juizoCompetenteCodigo = normalize(juizoCompetenteCodigo);
        tribunalCompetenteCodigo = normalize(tribunalCompetenteCodigo);
        requisicaoPagamentoId = normalize(requisicaoPagamentoId);
        modalidadePagamento = normalize(modalidadePagamento);
    }

    public static RecursalTransitionDetails empty() {
        return new RecursalTransitionDetails(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
