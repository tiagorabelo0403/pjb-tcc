package com.tcc.pjb.backend.service.secretariat.topology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SecretariatSpecializationResolverTest {

    @Test
    void shouldSpecializeSecondInstanceElectoralSecretariatWithoutDuplicatingBaseMesh() {
        SecretariatSpecializationResolver resolver = new SecretariatSpecializationResolver();

        SecretariatSpecializationResolver.SecretariatSpecializationProfile profile = resolver.resolve(
                "TRE-CE",
                "SEGUNDO_GRAU",
                "JUSTICA_ELEITORAL",
                "ELEITORAL",
                "SECRETARIA_TRE_CE",
                "SEC:TRE:2G:ELEITORAL:CE:FORTALEZA",
                "SAN_SECRETARIA_TRE_CE",
                "AUD_SECRETARIA_TRE_CE",
                "EXEC_SECRETARIA_TRE_CE",
                "ELEITORAL>TRE_CE>SEGUNDO_GRAU>SECRETARIA",
                Map.of(
                        "laneAxis", "ELEITORAL",
                        "forumAxis", "FORO_ELEITORAL",
                        "unitDescriptor", "Secretaria Judiciária TRE-CE"
                )
        );

        assertEquals("SECRETARIA_SEGUNDA_INSTANCIA_ELEITORAL", profile.secretariatClass());
        assertEquals("SEGUNDA_INSTANCIA", profile.secretariatInstanceClass());
        assertEquals("ELEITORAL", profile.secretariatBranchClass());
        assertEquals("PJB_SEGUNDA_INSTANCIA", profile.namespacePjb());
        assertEquals("PJB Segunda Instância | Eleitoral", profile.painelPjb());
        assertTrue(profile.connectedCapabilities().contains("EMBARGOS"));
        assertTrue(profile.connectedCapabilities().contains("RECEBIMENTO"));
        assertTrue(profile.connectedCapabilities().contains("AUTUACAO_DISTRIBUICAO_ELEITORAL"));
        assertTrue(profile.metadata().containsKey("institutionOperatingModel"));
        assertTrue(profile.specializedSecretariatName().contains("TRE-CE"));
    }
}
