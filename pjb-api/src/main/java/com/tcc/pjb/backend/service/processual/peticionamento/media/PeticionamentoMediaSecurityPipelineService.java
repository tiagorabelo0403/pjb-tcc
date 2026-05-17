package com.tcc.pjb.backend.service.processual.peticionamento.media;

import com.tcc.pjb.backend.core.storage.ObjectStorageProperties;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.upload.UploadContentPolicyService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

@Service
public class PeticionamentoMediaSecurityPipelineService {

    private static final Set<String> IMAGE_MIME = Set.of("image/jpeg", "image/png", "image/webp", "image/heic");
    private static final Set<String> AUDIO_MIME = Set.of("audio/mpeg", "audio/mp4", "audio/wav", "audio/x-wav", "audio/ogg");
    private static final Set<String> VIDEO_MIME = Set.of("video/mp4", "video/quicktime", "video/webm");
    private static final Set<String> DOCUMENT_MIME = Set.of("application/pdf");

    private final ObjectStorageProperties.Upload uploadProperties;
    private final UploadContentPolicyService uploadContentPolicyService;

    @Inject
    public PeticionamentoMediaSecurityPipelineService() {
        this(new ObjectStorageProperties(), new UploadContentPolicyService());
    }

    public PeticionamentoMediaSecurityPipelineService(ObjectStorageProperties props,
                                                      UploadContentPolicyService uploadContentPolicyService) {
        ObjectStorageProperties properties = props == null ? new ObjectStorageProperties() : props;
        this.uploadProperties = properties.getUpload() == null ? new ObjectStorageProperties.Upload() : properties.getUpload();
        this.uploadContentPolicyService = uploadContentPolicyService == null ? new UploadContentPolicyService(properties) : uploadContentPolicyService;
    }

