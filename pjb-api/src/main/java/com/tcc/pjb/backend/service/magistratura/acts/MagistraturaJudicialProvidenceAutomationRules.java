package com.tcc.pjb.backend.service.magistratura.acts;

import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialProvidenceCode;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class MagistraturaJudicialProvidenceAutomationRules {

    private MagistraturaJudicialProvidenceAutomationRules() {
    }

    static WorkItemType resolveWorkItemType(MagistraturaJudicialProvidenceCode code) {
        return switch (code) {
            case PREPARAR_AUDIENCIA -> WorkItemType.AUDIENCIA;
            case EXPEDIR_INTIMACOES -> WorkItemType.INTIMACAO;
            case PROVIDENCIAR_PUBLICACAO -> WorkItemType.EXPEDICAO;
            case CUMPRIR_DETERMINACAO_CARTORIO, SANEAR_PROCESSO, PROCESSAR_INCIDENTE_PROCESSUAL -> WorkItemType.OUTRO;
            case PROVIDENCIAR_PERICIA -> WorkItemType.PERICIA;
            case EXPEDIR_ORDEM_CUMPRIMENTO -> WorkItemType.DILIGENCIA;
            case ORGANIZAR_CONCLUSAO, REDISTRIBUIR_OU_PREVENIR -> WorkItemType.DISTRIBUICAO;
            case REMETER_COLEGIADO_OU_PLENARIO, ABRIR_VISTA_TECNICA -> WorkItemType.VISTA;
            case CONTROLAR_CALCULO_LIQUIDACAO -> WorkItemType.CALCULO;
            case IMPULSIONAR_EXECUCAO -> WorkItemType.CUMPRIMENTO_SENTENCA;
        };
    }

    static int resolvePriority(MagistraturaJudicialProvidenceCode code, MagistraturaJudicialActCommandRequest request) {
        int base = switch (code) {
            case PREPARAR_AUDIENCIA, EXPEDIR_ORDEM_CUMPRIMENTO, REDISTRIBUIR_OU_PREVENIR -> 1;
            case PROVIDENCIAR_PUBLICACAO, EXPEDIR_INTIMACOES, PROVIDENCIAR_PERICIA, ABRIR_VISTA_TECNICA, IMPULSIONAR_EXECUCAO, REMETER_COLEGIADO_OU_PLENARIO -> 2;
            default -> 3;
        };
        String corpus = normalize(join(request == null ? null : request.conteudo(), request == null ? null : request.observacao(), request == null ? null : request.fundamentacao()));
        if (containsAny(corpus, "urgencia", "urgente", "liminar", "tutela de urgencia", "plantao", "plantão")) {
            return Math.max(1, base - 1);
        }
        return base;
    }

    static boolean resolveBlocking(MagistraturaJudicialProvidenceCode code, Processo processo) {
        return switch (code) {
            case PREPARAR_AUDIENCIA, EXPEDIR_ORDEM_CUMPRIMENTO, IMPULSIONAR_EXECUCAO, REDISTRIBUIR_OU_PREVENIR -> true;
            default -> processo.getNivelSigilo() != null && !"PUBLICO".equalsIgnoreCase(processo.getNivelSigilo().name());
        };
    }

    static String label(MagistraturaJudicialProvidenceCode code) {
        return switch (code) {
            case PREPARAR_AUDIENCIA -> "Preparar audiência";
            case EXPEDIR_INTIMACOES -> "Intimar, citar ou abrir vista comunicável";
            case PROVIDENCIAR_PUBLICACAO -> "Providenciar publicação";
            case CUMPRIR_DETERMINACAO_CARTORIO -> "Cumprir determinação cartorária";
            case PROVIDENCIAR_PERICIA -> "Providenciar perícia";
            case EXPEDIR_ORDEM_CUMPRIMENTO -> "Expedir ordem, mandado, ofício ou cumprimento urgente";
            case ORGANIZAR_CONCLUSAO -> "Organizar conclusão automática";
            case REMETER_COLEGIADO_OU_PLENARIO -> "Processar fluxo recursal, pauta, sessão ou acórdão";
            case ABRIR_VISTA_TECNICA -> "Abrir vista para órgão obrigatório ou unidade técnica";
            case CONTROLAR_CALCULO_LIQUIDACAO -> "Controlar liquidação, cálculos e contadoria";
            case IMPULSIONAR_EXECUCAO -> "Impulsionar execução e cumprimento de sentença";
            case SANEAR_PROCESSO -> "Sanear pendências processuais";
            case PROCESSAR_INCIDENTE_PROCESSUAL -> "Processar incidente processual";
            case REDISTRIBUIR_OU_PREVENIR -> "Redistribuir ou aplicar prevenção";
        };
    }

    static String summarizeReasons(List<String> reasons) {
        return reasons == null || reasons.isEmpty() ? null : String.join(" | ", reasons);
    }

    static String mergeDescription(String left, String right) {
        if (left == null || left.isBlank()) {
            return right;
        }
        if (right == null || right.isBlank()) {
            return left;
        }
        return left.contains(right) ? left : left + " | " + right;
    }

    static String resolveAgendaTrack(MagistraturaJudicialProvidenceCode code) {
        return switch (code) {
            case PREPARAR_AUDIENCIA -> "AUDIENCIA_PROCESSUAL";
            case REMETER_COLEGIADO_OU_PLENARIO -> "SESSAO_COLEGIADA";
            case EXPEDIR_INTIMACOES, PROVIDENCIAR_PUBLICACAO, EXPEDIR_ORDEM_CUMPRIMENTO -> "COMUNICACAO_PROCESSUAL";
            case PROVIDENCIAR_PERICIA -> "PERICIA_TECNICA";
            case IMPULSIONAR_EXECUCAO -> "CUMPRIMENTO_EXECUCAO";
            case SANEAR_PROCESSO -> "SANEAMENTO_PROCESSUAL";
            default -> "OPERACIONAL_GERAL";
        };
    }

    static String resolveConfirmationStatus(MagistraturaJudicialProvidenceCode code) {
        return switch (code) {
            case PREPARAR_AUDIENCIA, REMETER_COLEGIADO_OU_PLENARIO,
                    EXPEDIR_INTIMACOES, PROVIDENCIAR_PUBLICACAO, EXPEDIR_ORDEM_CUMPRIMENTO -> "PENDENTE_CONFIRMACAO";
            default -> "NAO_APLICAVEL";
        };
    }

    static String resolveAttendanceStatus(MagistraturaJudicialProvidenceCode code) {
        return switch (code) {
            case PREPARAR_AUDIENCIA, REMETER_COLEGIADO_OU_PLENARIO -> "AGUARDANDO_REALIZACAO";
            default -> "NAO_APLICAVEL";
        };
    }

    static Duration optionalDuration(Duration preferred, Duration fallback) {
        return preferred == null || preferred.isNegative() || preferred.isZero() ? fallback : preferred;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> tribunalFlow(SecretariatOperationalRoutingProfile profile) {
        if (profile == null || profile.metadata() == null) {
            return Map.of();
        }
        Object raw = profile.metadata().get("tribunalFlow");
        return raw instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        if (source == null || key == null || key.isBlank()) {
            return Map.of();
        }
        Object raw = source.get(key);
        return raw instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    static List<String> immutableList(String... values) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    out.add(value);
                }
            }
        }
        return List.copyOf(out);
    }

    static String join(String... values) {
        StringBuilder builder = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    if (builder.length() > 0) {
                        builder.append(' ');
                    }
                    builder.append(value.trim());
                }
            }
        }
        return builder.toString();
    }

    static String normalize(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return normalized
                .replace('á', 'a')
                .replace('à', 'a')
                .replace('â', 'a')
                .replace('ã', 'a')
                .replace('é', 'e')
                .replace('ê', 'e')
                .replace('í', 'i')
                .replace('ó', 'o')
                .replace('ô', 'o')
                .replace('õ', 'o')
                .replace('ú', 'u')
                .replace('ç', 'c');
    }

    static String normalizeSpaces(String value) {
        return value == null ? null : value.replaceAll("\\s+", " ").trim();
    }

    static boolean containsAny(String corpus, String... needles) {
        if (corpus == null || corpus.isBlank() || needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && corpus.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    static String firstNonBlank(String... values) {
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

    static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static String deterministicKey(MagistraturaJudicialActCode action,
                                   MagistraturaJudicialProvidenceCode providence,
                                   Long processoId,
                                   Long usuarioId,
                                   Instant dueAt) {
        String raw = action.name() + ':' + providence.name() + ':' + processoId + ':' + usuarioId + ':' + (dueAt == null ? "SEM_PRAZO" : dueAt.toString());
        return "MAGISTRATURA:PROVIDENCIA:" + UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }

    static Map<String, Object> safeMap(Map<String, ?> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((key, value) -> {
                if (key != null && value != null) {
                    out.put(key, value);
                }
            });
        }
        return Collections.unmodifiableMap(out);
    }
}
