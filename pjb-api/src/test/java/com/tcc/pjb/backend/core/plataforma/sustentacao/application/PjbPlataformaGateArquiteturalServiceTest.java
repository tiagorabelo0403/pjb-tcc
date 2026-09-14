package com.tcc.pjb.backend.core.plataforma.sustentacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import com.tcc.pjb.backend.service.procedural.ProceduralArchitectureSanityService;
import com.tcc.pjb.backend.service.procedural.ProceduralLegacyBoundaryAuditService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PjbPlataformaGateArquiteturalServiceTest {

    private final PjbCodebaseSanityApplicationService codebaseSanityApplicationService = mock(PjbCodebaseSanityApplicationService.class);
    private final PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService = mock(PjbApiSurfaceSanityApplicationService.class);
    private final ProceduralArchitectureSanityService proceduralArchitectureSanityService = mock(ProceduralArchitectureSanityService.class);
    private final ProceduralLegacyBoundaryAuditService proceduralLegacyBoundaryAuditService = mock(ProceduralLegacyBoundaryAuditService.class);
    private final BuildGateGovernanceService buildGateGovernanceService = mock(BuildGateGovernanceService.class);

    private final PjbPlataformaGateArquiteturalService service = new PjbPlataformaGateArquiteturalService(
            codebaseSanityApplicationService,
            apiSurfaceSanityApplicationService,
            proceduralArchitectureSanityService,
            proceduralLegacyBoundaryAuditService,
            buildGateGovernanceService
    );

    private PjbCodebaseSanityAggregate codebase(boolean limpo) {
        return new PjbCodebaseSanityAggregate(true, limpo, 100, 0, 0, 0, List.of(), List.of(), List.of(), Instant.now());
    }

    private PjbApiSurfaceSanityAggregate apiSurface(boolean limpo) {
        return new PjbApiSurfaceSanityAggregate(true, limpo, 50, 20, 0, 0, 0, List.of(), Instant.now());
    }

    private ProceduralArchitectureSanityService.SanityReport architecture(boolean healthy) {
        return new ProceduralArchitectureSanityService.SanityReport(Instant.now(), healthy, 10, 5, 3, 3, List.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    private ProceduralLegacyBoundaryAuditService.BoundaryReport boundary(boolean clean) {
        return new ProceduralLegacyBoundaryAuditService.BoundaryReport(Instant.now(), true, clean, 500, List.of(), List.of());
    }

    private BuildGateEvaluationResponse buildGate(boolean approved) {
        return new BuildGateEvaluationResponse(approved, true, true, true, true, true, true, 0, List.of(), List.of());
    }

    @Test
    void avaliarRetornaEixoProntoQuandoTudoSaudavel() {
        when(codebaseSanityApplicationService.auditar()).thenReturn(codebase(true));
        when(apiSurfaceSanityApplicationService.auditar()).thenReturn(apiSurface(true));
        when(proceduralArchitectureSanityService.report()).thenReturn(architecture(true));
        when(proceduralLegacyBoundaryAuditService.report()).thenReturn(boundary(true));
        when(buildGateGovernanceService.evaluate()).thenReturn(buildGate(true));

        var eixo = service.avaliar();

        assertThat(eixo.codigo()).isEqualTo("gate.arquitetural");
        assertThat(eixo.pronto()).isTrue();
        assertThat(eixo.status()).isEqualTo("PRONTO");
        assertThat(eixo.bloqueadores()).isEmpty();
    }

    @Test
    void avaliarRetornaEixoBloqueadoQuandoArquiteturaDoente() {
        when(codebaseSanityApplicationService.auditar()).thenReturn(codebase(true));
        when(apiSurfaceSanityApplicationService.auditar()).thenReturn(apiSurface(true));
        when(proceduralArchitectureSanityService.report()).thenReturn(new ProceduralArchitectureSanityService.SanityReport(
                Instant.now(), false, 10, 5, 3, 3, List.of("catalogo desatualizado"), Map.of(), Map.of(), Map.of(), Map.of()));
        when(proceduralLegacyBoundaryAuditService.report()).thenReturn(boundary(true));
        when(buildGateGovernanceService.evaluate()).thenReturn(buildGate(true));

        var eixo = service.avaliar();

        assertThat(eixo.pronto()).isFalse();
        assertThat(eixo.bloqueadores()).contains("catalogo desatualizado");
        assertThat(eixo.proximasAcoes()).contains("NORMALIZAR_CATALOGO_PROCEDURAL_E_CONNECTORES_PREFERIDOS");
    }
}