    public SecurityReport assess(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<String> blockers = new ArrayList<>();
        ArrayList<String> alerts = new ArrayList<>();
        ArrayList<Map<String, Object>> evaluated = new ArrayList<>();
        int quarantined = 0;
        int sensitiveBlurred = 0;

        for (PeticionamentoMediaBlocoRequest block : safe.inlineMediaBlocks()) {
            if (block == null) {
                continue;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<>(block.toMap());
            item.put("validationStack", validationStackFor(block.tipoResolvido()));
            item.put("quarantineRequired", true);
            item.put("aiOnlyForbidden", true);
            item.put("previewMode", block.blurInicialObrigatorio() ? "BLURRED" : "NORMAL");
            item.put("magistradoPodeDesborrar", block.magistradoPodeDesborrarResolvido());
            item.put("advogadoContrarioPodeDesborrar", block.advogadoContrarioPodeDesborrarResolvido());
            item.put("policyStatus", "PENDENTE_VALIDACAO_TRIPLA");
            if (block.getMimeType() != null && block.getTamanhoBytes() != null && block.getTamanhoBytes() > 0L) {
                try {
                    var policy = uploadContentPolicyService.resolveReservation(block.getStorageKey(), block.getMimeType(), block.getTamanhoBytes());
                    var ingestionPlan = uploadContentPolicyService.buildIngestionPlan(policy, block.getTamanhoBytes());
                    item.put("processingLane", ingestionPlan.processingLane());
                    item.put("previewProfile", ingestionPlan.previewProfile());
                    item.put("deepInspectionRequired", ingestionPlan.deepInspectionRequired());
                } catch (RuntimeException ignored) {
                    item.put("processingLane", "PENDENTE_CLASSIFICACAO");
                }
            }
            evaluated.add(Map.copyOf(item));
            quarantined++;
            if (block.blurInicialObrigatorio()) {
                sensitiveBlurred++;
            }

            if ("DOCUMENTO".equals(block.tipoResolvido())) {
                blockers.add("Bloco " + block.ancoraResolvida() + " não pode carregar documento inline; use o bloco pós-petição de anexos.");
            }
            if (!block.metadataMinimaPresente()) {
                blockers.add("Bloco " + block.ancoraResolvida() + " sem metadado mínimo de rastreio, storage ou hash.");
            }
            if (!mimeAllowed(block)) {
                blockers.add("Bloco " + block.ancoraResolvida() + " com MIME não permitido para o tipo " + block.tipoResolvido().toLowerCase(Locale.ROOT) + ".");
            }
            if (sizeExceeded(block)) {
                blockers.add("Bloco " + block.ancoraResolvida() + " excedeu o limite prudencial de tamanho para o tipo informado.");
            }
            if (durationExceeded(block)) {
                blockers.add("Bloco " + block.ancoraResolvida() + " excedeu a duração prudencial inline; particione a prova, gere recorte ou deixe o material completo apenas no bloco pós-petição.");
            }
            if (block.blurInicialObrigatorio() && !block.magistradoPodeDesborrarResolvido()) {
                blockers.add("Bloco sensível " + block.ancoraResolvida() + " precisa permitir desborramento pelo magistrado.");
            }
            if (block.blurInicialObrigatorio() && !Boolean.TRUE.equals(block.getSensivelAdultoDeclarado()) && !Boolean.TRUE.equals(block.getContextoProbatorioSensivel())) {
                alerts.add("Bloco " + block.ancoraResolvida() + " ativou blur sem declaração expressa de sensibilidade; revisar classificação probatória.");
            }
            if (("AUDIO".equals(block.tipoResolvido()) || "VIDEO".equals(block.tipoResolvido())) && !hasText(block.getDescricao())) {
                alerts.add("Bloco " + block.ancoraResolvida() + " deveria informar descrição resumida e contexto fático para apoiar triagem e transcrição.");
            }
        }

        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("tripleShield", List.of(
                "ESTRUTURA_ALLOWLIST_MAGIC_BYTES",
                "CLAMAV_YARA_CANONICALIZACAO",
                "MODERACAO_GOVERNANCA_BLUR"
        ));
        workspace.put("quarantineMode", quarantined > 0 ? "OBRIGATORIO_ANTES_DA_PUBLICACAO" : "NAO_APLICAVEL");
        workspace.put("quarantineCount", quarantined);
        workspace.put("sensitiveBlurredCount", sensitiveBlurred);
        workspace.put("inlineMediaSecurity", List.copyOf(evaluated));
        workspace.put("contentPreviewPolicy", Map.of(
                "adultSensitiveDefault", "BLURRED",
                "judgeReveal", true,
                "opposingCounselReveal", "CONDICIONAL_MARCADO_PELO_PETICIONANTE",
                "publicPreview", false,
                "documentInlineForbidden", true
        ));
        workspace.put("runtimeDetectors", List.of("CLAMAV", "YARA", "FFMPEG_CANONICAL", "AI_COMPLEMENTAR"));
        workspace.put("allowlistPolicy", Map.of(
                "IMAGEM", IMAGE_MIME,
                "AUDIO", AUDIO_MIME,
                "VIDEO", VIDEO_MIME,
                "DOCUMENTO", DOCUMENT_MIME
        ));
        workspace.put("durationPolicy", Map.of(
                "maxAudioDurationMs", positiveOr(uploadProperties.getMaxAudioDurationMs(), 900_000L),
                "maxVideoDurationMs", positiveOr(uploadProperties.getMaxVideoDurationMs(), 900_000L)
        ));

        return new SecurityReport(
                resolveProfile(safe, blockers, sensitiveBlurred),
                !blockers.isEmpty(),
                List.copyOf(blockers),
                List.copyOf(alerts),
                Map.copyOf(workspace),
                Map.of(
                        "tripleShield", workspace.get("tripleShield"),
                        "quarantineMode", workspace.get("quarantineMode"),
                        "contentPreviewPolicy", workspace.get("contentPreviewPolicy"),
                        "durationPolicy", workspace.get("durationPolicy")
                )
        );
    }

    private static boolean mimeAllowed(PeticionamentoMediaBlocoRequest block) {
        String mime = normalizeMime(block.getMimeType());
        if (mime == null) {
            return false;
        }
        return switch (block.tipoResolvido()) {
            case "IMAGEM" -> IMAGE_MIME.contains(mime);
            case "AUDIO" -> AUDIO_MIME.contains(mime);
            case "VIDEO" -> VIDEO_MIME.contains(mime);
            default -> DOCUMENT_MIME.contains(mime);
        };
    }

    private boolean sizeExceeded(PeticionamentoMediaBlocoRequest block) {
        Long size = block.getTamanhoBytes();
        if (size == null || size <= 0L) {
            return false;
        }
        return switch (block.tipoResolvido()) {
            case "IMAGEM" -> size > positiveOr(uploadProperties.getMaxInlineImageBytes(), 10_485_760L);
            case "AUDIO" -> size > positiveOr(uploadProperties.getMaxInlineAudioBytes(), 25_165_824L);
            case "VIDEO" -> size > positiveOr(uploadProperties.getMaxInlineVideoBytes(), 100_663_296L);
            default -> size > positiveOr(uploadProperties.getMaxDocumentBytes(), 26_214_400L);
        };
    }

    private boolean durationExceeded(PeticionamentoMediaBlocoRequest block) {
        Long duration = block.getDuracaoMs();
        if (duration == null || duration <= 0L) {
            return false;
        }
        return switch (block.tipoResolvido()) {
            case "AUDIO" -> duration > positiveOr(uploadProperties.getMaxAudioDurationMs(), 900_000L);
            case "VIDEO" -> duration > positiveOr(uploadProperties.getMaxVideoDurationMs(), 900_000L);
            default -> false;
        };
    }

    private static long positiveOr(long value, long fallback) {
        return value > 0L ? value : fallback;
    }

    private static List<String> validationStackFor(String tipo) {
        return switch (Objects.requireNonNullElse(tipo, "DOCUMENTO")) {
            case "IMAGEM" -> List.of("MAGIC_BYTES", "CLAMAV", "YARA", "REENCODE_IMAGEM", "MODERACAO_SENSIVEL");
            case "AUDIO" -> List.of("MAGIC_BYTES", "CLAMAV", "YARA", "TRANSCODE_AUDIO", "TRANSCRICAO", "MODERACAO_SENSIVEL");
            case "VIDEO" -> List.of("MAGIC_BYTES", "CLAMAV", "YARA", "TRANSCODE_VIDEO", "THUMBNAIL", "TRANSCRICAO", "MODERACAO_SENSIVEL");
            default -> List.of("MAGIC_BYTES", "CLAMAV", "YARA", "CDR_PDF");
        };
    }

    private static String resolveProfile(ResolveRequest request, List<String> blockers, int sensitiveBlurred) {
        if (!blockers.isEmpty()) {
            return "MIDIA_BLOQUEADA_EM_QUARENTENA";
        }
        if (sensitiveBlurred > 0) {
            return "MIDIA_SENSIVEL_BLUR_CONTROLADO";
        }
        return request.inlineMediaBlocks().isEmpty() ? "SEM_MIDIA_INLINE" : "MIDIA_VALIDACAO_TRIPLA";
    }

    private static String normalizeMime(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record ResolveRequest(
            List<PeticionamentoMediaBlocoRequest> inlineMediaBlocks,
            TipoUsuario tipoUsuario,
            boolean sigiloSensivel) {
        public ResolveRequest {
            inlineMediaBlocks = inlineMediaBlocks == null ? List.of() : List.copyOf(inlineMediaBlocks);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest(List.of(), null, false);
        }
    }

    public record SecurityReport(
            String profile,
            boolean blocking,
            List<String> blockers,
            List<String> alerts,
            Map<String, Object> workspace,
            Map<String, Object> protocolSection) {
        public SecurityReport {
            profile = Objects.requireNonNullElse(profile, "SEM_MIDIA_INLINE");
            blockers = blockers == null ? List.of() : List.copyOf(blockers);
            alerts = alerts == null ? List.of() : List.copyOf(alerts);
            workspace = workspace == null ? Map.of() : Map.copyOf(workspace);
            protocolSection = protocolSection == null ? Map.of() : Map.copyOf(protocolSection);
        }
    }
}
