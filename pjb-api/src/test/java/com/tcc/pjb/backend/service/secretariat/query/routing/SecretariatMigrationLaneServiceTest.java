package com.tcc.pjb.backend.service.secretariat.query.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.service.innovation.PjbMigrationHygieneService;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecretariatMigrationLaneServiceTest {

    private final SecretariatMigrationLaneService service = new SecretariatMigrationLaneService(new PjbMigrationHygieneService());

    @Test
    void shouldBlockMigrationWhenQueueCarriesAgendaAndIdentityIssues() {
        SecretariatMigrationLaneService.MigrationLaneSnapshot snapshot = service.resolve(
                "TJCE:1G:COMUM:CE:FORTALEZA",
                "AUDIENCIA_PENDENTE",
                "Processo com prazo em aberto e sem CPF da parte",
                List.of("sem_cpf", "audiencia_designada"),
                new ForumDeskPortfolioProfile("MESA_TRIAGEM", "MESA_GABINETE", "MESA_AUDIENCIA", "MESA_COMPLIANCE", "MESA_COLEGIADA", "MESA_ASSISTENTE", "MESA_COORD", "MESA_REDIST", "DASH_1G", List.of(), new LinkedHashMap<>()),
                new SecretariatFlowBridgeProfile("ORIGINARIO", "LOCAL", "MESA_DISTRIBUICAO", "MESA_GABINETE", "MESA_RECURSAL", "MESA_ADMISSIBILIDADE", false, false, false, List.of(), new LinkedHashMap<>()),
                new SecretariatJudicialIntegrationProfile("EPROC", "MESA_PROTOCOLO", "REST", "CERT", "FULL", "SYNC", "Q_EXT", "MESA_REVISAO", "EPROC_CONNECTOR", "ACK", "MESA_REPLAY", "RETRY", "EVIDENCIA", "JANELA", "TRF5", "TRF5", "EPROC", "FEDERAL", "https://connector", "AUTOMATICO", "MANUAL", "MESA_CONTINGENCIA", "DLQ", "RETENCAO", "MESA_MANUAL", "TELEMETRIA", "AUDIT", "DLQ", "MESA_RECONCILIACAO", "AUDITADO", "SLA_8H", "MESA_ESCALACAO", "MESA_RECIBO", "PROVAS", "24H", true, true, List.of(), List.of(), new LinkedHashMap<>())
        );

        assertThat(snapshot.readiness()).isEqualTo("BLOCKED");
        assertThat(snapshot.connectorDecision()).isEqualTo("HOLD_CONNECTOR_DISPATCH");
        assertThat(snapshot.blockers()).isNotEmpty();
        assertThat(snapshot.sanitationActions()).isNotEmpty();
    }

    @Test
    void shouldPrepareAutomaticMigrationForCollegiateQueue() {
        SecretariatMigrationLaneService.MigrationLaneSnapshot snapshot = service.resolve(
                "TJCE:2G:COLEGIADO:CE:FORTALEZA",
                "PAUTA_COLEGIADA",
                "Acórdão pronto para migração e redistribuição",
                List.of("colegiado", "acordao_publicado"),
                new ForumDeskPortfolioProfile("MESA_TRIAGEM", "MESA_GABINETE", "MESA_AUDIENCIA", "MESA_COMPLIANCE", "MESA_COLEGIADA", "MESA_ASSISTENTE", "MESA_COORD", "MESA_REDIST", "DASH_2G", List.of(), new LinkedHashMap<>()),
                new SecretariatFlowBridgeProfile("RECURSAL", "COLEGIADO", "MESA_DISTRIBUICAO", "MESA_GABINETE", "MESA_RECURSAL", "MESA_ADMISSIBILIDADE", false, true, true, List.of(), new LinkedHashMap<>()),
                new SecretariatJudicialIntegrationProfile("PJB_INTERNAL", "MESA_PROTOCOLO", "INTERNAL", "NONE", "FULL", "SYNC", "Q_EXT", "MESA_REVISAO", "PJB_LOCAL", "ACK", "MESA_REPLAY", "RETRY", "EVIDENCIA", "JANELA", "TJCE", "TJCE", "PJB_INTERNAL", "ESTADUAL", null, "AUTOMATICO", "LOCAL", "MESA_CONTINGENCIA", "DLQ", "RETENCAO", "MESA_MANUAL", "TELEMETRIA", "AUDIT", "DLQ", "MESA_RECONCILIACAO", "AUDITADO", "SLA_8H", "MESA_ESCALACAO", "MESA_RECIBO", "PROVAS", "24H", false, false, List.of(), List.of(), new LinkedHashMap<>())
        );

        assertThat(snapshot.migrationDecision()).isNotBlank();
        assertThat(snapshot.targetDesk()).isEqualTo("MESA_COLEGIADA");
        assertThat(snapshot.automationOpportunities()).isNotEmpty();
    }
}
