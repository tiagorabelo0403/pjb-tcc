package com.tcc.pjb.backend.controller.processual.substituicao.routes;

public final class PjbSubstituicaoNacionalRoutes {

    public static final String CANONICAL_BASE = "/api/v1/processual/plataforma";
    public static final String PATH_ARQUITETURA = "/substituicao-nacional/arquitetura";
    public static final String PATH_PROGRAMA = "/substituicao-nacional/programa";
    public static final String PATH_CENTRO_COMANDO = "/substituicao-nacional/centro-comando";
    public static final String PATH_CENTRO_COMANDO_TRIBUNAL = PATH_CENTRO_COMANDO + "/tribunal/{tribunalCodigo}";
    public static final String PATH_WAR_ROOM = "/substituicao-nacional/war-room";
    public static final String PATH_WAR_ROOM_TRIBUNAL = PATH_WAR_ROOM + "/tribunal/{tribunalCodigo}";
    public static final String PATH_CUTOVER_MATRIX = "/substituicao-nacional/cutover-matrix";
    public static final String PATH_CUTOVER_MATRIX_TRIBUNAL = PATH_CUTOVER_MATRIX + "/tribunal/{tribunalCodigo}";
    public static final String PATH_NUCLEO_DURO = "/substituicao-nacional/nucleo-duro";
    public static final String PATH_NUCLEO_DURO_TRIBUNAL = PATH_NUCLEO_DURO + "/tribunal/{tribunalCodigo}";
    public static final String PATH_MALHA_JULGADORA = "/substituicao-nacional/malha-julgadora";
    public static final String PATH_MALHA_JULGADORA_TRIBUNAL = PATH_MALHA_JULGADORA + "/tribunal/{tribunalCodigo}";
    public static final String PATH_PRECEDENTES_QUALIFICADOS = "/substituicao-nacional/precedentes-qualificados";
    public static final String PATH_PRECEDENTES_QUALIFICADOS_TRIBUNAL = PATH_PRECEDENTES_QUALIFICADOS + "/tribunal/{tribunalCodigo}";
    public static final String PATH_TUTELA_COLETIVA = "/substituicao-nacional/tutela-coletiva";
    public static final String PATH_TUTELA_COLETIVA_TRIBUNAL = PATH_TUTELA_COLETIVA + "/tribunal/{tribunalCodigo}";
    public static final String PATH_POS_COLETIVA = "/substituicao-nacional/pos-coletiva";
    public static final String PATH_POS_COLETIVA_TRIBUNAL = PATH_POS_COLETIVA + "/tribunal/{tribunalCodigo}";
    public static final String PATH_EXECUCOES = "/substituicao-nacional/execucoes";
    public static final String PATH_EXECUCAO_ID = PATH_EXECUCOES + "/{execucaoId}";
    public static final String PATH_EXECUCAO_CONTROLE = PATH_EXECUCAO_ID + "/controle";
    public static final String PATH_EXECUCAO_OPERACIONAL = PATH_EXECUCAO_ID + "/operacional";
    public static final String PATH_COCKPIT = "/substituicao-nacional/cockpit";
    public static final String PATH_RECONCILIACAO_TRIBUNAL = "/substituicao-nacional/reconciliacao/tribunal/{tribunalCodigo}";
    public static final String PATH_EVIDENCIA_EXPORTAVEL_TRIBUNAL = PATH_RECONCILIACAO_TRIBUNAL + "/evidencia-exportavel";
    public static final String PATH_SUSTENTACAO = "/sustentacao";
    public static final String PATH_LEGADOS_PROCESSO = "/{processoId}/substituicao-legados";

    private PjbSubstituicaoNacionalRoutes() {
    }
}
