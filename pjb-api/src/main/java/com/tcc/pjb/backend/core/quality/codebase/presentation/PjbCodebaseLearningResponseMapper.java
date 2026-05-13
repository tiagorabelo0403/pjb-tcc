package com.tcc.pjb.backend.core.quality.codebase.presentation;

import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseCriticalFlow;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseExtractionBlueprint;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseExtractionLane;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseLearningAggregate;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseLearningSlice;
import com.tcc.pjb.backend.model.dto.governance.CodebaseLearningResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseCriticalFlowResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseLearningBlueprintResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseLearningHotspotResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseLearningLaneResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseLearningResponse;

public final class PjbCodebaseLearningResponseMapper {

    private PjbCodebaseLearningResponseMapper() {
    }

    public static CodebaseLearningResponse toGovernance(PjbCodebaseLearningAggregate aggregate) {
        return new CodebaseLearningResponse(
                aggregate.disponivel(),
                aggregate.temHotspotsCriticos(),
                aggregate.arquivosMain(),
                aggregate.arquivosTeste(),
                aggregate.testesIntegracao(),
                aggregate.fatiasMapeadas(),
                aggregate.hotspots().stream().map(PjbCodebaseLearningResponseMapper::toGovernanceHotspot).toList(),
                aggregate.blueprintsExtracao().stream().map(PjbCodebaseLearningResponseMapper::toGovernanceBlueprint).toList(),
                aggregate.fluxosCriticos().stream().map(PjbCodebaseLearningResponseMapper::toGovernanceCriticalFlow).toList(),
                aggregate.ondasPrioritarias(),
                aggregate.aprendizados(),
                aggregate.geradoEm()
        );
    }

    public static ProcessoCodebaseLearningResponse toProcessual(PjbCodebaseLearningAggregate aggregate) {
        return new ProcessoCodebaseLearningResponse(
                aggregate.disponivel(),
                aggregate.temHotspotsCriticos(),
                aggregate.arquivosMain(),
                aggregate.arquivosTeste(),
                aggregate.testesIntegracao(),
                aggregate.fatiasMapeadas(),
                aggregate.hotspots().stream().map(PjbCodebaseLearningResponseMapper::toProcessualHotspot).toList(),
                aggregate.blueprintsExtracao().stream().map(PjbCodebaseLearningResponseMapper::toProcessualBlueprint).toList(),
                aggregate.fluxosCriticos().stream().map(PjbCodebaseLearningResponseMapper::toProcessualCriticalFlow).toList(),
                aggregate.ondasPrioritarias(),
                aggregate.aprendizados(),
                aggregate.geradoEm()
        );
    }

    private static CodebaseLearningResponse.HotspotResponse toGovernanceHotspot(PjbCodebaseLearningSlice slice) {
        return new CodebaseLearningResponse.HotspotResponse(
                slice.fatia(),
                slice.arquivosMain(),
                slice.arquivosTeste(),
                slice.dependenciasEntrantes(),
                slice.dependenciasSaida(),
                slice.consumidoresController(),
                slice.razaoTeste(),
                slice.pressaoExtracao(),
                slice.prioridade(),
                slice.sinais(),
                slice.acoesRecomendadas(),
                slice.trilhasExtracao().stream().map(PjbCodebaseLearningResponseMapper::toGovernanceLane).toList()
        );
    }

    private static CodebaseLearningResponse.ExtractionLaneResponse toGovernanceLane(PjbCodebaseExtractionLane lane) {
        return new CodebaseLearningResponse.ExtractionLaneResponse(
                lane.nome(),
                lane.arquivosMain(),
                lane.arquivosTeste(),
                lane.razaoTeste(),
                lane.prontidao(),
                lane.sinais(),
                lane.acoesIniciais()
        );
    }

    private static CodebaseLearningResponse.ExtractionBlueprintResponse toGovernanceBlueprint(PjbCodebaseExtractionBlueprint blueprint) {
        return new CodebaseLearningResponse.ExtractionBlueprintResponse(
                blueprint.fatia(),
                blueprint.trilha(),
                blueprint.prontidao(),
                blueprint.scorePrioridade(),
                blueprint.pacoteAlvo(),
                blueprint.fachadaSugerida(),
                blueprint.portaSugerida(),
                blueprint.contratoIntegracaoSugerido(),
                blueprint.bloqueios(),
                blueprint.primeirasAcoes()
        );
    }

    private static CodebaseLearningResponse.CriticalFlowResponse toGovernanceCriticalFlow(PjbCodebaseCriticalFlow flow) {
        return new CodebaseLearningResponse.CriticalFlowResponse(
                flow.nome(),
                flow.status(),
                flow.cobertura(),
                flow.testesRelacionados(),
                flow.sinais(),
                flow.acoesIniciais()
        );
    }

    private static ProcessoCodebaseLearningHotspotResponse toProcessualHotspot(PjbCodebaseLearningSlice slice) {
        return new ProcessoCodebaseLearningHotspotResponse(
                slice.fatia(),
                slice.arquivosMain(),
                slice.arquivosTeste(),
                slice.dependenciasEntrantes(),
                slice.dependenciasSaida(),
                slice.consumidoresController(),
                slice.razaoTeste(),
                slice.pressaoExtracao(),
                slice.prioridade(),
                slice.sinais(),
                slice.acoesRecomendadas(),
                slice.trilhasExtracao().stream().map(PjbCodebaseLearningResponseMapper::toProcessualLane).toList()
        );
    }

    private static ProcessoCodebaseLearningLaneResponse toProcessualLane(PjbCodebaseExtractionLane lane) {
        return new ProcessoCodebaseLearningLaneResponse(
                lane.nome(),
                lane.arquivosMain(),
                lane.arquivosTeste(),
                lane.razaoTeste(),
                lane.prontidao(),
                lane.sinais(),
                lane.acoesIniciais()
        );
    }

    private static ProcessoCodebaseLearningBlueprintResponse toProcessualBlueprint(PjbCodebaseExtractionBlueprint blueprint) {
        return new ProcessoCodebaseLearningBlueprintResponse(
                blueprint.fatia(),
                blueprint.trilha(),
                blueprint.prontidao(),
                blueprint.scorePrioridade(),
                blueprint.pacoteAlvo(),
                blueprint.fachadaSugerida(),
                blueprint.portaSugerida(),
                blueprint.contratoIntegracaoSugerido(),
                blueprint.bloqueios(),
                blueprint.primeirasAcoes()
        );
    }

    private static ProcessoCodebaseCriticalFlowResponse toProcessualCriticalFlow(PjbCodebaseCriticalFlow flow) {
        return new ProcessoCodebaseCriticalFlowResponse(
                flow.nome(),
                flow.status(),
                flow.cobertura(),
                flow.testesRelacionados(),
                flow.sinais(),
                flow.acoesIniciais()
        );
    }
}
