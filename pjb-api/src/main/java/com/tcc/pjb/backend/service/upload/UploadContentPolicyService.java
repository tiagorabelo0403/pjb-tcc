package com.tcc.pjb.backend.service.upload;

import com.tcc.pjb.backend.core.storage.ObjectStorageProperties;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class UploadContentPolicyService {

    private static final String ASSET_DOCUMENTO = "DOCUMENTO";
    private static final String ASSET_IMAGEM = "IMAGEM";
    private static final String ASSET_AUDIO = "AUDIO";
    private static final String ASSET_VIDEO = "VIDEO";

    private final ObjectStorageProperties.Upload uploadProperties;
    private final Map<String, Policy> byContentType;
    private final Map<String, String> byExtension;

    public UploadContentPolicyService() {
        this(new ObjectStorageProperties());
    }

    public UploadContentPolicyService(ObjectStorageProperties props) {
        ObjectStorageProperties properties = props == null ? new ObjectStorageProperties() : props;
        this.uploadProperties = properties.getUpload() == null ? new ObjectStorageProperties.Upload() : properties.getUpload();
        this.byContentType = Map.copyOf(buildPolicies(this.uploadProperties));
        this.byExtension = Map.copyOf(buildExtensionFallback(this.byContentType));
    }

    public Policy resolveReservation(String nomeOriginal, String contentType, long tamanhoBytes) {
        String normalized = normalizeContentType(contentType);
        if (normalized == null) {
            normalized = inferFromFilename(nomeOriginal);
        }
        Policy policy = byContentType.get(normalized);
        if (policy == null) {
            throw new IllegalArgumentException("tipo de arquivo não permitido para upload processual");
        }
        if (tamanhoBytes <= 0L) {
            throw new IllegalArgumentException("tamanho do arquivo deve ser positivo");
        }
        if (tamanhoBytes > policy.maxBytes()) {
            throw new IllegalArgumentException("arquivo excedeu o limite prudencial para " + policy.assetClass().toLowerCase(Locale.ROOT));
        }
        return policy;
    }

    public void assertMagicBytes(String contentType, PushbackInputStream in) throws IOException {
        Policy policy = byContentType.get(normalizeContentType(contentType));
        if (policy == null) {
            throw new IllegalArgumentException("tipo de arquivo não permitido para ingestão");
        }
        byte[] header = in.readNBytes(32);
        if (header.length < policy.minHeaderBytes()) {
            throw new IllegalArgumentException("arquivo insuficiente para validação de assinatura");
        }
        in.unread(header);
        if (!policy.signature().matches(header)) {
            throw new IllegalArgumentException("assinatura binária incompatível com o tipo de arquivo reservado");
        }
    }

    public IngestionPlan buildIngestionPlan(Policy policy, long sizeBytes) {
        Policy safePolicy = Objects.requireNonNull(policy, "policy");
        long size = Math.max(0L, sizeBytes);
        boolean deepInspectionRequired = size >= safe(uploadProperties.getDeepInspectionThresholdBytes(), 50_331_648L)
                || ASSET_VIDEO.equals(safePolicy.assetClass())
                || ASSET_AUDIO.equals(safePolicy.assetClass());
        boolean syncPreview = size > 0L && size <= safe(uploadProperties.getSyncPreviewMaxBytes(), 8_388_608L) && !ASSET_VIDEO.equals(safePolicy.assetClass());
        String sizeClass = size <= 5L * 1024L * 1024L ? "LEVE" : size <= 32L * 1024L * 1024L ? "MODERADO" : "PESADO";
        String processingLane;
        if (ASSET_VIDEO.equals(safePolicy.assetClass())) {
            processingLane = "ASYNC_STREAMING_FORENSE";
        } else if (ASSET_AUDIO.equals(safePolicy.assetClass())) {
            processingLane = size > safe(uploadProperties.getSyncPreviewMaxBytes(), 8_388_608L) ? "ASYNC_TRANSCRICAO_FORENSE" : "SYNC_TRANSCRICAO_RESUMIDA";
        } else if (ASSET_IMAGEM.equals(safePolicy.assetClass())) {
            processingLane = syncPreview ? "SYNC_PREVIEW_CANONICO" : "ASYNC_PREVIEW_CANONICO";
        } else {
            processingLane = "SYNC_DOCUMENT_GATE";
        }
        String previewProfile = ASSET_VIDEO.equals(safePolicy.assetClass())
                ? "THUMBNAIL_E_STREAM_SEGURO"
                : ASSET_AUDIO.equals(safePolicy.assetClass())
                ? "WAVEFORM_E_TRANSCRICAO"
                : ASSET_IMAGEM.equals(safePolicy.assetClass())
                ? "THUMBNAIL_SANITIZADO"
                : "VISUALIZADOR_PDF_CONTROLADO";
        return new IngestionPlan(
                safePolicy.assetClass(),
                sizeClass,
                processingLane,
                previewProfile,
                deepInspectionRequired,
                ASSET_VIDEO.equals(safePolicy.assetClass()) || ASSET_AUDIO.equals(safePolicy.assetClass()) || ASSET_IMAGEM.equals(safePolicy.assetClass())
        );
    }

    public Map<String, Object> governanceSummary() {
        LinkedHashMap<String, Object> limits = new LinkedHashMap<>();
        limits.put(ASSET_DOCUMENTO, safe(uploadProperties.getMaxDocumentBytes(), 26_214_400L));
        limits.put(ASSET_IMAGEM, safe(uploadProperties.getMaxImageBytes(), 12_582_912L));
        limits.put(ASSET_AUDIO, safe(uploadProperties.getMaxAudioBytes(), 33_554_432L));
        limits.put(ASSET_VIDEO, safe(uploadProperties.getMaxVideoBytes(), 134_217_728L));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("maxBatchItems", Math.max(1, uploadProperties.getMaxBatchItems()));
        out.put("maxBatchTotalBytes", safe(uploadProperties.getMaxBatchTotalBytes(), 536_870_912L));
        out.put("maxPerFileBytes", Map.copyOf(limits));
        out.put("syncPreviewMaxBytes", safe(uploadProperties.getSyncPreviewMaxBytes(), 8_388_608L));
        out.put("deepInspectionThresholdBytes", safe(uploadProperties.getDeepInspectionThresholdBytes(), 50_331_648L));
        return Collections.unmodifiableMap(out);
    }

    private String inferFromFilename(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return null;
        }
        int idx = nomeOriginal.lastIndexOf('.');
        if (idx < 0 || idx == nomeOriginal.length() - 1) {
            return null;
        }
        String ext = nomeOriginal.substring(idx + 1).trim().toLowerCase(Locale.ROOT);
        return byExtension.get(ext);
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        String value = contentType.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }
        int semi = value.indexOf(';');
        if (semi > 0) {
            value = value.substring(0, semi).trim();
        }
        return value.isEmpty() ? null : value;
    }

    private static Map<String, Policy> buildPolicies(ObjectStorageProperties.Upload upload) {
        ObjectStorageProperties.Upload cfg = upload == null ? new ObjectStorageProperties.Upload() : upload;
        LinkedHashMap<String, Policy> out = new LinkedHashMap<>();
        out.put("application/pdf", new Policy("application/pdf", ".pdf", ASSET_DOCUMENTO, safe(cfg.getMaxDocumentBytes(), 26_214_400L), 5, header -> ascii(header, 0, 5).startsWith("%PDF-")));
        out.put("image/jpeg", new Policy("image/jpeg", ".jpg", ASSET_IMAGEM, safe(cfg.getMaxImageBytes(), 12_582_912L), 3, header -> match(header, 0xFF, 0xD8, 0xFF)));
        out.put("image/png", new Policy("image/png", ".png", ASSET_IMAGEM, safe(cfg.getMaxImageBytes(), 12_582_912L), 8, header -> match(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)));
        out.put("image/webp", new Policy("image/webp", ".webp", ASSET_IMAGEM, safe(cfg.getMaxImageBytes(), 12_582_912L), 12, header -> ascii(header, 0, 4).equals("RIFF") && ascii(header, 8, 4).equals("WEBP")));
        out.put("image/heic", new Policy("image/heic", ".heic", ASSET_IMAGEM, safe(cfg.getMaxImageBytes(), 12_582_912L), 12, header -> ascii(header, 4, 4).equals("ftyp") && matchesAny(ascii(header, 8, 4), "heic", "heif", "mif1", "msf1")));
        out.put("audio/mpeg", new Policy("audio/mpeg", ".mp3", ASSET_AUDIO, safe(cfg.getMaxAudioBytes(), 33_554_432L), 3, header -> ascii(header, 0, 3).equals("ID3") || (unsigned(header, 0) == 0xFF && (unsigned(header, 1) & 0xE0) == 0xE0)));
        out.put("audio/mp4", new Policy("audio/mp4", ".m4a", ASSET_AUDIO, safe(cfg.getMaxAudioBytes(), 33_554_432L), 12, header -> ascii(header, 4, 4).equals("ftyp") && matchesAny(ascii(header, 8, 4), "M4A ", "isom", "mp42")));
        out.put("audio/wav", new Policy("audio/wav", ".wav", ASSET_AUDIO, safe(cfg.getMaxAudioBytes(), 33_554_432L), 12, header -> ascii(header, 0, 4).equals("RIFF") && ascii(header, 8, 4).equals("WAVE")));
        out.put("audio/x-wav", new Policy("audio/x-wav", ".wav", ASSET_AUDIO, safe(cfg.getMaxAudioBytes(), 33_554_432L), 12, header -> ascii(header, 0, 4).equals("RIFF") && ascii(header, 8, 4).equals("WAVE")));
        out.put("audio/ogg", new Policy("audio/ogg", ".ogg", ASSET_AUDIO, safe(cfg.getMaxAudioBytes(), 33_554_432L), 4, header -> ascii(header, 0, 4).equals("OggS")));
        out.put("video/mp4", new Policy("video/mp4", ".mp4", ASSET_VIDEO, safe(cfg.getMaxVideoBytes(), 134_217_728L), 12, header -> ascii(header, 4, 4).equals("ftyp") && matchesAny(ascii(header, 8, 4), "isom", "iso2", "mp41", "mp42", "avc1")));
        out.put("video/quicktime", new Policy("video/quicktime", ".mov", ASSET_VIDEO, safe(cfg.getMaxVideoBytes(), 134_217_728L), 12, header -> ascii(header, 4, 4).equals("ftyp") && ascii(header, 8, 4).equals("qt  ")));
        out.put("video/webm", new Policy("video/webm", ".webm", ASSET_VIDEO, safe(cfg.getMaxVideoBytes(), 134_217_728L), 4, header -> match(header, 0x1A, 0x45, 0xDF, 0xA3)));
        return out;
    }

    private static Map<String, String> buildExtensionFallback(Map<String, Policy> byContentType) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (Policy policy : byContentType.values()) {
            out.put(policy.storageExtension().substring(1).toLowerCase(Locale.ROOT), policy.normalizedContentType());
        }
        out.put("jpeg", "image/jpeg");
        out.put("jpg", "image/jpeg");
        out.put("m4a", "audio/mp4");
        out.put("mov", "video/quicktime");
        return out;
    }

    private static long safe(long value, long fallback) {
        return value > 0L ? value : fallback;
    }

    private static boolean match(byte[] header, int... expected) {
        if (header == null || header.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (unsigned(header, i) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static int unsigned(byte[] header, int idx) {
        return Byte.toUnsignedInt(header[idx]);
    }

    private static boolean matchesAny(String value, String... options) {
        for (String option : options) {
            if (Objects.equals(value, option)) {
                return true;
            }
        }
        return false;
    }

    private static String ascii(byte[] header, int start, int len) {
        if (header == null || start < 0 || len <= 0 || header.length < start + len) {
            return "";
        }
        return new String(header, start, len, StandardCharsets.US_ASCII);
    }

    public record Policy(
            String normalizedContentType,
            String storageExtension,
            String assetClass,
            long maxBytes,
            int minHeaderBytes,
            SignatureMatcher signature) {
        public Policy {
            Objects.requireNonNull(normalizedContentType, "normalizedContentType");
            Objects.requireNonNull(storageExtension, "storageExtension");
            Objects.requireNonNull(assetClass, "assetClass");
            Objects.requireNonNull(signature, "signature");
        }
    }

    public record IngestionPlan(
            String assetClass,
            String sizeClass,
            String processingLane,
            String previewProfile,
            boolean deepInspectionRequired,
            boolean canonicalizationRecommended) {
    }

    @FunctionalInterface
    public interface SignatureMatcher {
        boolean matches(byte[] header);
    }
}
