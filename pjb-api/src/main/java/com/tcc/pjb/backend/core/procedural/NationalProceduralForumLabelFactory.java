package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

@Component
public class NationalProceduralForumLabelFactory {

    String buildForoLabel(String cidade, String uf, TipoJustica tipoJustica) {
        if (isBlank(cidade) && isBlank(uf)) {
            return null;
        }
        String prefix = switch (tipoJustica) {
            case FEDERAL -> "Subseção Judiciária de ";
            case TRABALHO -> "Foro Trabalhista de ";
            case ELEITORAL -> "Zona/Foro Eleitoral de ";
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "Circunscrição/Auditoria de ";
            case SUPERIOR -> "Tribunal Superior com origem em ";
            default -> "Comarca de ";
        };
        return prefix + firstNonBlank(cidade, "base territorial não informada") + (isBlank(uf) ? "" : "/" + uf.toUpperCase(Locale.ROOT));
    }

    String buildVaraLabel(String rito,
                          NationalProceduralActionProfile actionProfile,
                          TipoJustica tipoJustica,
                          NationalProceduralJuizadoDecision juizadoDecision,
                          Map<String, Object> payload) {
        if (!isBlank(text(payload.get("varaPretendida")))) {
            return text(payload.get("varaPretendida"));
        }
        if (juizadoDecision.ritoOverride() != null) {
            return switch (juizadoDecision.ritoOverride()) {
                case "JUIZADO_ESPECIAL_CIVEL" -> "Juizado Especial Cível";
                case "JUIZADO_ESPECIAL_FAZENDA_PUBLICA" -> "Juizado Especial da Fazenda Pública";
                case "JUIZADO_ESPECIAL_FEDERAL" -> "Juizado Especial Federal";
                case "JUIZADO_ESPECIAL_CRIMINAL" -> "Juizado Especial Criminal";
                default -> actionProfile.varaFamily();
            };
        }
        if (tipoJustica == TipoJustica.TRABALHO) {
            return "Vara do Trabalho";
        }
        if (tipoJustica == TipoJustica.ELEITORAL) {
            return "Zona Eleitoral / TRE competente";
        }
        if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
            return "Auditoria Militar / Conselho de Justiça";
        }
        if (containsAny(rito, "TRIBUNAL_JURI")) {
            return "Vara do Tribunal do Júri";
        }
        if (containsAny(rito, "EXECUCAO_PENAL")) {
            return "Vara de Execuções Penais";
        }
        if (containsAny(rito, "INFANCIA_JUVENTUDE")) {
            return "Vara da Infância e Juventude";
        }
        if (containsAny(rito, "ADMINISTRATIVO_PAD")) {
            return "Vara da Fazenda Pública / órgão com competência em controle disciplinar";
        }
        if (containsAny(rito, "ADMINISTRATIVO_CONCURSO_PUBLICO", "ADMINISTRATIVO_SERVIDORES", "ADMINISTRATIVO")) {
            return "Vara da Fazenda Pública / Vara de Direito Público";
        }
        if (containsAny(rito, "FAZENDA")) {
            return "Vara da Fazenda Pública";
        }
        if (containsAny(rito, "PREVIDENCIARIO_JEF", "JUIZADO_ESPECIAL_FEDERAL")) {
            return "Juizado Especial Federal";
        }
        if (containsAny(rito,
                "PREVIDENCIARIO_COMUM",
                "PREVIDENCIARIO_BPC_LOAS",
                "PREVIDENCIARIO_AUXILIO_INCAPACIDADE",
                "PREVIDENCIARIO_APOSENTADORIA",
                "PREVIDENCIARIO_REVISAO_BENEFICIO",
                "PREVIDENCIARIO_RESTABELECIMENTO",
                "PREVIDENCIARIO_ACIDENTARIO",
                "PREVIDENCIARIO_SALARIO_MATERNIDADE",
                "PREVIDENCIARIO_PENSAO_MORTE",
                "PREVIDENCIARIO_RURAL",
                "PREVIDENCIARIO_ESPECIAL",
                "PREVIDENCIARIO_RPPS")) {
            return "Vara Federal Previdenciária / Juizado Federal Previdenciário";
        }
        return firstNonBlank(actionProfile.varaFamily(), "Vara Cível");
    }

    private static boolean containsAny(String value, String... keys) {
        return NationalProceduralRoutingSupport.containsAny(value, keys);
    }

    private static String firstNonBlank(String... values) {
        return NationalProceduralRoutingSupport.firstNonBlank(values);
    }

    private static boolean isBlank(String value) {
        return NationalProceduralRoutingSupport.isBlank(value);
    }

    private static String text(Object value) {
        return NationalProceduralRoutingSupport.text(value);
    }
}
