package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.schema.LegalAiSchemaDefinition;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.List;
import java.util.Map;

public interface LegalMcpServerProfile {

    LegalMcpServerDescriptor descriptor();

    int score(ResolveRequest request);

    default boolean supports(ResolveRequest request) {
        return score(request) > 0;
    }

    record ResolveRequest(
            String capability,
            ApiVersion version,
            String userProfile,
            String ramo,
            String rito,
            String processoId,
            boolean sigilo,
            boolean promptInjectionDetected,
            boolean quarantinedContext,
            List<String> attachments,
            List<String> history,
            List<LegalAiToolDescriptor> routedTools,
            LegalAiSchemaDefinition recommendedSchema,
            Map<String, Object> context
    ) {
    }
}
