package com.tcc.pjb.backend.judicial.connectors.application;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.judicial.connectors.domain.JudicialConnectorStructuralArea;
import com.tcc.pjb.backend.judicial.connectors.domain.JudicialConnectorStructureNode;
import com.tcc.pjb.backend.judicial.connectors.domain.JudicialConnectorStructureReport;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorStructureService {

    public JudicialConnectorStructureReport report() {
        List<JudicialConnectorStructureNode> nodes = List.of(
                new JudicialConnectorStructureNode(
                        "governance",
                        JudicialConnectorStructuralArea.GOVERNANCE,
                        "com.tcc.pjb.backend.judicial.connectors.application",
                        List.of(
                                "com.tcc.pjb.backend.integration.judicial",
                                "com.tcc.pjb.backend.controller.admin"
                        ),
                        List.of(
                                "JudicialConnectorGovernanceService",
                                "JudicialConnectorControlPlaneService",
                                "JudicialConnectorPolicyService",
                                "JudicialConnectorCommandCenterService"
                        ),
                        List.of(
                                "governança nacional por sistema e tribunal",
                                "postura de produção, homologação e bloqueio",
                                "overlay persistido de política operacional",
                                "agregação administrativa nacional"
                        ),
                        Map.of(
                                "priority", 1,
                                "boundedContext", "judicial-connectors-governance"
                        )
                ),
                new JudicialConnectorStructureNode(
                        "routing-and-operations",
                        JudicialConnectorStructuralArea.OPERATIONS,
                        "com.tcc.pjb.backend.judicial.connectors.application",
                        List.of(
                                "com.tcc.pjb.backend.integration.judicial",
                                "com.tcc.pjb.backend.integration.judicial.routing"
                        ),
                        List.of(
                                "TribunalProtocolRoutingService",
                                "JudicialProtocolSubmissionService",
                                "JudicialConnectorDataPlaneService",
                                "JudicialConnectorAdminOpsService"
                        ),
                        List.of(
                                "roteamento por tribunal e sistema judicial",
                                "submissão protocolar com evidência operacional",
                                "execução administrativa controlada",
                                "telemetria transacional por horizonte"
                        ),
                        Map.of(
                                "priority", 2,
                                "boundedContext", "judicial-connectors-operations"
                        )
                ),
                new JudicialConnectorStructureNode(
                        "security-and-cryptography",
                        JudicialConnectorStructuralArea.SECURITY,
                        "com.tcc.pjb.backend.judicial.connectors.application",
                        List.of(
                                "com.tcc.pjb.backend.integration.judicial.security",
                                "com.tcc.pjb.backend.controller.admin"
                        ),
                        List.of(
                                "JudicialConnectorCryptographicContextService",
                                "JudicialConnectorCertificateValidationService",
                                "JudicialConnectorCertificateInventoryService",
                                "JudicialConnectorCryptoCommandCenterService",
                                "JudicialConnectorSecuritySessionService"
                        ),
                        List.of(
                                "mTLS, PKCS#11, A3 e HSM",
                                "inventário persistido de certificados",
                                "telemetria de segurança e trilha de falha criptográfica",
                                "histórico de sessões seguras por tribunal"
                        ),
                        Map.of(
                                "priority", 3,
                                "boundedContext", "judicial-connectors-security"
                        )
                ),
                new JudicialConnectorStructureNode(
                        "administration",
                        JudicialConnectorStructuralArea.ADMINISTRATION,
                        "com.tcc.pjb.backend.judicial.connectors.api.admin",
                        List.of(
                                "com.tcc.pjb.backend.controller.admin"
                        ),
                        List.of(
                                "AdminJudicialConnectorCommandCenterController",
                                "AdminJudicialConnectorCryptoCommandCenterController",
                                "AdminJudicialConnectorPolicyController",
                                "AdminJudicialConnectorOpsController"
                        ),
                        List.of(
                                "superfície administrativa nacional",
                                "consulta consolidada operacional e criptográfica",
                                "comandos administrativos persistidos",
                                "ponto de migração para APIs orientadas a domínio"
                        ),
                        Map.of(
                                "priority", 4,
                                "boundedContext", "judicial-connectors-admin"
                        )
                ),
                new JudicialConnectorStructureNode(
                        "observability",
                        JudicialConnectorStructuralArea.OBSERVABILITY,
                        "com.tcc.pjb.backend.judicial.connectors.application",
                        List.of(
                                "com.tcc.pjb.backend.integration.judicial",
                                "com.tcc.pjb.backend.integration.judicial.security"
                        ),
                        List.of(
                                "JudicialConnectorObservabilityService",
                                "JudicialConnectorTelemetryService",
                                "JudicialConnectorSecurityTelemetryService",
                                "JudicialConnectorCryptoHealthIndicator"
                        ),
                        List.of(
                                "métricas de conector e operação",
                                "leitura de degradação e bloqueio",
                                "sinais de segurança para Prometheus",
                                "indicadores de saúde administrativa"
                        ),
                        Map.of(
                                "priority", 5,
                                "boundedContext", "judicial-connectors-observability"
                        )
                )
        );
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nodeCount", nodes.size());
        metadata.put("legacyRootCount", 3);
        metadata.put("recommendedAdminApiRoot", "/api/admin/judicial/connectors/hub");
        metadata.put("recommendedPackageMigrationRoot", "com.tcc.pjb.backend.judicial.connectors");
        metadata.put("migrationState", "CONSOLIDATED_ENTRYPOINTS_ESTABLISHED");
        return new JudicialConnectorStructureReport(
                Instant.now(),
                "com.tcc.pjb.backend.judicial.connectors",
                List.of(
                        "com.tcc.pjb.backend.integration.judicial",
                        "com.tcc.pjb.backend.integration.judicial.security",
                        "com.tcc.pjb.backend.controller.admin"
                ),
                nodes,
                List.of(
                        "/api/admin/judicial/connectors/hub/national",
                        "/api/admin/judicial/connectors/hub/tribunal/{tribunalCodigo}",
                        "/api/admin/judicial/connectors/hub/structure"
                ),
                Map.copyOf(JudicialMapSupport.copyNonNull(metadata))
        );
    }
}
