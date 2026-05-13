package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

public final class InstitutionalHorizontalDataPlaneMessages {

    public static final String CNJ_SSO_REQUIRED = "sso_cnj_obrigatorio_para_usuarios_autenticados";
    public static final String PLATFORM_APPROVAL_CHAIN = "cadeia_confianca_pjb_diretor_geral_magistrado_referencial";
    public static final String DATA_PLANE_GRANULAR = "plano_dados_horizontal_por_uf_orgao_unidade_caixa";
    public static final String MONOLITH_READY_FOR_SPLIT = "modelo_monolitico_preparado_para_fatiamento_futuro_sem_quebrar_contrato";
    public static final String SHARED_ACCOUNTS_FORBIDDEN = "contas_compartilhadas_sao_bloqueadas";
    public static final String PERSONAL_IDENTITY_AND_FUNCTIONAL_CONTEXT = "identidade_pessoal_e_contexto_funcional_sao_distintos";

    private InstitutionalHorizontalDataPlaneMessages() {
    }

    public static String coverageMode(String mode) {
        return "coverage_mode=" + mode;
    }

    public static String profile(String profileKey) {
        return "profile_key=" + profileKey;
    }

    public static String replica(String replicaCode) {
        return replicaCode == null ? "replica_hint=PRIMARY_ONLY" : "replica_hint=" + replicaCode;
    }

    public static String writeBucket(int bucket, int bucketCount) {
        return "write_bucket=" + bucket + "/" + bucketCount;
    }

    public static String tribunal(String tribunalCode) {
        return tribunalCode == null ? "tribunal_hint=ORGAO_LOCAL" : "tribunal_hint=" + tribunalCode;
    }

    public static String routingKey(String key) {
        return "routing_key=" + key;
    }

    public static String primaryPartition(String key) {
        return "primary_partition=" + key;
    }

    public static String archivePartition(String key) {
        return "archive_partition=" + key;
    }

    public static String header(String name, String value) {
        return "header=" + name + ':' + value;
    }

    public static String municipalityFallback(String municipality, String responsibleUnitCode) {
        return "fallback_municipal=" + municipality + "->" + responsibleUnitCode;
    }

    public static String localCoverage(String municipality, String unitCode) {
        return "coverage_local=" + municipality + "->" + unitCode;
    }

    public static String panel(String panelCode) {
        return "panel=" + panelCode;
    }

    public static String landing(String path) {
        return "landing=" + path;
    }

    public static String approval(String approval) {
        return "required_approval=" + approval;
    }
}
