package com.tcc.pjb.backend.service.processual.representacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RepresentacaoProcessualPolicyJsonExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveExtrairEnvelopeNormalizadoDaRepresentationPolicy() {
        String poderes = """
                {
                  "representationPolicy": {
                    "regularidadeSuficiente": false,
                    "alertas": ["  Procuração incompleta  ", null],
                    "nested": {" chave ": " valor "},
                    "termoAudienciaReferencia": " termo-123 "
                  }
                }
                """;

        Map<String, Object> extracted = RepresentacaoProcessualPolicyJsonExtractor.extract(objectMapper, poderes);

        assertEquals(Boolean.FALSE, extracted.get("regularidadeSuficiente"));
        assertEquals(List.of("Procuração incompleta"), extracted.get("alertas"));
        assertEquals(Map.of("chave", "valor"), extracted.get("nested"));
        assertEquals("termo-123", extracted.get("termoAudienciaReferencia"));
    }

    @Test
    void deveExporConvenienciasDoEnvelope() {
        String poderes = """
                {
                  "representationPolicy": {
                    "regularidadeSuficiente": false,
                    "alertas": ["Documento faltante"],
                    "exigeTermoOuAtaAudiencia": true,
                    "ataAudienciaReferencia": "ata-7"
                  }
                }
                """;

        assertFalse(RepresentacaoProcessualPolicyJsonExtractor.regularidadeSuficiente(objectMapper, poderes));
        assertTrue(RepresentacaoProcessualPolicyJsonExtractor.exigeTermoOuAta(objectMapper, poderes));
        assertEquals("Documento faltante", RepresentacaoProcessualPolicyJsonExtractor.firstAlerta(objectMapper, poderes, "fallback"));
        assertTrue(RepresentacaoProcessualPolicyJsonExtractor.hasReference(objectMapper, poderes, "ataAudienciaReferencia"));
        assertFalse(RepresentacaoProcessualPolicyJsonExtractor.hasReference(objectMapper, poderes, "termoAudienciaReferencia"));
    }

    @Test
    void jsonInvalidoOuVazioDeveRetornarEnvelopeVazio() {
        assertTrue(RepresentacaoProcessualPolicyJsonExtractor.extract(objectMapper, null).isEmpty());
        assertTrue(RepresentacaoProcessualPolicyJsonExtractor.extract(objectMapper, " ").isEmpty());
        assertTrue(RepresentacaoProcessualPolicyJsonExtractor.extract(objectMapper, "{invalido").isEmpty());
    }
}
