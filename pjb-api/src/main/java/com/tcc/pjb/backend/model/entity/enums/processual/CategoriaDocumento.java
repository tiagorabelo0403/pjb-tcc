package com.tcc.pjb.backend.model.entity.enums.processual;

/**
 * Categoria funcional de um documento processual submetido por parte ao processo.
 *
 * <p>Este enum classifica o DOCUMENTO que a parte submete, não o ato processual em si.
 * A relação com {@link com.tcc.pjb.backend.core.processual.ato.AtoProcessualCategoria}
 * é de correspondência documentada, não de identidade: são domínios distintos.
 *
 * <p>Correspondências indicativas (não exaustivas):
 * <ul>
 *   <li>{@code PECA_INAUGURAL} — correlaciona com {@code AtoProcessualCategoria.DISTRIBUICAO}</li>
 *   <li>{@code PECA_RECURSAL} — correlaciona com {@code AtoProcessualCategoria.RECURSAL}</li>
 *   <li>{@code DOC_INSTRUCAO} — correlaciona com {@code AtoProcessualCategoria.INSTRUTORIA}</li>
 *   <li>{@code DOC_QUALIFICACAO} — sem ato correspondente direto: qualifica quem age, não um ato</li>
 * </ul>
 *
 * <p>{@code AtoProcessualCategoria} possui valores (DECISORIO, CERTIFICACAO, ADMINISTRATIVO,
 * COMUNICACAO, EXECUTIVO) sem equivalente aqui, pois correspondem a atos do juízo, não a
 * documentos da parte.
 */
public enum CategoriaDocumento {

    /** Peça processual que inaugura o processo (art. 319 CPC). Ex: petição inicial. */
    PECA_INAUGURAL,

    /** Peça processual recursal (arts. 994–1044 CPC). Ex: razões recursais, contrarrazões. */
    PECA_RECURSAL,

    /** Documento de instrução, prova ou suporte à causa. Ex: laudo, ata, planilha. */
    DOC_INSTRUCAO,

    /** Documento de qualificação de parte ou representação. Ex: identidade, procuração, CTPS. */
    DOC_QUALIFICACAO
}
