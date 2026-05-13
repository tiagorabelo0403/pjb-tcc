package com.tcc.pjb.backend.core.processual.routing;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix.RamoJusticaNacional;
import com.tcc.pjb.backend.core.forum.routing.CnjJusticeParser;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

final class NationalProcessRoutingSupport {

    NationalCompetenceMatrix resolveCompetence(String uf, RamoJusticaNacional ramoJustica, GrauJurisdicao grau, RitoProcessual rito) {
        if (grau == GrauJurisdicao.CONSTITUCIONAL || isConstitutionalExceptional(rito)) {
            return NationalCompetenceMatrix.STF;
        }
        if (grau == GrauJurisdicao.SUPERIOR) {
            return switch (ramoJustica) {
                case TRABALHO, TRABALHO_SUPERIOR -> NationalCompetenceMatrix.TST;
                case ELEITORAL, ELEITORAL_SUPERIOR -> NationalCompetenceMatrix.TSE;
                case MILITAR_ESTADUAL, MILITAR_SUPERIOR -> NationalCompetenceMatrix.STM;
                case SUPERIOR_STF -> NationalCompetenceMatrix.STF;
                default -> NationalCompetenceMatrix.STJ;
            };
        }
        return NationalCompetenceMatrix.resolver(uf, ramoJustica).orElseGet(() -> fallbackCompetence(uf, ramoJustica));
    }

    TipoJustica resolveTipoJustica(NationalProcessRoutingService.RoutingCommand command, RamoDireito ramo) {
        TipoJustica cnjHint = CnjJusticeParser.tryResolveTipoJustica(command.numeroProcesso()).orElse(null);
        if (command.grau() == GrauJurisdicao.CONSTITUCIONAL || command.grau() == GrauJurisdicao.SUPERIOR || isConstitutionalExceptional(command.rito())) {
            return TipoJustica.SUPERIOR;
        }
        if (cnjHint != null && cnjHint != TipoJustica.SUPERIOR) {
            return cnjHint;
        }
        if (command.rito().isTrabalhista() || ramo == RamoDireito.TRABALHISTA) {
            return TipoJustica.TRABALHO;
        }
        if (command.rito().isEleitoral() || ramo == RamoDireito.ELEITORAL) {
            return TipoJustica.ELEITORAL;
        }
        if (command.rito().isMilitar() || ramo == RamoDireito.MILITAR) {
            return hasMilitaryStateCourt(command.rito()) ? TipoJustica.MILITAR_ESTADUAL : TipoJustica.MILITAR_FEDERAL;
        }
        if (ramo == RamoDireito.PREVIDENCIARIO || ramo == RamoDireito.ADMINISTRATIVO || ramo == RamoDireito.TRIBUTARIO || command.rito() == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL) {
            return TipoJustica.FEDERAL;
        }
        return TipoJustica.ESTADUAL;
    }

    RamoJusticaNacional resolveRamoJustica(TipoJustica tipoJustica, GrauJurisdicao grau, RitoProcessual rito) {
        if (grau == GrauJurisdicao.CONSTITUCIONAL || isConstitutionalExceptional(rito)) {
            return RamoJusticaNacional.SUPERIOR_STF;
        }
        if (grau == GrauJurisdicao.SUPERIOR) {
            return switch (tipoJustica) {
                case TRABALHO -> RamoJusticaNacional.TRABALHO_SUPERIOR;
                case ELEITORAL -> RamoJusticaNacional.ELEITORAL_SUPERIOR;
                case MILITAR_ESTADUAL, MILITAR_FEDERAL -> RamoJusticaNacional.MILITAR_SUPERIOR;
                default -> RamoJusticaNacional.SUPERIOR;
            };
        }
        return switch (tipoJustica) {
            case FEDERAL -> RamoJusticaNacional.FEDERAL;
            case TRABALHO -> RamoJusticaNacional.TRABALHO;
            case ELEITORAL -> RamoJusticaNacional.ELEITORAL;
            case MILITAR_ESTADUAL -> RamoJusticaNacional.MILITAR_ESTADUAL;
            case MILITAR_FEDERAL -> RamoJusticaNacional.MILITAR_SUPERIOR;
            default -> RamoJusticaNacional.ESTADUAL;
        };
    }

    boolean requiresUrgentHandling(RitoProcessual rito) {
        return rito == RitoProcessual.ESPECIAL_HABEAS_CORPUS
                || rito == RitoProcessual.PENAL_HABEAS_CORPUS_PREVENTIVO
                || rito == RitoProcessual.CIVIL_TUTELA_URGENTE
                || rito == RitoProcessual.CIVIL_TUTELA_CAUTELAR_ANTECEDENTE
                || rito == RitoProcessual.CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE
                || rito == RitoProcessual.ESPECIAL_MANDADO_SEGURANCA
                || rito == RitoProcessual.ESPECIAL_MANDADO_SEGURANCA_COLETIVO;
    }

