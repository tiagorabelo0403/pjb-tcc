package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.ai.juridica.schema.LegalAiStructuredSchemaCatalog;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiStructuredOutputDescriptor;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JuridicaStructuredOutputProfileService {

    private final LegalAiStructuredSchemaCatalog structuredSchemaCatalog;

    public JuridicaStructuredOutputProfileService(LegalAiStructuredSchemaCatalog structuredSchemaCatalog) {
        this.structuredSchemaCatalog = Objects.requireNonNull(structuredSchemaCatalog, "structuredSchemaCatalog");
    }

    public List<LegalAiStructuredOutputDescriptor> resolve(ApiVersion version) {
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        return structuredSchemaCatalog.resolve(effectiveVersion).stream()
                .map(schema -> {
                    LinkedHashMap<String, Object> hints = new LinkedHashMap<>();
                    hints.put("output", schema.label());
                    hints.put("requiredSections", schema.requiredKeys());
                    hints.put("supportedVersions", schema.supportedVersions());
                    hints.put("audienceProfiles", schema.audienceProfiles());
                    hints.put("fieldKeys", schema.fields().stream().map(field -> field.key()).toList());
                    return new LegalAiStructuredOutputDescriptor(
                            schema.schemaId(),
                            schema.stage(),
                            schema.requiredKeys(),
                            schema.citationFirst(),
                            schema.symbolicValidationRequired(),
                            Map.copyOf(hints)
                    );
                })
                .toList();
    }
}
