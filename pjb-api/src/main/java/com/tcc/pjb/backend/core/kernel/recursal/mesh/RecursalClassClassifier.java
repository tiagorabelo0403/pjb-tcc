package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Objects;
import com.tcc.pjb.backend.core.util.EnumText;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public final class RecursalClassClassifier {

    private RecursalClassClassifier() {
    }

    public static RecursalClassFamily classify(String classeProcessual, RitoProcessual rito, RamoDireito ramo) {
        String token = EnumText.normalizeToken(Objects.toString(classeProcessual, ""));
        if (rito == null && ramo == null && token.isBlank()) {
            return RecursalClassFamily.OUTRA;
        }
        if (rito != null) {
            if (rito.isEspecialConstitucional()) {
                return RecursalClassFamily.CONSTITUCIONAL;
            }
            if (rito.isPenal()) {
                return rito == RitoProcessual.TRIBUNAL_JURI ? RecursalClassFamily.CRIMINAL_JURI : RecursalClassFamily.CRIMINAL_ACAO;
            }
            if (rito.isTrabalhista()) {
                return rito == RitoProcessual.TRABALHISTA_EXECUCAO
                        ? RecursalClassFamily.TRABALHISTA_EXECUCAO
                        : RecursalClassFamily.TRABALHISTA_CONHECIMENTO;
            }
            if (rito.isEleitoral()) {
                return RecursalClassFamily.ELEITORAL_CONTENCIOSO;
            }
            if (rito.isMilitar()) {
                return RecursalClassFamily.MILITAR_PENAL;
            }
            if (rito.isJuizado()) {
                return RecursalClassFamily.JUIZADO_ESPECIAL;
            }
            if (rito.isTribFazenda()) {
                return RecursalClassFamily.TRIBUTARIO_FISCAL;
            }
            if (rito.isPrevidenciario()) {
                return RecursalClassFamily.PREVIDENCIARIO;
            }
            if (rito.isInfancia()) {
                return RecursalClassFamily.INFANCIA_JUVENTUDE;
            }
            if (rito.isAmbiental()) {
                return RecursalClassFamily.AMBIENTAL;
            }
            if (rito.isAgrario()) {
                return RecursalClassFamily.AGRARIO;
            }
            if (rito.isEmpresarial()) {
                return RecursalClassFamily.EMPRESARIAL;
            }
            if (rito.isInternacional()) {
                return RecursalClassFamily.INTERNACIONAL;
            }
            if (rito == RitoProcessual.CIVIL_FAMILIA_ALIMENTOS
                    || rito == RitoProcessual.CIVIL_FAMILIA_DIVORCIO
                    || rito == RitoProcessual.CIVIL_INVENTARIO_ARROLAMENTO
                    || rito == RitoProcessual.CIVIL_DISSOLUCAO_CASAMENTO) {
                return RecursalClassFamily.FAMILIA_SUCESSOES;
            }
            if (rito == RitoProcessual.CUMPRIMENTO_SENTENCA
                    || rito == RitoProcessual.CUMPRIMENTO_PROVISORIO
                    || rito == RitoProcessual.EXECUCAO_TITULO_JUDICIAL
                    || rito == RitoProcessual.EXECUCAO_TITULO_EXTRAJUDICIAL
                    || rito == RitoProcessual.EXECUCAO_FISCAL) {
                return RecursalClassFamily.CIVIL_EXECUCAO;
            }
            return RecursalClassFamily.CIVIL_CONHECIMENTO;
        }
        if (ramo != null) {
            return switch (ramo) {
                case PENAL, PROCESSUAL_PENAL, EXECUCAO_PENAL -> RecursalClassFamily.CRIMINAL_ACAO;
                case MILITAR -> RecursalClassFamily.MILITAR_PENAL;
                case ELEITORAL, PROCESSUAL_ELEITORAL -> RecursalClassFamily.ELEITORAL_CONTENCIOSO;
                case TRABALHISTA, PROCESSUAL_TRABALHISTA -> RecursalClassFamily.TRABALHISTA_CONHECIMENTO;
                case PREVIDENCIARIO, ACIDENTARIO -> RecursalClassFamily.PREVIDENCIARIO;
                case FAMILIA, SUCESSOES -> RecursalClassFamily.FAMILIA_SUCESSOES;
                case INFANCIA_JUVENTUDE -> RecursalClassFamily.INFANCIA_JUVENTUDE;
                case EMPRESARIAL, FALIMENTAR_RECUPERACIONAL -> RecursalClassFamily.EMPRESARIAL;
                case ADMINISTRATIVO, LICITACOES_CONTRATOS, IMPROBIDADE_ADMINISTRATIVA, SERVIDOR_PUBLICO, REGULATORIO, ADUANEIRO -> RecursalClassFamily.ADMINISTRATIVO;
                case AMBIENTAL, URBANISTICO, CIVIL_PUBLICA_COLETIVO, MINERARIO, ENERGETICO -> RecursalClassFamily.AMBIENTAL;
                case AGRARIO -> RecursalClassFamily.AGRARIO;
                case TRIBUTARIO, EXECUCAO_FISCAL -> RecursalClassFamily.TRIBUTARIO_FISCAL;
                case CONSTITUCIONAL -> RecursalClassFamily.CONSTITUCIONAL;
                case INTERNACIONAL -> RecursalClassFamily.INTERNACIONAL;
                default -> RecursalClassFamily.CIVIL_CONHECIMENTO;
            };
        }
        if (token.contains("JUIZADO") || token.contains("JEF")) {
            return RecursalClassFamily.JUIZADO_ESPECIAL;
        }
        if (token.contains("EXECUCAO_FISCAL") || token.contains("CDA") || token.contains("TRIBUTARIO") || token.contains("FISCAL")) {
            return RecursalClassFamily.TRIBUTARIO_FISCAL;
        }
        if (token.contains("EXECUCAO")) {
            return RecursalClassFamily.CIVIL_EXECUCAO;
        }
        if (token.contains("PENAL") || token.contains("CRIMINAL") || token.contains("QUEIXA")) {
            return RecursalClassFamily.CRIMINAL_ACAO;
        }
        if (token.contains("FAMILIA") || token.contains("DIVORCIO") || token.contains("ALIMENTOS") || token.contains("INVENTARIO") || token.contains("SUCESSOES")) {
            return RecursalClassFamily.FAMILIA_SUCESSOES;
        }
        if (token.contains("PREVIDENCIARIO") || token.contains("BPC") || token.contains("LOAS") || token.contains("INSS") || token.contains("ACIDENTARIO")) {
            return RecursalClassFamily.PREVIDENCIARIO;
        }
        if (token.contains("AMBIENTAL") || token.contains("COLETIVO") || token.contains("URBANISTICO")) {
            return RecursalClassFamily.AMBIENTAL;
        }
        if (token.contains("AGRARIO") || token.contains("FUNDIARIO")) {
            return RecursalClassFamily.AGRARIO;
        }
        if (token.contains("EMPRESARIAL") || token.contains("FALENCIA") || token.contains("RECUPERACAO_JUDICIAL")) {
            return RecursalClassFamily.EMPRESARIAL;
        }
        if (token.contains("MANDADO_DE_SEGURANCA") || token.contains("ADI") || token.contains("ADPF") || token.contains("ADC")) {
            return RecursalClassFamily.CONSTITUCIONAL;
        }
        if (token.contains("INTERNACIONAL") || token.contains("HOMOLOGACAO_SENTENCA_ESTRANGEIRA") || token.contains("CARTA_ROGATORIA") || token.contains("COOPERACAO_INTERNACIONAL")) {
            return RecursalClassFamily.INTERNACIONAL;
        }
        return RecursalClassFamily.CIVIL_CONHECIMENTO;
    }
}
