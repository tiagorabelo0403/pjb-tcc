package com.tcc.pjb.backend.service.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import org.junit.jupiter.api.Test;

class PjbProcessualReadModelMeshServiceTest {

    @Test
    void shouldIgnoreFreshnessForUnmanagedDomain() {
        PjbProcessualReadModelMeshService service = new PjbProcessualReadModelMeshService(new PjbDataSourceRoutingProperties());

        service.registerEvent("DOMINIO_EXTERNO", "MOVIMENTACAO_REGISTRADA", "Processo", "123", "worker-x");

        PjbProcessualReadModelMeshService.ProcessualReadModelBlueprint blueprint = service.blueprint();

        assertThat(blueprint.domains())
                .extracting(PjbProcessualReadModelMeshService.DomainBlueprint::freshness)
                .containsOnlyNulls();
    }

    @Test
    void shouldTrackFreshnessForManagedDomainUsingNormalizedName() {
        PjbProcessualReadModelMeshService service = new PjbProcessualReadModelMeshService(new PjbDataSourceRoutingProperties());

        service.registerEvent("processo timeline hot", "movimentacao_registrada", "Processo", "321", "projector-a");

        PjbProcessualReadModelMeshService.DomainBlueprint domain = service.blueprint().domains().stream()
                .filter(candidate -> "PROCESSO_TIMELINE_HOT".equals(candidate.domain()))
                .findFirst()
                .orElseThrow();

        assertThat(domain.freshness()).isNotNull();
        assertThat(domain.freshness().lastEventType()).isEqualTo("MOVIMENTACAO_REGISTRADA");
        assertThat(domain.freshness().lastAggregateType()).isEqualTo("PROCESSO");
        assertThat(domain.freshness().lastAggregateId()).isEqualTo("321");
        assertThat(domain.freshness().source()).isEqualTo("PROJECTOR-A");
    }
}
