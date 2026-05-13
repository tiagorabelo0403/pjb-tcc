package com.tcc.pjb.backend.service.jurisprudencia.graph;

import java.util.List;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class CitationExtractorTest {

    private final CitationExtractor extractor = new CitationExtractor();

    @Test
    void extractsCommonBrazilianCitationsDeterministically() {
        String text = "Aplica-se o Tema 1.234 do STF; Súmula 7 do STJ; art. 300 do CPC; e o RE 1.234.567/RS.";
        List<CitationRef> refs = extractor.extract(text);
        assertFalse(refs.isEmpty());

        assertTrue(refs.stream().anyMatch(r -> r.targetType() == CitationTargetType.TEMA_REPERCUSSAO && r.targetRef().contains("1234")));
        assertTrue(refs.stream().anyMatch(r -> r.targetType() == CitationTargetType.SUMULA && r.targetRef().startsWith("SUMULA:7")));
        assertTrue(refs.stream().anyMatch(r -> r.targetType() == CitationTargetType.ARTIGO_LEI && r.targetRef().contains("ART:300") && r.targetRef().endsWith(":CPC")));
        assertTrue(refs.stream().anyMatch(r -> r.targetType() == CitationTargetType.PRECEDENTE && r.targetRef().contains("STF:RE")));
    }
}
