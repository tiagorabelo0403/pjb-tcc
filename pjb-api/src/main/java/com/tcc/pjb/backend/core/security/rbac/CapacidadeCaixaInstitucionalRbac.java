package com.tcc.pjb.backend.core.security.rbac;

public enum CapacidadeCaixaInstitucionalRbac {
    VISUALIZAR,
    APENAS_VISUALIZAR,
    RECEBER_COMUNICACAO,
    DAR_CIENCIA,
    REDISTRIBUIR_INTERNAMENTE,
    ATRIBUIR_MEMBRO,
    PREPARAR_MINUTA,
    ASSINAR_MANIFESTACAO,
    PETICIONAR_EM_NOME_DO_ORGAO,
    GERAR_CERTIDAO_CIENCIA,
    DEVOLVER_PARA_FILA,
    ESCALAR_AO_TITULAR,
    ESCALONA_COORDENADORIA,
    PEDIR_RETORNO_SECRETARIA,
    PEDIR_REDISTRIBUICAO_INSTITUCIONAL,
    REGISTRAR_IMPEDIMENTO,
    REGISTRAR_SUBSTITUICAO;

    public com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional toModel() {
        return switch (this) {
            case APENAS_VISUALIZAR -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.APENAS_VISUALIZAR;
            case ESCALONA_COORDENADORIA -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.ESCALONA_COORDENADORIA;
            case PEDIR_REDISTRIBUICAO_INSTITUCIONAL -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.PEDIR_REDISTRIBUICAO_INSTITUCIONAL;
            case VISUALIZAR -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.VISUALIZAR;
            case RECEBER_COMUNICACAO -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO;
            case DAR_CIENCIA -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.DAR_CIENCIA;
            case REDISTRIBUIR_INTERNAMENTE -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE;
            case ATRIBUIR_MEMBRO -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO;
            case PREPARAR_MINUTA -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.PREPARAR_MINUTA;
            case ASSINAR_MANIFESTACAO -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO;
            case PETICIONAR_EM_NOME_DO_ORGAO -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO;
            case GERAR_CERTIDAO_CIENCIA -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA;
            case DEVOLVER_PARA_FILA -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.DEVOLVER_PARA_FILA;
            case ESCALAR_AO_TITULAR -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR;
            case PEDIR_RETORNO_SECRETARIA -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.PEDIR_RETORNO_SECRETARIA;
            case REGISTRAR_IMPEDIMENTO -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.REGISTRAR_IMPEDIMENTO;
            case REGISTRAR_SUBSTITUICAO -> com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional.REGISTRAR_SUBSTITUICAO;
        };
    }
}
