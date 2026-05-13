package com.tcc.pjb.backend.platform.jusos.v2.jurimetria;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class JurimetriaSupportUtils {

    private JurimetriaSupportUtils() {
    }

    static double ratio(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return (double) numerator / (double) denominator;
    }

    static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    static double blend(double base, double external, double externalWeight) {
        double weight = Math.max(0.0, Math.min(1.0, externalWeight));
        return clamp(base * (1.0 - weight) + external * weight);
    }

    static double confidenceFromVolume(int volume) {
        if (volume >= 80) {
            return 0.92;
        }
        if (volume >= 30) {
            return 0.82;
        }
        if (volume >= 10) {
            return 0.70;
        }
        return 0.52;
    }

    static List<String> immutableDistinct(Collection<String> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String item : items) {
            String normalized = processLabel(item);
            if (!normalized.isBlank()) {
                set.add(normalized);
            }
        }
        return List.copyOf(set);
    }

    static String processLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    static String normalizeToken(String value) {
        return processLabel(value).toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Â', 'A')
                .replace('Ã', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    static String truncate(String value, int max) {
        String text = processLabel(value);
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, Math.max(0, max - 1)).trim();
    }

    static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String text = processLabel(value);
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    static void addIfHasText(List<String> list, String value) {
        String text = processLabel(value);
        if (!text.isBlank()) {
            list.add(text);
        }
    }

    static String resumirPrecedente(Precedente precedente) {
        return truncate(firstNonBlank(precedente.getTese(), precedente.getTitulo(), precedente.getEmentaResumo()), 160);
    }

    static double estimarTempoDias(RamoDireito ramo, GrauJurisdicao grau) {
        double base = estimarTempoBaseDias(ramo);
        return switch (grau) {
            case PRIMEIRO_GRAU -> base;
            case SEGUNDO_GRAU -> base * 1.35;
            case SUPERIOR -> base * 2.10;
            case CONSTITUCIONAL -> base * 2.60;
        };
    }

    private static double estimarTempoBaseDias(RamoDireito ramo) {
        if (ramo == null) {
            return 610;
        }
        if (ramo == RamoDireito.CONSUMIDOR) {
            return 380;
        }
        if (ramo == RamoDireito.PREVIDENCIARIO) {
            return 560;
        }
        if (ramo == RamoDireito.FAMILIA || ramo == RamoDireito.SUCESSOES) {
            return 520;
        }
        if (ramo == RamoDireito.EMPRESARIAL || ramo == RamoDireito.FALIMENTAR_RECUPERACIONAL) {
            return 760;
        }
        if (ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.EXECUCAO_FISCAL) {
            return 1380;
        }
        if (ramo == RamoDireito.AMBIENTAL
                || ramo == RamoDireito.URBANISTICO
                || ramo == RamoDireito.CIVIL_PUBLICA_COLETIVO
                || ramo == RamoDireito.MINERARIO
                || ramo == RamoDireito.ENERGETICO) {
            return 980;
        }
        if (ramo == RamoDireito.CONSTITUCIONAL) {
            return 1640;
        }
        if (ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            return 300;
        }
        if (ramo == RamoDireito.AGRARIO) {
            return 860;
        }
        if (ramo == RamoDireito.INTERNACIONAL) {
            return 1180;
        }
        return switch (ramo.verticalPrincipal()) {
            case "TRABALHISTA" -> 340;
            case "PENAL" -> ramo == RamoDireito.MILITAR ? 610 : 720;
            case "ELEITORAL" -> 240;
            case "FAZENDA" -> 840;
            case "DIFUSO" -> 980;
            case "CIVEL" -> 610;
            default -> 610;
        };
    }

    static String interpretarTempo(double dias) {
        if (dias < 365) {
            return "Tramitação potencialmente rápida";
        }
        if (dias < 730) {
            return "Tramitação em faixa moderada";
        }
        if (dias < 1460) {
            return "Tramitação longa com pressão de custo temporal";
        }
        return "Tramitação estruturalmente longa — exigir estratégia de marcos e redução de desgaste";
    }

    static List<String> buildDeadlineTriggers(NationalPrazoEngine.PrazoCalculado prazo, String titulo) {
        List<String> gatilhos = new ArrayList<>();
        if (prazo != null) {
            gatilhos.add(titulo + ": vencimento projetado em " + prazo.vencimento());
            if (!prazo.advertencias().isEmpty()) {
                gatilhos.addAll(prazo.advertencias().stream().limit(2).toList());
            }
        } else {
            gatilhos.add("Monitorar publicação, intimação e redistribuição interna do órgão julgador.");
        }
        return List.copyOf(new LinkedHashSet<>(gatilhos));
    }
}
