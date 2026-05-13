package com.tcc.pjb.backend.model.dto.processual.completude.codebase;

import java.time.Instant;
import java.util.List;

public record ProcessoCodebaseLearningResponse(
        boolean disponivel,
        boolean hotspotsCriticosPresentes,
        int arquivosMain,
        int arquivosTeste,
        int testesIntegracao,
        int fatiasCoreMapeadas,
        List<ProcessoCodebaseLearningHotspotResponse> hotspots,
        List<ProcessoCodebaseLearningBlueprintResponse> blueprintsExtracao,
        List<ProcessoCodebaseCriticalFlowResponse> fluxosCriticos,
        List<String> ondasPrioritarias,
        List<String> aprendizados,
        Instant geradoEm
) {
    public ProcessoCodebaseLearningResponse {
        hotspots = hotspots == null ? List.of() : List.copyOf(hotspots);
        blueprintsExtracao = blueprintsExtracao == null ? List.of() : List.copyOf(blueprintsExtracao);
        fluxosCriticos = fluxosCriticos == null ? List.of() : List.copyOf(fluxosCriticos);
        ondasPrioritarias = ondasPrioritarias == null ? List.of() : List.copyOf(ondasPrioritarias);
        aprendizados = aprendizados == null ? List.of() : List.copyOf(aprendizados);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
