package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

/**
 * Nível de consultoria da minuta assistida pelo Laiane, derivado do próprio sinal de confiança
 * que {@code LaianeJudicialDecisionAdvisoryService} já calcula por caso — nunca uma escolha do
 * usuário nem uma configuração externa. Em nenhum nível a Laiane decide ou publica:
 * {@code reviewRequired}/{@code publicationLocked} permanecem sempre {@code true} nos três.
 */
public enum LaianeAdvisoryMode {

    /** Padrão de caso reconhecido, sem pendência de fato identificada — minuta de dispositivo completa. */
    SUGESTIVO,

    /** Padrão de caso reconhecido, mas com pendência de fato não resolvida — sem minuta de dispositivo, só checklist e fundamentos. */
    RESTRITIVO,

    /** Nenhum padrão de caso reconhecido — sem minuta de dispositivo, assistência limitada ao checklist estruturante. */
    BLOQUEADOR
}
