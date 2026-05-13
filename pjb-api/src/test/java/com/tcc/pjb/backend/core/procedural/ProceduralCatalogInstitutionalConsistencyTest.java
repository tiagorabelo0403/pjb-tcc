package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProceduralCatalogInstitutionalConsistencyTest {

    private final ProceduralCatalogService catalogService = new ProceduralCatalogService();

    @Test
    void coverageMustStayAlignedWithCanonicalCatalog() {
        Map<String, Object> coverage = catalogService.coverage();

        int totalRitos = ((Number) coverage.get("totalRitos")).intValue();
        int withStages = ((Number) coverage.get("withStages")).intValue();
        int withRequiredParties = ((Number) coverage.get("withRequiredParties")).intValue();
        int withRequiredDocuments = ((Number) coverage.get("withRequiredDocuments")).intValue();
        int withExternalActor = ((Number) coverage.get("withExternalActor")).intValue();

        assertEquals(catalogService.catalogDrivenRitos().size(), totalRitos);
        assertEquals(totalRitos, withStages);
        assertEquals(totalRitos, withRequiredParties);
        assertEquals(totalRitos, withRequiredDocuments);
        assertEquals(totalRitos, withExternalActor);
        assertEquals(totalRitos, ((List<?>) coverage.get("items")).size());
    }

    @Test
    void criticalRitosMustExposeRequiredSchemasAndTransitions() {
        List<RitoProcessual> critical = List.of(
                RitoProcessual.COMUM_ORDINARIO,
                RitoProcessual.PROCEDIMENTO_PENAL_COMUM,
                RitoProcessual.TRABALHISTA_ORDINARIO,
                RitoProcessual.ELEITORAL,
                RitoProcessual.MILITAR,
                RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA,
                RitoProcessual.AGRARIO_DESAPROPRIACAO
        );

        for (RitoProcessual rito : critical) {
            var snapshot = catalogService.snapshot(rito);
            assertFalse(snapshot.stages().isEmpty(), rito.name());
            assertTrue(snapshot.parties().stream().anyMatch(ProceduralCatalogSupport.PartyRoleSpec::required), rito.name());
            assertTrue(snapshot.documents().stream().anyMatch(ProceduralCatalogSupport.DocumentSpec::required), rito.name());
            assertTrue(snapshot.stages().stream().allMatch(stage -> stage.getWork() != null && !stage.getWork().isEmpty()), rito.name());
        }
    }
}
