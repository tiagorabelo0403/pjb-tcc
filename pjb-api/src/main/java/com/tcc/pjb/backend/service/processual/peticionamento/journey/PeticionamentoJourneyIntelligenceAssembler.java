package com.tcc.pjb.backend.service.processual.peticionamento.journey;

import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookRow;
import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookStep;
import com.tcc.pjb.backend.core.procedural.NationalProceduralTribunalVariationRow;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoJourneyActionResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoJourneyIntelligenceResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoJourneyStepResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoSimpleProtocolWizardStepResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class PeticionamentoJourneyIntelligenceAssembler {

    private PeticionamentoJourneyIntelligenceAssembler() {
    }

    static PeticionamentoJourneyIntelligenceResponse assemble(PeticionamentoSessaoRequest request,
                                                              ProceduralSubmissionBlueprintReport blueprint,
                                                              NationalProceduralOperationalPlaybookRow playbook,
                                                              NationalProceduralTribunalVariationRow variation,
                                                              List<PeticionamentoSimpleProtocolWizardStepResponse> wizardSteps) {
        List<PeticionamentoJourneyStepResponse> steps = buildSteps(request, blueprint, playbook, wizardSteps);
        int completedWeight = steps.stream().filter(PeticionamentoJourneyIntelligenceAssembler::isCompleted).mapToInt(PeticionamentoJourneyStepResponse::weight).sum();
        int totalWeight = Math.max(1, steps.stream().mapToInt(PeticionamentoJourneyStepResponse::weight).sum());
        int completionScore = Math.min(100, Math.max(0, Math.round((completedWeight * 100.0f) / totalWeight)));
        List<String> observedSignals = buildObservedSignals(request, blueprint, variation, completionScore);
        List<String> missingDomains = buildMissingDomains(request, blueprint, playbook);
        List<PeticionamentoJourneyActionResponse> nextActions = buildNextActions(request, blueprint, variation, playbook, missingDomains);
        String phase = resolvePhase(steps, blueprint);
        String pulse = resolvePulse(blueprint, completionScore);
        return new PeticionamentoJourneyIntelligenceResponse(
                Instant.now(),
                playbook.rito(),
                playbook.ramo(),
                phase,
                pulse,
                completionScore,
                (int) steps.stream().filter(PeticionamentoJourneyIntelligenceAssembler::isCompleted).count(),
                steps.size(),
                true,
                true,
                blueprint != null && blueprint.readyForAssistedSubmission(),
                blueprint != null && blueprint.readyForRealConnectorSubmission(),
                blueprint != null && blueprint.requiresGovBrStepUp(),
                blueprint != null && blueprint.requiresCertificate(),
                isAutoAdvanceEligible(blueprint, completionScore),
                observedSignals,
                missingDomains,
                steps,
                nextActions,
                buildMetrics(blueprint, variation, completionScore, steps)
        );
    }

    private static List<PeticionamentoJourneyStepResponse> buildSteps(PeticionamentoSessaoRequest request,
                                                                      ProceduralSubmissionBlueprintReport blueprint,
                                                                      NationalProceduralOperationalPlaybookRow playbook,
                                                                      List<PeticionamentoSimpleProtocolWizardStepResponse> wizardSteps) {
        ArrayList<PeticionamentoJourneyStepResponse> out = new ArrayList<>();
        for (NationalProceduralOperationalPlaybookStep step : playbook.steps()) {
            String status = wizardStatus(step.code(), wizardSteps);
            if (status == null) {
                status = PeticionamentoProtocolProgressSupport.resolveStepStatus(step.code(), request, blueprint);
            }
            out.add(new PeticionamentoJourneyStepResponse(
                    step.orderIndex(),
                    step.code(),
                    step.lane(),
                    step.title(),
                    status,
                    resolveAutomatable(step.code(), request, blueprint),
                    resolveWeight(step.code())
            ));
        }
        return List.copyOf(out);
    }

    private static String wizardStatus(String code, List<PeticionamentoSimpleProtocolWizardStepResponse> wizardSteps) {
        if (wizardSteps == null || wizardSteps.isEmpty()) {
            return null;
        }
        return wizardSteps.stream()
                .filter(step -> Objects.equals(PeticionamentoJourneyPayloadSupport.normalizeUpper(step.code()), PeticionamentoJourneyPayloadSupport.normalizeUpper(code)))
                .map(PeticionamentoSimpleProtocolWizardStepResponse::status)
                .findFirst()
                .orElse(null);
    }

    private static boolean resolveAutomatable(String code,
                                              PeticionamentoSessaoRequest request,
                                              ProceduralSubmissionBlueprintReport blueprint) {
        return switch (PeticionamentoJourneyPayloadSupport.normalizeUpper(code)) {
            case "TRIAGEM_MATERIAL" -> PeticionamentoProtocolProgressSupport.hasMinimalCaseIdentity(request);
            case "COMPETENCIA_E_ORGAO" -> PeticionamentoJourneyPayloadSupport.filled(request == null ? null : request.getRitoProcessual())
                    && PeticionamentoJourneyPayloadSupport.filled(request == null ? null : request.getTipoJustica());
            case "PROVA_E_DOCUMENTOS" -> PeticionamentoProtocolProgressSupport.hasEvidence(request);
            case "ASSINATURA_E_PROTOCOLO" -> blueprint != null && blueprint.readyForAssistedSubmission();
            default -> false;
        };
    }

    private static int resolveWeight(String code) {
        return switch (PeticionamentoJourneyPayloadSupport.normalizeUpper(code)) {
            case "TRIAGEM_MATERIAL" -> 15;
            case "COMPETENCIA_E_ORGAO" -> 20;
            case "PARTES_E_REPRESENTACAO" -> 15;
            case "PROVA_E_DOCUMENTOS" -> 15;
            case "PEDIDOS_E_URGENCIA" -> 15;
            case "ASSINATURA_E_PROTOCOLO" -> 15;
            case "DISTRIBUICAO_E_ACOMPANHAMENTO" -> 5;
            default -> 10;
        };
    }

    private static boolean isCompleted(PeticionamentoJourneyStepResponse step) {
        return step != null && switch (step.status()) {
            case "CONCLUIDA", "PRONTA", "POS_PROTOCOLO" -> true;
            default -> false;
        };
    }

    private static List<String> buildObservedSignals(PeticionamentoSessaoRequest request,
                                                     ProceduralSubmissionBlueprintReport blueprint,
                                                     NationalProceduralTribunalVariationRow variation,
                                                     int completionScore) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (PeticionamentoProtocolProgressSupport.hasMinimalCaseIdentity(request)) {
            out.add("IDENTIDADE_DO_CASO_PRESENTE");
        }
        if (PeticionamentoProtocolProgressSupport.hasPartyData(request)) {
            out.add("PARTES_QUALIFICADAS");
        }
        if (PeticionamentoProtocolProgressSupport.hasEvidence(request)) {
            out.add("PROVA_OU_DOCUMENTOS_PRESENTES");
        }
        if (PeticionamentoProtocolProgressSupport.hasClaims(request)) {
            out.add("PEDIDOS_ESTRUTURADOS");
        }
        if (PeticionamentoJourneyPayloadSupport.filled(request == null ? null : request.getTipoJustica())) {
            out.add("COMPETENCIA_BASE_DECLARADA");
        }
        if (PeticionamentoJourneyPayloadSupport.filled(variation == null ? null : variation.tribunalCodigo())) {
            out.add("TRIBUNAL_CORRELACIONADO");
        }
        if (blueprint != null && blueprint.requiresGovBrStepUp()) {
            out.add("STEP_UP_GOVBR_EXIGIDO");
        }
        if (blueprint != null && blueprint.requiresCertificate()) {
            out.add("CERTIFICADO_EXIGIDO");
        }
        if (Boolean.TRUE.equals(request == null ? null : request.getTutelaUrgencia()) || Boolean.TRUE.equals(request == null ? null : request.getRequerLiminar())) {
            out.add("URGENCIA_DETECTADA");
        }
        if (completionScore >= 70) {
            out.add("PACOTE_MADURO_PARA_PROTOCOLO_ASSISTIDO");
        }
        return List.copyOf(out);
    }

    private static List<String> buildMissingDomains(PeticionamentoSessaoRequest request,
                                                    ProceduralSubmissionBlueprintReport blueprint,
                                                    NationalProceduralOperationalPlaybookRow playbook) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (!PeticionamentoProtocolProgressSupport.hasMinimalCaseIdentity(request)) {
            out.add("IDENTIFICACAO_DO_CASO");
        }
        if (blueprint != null && !blueprint.blockingIssues().isEmpty()) {
            out.add("COMPETENCIA_E_ORGAO");
        }
        if (!PeticionamentoProtocolProgressSupport.hasPartyData(request)) {
            out.add("PARTES_E_REPRESENTACAO");
        }
        if (!PeticionamentoProtocolProgressSupport.hasEvidence(request)) {
            out.add("PROVA_E_DOCUMENTOS");
        }
        if (!PeticionamentoProtocolProgressSupport.hasClaims(request)) {
            out.add("PEDIDOS_E_URGENCIA");
        }
        if (blueprint != null && !blueprint.readyForAssistedSubmission()) {
            out.add("ASSINATURA_E_PRE_FLIGHT");
        }
        if (out.isEmpty() && playbook != null && !playbook.requiredDocuments().isEmpty()) {
            out.add("PACOTE_MINIMO_DE_DOCUMENTOS");
        }
        return List.copyOf(out);
    }

    private static List<PeticionamentoJourneyActionResponse> buildNextActions(PeticionamentoSessaoRequest request,
                                                                              ProceduralSubmissionBlueprintReport blueprint,
                                                                              NationalProceduralTribunalVariationRow variation,
                                                                              NationalProceduralOperationalPlaybookRow playbook,
                                                                              List<String> missingDomains) {
        ArrayList<PeticionamentoJourneyActionResponse> out = new ArrayList<>();
        if (!PeticionamentoProtocolProgressSupport.hasMinimalCaseIdentity(request)) {
            out.add(new PeticionamentoJourneyActionResponse("QUALIFICAR_CASO", "Fechar identidade material do caso", "Faltam classe, rito, fatos resumidos ou título do caso.", true, "TRIAGEM"));
        }
        if (blueprint != null && !blueprint.blockingIssues().isEmpty()) {
            out.add(new PeticionamentoJourneyActionResponse("SANEAR_COMPETENCIA", "Sanear competência, unidade e pré-protocolo", blueprint.blockingIssues().getFirst(), true, "COMPETENCIA"));
        }
        if (!PeticionamentoProtocolProgressSupport.hasPartyData(request)) {
            out.add(new PeticionamentoJourneyActionResponse("QUALIFICAR_PARTES", "Qualificar partes e representação", "O pacote ainda não traz autor e réu em formato operacional.", false, "PARTES"));
        }
        if (!PeticionamentoProtocolProgressSupport.hasEvidence(request)) {
            out.add(new PeticionamentoJourneyActionResponse("ORGANIZAR_DOCUMENTOS", "Consolidar prova e anexos mínimos", "Sem documentos ou prova mínima o protocolo perde robustez.", false, "PROVA"));
        }
        if (!PeticionamentoProtocolProgressSupport.hasClaims(request)) {
            out.add(new PeticionamentoJourneyActionResponse("FECHAR_PEDIDOS", "Fechar pedidos, tutela e valor da causa", "Os pedidos ainda não estão maduros para envio governado.", false, "PEDIDOS"));
        }
        if (blueprint != null && blueprint.requiresGovBrStepUp()) {
            out.add(new PeticionamentoJourneyActionResponse("EXECUTAR_STEP_UP", "Executar step-up Gov.br", "O sistema judicial selecionado exige reforço de autenticação antes do protocolo.", true, "PROTOCOLO"));
        }
        if (blueprint != null && blueprint.requiresCertificate()) {
            out.add(new PeticionamentoJourneyActionResponse("VINCULAR_CERTIFICADO", "Vincular certificado ou assinatura compatível", "O conector judicial exige identidade criptográfica de protocolo.", false, "PROTOCOLO"));
        }
        if (variation != null && !variation.localRules().isEmpty()) {
            out.add(new PeticionamentoJourneyActionResponse("AJUSTAR_VARIACAO_LOCAL", "Aplicar variação local do tribunal/unidade", variation.localRules().getFirst(), true, "COMPETENCIA"));
        }
        if (out.isEmpty() && playbook != null) {
            out.add(new PeticionamentoJourneyActionResponse("PROTOCOLO_ASSISTIDO", "Executar protocolo assistido", "O pacote está consistente para avanço controlado.", true, "PROTOCOLO"));
        }
        return out.stream().limit(Math.max(1, Math.min(5, missingDomains.size() + 2))).toList();
    }

    private static String resolvePhase(List<PeticionamentoJourneyStepResponse> steps,
                                       ProceduralSubmissionBlueprintReport blueprint) {
        if (blueprint != null && blueprint.readyForRealConnectorSubmission()) {
            return "PROTOCOLO_REAL";
        }
        return steps.stream()
                .filter(step -> !isCompleted(step))
                .map(step -> switch (PeticionamentoJourneyPayloadSupport.normalizeUpper(step.code())) {
                    case "TRIAGEM_MATERIAL" -> "TRIAGEM";
                    case "COMPETENCIA_E_ORGAO" -> "COMPETENCIA";
                    case "PARTES_E_REPRESENTACAO" -> "PARTES";
                    case "PROVA_E_DOCUMENTOS" -> "PROVA";
                    case "PEDIDOS_E_URGENCIA" -> "PEDIDOS";
                    case "ASSINATURA_E_PROTOCOLO" -> "PROTOCOLO";
                    default -> "ACOMPANHAMENTO";
                })
                .findFirst()
                .orElse("ACOMPANHAMENTO");
    }

    private static String resolvePulse(ProceduralSubmissionBlueprintReport blueprint, int completionScore) {
        if (blueprint != null && !blueprint.blockingIssues().isEmpty()) {
            return "SANEAMENTO";
        }
        if (blueprint != null && blueprint.readyForRealConnectorSubmission()) {
            return "PROTOCOLO_REAL";
        }
        if (blueprint != null && blueprint.readyForAssistedSubmission()) {
            return "PROTOCOLO_ASSISTIDO";
        }
        if (completionScore >= 70) {
            return "CONSOLIDACAO";
        }
        if (completionScore >= 35) {
            return "ESTRUTURACAO";
        }
        return "ARRANQUE";
    }

    private static boolean isAutoAdvanceEligible(ProceduralSubmissionBlueprintReport blueprint, int completionScore) {
        return blueprint != null && blueprint.blockingIssues().isEmpty() && (blueprint.readyForAssistedSubmission() || completionScore >= 60);
    }

    private static Map<String, Object> buildMetrics(ProceduralSubmissionBlueprintReport blueprint,
                                                    NationalProceduralTribunalVariationRow variation,
                                                    int completionScore,
                                                    List<PeticionamentoJourneyStepResponse> steps) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "ON_DEMAND_COMPACT");
        out.put("backgroundTracking", false);
        out.put("sessionRetention", "NONE");
        out.put("stepComputation", "FIXED_LINEAR_SCAN");
        out.put("completionScore", completionScore);
        out.put("blockedSteps", steps.stream().filter(step -> "BLOQUEADA".equals(step.status())).count());
        out.put("pendingSteps", steps.stream().filter(step -> "PENDENTE".equals(step.status()) || "ASSISTIDA".equals(step.status()) || "AGUARDANDO_PROTOCOLO".equals(step.status())).count());
        out.put("connectorOperational", blueprint != null && blueprint.connectorOperational());
        out.put("judicialSystem", blueprint != null && blueprint.judicialSystem() != null ? blueprint.judicialSystem().name() : variation == null ? null : variation.judicialSystem());
        out.put("tribunalCodigo", blueprint == null ? variation == null ? null : variation.tribunalCodigo() : blueprint.tribunalCodigo());
        out.put("unidadeCodigo", blueprint == null ? variation == null ? null : variation.unidadeCodigo() : blueprint.unidadeJudiciariaCodigo());
        out.put("blockingIssues", blueprint == null ? 0 : blueprint.blockingIssues().size());
        out.put("warnings", blueprint == null ? 0 : blueprint.warnings().size());
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return out.isEmpty() ? Map.of() : java.util.Collections.unmodifiableMap(out);
    }
}
