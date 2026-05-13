package com.tcc.pjb.backend.governance.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;
import org.junit.jupiter.api.Test;

class KubernetesZeroTrustDataPlaneOverlayGovernanceTest {

    private static final Path ROOT = PjbTestPaths.projectRoot();
    private static final Path KUSTOMIZATION = ROOT.resolve("infra/k8s/overlays/prod-sovereign-zero-trust-data-plane/kustomization.yaml");
    private static final Path CONFIGMAP_PATCH = ROOT.resolve("infra/k8s/overlays/prod-sovereign-zero-trust-data-plane/patch-configmap.yaml");
    private static final Path API_NETWORK_PATCH = ROOT.resolve("infra/k8s/overlays/prod-sovereign-zero-trust-data-plane/patch-api-network-policy.yaml");
    private static final Path WORKER_NETWORK_PATCH = ROOT.resolve("infra/k8s/overlays/prod-sovereign-zero-trust-data-plane/patch-worker-network-policy.yaml");

    @Test
    void zeroTrustDataPlaneOverlayMustInheritOperationalResilienceMesh() throws IOException {
        String kustomization = Files.readString(KUSTOMIZATION);
        assertTrue(kustomization.contains("../prod-sovereign-operational-resilience-mesh"));
        assertTrue(kustomization.contains("patch-configmap.yaml"));
        assertTrue(kustomization.contains("patch-api-network-policy.yaml"));
        assertTrue(kustomization.contains("patch-worker-network-policy.yaml"));
    }

    @Test
    void zeroTrustDataPlaneOverlayMustRouteDatabaseTrafficThroughDbEdgeAndReadRouting() throws IOException {
        String configmap = Files.readString(CONFIGMAP_PATCH);
        assertTrue(configmap.contains("jdbc:postgresql://db-edge-rw.database-edge.svc.cluster.local:6432/pjb"));
        assertTrue(configmap.contains("PJB_DB_LOAD_BALANCER_MODE: DB_EDGE_TCP"));
        assertTrue(configmap.contains("PJB_DB_READ_ROUTING_ENABLED: \"true\""));
        assertTrue(configmap.contains("jdbc:postgresql://db-edge-ro.database-edge.svc.cluster.local:6432/pjb"));
    }

    @Test
    void zeroTrustDataPlaneOverlayMustRestrictAppEgressToDatabaseEdgeNamespace() throws IOException {
        String api = Files.readString(API_NETWORK_PATCH);
        String worker = Files.readString(WORKER_NETWORK_PATCH);
        assertTrue(api.contains("kubernetes.io/metadata.name: database-edge"));
        assertTrue(api.contains("port: 6432"));
        assertTrue(worker.contains("kubernetes.io/metadata.name: database-edge"));
        assertTrue(worker.contains("port: 6432"));
    }
}
