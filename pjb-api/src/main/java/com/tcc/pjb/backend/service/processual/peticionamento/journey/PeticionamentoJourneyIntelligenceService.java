package com.tcc.pjb.backend.service.processual.peticionamento.journey;

import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookRow;
import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralTribunalVariationRow;
import com.tcc.pjb.backend.core.procedural.NationalProceduralTribunalVariationService;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoJourneyIntelligenceResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoSimpleProtocolWizardStepResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoJourneyIntelligenceService {

    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    private final NationalProceduralOperationalPlaybookService operationalPlaybookService;
    private final NationalProceduralTribunalVariationService tribunalVariationService;

    public PeticionamentoJourneyIntelligenceService(ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService,
                                                    NationalProceduralOperationalPlaybookService operationalPlaybookService,
                                                    NationalProceduralTribunalVariationService tribunalVariationService) {
        this.proceduralSubmissionBlueprintService = Objects.requireNonNull(proceduralSubmissionBlueprintService);
        this.operationalPlaybookService = Objects.requireNonNull(operationalPlaybookService);
        this.tribunalVariationService = Objects.requireNonNull(tribunalVariationService);
    }

    public PeticionamentoJourneyIntelligenceResponse analyze(PeticionamentoSessaoRequest request) {
        Map<String, Object> payload = PeticionamentoJourneyPayloadSupport.buildPayload(request);
        ProceduralSubmissionBlueprintReport blueprint = proceduralSubmissionBlueprintService.analyzeContext(payload, request == null ? null : request.getProcessoId());
        NationalProceduralOperationalPlaybookRow playbook = operationalPlaybookService.describe(resolveRito(request, blueprint));
        NationalProceduralTribunalVariationRow variation = tribunalVariationService.describe(
                blueprint.tribunalCodigo(),
                blueprint.unidadeJudiciariaCodigo(),
                playbook.rito(),
                request == null ? null : request.getTipoJustica()
        );
        return PeticionamentoJourneyIntelligenceAssembler.assemble(request, blueprint, playbook, variation, null);
    }

    public PeticionamentoJourneyIntelligenceResponse analyze(PeticionamentoSessaoRequest request,
                                                             ProceduralSubmissionBlueprintReport blueprint,
                                                             NationalProceduralOperationalPlaybookRow playbook,
                                                             NationalProceduralTribunalVariationRow variation,
                                                             List<PeticionamentoSimpleProtocolWizardStepResponse> steps) {
        return PeticionamentoJourneyIntelligenceAssembler.assemble(request, blueprint, playbook, variation, steps);
    }

    private String resolveRito(PeticionamentoSessaoRequest request, ProceduralSubmissionBlueprintReport blueprint) {
        if (PeticionamentoJourneyPayloadSupport.filled(request == null ? null : request.getRitoProcessual())) {
            return request.getRitoProcessual();
        }
        if (PeticionamentoJourneyPayloadSupport.filled(blueprint == null ? null : blueprint.rito())) {
            return blueprint.rito();
        }
        return "COMUM_ORDINARIO";
    }
}
