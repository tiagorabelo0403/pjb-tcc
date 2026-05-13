package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public final class ComunicacaoJudicialTribunalSuperiorResolver {

    private ComunicacaoJudicialTribunalSuperiorResolver() {
    }

    public static ComunicacaoJudicialTribunalSuperior resolver(ProceduralCommunicationContext context) {
        if (context == null) {
            return ComunicacaoJudicialTribunalSuperior.NENHUM;
        }
        String tribunal = context.tribunalCodigo();
        if (contains(tribunal, "STF", "SUPREMO")) {
            return ComunicacaoJudicialTribunalSuperior.STF;
        }
        if (contains(tribunal, "STJ", "SUPERIOR TRIBUNAL DE JUSTICA")) {
            return ComunicacaoJudicialTribunalSuperior.STJ;
        }
        if (contains(tribunal, "TST", "TRIBUNAL SUPERIOR DO TRABALHO")) {
            return ComunicacaoJudicialTribunalSuperior.TST;
        }
        if (contains(tribunal, "TSE", "TRIBUNAL SUPERIOR ELEITORAL")) {
            return ComunicacaoJudicialTribunalSuperior.TSE;
        }
        if (contains(tribunal, "STM", "SUPERIOR TRIBUNAL MILITAR")) {
            return ComunicacaoJudicialTribunalSuperior.STM;
        }
        GrauJurisdicao grau = context.grau();
        if (grau == GrauJurisdicao.CONSTITUCIONAL) {
            return context.isConstitucionalOriginario() || context.isRecursoExtraordinarioEstrito()
                    ? ComunicacaoJudicialTribunalSuperior.STF
                    : ComunicacaoJudicialTribunalSuperior.CONSTITUCIONAL_GENERICO;
        }
        if (grau != GrauJurisdicao.SUPERIOR) {
            return ComunicacaoJudicialTribunalSuperior.NENHUM;
        }
        RamoDireito ramo = context.ramo();
        if (ramo == RamoDireito.TRABALHISTA || context.rito() != null && context.rito().isTrabalhista()) {
            return ComunicacaoJudicialTribunalSuperior.TST;
        }
        if (ramo == RamoDireito.ELEITORAL || context.rito() != null && context.rito().isEleitoral()) {
            return ComunicacaoJudicialTribunalSuperior.TSE;
        }
        if (ramo == RamoDireito.MILITAR || context.rito() != null && context.rito().isMilitar()) {
            return ComunicacaoJudicialTribunalSuperior.STM;
        }
        if (context.isConstitucionalOriginario() || context.isRecursoExtraordinarioEstrito()) {
            return ComunicacaoJudicialTribunalSuperior.STF;
        }
        return ComunicacaoJudicialTribunalSuperior.STJ;
    }

    private static boolean contains(String source, String... tokens) {
        if (source == null || source.isBlank() || tokens == null) {
            return false;
        }
        String normalized = source.toUpperCase();
        for (String token : tokens) {
            if (token != null && normalized.contains(token.toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}
