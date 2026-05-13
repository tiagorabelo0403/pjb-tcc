package com.tcc.pjb.backend.ai.juridica.conversation.security;

import com.tcc.pjb.backend.ai.juridica.conversation.ImmutableViewSupport;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSanitizationSnapshot;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalContextSanitizer {

    private static final int MAX_MESSAGE_LENGTH = 4_000;
    private static final int MAX_HISTORY_ITEM_LENGTH = 2_000;
    private static final int MAX_ATTACHMENT_NAME_LENGTH = 180;
    private static final int MAX_CONTEXT_DEPTH = 4;
    private static final int MAX_CONTEXT_ENTRIES = 64;
    private static final List<String> PROMPT_INJECTION_MARKERS = List.of(
            "ignore previous",
            "ignore all previous",
            "ignore system prompt",
            "developer message",
            "system message",
            "jailbreak",
            "override instruction",
            "bypass policy",
            "tool call",
            "function call",
            "act as",
            "modo desenvolvedor",
            "ignore as instruções",
            "desconsidere as instruções",
            "revele o prompt"
    );

    public LegalConversationSanitizationResult sanitize(LegalAiConversationRequest request) {
        if (request == null) {
            return new LegalConversationSanitizationResult(null, emptySnapshot());
        }
        List<String> alerts = new ArrayList<>();
        String message = sanitizeText(request.message(), MAX_MESSAGE_LENGTH, alerts, "message");
        List<String> history = sanitizeList(request.history(), MAX_HISTORY_ITEM_LENGTH, alerts, "history");
        List<String> attachments = sanitizeList(request.attachments(), MAX_ATTACHMENT_NAME_LENGTH, alerts, "attachment");
        Map<String, Object> context = sanitizeMap(request.context(), 0, alerts);
        boolean promptInjectionDetected = containsPromptInjection(message)
                || history.stream().anyMatch(this::containsPromptInjection)
                || containsPromptInjection(context);
        if (promptInjectionDetected) {
            alerts.add("Contexto conversacional com marcadores de prompt injection foi colocado em modo controlado.");
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("messageLength", message == null ? 0 : message.length());
        diagnostics.put("historyCount", history.size());
        diagnostics.put("attachmentCount", attachments.size());
        diagnostics.put("contextKeyCount", context.size());
        diagnostics.put("maxContextDepth", MAX_CONTEXT_DEPTH);
        diagnostics.put("maxContextEntries", MAX_CONTEXT_ENTRIES);
        String status = promptInjectionDetected ? "RESTRICTED" : alerts.isEmpty() ? "SANITIZED" : "SANITIZED_WITH_ALERTS";
        return new LegalConversationSanitizationResult(
                new LegalAiConversationRequest(
                        blankToNull(request.conversationId()),
                        blankToNull(request.processoId()),
                        message,
                        blankToNull(request.userProfile()),
                        history,
                        attachments,
                        context
                ),
                new LegalAiConversationSanitizationSnapshot(
                        status,
                        promptInjectionDetected,
                        List.copyOf(alerts),
                        ImmutableViewSupport.map(diagnostics)
                )
        );
    }

    private LegalAiConversationSanitizationSnapshot emptySnapshot() {
        return new LegalAiConversationSanitizationSnapshot("EMPTY", false, List.of(), Map.of());
    }

    private List<String> sanitizeList(List<String> values, int limit, List<String> alerts, String field) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(value -> sanitizeText(value, limit, alerts, field))
                .filter(Objects::nonNull)
                .distinct()
                .limit(MAX_CONTEXT_ENTRIES)
                .toList();
    }

    private Map<String, Object> sanitizeMap(Map<String, Object> source, int depth, List<String> alerts) {
        if (source == null || source.isEmpty() || depth > MAX_CONTEXT_DEPTH) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (count >= MAX_CONTEXT_ENTRIES) {
                alerts.add("Contexto excedeu a janela máxima e foi truncado de forma controlada.");
                break;
            }
            String key = sanitizeKey(entry.getKey());
            if (key == null) {
                continue;
            }
            Object value = sanitizeValue(entry.getValue(), depth + 1, alerts);
            if (value != null) {
                out.put(key, value);
                count++;
            }
        }
        return ImmutableViewSupport.map(out);
    }

    private Object sanitizeValue(Object value, int depth, List<String> alerts) {
        if (value == null) {
            return null;
        }
        if (depth > MAX_CONTEXT_DEPTH) {
            alerts.add("Contexto excedeu a profundidade máxima e foi truncado de forma controlada.");
            return null;
        }
        if (value instanceof String text) {
            return sanitizeText(text, MAX_HISTORY_ITEM_LENGTH, alerts, "context");
        }
        if (value instanceof Map<?, ?> nested) {
            LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
            nested.forEach((key, item) -> {
                String safeKey = sanitizeKey(key == null ? null : String.valueOf(key));
                Object safeValue = sanitizeValue(item, depth + 1, alerts);
                if (safeKey != null && safeValue != null && normalized.size() < MAX_CONTEXT_ENTRIES) {
                    normalized.put(safeKey, safeValue);
                }
            });
            return ImmutableViewSupport.map(normalized);
        }
        if (value instanceof List<?> list) {
            List<Object> normalized = list.stream()
                    .limit(MAX_CONTEXT_ENTRIES)
                    .map(item -> sanitizeValue(item, depth + 1, alerts))
                    .filter(Objects::nonNull)
                    .toList();
            return List.copyOf(normalized);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return sanitizeText(String.valueOf(value), MAX_HISTORY_ITEM_LENGTH, alerts, "context");
    }

    private String sanitizeText(String raw, int maxLength, List<String> alerts, String field) {
        String normalized = blankToNull(raw);
        if (normalized == null) {
            return null;
        }
        String text = Normalizer.normalize(normalized, Normalizer.Form.NFKC)
                .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ")
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        if (text.length() > maxLength) {
            alerts.add("Campo " + field + " excedeu o limite seguro e foi truncado.");
            text = text.substring(0, maxLength).trim();
        }
        return blankToNull(text);
    }

    private String sanitizeKey(String raw) {
        String key = blankToNull(raw);
        if (key == null) {
            return null;
        }
        String normalized = Normalizer.normalize(key, Normalizer.Form.NFKC)
                .replaceAll("[^a-zA-Z0-9_\\-]", "_")
                .replaceAll("_+", "_")
                .trim();
        return blankToNull(normalized);
    }

    private boolean containsPromptInjection(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return false;
        }
        return context.values().stream().anyMatch(this::containsPromptInjectionValue);
    }

    private boolean containsPromptInjectionValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return containsPromptInjection(text);
        }
        if (value instanceof Map<?, ?> map) {
            return map.values().stream().anyMatch(this::containsPromptInjectionValue);
        }
        if (value instanceof List<?> list) {
            return list.stream().anyMatch(this::containsPromptInjectionValue);
        }
        return containsPromptInjection(String.valueOf(value));
    }

    private boolean containsPromptInjection(String text) {
        String normalized = blankToNull(text);
        if (normalized == null) {
            return false;
        }
        String value = normalized.toLowerCase(Locale.ROOT);
        return PROMPT_INJECTION_MARKERS.stream().anyMatch(value::contains);
    }

    private String blankToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isBlank() ? null : value;
    }

    public record LegalConversationSanitizationResult(
            LegalAiConversationRequest request,
            LegalAiConversationSanitizationSnapshot snapshot
    ) {
    }
}
