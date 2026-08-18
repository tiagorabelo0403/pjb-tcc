package com.tcc.pjb.backend.service.processual.peticionamento.media;

import com.tcc.pjb.backend.core.storage.ObjectStorageProperties;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import com.tcc.pjb.backend.service.upload.UploadCapacityGovernanceService;
import com.tcc.pjb.backend.service.upload.UploadContentPolicyService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

@Service
public class PeticionamentoMediaStorageShieldService {

    private final ObjectStorageProperties.Upload uploadProperties;
    private final UploadContentPolicyService uploadContentPolicyService;
    private final UploadCapacityGovernanceService uploadCapacityGovernanceService;

    @Inject
    public PeticionamentoMediaStorageShieldService() {
        this(new ObjectStorageProperties(), new UploadContentPolicyService(), new UploadCapacityGovernanceService(new ObjectStorageProperties(), new UploadContentPolicyService()));
    }

    public PeticionamentoMediaStorageShieldService(ObjectStorageProperties props,
                                                   UploadContentPolicyService uploadContentPolicyService,
                                                   UploadCapacityGovernanceService uploadCapacityGovernanceService) {
        ObjectStorageProperties properties = props == null ? new ObjectStorageProperties() : props;
        this.uploadProperties = properties.getUpload() == null ? new ObjectStorageProperties.Upload() : properties.getUpload();
        this.uploadContentPolicyService = uploadContentPolicyService == null ? new UploadContentPolicyService(properties) : uploadContentPolicyService;
        this.uploadCapacityGovernanceService = uploadCapacityGovernanceService == null
                ? new UploadCapacityGovernanceService(properties, this.uploadContentPolicyService)
                : uploadCapacityGovernanceService;
    }

