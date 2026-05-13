package com.tcc.pjb.backend.service.recursal.routing;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public record WorkItemSpec(
        String queueCode,
        String inboxKey,
        String title,
        String description,
        LocalDate dueDate,
        TipoUsuario assignedRole,
        Integer priority,
        boolean blocking
) {

    public WorkItemSpec {
        queueCode = normalize(queueCode);
        inboxKey = normalize(inboxKey);
        title = normalizeTitle(title);
        description = normalizeDescription(description);
        priority = normalizePriority(priority, queueCode, title, assignedRole, blocking);
    }

    public String normalizedQueueCode() {
        return queueCode == null ? null : queueCode.toUpperCase(Locale.ROOT);
    }

    public String normalizedInboxKey() {
        return inboxKey == null ? null : inboxKey.toUpperCase(Locale.ROOT);
    }

    public String instanceToken() {
        String[] parts = queueParts();
        return parts.length > 1 ? parts[1] : null;
    }

    public String courtToken() {
        String[] parts = queueParts();
        return parts.length > 2 ? parts[2] : null;
    }

    public String laneToken() {
        String[] parts = queueParts();
        return parts.length > 3 ? parts[3] : null;
    }

    public String suffixToken() {
        String[] parts = queueParts();
        return parts.length > 4 ? parts[4] : null;
    }

    public boolean isUrgent() {
        String t = fingerprintBase();
        return t.contains("URGENT") || t.contains("URGENTE") || t.contains("LIMINAR") || t.contains("PLANTAO");
    }

    public boolean isCabinetTarget() {
        String lane = Objects.requireNonNullElse(laneToken(), "");
        return lane.contains("GAB") || lane.contains("CAMARA") || lane.contains("CONSELHO");
    }

    public boolean isTriagingTarget() {
        String lane = Objects.requireNonNullElse(laneToken(), "");
        return lane.contains("TRIAGEM") || lane.contains("DISTRIB") || lane.contains("MESA");
    }

    public boolean isMagistrateTarget() {
        return assignedRole != null && assignedRole.isMagistratura();
    }

    public boolean isSecretariatTarget() {
        return assignedRole != null && assignedRole.isServidorJudiciario();
    }

    public boolean isUpperCourtTarget() {
        String instance = Objects.requireNonNullElse(instanceToken(), "");
        return "STJ".equals(instance) || "STF".equals(instance);
    }

    public int resolvedPriority() {
        return priority != null ? priority : 3;
    }

    public String targetDescriptor() {
        StringJoiner joiner = new StringJoiner(" • ");
        if (instanceToken() != null) {
            joiner.add(instanceToken());
        }
        if (courtToken() != null) {
            joiner.add(courtToken());
        }
        if (laneToken() != null) {
            joiner.add(laneToken());
        }
        if (assignedRole != null) {
            joiner.add(assignedRole.name());
        }
        String built = joiner.toString();
        return built.isBlank() ? "REC" : built;
    }

    public String routingFingerprint() {
        return normalized(normalizedQueueCode()) + "|" + normalized(title) + "|" + normalized(targetDescriptor());
    }

    private String[] queueParts() {
        String q = normalizedQueueCode();
        return q == null ? new String[0] : q.split(":");
    }

    private String fingerprintBase() {
        return normalized(normalizedQueueCode()) + " " + normalized(title) + " " + normalized(description);
    }

    private static String normalizeTitle(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException("title é obrigatório");
        }
        return normalized;
    }

    private static String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n').trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static Integer normalizePriority(Integer rawPriority,
                                             String queueCode,
                                             String title,
                                             TipoUsuario role,
                                             boolean blocking) {
        if (rawPriority != null) {
            return clampPriority(rawPriority);
        }
        String base = normalized(queueCode) + " " + normalized(title);
        if (base.contains("URGENTE") || base.contains("URGENT") || base.contains("LIMINAR") || base.contains("PLANTAO")) {
            return 1;
        }
        if (blocking) {
            return 2;
        }
        if (role != null && role.isMagistratura()) {
            return 2;
        }
        if (base.contains("GAB") || base.contains("TRIAGEM") || base.contains("DISTRIB")) {
            return 2;
        }
        return 3;
    }

    private static Integer clampPriority(Integer value) {
        return Math.max(1, Math.min(5, value));
    }

    private static String normalize(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        return v.isBlank() ? null : v;
    }

    private static String normalized(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}
