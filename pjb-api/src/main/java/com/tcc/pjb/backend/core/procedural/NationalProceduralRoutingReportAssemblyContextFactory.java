package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingReportAssemblyContextFactory {

    NationalProceduralRoutingReportAssemblyContext create(NationalProceduralRoutingCoreResolution resolution,
                                                          ProceduralEconomicGateReport economicGate,
                                                          Map<String, Object> metadata) {
        Objects.requireNonNull(resolution);
        return new NationalProceduralRoutingReportAssemblyContext(
                resolution.actionProfile(),
                resolution.proceduralRegime(),
                resolution.proceduralTrack(),
                tipoJusticaName(resolution.tipoJustica()),
                resolution.ritoSugerido(),
                resolution.judicialPlacement().tribunalCodigo(),
                resolution.judicialPlacement().tribunalNome(),
                resolution.judicialPlacement().judicialSystem(),
                resolution.judicialPlacement().foroSugerido(),
                resolution.judicialPlacement().cidadeSugerida(),
                resolution.judicialPlacement().ufSugerida(),
                resolution.judicialPlacement().varaSugerida(),
                resolution.judicialPlacement().tipoVaraSugerido(),
                resolution.complexityBand(),
                resolution.probatoryProfile(),
                resolution.juizadoDecision(),
                resolution.reviewSynthesis(),
                economicGate,
                resolution.judicialPlacement().forumAllocation(),
                metadata
        );
    }

    private static String tipoJusticaName(TipoJustica tipoJustica) {
        return tipoJustica != null ? tipoJustica.name() : null;
    }
}
