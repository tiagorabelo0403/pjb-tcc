package com.tcc.pjb.backend.service.infra;

import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatInstitutionalAlignmentService;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatJudicialReferenceModelService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalActionModelService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalDeskModelService;
import com.tcc.pjb.backend.service.secretariat.query.operational.SecretariatOperationalTransactionModelService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catálogos de modelos de secretaria e alinhamento institucional (mesas operacionais, ações,
 * transações, eixos institucionais). Extraído de {@link ScaleArchitectureService} porque esses
 * 5 colaboradores são usados exclusivamente por esse subconjunto de métodos -- cada um mapeia
 * 1:1 para o seu próprio método de visualização, sem estado compartilhado com os outros grupos.
 */
@Service
public class ScaleSecretariatModelGovernanceService {

    private final SecretariatJudicialReferenceModelService referenceModelService;
    private final SecretariatInstitutionalAlignmentService institutionalAlignmentService;
    private final SecretariatOperationalDeskModelService operationalDeskModelService;
    private final SecretariatOperationalActionModelService operationalActionModelService;
    private final SecretariatOperationalTransactionModelService operationalTransactionModelService;

    public ScaleSecretariatModelGovernanceService(SecretariatJudicialReferenceModelService referenceModelService,
                                                   SecretariatInstitutionalAlignmentService institutionalAlignmentService,
                                                   SecretariatOperationalDeskModelService operationalDeskModelService,
                                                   SecretariatOperationalActionModelService operationalActionModelService,
                                                   SecretariatOperationalTransactionModelService operationalTransactionModelService) {
        this.referenceModelService = Objects.requireNonNull(referenceModelService);
        this.institutionalAlignmentService = Objects.requireNonNull(institutionalAlignmentService);
        this.operationalDeskModelService = Objects.requireNonNull(operationalDeskModelService);
        this.operationalActionModelService = Objects.requireNonNull(operationalActionModelService);
        this.operationalTransactionModelService = Objects.requireNonNull(operationalTransactionModelService);
    }

    @Transactional(readOnly = true)
    public JudicialSecretariatModelGovernanceView judicialSecretariatModelsView() {
        SecretariatJudicialReferenceModelService.ReferenceCatalogView catalog = referenceModelService.catalog();
        List<JudicialSecretariatModelRowView> rows = catalog.rows().stream()
                .map(row -> new JudicialSecretariatModelRowView(
                        row.instanceClass(),
                        row.branchClass(),
                        row.descriptor(),
                        row.queueFamilies(),
                        row.capabilities()
                ))
                .toList();
        return new JudicialSecretariatModelGovernanceView(rows);
    }

    @Transactional(readOnly = true)
    public JudicialOperationalDeskGovernanceView judicialOperationalDesksView() {
        SecretariatOperationalDeskModelService.OperationalDeskCatalogView catalog = operationalDeskModelService.catalog();
        List<JudicialOperationalDeskRowView> rows = catalog.rows().stream()
                .map(row -> new JudicialOperationalDeskRowView(
                        row.journeyMode(),
                        row.descriptor(),
                        row.desks()
                ))
                .toList();
        return new JudicialOperationalDeskGovernanceView(rows);
    }

    @Transactional(readOnly = true)
    public JudicialOperationalActionGovernanceView judicialOperationalActionsView() {
        SecretariatOperationalActionModelService.OperationalActionCatalogView catalog = operationalActionModelService.catalog();
        List<JudicialOperationalActionRowView> rows = catalog.rows().stream()
                .map(row -> new JudicialOperationalActionRowView(
                        row.journeyMode(),
                        row.descriptor(),
                        row.actions()
                ))
                .toList();
        return new JudicialOperationalActionGovernanceView(rows);
    }

    @Transactional(readOnly = true)
    public JudicialOperationalTransactionGovernanceView judicialOperationalTransactionsView() {
        SecretariatOperationalTransactionModelService.OperationalTransactionCatalogView catalog = operationalTransactionModelService.catalog();
        List<JudicialOperationalTransactionRowView> rows = catalog.rows().stream()
                .map(row -> new JudicialOperationalTransactionRowView(
                        row.journeyMode(),
                        row.descriptor(),
                        row.transactions()
                ))
                .toList();
        return new JudicialOperationalTransactionGovernanceView(rows);
    }

    @Transactional(readOnly = true)
    public JudicialInstitutionalAlignmentGovernanceView judicialInstitutionalAlignmentView() {
        SecretariatInstitutionalAlignmentService.InstitutionalCatalogView catalog = institutionalAlignmentService.catalog();
        List<JudicialInstitutionalAlignmentRowView> rows = catalog.rows().stream()
                .map(row -> new JudicialInstitutionalAlignmentRowView(
                        row.institutionalAxis(),
                        row.descriptor(),
                        row.cells(),
                        row.touchpoints()
                ))
                .toList();
        return new JudicialInstitutionalAlignmentGovernanceView(rows);
    }

    public record JudicialSecretariatModelGovernanceView(
            List<JudicialSecretariatModelRowView> rows
    ) {
    }

    public record JudicialSecretariatModelRowView(
            String instanceClass,
            String branchClass,
            String descriptor,
            List<String> queueFamilies,
            Map<String, Object> capabilities
    ) {
    }

    public record JudicialOperationalDeskGovernanceView(
            List<JudicialOperationalDeskRowView> rows
    ) {
    }

    public record JudicialOperationalDeskRowView(
            String journeyMode,
            String descriptor,
            List<SecretariatOperationalDeskModelService.OperationalDeskView> desks
    ) {
    }

    public record JudicialOperationalActionGovernanceView(
            List<JudicialOperationalActionRowView> rows
    ) {
    }

    public record JudicialOperationalActionRowView(
            String journeyMode,
            String descriptor,
            List<SecretariatOperationalActionModelService.OperationalDeskActionView> actions
    ) {
    }

    public record JudicialOperationalTransactionGovernanceView(
            List<JudicialOperationalTransactionRowView> rows
    ) {
    }

    public record JudicialOperationalTransactionRowView(
            String journeyMode,
            String descriptor,
            List<SecretariatOperationalTransactionModelService.OperationalTransactionView> transactions
    ) {
    }

    public record JudicialInstitutionalAlignmentGovernanceView(
            List<JudicialInstitutionalAlignmentRowView> rows
    ) {
    }

    public record JudicialInstitutionalAlignmentRowView(
            String institutionalAxis,
            String descriptor,
            List<String> cells,
            List<String> touchpoints
    ) {
    }
}
