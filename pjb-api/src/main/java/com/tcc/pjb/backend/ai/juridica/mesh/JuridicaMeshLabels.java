package com.tcc.pjb.backend.ai.juridica.mesh;

import java.util.List;

public final class JuridicaMeshLabels {

    private JuridicaMeshLabels() {
    }

    public static final String PROFILE_BALANCED = "LEGAL_JURIDICA_MESH_BALANCED_2026";
    public static final String PROFILE_STRICT = "LEGAL_JURIDICA_MESH_STRICT_2026";
    public static final String PROFILE_PROTOCOL = "LEGAL_JURIDICA_MESH_PROTOCOL_2026";
    public static final String RAG_MODE = "HYBRID_BM25_DENSE_RERANK_HIERARCHY";
    public static final String MCP_SESSION_MODE = "SERVER_MANAGED_READ_ONLY";
    public static final String RUNTIME_VIRTUAL_THREAD_SPINE = "PJB_VIRTUAL_THREAD_SPINE";
    public static final String RUNTIME_EXECUTION_GOVERNANCE = "PJB_EXECUTION_ORCHESTRATOR";

    public static List<String> qualityFilters() {
        return List.of(
                "PROMPT_INJECTION_FENCE",
                "LEGAL_SAFETY_GATE",
                "EVIDENCE_SUFFICIENCY",
                "CONTRADICTION_RESOLUTION",
                "TEMPORAL_VALIDITY",
                "AUTHORITY_FLOOR",
                "PROCEDURAL_COMPATIBILITY",
                "SIGILO_VISIBILITY_POLICY",
                "CITATION_FIRST_OUTPUT"
        );
    }

    public static List<String> memoryScopes() {
        return List.of(
                "INSTITUTIONAL_POLICY_MEMORY",
                "PROCESS_SCOPED_MEMORY",
                "PROFILE_SCOPED_MEMORY",
                "SESSION_EPHEMERAL_MEMORY"
        );
    }

    public static List<String> mcpServers() {
        return List.of(
                "MCP_LEGISLACAO",
                "MCP_JURISPRUDENCIA",
                "MCP_PROCESSUAL",
                "MCP_DOCUMENTAL",
                "MCP_AGENDA_PRAZOS",
                "MCP_INTEROPERABILIDADE"
        );
    }

    public static List<String> legalDepthSources() {
        return List.of(
                "CONSTITUICAO_E_CODIGOS",
                "LEIS_ESPECIAIS_E_ATOS_NORMATIVOS",
                "JURISPRUDENCIA_E_PRECEDENTES_QUALIFICADOS",
                "TPU_E_TAXONOMIA_PROCESSUAL",
                "CURRICULUM_JURIDICO_INTERNO",
                "PLAYBOOKS_E_WORKSPACES_DO_PJB"
        );
    }
}
