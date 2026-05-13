package com.tcc.pjb.backend.core.distribuicao;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class DistribuicaoProcessualProcessoSupport {

    DistribuicaoProcessualNacionalEngine.DistribuicaoRequest buildFromProcesso(Processo processo) {
        String preventionReference = extractReference(processo.getPreventionMode());
        String relationReference = extractReference(processo.getLinkageMode());
        boolean dependencia = containsAny(processo.getLinkageMode(), "DEPEND", "DEPENDENCIA");
        boolean conexao = containsAny(processo.getLinkageMode(), "CONEXAO");
        boolean continencia = containsAny(processo.getLinkageMode(), "CONTINENCIA");
        boolean sigiloReforcado = processo.getNivelSigilo() != null && processo.getNivelSigilo().nivel() > 0;
        return new DistribuicaoProcessualNacionalEngine.DistribuicaoRequest(
                firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero()),
                normalizeToken(firstNonBlank(processo.getUf(), "BR")),
                firstNonBlank(processo.getComarca(), processo.getVara(), "COMARCA_NAO_INFORMADA"),
                processo.getRito() == null ? RitoProcessual.COMUM_ORDINARIO : processo.getRito(),
                processo.getValorCausa() == null ? 0d : processo.getValorCausa().doubleValue(),
                processo.getParteAutoraNome(),
                processo.getParteReuNome(),
                inferGrau(processo),
                firstNonBlank(processo.getComarca(), processo.getVara()),
                firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal(), processo.getComarca()),
                processo.getUnidadeJudiciariaCodigo(),
                firstNonBlank(processo.getUnidadeJudiciariaCodigo(), processo.getVara()),
                processo.getAssunto(),
                processo.getClasseProcessual(),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                inferAreaEspecializada(processo),
                preventionReference,
                relationReference,
                processo.getClasseProcessual(),
                firstNonBlank(processo.getAssunto(), processo.getObjetoProcessual()),
                dependencia,
                conexao,
                continencia,
                isUrgentProcess(processo),
                false,
                sigiloReforcado,
                requiresGovernanceEscalation(processo),
                containsAny(processo.getLinkageMode(), "IMPEDIMENTO", "SUSPEICAO"),
                containsAny(processo.getPreProtocoloStatus(), "PLANTAO"),
                containsAny(processo.getPreProtocoloStatus(), "LIMINAR", "TUTELA", "URGENTE"),
                sigiloReforcado
        );
    }

    private GrauJurisdicao inferGrau(Processo processo) {
        if (processo == null) {
            return GrauJurisdicao.PRIMEIRO_GRAU;
        }
        if (containsAny(processo.getUnidadeJudiciariaCodigo(), "CAMARA", "TURMA", "SECAO", "PLENARIO", "SEGUNDO_GRAU", "TRF", "TJ")) {
            return GrauJurisdicao.SEGUNDO_GRAU;
        }
        return GrauJurisdicao.PRIMEIRO_GRAU;
    }

    private String inferAreaEspecializada(Processo processo) {
        if (processo == null) {
            return null;
        }
        if (processo.getMateria() != null) {
            return processo.getMateria().name();
        }
        if (processo.getRito() != null) {
            if (processo.getRito() == RitoProcessual.CIVIL_TUTELA_URGENTE || processo.getRito() == RitoProcessual.CIVIL_TUTELA_CAUTELAR_ANTECEDENTE || processo.getRito() == RitoProcessual.CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE) return "CIVEL_URGENCIA";
            if (processo.getRito().isPenal()) return "CRIMINAL";
            if (processo.getRito().isTrabalhista()) return "TRABALHISTA";
            if (processo.getRito().isEleitoral()) return "ELEITORAL";
            if (processo.getRito().isMilitar()) return "MILITAR";
            if (processo.getRito().isTribFazenda()) return "FAZENDA_PUBLICA";
            if (processo.getRito().isPrevidenciario()) return "PREVIDENCIARIO";
            if (processo.getRito().isInfancia()) return "INFANCIA_JUVENTUDE";
            if (processo.getRito().isAmbiental()) return "AMBIENTAL";
            if (processo.getRito().isAgrario()) return "AGRARIO";
            if (processo.getRito().isEmpresarial()) return "EMPRESARIAL";
            if (processo.getRito().isFamiliaSucessoes()) return "FAMILIA_SUCESSOES";
            if (processo.getRito().isAdministrativo()) return "ADMINISTRATIVO";
            if (processo.getRito().isInternacional()) return "INTERNACIONAL";
            if (processo.getRito().isAutocompositivo()) return "AUTOCOMPOSICAO";
            if (DistribuicaoProcessualTrackSupport.isConstitutionalTrack(processo.getRito(), inferGrau(processo), null)) return "CONSTITUCIONAL";
        }
        String unidade = normalizeToken(firstNonBlank(processo.getUnidadeJudiciariaCodigo(), processo.getVara()));
        if (containsAny(unidade, "JUIZADO")) return "JUIZADO";
        if (containsAny(unidade, "CRIM", "JURI", "CUSTODIA", "EXEC_PENAL")) return "CRIMINAL";
        if (containsAny(unidade, "FAZENDA")) return "FAZENDA_PUBLICA";
        if (containsAny(unidade, "FAMILIA", "SUCESS")) return "FAMILIA_SUCESSOES";
        if (containsAny(unidade, "AMBIENTAL")) return "AMBIENTAL";
        if (containsAny(unidade, "AGRAR")) return "AGRARIO";
        if (containsAny(unidade, "EMPRESARIAL", "FALENCIA", "RECUPERACAO")) return "EMPRESARIAL";
        return null;
    }

    private boolean isUrgentProcess(Processo processo) {
        if (processo == null) {
            return false;
        }
        if (containsAny(processo.getRoutingRiskLevel(), "CRITICO")) {
            return true;
        }
        if (containsAny(processo.getPreProtocoloStatus(), "PLANTAO", "LIMINAR", "TUTELA", "URGENTE")) {
            return true;
        }
        RitoProcessual rito = processo.getRito();
        return rito == RitoProcessual.ESPECIAL_HABEAS_CORPUS
                || rito == RitoProcessual.PENAL_HABEAS_CORPUS_PREVENTIVO
                || rito == RitoProcessual.CIVIL_TUTELA_URGENTE
                || rito == RitoProcessual.CIVIL_TUTELA_CAUTELAR_ANTECEDENTE
                || rito == RitoProcessual.CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE
                || rito == RitoProcessual.ESPECIAL_MANDADO_SEGURANCA
                || rito == RitoProcessual.ESPECIAL_MANDADO_SEGURANCA_COLETIVO;
    }

    private boolean requiresGovernanceEscalation(Processo processo) {
        if (processo == null) {
            return false;
        }
        return containsAny(processo.getRoutingRiskLevel(), "CRITICO", "ALTO")
                || containsAny(processo.getPreProtocoloStatus(), "REVISAO", "BLOQUEADO", "INCOMPATIVEL")
                || containsAny(processo.getLinkageMode(), "DEPEND", "CONEXAO", "CONTINENCIA", "IMPEDIMENTO");
    }

    private String extractReference(String mode) {
        String normalized = normalizeText(mode);
        if (normalized == null) {
            return null;
        }
        int colon = normalized.indexOf(':');
        if (colon >= 0 && colon + 1 < normalized.length()) {
            return normalizeText(normalized.substring(colon + 1));
        }
        return containsAny(normalized, "PREVENCAO", "DEPENDENCIA", "CONEXAO", "CONTINENCIA") ? normalized : null;
    }

    private static boolean containsAny(String value, String... needles) {
        String normalized = normalizeToken(value);
        if (normalized == null || needles == null) {
            return false;
        }
        for (String needle : needles) {
            String token = normalizeToken(needle);
            if (token != null && normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? null : normalized;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
