package com.tcc.pjb.backend.service.processual.peticionamento.media;

import com.tcc.pjb.backend.core.storage.ObjectStorageProperties;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import com.tcc.pjb.backend.service.upload.UploadContentPolicyService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.service.processual.peticionamento.PeticionamentoPericiaEvidenceIntelligenceService;

@Service
public class PeticionamentoMediaPublicationGateService {

    private final ObjectStorageProperties.Upload uploadProperties;
    private final UploadContentPolicyService uploadContentPolicyService;

    public PeticionamentoMediaPublicationGateService() {
        this(new ObjectStorageProperties(), new UploadContentPolicyService());
    }

    public PeticionamentoMediaPublicationGateService(ObjectStorageProperties props,
                                                     UploadContentPolicyService uploadContentPolicyService) {
        ObjectStorageProperties properties = props == null ? new ObjectStorageProperties() : props;
        this.uploadProperties = properties.getUpload() == null ? new ObjectStorageProperties.Upload() : properties.getUpload();
        this.uploadContentPolicyService = uploadContentPolicyService == null ? new UploadContentPolicyService(properties) : uploadContentPolicyService;
    }

    public PublicationGateReport resolve(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<Map<String, Object>> fileStates = new ArrayList<>();
        ArrayList<String> publicationGates = new ArrayList<>();
        ArrayList<String> globalBlockers = new ArrayList<>();
        int blockedCount = 0;
        int pendingUploadCount = 0;
        int pendingQuarantineCount = 0;
        int pendingPericiaCount = 0;
        int readyForPublicationCount = 0;

        if (safe.multimediaComposition() != null && safe.multimediaComposition().blocking()) {
            globalBlockers.addAll(safe.multimediaComposition().blockers());
        }
        if (safe.mediaStorageShield() != null && safe.mediaStorageShield().blockers().stream().anyMatch(v -> v != null && !hasAnchorToken(v, safe.inlineMediaBlocks()))) {
            globalBlockers.addAll(safe.mediaStorageShield().blockers().stream().filter(v -> v != null && !hasAnchorToken(v, safe.inlineMediaBlocks())).toList());
        }
        if (safe.mediaSecurity() != null && safe.mediaSecurity().blockers().stream().anyMatch(v -> v != null && !hasAnchorToken(v, safe.inlineMediaBlocks()))) {
            globalBlockers.addAll(safe.mediaSecurity().blockers().stream().filter(v -> v != null && !hasAnchorToken(v, safe.inlineMediaBlocks())).toList());
        }

        for (PeticionamentoMediaBlocoRequest block : safe.inlineMediaBlocks()) {
            if (block == null) {
                continue;
            }
            String anchor = block.ancoraResolvida();
            boolean missingMetadata = !block.metadataMinimaPresente();
            boolean inlineDocument = "DOCUMENTO".equals(block.tipoResolvido());
            boolean durationExceeded = durationExceeded(block);
            ArrayList<String> reasons = new ArrayList<>();
            reasons.addAll(filterByAnchor(safe.multimediaComposition() == null ? List.of() : safe.multimediaComposition().blockers(), anchor));
            reasons.addAll(filterByAnchor(safe.mediaSecurity() == null ? List.of() : safe.mediaSecurity().blockers(), anchor));
            reasons.addAll(filterByAnchor(safe.mediaStorageShield() == null ? List.of() : safe.mediaStorageShield().blockers(), anchor));
            if (inlineDocument) {
                reasons.add("Documento inline é vedado no corpo da petição.");
            }
            if (missingMetadata) {
                reasons.add("Mídia sem reserva, storage key ou hash canônico.");
            }
            if (durationExceeded) {
                reasons.add("Mídia excedeu a duração prudencial inline.");
            }
            UploadContentPolicyService.IngestionPlan ingestionPlan = resolvePlan(block);
            boolean deepInspection = ingestionPlan != null && ingestionPlan.deepInspectionRequired();
            boolean canonicalization = ingestionPlan != null && ingestionPlan.canonicalizationRecommended();
            boolean sensitive = block.blurInicialObrigatorio();
            boolean periciaSuggested = "AUDIO".equals(block.tipoResolvido()) || "VIDEO".equals(block.tipoResolvido()) || sensitive;
            String uploadState = missingMetadata ? "AGUARDANDO_RESERVA_UPLOAD" : "UPLOAD_RESERVADO";
            String canonicalizationState = resolveCanonicalizationState(ingestionPlan, missingMetadata);
            String securityState = resolveSecurityState(missingMetadata, deepInspection, sensitive, inlineDocument);
            String periciaState = periciaSuggested ? (sensitive ? "TRILHA_PERICIAL_SENSIVEL" : "TRIAGEM_PERICIAL_ATIVA") : "TRIAGEM_PADRAO";
            String publicationState;
            String protocolGate;
            String nextStep;
            if (!globalBlockers.isEmpty()) {
                publicationState = "RETIDO_POR_GOVERNANCA_GLOBAL";
                protocolGate = safe.preparingProtocolPackage() ? "RETIDO_NO_PROTOCOLO" : "AGUARDANDO_GOVERNANCA_GLOBAL";
                nextStep = "REDUZIR_CARGA_MULTIMIDIA_OU_REGULARIZAR_GATES_GLOBAIS";
                blockedCount++;
            } else if (!reasons.isEmpty()) {
                publicationState = "BLOQUEADO";
                protocolGate = safe.preparingProtocolPackage() ? "RETIDO_NO_PROTOCOLO" : "AGUARDANDO_REGULARIZACAO";
                nextStep = missingMetadata ? "VINCULAR_UPLOAD_RESERVADO" : inlineDocument ? "MOVER_PARA_BLOCO_POS_PETICAO" : durationExceeded ? "GERAR_RECORTE_OU_REFERENCIA_EXTERNA" : "REGULARIZAR_VALIDACAO_DA_MIDIA";
                blockedCount++;
            } else if (missingMetadata) {
                publicationState = "AGUARDANDO_UPLOAD";
                protocolGate = safe.preparingProtocolPackage() ? "RETIDO_ATE_VINCULO_DE_UPLOAD" : "AGUARDANDO_UPLOAD";
                nextStep = "FINALIZAR_UPLOAD_E_HASH_CANONICO";
                pendingUploadCount++;
            } else if (deepInspection || canonicalization || periciaSuggested) {
                publicationState = "AGUARDANDO_TRIPLO_OK";
                protocolGate = safe.preparingProtocolPackage() ? "CONDICIONADO_A_TRIPLO_OK" : "AGUARDANDO_TRIPLO_OK";
                nextStep = deepInspection ? "CONCLUIR_QUARENTENA_E_HUNTING" : periciaSuggested ? "CONCLUIR_TRIAGEM_PERICIAL" : "CONCLUIR_CANONICALIZACAO";
                pendingQuarantineCount++;
                if (periciaSuggested) {
                    pendingPericiaCount++;
                }
            } else {
                publicationState = "PRONTO_PARA_PUBLICACAO_CONTROLADA";
                protocolGate = safe.preparingProtocolPackage() ? "LIBERAVEL_APOS_SNAPSHOT_PROTOCOLAR" : "LIBERAVEL";
                nextStep = "GERAR_SNAPSHOT_PROTOCOLAR";
                readyForPublicationCount++;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<>(block.toMap());
            item.put("uploadState", uploadState);
            item.put("securityState", securityState);
            item.put("canonicalizationState", canonicalizationState);
            item.put("periciaState", periciaState);
            item.put("publicationState", publicationState);
            item.put("protocolGate", protocolGate);
            item.put("nextStep", nextStep);
            item.put("processingLane", ingestionPlan == null ? "PENDENTE_CLASSIFICACAO" : ingestionPlan.processingLane());
            item.put("previewProfile", ingestionPlan == null ? "PENDENTE_CLASSIFICACAO" : ingestionPlan.previewProfile());
            item.put("deepInspectionRequired", deepInspection);
            item.put("canonicalizationRecommended", canonicalization);
            item.put("sensitiveBlur", sensitive);
            item.put("blockingReasons", reasons.isEmpty() ? List.of() : List.copyOf(reasons));
            fileStates.add(Map.copyOf(item));
        }

        if (!fileStates.isEmpty()) {
            publicationGates.add("RESERVA_E_UPLOAD_CONTROLADO");
            publicationGates.add("QUARENTENA_TRIPLA");
            publicationGates.add("CANONICALIZACAO_E_PREVIEW_SEGURO");
            publicationGates.add("SNAPSHOT_PROTOCOLAR_E_PUBLICACAO_CONTROLADA");
        }
        if (pendingPericiaCount > 0) {
            publicationGates.add("TRILHA_PERICIAL_COMPLEMENTAR");
        }

        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("profile", resolveProfile(blockedCount, pendingUploadCount, pendingQuarantineCount, readyForPublicationCount));
        workspace.put("fileStates", List.copyOf(fileStates));
        workspace.put("publicationGates", List.copyOf(publicationGates));
        workspace.put("blockedCount", blockedCount);
        workspace.put("pendingUploadCount", pendingUploadCount);
        workspace.put("pendingQuarantineCount", pendingQuarantineCount);
        workspace.put("pendingPericiaCount", pendingPericiaCount);
        workspace.put("readyForPublicationCount", readyForPublicationCount);
        workspace.put("globalBlockers", List.copyOf(globalBlockers));
        workspace.put("publicationModel", Map.of(
                "publicPreview", false,
                "previewOnlyDerivedAssets", true,
                "protocolDependsOnSnapshot", true,
                "inlineDocumentForbidden", true
        ));
        workspace.put("limits", Map.of(
                "maxInlineBlocks", Math.max(1, uploadProperties.getMaxInlineBlocks()),
                "maxInlineTotalBytes", positiveOr(uploadProperties.getMaxInlineTotalBytes(), 167_772_160L),
                "syncPreviewMaxBytes", positiveOr(uploadProperties.getSyncPreviewMaxBytes(), 8_388_608L),
                "deepInspectionThresholdBytes", positiveOr(uploadProperties.getDeepInspectionThresholdBytes(), 50_331_648L)
        ));

        return new PublicationGateReport(
                resolveProfile(blockedCount, pendingUploadCount, pendingQuarantineCount, readyForPublicationCount),
                blockedCount > 0 || !globalBlockers.isEmpty(),
                pendingUploadCount > 0 || pendingQuarantineCount > 0,
                List.copyOf(publicationGates),
                Map.copyOf(workspace),
                Map.of(
                        "profile", workspace.get("profile"),
                        "publicationGates", workspace.get("publicationGates"),
                        "blockedCount", workspace.get("blockedCount"),
                        "pendingQuarantineCount", workspace.get("pendingQuarantineCount")
                )
        );
    }

    private UploadContentPolicyService.IngestionPlan resolvePlan(PeticionamentoMediaBlocoRequest block) {
        if (block == null || block.getMimeType() == null || block.getMimeType().isBlank() || block.getTamanhoBytes() == null || block.getTamanhoBytes() <= 0L) {
            return null;
        }
        try {
            var policy = uploadContentPolicyService.resolveReservation(block.getStorageKey(), block.getMimeType(), block.getTamanhoBytes());
            return uploadContentPolicyService.buildIngestionPlan(policy, block.getTamanhoBytes());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean durationExceeded(PeticionamentoMediaBlocoRequest block) {
        if (block == null || block.getDuracaoMs() == null || block.getDuracaoMs() <= 0L) {
            return false;
        }
        return switch (block.tipoResolvido()) {
            case "AUDIO" -> block.getDuracaoMs() > positiveOr(uploadProperties.getMaxAudioDurationMs(), 900_000L);
            case "VIDEO" -> block.getDuracaoMs() > positiveOr(uploadProperties.getMaxVideoDurationMs(), 900_000L);
            default -> false;
        };
    }

    private static String resolveCanonicalizationState(UploadContentPolicyService.IngestionPlan plan, boolean missingMetadata) {
        if (missingMetadata) {
            return "AGUARDANDO_CLASSIFICACAO";
        }
        if (plan == null) {
            return "CLASSIFICACAO_PENDENTE";
        }
        return switch (Objects.requireNonNullElse(plan.processingLane(), "PENDENTE_CLASSIFICACAO")) {
            case "SYNC_PREVIEW_CANONICO", "SYNC_TRANSCRICAO_RESUMIDA", "SYNC_DOCUMENT_GATE" -> "PREVIEW_SINCRONO_CONTROLADO";
            case "ASYNC_PREVIEW_CANONICO", "ASYNC_TRANSCRICAO_FORENSE", "ASYNC_STREAMING_FORENSE" -> "CANONICALIZACAO_ASSINCRONA";
            default -> "CLASSIFICACAO_PENDENTE";
        };
    }

    private static String resolveSecurityState(boolean missingMetadata, boolean deepInspection, boolean sensitive, boolean inlineDocument) {
        if (inlineDocument) {
            return "DOCUMENTO_INLINE_VEDADO";
        }
        if (missingMetadata) {
            return "AGUARDANDO_TRIAGEM";
        }
        if (deepInspection || sensitive) {
            return "EM_QUARENTENA_TRIPLA";
        }
        return "EM_VALIDACAO_CONTROLADA";
    }

    private static List<String> filterByAnchor(List<String> reasons, String anchor) {
        if (reasons == null || reasons.isEmpty() || anchor == null || anchor.isBlank()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (String reason : reasons) {
            if (reason != null && reason.contains(anchor)) {
                out.add(reason);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static boolean hasAnchorToken(String value, List<PeticionamentoMediaBlocoRequest> blocks) {
        if (value == null || value.isBlank() || blocks == null || blocks.isEmpty()) {
            return false;
        }
        for (PeticionamentoMediaBlocoRequest block : blocks) {
            if (block != null && value.contains(block.ancoraResolvida())) {
                return true;
            }
        }
        return false;
    }

    private static String resolveProfile(int blockedCount, int pendingUploadCount, int pendingQuarantineCount, int readyForPublicationCount) {
        if (blockedCount > 0) {
            return "PUBLICACAO_CONTROLADA_BLOQUEADA";
        }
        if (pendingUploadCount > 0) {
            return "PUBLICACAO_AGUARDANDO_UPLOAD";
        }
        if (pendingQuarantineCount > 0) {
            return "PUBLICACAO_AGUARDANDO_TRIPLO_OK";
        }
        if (readyForPublicationCount > 0) {
            return "PUBLICACAO_CONTROLADA_PRONTA";
        }
        return "PETICAO_SEM_MIDIA_INLINE";
    }

    private static long positiveOr(long value, long fallback) {
        return value > 0L ? value : fallback;
    }

    public record ResolveRequest(List<PeticionamentoMediaBlocoRequest> inlineMediaBlocks,
                                 PeticionamentoMultimidiaComposerService.CompositionReport multimediaComposition,
                                 PeticionamentoMediaSecurityPipelineService.SecurityReport mediaSecurity,
                                 PeticionamentoMediaStorageShieldService.StorageShieldReport mediaStorageShield,
                                 PeticionamentoPericiaEvidenceIntelligenceService.PericiaEvidenceReport periciaEvidence,
                                 boolean preparingProtocolPackage) {
        public ResolveRequest {
            inlineMediaBlocks = inlineMediaBlocks == null ? List.of() : List.copyOf(inlineMediaBlocks);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest(List.of(), null, null, null, null, false);
        }
    }

    public record PublicationGateReport(String profile,
                                        boolean blocking,
                                        boolean pendingPublication,
                                        List<String> publicationGates,
                                        Map<String, Object> workspace,
                                        Map<String, Object> protocolSection) {
        public PublicationGateReport {
            profile = Objects.requireNonNullElse(profile, "PETICAO_SEM_MIDIA_INLINE");
            publicationGates = publicationGates == null ? List.of() : List.copyOf(publicationGates);
            workspace = workspace == null ? Map.of() : Map.copyOf(workspace);
            protocolSection = protocolSection == null ? Map.of() : Map.copyOf(protocolSection);
        }
    }
}
