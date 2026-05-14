package com.tcc.pjb.backend.service.processual.celerity;

public final class JudicialIndependenceSafeAccelerationPolicy {

    public enum BloqueioTecnicoRazao {
        SIGILO_INCOMPATIVEL,
        ASSINATURA_INVALIDA,
        PROTOCOLO_INVALIDO,
        ACESSO_NAO_AUTORIZADO,
        HASH_DOCUMENTO_INVALIDO,
        CERTIFICADO_DIGITAL_EXPIRADO
    }

    public enum ModoAceleracao {
        SUGESTAO,
        AUTOMATIZAR
    }

    private JudicialIndependenceSafeAccelerationPolicy() {}

    public static boolean canBlockJudicialAction() {
        return false;
    }

    public static boolean canBlockTechnicalOperation(BloqueioTecnicoRazao razao) {
        return razao != null;
    }

    public static ModoAceleracao modoParaAcao(ProcessoAceleracaoAcaoTipo acao) {
        return acao.isAtoJurisdicional() ? ModoAceleracao.SUGESTAO : ModoAceleracao.AUTOMATIZAR;
    }

    public static String mensagemAceleracaoSegura(ProcessoAceleracaoAcaoTipo acao, int quantidade) {
        return switch (acao) {
            case CERTIFICAR_DECURSO_PRAZO ->
                quantidade + " processo(s) apto(s) para certificação de decurso de prazo.";
            case LOTE_SENTENCAS ->
                "Há " + quantidade + " processo(s) com minuta similar disponível. "
                + "Exige revisão e assinatura do magistrado.";
            case MINUTAR_DECISAO, MINUTAR_SENTENCA ->
                "Processo apto para julgamento antecipado. "
                + "Sugestão de minuta disponível para análise do magistrado.";
            case DESIGNAR_AUDIENCIA ->
                quantidade + " processo(s) com audiência sem designação de data.";
            case VERIFICAR_AR ->
                quantidade + " expediente(s) sem confirmação de ciência — verificar AR.";
            default ->
                quantidade + " processo(s) com ação sugerida: " + acao.getDescricao();
        };
    }

    public static String mensagemNaoBloqueio() {
        return "O sistema nunca bloqueia ato jurisdicional. "
             + "Art. 5º, LXXVIII CF/88: razoável duração do processo.";
    }
}
