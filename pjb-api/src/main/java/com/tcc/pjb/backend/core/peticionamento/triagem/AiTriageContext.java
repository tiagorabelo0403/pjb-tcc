package com.tcc.pjb.backend.core.peticionamento.triagem;

import java.util.List;

public record AiTriageContext(
        Long rascunhoId,
        String classeProcessual,
        String assunto,
        String ramoDireito,
        String comarca,
        String uf,
        List<String> tiposDocumentosAnexados,
        String textoExtraidoPrincipal,
        AiTriageSuggestionLayer camada
) {
    public AiTriageContext {
        tiposDocumentosAnexados = tiposDocumentosAnexados == null ? List.of() : List.copyOf(tiposDocumentosAnexados);
        camada = camada == null ? AiTriageSuggestionLayer.CAMADA_C_ASSISTIVA : camada;
    }
}
