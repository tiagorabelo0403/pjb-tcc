package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralProbatoryProfileResolverTest {

    @Test
    void mustDetectComplexMedicalEvidence() {
        NationalProceduralProbatoryProfileResolver resolver = new NationalProceduralProbatoryProfileResolver();

        String profile = resolver.resolve(Map.of("materialProbatorioResumo", "Laudo medico complexo com analise de cirurgia e nexo causal medico"), "");

        assertEquals("MEDICA_COMPLEXA", profile);
    }

    @Test
    void mustFallbackToSimpleDocumentalEvidence() {
        NationalProceduralProbatoryProfileResolver resolver = new NationalProceduralProbatoryProfileResolver();

        String profile = resolver.resolve(Map.of("provas", "documentos contratuais e notificacao extrajudicial"), "");

        assertEquals("DOCUMENTAL_SIMPLES", profile);
    }
}
