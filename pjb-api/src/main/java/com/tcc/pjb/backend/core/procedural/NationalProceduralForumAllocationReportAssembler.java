package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.preflight.ProceduralPreflightEngine;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralForumAllocationReportAssembler {

    private final NationalProceduralTerritorialAnalysisFactory territorialAnalysisFactory;

    public NationalProceduralForumAllocationReportAssembler(NationalProceduralTerritorialAnalysisFactory territorialAnalysisFactory) {
        this.territorialAnalysisFactory = Objects.requireNonNull(territorialAnalysisFactory);
    }

    ProceduralForumAllocationReport assemble(NationalProceduralForumAllocationContext context,
                                             NationalProceduralForumAllocationSeed seed,
                                             NationalProceduralForumRoutingReadiness readiness) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(seed);
        Objects.requireNonNull(readiness);
        TribunalProtocolRoutingService.RoutingDecision routingDecision = readiness.routingDecision();
        JudicialSubmissionCapability capability = readiness.capability();
        ProceduralPreflightEngine.PreflightResult preflight = readiness.preflight();

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("territorial", seed.territorial().metadata());
        metadata.put("linkage", seed.linkage().metadata());
        if (routingDecision != null) {
            LinkedHashMap<String, Object> capabilityMetadata = new LinkedHashMap<>();
            if (capability != null) {
                capabilityMetadata.put("enabled", capability.enabled());
                capabilityMetadata.put("supportsProtocol", capability.supportsProtocol());
                capabilityMetadata.put("supportsDryRun", capability.supportsDryRun());
                capabilityMetadata.put("requiresStepUpGovBr", capability.requiresStepUpGovBr());
                capabilityMetadata.put("requiresCertificate", capability.requiresCertificate());
                capabilityMetadata.put("baseUrl", capability.baseUrl());
            }
            LinkedHashMap<String, Object> protocolRoutingMetadata = new LinkedHashMap<>();
            protocolRoutingMetadata.put("tribunalCodigo", routingDecision.tribunalCodigo());
            protocolRoutingMetadata.put("tribunalNome", routingDecision.tribunalNome());
            protocolRoutingMetadata.put("judicialSystem", routingDecision.judicialSystem() != null ? routingDecision.judicialSystem().name() : null);
            protocolRoutingMetadata.put("capability", capabilityMetadata);
            protocolRoutingMetadata.put("competenceHint", routingDecision.competenceHint());
            protocolRoutingMetadata.put("warnings", routingDecision.warnings());
            protocolRoutingMetadata.put("metadata", routingDecision.metadata());
            metadata.put("protocolRouting", protocolRoutingMetadata);
        }
        if (preflight != null) {
            LinkedHashMap<String, Object> preflightMetadata = new LinkedHashMap<>();
            preflightMetadata.put("requestId", preflight.requestId());
            preflightMetadata.put("readyForSubmission", preflight.readyForSubmission());
            preflightMetadata.put("resolvedRito", preflight.resolvedRito());
            preflightMetadata.put("resolvedClasseTpu", preflight.resolvedClasseTpu());
            preflightMetadata.put("resolvedTribunalCodigo", preflight.resolvedTribunalCodigo());
            preflightMetadata.put("resolvedConnector", preflight.resolvedConnector());
            preflightMetadata.put("missingParties", preflight.missingParties());
            preflightMetadata.put("missingDocuments", preflight.missingDocuments());
            List<ProceduralPreflightEngine.PreflightIssue> issues = preflight.issues() == null ? List.of() : preflight.issues();
            preflightMetadata.put("issues", issues.stream().map(ProceduralPreflightEngine.PreflightIssue::toMap).toList());
            preflightMetadata.put("metadata", preflight.metadata());
            metadata.put("preflight", preflightMetadata);
        }
        if (seed.perfil() != null) {
            LinkedHashMap<String, Object> varaPerfilMetadata = new LinkedHashMap<>();
            varaPerfilMetadata.put("varaId", seed.perfil().varaId());
            varaPerfilMetadata.put("varaDescricao", seed.perfil().varaDescricao());
            varaPerfilMetadata.put("tipoVara", seed.perfil().tipoVara() != null ? seed.perfil().tipoVara().name() : null);
            varaPerfilMetadata.put("tribunalCodigo", seed.perfil().tribunalCodigo());
            varaPerfilMetadata.put("comarcaId", seed.perfil().comarcaId());
            varaPerfilMetadata.put("uf", seed.perfil().uf());
            varaPerfilMetadata.put("modoOperacao", seed.perfil().modoOperacao() != null ? seed.perfil().modoOperacao().name() : null);
            varaPerfilMetadata.put("statusOperacional", seed.perfil().statusOperacional() != null ? seed.perfil().statusOperacional().name() : null);
            varaPerfilMetadata.put("scoreDisponibilidade", seed.perfil().scoreDisponibilidade());
            metadata.put("varaPerfil", varaPerfilMetadata);
        }

        Map<String, Object> safeMetadata = PayloadMaps.deepCopyWithoutNulls(metadata);

        return new ProceduralForumAllocationReport(
                Instant.now(),
                seed.classeTpu() != null ? String.valueOf(seed.classeTpu().codigoTpu()) : context.canonical().classeTpuCodigo(),
                seed.classeTpu() != null ? seed.classeTpu().descricao() : context.canonical().classeTpuNome(),
                seed.territorial().mode(),
                seed.comarca(),
                seed.uf(),
                seed.territorial().reason(),
                seed.linkage().preventionMode(),
                seed.linkage().linkageMode(),
                seed.linkage().relatedProcessNumbers(),
                seed.tribunalCodigo(),
                seed.tribunalNome(),
                seed.unidadeCodigo(),
                seed.varaSugerida(),
                seed.tipoVara(),
                territorialAnalysisFactory.detectSpecializedVara(seed.tipoVara(), seed.varaSugerida(), context.actionProfile().varaFamily()),
                seed.perfil() != null ? seed.perfil().permiteDistribuicaoAutomatica() : context.distribution() != null,
                NationalProceduralRoutingSupport.round(Math.max(0.0d, seed.distributionScore())),
                routingDecision != null && routingDecision.judicialSystem() != null ? routingDecision.judicialSystem().name() : null,
                readiness.connectorOperational(),
                capability != null && capability.requiresStepUpGovBr(),
                capability != null && capability.requiresCertificate(),
                readiness.protocoloRealDisponivel(),
                readiness.preProtocoloApto(),
                readiness.preProtocoloStatus(),
                readiness.incompatibilities(),
                readiness.warnings(),
                readiness.reviewChecklist(),
                safeMetadata
        );
    }
}
