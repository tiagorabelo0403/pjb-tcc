package com.tcc.pjb.backend.governance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbZeroTrustDataPlaneInfraGuardTest {

    @Test
    void sovereign_zero_trust_overlay_keeps_database_edge_and_restricted_egress() throws Exception {
        String configMap = Files.readString(Path.of("..", "infra", "k8s", "overlays", "prod-sovereign-zero-trust-data-plane", "patch-configmap.yaml"));
        String networkPolicy = Files.readString(Path.of("..", "infra", "k8s", "overlays", "prod-sovereign-zero-trust-data-plane", "patch-api-network-policy.yaml"));
        assertTrue(configMap.contains("db-edge-rw.database-edge.svc.cluster.local:6432"),
                "Overlay soberano deve manter datasource principal passando pelo db-edge-rw interno.");
        assertTrue(configMap.contains("db-edge-ro.database-edge.svc.cluster.local:6432"),
                "Overlay soberano deve manter datasource de leitura passando pelo db-edge-ro interno.");
        assertTrue(networkPolicy.contains("kubernetes.io/metadata.name: database-edge"),
                "NetworkPolicy soberana deve manter egress de banco restrito ao namespace database-edge.");
        assertTrue(networkPolicy.contains("port: 6432"),
                "NetworkPolicy soberana deve manter o edge TCP interno do banco na porta 6432.");
    }
}
