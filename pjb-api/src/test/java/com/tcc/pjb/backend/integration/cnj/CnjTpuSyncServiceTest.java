package com.tcc.pjb.backend.integration.cnj;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CnjTpuSyncServiceTest {

    @Test
    void healthComSnapshotFrioUsaCatalogoLocalEfetivo() {
        CnjTpuSyncService service = new CnjTpuSyncService(HttpClient.newHttpClient());

        Map<String, Object> health = service.health();

        assertThat(health.get("hasSnapshot")).isEqualTo(true);
        assertThat(health.get("snapshotFresh")).isEqualTo(true);
        assertThat(health.get("snapshotId")).isNotNull();
        assertThat(health.get("syncedAt")).isNotNull();
    }
}
