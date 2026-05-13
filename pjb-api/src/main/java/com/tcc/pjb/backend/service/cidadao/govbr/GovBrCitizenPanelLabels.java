package com.tcc.pjb.backend.service.cidadao.govbr;

import com.tcc.pjb.backend.model.entity.enums.PapelProcessualNacional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.util.Locale;

public final class GovBrCitizenPanelLabels {

    private GovBrCitizenPanelLabels() {
    }

    public static final String MODO_ENTRADA_GOVBR = "LOGIN_UNICO_GOVBR";
    public static final String MODO_ENTRADA_LOCAL = "IDENTIDADE_PJB";
    public static final String MODO_CONSOLIDACAO = "CPF_CANONICO_MULTISSISTEMA";
    public static final String SISTEMA_PJB = "PJB";

    public static String sourceLabel(String sistema) {
        if (sistema == null || sistema.isBlank()) {
            return "PJB";
        }
        String token = sistema.trim().toUpperCase(Locale.ROOT);
        return switch (token) {
            case "PJE" -> "PJe";
            case "ESAJ" -> "e-SAJ";
            case "EPROC" -> "eproc";
            case "CRETA" -> "Creta";
            case "PROJUDI" -> "Projudi";
            case "SEEU" -> "SEEU";
            case "PJB" -> "PJB";
            default -> token;
        };
    }

    public static String roleLabel(PapelProcessualNacional papel) {
        if (papel == null) {
            return "Vínculo processual";
        }
        return switch (papel) {
            case AUTOR -> "Autor";
            case REU -> "Réu";
            case VITIMA -> "Vítima";
            case INVESTIGADO -> "Investigado";
            case EXECUTANTE -> "Exequente";
            case EXECUTADO -> "Executado";
            case IMPETRANTE -> "Impetrante";
            case IMPETRADO -> "Impetrado";
            case INTERESSADO -> "Interessado";
            case TERCEIRO_INTERESSADO -> "Terceiro interessado";
            case ADVOGADO -> "Advogado";
            case REPRESENTANTE_LEGAL -> "Representante legal";
            case MEMBRO_MINISTERIO_PUBLICO -> "Ministério Público";
            case DEFENSOR_PUBLICO -> "Defensoria";
            case PROCURADOR_PUBLICO -> "Procuradoria";
            case PERITO -> "Perito";
            case TESTEMUNHA -> "Testemunha";
            case ASSISTENTE -> "Assistente";
            case AUTORIDADE -> "Autoridade";
            case SUJEITO_PROCESSUAL -> "Sujeito processual";
        };
    }

    public static String colorToken(String rito, RamoDireito ramo) {
        String token = rito == null || rito.isBlank()
                ? (ramo == null ? "INTEGRADO_NACIONAL" : ramo.name())
                : rito.trim().toUpperCase(Locale.ROOT);
        if (token.contains("PENAL") || token.contains("JURI") || token.contains("DROGAS") || token.contains("EXECUCAO_PENAL")) {
            return "VERMELHO_PENAL";
        }
        if (token.contains("TRABALH")) {
            return "VERDE_TRABALHISTA";
        }
        if (token.contains("ELEITORAL")) {
            return "AMARELO_ELEITORAL";
        }
        if (token.contains("MILITAR")) {
            return "GRAFITE_MILITAR";
        }
        if (token.contains("FAZENDA") || token.contains("TRIBUTARIO")) {
            return "ROXO_FAZENDARIO";
        }
        if (token.contains("FAMILIA") || token.contains("SUCESS")) {
            return "VINHO_FAMILIA";
        }
        if (token.contains("JUIZADO")) {
            return "LARANJA_JUIZADO";
        }
        if (token.contains("INFANCIA") || token.contains("ADOLESCENTE")) {
            return "CINZA_AZULADO_INFANCIA";
        }
        if (token.contains("FEDERAL")) {
            return "AZUL_FEDERAL";
        }
        return "AZUL_CIVEL";
    }

    public static String colorLabel(String colorToken) {
        if (colorToken == null || colorToken.isBlank()) {
            return "Cível";
        }
        return switch (colorToken) {
            case "VERMELHO_PENAL" -> "Penal";
            case "VERDE_TRABALHISTA" -> "Trabalhista";
            case "AMARELO_ELEITORAL" -> "Eleitoral";
            case "GRAFITE_MILITAR" -> "Militar";
            case "ROXO_FAZENDARIO" -> "Fazendário";
            case "VINHO_FAMILIA" -> "Família";
            case "LARANJA_JUIZADO" -> "Juizado";
            case "CINZA_AZULADO_INFANCIA" -> "Infância e juventude";
            case "AZUL_FEDERAL" -> "Federal";
            default -> "Cível";
        };
    }

    public static String ritoLabel(String rito, RamoDireito ramo, String fallback) {
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        if (rito != null && !rito.isBlank()) {
            return rito;
        }
        return ramo == null ? "Malha processual integrada" : ramo.name();
    }
}
