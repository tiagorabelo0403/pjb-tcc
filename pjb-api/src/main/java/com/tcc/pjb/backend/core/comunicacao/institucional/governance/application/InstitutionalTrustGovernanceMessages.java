package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

public final class InstitutionalTrustGovernanceMessages {

    public static final String BASELINE_CNJ = "baseline_cnj_2025_magistrados_18748_servidores_278826";
    public static final String TRI_KEY_GOVERNANCE = "perfil_institucional_sensivel_depende_de_governanca_trina";
    public static final String INSTITUTION_CREATES_PROFILE_PJB_HARDENS = "instituicao_cria_perfil_pjb_homologa_e_endurece";
    public static final String ENTRY_ROUTED_BY_CONTEXT = "entrada_orientada_por_contexto_funcional_e_painel";
    public static final String PARTITION_AXES = "particionamento_horizontal_por_uf_orgao_unidade_caixa";
    public static final String SHARED_ACCOUNT_FORBIDDEN = "conta_compartilhada_proibida";

    private InstitutionalTrustGovernanceMessages() {
    }

    public static String profileKey(String profileKey) {
        return "profile_key=" + profileKey;
    }

    public static String approvalRequired(String code) {
        return "approval_required=" + code;
    }

    public static String approvalSatisfied(String code) {
        return "approval_satisfied=" + code;
    }

    public static String approvalPending(String code) {
        return "approval_pending=" + code;
    }

    public static String approvalRejected(String code) {
        return "approval_rejected=" + code;
    }

    public static String landing(String path) {
        return "landing_path=" + path;
    }

    public static String panel(String panelCode) {
        return "panel_code=" + panelCode;
    }

    public static String dataPlane(String key) {
        return "horizontal_data_plane=" + key;
    }

    public static String audience(String audience) {
        return "audience=" + audience;
    }

    public static String partitionBuckets(int buckets) {
        return "write_partition_buckets_min=" + buckets;
    }

    public static String replicaFloor(int replicas) {
        return "read_replica_floor_min=" + replicas;
    }

    public static String modeledContexts(int count) {
        return "contextos_institucionais_modelados=" + count;
    }

    public static String authority(String authority) {
        return "authority=" + authority;
    }
}
