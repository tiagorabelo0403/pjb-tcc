package com.tcc.pjb.backend.service.secretariat.query.queue;

import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalActionModelService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalDeskModelService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalTransactionModelService;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatInstitutionalAlignmentService;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatJudicialReferenceModelService;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatFlowBridgeProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatHearingMediaLaneService;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatJudicialIntegrationProfile;
import com.tcc.pjb.backend.service.secretariat.query.routing.SecretariatMigrationLaneService;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SecretariatQueueSummaryAssembler {

    private final SecretariatJudicialReferenceModelService referenceModelService;
    private final SecretariatInstitutionalAlignmentService institutionalAlignmentService;
    private final SecretariatOperationalDeskModelService operationalDeskModelService;
    private final SecretariatOperationalActionModelService operationalActionModelService;
    private final SecretariatOperationalTransactionModelService operationalTransactionModelService;
    private final SecretariatMigrationLaneService migrationLaneService;
    private final SecretariatHearingMediaLaneService hearingMediaLaneService;

    public SecretariatQueueSummaryAssembler(SecretariatJudicialReferenceModelService referenceModelService,
                                            SecretariatInstitutionalAlignmentService institutionalAlignmentService,
                                            SecretariatOperationalDeskModelService operationalDeskModelService,
                                            SecretariatOperationalActionModelService operationalActionModelService,
                                            SecretariatOperationalTransactionModelService operationalTransactionModelService,
                                            SecretariatMigrationLaneService migrationLaneService,
                                            SecretariatHearingMediaLaneService hearingMediaLaneService) {
        this.referenceModelService = Objects.requireNonNull(referenceModelService);
        this.institutionalAlignmentService = Objects.requireNonNull(institutionalAlignmentService);
        this.operationalDeskModelService = Objects.requireNonNull(operationalDeskModelService);
        this.operationalActionModelService = Objects.requireNonNull(operationalActionModelService);
        this.operationalTransactionModelService = Objects.requireNonNull(operationalTransactionModelService);
        this.migrationLaneService = Objects.requireNonNull(migrationLaneService);
        this.hearingMediaLaneService = Objects.requireNonNull(hearingMediaLaneService);
    }

    public SecretariatQueueSummaryProjection assemble(SecretariatQueueInboxContext context,
                                                      SecretariatFlowBridgeProfile bridgeProfile,
                                                      SecretariatJudicialIntegrationProfile integrationProfile) {
        SecretariatJudicialReferenceModelService.ReferenceModelSnapshot referenceSnapshot = referenceModelService.resolve(
            context.inboxKey(),
            null,
            context.portfolio(),
            context.deskProfile(),
            bridgeProfile,
            integrationProfile
        );
        SecretariatInstitutionalAlignmentService.InstitutionalAlignmentSnapshot institutionalSnapshot = institutionalAlignmentService.resolve(
            context.inboxKey(),
            null,
            context.inboxProfile().specialization(),
            bridgeProfile,
            integrationProfile
        );
        SecretariatOperationalDeskModelService.OperationalDeskSnapshot operationalDeskSnapshot = operationalDeskModelService.resolve(
            context.inboxKey(),
            null,
            context.inboxProfile().specialization(),
            context.portfolio(),
            context.deskProfile(),
            bridgeProfile,
            integrationProfile
        );
        SecretariatOperationalActionModelService.OperationalActionSnapshot operationalActionSnapshot = operationalActionModelService.resolve(
            context.inboxKey(),
            null,
            context.inboxProfile().specialization(),
            operationalDeskSnapshot,
            integrationProfile
        );
        SecretariatOperationalTransactionModelService.OperationalTransactionSnapshot operationalTransactionSnapshot = operationalTransactionModelService.resolve(
            operationalDeskSnapshot.journeyMode()
        );
        SecretariatMigrationLaneService.MigrationLaneSnapshot migrationSnapshot = migrationLaneService.resolve(
            context.inboxKey(),
            null,
            null,
            List.of(),
            context.portfolio(),
            bridgeProfile,
            integrationProfile
        );
        SecretariatHearingMediaLaneService.HearingMediaLaneSnapshot hearingMediaSnapshot = hearingMediaLaneService.resolve(
            context.inboxKey(),
            null,
            null,
            List.of(),
            context.portfolio(),
            bridgeProfile,
            integrationProfile
        );
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(context.loadProfile().toMap());
        metadata.putAll(context.deskProfile().toMap());
        metadata.putAll(context.portfolio().toMap());
        metadata.putAll(bridgeProfile.toMap());
        metadata.putAll(integrationProfile.toMap());
        metadata.put("institutionalVisibility", context.inboxProfile().toMap());
        metadata.put("referenceModels", referenceSnapshot.models());
        metadata.put("referenceGaps", referenceSnapshot.gaps());
        metadata.put("referenceDiagnostics", referenceSnapshot.diagnostics());
        metadata.put("institutionalCells", institutionalSnapshot.cells());
        metadata.put("institutionalTouchpoints", institutionalSnapshot.touchpoints());
        metadata.put("institutionalGaps", institutionalSnapshot.gaps());
        metadata.put("institutionalDiagnostics", institutionalSnapshot.diagnostics());
        metadata.put("operationalJourneyMode", operationalDeskSnapshot.journeyMode());
        metadata.put("operationalDesks", operationalDeskSnapshot.desks());
        metadata.put("operationalDeskGaps", operationalDeskSnapshot.gaps());
        metadata.put("operationalDeskDiagnostics", operationalDeskSnapshot.diagnostics());
        metadata.put("operationalActions", operationalActionSnapshot.actions());
        metadata.put("operationalActionGaps", operationalActionSnapshot.gaps());
        metadata.put("operationalActionDiagnostics", operationalActionSnapshot.diagnostics());
        metadata.put("operationalTransactions", operationalTransactionSnapshot.transactions());
        metadata.put("operationalTransactionDiagnostics", operationalTransactionSnapshot.diagnostics());
        metadata.put("migrationReadiness", migrationSnapshot.readiness());
        metadata.put("migrationDecision", migrationSnapshot.migrationDecision());
        metadata.put("migrationConnectorDecision", migrationSnapshot.connectorDecision());
        metadata.put("migrationTargetDesk", migrationSnapshot.targetDesk());
        metadata.put("migrationBlockers", migrationSnapshot.blockers());
        metadata.put("migrationSanitationActions", migrationSnapshot.sanitationActions());
        metadata.put("migrationAutomationOpportunities", migrationSnapshot.automationOpportunities());
        metadata.put("migrationDiagnostics", migrationSnapshot.diagnostics());
        metadata.put("hearingMediaTargetDesk", hearingMediaSnapshot.targetDesk());
        metadata.put("hearingMediaIndexingMode", hearingMediaSnapshot.indexingMode());
        metadata.put("hearingMediaAgendaReflection", hearingMediaSnapshot.agendaReflection());
        metadata.put("hearingMediaConnectorDecision", hearingMediaSnapshot.connectorMediaDecision());
        metadata.put("hearingMediaDiagnostics", hearingMediaSnapshot.diagnostics());
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        List<String> labels = mergeLabels(
            context.portfolio().labels(),
            context.deskProfile().labels(),
            bridgeProfile.labels(),
            integrationProfile.labels(),
            referenceSnapshot.labels(),
            institutionalSnapshot.labels(),
            operationalDeskSnapshot.labels(),
            operationalTransactionSnapshot.labels(),
            migrationSnapshot.labels(),
            hearingMediaSnapshot.labels()
        );
        return new SecretariatQueueSummaryProjection(metadata, labels);
    }

    @SafeVarargs
    private static List<String> mergeLabels(List<String>... groups) {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        if (groups != null) {
            for (List<String> group : groups) {
                if (group == null) {
                    continue;
                }
                for (String label : group) {
                    if (label != null && !label.isBlank()) {
                        labels.add(label.trim());
                    }
                }
            }
        }
        return List.copyOf(labels);
    }
}
