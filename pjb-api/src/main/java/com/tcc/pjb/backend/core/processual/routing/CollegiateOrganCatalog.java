package com.tcc.pjb.backend.core.processual.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.domain.enums.TipoJustica;

@Component
public class CollegiateOrganCatalog {

    public CollegiateOrganCatalogProfile resolve(NationalProcessRoutingService.RoutingCommand command,
                                                 TipoJustica tipoJustica,
                                                 String tribunalCodigo,
                                                 String orgaoFracionario,
                                                 String specializationAxis) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        String tribunal = normalize(firstNonBlank(tribunalCodigo, inferTribunalCode(tipoJustica, command)));
        String tribunalMacroFamily = inferMacroFamily(tribunal, tipoJustica);
        String secretariatDesk = resolveSecretariatDesk(tribunalMacroFamily, specializationAxis);
        String gabineteCluster = resolveGabineteCluster(tribunalMacroFamily, orgaoFracionario, specializationAxis);
        String presidencyChannel = resolvePresidencyChannel(tribunalMacroFamily);
        String compositionHint = resolveCompositionHint(tribunalMacroFamily, orgaoFracionario);
        String sessionCadence = resolveSessionCadence(tribunalMacroFamily, command);
        String quorumLabel = resolveQuorumLabel(tribunalMacroFamily, orgaoFracionario);

