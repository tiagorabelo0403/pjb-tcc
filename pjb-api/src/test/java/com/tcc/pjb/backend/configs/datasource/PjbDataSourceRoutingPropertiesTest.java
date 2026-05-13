package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PjbDataSourceRoutingPropertiesTest {

    @Test
    void regionalSelectionMustExposeInstitutionalRoutingHeadersDefaults() {
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        assertThat(properties.getRegionalSelection().getRequestHeaderReplica()).isEqualTo("X-PJB-Read-Replica");
        assertThat(properties.getRegionalSelection().getRequestHeaderTribunal()).isEqualTo("X-PJB-Tribunal");
        assertThat(properties.getRegionalSelection().getRequestHeaderUf()).isEqualTo("X-PJB-UF");
        assertThat(properties.getRegionalSelection().getRequestHeaderOrgao()).isEqualTo("X-PJB-Orgao");
        assertThat(properties.getRegionalSelection().getRequestHeaderUnidade()).isEqualTo("X-PJB-Unidade");
        assertThat(properties.getRegionalSelection().getRequestHeaderCaixa()).isEqualTo("X-PJB-Caixa");
    }

    @Test
    void adaptivePlaneMustExposeSovereignDefaults() {
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        assertThat(properties.getAdaptivePlane().isEnabled()).isTrue();
        assertThat(properties.getAdaptivePlane().isEmitResponseHeaders()).isTrue();
        assertThat(properties.getAdaptivePlane().getReplicaLagTolerance()).hasSeconds(2);
        assertThat(properties.getAdaptivePlane().getPrimaryCriticalPrefixes()).contains("/api/v1/processos", "/api/v1/peticionamento");
        assertThat(properties.getAdaptivePlane().getHotCachePrefixes()).contains("/api/v1/painel", "/api/v1/timeline");
        assertThat(properties.getAdaptivePlane().getSearchBackedPrefixes()).contains("/api/v1/busca", "/api/v1/indexacao");
        assertThat(properties.getAdaptivePlane().getAsyncWritePrefixes()).contains("/api/v1/documentos/upload", "/api/v1/anexos");
    }
}
