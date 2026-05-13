package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record PjbRitoContext(
        RitoProcessual canonicalRito,
        PjbDigitalCoreRitoKind ritoKind,
        String prazoPolicy,
        String movementPolicy,
        String labelPolicy,
        boolean simplifiedProcedure,
        boolean allowsEdital,
        boolean costsAtFirstDegree,
        boolean digitalHearingDefault,
        boolean humanReviewRequired
) {

    public static PjbRitoContext from(RitoProcessual rito, boolean humanReviewRequired) {
        RitoProcessual safeRito = rito == null ? RitoProcessual.COMUM_ORDINARIO : rito;
        PjbDigitalCoreRitoKind kind = PjbDigitalCoreRitoKind.fromRitoProcessual(safeRito);
        if (safeRito.isJuizado()) {
            String n = safeRito.name();
            if (n.contains("FAZENDA")) {
                return new PjbRitoContext(safeRito, kind, "PRAZO_JUIZADO_FAZENDA", "SUMARISSIMO_FAZENDA_PUBLICA", "PAINEL_JUIZADO_FAZENDA", true, false, false, true, humanReviewRequired);
            }
            if (n.contains("CRIMINAL")) {
                return new PjbRitoContext(safeRito, kind, "PRAZO_JECRIM", "JECRIM_COMPATIVEL", "PAINEL_JECRIM", true, false, false, true, humanReviewRequired);
            }
            return new PjbRitoContext(safeRito, kind, "PRAZO_JUIZADO_ESPECIAL", "SUMARISSIMO_COMPATIVEL", "PAINEL_JUIZADO_CIVEL", true, false, false, true, humanReviewRequired);
        }
        if (safeRito.isPenal()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_PENAL", "CRIMINAL_COMPLETO", "PAINEL_CRIMINAL", false, true, false, false, humanReviewRequired);
        }
        if (safeRito.isTrabalhista()) {
            boolean simplified = safeRito == RitoProcessual.TRABALHISTA_SUMARISSIMO || safeRito == RitoProcessual.TRABALHISTA_SUMARIO_ALCADA;
            return new PjbRitoContext(safeRito, kind, "PRAZO_TRABALHISTA", "TRABALHISTA_COMPATIVEL", "PAINEL_TRABALHISTA", simplified, false, false, true, humanReviewRequired);
        }
        if (safeRito.isEleitoral()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_ELEITORAL", "ELEITORAL_COMPLETO", "PAINEL_ELEITORAL", false, false, false, false, true);
        }
        if (safeRito.isMilitar()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_MILITAR", "MILITAR_COMPLETO", "PAINEL_MILITAR", false, false, false, false, true);
        }
        if (safeRito.isPrevidenciario()) {
            boolean jef = safeRito == RitoProcessual.PREVIDENCIARIO_JEF;
            return new PjbRitoContext(safeRito, kind, "PRAZO_PREVIDENCIARIO", "PREVIDENCIARIO_COMPATIVEL", "PAINEL_PREVIDENCIARIO", jef, false, !jef, jef, humanReviewRequired);
        }
        if (safeRito.isTribFazenda()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_FAZENDA", "ORDINARIO_FAZENDA", "PAINEL_FAZENDA", false, true, true, false, humanReviewRequired);
        }
        if (safeRito.isInfancia()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_INFANCIA", "PROTECAO_INTEGRAL", "PAINEL_INFANCIA", false, false, false, true, true);
        }
        if (safeRito.isAmbiental()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_AMBIENTAL", "AMBIENTAL_COMPATIVEL", "PAINEL_AMBIENTAL", false, true, true, false, humanReviewRequired);
        }
        if (safeRito.isAgrario()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_AGRARIO", "AGRARIO_COMPATIVEL", "PAINEL_AGRARIO", false, true, true, false, humanReviewRequired);
        }
        if (safeRito.isEmpresarial()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_FALENCIA", "RECUPERACAO_JUDICIAL_COMPLETO", "PAINEL_FALENCIA", false, true, true, false, humanReviewRequired);
        }
        if (safeRito.isAutocompositivo()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_CONSENSUAL", "AUTOCOMPOSICAO_VOLUNTARIA", "PAINEL_CONSENSUAL", true, false, false, true, false);
        }
        if (safeRito.isAdministrativo()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_ADMINISTRATIVO", "ORDINARIO_ADMINISTRATIVO", "PAINEL_ADMINISTRATIVO", false, true, true, false, humanReviewRequired);
        }
        if (safeRito.isEspecialConstitucional()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_CONSTITUCIONAL", "ESPECIAL_CONSTITUCIONAL", "PAINEL_CONSTITUCIONAL", false, false, false, false, true);
        }
        if (safeRito.isInternacional()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_INTERNACIONAL", "COOPERACAO_INTERNACIONAL", "PAINEL_INTERNACIONAL", false, false, false, false, true);
        }
        if (safeRito.isFamiliaSucessoes()) {
            return new PjbRitoContext(safeRito, kind, "PRAZO_FAMILIA", "FAMILIA_COMPATIVEL", "PAINEL_FAMILIA", false, false, false, false, humanReviewRequired);
        }
        return new PjbRitoContext(safeRito, kind, "PRAZO_CIVIL_ORDINARIO", "ORDINARIO_COMPLETO", "PAINEL_ORDINARIO", false, true, true, false, humanReviewRequired);
    }

    public static PjbRitoContext from(PjbDigitalCoreRitoKind kind, boolean humanReviewRequired) {
        PjbDigitalCoreRitoKind safeKind = kind == null ? PjbDigitalCoreRitoKind.DESCONHECIDO : kind;
        if (safeKind == PjbDigitalCoreRitoKind.DESCONHECIDO) {
            return new PjbRitoContext(RitoProcessual.COMUM_ORDINARIO, PjbDigitalCoreRitoKind.DESCONHECIDO, "REVISAO_HUMANA", "BLOQUEADO_ATE_CLASSIFICACAO", "PAINEL_TRIAGEM", false, false, false, false, true);
        }
        return from(safeKind.toCanonicalRito(), humanReviewRequired);
    }
}