        if (tribunal == null) {
            warnings.add("Tribunal colegiado não explicitado; family macro aplicada por inferência do ramo e do grau.");
            reviewChecklist.add("Confirmar tribunal, órgão fracionário e secretaria colegiada efetivamente competentes.");
        }
        if (command != null && command.grau() != null && command.grau() != com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao.PRIMEIRO_GRAU) {
            fundamentos.add("Malha colegiada calibrada para " + firstNonBlank(tribunalMacroFamily, "COLEGIADO_GERAL") + '.');
            fundamentos.add("Desk colegiado sugerido: " + secretariatDesk + '.');
        }
        if (command != null && command.segredoSolicitado()) {
            warnings.add("Órgão colegiado deve operar em secretaria, gabinete e pauta compatíveis com sigilo reforçado.");
            reviewChecklist.add("Verificar autorização de pauta, sessão virtual e visibilidade da secretaria colegiada.");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalCode", tribunal);
        metadata.put("tipoJustica", tipoJustica != null ? tipoJustica.name() : null);
        metadata.put("orgaoFracionario", orgaoFracionario);
        metadata.put("specializationAxis", specializationAxis);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new CollegiateOrganCatalogProfile(
                tribunalMacroFamily,
                secretariatDesk,
                gabineteCluster,
                presidencyChannel,
                compositionHint,
                sessionCadence,
                quorumLabel,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private String inferMacroFamily(String tribunal, TipoJustica tipoJustica) {
        if (tribunal == null) {
            return tipoJustica == null ? "COLEGIADO_GERAL" : switch (tipoJustica) {
                case FEDERAL -> "TRF";
                case TRABALHO -> "TRT";
                case ELEITORAL -> "TRE";
                case MILITAR_ESTADUAL -> "TJM";
                case MILITAR_FEDERAL -> "STM";
                case SUPERIOR -> "TRIBUNAL_SUPERIOR";
                default -> "TJ";
            };
        }
        if (tribunal.startsWith("TRF")) return "TRF";
        if (tribunal.startsWith("TRT")) return "TRT";
        if (tribunal.startsWith("TRE")) return "TRE";
        if (tribunal.startsWith("TJM")) return "TJM";
        if (tribunal.startsWith("TJ")) return "TJ";
        if (tribunal.startsWith("STJ")) return "STJ";
        if (tribunal.startsWith("TST")) return "TST";
        if (tribunal.startsWith("TSE")) return "TSE";
        if (tribunal.startsWith("STM")) return "STM";
        if (tribunal.startsWith("STF")) return "STF";
        if (tribunal.startsWith("TNU")) return "TNU";
        return "COLEGIADO_GERAL";
    }

    private String resolveSecretariatDesk(String macroFamily, String specializationAxis) {
        return switch (firstNonBlank(macroFamily, "COLEGIADO_GERAL")) {
            case "STF" -> "SECRETARIA_JUDICIARIA_STF_" + normalize(firstNonBlank(specializationAxis, "GERAL"));
            case "STJ" -> "SECRETARIA_TURMA_STJ_" + normalize(firstNonBlank(specializationAxis, "GERAL"));
            case "TST" -> "SECRETARIA_TST_" + normalize(firstNonBlank(specializationAxis, "GERAL"));
            case "TSE" -> "SECRETARIA_JUDICIARIA_TSE";
            case "STM" -> "SECRETARIA_JUDICIARIA_STM";
            case "TRF" -> "SECRETARIA_TURMA_REGIONAL_FEDERAL";
            case "TRT" -> "SECRETARIA_TURMA_TRABALHISTA";
            case "TRE" -> "SECRETARIA_PLENARIO_ELEITORAL";
            case "TJM" -> "SECRETARIA_JUSTICA_MILITAR_ESTADUAL";
            case "TJ" -> "SECRETARIA_CAMARA_ESTADUAL";
            case "TNU" -> "SECRETARIA_TNU";
            default -> "SECRETARIA_COLEGIADA_GERAL";
        };
    }

    private String resolveGabineteCluster(String macroFamily, String orgaoFracionario, String specializationAxis) {
        return firstNonBlank(orgaoFracionario,
                switch (firstNonBlank(macroFamily, "COLEGIADO_GERAL")) {
                    case "STF" -> "GABINETE_MINISTRO_STF";
                    case "STJ" -> "GABINETE_MINISTRO_STJ";
                    case "TST" -> "GABINETE_MINISTRO_TST";
                    case "TSE" -> "GABINETE_MINISTRO_TSE";
                    case "STM" -> "GABINETE_MINISTRO_STM";
                    case "TRF" -> "GABINETE_DESEMBARGADOR_FEDERAL";
                    case "TRT" -> "GABINETE_DESEMBARGADOR_TRT";
                    case "TRE" -> "GABINETE_MEMBRO_TRE";
                    case "TJM" -> "GABINETE_JUIZ_MILITAR";
                    case "TJ" -> "GABINETE_DESEMBARGADOR_TJ";
                    case "TNU" -> "GABINETE_JUIZ_FEDERAL_TNU";
                    default -> "GABINETE_COLEGIADO_" + normalize(firstNonBlank(specializationAxis, "GERAL"));
                });
    }

    private String resolvePresidencyChannel(String macroFamily) {
        return switch (firstNonBlank(macroFamily, "COLEGIADO_GERAL")) {
            case "STF", "STJ", "TST", "TSE", "STM" -> "PRESIDENCIA_CORTE_SUPERIOR";
            case "TRF", "TRT", "TRE", "TJM", "TJ" -> "PRESIDENCIA_REGIMENTAL_COLEGIADA";
            case "TNU" -> "COORDENACAO_NACIONAL_UNIFORMIZACAO";
            default -> "PRESIDENCIA_COLEGIADA_GERAL";
        };
    }

    private String resolveCompositionHint(String macroFamily, String orgaoFracionario) {
        if (orgaoFracionario != null && orgaoFracionario.toUpperCase(Locale.ROOT).contains("PLENARIO")) {
            return "COMPOSICAO_PLENARIA";
        }
        return switch (firstNonBlank(macroFamily, "COLEGIADO_GERAL")) {
            case "STF" -> "5_OU_11_MINISTROS";
            case "STJ", "TST", "TSE", "STM" -> "COLEGIADO_SUPERIOR_ESPECIALIZADO";
            case "TRF", "TRT", "TRE", "TJM", "TJ" -> "3_OU_MAIS_JULGADORES";
            case "TNU" -> "JUIZES_FEDERAIS_UNIFORMIZACAO";
            default -> "COMPOSICAO_COLEGIADA_PADRAO";
        };
    }

    private String resolveSessionCadence(String macroFamily, NationalProcessRoutingService.RoutingCommand command) {
        if (command != null && (command.plantaoJudicial() || command.pedidoLiminar())) {
            return "PAUTA_URGENTE_OU_SESSAO_EXTRAORDINARIA";
        }
        return switch (firstNonBlank(macroFamily, "COLEGIADO_GERAL")) {
            case "STF", "STJ", "TST", "TRF", "TRT", "TJ" -> "VIRTUAL_E_PRESENCIAL";
            case "TSE", "TRE", "STM", "TJM" -> "PRESENCIAL_COM_SESSAO_REGIMENTAL";
            case "TNU" -> "VIRTUAL_COM_UNIFORMIZACAO";
            default -> "SESSAO_COLEGIADA_PADRAO";
        };
    }

    private String resolveQuorumLabel(String macroFamily, String orgaoFracionario) {
        if (orgaoFracionario != null && orgaoFracionario.toUpperCase(Locale.ROOT).contains("PLENARIO")) {
            return "QUORUM_PLENARIO";
        }
        return switch (firstNonBlank(macroFamily, "COLEGIADO_GERAL")) {
            case "STF", "STJ", "TST", "TSE", "STM" -> "QUORUM_REGIMENTAL_CORTE_SUPERIOR";
            case "TRF", "TRT", "TRE", "TJM", "TJ" -> "QUORUM_CAMARA_OU_TURMA";
            case "TNU" -> "QUORUM_UNIFORMIZACAO";
            default -> "QUORUM_COLEGIADO_PADRAO";
        };
    }

    private String inferTribunalCode(TipoJustica tipoJustica, NationalProcessRoutingService.RoutingCommand command) {
        if (command != null && command.grau() != null && command.grau() != com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao.PRIMEIRO_GRAU && tipoJustica != null) {
            return switch (tipoJustica) {
                case FEDERAL -> "TRF";
                case TRABALHO -> "TRT";
                case ELEITORAL -> "TRE";
                case MILITAR_ESTADUAL -> "TJM";
                case MILITAR_FEDERAL -> "STM";
                case SUPERIOR -> command.grau().name();
                default -> "TJ";
            };
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
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
