package com.tcc.pjb.backend.model.dto.cidadao;

public record Links(
        String timelineUrl,
        String pastaDigitalUrl,
        String pastaDigitalBuscaUrl,
        String uiHistoryUrl,
        String uiHistorySseUrl,
        String instanciasUrl,
        String julgamentosUrl
) {}
