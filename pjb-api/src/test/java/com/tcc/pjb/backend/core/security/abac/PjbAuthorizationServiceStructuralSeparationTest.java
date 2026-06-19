package com.tcc.pjb.backend.core.security.abac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbAuthorizationServiceStructuralSeparationTest {

    private static final Path SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationService.java");
    private static final Path POLICY_FACADE = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationPolicyFacade.java");
    private static final Path SIGILO_RESOLVER = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationSigiloResolver.java");
    private static final Path AUDIT_FACADE = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationAuditFacade.java");
    private static final Path EXTERNAL_POLICY = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationExternalSystemAccessPolicy.java");
    private static final Path INSTITUTIONAL_FACADE = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationInstitutionalCapabilityFacade.java");
    private static final Path SENSITIVE_INTEGRATION_FACADE = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationSensitiveIntegrationFacade.java");
    private static final Path TRAIL_ASSEMBLER = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailAssembler.java");
    private static final Path TRAIL_REGISTRY = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailRegistry.java");
    private static final Path TRAIL_ADMIN_SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailAdminService.java");
    private static final Path TRAIL_READ_MODEL_SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailReadModelService.java");
    private static final Path TRAIL_READ_MODEL_REPOSITORY = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailReadModelRepository.java");
    private static final Path TRAIL_ANALYTICS_SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailAnalyticsService.java");
    private static final Path TRAIL_ANALYTICS_MATERIALIZATION_SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailAnalyticsMaterializationService.java");
    private static final Path TRAIL_ANALYTICS_ASSEMBLER = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailAnalyticsMaterializationAssembler.java");
    private static final Path TRAIL_ANALYTICS_INCREMENTAL_REFRESH_SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailAnalyticsIncrementalRefreshService.java");
    private static final Path TRAIL_ANALYTICS_REFRESH_QUEUE_SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailAnalyticsRefreshQueueService.java");
    private static final Path TRAIL_ANALYTICS_REFRESH_SCHEDULER = Path.of("src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailAnalyticsRefreshScheduler.java");

    @Test
    void mustKeepAuthorizationServiceAsShortOrchestrator() throws Exception {
        String source = Files.readString(SERVICE, StandardCharsets.UTF_8);
        assertTrue(source.contains("private final PjbAuthorizationPolicyFacade policyFacade;"));
        assertTrue(source.contains("private final PjbAuthorizationSigiloResolver sigiloResolver;"));
        assertTrue(source.contains("private final PjbAuthorizationAuditFacade auditFacade;"));
        assertTrue(source.contains("private final PjbAuthorizationExternalSystemAccessPolicy externalSystemAccessPolicy;"));
        assertTrue(source.contains("private final PjbAuthorizationInstitutionalCapabilityFacade institutionalCapabilityFacade;"));
        assertTrue(source.contains("private final PjbAuthorizationSensitiveIntegrationFacade sensitiveIntegrationFacade;"));
        assertTrue(source.contains("PjbAuthorizationTrailRegistry authorizationTrailRegistry"));
        assertTrue(source.contains("PjbAuthorizationTrailReadModelService authorizationTrailReadModelService"));
        assertTrue(source.contains("PjbAuthorizationTrailAnalyticsRefreshQueueService authorizationTrailAnalyticsRefreshQueueService"));
        assertFalse(source.contains("private static NivelSigilo maxSigilo("));
        assertFalse(source.contains("private static Processo shadowSigilo("));
        assertFalse(source.contains("private NivelSigilo computeDocumentoSigiloEfetivo("));
        assertFalse(source.contains("private void requireStepUpForSigiloEfetivo("));
        assertFalse(source.contains("private boolean jwtHasMfaClaim("));
        assertFalse(source.contains("private boolean canConsultBnmpInternal("));
        assertFalse(source.contains("private boolean canRequestInfojudInternal("));
    }

    @Test
    void mustKeepDecisionSigiloAuditTrailAndExternalAccessInDedicatedCollaborators() throws Exception {
        String policyFacade = Files.readString(POLICY_FACADE, StandardCharsets.UTF_8);
        String sigiloResolver = Files.readString(SIGILO_RESOLVER, StandardCharsets.UTF_8);
        String auditFacade = Files.readString(AUDIT_FACADE, StandardCharsets.UTF_8);
        String externalPolicy = Files.readString(EXTERNAL_POLICY, StandardCharsets.UTF_8);
        String institutionalFacade = Files.readString(INSTITUTIONAL_FACADE, StandardCharsets.UTF_8);
        String sensitiveIntegrationFacade = Files.readString(SENSITIVE_INTEGRATION_FACADE, StandardCharsets.UTF_8);
        String trailAssembler = Files.readString(TRAIL_ASSEMBLER, StandardCharsets.UTF_8);
        String trailRegistry = Files.readString(TRAIL_REGISTRY, StandardCharsets.UTF_8);
        String trailAdminService = Files.readString(TRAIL_ADMIN_SERVICE, StandardCharsets.UTF_8);
        String trailReadModelService = Files.readString(TRAIL_READ_MODEL_SERVICE, StandardCharsets.UTF_8);
        String trailReadModelRepository = Files.readString(TRAIL_READ_MODEL_REPOSITORY, StandardCharsets.UTF_8);
        String trailAnalyticsService = Files.readString(TRAIL_ANALYTICS_SERVICE, StandardCharsets.UTF_8);
        String trailAnalyticsMaterializationService = Files.readString(TRAIL_ANALYTICS_MATERIALIZATION_SERVICE, StandardCharsets.UTF_8);
        String trailAnalyticsAssembler = Files.readString(TRAIL_ANALYTICS_ASSEMBLER, StandardCharsets.UTF_8);
        String trailAnalyticsIncrementalRefreshService = Files.readString(TRAIL_ANALYTICS_INCREMENTAL_REFRESH_SERVICE, StandardCharsets.UTF_8);
        String trailAnalyticsRefreshQueueService = Files.readString(TRAIL_ANALYTICS_REFRESH_QUEUE_SERVICE, StandardCharsets.UTF_8);
        String trailAnalyticsRefreshScheduler = Files.readString(TRAIL_ANALYTICS_REFRESH_SCHEDULER, StandardCharsets.UTF_8);
        assertTrue(policyFacade.contains("private final PjbAuthorizationTrailAssembler trailAssembler;"));
        assertTrue(policyFacade.contains("PjbAuthorizationEvaluation evaluateReadProcesso("));
        assertTrue(policyFacade.contains("PjbAuthorizationEvaluation evaluateWriteProcesso("));
        assertTrue(sigiloResolver.contains("PjbAuthorizationStepUpAssessment assessStepUpForSigiloEfetivo("));
        assertTrue(auditFacade.contains("void registerDecision(PjbAuthorizationEvaluation evaluation)"));
        assertTrue(auditFacade.contains("trailRegistry.register(trail);"));
        assertTrue(auditFacade.contains("trailReadModelService.materializeReturningSnapshot(trail);"));
        assertTrue(auditFacade.contains("refreshQueueService.enqueueSnapshot(persistedSnapshot);"));
        assertTrue(auditFacade.contains("void registerGrantedReadProcesso("));
        assertTrue(externalPolicy.contains("boolean canRequestInfojud("));
        assertTrue(externalPolicy.contains("boolean canConsultarMandadosPessoa("));
        assertTrue(institutionalFacade.contains("PjbAuthorizationEvaluation evaluate("));
        assertTrue(sensitiveIntegrationFacade.contains("PjbAuthorizationEvaluation evaluateRequestInfojud("));
        assertTrue(sensitiveIntegrationFacade.contains("PjbAuthorizationEvaluation evaluateAccessEnderecoEstrito("));
        assertTrue(trailAssembler.contains("PjbAuthorizationEvaluation assembleProcessRead("));
        assertTrue(trailAssembler.contains("PjbAuthorizationEvaluation assembleInstitutionalCapability("));
        assertTrue(trailAssembler.contains("PjbAuthorizationEvaluation assembleExternalAccess("));
        assertTrue(trailAssembler.contains("PjbAuthorizationEvaluation assembleProcessWrite("));
        assertTrue(trailRegistry.contains("public void register(PjbAuthorizationDecisionTrail trail)"));
        assertTrue(trailRegistry.contains("public List<PjbAuthorizationTrailSnapshot> search(PjbAuthorizationTrailQueryCriteria criteria)"));
        assertTrue(trailAdminService.contains("public PjbAuthorizationTrailQueryResponse search("));
        assertTrue(trailAdminService.contains("PjbAuthorizationTrailSourceMode sourceMode"));
        assertTrue(trailReadModelService.contains("public void materialize(PjbAuthorizationDecisionTrail trail)"));
        assertTrue(trailReadModelService.contains("public List<PjbAuthorizationTrailSnapshot> search(PjbAuthorizationTrailQueryCriteria criteria)"));
        assertTrue(trailAnalyticsService.contains("public PjbAuthorizationTrailAnalyticsResponse analytics("));
        assertTrue(trailAnalyticsService.contains("public PjbAuthorizationTrailAnalyticsMaterializationResponse materialize("));
        assertTrue(trailAnalyticsService.contains("public PjbAuthorizationTrailAnalyticsMaterializationResponse refreshBucket("));
        assertTrue(trailAnalyticsService.contains("public PjbAuthorizationTrailAnalyticsOperationalStatusResponse status("));
        assertTrue(trailAnalyticsService.contains("public PjbAuthorizationTrailAnalyticsRefreshQueueStatusResponse queueStatus("));
        assertTrue(trailAnalyticsService.contains("public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse drainQueue("));
        assertTrue(trailAnalyticsService.contains("public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse backfillQueue("));
        assertTrue(trailAnalyticsService.contains("public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse recoverStaleQueue("));
        assertTrue(trailAnalyticsService.contains("public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse cleanupCompletedQueue("));
        assertTrue(trailAnalyticsMaterializationService.contains("public PjbAuthorizationTrailAnalyticsMaterializationResponse materialize("));
        assertTrue(trailAnalyticsMaterializationService.contains("private final PjbAuthorizationTrailAnalyticsMaterializationAssembler materializationAssembler;"));
        assertTrue(trailAnalyticsMaterializationService.contains("analyticsRepository.existsByAnalyticsKey(entry.getAnalyticsKey())"));
        assertTrue(trailAnalyticsAssembler.contains("public List<PjbAuthorizationTrailAnalyticsEntry> assemble("));
        assertTrue(trailAnalyticsIncrementalRefreshService.contains("public PjbAuthorizationTrailAnalyticsMaterializationResponse refreshBucket("));
        assertTrue(trailAnalyticsIncrementalRefreshService.contains("analyticsRepository.findByAnalyticsKey(incoming.getAnalyticsKey())"));
        assertTrue(trailAnalyticsRefreshQueueService.contains("public void enqueueSnapshot(PjbAuthorizationTrailSnapshot snapshot)"));
        assertTrue(trailAnalyticsRefreshQueueService.contains("public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse drain("));
        assertTrue(trailAnalyticsRefreshQueueService.contains("public PjbAuthorizationTrailAnalyticsRefreshQueueStatusResponse status("));
        assertTrue(trailAnalyticsRefreshQueueService.contains("public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse recoverStaleProcessing("));
        assertTrue(trailAnalyticsRefreshQueueService.contains("public PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse cleanupCompleted("));
        assertTrue(trailAnalyticsRefreshScheduler.contains("@Scheduled(fixedDelayString = \"${pjb.authz.analytics.refresh.fixed-delay-ms:5000}\")"));
        assertTrue(trailAnalyticsRefreshScheduler.contains("@Scheduled(fixedDelayString = \"${pjb.authz.analytics.refresh.cleanup-fixed-delay-ms:3600000}\")"));
        assertFalse(trailReadModelRepository.contains("existsByPayloadHash"));
        assertTrue(trailReadModelRepository.contains("findByOccurredAtWindowWithMonthRange"));
        assertTrue(trailReadModelRepository.contains("searchForensics"));
    }
}
