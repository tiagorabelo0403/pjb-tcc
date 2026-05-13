package com.tcc.pjb.backend.modules.laiane.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LaianeSubmissionGuardrailService {

    private final ObjectMapper objectMapper;

    public LaianeSubmissionGuardrailService(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public GuardrailSnapshot analyze(Object payload) {
        return analyze(toMap(payload));
    }

    public GuardrailSnapshot analyze(Map<String, Object> payload) {
        Map<String, Object> map = payload == null ? Map.of() : new LinkedHashMap<>(payload);
        String channel = text(map.get("channel"));
        boolean assistedPetition = equalsNormalized(channel, "laiane_peticao_assistida");

        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();

        Boolean prontaParaProtocolo = bool(map.get("prontaParaProtocolo"));
        Double readinessScore = decimal(map.get("readinessScore"));
        if (readinessScore != null) {
            envelope.put("readinessScore", readinessScore);
            if (readinessScore < 0.80d) {
                warnings.add("Readiness abaixo do limiar seguro para submissão automática integral.");
            }
        }

        Map<String, Object> validator = nestedMap(map, "validator");
        if (!validator.isEmpty()) {
            Boolean ok = bool(validator.get("ok"));
            envelope.put("validatorOk", ok);
            if (Boolean.FALSE.equals(ok)) {
                blockers.addAll(stringList(validator.get("errors")));
                if (blockers.isEmpty()) {
                    blockers.add("Validação jurídica da petição retornou inconsistências impeditivas.");
                }
            }
            warnings.addAll(stringList(validator.get("warnings")));
        }

        Map<String, Object> triagem = nestedMap(map, "triagem");
        if (!triagem.isEmpty()) {
            String veredito = normalized(text(triagem.get("veredito")));
            envelope.put("triagemVeredito", veredito);
            if (equalsAny(veredito, "BLOQUEADO", "PENDENTE_CORRECAO")) {
                blockers.add(firstNonBlank(text(triagem.get("resumo")), "Triagem nacional exige correção antes da submissão."));
            } else if (equalsAny(veredito, "REQUER_REVISAO_HUMANA", "APROVADO_COM_RESSALVAS")) {
                warnings.add(firstNonBlank(text(triagem.get("resumo")), "Triagem nacional recomenda revisão humana antes do envio definitivo."));
            }
        }

        Map<String, Object> teto = nestedMap(map, "tetoProcessual");
        if (!teto.isEmpty()) {
            envelope.put("tetoCodigo", text(teto.get("codigoDiagnostico")));
            if (Boolean.TRUE.equals(bool(teto.get("bloqueante")))) {
                blockers.add(firstNonBlank(text(teto.get("fundamentoLegal")), "Erro de teto bloqueante detectado para o valor da causa."));
            } else if (Boolean.TRUE.equals(bool(teto.get("alerta")))) {
                warnings.add(firstNonBlank(text(teto.get("fundamentoLegal")), "Erro de teto em faixa de alerta recomenda revisão."));
            }
        }

        Map<String, Object> territorial = nestedMap(map, "territorialProcessual");
        if (!territorial.isEmpty()) {
            envelope.put("territorialCodigo", text(territorial.get("codigoDiagnostico")));
            envelope.put("territorialMode", text(territorial.get("territorialMode")));
            if (Boolean.TRUE.equals(bool(territorial.get("bloqueante")))) {
                blockers.add(firstNonBlank(text(territorial.get("fundamentoLegal")), "Inconsistência territorial bloqueante detectada pela malha nacional."));
            } else if (Boolean.TRUE.equals(bool(territorial.get("alerta")))) {
                warnings.add(firstNonBlank(text(territorial.get("fundamentoLegal")), "Risco territorial recomenda revisão humana antes do protocolo."));
            }
            warnings.addAll(stringList(territorial.get("alertas")));
            warnings.addAll(stringList(territorial.get("reviewChecklist")));
        }

        Map<String, Object> attachmentValidation = nestedMap(map, "attachmentValidation");
        if (!attachmentValidation.isEmpty()) {
            Boolean ok = bool(attachmentValidation.get("ok"));
            envelope.put("attachmentValidationOk", ok);
            if (Boolean.FALSE.equals(ok)) {
                List<String> missing = stringList(attachmentValidation.get("missing"));
                blockers.addAll(missing);
                if (missing.isEmpty()) {
                    blockers.add("Documentos obrigatórios ainda faltam para o rito indicado.");
                }
            }
        }

        Map<String, Object> materialStrategy = nestedMap(map, "materialStrategy");
        if (!materialStrategy.isEmpty()) {
            List<String> protocolBlockers = stringList(materialStrategy.get("protocolBlockers"));
            blockers.addAll(protocolBlockers);
            warnings.addAll(stringList(materialStrategy.get("executionChecklist")));
        }

        Map<String, Object> submissionBlueprint = nestedMap(map, "submissionBlueprint");
        if (!submissionBlueprint.isEmpty() && Boolean.FALSE.equals(bool(submissionBlueprint.get("preProtocoloApto")))) {
            warnings.add("Blueprint procedimental ainda não está apto para submissão automática integral.");
        }

        Map<String, Object> connectorExecution = nestedMap(map, "connectorExecution");
        if (!connectorExecution.isEmpty() && Boolean.FALSE.equals(bool(connectorExecution.get("connectorReady")))) {
            warnings.add("Conector judicial ainda requer saneamento operacional antes do envio definitivo.");
        }

        boolean blocking = !blockers.isEmpty();
        boolean ready = !blocking && (!assistedPetition || Boolean.TRUE.equals(prontaParaProtocolo));
        String status = blocking ? "BLOCKED" : ready ? "READY" : !warnings.isEmpty() ? "REVIEW" : "PENDING";
        String nextAction = resolveNextAction(blockers, warnings, assistedPetition, prontaParaProtocolo);

        envelope.put("channel", channel);
        envelope.put("assistedPetition", assistedPetition);
        envelope.put("readyForSubmission", ready);
        envelope.put("blockingCount", blockers.size());
        envelope.put("warningCount", warnings.size());
        envelope.put("prontaParaProtocolo", prontaParaProtocolo);

        return new GuardrailSnapshot(
                status,
                blocking,
                !warnings.isEmpty(),
                ready,
                nextAction,
                List.copyOf(blockers),
                List.copyOf(warnings),
                envelope
        );
    }

    public Map<String, Object> enrichPayload(Map<String, Object> payload) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        GuardrailSnapshot snapshot = analyze(out);
        out.put("submissionGuardrails", snapshot.toMap());
        out.put("readyForSubmissionGuardrail", snapshot.readyForSubmission());
        out.put("guardrailStatus", snapshot.status());
        out.put("guardrailNextAction", snapshot.nextAction());
        return out;
    }

    public Map<String, Object> toMap(Object payload) {
        if (payload == null) {
            return new LinkedHashMap<>();
        }
        if (payload instanceof Map<?, ?> map) {
            return objectMapper.convertValue(map, new TypeReference<LinkedHashMap<String, Object>>() {});
        }
        if (payload instanceof String text && !text.isBlank()) {
            try {
                return objectMapper.readValue(text, new TypeReference<LinkedHashMap<String, Object>>() {});
            } catch (Exception ignored) {
                return new LinkedHashMap<>(Map.of("texto", text));
            }
        }
        return objectMapper.convertValue(payload, new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    private static String resolveNextAction(LinkedHashSet<String> blockers,
                                            LinkedHashSet<String> warnings,
                                            boolean assistedPetition,
                                            Boolean prontaParaProtocolo) {
        if (!blockers.isEmpty()) {
            return "SANEAR_BLOQUEIOS_DE_PROTOCOLO";
        }
        if (assistedPetition && !Boolean.TRUE.equals(prontaParaProtocolo)) {
            return "REVISAR_PETICAO_ASSISTIDA";
        }
        if (!warnings.isEmpty()) {
            return "REALIZAR_REVISAO_FINAL_E_VALIDAR_PROTOCOLO";
        }
        return "SUBMETER_PROTOCOLO";
    }

    private static Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        if (source == null || key == null || key.isBlank()) {
            return Map.of();
        }
        Object value = source.get(key);
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                if (k != null) {
                    out.put(String.valueOf(k), v);
                }
            });
            return out;
        }
        return Map.of();
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (Object item : list) {
            String text = text(item);
            if (text != null) {
                out.add(text);
            }
        }
        return List.copyOf(out);
    }

    private static Boolean bool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        String text = text(value);
        if (text == null) {
            return null;
        }
        if (equalsAny(text, "TRUE", "SIM", "1", "YES")) {
            return true;
        }
        if (equalsAny(text, "FALSE", "NAO", "NÃO", "0", "NO")) {
            return false;
        }
        return null;
    }

    private static Double decimal(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        String text = text(value);
        if (text == null) {
            return null;
        }
        try {
            return Double.parseDouble(text.replace(',', '.'));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static boolean equalsAny(String value, String... candidates) {
        String normalizedValue = normalized(value);
        if (normalizedValue == null || candidates == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (normalizedValue.equals(normalized(candidate))) {
                return true;
            }
        }
        return false;
    }

    private static boolean equalsNormalized(String left, String right) {
        return Objects.equals(normalized(left), normalized(right));
    }

    private static String normalized(String value) {
        String text = text(value);
        return text == null ? null : text.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String text = text(value);
            if (text != null) {
                return text;
            }
        }
        return null;
    }


    private static Map<String, Object> sanitizeEnvelope(Map<String, Object> envelope) {
        if (envelope == null || envelope.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        envelope.forEach((key, value) -> {
            String normalizedKey = text(key);
            if (normalizedKey != null && value != null) {
                out.put(normalizedKey, value);
            }
        });
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    public record GuardrailSnapshot(
            String status,
            boolean blocking,
            boolean warning,
            boolean readyForSubmission,
            String nextAction,
            List<String> blockers,
            List<String> warnings,
            Map<String, Object> envelope
    ) {
        public GuardrailSnapshot {
            blockers = blockers == null ? List.of() : List.copyOf(new LinkedHashSet<>(blockers));
            warnings = warnings == null ? List.of() : List.copyOf(new LinkedHashSet<>(warnings));
            envelope = sanitizeEnvelope(envelope);
        }

        public String summary() {
            ArrayList<String> parts = new ArrayList<>();
            if (!blockers.isEmpty()) {
                parts.add(String.join(" | ", blockers.stream().limit(4).toList()));
            }
            if (parts.isEmpty() && !warnings.isEmpty()) {
                parts.add(String.join(" | ", warnings.stream().limit(4).toList()));
            }
            if (parts.isEmpty()) {
                parts.add("Guard rails de submissão preservados.");
            }
            String summary = String.join(" | ", parts);
            return summary.length() > 950 ? summary.substring(0, 950) : summary;
        }

        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("status", status);
            map.put("blocking", blocking);
            map.put("warning", warning);
            map.put("readyForSubmission", readyForSubmission);
            map.put("nextAction", nextAction);
            map.put("blockers", blockers);
            map.put("warnings", warnings);
            map.put("envelope", envelope);
            return map;
        }
    }
}
