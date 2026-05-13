package com.tcc.pjb.backend.model.dto.processual.recursal.foundation;

public record RecursalPrazoRuleView(
        String recurso,
        int diasUteis,
        boolean contaDaPostagemViaCorreio,
        boolean exigeComprovacaoFeriadoLocal,
        boolean suspendeNoRecessoForense,
        boolean admitePrazoEmDobroFazendaPublica,
        boolean admitePrazoEmDobroLitisconsortes) {
}
