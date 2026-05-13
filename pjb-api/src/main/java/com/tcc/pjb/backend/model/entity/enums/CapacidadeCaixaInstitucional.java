package com.tcc.pjb.backend.model.entity.enums;

public enum CapacidadeCaixaInstitucional {
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
    REGISTRAR_SUBSTITUICAO,
    ORGANIZAR_DOCUMENTOS,
    SOLICITAR_AUDIENCIA,
    AGENDAR_AUDIENCIA,
    REMARCAR_AUDIENCIA,
    CANCELAR_AUDIENCIA,
    RESERVAR_SALA_AUDIENCIA,
    REGISTRAR_TERMO_AUDIENCIA,
    EMITIR_PARECER,
    ACESSAR_CALCULADORA_JUDICIAL;

    public boolean isMutacaoFila() {
        return switch (this) {
            case REDISTRIBUIR_INTERNAMENTE,
                    ATRIBUIR_MEMBRO,
                    DEVOLVER_PARA_FILA,
                    ESCALAR_AO_TITULAR,
                    ESCALONA_COORDENADORIA,
                    PEDIR_RETORNO_SECRETARIA,
                    PEDIR_REDISTRIBUICAO_INSTITUCIONAL,
                    REGISTRAR_SUBSTITUICAO -> true;
            default -> false;
        };
    }

    public boolean isAtoDeAssinaturaOuManifestacao() {
        return this == ASSINAR_MANIFESTACAO || this == PETICIONAR_EM_NOME_DO_ORGAO;
    }

    public boolean isAtoDeCiencia() {
        return this == RECEBER_COMUNICACAO || this == DAR_CIENCIA || this == GERAR_CERTIDAO_CIENCIA;
    }

    public boolean isGestaoAudiencia() {
        return switch (this) {
            case SOLICITAR_AUDIENCIA,
                    AGENDAR_AUDIENCIA,
                    REMARCAR_AUDIENCIA,
                    CANCELAR_AUDIENCIA,
                    RESERVAR_SALA_AUDIENCIA,
                    REGISTRAR_TERMO_AUDIENCIA -> true;
            default -> false;
        };
    }

    public boolean isGestaoDocumental() {
        return this == ORGANIZAR_DOCUMENTOS;
    }

    public boolean isParecerOuOpiniao() {
        return this == EMITIR_PARECER || this == PREPARAR_MINUTA || this == ASSINAR_MANIFESTACAO;
    }

    public boolean isFerramentaDeCalculo() {
        return this == ACESSAR_CALCULADORA_JUDICIAL;
    }
}
