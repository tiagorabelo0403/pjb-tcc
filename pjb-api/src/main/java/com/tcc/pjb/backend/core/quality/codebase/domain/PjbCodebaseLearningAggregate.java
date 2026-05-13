package com.tcc.pjb.backend.core.quality.codebase.domain;

import java.time.Instant;
import java.util.List;

public record PjbCodebaseLearningAggregate(
        boolean disponivel,
        int arquivosMain,
        int arquivosTeste,
        int testesIntegracao,
        int fatiasMapeadas,
        List<PjbCodebaseLearningSlice> hotspots,
        List<PjbCodebaseExtractionBlueprint> blueprintsExtracao,
        List<PjbCodebaseCriticalFlow> fluxosCriticos,
        List<String> ondasPrioritarias,
        List<String> aprendizados,
        Instant geradoEm
) {
    public PjbCodebaseLearningAggregate {
        hotspots = hotspots == null ? List.of() : List.copyOf(hotspots);
        blueprintsExtracao = blueprintsExtracao == null ? List.of() : List.copyOf(blueprintsExtracao);
        fluxosCriticos = fluxosCriticos == null ? List.of() : List.copyOf(fluxosCriticos);
        ondasPrioritarias = ondasPrioritarias == null ? List.of() : List.copyOf(ondasPrioritarias);
        aprendizados = aprendizados == null ? List.of() : List.copyOf(aprendizados);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }

    public boolean temHotspotsCriticos() {
        return hotspots.stream().anyMatch(item -> "CRITICA".equals(item.prioridade()));
    }

    public double razaoIntegracao() {
        return arquivosMain == 0 ? 0.0d : (double) testesIntegracao / (double) arquivosMain;
    }

    public boolean possuiFluxoCriticoAusente() {
        return fluxosCriticos.stream().anyMatch(item -> "AUSENTE".equals(item.status()));
    }
}
