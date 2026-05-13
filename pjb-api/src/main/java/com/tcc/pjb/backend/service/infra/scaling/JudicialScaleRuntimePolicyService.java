package com.tcc.pjb.backend.service.infra.scaling;

import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.configs.kafka.PjbKafkaScaleProperties;
import com.tcc.pjb.backend.model.entity.infra.ProcessualReadModelRecompositionJob;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Year;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class JudicialScaleRuntimePolicyService {

    private final JudicialScaleProfileResolver resolver;
    private final PjbDataSourceRoutingProperties routingProperties;
    private final ObjectProvider<PjbKafkaScaleProperties> kafkaScalePropertiesProvider;
    private final Environment environment;

    public JudicialScaleRuntimePolicyService(JudicialScaleProfileResolver resolver,
                                             PjbDataSourceRoutingProperties routingProperties,
                                             ObjectProvider<PjbKafkaScaleProperties> kafkaScalePropertiesProvider,
                                             Environment environment) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.routingProperties = Objects.requireNonNull(routingProperties, "routingProperties");
        this.kafkaScalePropertiesProvider = Objects.requireNonNull(kafkaScalePropertiesProvider, "kafkaScalePropertiesProvider");
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    public JudicialRuntimePolicy resolve(SecretariatOperationalRoutingProfile routing) {
        if (routing == null) {
            return resolve((String) null, (String) null);
        }
        JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy = routing.scaleProfile() == null
                ? resolver.resolvePolicy(routing.instanciaAxis(), routing.ramoAxis(), routing.specialization())
                : resolver.buildPolicy(routing.scaleProfile(), routing.instanciaAxis(), routing.ramoAxis(), "ROUTING_RUNTIME");
        return build(scalePolicy);
    }

    public JudicialRuntimePolicy resolve(String instanciaAxis, String ramoAxis) {
        JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy = resolver.resolvePolicy(instanciaAxis, ramoAxis, null);
        return build(scalePolicy);
    }

    public JudicialRuntimePolicy resolve(ProcessualReadModelRecompositionJob job) {
        if (job == null) {
            return resolve((String) null, (String) null);
        }
        String instanceAxis = inferInstanceAxis(job.getDomain(), job.getTribunalCode(), job.getScopeKey(), job.getReason());
        String branchAxis = inferBranchAxis(job.getRamoCode(), job.getDomain(), job.getScopeKey(), job.getReason());
        return resolve(instanceAxis, branchAxis);
    }

    public JudicialRuntimePolicy resolve(OutboxEvent event) {
        if (event == null) {
            return resolve((String) null, (String) null);
        }
        String instanceAxis = inferInstanceAxis(event.getRoutingKey(), event.getEventType(), event.getAggregateType(), event.getAggregateId(), event.getHeadersJson());
        String branchAxis = inferBranchAxis(event.getRoutingKey(), event.getEventType(), event.getAggregateType(), event.getAggregateId(), event.getHeadersJson());
        return resolve(instanceAxis, branchAxis);
    }

    public double priorityWeight(ProcessualReadModelRecompositionJob job) {
        return resolve(job).eventPriorityWeight();
    }

    public double priorityWeight(OutboxEvent event) {
        return resolve(event).eventPriorityWeight();
    }

    public int recompositionClaimBatch(ProcessualReadModelRecompositionJob job) {
        return resolve(job).recompositionClaimBatch();
    }

    public int outboxClaimBatch(OutboxEvent event) {
        return resolve(event).outboxClaimBatch();
    }

    public long scaleSlaHorizonHours(long baseHours, SecretariatOperationalRoutingProfile routing) {
        JudicialRuntimePolicy policy = resolve(routing);
        return policy.scaleSlaHorizonHours(baseHours);
    }

    public JudicialRuntimePolicyView preview(String instanciaAxis, String ramoAxis) {
        JudicialRuntimePolicy policy = resolve(instanciaAxis, ramoAxis);
        return new JudicialRuntimePolicyView(
                policy.profileCode(),
                policy.displayName(),
                policy.instanceClass(),
                policy.branchClass(),
                policy.eventPriorityWeight(),
                policy.outboxClaimBatch(),
                policy.recompositionClaimBatch(),
                policy.schedulerPollMs(),
                policy.kafkaListenerConcurrency(),
                policy.slaEscalationHours(),
                policy.followUpEscalationHours(),
                policy.dueSoonPressureThreshold(),
                policy.expeditionBatchSize(),
                policy.metadata()
        );
    }

    private JudicialRuntimePolicy build(JudicialScaleProfileResolver.JudicialScalePolicy scalePolicy) {
        PjbKafkaScaleProperties kafkaProperties = kafkaScalePropertiesProvider.getIfAvailable();
        int baseOutboxBatch = environment.getProperty("pjb.outbox.poller.batch", Integer.class, 50);
        long baseSchedulerPoll = Math.max(1_000L, routingProperties.getProcessualReadModels().getRecompositionPollMs());
        int baseRecompositionBatch = Math.max(1, routingProperties.getProcessualReadModels().getRecompositionBatchSize());
        int baseKafkaConcurrency = Math.max(1, kafkaProperties == null ? 3 : kafkaProperties.getListenerConcurrency());
        double factor = clamp(scalePolicy.queueParallelismFactor(), 0.75d, 1.80d);
        double priorityWeight = clamp((scalePolicy.queueParallelismFactor() * 0.55d)
                + (scalePolicy.rateLimitFactor() * 0.25d)
                + (scalePolicy.queueBudgetFactor() * 0.20d), 0.75d, 1.80d);
        String seasonalMode = seasonalMode(scalePolicy.branchClass());
        if ("ELEICAO".equals(seasonalMode)) {
            factor = clamp(factor * 1.12d, 0.75d, 1.90d);
            priorityWeight = clamp(priorityWeight * 1.10d, 0.75d, 1.90d);
        }
        if (isCollegiateInstance(scalePolicy.instanceClass())) {
            factor = clamp(factor * 1.06d, 0.75d, 1.90d);
            priorityWeight = clamp(priorityWeight * 1.04d, 0.75d, 1.90d);
        }
        int outboxClaimBatch = scaleInt(baseOutboxBatch, factor, 10, Math.max(10, baseOutboxBatch * 4));
        int recompositionClaimBatch = scaleInt(baseRecompositionBatch, factor, 1, Math.max(1, baseRecompositionBatch * 4));
        long schedulerPollMs = scaleLong(baseSchedulerPoll, 1d / priorityWeight, 1_000L, 60_000L);
        int kafkaListenerConcurrency = scaleInt(baseKafkaConcurrency, Math.max(scalePolicy.queueParallelismFactor(), scalePolicy.rateLimitFactor()), 1, Math.max(8, baseKafkaConcurrency * 4));
        long slaEscalationHours = scaleLong(isCollegiateInstance(scalePolicy.instanceClass()) ? 6L : 4L, 1d / priorityWeight, 1L, 18L);
        long followUpEscalationHours = Math.max(slaEscalationHours + 2L, scaleLong(isCollegiateInstance(scalePolicy.instanceClass()) ? 12L : 8L, 1d / factor, 2L, 36L));
        int dueSoonThreshold = scaleInt(isCollegiateInstance(scalePolicy.instanceClass()) ? 4 : 3, Math.max(1d, scalePolicy.readPressureFactor()), 2, 8);
        int expeditionBatchSize = scaleInt(24, factor, 8, 96);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("asyncWritePreferred", scalePolicy.asyncWritePreferred());
        metadata.put("cacheHotPreferred", scalePolicy.cacheHotPreferred());
        metadata.put("searchPreferred", scalePolicy.searchPreferred());
        metadata.put("replicaLagFactor", scalePolicy.replicaLagFactor());
        metadata.put("degradedReplicaLagFactor", scalePolicy.degradedReplicaLagFactor());
        metadata.put("readPressureFactor", scalePolicy.readPressureFactor());
        metadata.put("queueBudgetFactor", scalePolicy.queueBudgetFactor());
        metadata.put("seasonalMode", seasonalMode);
        metadata.put("collegiateMode", isCollegiateInstance(scalePolicy.instanceClass()));
        metadata.put("suggestedSchedulerDiscipline", isCollegiateInstance(scalePolicy.instanceClass()) ? "PAUTA_SESSAO_ACORDAO" : "RECEBIMENTO_SANEAMENTO_CUMPRIMENTO");
        return new JudicialRuntimePolicy(
                scalePolicy.profile().name(),
                scalePolicy.displayName(),
                scalePolicy.instanceClass(),
                scalePolicy.branchClass(),
                priorityWeight,
                outboxClaimBatch,
                recompositionClaimBatch,
                schedulerPollMs,
                kafkaListenerConcurrency,
                slaEscalationHours,
                followUpEscalationHours,
                dueSoonThreshold,
                expeditionBatchSize,
                Collections.unmodifiableMap(metadata)
        );
    }

    private boolean isCollegiateInstance(String instanceClass) {
        String token = instanceClass == null ? "" : instanceClass.trim().toUpperCase(Locale.ROOT);
        return token.contains("SEGUNDA") || token.contains("SUPERIOR");
    }

    private String seasonalMode(String branchClass) {
        if (branchClass == null || branchClass.isBlank()) {
            return "PADRAO";
        }
        String normalized = branchClass.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("ELEITORAL") && Year.now().getValue() % 2 == 0) {
            return "ELEICAO";
        }
        return "PADRAO";
    }


    private String inferInstanceAxis(String... sources) {
        String token = normalize(sources);
        if (containsAny(token, "STF", "STJ", "TST", "TSE", "STM", "SUPERIOR", "TRIBUNAL_SUPERIOR", "PLENARIO", "SECAO")) {
            return "TRIBUNAL_SUPERIOR";
        }
        if (containsAny(token, "TURMA_RECURSAL", "RECURSAL", "SEGUNDA_INSTANCIA", "SEGUNDO_GRAU", "2G", "TRF", "TJ", "CAMARA", "COLEGIADO", "ACORDAO")) {
            return "SEGUNDO_GRAU";
        }
        return "PRIMEIRO_GRAU";
    }

    private String inferBranchAxis(String... sources) {
        String token = normalize(sources);
        if (containsAny(token, "JUIZADO", "JEC", "JEF", "TURMA_RECURSAL")) {
            return "JUIZADO_ESPECIAL";
        }
        if (containsAny(token, "PENAL", "CRIM", "INQUERITO", "EXECUCAO_PENAL", "JURI")) {
            return "PENAL";
        }
        if (containsAny(token, "TRABALHO", "TRABALHISTA", "TRT", "TST")) {
            return "TRABALHISTA";
        }
        if (containsAny(token, "ELEITORAL", "TRE", "TSE", "ZONA_ELEITORAL")) {
            return "ELEITORAL";
        }
        if (containsAny(token, "MILITAR", "TJM", "STM", "AUDITORIA_MILITAR")) {
            return "MILITAR";
        }
        if (containsAny(token, "FEDERAL", "TRF", "JF", "SECAO_JUDICIARIA", "SUBSECAO")) {
            return "FEDERAL";
        }
        return "ESTADUAL";
    }

    private String normalize(String... sources) {
        StringBuilder builder = new StringBuilder();
        if (sources != null) {
            for (String source : sources) {
                if (source == null || source.isBlank()) {
                    continue;
                }
                if (!builder.isEmpty()) {
                    builder.append('|');
                }
                builder.append(source.trim().toUpperCase(Locale.ROOT)
                        .replace('-', '_')
                        .replace(' ', '_')
                        .replace('Ç', 'C'));
            }
        }
        return builder.toString();
    }

    private boolean containsAny(String token, String... options) {
        if (token == null || token.isBlank() || options == null) {
            return false;
        }
        for (String option : options) {
            if (option != null && !option.isBlank() && token.contains(option.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'))) {
                return true;
            }
        }
        return false;
    }

    private int scaleInt(int base, double factor, int min, int max) {
        return (int) Math.max(min, Math.min(max, Math.round(base * factor)));
    }

    private long scaleLong(long base, double factor, long min, long max) {
        return Math.max(min, Math.min(max, Math.round(base * factor)));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record JudicialRuntimePolicy(
            String profileCode,
            String displayName,
            String instanceClass,
            String branchClass,
            double eventPriorityWeight,
            int outboxClaimBatch,
            int recompositionClaimBatch,
            long schedulerPollMs,
            int kafkaListenerConcurrency,
            long slaEscalationHours,
            long followUpEscalationHours,
            int dueSoonPressureThreshold,
            int expeditionBatchSize,
            Map<String, Object> metadata
    ) {
        public long scaleSlaHorizonHours(long baseHours) {
            double factor = Math.max(0.85d, Math.min(1.60d, eventPriorityWeight));
            return Math.max(1L, Math.round(baseHours * factor));
        }
    }

    public record JudicialRuntimePolicyView(
            String profileCode,
            String displayName,
            String instanceClass,
            String branchClass,
            double eventPriorityWeight,
            int outboxClaimBatch,
            int recompositionClaimBatch,
            long schedulerPollMs,
            int kafkaListenerConcurrency,
            long slaEscalationHours,
            long followUpEscalationHours,
            int dueSoonPressureThreshold,
            int expeditionBatchSize,
            Map<String, Object> metadata
    ) {
    }
}
