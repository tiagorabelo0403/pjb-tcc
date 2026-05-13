package com.tcc.pjb.backend.controller.admin;

public final class NationalObservabilityRoutes {

    public static final String CANONICAL_BASE = "/api/v1/admin/observability-national";
    public static final String PATH_DASHBOARD = "/dashboard";
    public static final String PATH_SLA_REPORT = "/sla-report";
    public static final String PATH_RUNBOOK_STATUS = "/runbook-status";
    public static final String PATH_SUBSTITUICAO_READINESS = "/substituicao-readiness";
    public static final String PATH_SUBSTITUICAO_CENTRO_COMANDO = "/substituicao-centro-comando";
    public static final String PATH_SUBSTITUICAO_WAR_ROOM = "/substituicao-war-room";
    public static final String PATH_SUBSTITUICAO_WAR_ROOM_TRIBUNAL = PATH_SUBSTITUICAO_WAR_ROOM + "/tribunal/{tribunalCodigo}";
    public static final String PATH_SUBSTITUICAO_CUTOVER_MATRIX = "/substituicao-cutover-matrix";
    public static final String PATH_SUBSTITUICAO_CUTOVER_MATRIX_TRIBUNAL = PATH_SUBSTITUICAO_CUTOVER_MATRIX + "/tribunal/{tribunalCodigo}";
    public static final String PATH_SUBSTITUICAO_NUCLEO_DURO = "/substituicao-nucleo-duro";
    public static final String PATH_SUBSTITUICAO_NUCLEO_DURO_TRIBUNAL = PATH_SUBSTITUICAO_NUCLEO_DURO + "/tribunal/{tribunalCodigo}";
    public static final String PATH_SUBSTITUICAO_MALHA_JULGADORA = "/substituicao-malha-julgadora";
    public static final String PATH_SUBSTITUICAO_MALHA_JULGADORA_TRIBUNAL = PATH_SUBSTITUICAO_MALHA_JULGADORA + "/tribunal/{tribunalCodigo}";
    public static final String PATH_SUBSTITUICAO_PRECEDENTES_QUALIFICADOS = "/substituicao-precedentes-qualificados";
    public static final String PATH_SUBSTITUICAO_PRECEDENTES_QUALIFICADOS_TRIBUNAL = PATH_SUBSTITUICAO_PRECEDENTES_QUALIFICADOS + "/tribunal/{tribunalCodigo}";
    public static final String PATH_SUBSTITUICAO_TUTELA_COLETIVA = "/substituicao-tutela-coletiva";
    public static final String PATH_SUBSTITUICAO_TUTELA_COLETIVA_TRIBUNAL = PATH_SUBSTITUICAO_TUTELA_COLETIVA + "/tribunal/{tribunalCodigo}";
    public static final String PATH_SUBSTITUICAO_POS_COLETIVA = "/substituicao-pos-coletiva";
    public static final String PATH_SUBSTITUICAO_POS_COLETIVA_TRIBUNAL = PATH_SUBSTITUICAO_POS_COLETIVA + "/tribunal/{tribunalCodigo}";
    public static final String PATH_PLATAFORMA_SUSTENTACAO = "/plataforma-sustentacao";

    private NationalObservabilityRoutes() {
    }
}