    public StorageShieldReport plan(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<String> blockers = new ArrayList<>();
        ArrayList<String> alerts = new ArrayList<>();
        ArrayList<Map<String, Object>> inlinePlan = new ArrayList<>();

        long totalInlineBytes = 0L;
        int imageCount = 0;
        int audioCount = 0;
        int videoCount = 0;
        int inlineWithoutStorage = 0;

        for (PeticionamentoMediaBlocoRequest block : safe.inlineMediaBlocks()) {
            if (block == null) {
                continue;
            }
            String tipo = block.tipoResolvido();
            long size = safeBytes(block.getTamanhoBytes());
            if ("DOCUMENTO".equals(tipo)) {
                blockers.add("Documento não pode permanecer inline no corpo da petição; mova o item " + block.ancoraResolvida() + " para o bloco pós-petição de anexos.");
            }
            if (!block.metadataMinimaPresente()) {
                inlineWithoutStorage++;
                alerts.add("Bloco " + block.ancoraResolvida() + " ainda não aponta para upload reservado, storage key ou hash canônico.");
            }
            if (size > 0L) {
                totalInlineBytes = saturatingAdd(totalInlineBytes, size);
            }
            switch (tipo) {
                case "IMAGEM" -> imageCount++;
                case "AUDIO" -> audioCount++;
                case "VIDEO" -> videoCount++;
                default -> { }
            }
            if (sizeExceeded(block)) {
                blockers.add("Bloco " + block.ancoraResolvida() + " excedeu o limite prudencial inline para " + tipo.toLowerCase(Locale.ROOT) + ".");
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<>(block.toMap());
            item.put("storageDiscipline", "OBJECT_STORAGE_ONLY");
            item.put("databasePersistence", "METADATA_ONLY");
            item.put("previewDerivatives", List.of("THUMBNAIL", "WAVEFORM", "STREAM_PROXY"));
            item.put("postProtocolMutability", "IMMUTABLE_VERSIONED");
            item.put("inlineEligible", !"DOCUMENTO".equals(tipo));
            if (size > 0L && block.getMimeType() != null && !block.getMimeType().isBlank()) {
                try {
                    UploadContentPolicyService.Policy policy = uploadContentPolicyService.resolveReservation(block.getStorageKey(), block.getMimeType(), size);
                    UploadContentPolicyService.IngestionPlan ingestionPlan = uploadContentPolicyService.buildIngestionPlan(policy, size);
                    item.put("ingestionLane", ingestionPlan.processingLane());
                    item.put("previewProfile", ingestionPlan.previewProfile());
                    item.put("deepInspectionRequired", ingestionPlan.deepInspectionRequired());
                } catch (RuntimeException ignored) {
                    item.put("ingestionLane", "PENDENTE_NORMALIZACAO");
                }
            }
            inlinePlan.add(Map.copyOf(item));
        }

        if (safe.inlineMediaBlocks().size() > Math.max(1, uploadProperties.getMaxInlineBlocks())) {
            blockers.add("A petição multimídia excedeu o teto prudencial de " + Math.max(1, uploadProperties.getMaxInlineBlocks()) + " blocos inline.");
        }
        if (totalInlineBytes > positiveOr(uploadProperties.getMaxInlineTotalBytes(), 167_772_160L)) {
            blockers.add("A soma da mídia inline excedeu o teto prudencial de " + humanBytes(positiveOr(uploadProperties.getMaxInlineTotalBytes(), 167_772_160L)) + "; use referências mais curtas e mantenha o conjunto principal em anexos separados.");
        }
        if (countItems(safe.provasDocumentais(), safe.documentosPessoais(), safe.documentosRepresentacao(), safe.documentosAnexados()) > Math.max(1, uploadProperties.getMaxPostPetitionReferences())) {
            alerts.add("O bloco pós-petição de anexos está muito volumoso; particione por lotes lógicos para reduzir pressão de ingestão e leitura.");
        }
        if (!safe.inlineMediaBlocks().isEmpty() && safe.postPetitionSectionsEmpty()) {
            alerts.add("A petição tem mídia inline, mas o bloco pós-petição de anexos está vazio; separar anexos principais melhora protocolo e leitura judicial.");
        }

        LinkedHashMap<String, Object> attachmentSections = new LinkedHashMap<>();
        attachmentSections.put("provasDocumentais", List.copyOf(safe.provasDocumentais()));
        attachmentSections.put("documentosPessoais", List.copyOf(safe.documentosPessoais()));
        attachmentSections.put("documentosRepresentacao", List.copyOf(safe.documentosRepresentacao()));
        attachmentSections.put("documentosGerais", List.copyOf(safe.documentosAnexados()));
        attachmentSections.put("postPetitionBlockEnabled", !safe.postPetitionSectionsEmpty());

        LinkedHashMap<String, Object> persistencePolicy = new LinkedHashMap<>();
        persistencePolicy.put("inlineBodyRule", "SOMENTE_IMAGEM_AUDIO_VIDEO");
        persistencePolicy.put("databaseRule", "NAO_PERSISTIR_BLOB_MIDIA_NO_BANCO_RELACIONAL");
        persistencePolicy.put("databaseFields", List.of("sha256", "sha384", "contentType", "tamanhoBytes", "storageBackend", "storageUri", "externalizedAt"));
        persistencePolicy.put("objectStorageLanes", List.of("QUARENTENA", "CANONICO_IMUTAVEL", "DERIVADOS_PREVIEW"));
        persistencePolicy.put("ingestionFlow", List.of("RESERVA", "UPLOAD_DIRETO", "QUARENTENA", "CANONICALIZACAO_ASSINCRONA", "VINCULO_PROCESSUAL"));
        persistencePolicy.put("deduplicationKey", "SHA384_CONTENT_ADDRESSABLE");
        persistencePolicy.put("asyncHeavyWork", List.of("TRANSCODE_VIDEO", "TRANSCODE_AUDIO", "THUMBNAIL", "TRANSCRICAO"));
        persistencePolicy.put("chunkedFinalize", true);
        persistencePolicy.put("allowBase64Inline", false);
        persistencePolicy.put("allowByteaInline", false);
        persistencePolicy.put("allowDocumentInline", false);

        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("profile", resolveProfile(blockers, totalInlineBytes, safe.inlineMediaBlocks().size()));
        workspace.put("inlineBodyPolicy", "SOMENTE_IMAGEM_AUDIO_VIDEO");
        workspace.put("inlineMediaPlan", List.copyOf(inlinePlan));
        workspace.put("postPetitionAttachmentSections", Map.copyOf(attachmentSections));
        workspace.put("persistencePolicy", Map.copyOf(persistencePolicy));
        workspace.put("databaseBackpressureGuards", List.of(
                "OBJECT_STORAGE_ONLY",
                "DIRECT_UPLOAD_ONLY",
                "METADATA_ONLY_IN_POSTGRES",
                "ASYNC_DERIVATIVE_GENERATION",
                "CHUNKED_FINALIZE",
                "CONTENT_ADDRESSABLE_DEDUP"
        ));
        workspace.put("byteBudget", Map.of(
                "inlineTotalBytes", totalInlineBytes,
                "inlineTotalHuman", humanBytes(totalInlineBytes),
                "inlineLimitHuman", humanBytes(positiveOr(uploadProperties.getMaxInlineTotalBytes(), 167_772_160L)),
                "imageLimitHuman", humanBytes(positiveOr(uploadProperties.getMaxInlineImageBytes(), 10_485_760L)),
                "audioLimitHuman", humanBytes(positiveOr(uploadProperties.getMaxInlineAudioBytes(), 25_165_824L)),
                "videoLimitHuman", humanBytes(positiveOr(uploadProperties.getMaxInlineVideoBytes(), 100_663_296L))
        ));
        workspace.put("topology", Map.of(
                "imageCount", imageCount,
                "audioCount", audioCount,
                "videoCount", videoCount,
                "inlineWithoutStorage", inlineWithoutStorage,
                "attachmentReferenceCount", countItems(safe.provasDocumentais(), safe.documentosPessoais(), safe.documentosRepresentacao(), safe.documentosAnexados())
        ));
        workspace.put("uploadGovernance", uploadCapacityGovernanceService.governanceSummary());

        return new StorageShieldReport(
                resolveProfile(blockers, totalInlineBytes, safe.inlineMediaBlocks().size()),
                !blockers.isEmpty(),
                List.copyOf(blockers),
                List.copyOf(alerts),
                Map.copyOf(workspace),
                Map.of(
                        "inlineBodyPolicy", workspace.get("inlineBodyPolicy"),
                        "postPetitionAttachmentSections", workspace.get("postPetitionAttachmentSections"),
                        "persistencePolicy", workspace.get("persistencePolicy"),
                        "uploadGovernance", workspace.get("uploadGovernance")
                )
        );
    }

    private boolean sizeExceeded(PeticionamentoMediaBlocoRequest block) {
        long size = safeBytes(block.getTamanhoBytes());
        if (size <= 0L) {
            return false;
        }
        return switch (block.tipoResolvido()) {
            case "IMAGEM" -> size > positiveOr(uploadProperties.getMaxInlineImageBytes(), 10_485_760L);
            case "AUDIO" -> size > positiveOr(uploadProperties.getMaxInlineAudioBytes(), 25_165_824L);
            case "VIDEO" -> size > positiveOr(uploadProperties.getMaxInlineVideoBytes(), 100_663_296L);
            default -> false;
        };
    }

    private static int countItems(List<String> a, List<String> b, List<String> c, List<String> d) {
        return safeSize(a) + safeSize(b) + safeSize(c) + safeSize(d);
    }

    private static int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private static long safeBytes(Long value) {
        return value == null || value <= 0L ? 0L : value;
    }

    private static long positiveOr(long value, long fallback) {
        return value > 0L ? value : fallback;
    }

    private static long saturatingAdd(long a, long b) {
        if (Long.MAX_VALUE - a < b) {
            return Long.MAX_VALUE;
        }
        return a + b;
    }

    private static String humanBytes(long bytes) {
        if (bytes <= 0L) {
            return "0 B";
        }
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int idx = 0;
        while (value >= 1024d && idx < units.length - 1) {
            value /= 1024d;
            idx++;
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[idx]);
    }

    private static String resolveProfile(List<String> blockers, long totalInlineBytes, int blockCount) {
        if (blockers != null && !blockers.isEmpty()) {
            return "STORAGE_SHIELD_BLOQUEANTE";
        }
        if (blockCount == 0) {
            return "PETICAO_TEXTUAL_COM_ANEXOS_EXTERNOS";
        }
        if (totalInlineBytes > 96L * 1024L * 1024L) {
            return "PETICAO_MULTIMIDIA_COM_GUARDA_DE_CAPACIDADE";
        }
        return "PETICAO_MULTIMIDIA_STORAGE_DISCIPLINADO";
    }

    public record ResolveRequest(
            List<PeticionamentoMediaBlocoRequest> inlineMediaBlocks,
            List<String> provasDocumentais,
            List<String> documentosPessoais,
            List<String> documentosRepresentacao,
            List<String> documentosAnexados,
            boolean preparingProtocolPackage) {
        public ResolveRequest {
            inlineMediaBlocks = inlineMediaBlocks == null ? List.of() : List.copyOf(inlineMediaBlocks);
            provasDocumentais = sanitize(provasDocumentais);
            documentosPessoais = sanitize(documentosPessoais);
            documentosRepresentacao = sanitize(documentosRepresentacao);
            documentosAnexados = sanitize(documentosAnexados);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest(List.of(), List.of(), List.of(), List.of(), List.of(), false);
        }

        public boolean postPetitionSectionsEmpty() {
            return provasDocumentais.isEmpty() && documentosPessoais.isEmpty() && documentosRepresentacao.isEmpty() && documentosAnexados.isEmpty();
        }

        private static List<String> sanitize(List<String> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            ArrayList<String> out = new ArrayList<>();
            for (String value : values) {
                if (value == null) {
                    continue;
                }
                String normalized = value.trim();
                if (!normalized.isEmpty() && !out.contains(normalized)) {
                    out.add(normalized);
                }
            }
            return out.isEmpty() ? List.of() : List.copyOf(out);
        }
    }

    public record StorageShieldReport(
            String profile,
            boolean blocking,
            List<String> blockers,
            List<String> alerts,
            Map<String, Object> workspace,
            Map<String, Object> protocolSection) {
        public StorageShieldReport {
            profile = Objects.requireNonNullElse(profile, "PETICAO_TEXTUAL_COM_ANEXOS_EXTERNOS");
            blockers = blockers == null ? List.of() : List.copyOf(blockers);
            alerts = alerts == null ? List.of() : List.copyOf(alerts);
            workspace = workspace == null ? Map.of() : Map.copyOf(workspace);
            protocolSection = protocolSection == null ? Map.of() : Map.copyOf(protocolSection);
        }
    }
}
