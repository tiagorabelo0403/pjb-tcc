package com.tcc.pjb.backend.service.audiencia.digital;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PjbDigitalHearingOrchestrator {

    public PjbDigitalHearingPlan plan(PjbDigitalHearingInput input) {
        LinkedHashSet<String> missing = new LinkedHashSet<>();
        LinkedHashSet<String> steps = new LinkedHashSet<>();
        Set<String> profiles = input == null || input.requiredProfiles() == null ? Set.of() : input.requiredProfiles();
        if (input == null || Objects.toString(input.processNumber(), "").isBlank()) {
            missing.add("PROCESSO_IDENTIFICADO");
        }
        if (input == null || input.scheduledAt() == null || input.scheduledAt().isBefore(Instant.now())) {
            missing.add("AGENDA_FUTURA_VALIDA");
        }
        if (profiles.isEmpty()) {
            missing.add("PARTICIPANTES_COM_PERFIL");
        }
        if (input == null || !input.videoRoomProvisioned()) {
            missing.add("SALA_DIGITAL_SEGURA");
            steps.add("provisionar sala com link escopado por perfil");
        }
        if (input == null || !input.recordingProvisioned()) {
            missing.add("GRAVACAO_AUDITAVEL");
            steps.add("habilitar gravação com cadeia de custódia");
        }
        if (input == null || !input.transcriptionProvisioned()) {
            steps.add("preparar transcrição assistida revisável");
        }
        if (input != null && input.accessibilityRequested() && !input.accessibilityProvisioned()) {
            missing.add("ACESSIBILIDADE_DA_AUDIENCIA");
            steps.add("habilitar recurso de acessibilidade solicitado");
        }
        if (input != null && input.identityCheckRequired() && !input.identityCheckProvisioned()) {
            missing.add("VALIDACAO_IDENTIDADE");
            steps.add("validar identidade dos participantes antes da abertura");
        }
        boolean humanReview = !missing.isEmpty() || profiles.stream().anyMatch(PjbDigitalHearingOrchestrator::sensitiveProfile);
        PjbDigitalHearingStatus status = status(missing, humanReview, input);
        if (steps.isEmpty()) {
            steps.add("emitir convites, intimações e termo de comparecimento digital");
        }
        return new PjbDigitalHearingPlan(status, missing.isEmpty(), humanReview, List.copyOf(missing), new ArrayList<>(steps));
    }

    private PjbDigitalHearingStatus status(Set<String> missing, boolean humanReview, PjbDigitalHearingInput input) {
        if (missing.contains("PROCESSO_IDENTIFICADO") || missing.contains("AGENDA_FUTURA_VALIDA")) {
            return PjbDigitalHearingStatus.BLOCKED;
        }
        if (missing.contains("ACESSIBILIDADE_DA_AUDIENCIA")) {
            return PjbDigitalHearingStatus.WAITING_ACCESSIBILITY;
        }
        if (missing.contains("GRAVACAO_AUDITAVEL")) {
            return PjbDigitalHearingStatus.WAITING_RECORDING;
        }
        if (input != null && (input.requiredProfiles() == null || input.requiredProfiles().isEmpty())) {
            return PjbDigitalHearingStatus.WAITING_ATTENDEES;
        }
        return humanReview ? PjbDigitalHearingStatus.HUMAN_REVIEW_REQUIRED : PjbDigitalHearingStatus.READY;
    }

    private static boolean sensitiveProfile(String profile) {
        String token = Objects.toString(profile, "").toUpperCase();
        return token.contains("INCAPAZ") || token.contains("CRIANCA") || token.contains("ADOLESCENTE") || token.contains("SIGILO");
    }
}
