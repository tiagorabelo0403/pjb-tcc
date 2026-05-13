package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceEventDto;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CalendarEventAttentionPolicyService {

    public AttentionDescriptor describe(CalendarWorkspaceEventDto event, LocalDateTime now) {
        if (event == null) {
            return new AttentionDescriptor("EVENTO", "Evento", "ATO", "Ato processual", "CALENDAR", "NORMAL", "Normal", "FUTURO", "Janela futura", 10, "BLUE");
        }
        LocalDateTime referenceNow = now == null ? LocalDateTime.now() : now;
        String eventType = normalize(event.eventType());
        String laneCode = normalize(event.laneCode());
        String segmentCode = normalize(event.segmentCode());
        String presentationCode;
        String presentationTitle;
        String detailCode;
        String detailTitle;
        String iconCode;
        if (containsAny(eventType, "MANDADO_MULTI_TENTATIVA", "MANDADO_TENTATIVA")) {
            presentationCode = "MANDADO_TENTATIVA";
            presentationTitle = "Tentativa de diligência";
            detailCode = "TENTATIVAS";
            detailTitle = "Tentativas de diligência";
            iconCode = "ROUTE";
        } else if (containsAny(eventType, "MANDADO_JANELA_RETORNO", "MANDADO_RETORNO")) {
            presentationCode = "MANDADO_RETORNO";
            presentationTitle = "Retorno do mandado";
            detailCode = "RETORNO";
            detailTitle = "Retorno e reexpedição";
            iconCode = "RETURN";
        } else if (containsAny(eventType, "MANDADO_CERTIDAO")) {
            presentationCode = "MANDADO_CERTIDAO";
            presentationTitle = "Certidão de mandado";
            detailCode = "CERTIDAO";
            detailTitle = "Certidões";
            iconCode = "FILE";
        } else if (containsAny(eventType, "MANDADO_ROTA")) {
            presentationCode = "MANDADO_ROTA";
            presentationTitle = "Rota do mandado";
            detailCode = "ROTA";
            detailTitle = "Rota e deslocamento";
            iconCode = "MAP";
        } else if (containsAny(eventType, "SECRETARIA_SLA")) {
            presentationCode = "SECRETARIA_SLA";
            presentationTitle = "SLA da secretaria";
            detailCode = "SLA";
            detailTitle = "SLA e expediente";
            iconCode = "CLOCK";
        } else if (containsAny(eventType, "SECRETARIA_PAUTA_INTERNA")) {
            presentationCode = "SECRETARIA_PAUTA";
            presentationTitle = "Pauta interna da secretaria";
            detailCode = "PAUTA_INTERNA";
            detailTitle = "Pauta interna";
            iconCode = "LIST";
        } else if (containsAny(eventType, "SECRETARIA_AUDIENCIA", "SECRETARIA_FILA_AUDIENCIA")) {
            presentationCode = "SECRETARIA_AUDIENCIA";
            presentationTitle = "Fila de audiência";
            detailCode = "AUDIENCIA";
            detailTitle = "Audiência e fila";
            iconCode = "GAVEL";
        } else if (containsAny(eventType, "GABINETE_VOTO")) {
            presentationCode = "GABINETE_VOTO";
            presentationTitle = "Voto pendente";
            detailCode = "VOTO";
            detailTitle = "Votos";
            iconCode = "SCALE";
        } else if (containsAny(eventType, "GABINETE_MINUTA")) {
            presentationCode = "GABINETE_MINUTA";
            presentationTitle = "Minuta de gabinete";
            detailCode = "MINUTA";
            detailTitle = "Minutas";
            iconCode = "PEN";
        } else if (containsAny(eventType, "GABINETE_CONCLUSAO")) {
            presentationCode = "GABINETE_CONCLUSAO";
            presentationTitle = "Conclusão de gabinete";
            detailCode = "CONCLUSAO";
            detailTitle = "Conclusões";
            iconCode = "INBOX";
        } else if (containsAny(eventType, "GABINETE_PAUTA", "PAUTA_COLEGIADA", "PAUTA_SUSTENTACAO")) {
            presentationCode = "GABINETE_PAUTA";
            presentationTitle = "Pauta colegiada";
            detailCode = "PAUTA";
            detailTitle = "Pauta e sessão";
            iconCode = "COURT";
        } else if (containsAny(eventType, "PERICIA_HONORARIOS")) {
            presentationCode = "PERICIA_HONORARIOS";
            presentationTitle = "Honorários periciais";
            detailCode = "HONORARIOS";
            detailTitle = "Honorários";
            iconCode = "MONEY";
        } else if (containsAny(eventType, "PERICIA_LAUDO_PENDENTE")) {
            presentationCode = "PERICIA_LAUDO_PENDENTE";
            presentationTitle = "Laudo pendente";
            detailCode = "LAUDO_PENDENTE";
            detailTitle = "Laudos pendentes";
            iconCode = "ALERT";
        } else if (containsAny(eventType, "PERICIA_ENTREGA_TECNICA", "PERICIA_LAUDO")) {
            presentationCode = "PERICIA_ENTREGA";
            presentationTitle = "Entrega técnica";
            detailCode = "ENTREGA_TECNICA";
            detailTitle = "Entrega e laudo";
            iconCode = "LAB";
        } else if (containsAny(eventType, "PERICIA_ACEITE", "PERICIA_NOMEACAO")) {
            presentationCode = "PERICIA_ACEITE";
            presentationTitle = "Aceite pericial";
            detailCode = "ACEITE";
            detailTitle = "Aceites";
            iconCode = "CHECK";
        } else if (containsAny(eventType, "PRECATORIO_RPV_OPERACIONAL")) {
            presentationCode = "RPV";
            presentationTitle = "RPV";
            detailCode = "RPV";
            detailTitle = "RPV";
            iconCode = "MONEY";
        } else if (containsAny(eventType, "PRECATORIO_LIBERACAO_OPERACIONAL")) {
            presentationCode = "PRECATORIO_LIBERACAO";
            presentationTitle = "Liberação de precatório";
            detailCode = "LIBERACAO";
            detailTitle = "Liberação";
            iconCode = "BANK";
        } else if (containsAny(eventType, "PRECATORIO_OPERACIONAL")) {
            presentationCode = "PRECATORIO";
            presentationTitle = "Marco de precatório";
            detailCode = "PRECATORIO";
            detailTitle = "Precatórios";
            iconCode = "BANK";
        } else if (containsAny(eventType, "PRAZO_EMBARGOS_OPERACIONAL") || containsAny(segmentCode, "EMBARGOS")) {
            presentationCode = "PRAZO_EMBARGOS";
            presentationTitle = "Prazo de embargos";
            detailCode = "EMBARGOS";
            detailTitle = "Embargos";
            iconCode = "CLOCK";
        } else if (containsAny(eventType, "PRAZO_RECURSAL_OPERACIONAL") || containsAny(segmentCode, "RECURSAL")) {
            presentationCode = "PRAZO_RECURSAL";
            presentationTitle = "Prazo recursal";
            detailCode = "RECURSAL";
            detailTitle = "Recursal";
            iconCode = "APPEAL";
        } else if (containsAny(eventType, "AUDIENCIA", "AUDIENCIA_PROCESSUAL", "AUDIENCIA_RECURSO_SECRETARIA", "AUDIENCIA_PRESENCA_SECRETARIA") || containsAny(segmentCode, "AUDIEN")) {
            presentationCode = "AUDIENCIA";
            presentationTitle = "Audiência";
            detailCode = "AUDIENCIAS";
            detailTitle = "Audiências";
            iconCode = "GAVEL";
        } else if (containsAny(laneCode, "PRECATORIOS")) {
            presentationCode = "FINANCEIRO";
            presentationTitle = "Agenda financeira";
            detailCode = "PRECATORIOS";
            detailTitle = "Precatórios e RPV";
            iconCode = "BANK";
        } else if (containsAny(laneCode, "PRAZOS")) {
            presentationCode = "PRAZO";
            presentationTitle = "Prazo processual";
            detailCode = "PRAZO_GERAL";
            detailTitle = "Prazos gerais";
            iconCode = "CLOCK";
        } else if (containsAny(laneCode, "PESSOAL")) {
            presentationCode = "PESSOAL";
            presentationTitle = "Agenda pessoal";
            detailCode = "PESSOAL";
            detailTitle = "Agenda pessoal";
            iconCode = "USER";
        } else {
            presentationCode = "ATO_PROCESSUAL";
            presentationTitle = "Ato processual";
            detailCode = "ATOS";
            detailTitle = "Atos processuais";
            iconCode = "CALENDAR";
        }
        String color = normalizeColor(event.color(), defaultColorForPresentation(presentationCode));
        String windowCode = windowCode(event.at(), referenceNow);
        String windowLabel = windowLabel(windowCode);
        String priorityCode = priorityCode(color, event.at(), referenceNow);
        String priorityLabel = priorityLabel(priorityCode);
        int attentionScore = attentionScore(event, presentationCode, color, event.at(), referenceNow);
        return new AttentionDescriptor(
                presentationCode,
                presentationTitle,
                detailCode,
                detailTitle,
                iconCode,
                priorityCode,
                priorityLabel,
                windowCode,
                windowLabel,
                attentionScore,
                color
        );
    }

    private static int attentionScore(CalendarWorkspaceEventDto event,
                                      String presentationCode,
                                      String color,
                                      LocalDateTime at,
                                      LocalDateTime now) {
        int score = switch (priorityCode(color, at, now)) {
            case "CRITICA" -> 100;
            case "ALTA" -> 80;
            case "MEDIA" -> 55;
            default -> 30;
        };
        if (at != null) {
            long hours = ChronoUnit.HOURS.between(now, at);
            if (hours <= 0) {
                score += 20;
            } else if (hours <= 6) {
                score += 16;
            } else if (hours <= 24) {
                score += 12;
            } else if (hours <= 48) {
                score += 8;
            } else if (hours <= 72) {
                score += 4;
            }
        }
        if (containsAny(presentationCode, "MANDADO_RETORNO", "PERICIA_LAUDO_PENDENTE", "PRAZO_EMBARGOS")) {
            score += 14;
        } else if (containsAny(presentationCode, "GABINETE_VOTO", "PRAZO_RECURSAL", "AUDIENCIA")) {
            score += 12;
        } else if (containsAny(presentationCode, "MANDADO_TENTATIVA", "SECRETARIA_SLA", "PERICIA_HONORARIOS")) {
            score += 8;
        }
        if (event != null && event.processoId() != null) {
            score += 2;
        }
        return Math.min(score, 130);
    }

    private static String defaultColorForPresentation(String presentationCode) {
        return switch (normalize(presentationCode)) {
            case "MANDADO_RETORNO", "PERICIA_LAUDO_PENDENTE", "PRAZO_EMBARGOS", "PRAZO_RECURSAL", "AUDIENCIA" -> "RED";
            case "SECRETARIA_SLA", "MANDADO_TENTATIVA", "GABINETE_PAUTA", "GABINETE_VOTO" -> "AMBER";
            case "PERICIA_ENTREGA", "RPV", "PRECATORIO", "PRECATORIO_LIBERACAO" -> "GREEN";
            case "GABINETE_MINUTA", "GABINETE_CONCLUSAO", "FINANCEIRO" -> "PURPLE";
            default -> "BLUE";
        };
    }

    private static String priorityCode(String color, LocalDateTime at, LocalDateTime now) {
        if (at != null && now != null && at.isBefore(now)) {
            return "CRITICA";
        }
        return switch (severity(color)) {
            case 5 -> "CRITICA";
            case 4 -> "ALTA";
            case 3 -> "MEDIA";
            default -> "NORMAL";
        };
    }

    private static String priorityLabel(String code) {
        return switch (normalize(code)) {
            case "CRITICA" -> "Crítica";
            case "ALTA" -> "Alta";
            case "MEDIA" -> "Média";
            default -> "Normal";
        };
    }

    private static String windowCode(LocalDateTime at, LocalDateTime now) {
        if (at == null) {
            return "FUTURO";
        }
        if (at.isBefore(now)) {
            return "VENCIDO";
        }
        long hours = ChronoUnit.HOURS.between(now, at);
        if (at.toLocalDate().isEqual(now.toLocalDate())) {
            return "HOJE";
        }
        if (hours <= 24) {
            return "ATE_24H";
        }
        if (hours <= 48) {
            return "ATE_48H";
        }
        if (hours <= 24 * 7) {
            return "ATE_7D";
        }
        return "FUTURO";
    }

    private static String windowLabel(String code) {
        return switch (normalize(code)) {
            case "VENCIDO" -> "Janela vencida";
            case "HOJE" -> "Hoje";
            case "ATE_24H" -> "Próximas 24h";
            case "ATE_48H" -> "Próximas 48h";
            case "ATE_7D" -> "Próximos 7 dias";
            default -> "Janela futura";
        };
    }

    private static int severity(String color) {
        return switch (normalize(color)) {
            case "RED", "VERMELHO" -> 5;
            case "AMBER", "LARANJA", "ORANGE" -> 4;
            case "PURPLE", "ROXO" -> 3;
            case "BLUE", "AZUL" -> 2;
            case "GREEN", "VERDE" -> 1;
            default -> 0;
        };
    }

    private static String normalizeColor(String color, String fallback) {
        String normalized = normalize(color);
        return normalized == null ? normalize(fallback) : normalized;
    }

    private static boolean containsAny(String value, String... probes) {
        String normalized = normalize(value);
        if (normalized == null || probes == null) {
            return false;
        }
        for (String probe : probes) {
            String token = normalize(probe);
            if (token != null && normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT)
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
                .replace('Ç', 'C');
    }

    public record AttentionDescriptor(
            String presentationCode,
            String presentationTitle,
            String detailCode,
            String detailTitle,
            String iconCode,
            String priorityCode,
            String priorityLabel,
            String windowCode,
            String windowLabel,
            int attentionScore,
            String color
    ) {
    }
}
