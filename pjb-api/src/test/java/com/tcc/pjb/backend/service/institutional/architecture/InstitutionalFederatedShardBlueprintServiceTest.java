package com.tcc.pjb.backend.service.institutional.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalArchitectureResponse;
import org.junit.jupiter.api.Test;

class InstitutionalFederatedShardBlueprintServiceTest {

    private final InstitutionalFederatedShardBlueprintService service = new InstitutionalFederatedShardBlueprintService();

    @Test
    void shouldRouteSuperiorCourtToFederalCluster() {
        AdminInstitutionalArchitectureResponse.ClusterResolution resolution = service.resolve("STJ", null);

        assertThat(resolution.clusterCode()).isEqualTo("FEDERAL_SUPERIOR");
        assertThat(resolution.federated()).isTrue();
    }

    @Test
    void shouldRouteCearaToNordesteCluster() {
        AdminInstitutionalArchitectureResponse.ClusterResolution resolution = service.resolve("TJCE", "CE");

        assertThat(resolution.clusterCode()).isEqualTo("NORDESTE");
        assertThat(resolution.metadataKey()).contains("NORDESTE");
    }
}
