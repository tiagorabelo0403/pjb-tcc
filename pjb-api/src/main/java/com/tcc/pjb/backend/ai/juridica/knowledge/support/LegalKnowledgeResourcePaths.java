package com.tcc.pjb.backend.ai.juridica.knowledge.support;

import java.util.List;

public final class LegalKnowledgeResourcePaths {

    public static final String SOURCE_CATALOG = "catalog/legal_ai_knowledge_sources_2026.json";
    public static final String BRANCH_INFERENCE_CATALOG = "catalog/legal_ai_branch_inference_2026.json";
    public static final String INGESTION_POLICY_CATALOG = "catalog/legal_ai_ingestion_policy_2026.json";
    public static final String SURFACE_TEXT_CATALOG = "catalog/legal_ai_surface_text_2026.json";
    public static final String COMMENTARY_TEXT_CATALOG = "catalog/legal_ai_commentary_text_2026.json";
    public static final String CATALOG_MANIFEST = "catalog/legal_ai_catalog_manifest_2026.json";
    public static final String RITO_PACK = "ritos/rito_pack_2026.json";
    public static final String PRECEDENT_PACK = "jurisprudencia/seed_precedentes_2026.json";
    public static final String MATERIAL_PACK = "material/material_pack_2026.json";
    public static final String CONSTITUTION_PACK = "constituicao/constitution_pack_2026.json";
    public static final String SUMULA_PACK = "sumulas/sumula_pack_2026.json";
    public static final String DOCTRINE_PACK = "doutrina/doctrine_catalog_2026.json";
    public static final String SPECIAL_LAW_PACK = "leis-especiais/special_law_pack_2026.json";
    public static final String QUALIFIED_PRECEDENT_PACK = "precedentes/qualified_precedent_pack_2026.json";
    public static final String GOVERNANCE_ACT_PACK = "governanca/governance_act_pack_2026.json";

    private LegalKnowledgeResourcePaths() {
    }

    public static List<String> internalPackPaths() {
        return List.of(
                RITO_PACK,
                PRECEDENT_PACK,
                MATERIAL_PACK,
                CONSTITUTION_PACK,
                SUMULA_PACK,
                DOCTRINE_PACK,
                SPECIAL_LAW_PACK,
                QUALIFIED_PRECEDENT_PACK,
                GOVERNANCE_ACT_PACK
        );
    }
}
