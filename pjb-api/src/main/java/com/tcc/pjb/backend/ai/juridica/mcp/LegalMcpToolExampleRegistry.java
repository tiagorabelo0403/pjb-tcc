package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.ai.juridica.mcp.support.LegalMcpToolExampleCatalogService;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpSkillDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpToolExample;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalMcpToolExampleRegistry {

    private final LegalMcpToolExampleCatalogService toolExampleCatalogService;

    public LegalMcpToolExampleRegistry(LegalMcpToolExampleCatalogService toolExampleCatalogService) {
        this.toolExampleCatalogService = Objects.requireNonNull(toolExampleCatalogService, "toolExampleCatalogService");
    }

    public List<LegalMcpToolExample> resolve(LegalMcpServerProfile.ResolveRequest request,
                                             List<LegalMcpSkillDescriptor> skills,
                                             List<LegalMcpServerDescriptor> pinnedServers) {
        LinkedHashSet<String> exampleIds = new LinkedHashSet<>();
        if (hasAttachments(request)) {
            String signatureExample = toolExampleCatalogService.exampleIdForTool("document.signature.verify");
            if (signatureExample != null) {
                exampleIds.add(signatureExample);
            }
        }
        if (skills != null) {
            skills.stream()
                    .filter(Objects::nonNull)
                    .flatMap(skill -> skill.preferredToolIds().stream())
                    .map(toolExampleCatalogService::exampleIdForTool)
                    .filter(Objects::nonNull)
                    .forEach(exampleIds::add);
        }
        if (pinnedServers != null) {
            pinnedServers.stream()
                    .filter(Objects::nonNull)
                    .flatMap(server -> server.tools().stream())
                    .map(tool -> toolExampleCatalogService.exampleIdForTool(tool.toolId()))
                    .filter(Objects::nonNull)
                    .limit(request.promptInjectionDetected() || request.quarantinedContext() ? 2 : 5)
                    .forEach(exampleIds::add);
        }
        return exampleIds.stream()
                .map(toolExampleCatalogService::example)
                .filter(Objects::nonNull)
                .limit(resolveBudget(request))
                .toList();
    }

    public List<LegalMcpToolExample> catalog() {
        return toolExampleCatalogService.catalog();
    }

    private int resolveBudget(LegalMcpServerProfile.ResolveRequest request) {
        if (request == null) {
            return 5;
        }
        if (request.promptInjectionDetected() || request.quarantinedContext()) {
            return 2;
        }
        if (request.sigilo() && hasAttachments(request)) {
            return 8;
        }
        return request.sigilo() ? 6 : 5;
    }

    private boolean hasAttachments(LegalMcpServerProfile.ResolveRequest request) {
        return request != null && request.attachments() != null && !request.attachments().isEmpty();
    }
}
