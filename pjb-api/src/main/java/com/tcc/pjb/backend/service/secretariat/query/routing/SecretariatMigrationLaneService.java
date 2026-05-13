package com.tcc.pjb.backend.service.secretariat.query.routing;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.model.dto.innovation.PjbMigrationHygienePreviewRequest;
import com.tcc.pjb.backend.model.dto.innovation.PjbMigrationHygienePreviewResponse;
import com.tcc.pjb.backend.service.innovation.PjbMigrationHygieneService;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SecretariatMigrationLaneService {

    private final PjbMigrationHygieneService migrationHygieneService;

    public SecretariatMigrationLaneService(PjbMigrationHygieneService migrationHygieneService) {
        this.migrationHygieneService = Objects.requireNonNull(migrationHygieneService);
    }

    public MigrationLaneSnapshot resolve(String inboxKey,
                                         String queueCode,
                                         String title,
                                         Collection<String> tags,
                                         ForumDeskPortfolioProfile portfolio,
                                         SecretariatFlowBridgeProfile bridgeProfile,
                                         SecretariatJudicialIntegrationProfile integrationProfile) {
        String source = normalize(((title == null ? "" : title) + ' ' + (queueCode == null ? "" : queueCode) + ' ' + (tags == null ? "" : String.join(" ", tags))));
        boolean hearingScheduled = containsAny(source, "AUDIENCIA", "AUDIÊNCIA", "DESIGNADA", "DESIGNACAO", "PAUTA_AUDIENCIA");
        boolean judgmentScheduled = containsAny(source, "JULGAMENTO", "SESSAO", "SESSÃO", "PAUTA", "ACORDAO", "ACÓRDÃO", "SUSTENTACAO", "SUSTENTAÇÃO");
        boolean pendingSignatures = containsAny(source, "ASSINATURA_PENDENTE", "ASSINAR", "PENDENCIA_ASSINATURA", "PENDÊNCIA_ASSINATURA");
        boolean openDeadlines = containsAny(source, "PRAZO", "INTIMACAO", "INTIMAÇÃO", "AGUARDANDO_CIENCIA", "AGUARDANDO_CIÊNCIA", "CUMPRIMENTO_PENDENTE");
        boolean pendingTribunalAppeals = containsAny(source, "RECURSO", "APELACAO", "APELAÇÃO", "AGRAVO", "EMBARGOS", "RESP", "RE", "TNU", "COLEGIADO")
                || bridgeProfile != null && "RECURSAL".equals(bridgeProfile.downstreamAxis());
        boolean missingNationalIds = containsAny(source, "SEM_CPF", "SEM_CNPJ", "CPF_AUSENTE", "CNPJ_AUSENTE", "PARTE_INCOMPLETA");
        boolean tpuConsistent = !containsAny(source, "TPU_INCONSISTENTE", "SEM_TPU", "CLASSIFICACAO_INCONSISTENTE", "CLASSIFICAÇÃO_INCONSISTENTE");
        boolean suspended = containsAny(source, "SUSPENSO", "SUSPENSA", "SOBRESTADO", "SOBRESTADA");
        boolean archived = containsAny(source, "ARQUIVADO", "ARQUIVADA", "BAIXADO", "BAIXADA");
        int mediaCount = containsAny(source, "VIDEO", "VÍDEO", "AUDIO", "ÁUDIO", "MIDIA", "MÍDIA", "WEBRTC", "GRAVACAO", "GRAVAÇÃO") ? 1 : 0;
        boolean collegiateCase = bridgeProfile != null && "RECURSAL".equals(bridgeProfile.downstreamAxis())
                || containsAny(source, "COLEGIADO", "CAMARA", "CÂMARA", "TURMA", "ACORDAO", "ACÓRDÃO", "RELATOR");
        String sourceSystem = firstNonBlank(integrationProfile == null ? null : integrationProfile.connectorSystem(), integrationProfile == null ? null : integrationProfile.targetSystem(), "PJB_INTERNAL");
        PjbMigrationHygienePreviewResponse preview = migrationHygieneService.preview(new PjbMigrationHygienePreviewRequest(
                sourceSystem,
                pendingSignatures,
                hearingScheduled,
                judgmentScheduled,
                openDeadlines,
                pendingTribunalAppeals,
                missingNationalIds,
                tpuConsistent,
                suspended,
                archived,
                mediaCount,
                collegiateCase
        ));

        String migrationDecision = switch (preview.readiness()) {
            case "BLOCKED" -> "BLOQUEAR_MIGRACAO_E_RETER_NA_SECRETARIA";
            case "READY_WITH_ATTENTION" -> "MIGRAR_COM_SANEAMENTO_ASSISTIDO";
            default -> "MIGRAR_E_REDISTRIBUIR_AUTOMATICAMENTE";
        };
        String targetDesk = "TRIBUNAL_COLLEGIATE_SECRETARIAT".equals(preview.suggestedJourney())
                ? firstNonBlank(portfolio == null ? null : portfolio.escalationDesk(), "SECRETARIA_COLEGIADA")
                : firstNonBlank(portfolio == null ? null : portfolio.triageDesk(), "SECRETARIA_TRIAGEM");
        String connectorDecision = "BLOCKED".equals(preview.readiness())
                ? "HOLD_CONNECTOR_DISPATCH"
                : "PJB_INTERNAL".equals(normalizeSystem(sourceSystem))
                ? "LOCALIZE_AND_CONTINUE"
                : "PREPARE_CONNECTOR_MIGRATION_AND_ACK";

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(preview.readiness());
        labels.add(migrationDecision);
        labels.add(connectorDecision);
        if (preview.readinessScore() >= 85) {
            labels.add("MIGRATION_READY_HIGH_CONFIDENCE");
        }
        if (!preview.blockers().isEmpty()) {
            labels.add("MIGRATION_BLOCKERS_PRESENT");
        }
        if (!preview.automationOpportunities().isEmpty()) {
            labels.add("MIGRATION_AUTOMATION_AVAILABLE");
        }

        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("inboxKey", inboxKey);
        diagnostics.put("queueCode", queueCode);
        diagnostics.put("sourceSystem", preview.sourceSystem());
        diagnostics.put("readiness", preview.readiness());
        diagnostics.put("suggestedJourney", preview.suggestedJourney());
        diagnostics.put("readinessScore", preview.readinessScore());
        diagnostics.put("connectorDecision", connectorDecision);
        diagnostics.put("targetDesk", targetDesk);

        return new MigrationLaneSnapshot(
                preview.sourceSystem(),
                preview.readiness(),
                migrationDecision,
                connectorDecision,
                targetDesk,
                preview.blockers(),
                preview.sanitationActions(),
                preview.automationOpportunities(),
                preview.readinessScore(),
                List.copyOf(labels),
                Map.copyOf(diagnostics)
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeSystem(String value) {
        return value == null || value.isBlank() ? "PJB_INTERNAL" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static boolean containsAny(String source, String... tokens) {
        if (source == null || source.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && source.contains(token.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public record MigrationLaneSnapshot(String sourceSystem,
                                        String readiness,
                                        String migrationDecision,
                                        String connectorDecision,
                                        String targetDesk,
                                        List<String> blockers,
                                        List<String> sanitationActions,
                                        List<String> automationOpportunities,
                                        int readinessScore,
                                        List<String> labels,
                                        java.util.Map<String, Object> diagnostics) {
    }
}
