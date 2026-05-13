package com.tcc.pjb.backend.governance.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;
import org.junit.jupiter.api.Test;

class KubernetesSovereignFapiGatewayOverlayGovernanceTest {

    private static final Path ROOT = PjbTestPaths.projectRoot();
    private static final Path KUSTOMIZATION = ROOT.resolve("infra/k8s/overlays/prod-sovereign-fapi-gateway/kustomization.yaml");
    private static final Path GATEWAY = ROOT.resolve("infra/k8s/overlays/prod-sovereign-fapi-gateway/gateway.yaml");
    private static final Path ROUTE = ROOT.resolve("infra/k8s/overlays/prod-sovereign-fapi-gateway/httproute-api.yaml");
    private static final Path BACKEND_TLS = ROOT.resolve("infra/k8s/overlays/prod-sovereign-fapi-gateway/backendtlspolicy-api.yaml");

    @Test
    void overlayMustReuseSpiffeTrustPlaneAndExposeGatewayApiResources() throws IOException {
        String kustomization = Files.readString(KUSTOMIZATION);
        assertTrue(kustomization.contains("../prod-sovereign-spiffe-trust-plane"));
        assertTrue(kustomization.contains("gateway.yaml"));
        assertTrue(kustomization.contains("httproute-api.yaml"));
        assertTrue(kustomization.contains("backendtlspolicy-api.yaml"));
    }

    @Test
    void gatewayOverlayMustRouteApiTrafficThroughSovereignGateway() throws IOException {
        String gateway = Files.readString(GATEWAY);
        String route = Files.readString(ROUTE);
        assertTrue(gateway.contains("kind: Gateway"));
        assertTrue(gateway.contains("gatewayClassName: pjb-sovereign-gateway"));
        assertTrue(route.contains("kind: HTTPRoute"));
        assertTrue(route.contains("api.pjb.jus.br"));
        assertTrue(route.contains("name: pjb-api"));
    }

    @Test
    void gatewayOverlayMustProtectBackendHopWithBackendTlsAndSpiffeSan() throws IOException {
        String backendTls = Files.readString(BACKEND_TLS);
        assertTrue(backendTls.contains("kind: BackendTLSPolicy"));
        assertTrue(backendTls.contains("wellKnownCACertificates: System"));
        assertTrue(backendTls.contains("hostname: pjb-api.pjb.svc.cluster.local"));
        assertTrue(backendTls.contains("uri: spiffe://pjb.jus.br/pjb/workload/api"));
    }
}
