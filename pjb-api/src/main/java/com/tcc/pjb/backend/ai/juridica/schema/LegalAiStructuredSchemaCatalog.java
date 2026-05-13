package com.tcc.pjb.backend.ai.juridica.schema;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.schema.LegalAiChecklistSchema;
import com.tcc.pjb.backend.model.dto.ai.legal.schema.LegalAiDecisaoSchema;
import com.tcc.pjb.backend.model.dto.ai.legal.schema.LegalAiDespachoSchema;
import com.tcc.pjb.backend.model.dto.ai.legal.schema.LegalAiDraftEnvelopeSchema;
import com.tcc.pjb.backend.model.dto.ai.legal.schema.LegalAiParecerSchema;
import com.tcc.pjb.backend.model.dto.ai.legal.schema.LegalAiProceduralPlanSchema;
import com.tcc.pjb.backend.model.dto.ai.legal.schema.LegalAiRiskReportSchema;
import com.tcc.pjb.backend.model.dto.ai.legal.schema.LegalAiSchemaDefinition;
import com.tcc.pjb.backend.model.dto.ai.legal.schema.LegalAiTriageSchema;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalAiStructuredSchemaCatalog {

    public List<LegalAiSchemaDefinition> resolve(ApiVersion version) {
        ApiVersion effectiveVersion = version == null ? ApiVersion.latest() : version;
        if (effectiveVersion == ApiVersion.V1) {
            return List.of(
                    LegalAiTriageSchema.definition(),
                    LegalAiChecklistSchema.definition()
            );
        }
        if (effectiveVersion == ApiVersion.V2) {
            return List.of(
                    LegalAiTriageSchema.definition(),
                    LegalAiChecklistSchema.definition(),
                    LegalAiProceduralPlanSchema.definition(),
                    LegalAiParecerSchema.definition(),
                    LegalAiRiskReportSchema.definition()
            );
        }
        return List.of(
                LegalAiTriageSchema.definition(),
                LegalAiChecklistSchema.definition(),
                LegalAiProceduralPlanSchema.definition(),
                LegalAiParecerSchema.definition(),
                LegalAiRiskReportSchema.definition(),
                LegalAiDraftEnvelopeSchema.definition(),
                LegalAiDespachoSchema.definition(),
                LegalAiDecisaoSchema.definition()
        );
    }

    public LegalAiSchemaDefinition recommend(ApiVersion version,
                                             String capability,
                                             LegalAiConversationRequest request) {
        Objects.requireNonNull(version == null ? ApiVersion.latest() : version);
        List<LegalAiSchemaDefinition> available = resolve(version);
        String capabilityHint = normalize(capability);
        String messageHint = normalize(request == null ? null : request.message());
        String profileHint = normalize(request == null ? null : request.userProfile());

        if (containsAny(messageHint, "despacho", "determino", "intime-se", "cite-se") || containsAny(capabilityHint, "DESPACHO")) {
            return byIdOrFallback(available, "LEGAL_AI_DESPACHO_SCHEMA");
        }
        if (containsAny(messageHint, "decisão", "decisao", "julgo", "sentença", "sentenca", "acordao", "acórdão")
                || containsAny(capabilityHint, "DECISAO", "DECISION", "SENTENCA")) {
            return byIdOrFallback(available, "LEGAL_AI_DECISAO_SCHEMA");
        }
        if (containsAny(messageHint, "parecer", "opino", "consulta", "manifestação", "manifestacao")
                || containsAny(capabilityHint, "PARECER")) {
            return byIdOrFallback(available, "LEGAL_AI_PARECER_SCHEMA");
        }
        if (containsAny(messageHint, "checklist", "itens", "conferir", "validar") || containsAny(capabilityHint, "CHECKLIST")) {
            return byIdOrFallback(available, "LEGAL_AI_CHECKLIST_SCHEMA");
        }
        if (containsAny(messageHint, "risco", "probabilidade", "impacto") || containsAny(capabilityHint, "RISK", "RISCO")) {
            return byIdOrFallback(available, "LEGAL_AI_RISK_REPORT_SCHEMA");
        }
        if (containsAny(messageHint, "plano", "estratégia", "estrategia", "próximos passos", "proximos passos")
                || containsAny(capabilityHint, "PLAN", "PROCEDURAL_PLAN")) {
            return byIdOrFallback(available, "LEGAL_AI_PROCEDURAL_PLAN");
        }
        if (containsAny(profileHint, "MAGISTRADO") && version == ApiVersion.V3) {
            return byIdOrFallback(available, "LEGAL_AI_DECISAO_SCHEMA");
        }
        if (version == ApiVersion.V3) {
            return byIdOrFallback(available, "LEGAL_AI_DRAFT_ENVELOPE");
        }
        if (version == ApiVersion.V2) {
            return byIdOrFallback(available, "LEGAL_AI_PROCEDURAL_PLAN");
        }
        return byIdOrFallback(available, "LEGAL_AI_TRIAGE_RESULT");
    }

    private LegalAiSchemaDefinition byIdOrFallback(List<LegalAiSchemaDefinition> available, String schemaId) {
        return available.stream()
                .filter(item -> schemaId.equals(item.schemaId()))
                .findFirst()
                .orElseGet(() -> available.isEmpty() ? LegalAiTriageSchema.definition() : available.getFirst());
    }

    private boolean containsAny(String value, String... terms) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String term : terms) {
            if (term != null && !term.isBlank() && value.contains(normalize(term))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
