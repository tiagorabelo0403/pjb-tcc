package com.tcc.pjb.backend.model.entity.enums.processual;

/**
 * Nível de urgência que o próprio rito já carrega por definição legal, independente do
 * conteúdo do processo. Usado para priorizar o painel do magistrado sem depender de
 * classificação automática de texto — só do rito, que é dado estruturado e confiável.
 */
public enum NivelUrgenciaProcessual {
    MAXIMA,
    ALTA,
    PADRAO
}
