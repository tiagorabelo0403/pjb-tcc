package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.ai.juridica.symbolic.JuridicaSymbolicValidationExecutionService;
import com.tcc.pjb.backend.ai.juridica.symbolic.LegalSymbolicValidationContext;
import com.tcc.pjb.backend.ai.juridica.symbolic.LegalSymbolicValidationExecution;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JuridicaValidationEnvelopeService {

    private final JuridicaLegalAiSpineService juridicaLegalAiSpineService;
    private final JuridicaSymbolicValidationExecutionService juridicaSymbolicValidationExecutionService;

    public JuridicaValidationEnvelopeService(JuridicaLegalAiSpineService juridicaLegalAiSpineService,
                                             JuridicaSymbolicValidationExecutionService juridicaSymbolicValidationExecutionService) {
        this.juridicaLegalAiSpineService = Objects.requireNonNull(juridicaLegalAiSpineService, "juridicaLegalAiSpineService");
        this.juridicaSymbolicValidationExecutionService = Objects.requireNonNull(juridicaSymbolicValidationExecutionService, "juridicaSymbolicValidationExecutionService");
    }

    public LegalValidationResponse validate(LegalValidationRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (request != null) {
            payload.put("ramo", request.ramo());
            payload.put("rito", request.rito());
            payload.put("classe", request.classe());
            payload.put("sigilo", request.sigilo());
            payload.put("objetivo", request.objetivo());
            if (request.filtros() != null) {
                payload.putAll(request.filtros());
            }
        }

        var spine = juridicaLegalAiSpineService.resolveForSkill(
                JuridicaSpineLabels.CAPABILITY_VALIDATE_ENVELOPE,
                ApiVersion.V3,
                payload
        );

        String text = request == null || request.texto() == null ? "" : request.texto().trim();
        LegalSymbolicValidationExecution symbolicExecution = juridicaSymbolicValidationExecutionService.execute(
                LegalSymbolicValidationContext.from(request),
                spine.validation().symbolicEngines()
        );
        List<String> missingEvidence = new ArrayList<>(symbolicExecution.missingEvidence());
        List<String> contradictions = new ArrayList<>(symbolicExecution.contradictions());

        if (text.isBlank()) {
            addDistinct(missingEvidence, "Texto base ausente.");
        }
        if (!containsCitation(text)) {
            addDistinct(missingEvidence, "Fundamentacao normativa ou jurisprudencial nao identificada.");
        }
        if ((containsArticleReference(text) || containsPrecedentReference(text)) && !containsCitationEvidence(text)) {
            addDistinct(contradictions, "Citacao normativa ou jurisprudencial sem marcador minimo de confirmacao/grounding.");
        }
        if (request != null && value(request.ramo()).equalsIgnoreCase("penal") && !containsAny(text, List.of("denuncia", "acusacao", "reus", "materialidade"))) {
            addDistinct(contradictions, "Texto penal sem marcadores minimos de acusacao/materialidade.");
        }
        if (request != null && value(request.rito()).toLowerCase(Locale.ROOT).contains("juizado") && containsAny(text, List.of("agravo de instrumento", "recurso especial", "recurso extraordinario"))) {
            addDistinct(contradictions, "Rito de juizado com indicio de peca incompatível de instância superior.");
        }

        String status = contradictions.isEmpty() && missingEvidence.isEmpty() ? "VALIDATED" : contradictions.isEmpty() ? "NEEDS_COMPLEMENT" : "BLOCKED";

        LinkedHashMap<String, Object> trace = new LinkedHashMap<>();
        trace.put("lane", spine.trace().lane());
        trace.put("auditFields", spine.trace().requiredAuditFields());
        trace.put("gates", validationGates(spine));
        trace.put("confidenceMode", status);
        trace.put("citationEmissionMode", spine.hallucinationGuard().citationEmissionMode());
        trace.put("unresolvedCitationPlaceholder", spine.hallucinationGuard().unresolvedCitationPlaceholder());
        trace.put("symbolicExecutionStatus", symbolicExecution.status());
        trace.put("symbolicExecution", symbolicExecution.diagnostics());
        trace.put("symbolicOutcomes", symbolicExecution.outcomes().stream().map(outcome -> Map.of(
                "engineCode", outcome.engineCode(),
                "verdict", outcome.verdict(),
                "issueCount", outcome.issues().size(),
                "diagnostics", outcome.diagnostics()
        )).toList());

        return new LegalValidationResponse(
                spine.profileCode(),
                spine.version(),
                spine.capability(),
                status,
                spine.validation().citationGroundingRequired(),
                approvalMode(spine),
                spine.validation().symbolicEngines(),
                spine.evaluation().evalSuites(),
                List.copyOf(missingEvidence),
                List.copyOf(contradictions),
                spine.structuredOutputs().isEmpty() ? null : spine.structuredOutputs().getFirst().schemaId(),
                Map.copyOf(trace)
        );
    }

    private String approvalMode(com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiSpineProfileResponse spine) {
        if (spine.approval().approvalRequired()) {
            return spine.approval().stepUpRequired() ? JuridicaSpineLabels.APPROVAL_STEP_UP : JuridicaSpineLabels.APPROVAL_HUMAN;
        }
        return JuridicaSpineLabels.APPROVAL_NONE;
    }

    private List<String> validationGates(com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiSpineProfileResponse spine) {
        Object gates = spine.validation().validationPolicy().get("gates");
        if (gates instanceof Iterable<?> iterable) {
            return java.util.stream.StreamSupport.stream(iterable.spliterator(), false)
                    .map(item -> item == null ? null : String.valueOf(item))
                    .filter(item -> item != null && !item.isBlank())
                    .toList();
        }
        return List.of();
    }

    private boolean containsCitation(String text) {
        return containsAny(text, List.of("art.", "artigo", "stj", "stf", "tst", "tema", "sumula", "súmula", "precedente"));
    }

    private boolean containsArticleReference(String text) {
        return containsAny(text, List.of("art.", "artigo"));
    }

    private boolean containsPrecedentReference(String text) {
        return containsAny(text, List.of("stj", "stf", "tst", "tema", "sumula", "súmula", "precedente", "resp", "re ", "are"));
    }

    private boolean containsCitationEvidence(String text) {
        return containsAny(text, List.of("fundamento confirmado", "precedente confirmado", JuridicaSpineLabels.UNRESOLVED_CITATION_PLACEHOLDER.toLowerCase(Locale.ROOT)));
    }

    private boolean containsAny(String text, List<String> markers) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return markers.stream().anyMatch(marker -> normalized.contains(marker.toLowerCase(Locale.ROOT)));
    }

    private String value(String input) {
        return input == null ? "" : input.trim();
    }

    private void addDistinct(List<String> target, String value) {
        if (value != null && !value.isBlank() && !target.contains(value)) {
            target.add(value);
        }
    }
}
