package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralPartyProfileResolverTest {

    private final NationalProceduralPartyProfileResolver resolver = new NationalProceduralPartyProfileResolver();

    @Test
    void mustClassifyFederalPublicPartyAndLaborSignals() {
        NationalProceduralPartyProfile profile = resolver.resolve(
                Map.of(
                        "parteReuNome", "Instituto Nacional do Seguro Social - INSS",
                        "parteAutoraNome", "Maria da Silva",
                        "envolveRelacaoTrabalho", true
                ),
                "pedido de horas extras com discussão previdenciária contra o INSS"
        );

        assertTrue(profile.federal());
        assertTrue(profile.autarquiaFederal());
        assertTrue(profile.publicParty());
        assertTrue(profile.trabalho());
        assertTrue(profile.tags().contains("PARTE_FEDERAL"));
        assertTrue(profile.tags().contains("AUTARQUIA_FEDERAL"));
        assertTrue(profile.tags().contains("RELACAO_TRABALHO"));
        assertEquals("MARIA DA SILVA", profile.autor());
    }

    @Test
    void mustClassifyMunicipalAndElectoralSignals() {
        NationalProceduralPartyProfile profile = resolver.resolve(
                Map.of(
                        "parteReuNome", "Prefeitura Municipal de Exemplo",
                        "envolveEleitoral", true
                ),
                "representação perante a zona eleitoral"
        );

        assertTrue(profile.municipal());
        assertTrue(profile.publicParty());
        assertTrue(profile.eleitoral());
        assertTrue(profile.tags().contains("PARTE_MUNICIPAL"));
        assertTrue(profile.tags().contains("MATERIA_ELEITORAL"));
    }
}
