package com.tcc.pjb.backend.core.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import org.junit.jupiter.api.Test;

class LegalCompilerServiceTest {

    @Test
    void devePropagarStatusEstruturalQuandoContextoCanonicoEstiverIncompleto() {
        ProceduralCatalogService catalogService = new ProceduralCatalogService();
        ProceduralCanonicalResolver resolver = new ProceduralCanonicalResolver(catalogService);
        CanonicalSanityGate gate = new CanonicalSanityGate(catalogService);
        LegalCompilerService service = new LegalCompilerService(resolver, gate);

        Processo processo = new Processo();
        LegalCompilerService.CompiledProcess compiled = service.compile(processo);

        assertTrue(compiled.isBlocking());
        assertEquals("CANONICAL_CONTEXT_INCOMPLETE", compiled.getStatus());
        assertTrue(compiled.getStatusCodes().contains("CANONICAL_CONTEXT_INCOMPLETE"));
        assertTrue(compiled.getMetadata().containsKey("sanityGate"));
    }
}
