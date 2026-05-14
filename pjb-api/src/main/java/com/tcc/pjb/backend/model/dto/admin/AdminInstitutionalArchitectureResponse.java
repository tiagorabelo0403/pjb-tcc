package com.tcc.pjb.backend.model.dto.admin;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AdminInstitutionalArchitectureResponse(
        String blueprintVersion,
        Instant generatedAt,
        Topology topology,
        Activation activation,
        Visibility visibility,
        Identity identity,
        Federation federation,
        Readiness readiness
) {

    public record Topology(
            List<TopologyLayer> judicialHierarchy,
            List<ExternalInstitutionNetwork> connectedNetworks,
            Map<String, Integer> officialCounts,
            CoverageSnapshot currentCoverage,
            List<String> guarantees
    ) {
    }

    public record TopologyLayer(
            int order,
            String code,
            String label,
            String scope,
            String defaultChild
    ) {
    }

    public record ExternalInstitutionNetwork(
            String code,
            String label,
            String scope,
            String integrationMode
    ) {
    }

    public record CoverageSnapshot(
            int catalogUnits,
            int catalogKinds,
            int activeUfCoverage,
            int governanceEntries,
            int governanceKindsCovered,
            List<String> highlights
    ) {
    }

    public record Activation(
            List<ActivationLayer> layers,
            List<ActivationSource> sources,
            List<String> guarantees,
            List<String> missingCapabilities
    ) {
    }

    public record ActivationLayer(
            int order,
            String name,
            String mode,
            String currentStatus,
            List<String> responsibilities
    ) {
    }

    public record ActivationSource(
            String code,
            String label,
            String usage,
            boolean readyInCode
    ) {
    }

    public record Visibility(
            List<VisibilityTier> tiers,
            VisibilitySimulation defaultSimulation,
            List<String> guarantees,
            List<String> missingCapabilities
    ) {
    }

    public record VisibilityTier(
            int level,
            String code,
            String label,
            String grantModel,
            boolean auditRequired
    ) {
    }

    public record VisibilitySimulation(
            String tierCode,
            String tierLabel,
            boolean allowed,
            boolean auditRequired,
            boolean timeBound,
            List<String> reasons,
            List<String> restrictions
    ) {
    }

    public record Identity(
            List<IdentityRail> rails,
            boolean govBrEntryReady,
            boolean govBrIdentityReady,
            boolean tokenVerificationReady,
            boolean strongBindingReady,
            List<String> federationProtocols,
            List<String> guarantees,
            List<String> missingCapabilities
    ) {
    }

    public record IdentityRail(
            String code,
            String audience,
            String assurance,
            List<String> factors,
            List<String> privileges,
            List<String> restrictions
    ) {
    }

    public record Federation(
            List<ShardCluster> clusters,
            String metadataStoreModel,
            String defaultCluster,
            ClusterResolution defaultResolution,
            List<String> guarantees,
            List<String> missingCapabilities,
            Map<String, Object> health
    ) {
    }

    public record ShardCluster(
            String code,
            String label,
            List<String> tribunalPrefixes,
            List<String> ufs,
            String routingRule
    ) {
    }

    public record ClusterResolution(
            String clusterCode,
            String clusterLabel,
            String metadataKey,
            boolean federated,
            List<String> reasons
    ) {
    }

    public record Readiness(
            List<String> implemented,
            List<String> novasFuncionalidades,
            List<String> stillMissing,
            List<String> nextSafeSteps
    ) {
    }
}
