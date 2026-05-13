package com.tcc.pjb.backend.service.rito.workflow;

import com.tcc.pjb.backend.core.workflow.BpmnWorkflowGenerator;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate.GateResult;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.workflow.BpmnWorkflowGenerator.BpmnOutput;
import com.tcc.pjb.backend.core.workflow.BpmnWorkflowGenerator.WorkflowConsistencyReport;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProceduralWorkflowBpmnService {

    public record GeneratedWorkflow(String rito, String processId, String bpmnXml, String checksum, Map<String, Object> blueprint) {
    }

    private final ProceduralCatalogService proceduralCatalogService;
    private final BpmnWorkflowGenerator bpmnWorkflowGenerator;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final CanonicalSanityGate canonicalSanityGate;

    public ProceduralWorkflowBpmnService(ProceduralCatalogService proceduralCatalogService,
                                         BpmnWorkflowGenerator bpmnWorkflowGenerator,
                                         ProceduralCanonicalResolver proceduralCanonicalResolver,
                                         CanonicalSanityGate canonicalSanityGate) {
        this.proceduralCatalogService = Objects.requireNonNull(proceduralCatalogService);
        this.bpmnWorkflowGenerator = Objects.requireNonNull(bpmnWorkflowGenerator);
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.canonicalSanityGate = Objects.requireNonNull(canonicalSanityGate);
    }

    public GeneratedWorkflow generate(RitoProcessual rito) {
        return generate(Objects.requireNonNull(rito).name());
    }

    public GeneratedWorkflow generate(String ritoName) {
        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(
                ritoName == null || ritoName.isBlank() ? Map.of() : Map.of("rito", ritoName)
        );
        return generate(canonicalContext);
    }

    public GeneratedWorkflow generate(CanonicalContext canonicalContext) {
        Objects.requireNonNull(canonicalContext);
        GateResult gateResult = canonicalSanityGate.evaluate(canonicalContext);
        if (canonicalContext.rito() == null) {
            LinkedHashMap<String, Object> blueprint = new LinkedHashMap<>();
            blueprint.put("canonicalContext", canonicalContext.toMap());
            blueprint.put("sanityGate", gateResult.toMap());
            blueprint.put("sanityStatus", gateResult.overallStatus());
            blueprint.put("blockingSanityIssues", gateResult.hasBlockingIssues());
            return new GeneratedWorkflow("INDEFINIDO", "erro", "", "", Map.copyOf(blueprint));
        }
        BpmnOutput output = bpmnWorkflowGenerator.generate(canonicalContext);
        Map<String, Object> blueprint = new LinkedHashMap<>(output.blueprint());
        blueprint.putIfAbsent("catalogSnapshot", proceduralCatalogService.snapshot(output.rito()).title());
        blueprint.putIfAbsent("catalogDriven", true);
        blueprint.put("canonicalContext", canonicalContext.toMap());
        blueprint.put("sanityGate", gateResult.toMap());
        blueprint.put("sanityStatus", gateResult.overallStatus());
        blueprint.put("blockingSanityIssues", gateResult.hasBlockingIssues());
        return new GeneratedWorkflow(output.rito().name(), output.processId(), output.xml(), output.sha256(), Map.copyOf(blueprint));
    }

    public GeneratedWorkflow generate(Map<String, Object> payload) {
        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(payload == null ? Map.of() : payload);
        return generate(canonicalContext);
    }

    public Map<String, GeneratedWorkflow> generateCatalogDriven() {
        LinkedHashMap<String, GeneratedWorkflow> out = new LinkedHashMap<>();
        for (RitoProcessual rito : proceduralCatalogService.catalogDrivenRitos()) {
            GeneratedWorkflow generated = generate(rito);
            out.put(generated.rito(), generated);
        }
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> blueprint(RitoProcessual rito) {
        return generate(rito).blueprint();
    }

    public Map<String, Object> blueprint(String ritoName) {
        return generate(ritoName).blueprint();
    }

    public WorkflowConsistencyReport consistencyGate() {
        return bpmnWorkflowGenerator.consistencyGate();
    }
}
