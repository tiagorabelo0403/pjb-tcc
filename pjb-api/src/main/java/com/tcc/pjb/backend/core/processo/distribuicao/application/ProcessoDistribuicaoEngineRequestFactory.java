package com.tcc.pjb.backend.core.processo.distribuicao.application;

import com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualNacionalEngine;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaMalhaAggregate;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Locale;

final class ProcessoDistribuicaoEngineRequestFactory {

    private ProcessoDistribuicaoEngineRequestFactory() {
    }

    static DistribuicaoProcessualNacionalEngine.DistribuicaoRequest fromProcesso(Processo processo,
                                                                                 ProcessoDistribuicaoMalhaAggregate distribuicaoMalha,
                                                                                 ProcessoAnomaliaMalhaAggregate anomaliaMalha) {
        RitoProcessual rito = processo.getRito() == null ? RitoProcessual.COMUM_ORDINARIO : processo.getRito();
        boolean sigiloReforcado = distribuicaoMalha.exigeSigiloReforcado()
                || processo.getNivelSigilo() != null && processo.getNivelSigilo().nivel() > 0;
        boolean dependencia = distribuicaoMalha.motivos().stream().anyMatch(item -> containsAny(item.dominio(), "DEPENDENCIA"));
        boolean conexao = distribuicaoMalha.motivos().stream().anyMatch(item -> containsAny(item.dominio(), "CONEXAO"));
        boolean continencia = distribuicaoMalha.motivos().stream().anyMatch(item -> containsAny(item.dominio(), "CONTINENCIA"));
        boolean redistribuicaoImpedimento = anomaliaMalha.exigeEscalonamento() || containsAny(joinMotives(distribuicaoMalha), "IMPEDIMENTO", "SUSPEICAO");
        boolean urgente = distribuicaoMalha.prioridade() <= 2
                || containsAny(processo.getPreProtocoloStatus(), "PLANTAO", "URGENTE", "LIMINAR", "TUTELA")
                || containsAny(processo.getRoutingRiskLevel(), "CRITICO");
        String referenciaBase = firstNonBlank(processo.getNumero(), processo.getNumeroUnificado(), processo.getNumeroProcesso());
        return new DistribuicaoProcessualNacionalEngine.DistribuicaoRequest(
                referenciaBase,
                firstNonBlank(processo.getUf(), "BR"),
                firstNonBlank(processo.getComarca(), processo.getVara(), "COMARCA_NAO_INFORMADA"),
                rito,
                processo.getValorCausa() == null ? 0d : processo.getValorCausa().doubleValue(),
                firstNonBlank(processo.getParteAutoraNome(), "AUTOR_NAO_INFORMADO"),
                firstNonBlank(processo.getParteReuNome(), "REU_NAO_INFORMADO"),
                inferGrau(processo),
                firstNonBlank(processo.getComarca(), processo.getVara(), "CIDADE_NAO_INFORMADA"),
                firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal(), processo.getComarca(), "FORO_NAO_INFORMADO"),
                processo.getUnidadeJudiciariaCodigo(),
                firstNonBlank(processo.getUnidadeJudiciariaCodigo(), processo.getVara()),
                processo.getAssunto(),
                processo.getClasseProcessual(),
                processo.getClasseProcessual(),
                firstNonBlank(processo.getAssunto(), processo.getObjetoProcessual()),
                inferAreaEspecializada(processo),
                distribuicaoMalha.exigeRemessa() ? referenciaBase : extractReference(processo.getPreventionMode()),
                distribuicaoMalha.exigeReuniao() ? referenciaBase : extractReference(processo.getLinkageMode()),
                firstNonBlank(processo.getClasseProcessual(), "CLASSE_NAO_INFORMADA"),
                firstNonBlank(processo.getAssunto(), processo.getObjetoProcessual(), "ASSUNTO_NAO_INFORMADO"),
                dependencia,
                conexao,
                continencia,
                urgente,
                false,
                sigiloReforcado,
                anomaliaMalha.exigeEscalonamento(),
                redistribuicaoImpedimento,
                containsAny(processo.getPreProtocoloStatus(), "PLANTAO"),
                containsAny(processo.getPreProtocoloStatus(), "LIMINAR", "TUTELA", "URGENTE"),
                sigiloReforcado
        );
    }

    private static GrauJurisdicao inferGrau(Processo processo) {
        String unidade = normalizeToken(firstNonBlank(processo.getUnidadeJudiciariaCodigo(), processo.getVara()));
        if (containsAny(unidade, "CAMARA", "TURMA", "SECAO", "PLENARIO", "SEGUNDO_GRAU")) {
            return GrauJurisdicao.SEGUNDO_GRAU;
        }
        return GrauJurisdicao.PRIMEIRO_GRAU;
    }

    private static String inferAreaEspecializada(Processo processo) {
        if (processo.getMateria() != null) {
            return processo.getMateria().name();
        }
        if (processo.getRito() != null) {
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
            if (processo.getRito().isEspecialConstitucional()) return "CONSTITUCIONAL";
        }
        return normalizeToken(firstNonBlank(processo.getUnidadeJudiciariaCodigo(), processo.getVara()));
    }

    private static String extractReference(String mode) {
        String normalized = firstNonBlank(mode);
        if (normalized == null) {
            return null;
        }
        int colon = normalized.indexOf(':');
        if (colon >= 0 && colon + 1 < normalized.length()) {
            return normalized.substring(colon + 1).trim();
        }
        return null;
    }

    private static String joinMotives(ProcessoDistribuicaoMalhaAggregate distribuicaoMalha) {
        return distribuicaoMalha.motivos().stream()
                .map(item -> firstNonBlank(item.dominio(), item.resumo(), item.referencia()))
                .filter(item -> item != null && !item.isBlank())
                .reduce((left, right) -> left + '|' + right)
                .orElse("");
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

    private static String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? null : normalized;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
