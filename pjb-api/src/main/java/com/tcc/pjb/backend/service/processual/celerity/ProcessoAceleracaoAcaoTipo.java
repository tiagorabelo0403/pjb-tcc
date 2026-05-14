package com.tcc.pjb.backend.service.processual.celerity;

public enum ProcessoAceleracaoAcaoTipo {

    CERTIFICAR_DECURSO_PRAZO("Certificar decurso de prazo", true),
    EXPEDIR_MANDADO("Expedir mandado de cumprimento", true),
    EXPEDIR_OFICIO("Expedir ofício a órgão público", true),
    JUNTAR_DOCUMENTO("Juntar documento aos autos", true),
    INTIMAR_PARTE("Intimar parte sobre ato processual", true),
    DESIGNAR_AUDIENCIA("Designar data para audiência", true),
    EMITIR_CERTIDAO("Emitir certidão de inteiro teor ou negativa", true),
    VERIFICAR_AR("Verificar aviso de recebimento de intimação", true),
    REDISTRIBUIR_PROCESSO("Redistribuir processo por incompetência ou prevenção", true),
    LOTE_DESPACHOS("Preparar lote de despachos idênticos", true),
    LOTE_SENTENCAS("Preparar lote de minutas de sentença similares", false),
    MINUTAR_DECISAO("Preparar minuta de decisão interlocutória", false),
    MINUTAR_SENTENCA("Preparar minuta de sentença", false),
    MINUTAR_ACORDO("Preparar minuta de acordo para homologação", false),
    AVALIAR_ACORDO("Avaliar proposta de acordo das partes", false);

    private final String descricao;
    private final boolean automatizavel;

    ProcessoAceleracaoAcaoTipo(String descricao, boolean automatizavel) {
        this.descricao = descricao;
        this.automatizavel = automatizavel;
    }

    public String getDescricao() { return descricao; }

    public boolean isAutomatizavel() { return automatizavel; }

    public boolean isAtoJurisdicional() {
        return switch (this) {
            case MINUTAR_DECISAO, MINUTAR_SENTENCA,
                 MINUTAR_ACORDO, AVALIAR_ACORDO, LOTE_SENTENCAS -> true;
            default -> false;
        };
    }
}
