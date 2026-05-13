package com.tcc.pjb.backend.ai.juridica.spine;

import java.util.List;

public final class JuridicaSpineLabels {

    private JuridicaSpineLabels() {
    }

    public static final String PROFILE_LEGAL_SPINE = "LEGAL_AI_SPINE";
    public static final String PIPELINE_LEGAL_HYBRID_RAG = "LEGAL_HYBRID_RAG_PIPELINE";
    public static final String TRACE_LANE = "LEGAL_DECISION_TRACE";
    public static final String MULTIMODAL_LANE = "LEGAL_MULTIMODAL_EVIDENCE";
    public static final String PROVENANCE_ENVELOPE = "LEGAL_PROVENANCE_ENVELOPE";
    public static final String APPROVAL_NONE = "AUTO_READONLY";
    public static final String APPROVAL_STEP_UP = "STEP_UP_REQUIRED";
    public static final String APPROVAL_HUMAN = "HUMAN_REVIEW_REQUIRED";
    public static final String CAPABILITY_RESEARCH_DOSSIER = "LEGAL_RESEARCH_DOSSIER_V2";
    public static final String CAPABILITY_VALIDATE_ENVELOPE = "LEGAL_VALIDATE_ENVELOPE_V3";
    public static final String CAPABILITY_HALLUCINATION_GUARD = "LEGAL_HALLUCINATION_GUARD_V3";
    public static final String CAPABILITY_CONVERSATION = "LEGAL_CONVERSATION_ORCHESTRATED_V3";
    public static final String UNRESOLVED_CITATION_PLACEHOLDER = "[NAO_CONFIRMADO]";

    public static List<String> defaultAuditFields() {
        return List.of(
                "requestId",
                "correlationId",
                "version",
                "capability",
                "tools",
                "sources",
                "authorityFloor",
                "citationFirst",
                "sigilo",
                "approval"
        );
    }

}
