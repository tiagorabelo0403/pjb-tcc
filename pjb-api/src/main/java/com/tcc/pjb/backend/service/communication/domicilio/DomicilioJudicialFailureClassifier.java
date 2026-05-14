package com.tcc.pjb.backend.service.communication.domicilio;

public final class DomicilioJudicialFailureClassifier {

    private DomicilioJudicialFailureClassifier() {}

    public enum FalhaClassificacao {
        TRANSITORIA,
        PERMANENTE,
        INDISPONIBILIDADE_SERVICO,
        DESTINATARIO_NAO_ENCONTRADO,
        CAIXA_CHEIA,
        CREDENCIAL_INVALIDA,
        DESCONHECIDA
    }

    public record FalhaClassificada(
            FalhaClassificacao classificacao,
            boolean recomendaRetry,
            String orientacao
    ) {}

    public FalhaClassificada classificar(int httpStatus, String mensagemErro) {
        if (httpStatus == 503 || httpStatus == 502 || httpStatus == 504) {
            return new FalhaClassificada(FalhaClassificacao.INDISPONIBILIDADE_SERVICO, true,
                    "Serviço indisponível — retry com backoff exponencial.");
        }
        if (httpStatus == 401 || httpStatus == 403) {
            return new FalhaClassificada(FalhaClassificacao.CREDENCIAL_INVALIDA, false,
                    "Credencial inválida ou expirada — renovar antes de retentar.");
        }
        if (httpStatus == 404) {
            return new FalhaClassificada(FalhaClassificacao.DESTINATARIO_NAO_ENCONTRADO, false,
                    "Destinatário não encontrado — verificar cadastro no domicílio judicial.");
        }
        if (mensagemErro != null && mensagemErro.contains("caixa cheia")) {
            return new FalhaClassificada(FalhaClassificacao.CAIXA_CHEIA, true,
                    "Caixa cheia — aguardar e retentar.");
        }
        if (httpStatus >= 500) {
            return new FalhaClassificada(FalhaClassificacao.TRANSITORIA, true,
                    "Erro de servidor — retry após intervalo.");
        }
        return new FalhaClassificada(FalhaClassificacao.DESCONHECIDA, false,
                "Falha não classificada — analisar log e acionar suporte se necessário.");
    }
}
