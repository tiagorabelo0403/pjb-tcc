package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThreadMemberSettings;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

final class AtendimentoChatSupportUtils {

    private static final ZoneId ZONE_NOTIFY = ZoneId.of("America/Fortaleza");

    private AtendimentoChatSupportUtils() {
    }

    static Pageable normalizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 50);
        int page = Math.max(pageable.getPageNumber(), 0);
        return PageRequest.of(page, size, pageable.getSort());
    }

    static Usuario otherParty(Usuario actor, AtendimentoThread thread, Map<Long, Usuario> users) {
        if (actor == null || thread == null || users == null) {
            return null;
        }
        if (actor.getTipoUsuario() == TipoUsuario.CIDADAO) {
            return users.get(thread.getAdvogadoId());
        }
        if (actor.getTipoUsuario() == TipoUsuario.ADVOGADO) {
            return users.get(thread.getCidadaoUsuarioId());
        }
        return null;
    }

    static Long otherUserId(Usuario actor, AtendimentoThread thread) {
        if (actor == null || thread == null) {
            return null;
        }
        if (actor.getTipoUsuario() == TipoUsuario.CIDADAO) {
            return thread.getAdvogadoId();
        }
        if (actor.getTipoUsuario() == TipoUsuario.ADVOGADO) {
            return thread.getCidadaoUsuarioId();
        }
        return null;
    }

    static boolean isMutedNow(AtendimentoThreadMemberSettings settings, Instant at) {
        if (settings == null || at == null) {
            return false;
        }
        Instant mutedUntil = settings.getMutedUntil();
        if (mutedUntil != null && at.isBefore(mutedUntil)) {
            return true;
        }
        return isQuietHoursActive(settings, at);
    }

    static boolean isQuietHoursActive(AtendimentoThreadMemberSettings settings, Instant at) {
        if (settings == null || at == null) {
            return false;
        }
        Short start = settings.getQuietHoursStartMin();
        Short end = settings.getQuietHoursEndMin();
        Integer mask = settings.getQuietDaysMask();
        if (start == null || end == null || mask == null) {
            return false;
        }

        ZonedDateTime zonedDateTime = at.atZone(ZONE_NOTIFY);
        int dowBit = dayMaskBit(zonedDateTime.getDayOfWeek());
        if ((mask & dowBit) == 0) {
            return false;
        }

        int minutes = zonedDateTime.getHour() * 60 + zonedDateTime.getMinute();
        int startMinutes = start;
        int endMinutes = end;
        if (startMinutes == endMinutes) {
            return true;
        }
        if (startMinutes < endMinutes) {
            return minutes >= startMinutes && minutes < endMinutes;
        }
        return minutes >= startMinutes || minutes < endMinutes;
    }

    static int dayMaskBit(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SUNDAY -> 1;
            case MONDAY -> 2;
            case TUESDAY -> 4;
            case WEDNESDAY -> 8;
            case THURSDAY -> 16;
            case FRIDAY -> 32;
            case SATURDAY -> 64;
        };
    }

    static String safeTitle(Processo processo) {
        String classe = firstNonBlank(processo.getClasseProcessual(), "Processo");
        String assunto = firstNonBlank(processo.getAssunto(), processo.getObjetoProcessual(), processo.getPedidoPrincipal(), "Sem descrição");
        String title = (classe + " - " + assunto).trim();
        if (title.length() > 120) {
            return title.substring(0, 120);
        }
        return title;
    }

    static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return null;
    }

    static String normalizeBody(String body) {
        if (body == null) {
            return null;
        }
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 4000) {
            return trimmed.substring(0, 4000);
        }
        return trimmed;
    }

    static String digitsOnly(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digits = cpf.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    static String cpfHash(String cpf) {
        String digits = digitsOnly(cpf);
        return Hashes.sha256Hex(digits == null ? "" : digits);
    }

    static String topicForUser(Long usuarioId) {
        return "ATEND:USR:" + usuarioId;
    }

    static <T> List<T> reverse(List<T> input) {
        List<T> output = new ArrayList<>(input);
        java.util.Collections.reverse(output);
        return output;
    }

    static int clampToInt(long value) {
        if (value <= 0) {
            return 0;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }

    static String computeMsgHash(String prevHash,
                                 Long threadId,
                                 Long senderId,
                                 String senderTipo,
                                 Instant at,
                                 String body,
                                 Long replyToMessageId,
                                 List<Long> attachmentIds) {
        String previous = prevHash != null ? prevHash : "";
        String normalizedBody = body != null ? body : "";
        String reply = replyToMessageId != null ? String.valueOf(replyToMessageId) : "";
        String attachments = attachmentIds == null
                ? ""
                : attachmentIds.stream()
                .filter(Objects::nonNull)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String raw = previous + "|" + threadId + "|" + senderId + "|" + senderTipo + "|" + at.toEpochMilli() + "|" + normalizedBody + "|" + reply + "|" + attachments;
        return Hashes.sha256Hex(raw);
    }
}
