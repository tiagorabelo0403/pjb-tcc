package com.tcc.pjb.backend.service.rito.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.workflow.BpmnWorkflowGenerator;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProceduralWorkflowBpmnServiceTest {

    @Test
    void returnsExplicitUndefinedWorkflowWhenCanonicalContextIsIncomplete() {
        ProceduralCatalogService catalogService = new ProceduralCatalogService();
        ProceduralCanonicalResolver resolver = new ProceduralCanonicalResolver(catalogService);
        CanonicalSanityGate gate = new CanonicalSanityGate(catalogService);
        BpmnWorkflowGenerator generator = new BpmnWorkflowGenerator(catalogService);
        ProceduralWorkflowBpmnService service = new ProceduralWorkflowBpmnService(catalogService, generator, resolver, gate);

        var generated = service.generate(Map.of());

        assertEquals("INDEFINIDO", generated.rito());
        assertEquals("erro", generated.processId());
        assertTrue(generated.blueprint().containsKey("sanityStatus"));
    }
}