    boolean shouldDefaultConciliation(RitoProcessual rito) {
        return !rito.isPenal() && !rito.isEleitoral() && !rito.isMilitar() && rito != RitoProcessual.EXECUCAO_FISCAL;
    }

    boolean isJuizado(RitoProcessual rito) {
        return rito == RitoProcessual.JUIZADO_ESPECIAL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_CIVEL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA
                || rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL;
    }

    boolean isContentiousCivil(RamoDireito ramo, RitoProcessual rito) {
        return ramo == RamoDireito.CIVIL || ramo == RamoDireito.FAMILIA || ramo == RamoDireito.CONSUMIDOR || rito.name().startsWith("CIVIL_");
    }

    boolean hasMilitaryStateCourt(RitoProcessual rito) {
        return rito != null && rito.isMilitar();
    }

    boolean isConstitutionalExceptional(RitoProcessual rito) {
        return rito == RitoProcessual.ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE
                || rito == RitoProcessual.ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE
                || rito == RitoProcessual.ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL;
    }

    String specializationToken(RitoProcessual rito, String unidadeBase) {
        Objects.requireNonNull(rito);
        if (rito.name().startsWith("JUIZADO")) {
            return "JUIZADO";
        }
        if (rito.name().startsWith("TRABALHISTA")) {
            return "VARA_TRABALHO";
        }
        if (rito.name().startsWith("ELEITORAL")) {
            return "ZONA_ELEITORAL";
        }
        if (rito.name().startsWith("MILITAR")) {
            return "AUDITORIA_MILITAR";
        }
        if (rito.name().contains("FAZENDA")) {
            return "VARA_FAZENDA";
        }
        if (rito.name().contains("FAMILIA")) {
            return "VARA_FAMILIA";
        }
        if (rito.name().contains("INVENTARIO") || rito.name().contains("SUCESS")) {
            return "VARA_SUCESSOES";
        }
        if (rito == RitoProcessual.PENAL_MARIA_DA_PENHA) {
            return "VARA_VIOLENCIA_DOMESTICA";
        }
        if (rito.isInfancia()) {
            return "VARA_INFANCIA_JUVENTUDE";
        }
        if (rito.isPrevidenciario()) {
            return "VARA_PREVIDENCIARIA";
        }
        if (rito.isAmbiental()) {
            return "VARA_AMBIENTAL";
        }
        if (rito.isAgrario()) {
            return "VARA_AGRARIA";
        }
        if (rito.isEmpresarial()) {
            return "VARA_EMPRESARIAL";
        }
        if (rito.name().contains("PENAL") || rito.name().contains("JURI")) {
            return "VARA_CRIMINAL";
        }
        return normalizeToken(firstNonBlank(unidadeBase, "VARA"));
    }

    String normalizeUf(String uf) {
        if (uf == null || uf.isBlank()) {
            return null;
        }
        return uf.trim().toUpperCase(Locale.ROOT);
    }

    String normalizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
    }

    String metadataString(Map<String, Object> source, String dottedPath) {
        if (source == null || dottedPath == null || dottedPath.isBlank()) {
            return null;
        }
        Object current = source;
        for (String token : dottedPath.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(token);
            if (current == null) {
                return null;
            }
        }
        if (current instanceof String value) {
            return value.isBlank() ? null : value.trim();
        }
        return String.valueOf(current);
    }

    String firstNonBlank(String... values) {
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

    private NationalCompetenceMatrix fallbackCompetence(String uf, RamoJusticaNacional ramoJustica) {
        return switch (ramoJustica) {
            case FEDERAL -> NationalCompetenceMatrix.TRF5;
            case TRABALHO -> NationalCompetenceMatrix.TRT7;
            case ELEITORAL -> NationalCompetenceMatrix.TRE_CE;
            case MILITAR_ESTADUAL -> switch (normalizeUf(uf)) {
                case "MG" -> NationalCompetenceMatrix.TJM_MG;
                case "RS" -> NationalCompetenceMatrix.TJM_RS;
                case "SP" -> NationalCompetenceMatrix.TJM_SP;
                default -> NationalCompetenceMatrix.STM;
            };
            case MILITAR_SUPERIOR -> NationalCompetenceMatrix.STM;
            case TRABALHO_SUPERIOR -> NationalCompetenceMatrix.TST;
            case ELEITORAL_SUPERIOR -> NationalCompetenceMatrix.TSE;
            case SUPERIOR_STF -> NationalCompetenceMatrix.STF;
            case SUPERIOR -> NationalCompetenceMatrix.STJ;
            default -> Optional.ofNullable(normalizeUf(uf))
                    .flatMap(code -> NationalCompetenceMatrix.resolver(code, RamoJusticaNacional.ESTADUAL))
                    .orElse(NationalCompetenceMatrix.TJCE);
        };
    }
}
