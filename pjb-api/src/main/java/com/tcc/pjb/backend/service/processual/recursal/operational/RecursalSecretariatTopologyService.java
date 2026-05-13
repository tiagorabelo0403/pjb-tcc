package com.tcc.pjb.backend.service.processual.recursal.operational;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RecursalSecretariatTopologyService {

    public Map<String, Object> resolve(Processo processo,
                                       LegalAppealType appealType,
                                       RecursalAdmissibilityResponse admissibility) {
        String tribunal = firstNonBlank(
                admissibility == null ? null : admissibility.tribunalDestino(),
                processo == null ? null : processo.getTribunal(),
                "TRIBUNAL_NAO_IDENTIFICADO"
        );
        String instancia = normalizeInstance(firstNonBlank(
                admissibility == null ? null : admissibility.instanciaDestino(),
                inferInstanceFromCourt(tribunal)
        ));
        String secretariaOrigem = firstNonBlank(admissibility == null ? null : admissibility.secretariaOrigem(), inferOriginSecretariat(tribunal, instancia));
        String secretariaDestino = firstNonBlank(admissibility == null ? null : admissibility.secretariaDestino(), inferDestinationSecretariat(tribunal, instancia));
        String admissibilityDesk = firstNonBlank(admissibility == null ? null : admissibility.admissibilityDesk(), inferAdmissibilityDesk(tribunal, instancia));
        String reviewDesk = firstNonBlank(admissibility == null ? null : admissibility.reviewDesk(), inferReviewDesk(tribunal, instancia));
        String counterReasonsDesk = firstNonBlank(admissibility == null ? null : admissibility.counterReasonsDesk(), secretariaDestino);
        String protocolDesk = firstNonBlank(admissibility == null ? null : admissibility.protocolDesk(), secretariaDestino);
        String autuacaoDesk = firstNonBlank(admissibility == null ? null : admissibility.autuacaoDesk(), secretariaDestino);
        String distributionDesk = firstNonBlank(admissibility == null ? null : admissibility.distributionDesk(), secretariaDestino);
        String supportDesk = firstNonBlank(admissibility == null ? null : admissibility.supportDesk(), inferSupportDesk(tribunal, instancia));
        String embargosDesk = resolveEmbargosDesk(appealType, secretariaDestino, reviewDesk, counterReasonsDesk, admissibilityDesk, tribunal, instancia);

        String secretariaInstanciaClassificada = resolveSecretariatInstanceClass(instancia);
        String secretariaRamoClassificado = resolveSecretariatBranchClass(tribunal, admissibility == null ? null : admissibility.secretariaDestino());
        String namespacePjb = resolvePjbNamespace(instancia, secretariaRamoClassificado);
        String painelPjb = resolvePjbDisplayName(namespacePjb, secretariaInstanciaClassificada, secretariaRamoClassificado);
        String secretariaEspecializada = firstNonBlank(secretariaDestino, buildSpecializedDestination(tribunal, instancia));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("tribunal", tribunal);
        out.put("instancia", instancia);
        out.put("secretariaOrigem", secretariaOrigem);
        out.put("secretariaDestino", secretariaDestino);
        out.put("admissibilityDesk", admissibilityDesk);
        out.put("reviewDesk", reviewDesk);
        out.put("counterReasonsDesk", counterReasonsDesk);
        out.put("protocolDesk", protocolDesk);
        out.put("autuacaoDesk", autuacaoDesk);
        out.put("distributionDesk", distributionDesk);
        out.put("supportDesk", supportDesk);
        out.put("secretariaPoloRecursalExiste", !isBlank(secretariaDestino) || !isBlank(admissibilityDesk));
        out.put("secretariaUltimaInstanciaExiste", "SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia));
        out.put("secretariaEmbargosExiste", !isBlank(embargosDesk));
        out.put("secretariaEmbargos", embargosDesk);
        out.put("orgaoEmbargos", firstNonBlank(embargosDesk, reviewDesk, secretariaDestino));
        out.put("secretariaEspecializada", secretariaEspecializada);
        out.put("secretariaInstanciaClassificada", secretariaInstanciaClassificada);
        out.put("secretariaRamoClassificado", secretariaRamoClassificado);
        out.put("namespacePjb", namespacePjb);
        out.put("painelPjb", painelPjb);
        out.put("secretariaSegundaInstancia", "SEGUNDA_INSTANCIA".equals(secretariaInstanciaClassificada) ? secretariaEspecializada : null);
        out.put("secretariaInstanciaSuperior", "INSTANCIA_SUPERIOR".equals(secretariaInstanciaClassificada) ? secretariaEspecializada : null);
        out.put("secretariaJuizadoEspecial", "JUIZADO_ESPECIAL".equals(secretariaRamoClassificado) ? secretariaEspecializada : null);
        out.put("secretariaTrabalhista", "TRABALHISTA".equals(secretariaRamoClassificado) ? secretariaEspecializada : null);
        out.put("secretariaEleitoral", "ELEITORAL".equals(secretariaRamoClassificado) ? secretariaEspecializada : null);
        out.put("secretariaMilitar", "MILITAR".equals(secretariaRamoClassificado) ? secretariaEspecializada : null);
        out.put("etiquetaTopologia", buildLabel(tribunal, instancia, secretariaDestino, embargosDesk));
        out.put("alcance", inferScope(instancia));
        return Collections.unmodifiableMap(out);
    }

    private String resolveEmbargosDesk(LegalAppealType appealType,
                                       String secretariaDestino,
                                       String reviewDesk,
                                       String counterReasonsDesk,
                                       String admissibilityDesk,
                                       String tribunal,
                                       String instancia) {
        if (appealType != null && appealType.name().startsWith("EMBARGOS")) {
            return firstNonBlank(reviewDesk, secretariaDestino, admissibilityDesk, counterReasonsDesk, inferEmbargosDesk(tribunal, instancia));
        }
        return firstNonBlank(reviewDesk, inferEmbargosDesk(tribunal, instancia));
    }

    private String inferOriginSecretariat(String tribunal, String instancia) {
        if ("PRIMEIRO_GRAU".equals(instancia)) {
            return "SECRETARIA_UNIDADE_ORIGEM_" + normalizeToken(tribunal);
        }
        return "SECRETARIA_RECURSAL_ORIGEM_" + normalizeToken(tribunal);
    }

    private String inferDestinationSecretariat(String tribunal, String instancia) {
        return switch (instancia) {
            case "SEGUNDO_GRAU" -> buildSpecializedDestination(tribunal, instancia);
            case "SUPERIOR" -> buildSpecializedDestination(tribunal, instancia);
            case "CONSTITUCIONAL" -> buildSpecializedDestination(tribunal, instancia);
            default -> "SECRETARIA_DESTINO_" + normalizeToken(tribunal);
        };
    }

    private String resolveSecretariatInstanceClass(String instancia) {
        return switch (instancia) {
            case "SEGUNDO_GRAU" -> "SEGUNDA_INSTANCIA";
            case "SUPERIOR", "CONSTITUCIONAL" -> "INSTANCIA_SUPERIOR";
            default -> "PRIMEIRO_GRAU";
        };
    }

    private String resolveSecretariatBranchClass(String tribunal, String secretariaDestino) {
        String token = normalizeToken(firstNonBlank(tribunal, secretariaDestino));
        if (token.contains("TURMA_RECURSAL") || token.contains("JUIZADO")) {
            return "JUIZADO_ESPECIAL";
        }
        if (token.startsWith("TSE") || token.startsWith("TRE")) {
            return "ELEITORAL";
        }
        if (token.startsWith("TST") || token.startsWith("TRT")) {
            return "TRABALHISTA";
        }
        if (token.startsWith("STM") || token.startsWith("TJM") || token.contains("AUDITORIA")) {
            return "MILITAR";
        }
        if (token.startsWith("STJ") || token.startsWith("TRF") || token.startsWith("JF")) {
            return "FEDERAL";
        }
        return "ESTADUAL";
    }

    private String resolvePjbNamespace(String instancia, String ramo) {
        if ("SEGUNDO_GRAU".equals(instancia)) {
            return "PJB_SEGUNDA_INSTANCIA";
        }
        if ("SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)) {
            return "PJB_INSTANCIA_SUPERIOR";
        }
        return switch (ramo) {
            case "JUIZADO_ESPECIAL" -> "PJB_JUIZADO_ESPECIAL";
            case "TRABALHISTA" -> "PJB_TRABALHISTA";
            case "ELEITORAL" -> "PJB_ELEITORAL";
            case "MILITAR" -> "PJB_MILITAR";
            case "FEDERAL" -> "PJB_FEDERAL";
            default -> "PJB_ESTADUAL";
        };
    }

    private String resolvePjbDisplayName(String namespace, String instancia, String ramo) {
        String base = switch (namespace) {
            case "PJB_SEGUNDA_INSTANCIA" -> "PJB Segunda Instância";
            case "PJB_INSTANCIA_SUPERIOR" -> "PJB Instância Superior";
            case "PJB_JUIZADO_ESPECIAL" -> "PJB Juizado Especial";
            case "PJB_TRABALHISTA" -> "PJB Trabalhista";
            case "PJB_ELEITORAL" -> "PJB Eleitoral";
            case "PJB_MILITAR" -> "PJB Militar";
            case "PJB_FEDERAL" -> "PJB Federal";
            default -> "PJB Estadual";
        };
        if (base.contains("Instância") || ramo == null || ramo.isBlank()) {
            return base + (ramo == null || ramo.isBlank() ? "" : " | " + ramo.replace('_', ' '));
        }
        return base + (instancia == null || instancia.isBlank() ? "" : " | " + instancia.replace('_', ' '));
    }

    private String buildSpecializedDestination(String tribunal, String instancia) {
        String ramo = resolveSecretariatBranchClass(tribunal, tribunal);
        String tribunalToken = normalizeToken(tribunal);
        if ("SEGUNDO_GRAU".equals(instancia)) {
            return switch (ramo) {
                case "ELEITORAL" -> "SECRETARIA_JUDICIARIA_ELEITORAL_2G_" + tribunalToken;
                case "TRABALHISTA" -> "SECRETARIA_GERAL_JUDICIARIA_TRABALHISTA_2G_" + tribunalToken;
                case "MILITAR" -> "SECRETARIA_JUDICIARIA_MILITAR_2G_" + tribunalToken;
                case "JUIZADO_ESPECIAL" -> "SECRETARIA_TURMA_RECURSAL_JUIZADO_" + tribunalToken;
                case "FEDERAL" -> "SECRETARIA_JUDICIARIA_FEDERAL_2G_" + tribunalToken;
                default -> "SECRETARIA_JUDICIARIA_ESTADUAL_2G_" + tribunalToken;
            };
        }
        if ("SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)) {
            return switch (ramo) {
                case "ELEITORAL" -> "SECRETARIA_JUDICIARIA_ELEITORAL_SUPERIOR_" + tribunalToken;
                case "TRABALHISTA" -> "SECRETARIA_GERAL_JUDICIARIA_TRABALHISTA_SUPERIOR_" + tribunalToken;
                case "MILITAR" -> "SECRETARIA_JUDICIARIA_MILITAR_SUPERIOR_" + tribunalToken;
                case "FEDERAL" -> "SECRETARIA_JUDICIARIA_FEDERAL_SUPERIOR_" + tribunalToken;
                default -> "SECRETARIA_JUDICIARIA_SUPERIOR_" + tribunalToken;
            };
        }
        return "SECRETARIA_DESTINO_" + tribunalToken;
    }

    private String inferAdmissibilityDesk(String tribunal, String instancia) {
        if ("SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)) {
            return "MESA_ADMISSIBILIDADE_RECURSAL_" + normalizeToken(tribunal);
        }
        return "MESA_ADMISSIBILIDADE_" + normalizeToken(tribunal);
    }

    private String inferReviewDesk(String tribunal, String instancia) {
        return ("SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)
                ? "MESA_REVISAO_JUDICIARIA_"
                : "MESA_REVISAO_RECURSAL_") + normalizeToken(tribunal);
    }

    private String inferSupportDesk(String tribunal, String instancia) {
        return ("SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)
                ? "SECRETARIA_SUPORTE_CORTE_"
                : "SECRETARIA_SUPORTE_RECURSAL_") + normalizeToken(tribunal);
    }

    private String inferEmbargosDesk(String tribunal, String instancia) {
        return ("SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)
                ? "SECRETARIA_EMBARGOS_CORTE_"
                : "SECRETARIA_EMBARGOS_RECURSAIS_") + normalizeToken(tribunal);
    }

    private String inferInstanceFromCourt(String tribunal) {
        String token = normalizeToken(tribunal);
        if (token.startsWith("STF")) {
            return "CONSTITUCIONAL";
        }
        if (token.startsWith("STJ") || token.startsWith("TST") || token.startsWith("TSE") || token.startsWith("STM")) {
            return "SUPERIOR";
        }
        if (token.startsWith("TJ") || token.startsWith("TRF") || token.startsWith("TRE") || token.startsWith("TRT") || token.startsWith("TJM")) {
            return "SEGUNDO_GRAU";
        }
        return "PRIMEIRO_GRAU";
    }

    private String normalizeInstance(String value) {
        String token = normalizeToken(value);
        if (token.contains("CONSTITUC")) {
            return "CONSTITUCIONAL";
        }
        if (token.contains("SUPERIOR") || token.contains("ULTIMA")) {
            return "SUPERIOR";
        }
        if (token.contains("SEGUNDO") || token.contains("2")) {
            return "SEGUNDO_GRAU";
        }
        return "PRIMEIRO_GRAU";
    }

    private String inferScope(String instancia) {
        return switch (instancia) {
            case "CONSTITUCIONAL" -> "ULTIMA_INSTANCIA_CONSTITUCIONAL";
            case "SUPERIOR" -> "ULTIMA_INSTANCIA_INFRACONSTITUCIONAL";
            case "SEGUNDO_GRAU" -> "POLO_RECURSAL_ORDINARIO";
            default -> "UNIDADE_ORIGEM";
        };
    }

    private String buildLabel(String tribunal, String instancia, String secretariaDestino, String embargosDesk) {
        return String.join(" | ",
                firstNonBlank(tribunal, "TRIBUNAL"),
                firstNonBlank(instancia, "INSTANCIA"),
                firstNonBlank(secretariaDestino, "SECRETARIA_DESTINO"),
                firstNonBlank(embargosDesk, "SEM_MESA_EMBARGOS_EXPLICITA"));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeToken(String value) {
        return value == null ? "NAO_IDENTIFICADO" : value.trim().replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }
}
