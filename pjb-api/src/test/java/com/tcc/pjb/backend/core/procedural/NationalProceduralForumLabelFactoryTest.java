package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralForumLabelFactoryTest {

    private final NationalProceduralForumLabelFactory factory = new NationalProceduralForumLabelFactory();

    @Test
    void mustBuildFederalForoLabel() {
        assertEquals(
                "Subseção Judiciária de Fortaleza/CE",
                factory.buildForoLabel("Fortaleza", "ce", TipoJustica.FEDERAL)
        );
    }

    @Test
    void mustPreferPayloadVaraAndJuizadoOverrides() {
        NationalProceduralActionProfile actionProfile = new NationalProceduralActionProfile(
                "INDENIZATORIA",
                "CIVIL_GERAL",
                false,
                "COMUM_ORDINARIO",
                "Vara Cível",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        NationalProceduralJuizadoDecision juizadoDecision = new NationalProceduralJuizadoDecision(
                true,
                "JUIZADO_ESPECIAL_FAZENDA_PUBLICA",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0.89d,
                false
        );

        assertEquals(
                "Vara pretendida customizada",
                factory.buildVaraLabel("COMUM_ORDINARIO", actionProfile, TipoJustica.ESTADUAL, juizadoDecision, Map.of("varaPretendida", "Vara pretendida customizada"))
        );
        assertEquals(
                "Juizado Especial da Fazenda Pública",
                factory.buildVaraLabel("COMUM_ORDINARIO", actionProfile, TipoJustica.ESTADUAL, juizadoDecision, Map.of())
        );
    }
}
