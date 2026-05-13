package com.tcc.pjb.backend.service.operational.catalog;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.institutional.support.lane.InstitutionalSupportLaneResolver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProceduralFormalCatalogService {

    public FormalCatalogProjection resolveSecretariatCatalog(String inboxKey,
                                                             Collection<SecretariatQueueItem> items,
                                                             Map<Long, Map<String, Object>> metadataByWorkItemId) {
        CatalogContext context = buildSecretariatContext(inboxKey, items, metadataByWorkItemId);
        List<FormalDocument> documents = buildDocuments(context);
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("documentCount", documents.size());
        metrics.put("ramoAxis", context.ramoAxis());
        metrics.put("ritoAxis", context.ritoAxis());
        metrics.put("instanceAxis", context.instanceAxis());
        metrics.put("phaseAxis", context.phaseAxis());
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (context.sensitive()) {
            warnings.add("Catálogo formal exige variantes sigilosas e segregação de acesso por operação.");
        }
        if (documents.isEmpty()) {
            warnings.add("Nenhum documento formal projetado; revisar ramo, rito e malha da secretaria.");
        }
        return new FormalCatalogProjection(context.ramoAxis(), context.ritoAxis(), context.instanceAxis(), context.phaseAxis(), List.copyOf(documents), Map.copyOf(metrics), List.copyOf(warnings));
    }

    public FormalCatalogProjection resolveInstitutionalCatalog(String branchCode,
                                                               InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane,
                                                               Processo processo,
                                                               Collection<WorkItem> items) {
        CatalogContext context = buildInstitutionalContext(branchCode, lane, processo, items);
        List<FormalDocument> documents = buildDocuments(context);
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("documentCount", documents.size());
        metrics.put("branchAxis", context.branchAxis());
        metrics.put("ramoAxis", context.ramoAxis());
        metrics.put("ritoAxis", context.ritoAxis());
        metrics.put("phaseAxis", context.phaseAxis());
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if ("PROCURADORIA".equals(context.branchAxis())) {
            warnings.add("Secretaria institucional da Procuradoria demanda ofício e representação do ente público parametrizados pelo ramo.");
        }
        if (context.sensitive()) {
            warnings.add("Contexto institucional sensível pede dossiê e pacote de peças com visibilidade graduada.");
        }
        return new FormalCatalogProjection(context.ramoAxis(), context.ritoAxis(), context.instanceAxis(), context.phaseAxis(), List.copyOf(documents), Map.copyOf(metrics), List.copyOf(warnings));
    }

    private List<FormalDocument> buildDocuments(CatalogContext context) {
        LinkedHashMap<String, FormalDocument> documents = new LinkedHashMap<>();
        put(documents, doc("CARTA_INTIMACAO_AUDIENCIA", "Carta de intimação para audiência", "AUDIENCIA", context, false, List.of("AUDIENCIA", "INTIMACAO")));
        put(documents, doc("CARTA_MANDADO_CITACAO_PESSOAL", "Carta/mandado de citação pessoal", "CITACAO", context, false, List.of("CITACAO", "DILIGENCIA_PESSOAL")));
        put(documents, doc("CERTIDAO_NAO_REALIZACAO", "Certidão de não realização", "CERTIDAO", context, context.sensitive(), List.of("CERTIDAO", "OCORRENCIA")));
        put(documents, doc("TERMO_REDESIGNACAO", "Termo de redesignação ou remarcação", "REDESIGNACAO", context, false, List.of("AGENDA", "REDESIGNACAO")));
        put(documents, doc("CHECKLIST_PRE_AUDIENCIA", "Checklist pré-audiência", "CHECKLIST", context, context.sensitive(), List.of("CHECKLIST", "PREPARACAO")));
        put(documents, doc("ATO_URGENCIA", "Ato de urgência e contingência", "URGENCIA", context, context.sensitive(), List.of("URGENCIA", context.phaseAxis())));
        if (context.recursalOrCollegiate()) {
            put(documents, doc("PAUTA_PACOTE_COLEGIADO", "Pauta e pacote colegiado", "PAUTA", context, context.sensitive(), List.of("COLEGIADO", "PAUTA")));
            put(documents, doc("COMUNICACAO_ACORDAO_BAIXA", "Comunicação de acórdão e baixa", "ACORDAO", context, context.sensitive(), List.of("COLEGIADO", "BAIXA")));
        }
        if (context.penalLike()) {
            put(documents, doc("CERTIDAO_COMPARECIMENTO_PENAL", "Certidão de comparecimento penal", "COMPARECIMENTO", context, true, List.of("PENAL", "COMPARECIMENTO")));
        }
        if (context.publicEntityCommunication()) {
            put(documents, doc("OFICIO_ENTE_PUBLICO", "Ofício para ente público", "OFICIO", context, context.sensitive(), List.of("ENTE_PUBLICO", context.branchAxis())));
        }
        return List.copyOf(documents.values());
    }

    private CatalogContext buildSecretariatContext(String inboxKey,
                                                   Collection<SecretariatQueueItem> items,
                                                   Map<Long, Map<String, Object>> metadataByWorkItemId) {
        ContextAccumulator accumulator = new ContextAccumulator();
        if (items != null) {
            for (SecretariatQueueItem item : items) {
                accumulator.accept(item, metadataByWorkItemId == null ? Map.of() : metadataByWorkItemId.get(item.getWorkItemId()));
            }
        }
        String instanceAxis = inferSecretariatInstance(inboxKey, accumulator.phase);
        String branchAxis = inferBranchByInbox(inboxKey, accumulator.ramo);
        return new CatalogContext(branchAxis, accumulator.resolveRamo(), accumulator.resolveRito(), instanceAxis, accumulator.resolvePhase(), accumulator.sensitive, accumulator.recursalOrCollegiate);
    }

    private CatalogContext buildInstitutionalContext(String branchCode,
                                                     InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane,
                                                     Processo processo,
                                                     Collection<WorkItem> items) {
        ContextAccumulator accumulator = new ContextAccumulator();
        if (processo != null) {
            accumulator.accept(processo);
        }
        if (items != null) {
            for (WorkItem item : items) {
                accumulator.accept(item);
            }
        }
        String instanceAxis = processo != null && processo.getFaseAtual() == FaseProcessual.RECURSAL ? "SEGUNDA_INSTANCIA" : inferLaneInstance(lane);
        return new CatalogContext(firstNonBlank(branchCode, lane == null ? null : lane.branchCode(), "INSTITUTIONAL"), accumulator.resolveRamo(), accumulator.resolveRito(), instanceAxis, accumulator.resolvePhase(), accumulator.sensitive, accumulator.recursalOrCollegiate);
    }

    private String inferLaneInstance(InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane) {
        if (lane == null || lane.tribunalCodigo() == null) {
            return "PRIMEIRA_INSTANCIA";
        }
        return lane.tribunalCodigo().startsWith("ST") ? "SUPERIOR" : "PRIMEIRA_INSTANCIA";
    }

    private String inferSecretariatInstance(String inboxKey, String phase) {
        String normalized = upper(inboxKey);
        if (normalized.contains(":2G:") || Objects.equals(phase, FaseProcessual.RECURSAL.name())) {
            return "SEGUNDA_INSTANCIA";
        }
        if (normalized.contains(":SUP:")) {
            return "SUPERIOR";
        }
        return "PRIMEIRA_INSTANCIA";
    }

    private String inferBranchByInbox(String inboxKey, String ramo) {
        if (ramo != null) {
            RamoDireito ramoDireito = RamoDireito.fromString(ramo);
            if (ramoDireito != null) {
                return ramoDireito.verticalPrincipal();
            }
        }
        String normalized = upper(inboxKey);
        if (normalized.contains("TRT")) {
            return "TRABALHISTA";
        }
        if (normalized.contains("TRE") || normalized.contains("TSE")) {
            return "ELEITORAL";
        }
        if (normalized.contains("TRF")) {
            return "FEDERAL";
        }
        return "ESTADUAL";
    }

    private FormalDocument doc(String code,
                               String title,
                               String actAxis,
                               CatalogContext context,
                               boolean sensitive,
                               List<String> tags) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("ramoAxis", context.ramoAxis());
        metadata.put("ritoAxis", context.ritoAxis());
        metadata.put("instanceAxis", context.instanceAxis());
        metadata.put("phaseAxis", context.phaseAxis());
        metadata.put("generatedAt", Instant.now());
        return new FormalDocument(code, title, actAxis, context.branchAxis(), context.phaseAxis(), context.sensitive() ? "ALTO" : "PADRAO", sensitive, List.copyOf(tags), Collections.unmodifiableMap(metadata));
    }

    private void put(Map<String, FormalDocument> documents, FormalDocument document) {
        documents.putIfAbsent(document.documentCode(), document);
    }

    private static String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public record FormalCatalogProjection(
            String ramoAxis,
            String ritoAxis,
            String instanceAxis,
            String phaseAxis,
            List<FormalDocument> documents,
            Map<String, Object> metrics,
            List<String> warnings
    ) {
    }

    public record FormalDocument(
            String documentCode,
            String title,
            String actAxis,
            String targetBranch,
            String targetPhase,
            String urgencyAxis,
            boolean sensitive,
            List<String> tags,
            Map<String, Object> metadata
    ) {
    }

    private record CatalogContext(
            String branchAxis,
            String ramoAxis,
            String ritoAxis,
            String instanceAxis,
            String phaseAxis,
            boolean sensitive,
            boolean recursalOrCollegiate
    ) {
        boolean penalLike() {
            RamoDireito ramo = RamoDireito.fromString(ramoAxis);
            return ramo != null && ramo.isPenalLike() || Objects.equals(ritoAxis, RitoProcessual.TRIBUNAL_JURI.name());
        }

        boolean publicEntityCommunication() {
            RamoDireito ramo = RamoDireito.fromString(ramoAxis);
            return "PROCURADORIA".equals(branchAxis)
                    || ramo != null && ramo.isFazendaLike()
                    || Objects.equals(ritoAxis, RitoProcessual.EXECUCAO_FISCAL.name())
                    || Objects.equals(ritoAxis, RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO.name())
                    || Objects.equals(ritoAxis, RitoProcessual.FAZENDA_PUBLICA_EXECUCAO.name());
        }
    }

    private static final class ContextAccumulator {
        private final Map<String, Integer> ramoVotes = new LinkedHashMap<>();
        private final Map<String, Integer> ritoVotes = new LinkedHashMap<>();
        private String phase;
        private boolean sensitive;
        private boolean recursalOrCollegiate;
        private boolean jurySignal;
        private String ramo;

        private void accept(SecretariatQueueItem item, Map<String, Object> metadata) {
            if (item == null) {
                return;
            }
            vote(ramoVotes, token(metadata, "ramoDireito"));
            vote(ritoVotes, token(metadata, "ritoProcessual"));
            phase = firstNonBlank(token(metadata, "faseProcessual"), phase);
            ramo = firstNonBlank(token(metadata, "ramoDireito"), ramo);
            sensitive = sensitive || item.isSecrecyReviewRequired() || item.isHearingSensitive() || Objects.equals(token(metadata, "nivelSigilo"), NivelSigilo.SIGILO_N2.name());
            recursalOrCollegiate = recursalOrCollegiate || contains(item.getQueueCode(), "COLEGIADO", "RECURSAL", "ACORDAO") || contains(item.getTitulo(), "SESSAO", "COLEGIADO");
            jurySignal = jurySignal || contains(item.getQueueCode(), "JURI", "JÚRI") || contains(item.getTitulo(), "JURI", "JÚRI");
        }

        private void accept(Processo processo) {
            if (processo == null) {
                return;
            }
            vote(ramoVotes, processo.getRamoDireito() == null ? null : processo.getRamoDireito().name());
            vote(ritoVotes, processo.getRito() == null ? null : processo.getRito().name());
            phase = firstNonBlank(processo.getFaseAtual() == null ? null : processo.getFaseAtual().name(), phase);
            ramo = firstNonBlank(processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(), ramo);
            sensitive = sensitive || processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
            recursalOrCollegiate = recursalOrCollegiate || processo.getFaseAtual() == FaseProcessual.RECURSAL;
        }

        private void accept(WorkItem item) {
            if (item == null) {
                return;
            }
            accept(item.getProcesso());
            recursalOrCollegiate = recursalOrCollegiate || contains(item.getQueueCode(), "COLEGIADO", "RECURSAL", "ACORDAO");
        }

        private String resolveRamo() {
            String voted = winner(ramoVotes);
            if (voted != null) {
                return voted;
            }
            RitoProcessual rito = RitoProcessual.fromString(resolveRito());
            RamoDireito ramoDireito = rito == null ? null : rito.suggestedRamo();
            return ramoDireito == null ? RamoDireito.CIVIL.name() : ramoDireito.name();
        }

        private String resolveRito() {
            String voted = winner(ritoVotes);
            if (voted != null) {
                return voted;
            }
            String ramoResolvido = winner(ramoVotes);
            if (jurySignal && Objects.equals(ramoResolvido, RamoDireito.PENAL.name())) {
                return RitoProcessual.TRIBUNAL_JURI.name();
            }
            return RitoProcessual.COMUM_ORDINARIO.name();
        }

        private String resolvePhase() {
            return firstNonBlank(phase, recursalOrCollegiate ? FaseProcessual.RECURSAL.name() : FaseProcessual.CONHECIMENTO.name());
        }

        private static void vote(Map<String, Integer> bucket, String token) {
            if (token == null || token.isBlank()) {
                return;
            }
            bucket.merge(token.trim().toUpperCase(Locale.ROOT), 1, Integer::sum);
        }

        private static String winner(Map<String, Integer> bucket) {
            return bucket.entrySet().stream().max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey())).map(Map.Entry::getKey).orElse(null);
        }

        private static boolean contains(String value, String... tokens) {
            if (value == null || value.isBlank() || tokens == null) {
                return false;
            }
            String normalized = value.toUpperCase(Locale.ROOT);
            for (String token : tokens) {
                if (token != null && normalized.contains(token)) {
                    return true;
                }
            }
            return false;
        }

        private static String token(Map<String, Object> metadata, String key) {
            if (metadata == null || key == null) {
                return null;
            }
            Object raw = metadata.get(key);
            return raw == null ? null : String.valueOf(raw);
        }
    }
}
