package com.tcc.pjb.backend.service.processual.peticionamento.journey;

import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookRow;
import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookStep;
import com.tcc.pjb.backend.core.procedural.NationalProceduralTribunalVariationRow;
import com.tcc.pjb.backend.core.procedural.NationalProceduralTribunalVariationService;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.service.processual.guard.DefensoriaInstitutionalCompetenceGuardService;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoSimpleProtocolWizardResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoSimpleProtocolWizardStepResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoSimpleProtocolWizardService {

    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    private final NationalProceduralOperationalPlaybookService operationalPlaybookService;
    private final NationalProceduralTribunalVariationService tribunalVariationService;
    private final PeticionamentoJourneyIntelligenceService journeyIntelligenceService;
    private final DefensoriaInstitutionalCompetenceGuardService defensoriaInstitutionalCompetenceGuardService;

    public PeticionamentoSimpleProtocolWizardService(ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService,
                                                     NationalProceduralOperationalPlaybookService operationalPlaybookService,
                                                     NationalProceduralTribunalVariationService tribunalVariationService,
                                                     PeticionamentoJourneyIntelligenceService journeyIntelligenceService,
                                                     DefensoriaInstitutionalCompetenceGuardService defensoriaInstitutionalCompetenceGuardService) {
        this.proceduralSubmissionBlueprintService = Objects.requireNonNull(proceduralSubmissionBlueprintService);
        this.operationalPlaybookService = Objects.requireNonNull(operationalPlaybookService);
        this.tribunalVariationService = Objects.requireNonNull(tribunalVariationService);
        this.journeyIntelligenceService = Objects.requireNonNull(journeyIntelligenceService);
        this.defensoriaInstitutionalCompetenceGuardService = Objects.requireNonNull(defensoriaInstitutionalCompetenceGuardService);
    }

    public PeticionamentoSimpleProtocolWizardResponse build(PeticionamentoSessaoRequest request) {
        Map<String, Object> payload = PeticionamentoJourneyPayloadSupport.buildPayload(request);
        ProceduralSubmissionBlueprintReport blueprint = proceduralSubmissionBlueprintService.analyzeContext(payload, request == null ? null : request.getProcessoId());
        NationalProceduralOperationalPlaybookRow playbook = operationalPlaybookService.describe(resolveRito(request, blueprint));
        NationalProceduralTribunalVariationRow variation = tribunalVariationService.describe(
                blueprint.tribunalCodigo(),
                blueprint.unidadeJudiciariaCodigo(),
                playbook.rito(),
                request == null ? null : request.getTipoJustica()
        );
        DefensoriaInstitutionalCompetenceGuardService.GuardDecision institutionalGuard = defensoriaInstitutionalCompetenceGuardService.analyzeInitialFiling(request, blueprint);
        List<PeticionamentoSimpleProtocolWizardStepResponse> steps = buildSteps(request, playbook, blueprint);
        List<String> blockingIssues = buildBlockingIssues(blueprint, institutionalGuard);
        List<String> warnings = buildWarnings(playbook, variation, blueprint, institutionalGuard);
        List<String> nextSteps = buildNextSteps(steps, blueprint, variation, institutionalGuard);
        boolean blockedByInstitutionalGuard = institutionalGuard.blocked();
        return new PeticionamentoSimpleProtocolWizardResponse(
                Instant.now(),
                resolveWizardStatus(blueprint, blockedByInstitutionalGuard),
                playbook.rito(),
                playbook.ramo(),
                playbook.grupo(),
                blueprint.tribunalCodigo(),
                blueprint.tribunalNome(),
                blueprint.judicialSystem() != null ? blueprint.judicialSystem().name() : variation.judicialSystem(),
                !blockedByInstitutionalGuard && blueprint.readyForAssistedSubmission(),
                !blockedByInstitutionalGuard && blueprint.readyForRealConnectorSubmission(),
                blueprint.requiresGovBrStepUp(),
                blueprint.requiresCertificate(),
                resolveNextAction(blueprint, steps, institutionalGuard),
                steps,
                blockingIssues,
                PeticionamentoJourneyPayloadSupport.mergeDistinct(playbook.preProtocolChecklist(), blueprint.reviewChecklist()),
                warnings,
                mergeGuardIntoPreview(blueprint.protocolRequestPreview(), institutionalGuard),
                toPlaybookMap(playbook),
                toVariationMap(variation, institutionalGuard),
                nextSteps,
                journeyIntelligenceService.analyze(request, blueprint, playbook, variation, steps)
        );
    }

    private List<PeticionamentoSimpleProtocolWizardStepResponse> buildSteps(PeticionamentoSessaoRequest request,
                                                                            NationalProceduralOperationalPlaybookRow playbook,
                                                                            ProceduralSubmissionBlueprintReport blueprint) {
        ArrayList<PeticionamentoSimpleProtocolWizardStepResponse> out = new ArrayList<>();
        for (NationalProceduralOperationalPlaybookStep step : playbook.steps()) {
            out.add(new PeticionamentoSimpleProtocolWizardStepResponse(
                    step.orderIndex(),
                    step.code(),
                    step.lane(),
                    step.title(),
                    PeticionamentoProtocolProgressSupport.resolveStepStatus(step.code(), request, blueprint),
                    step.blocking(),
                    step.outputs()
            ));
        }
        return List.copyOf(out);
    }

    private List<String> buildNextSteps(List<PeticionamentoSimpleProtocolWizardStepResponse> steps,
                                        ProceduralSubmissionBlueprintReport blueprint,
                                        NationalProceduralTribunalVariationRow variation,
                                        DefensoriaInstitutionalCompetenceGuardService.GuardDecision institutionalGuard) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        steps.stream()
                .filter(step -> !"CONCLUIDA".equals(step.status()) && !"PRONTA".equals(step.status()) && !"POS_PROTOCOLO".equals(step.status()))
                .map(PeticionamentoSimpleProtocolWizardStepResponse::title)
                .limit(3)
                .forEach(title -> out.add("Fechar etapa: " + title));
        if (blueprint.requiresGovBrStepUp()) {
            out.add("Executar step-up Gov.br antes do protocolo final.");
        }
        if (blueprint.requiresCertificate()) {
            out.add("Vincular assinatura ou certificado compatível ao pacote de protocolo.");
        }
        if (!variation.localRules().isEmpty()) {
            out.add("Aplicar variação local principal: " + variation.localRules().getFirst() + '.');
        }
        if (institutionalGuard.blocked()) {
            out.add("Redirecionar o caso para a DPU ou para a unidade federal competente.");
        } else if (institutionalGuard.verdict() == DefensoriaInstitutionalCompetenceGuardService.Verdict.REVIEW) {
            out.add("Revisar atribuição institucional da Defensoria antes do protocolo final.");
        }
        return List.copyOf(out);
    }

    private String resolveWizardStatus(ProceduralSubmissionBlueprintReport blueprint, boolean blockedByInstitutionalGuard) {
        if (blockedByInstitutionalGuard) {
            return "BLOQUEADO_ATRIBUICAO_INSTITUCIONAL";
        }
        if (!blueprint.blockingIssues().isEmpty()) {
            return "BLOQUEADO";
        }
        if (blueprint.readyForRealConnectorSubmission()) {
            return "PRONTO_PARA_PROTOCOLO_REAL";
        }
        if (blueprint.readyForAssistedSubmission()) {
            return "PRONTO_PARA_PROTOCOLO_ASSISTIDO";
        }
        return "EM_PREPARACAO";
    }

    private String resolveNextAction(ProceduralSubmissionBlueprintReport blueprint,
                                     List<PeticionamentoSimpleProtocolWizardStepResponse> steps,
                                     DefensoriaInstitutionalCompetenceGuardService.GuardDecision institutionalGuard) {
        if (institutionalGuard.blocked()) {
            return "REDIRECIONAR_DPU";
        }
        if (institutionalGuard.verdict() == DefensoriaInstitutionalCompetenceGuardService.Verdict.REVIEW) {
            return "REVISAR_ATRIBUICAO_INSTITUCIONAL";
        }
        if (!blueprint.blockingIssues().isEmpty()) {
            return "SANEAR_COMPETENCIA_E_DOCUMENTOS";
        }
        return steps.stream()
                .filter(step -> !"CONCLUIDA".equals(step.status()) && !"PRONTA".equals(step.status()) && !"POS_PROTOCOLO".equals(step.status()))
                .map(step -> PeticionamentoJourneyPayloadSupport.normalizeUpper(step.code()))
                .findFirst()
                .orElse("ASSINAR_E_PROTOCOLAR");
    }

    private List<String> buildBlockingIssues(ProceduralSubmissionBlueprintReport blueprint,
                                             DefensoriaInstitutionalCompetenceGuardService.GuardDecision institutionalGuard) {
        ArrayList<String> out = new ArrayList<>(blueprint.blockingIssues());
        if (institutionalGuard.blocked()) {
            out.add(institutionalGuard.publicMessage());
        }
        return List.copyOf(new LinkedHashSet<>(out));
    }

    private List<String> buildWarnings(NationalProceduralOperationalPlaybookRow playbook,
                                       NationalProceduralTribunalVariationRow variation,
                                       ProceduralSubmissionBlueprintReport blueprint,
                                       DefensoriaInstitutionalCompetenceGuardService.GuardDecision institutionalGuard) {
        return PeticionamentoJourneyPayloadSupport.mergeDistinct(
                playbook.warnings(),
                PeticionamentoJourneyPayloadSupport.mergeDistinct(
                        variation.localRules(),
                        PeticionamentoJourneyPayloadSupport.mergeDistinct(
                                blueprint.warnings(),
                                institutionalGuard.warnings()
                        )
                )
        );
    }

    private Map<String, Object> mergeGuardIntoPreview(Map<String, Object> protocolPreview,
                                                      DefensoriaInstitutionalCompetenceGuardService.GuardDecision institutionalGuard) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (protocolPreview != null) {
            out.putAll(protocolPreview);
        }
        out.put("institutionalCompetenceGuard", institutionalGuard.toMap());
        return Collections.unmodifiableMap(out);
    }

    private String resolveRito(PeticionamentoSessaoRequest request, ProceduralSubmissionBlueprintReport blueprint) {
        if (PeticionamentoJourneyPayloadSupport.filled(request == null ? null : request.getRitoProcessual())) {
            return request.getRitoProcessual();
        }
        if (PeticionamentoJourneyPayloadSupport.filled(blueprint.rito())) {
            return blueprint.rito();
        }
        return "COMUM_ORDINARIO";
    }

    private Map<String, Object> toPlaybookMap(NationalProceduralOperationalPlaybookRow playbook) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("rito", playbook.rito());
        out.put("ramo", playbook.ramo());
        out.put("grupo", playbook.grupo());
        out.put("protocoloSugerido", playbook.protocoloSugerido());
        out.put("competenceTracks", playbook.competenceTracks());
        out.put("unitAnchors", playbook.unitAnchors());
        out.put("requiredDocuments", playbook.requiredDocuments());
        out.put("guarantees", playbook.guarantees());
        out.put("steps", playbook.steps().stream().map(step -> Map.of(
                "orderIndex", step.orderIndex(),
                "code", step.code(),
                "lane", step.lane(),
                "title", step.title(),
                "blocking", step.blocking(),
                "outputs", step.outputs()
        )).toList());
        out.put("metadata", playbook.metadata());
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> toVariationMap(NationalProceduralTribunalVariationRow variation,
                                                DefensoriaInstitutionalCompetenceGuardService.GuardDecision institutionalGuard) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("tribunalCodigo", variation.tribunalCodigo());
        out.put("unidadeCodigo", variation.unidadeCodigo());
        out.put("judicialSystem", variation.judicialSystem());
        out.put("protocolChannels", variation.protocolChannels());
        out.put("unitAnchors", variation.unitAnchors());
        out.put("localRules", variation.localRules());
        out.put("stepUpRequired", variation.stepUpRequired());
        out.put("certificateRequired", variation.certificateRequired());
        out.put("metadata", variation.metadata());
        out.put("institutionalCompetenceGuard", institutionalGuard.toMap());
        return Collections.unmodifiableMap(out);
    }
}
