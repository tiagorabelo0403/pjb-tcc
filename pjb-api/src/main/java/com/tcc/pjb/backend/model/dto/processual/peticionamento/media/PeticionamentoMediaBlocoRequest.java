
package com.tcc.pjb.backend.model.dto.processual.peticionamento.media;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionamentoMediaBlocoRequest {
    @Size(max = 120)
    private String blocoId;
    @Size(max = 32)
    private String tipo;
    @Size(max = 48)
    private String categoria;
    @Size(max = 160)
    private String ancora;
    @Size(max = 240)
    private String titulo;
    @Size(max = 4000)
    private String descricao;
    @Size(max = 240)
    private String uploadItemId;
    @Size(max = 500)
    private String storageKey;
    @Pattern(regexp = "^$|^[A-Fa-f0-9]{96}$")
    private String hashSha384;
    @Size(max = 160)
    private String mimeType;
    @Min(0)
    @Max(524288000)
    private Long tamanhoBytes;
    @Min(0)
    @Max(21600000)
    private Long duracaoMs;
    private Boolean sensivelAdultoDeclarado;
    private Boolean exigirBlurInicial;
    private Boolean contextoProbatorioSensivel;
    private Boolean magistradoPodeDesborrar;
    private Boolean advogadoContrarioPodeDesborrar;

    @JsonIgnore
    public String blocoIdResolvido() {
        if (hasText(blocoId)) {
            return blocoId.trim();
        }
        String seed = String.join("|",
                safe(tipo),
                safe(categoria),
                safe(ancora),
                safe(titulo),
                safe(uploadItemId),
                safe(storageKey));
        return UUID.nameUUIDFromBytes(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    @JsonIgnore
    public String tipoResolvido() {
        String normalized = normalizeUpper(tipo);
        if (normalized == null) {
            return "DOCUMENTO";
        }
        return switch (normalized) {
            case "IMAGEM", "IMAGE", "FOTO" -> "IMAGEM";
            case "AUDIO", "MP3", "SOM" -> "AUDIO";
            case "VIDEO", "MP4", "FILME" -> "VIDEO";
            default -> "DOCUMENTO";
        };
    }

    @JsonIgnore
    public String categoriaResolvida() {
        String normalized = normalizeUpper(categoria);
        if (normalized == null) {
            return "INLINE_NARRATIVA";
        }
        return switch (normalized) {
            case "PROVA_DOCUMENTAL", "PROVAS_DOCUMENTAIS" -> "PROVA_DOCUMENTAL";
            case "DOCUMENTO_PESSOAL", "DOCUMENTOS_PESSOAIS" -> "DOCUMENTO_PESSOAL";
            case "DOCUMENTO_REPRESENTACAO", "REPRESENTACAO" -> "DOCUMENTO_REPRESENTACAO";
            case "PROVA_TECNICA" -> "PROVA_TECNICA";
            case "MIDIA_SENSIVEL", "SENSIVEL" -> "MIDIA_SENSIVEL";
            default -> "INLINE_NARRATIVA";
        };
    }

    @JsonIgnore
    public boolean blurInicialObrigatorio() {
        return Boolean.TRUE.equals(exigirBlurInicial)
                || Boolean.TRUE.equals(sensivelAdultoDeclarado)
                || Boolean.TRUE.equals(contextoProbatorioSensivel)
                || "MIDIA_SENSIVEL".equals(categoriaResolvida());
    }

    @JsonIgnore
    public boolean magistradoPodeDesborrarResolvido() {
        return magistradoPodeDesborrar == null || magistradoPodeDesborrar;
    }

    @JsonIgnore
    public boolean advogadoContrarioPodeDesborrarResolvido() {
        return Boolean.TRUE.equals(advogadoContrarioPodeDesborrar);
    }

    @JsonIgnore
    public boolean metadataMinimaPresente() {
        return hasText(uploadItemId) || hasText(storageKey) || hasText(hashSha384);
    }

    @JsonIgnore
    public String ancoraResolvida() {
        if (hasText(ancora)) {
            return ancora.trim();
        }
        return "bloco-" + blocoIdResolvido();
    }

    @JsonIgnore
    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        put(map, "blocoId", blocoIdResolvido());
        put(map, "tipo", tipoResolvido());
        put(map, "categoria", categoriaResolvida());
        put(map, "ancora", ancoraResolvida());
        put(map, "titulo", trimToNull(titulo));
        put(map, "descricao", trimToNull(descricao));
        put(map, "uploadItemId", trimToNull(uploadItemId));
        put(map, "storageKey", trimToNull(storageKey));
        put(map, "hashSha384", trimToNull(hashSha384));
        put(map, "mimeType", trimToNull(mimeType));
        put(map, "tamanhoBytes", tamanhoBytes);
        put(map, "duracaoMs", duracaoMs);
        put(map, "sensivelAdultoDeclarado", Boolean.TRUE.equals(sensivelAdultoDeclarado));
        put(map, "exigirBlurInicial", blurInicialObrigatorio());
        put(map, "contextoProbatorioSensivel", Boolean.TRUE.equals(contextoProbatorioSensivel));
        put(map, "magistradoPodeDesborrar", magistradoPodeDesborrarResolvido());
        put(map, "advogadoContrarioPodeDesborrar", advogadoContrarioPodeDesborrarResolvido());
        return Map.copyOf(map);
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        target.put(key, value);
    }

    private static String normalizeUpper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(java.util.Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean hasText(String value) {
        return trimToNull(value) != null;
    }

    private static String safe(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "" : normalized;
    }
}
