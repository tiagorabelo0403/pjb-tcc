package com.tcc.pjb.backend.core.comunicacao.institucional.closure.application;

final class InstitutionalOperatingModelMessages {

    static final String PERSONAL_ROOT_IDENTITY = "identidade_pessoal_raiz_com_contexto_institucional_delegado";
    static final String INSTITUTION_OWNS_SEATS = "instituicao_aderente_define_lotacoes_cargos_e_vigencias_no_pjb";
    static final String PJB_HOMOLOGATES = "pjb_homologa_afiliacao_nomeacoes_capacidades_e_trilhas";
    static final String SHARED_ACCOUNT_FORBIDDEN = "conta_compartilhada_proibida";
    static final String JUDGE_PERSONAL_ENTRY = "magistratura_usa_identidade_pessoal_com_contexto_de_forum_gabinete_secretaria";
    static final String MUNICIPAL_FALLBACK = "municipio_sem_unidade_propria_resolvido_para_sede_competente";
    static final String LOCAL_COVERAGE = "municipio_coberto_por_unidade_local_compatível";
    static final String AFFILIATION_INACTIVE = "afiliacao_institucional_inativa_ou_nao_homologada";
    static final String MASTER_ADMIN_MISSING = "gestao_mestra_institucional_sem_nomeacao_ativa";
    static final String TITULAR_MISSING = "atuacao_institucional_sem_titular_ativo";
    static final String MUNICIPALITY_FALLBACK_FINDING = "municipio_sem_unidade_propria_redirecionado_para_sede_competente";
    static final String NATIONAL_FALLBACK_FINDING = "catalogo_sem_unidade_local_ou_estadual_especifica";

    private InstitutionalOperatingModelMessages() {
    }

    static String governanceAnchor(String scope) {
        return "governanca_scope=" + safe(scope);
    }

    static String entryMode(String entryMode) {
        return "entry_mode=" + safe(entryMode);
    }

    static String blueprint(String code) {
        return "blueprint=" + safe(code);
    }

    static String responsibleUnit(String code) {
        return "unidade_responsavel=" + safe(code);
    }

    static String requestedTerritory(String municipio, String uf) {
        return "territorio_requisitado=" + safe(municipio) + '@' + safe(uf);
    }

    static String seat(String code) {
        return "seat=" + safe(code);
    }

    static String roleBand(String key) {
        return "role_band=" + safe(key);
    }

    static String nominationCount(long count) {
        return "nomeacoes_ativas=" + count;
    }

    static String coverageMode(String mode) {
        return "coverage_mode=" + safe(mode);
    }

    static String safe(String value) {
        return value == null || value.isBlank() ? "NAO_INFORMADO" : value.trim();
    }
}
