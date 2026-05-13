package com.tcc.pjb.backend.service.processual.peticionamento;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoEnderecoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoVisualIdentityRequest;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoPayloadHardeningService {

    private static final int MAX_TITLE = 240;
    private static final int MAX_NAME = 320;
    private static final int MAX_SHORT_CODE = 120;
    private static final int MAX_MATERIA = 160;
    private static final int MAX_SUMMARY_TEXT = 8_000;
    private static final int MAX_LONG_TEXT = 120_000;
    private static final int MAX_PROTOCOL_TITLE = 240;
    private static final int MAX_REFERENCE = 240;
    private static final int MAX_LIST_ITEMS = 96;
    private static final int MAX_LIST_ITEM_TEXT = 2_000;
    private static final int MAX_DOCUMENT_NAME = 260;
    private static final int MAX_CTX_ENTRIES = 64;
    private static final int MAX_CTX_DEPTH = 3;
    private static final int MAX_CTX_STRING = 1_000;
    private static final int MAX_ADDRESS_FIELD = 160;
    private static final int MAX_IDENTITY_TEXT = 600;
    private static final Pattern UF_PATTERN = Pattern.compile("^[A-Z]{2}$");
    private static final Pattern CEP_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern CTX_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_.:-]{1,80}$");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$");
    private static final Set<String> TRUSTED_URI_PREFIXES = Set.of("https://", "data:image/", "/", "assets/", "storage/");

    public HardenedPayload harden(PeticionamentoSessaoRequest request) {
        PeticionamentoSessaoRequest source = request == null ? PeticionamentoSessaoRequest.builder().tituloCaso("PETIÇÃO INICIAL").build() : request;
        ArrayList<String> diagnostics = new ArrayList<>();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();

        PeticionamentoSessaoRequest hardened = PeticionamentoSessaoRequest.builder()
                .modo(normalizeToken(source.getModo(), 32, diagnostics, "modo", false))
                .processoId(normalizePositiveLong(source.getProcessoId(), diagnostics, "processoId"))
                .tituloCaso(defaultIfBlank(sanitizeInline(source.getTituloCaso(), MAX_TITLE, diagnostics, "tituloCaso"), "PETIÇÃO INICIAL"))
                .parteAutora(sanitizeInline(source.getParteAutora(), MAX_NAME, diagnostics, "parteAutora"))
                .parteRe(sanitizeInline(source.getParteRe(), MAX_NAME, diagnostics, "parteRe"))
                .ramoDireito(normalizeToken(source.getRamoDireito(), MAX_SHORT_CODE, diagnostics, "ramoDireito", false))
                .ritoProcessual(normalizeToken(source.getRitoProcessual(), MAX_SHORT_CODE, diagnostics, "ritoProcessual", false))
                .classeProcessual(sanitizeInline(source.getClasseProcessual(), MAX_SHORT_CODE, diagnostics, "classeProcessual"))
                .assuntoTpu(sanitizeInline(source.getAssuntoTpu(), MAX_SHORT_CODE, diagnostics, "assuntoTpu"))
                .materiaPrincipal(sanitizeInline(source.getMateriaPrincipal(), MAX_MATERIA, diagnostics, "materiaPrincipal"))
                .tipoJustica(normalizeToken(source.getTipoJustica(), 40, diagnostics, "tipoJustica", false))
                .textoFatosResumido(sanitizeMultiline(source.getTextoFatosResumido(), MAX_SUMMARY_TEXT, diagnostics, "textoFatosResumido"))
                .valorCausa(normalizeValorCausa(source.getValorCausa(), diagnostics))
                .tutelaUrgencia(source.getTutelaUrgencia())
                .casoUrgente(source.getCasoUrgente())
                .preferenciaDigital(source.getPreferenciaDigital())
                .requerLiminar(source.getRequerLiminar())
                .requerJuizadoEspecial(source.getRequerJuizadoEspecial())
                .requerVaraEspecializada(source.getRequerVaraEspecializada())
                .draftMarkdown(sanitizeMultiline(source.getDraftMarkdown(), MAX_LONG_TEXT, diagnostics, "draftMarkdown"))
                .textoPeticaoLivre(sanitizeMultiline(source.getTextoPeticaoLivre(), MAX_LONG_TEXT, diagnostics, "textoPeticaoLivre"))
                .cidadeFato(sanitizeInline(source.getCidadeFato(), MAX_ADDRESS_FIELD, diagnostics, "cidadeFato"))
                .ufFato(normalizeUf(source.getUfFato(), diagnostics, "ufFato"))
                .cidadeProtocolo(sanitizeInline(source.getCidadeProtocolo(), MAX_ADDRESS_FIELD, diagnostics, "cidadeProtocolo"))
                .ufProtocolo(normalizeUf(source.getUfProtocolo(), diagnostics, "ufProtocolo"))
                .naturezaJuridica(sanitizeInline(source.getNaturezaJuridica(), MAX_MATERIA, diagnostics, "naturezaJuridica"))
                .protocolTitle(sanitizeInline(source.getProtocolTitle(), MAX_PROTOCOL_TITLE, diagnostics, "protocolTitle"))
                .tipoInstrumentoRepresentacao(sanitizeInline(source.getTipoInstrumentoRepresentacao(), MAX_SHORT_CODE, diagnostics, "tipoInstrumentoRepresentacao"))
                .audienciaId(normalizePositiveLong(source.getAudienciaId(), diagnostics, "audienciaId"))
                .tipoAudiencia(sanitizeInline(source.getTipoAudiencia(), MAX_SHORT_CODE, diagnostics, "tipoAudiencia"))
                .contextoConsensual(source.getContextoConsensual())
                .poderesEspeciaisTransigir(source.getPoderesEspeciaisTransigir())
                .termoAudienciaReferencia(sanitizeInline(source.getTermoAudienciaReferencia(), MAX_REFERENCE, diagnostics, "termoAudienciaReferencia"))
                .ataAudienciaReferencia(sanitizeInline(source.getAtaAudienciaReferencia(), MAX_REFERENCE, diagnostics, "ataAudienciaReferencia"))
                .prepararPacoteProtocolo(source.getPrepararPacoteProtocolo())
                .resolverEnderecoAutomaticamente(source.getResolverEnderecoAutomaticamente())
                .fatos(sanitizeTextList(source.getFatos(), MAX_LIST_ITEMS, MAX_LIST_ITEM_TEXT, diagnostics, "fatos"))
                .fundamentosJuridicos(sanitizeTextList(source.getFundamentosJuridicos(), MAX_LIST_ITEMS, MAX_LIST_ITEM_TEXT, diagnostics, "fundamentosJuridicos"))
                .pedidos(sanitizeTextList(source.getPedidos(), MAX_LIST_ITEMS, MAX_LIST_ITEM_TEXT, diagnostics, "pedidos"))
                .provasIndicadas(sanitizeTextList(source.getProvasIndicadas(), MAX_LIST_ITEMS, MAX_LIST_ITEM_TEXT, diagnostics, "provasIndicadas"))
                .documentosAnexados(sanitizeTextList(source.getDocumentosAnexados(), MAX_LIST_ITEMS, MAX_DOCUMENT_NAME, diagnostics, "documentosAnexados"))
                .ctx(sanitizeContext(source.getCtx(), diagnostics))
                .enderecoAutor(sanitizeAddress(source.getEnderecoAutor(), diagnostics, "enderecoAutor"))
                .enderecoReu(sanitizeAddress(source.getEnderecoReu(), diagnostics, "enderecoReu"))
                .identidadeVisual(sanitizeIdentity(source.getIdentidadeVisual(), diagnostics))
                .build();

        String fingerprint = computeFingerprint(hardened);
        metadata.put("profile", "PETICIONAMENTO_INPUT_HARDENING_V2");
        metadata.put("fingerprint", fingerprint);
        metadata.put("diagnosticsCount", diagnostics.size());
        metadata.put("documentCount", hardened.getDocumentosAnexados() == null ? 0 : hardened.getDocumentosAnexados().size());
        metadata.put("factCount", hardened.getFatos() == null ? 0 : hardened.getFatos().size());
        metadata.put("ctxEntryCount", hardened.getCtx() == null ? 0 : hardened.getCtx().size());
        metadata.put("longTextPresent", hasText(hardened.getTextoPeticaoLivre()) || hasText(hardened.getDraftMarkdown()));
        metadata.put("brandingEnabled", hardened.getIdentidadeVisual() != null && Boolean.TRUE.equals(hardened.getIdentidadeVisual().getExibirBrasaoOuLogomarca()));
        metadata.put("addressAutoResolutionRequested", hardened.resolverEnderecoAutomaticamenteResolvido());
        return new HardenedPayload(hardened, List.copyOf(diagnostics), Collections.unmodifiableMap(metadata), fingerprint);
    }

    private static PeticionamentoEnderecoRequest sanitizeAddress(PeticionamentoEnderecoRequest source,
                                                                 ArrayList<String> diagnostics,
                                                                 String field) {
        if (source == null) {
            return null;
        }
        return PeticionamentoEnderecoRequest.builder()
                .cep(normalizeCep(source.getCep(), diagnostics, field + ".cep"))
                .logradouro(sanitizeInline(source.getLogradouro(), MAX_ADDRESS_FIELD, diagnostics, field + ".logradouro"))
                .numero(sanitizeInline(source.getNumero(), 30, diagnostics, field + ".numero"))
                .complemento(sanitizeInline(source.getComplemento(), 80, diagnostics, field + ".complemento"))
                .bairro(sanitizeInline(source.getBairro(), 80, diagnostics, field + ".bairro"))
                .cidade(sanitizeInline(source.getCidade(), MAX_ADDRESS_FIELD, diagnostics, field + ".cidade"))
                .uf(normalizeUf(source.getUf(), diagnostics, field + ".uf"))
                .referencia(sanitizeInline(source.getReferencia(), 120, diagnostics, field + ".referencia"))
                .build();
    }

    private static PeticionamentoVisualIdentityRequest sanitizeIdentity(PeticionamentoVisualIdentityRequest source,
                                                                        ArrayList<String> diagnostics) {
        if (source == null) {
            return null;
        }
        PeticionamentoVisualIdentityRequest target = new PeticionamentoVisualIdentityRequest();
        target.setNomeExibicao(sanitizeInline(source.getNomeExibicao(), MAX_IDENTITY_TEXT, diagnostics, "identidadeVisual.nomeExibicao"));
        target.setNomeInstituicao(sanitizeInline(source.getNomeInstituicao(), MAX_IDENTITY_TEXT, diagnostics, "identidadeVisual.nomeInstituicao"));
        target.setBrasaoOuLogomarcaUri(normalizeTrustedUri(source.getBrasaoOuLogomarcaUri(), diagnostics, "identidadeVisual.brasaoOuLogomarcaUri"));
        target.setCabecalhoLivre(sanitizeMultiline(source.getCabecalhoLivre(), 1_500, diagnostics, "identidadeVisual.cabecalhoLivre"));
        target.setRodapeLivre(sanitizeMultiline(source.getRodapeLivre(), 1_500, diagnostics, "identidadeVisual.rodapeLivre"));
        target.setPaletaPrimaria(normalizeHexColor(source.getPaletaPrimaria(), diagnostics, "identidadeVisual.paletaPrimaria"));
        target.setPaletaSecundaria(normalizeHexColor(source.getPaletaSecundaria(), diagnostics, "identidadeVisual.paletaSecundaria"));
        target.setExibirRegistroProfissional(source.getExibirRegistroProfissional());
        target.setExibirBrasaoOuLogomarca(source.getExibirBrasaoOuLogomarca());
        return target;
    }

    private static LinkedHashMap<String, Object> sanitizeContext(Map<String, Object> ctx, ArrayList<String> diagnostics) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (ctx == null || ctx.isEmpty()) {
            return out;
        }
        int count = 0;
        for (Map.Entry<String, Object> entry : ctx.entrySet()) {
            if (count >= MAX_CTX_ENTRIES) {
                diagnostics.add("ctx truncado no limite de " + MAX_CTX_ENTRIES + " entradas.");
                break;
            }
            String key = sanitizeCtxKey(entry.getKey());
            if (key == null) {
                diagnostics.add("ctx ignorou chave inválida ou vazia.");
                continue;
            }
            Object sanitizedValue = sanitizeJsonLikeValue(entry.getValue(), diagnostics, "ctx." + key, 0);
            if (sanitizedValue == null) {
                continue;
            }
            out.put(key, sanitizedValue);
            count++;
        }
        return out;
    }

    private static Object sanitizeJsonLikeValue(Object value,
                                                ArrayList<String> diagnostics,
                                                String path,
                                                int depth) {
        if (value == null || depth > MAX_CTX_DEPTH) {
            if (depth > MAX_CTX_DEPTH) {
                diagnostics.add(path + " ultrapassou a profundidade segura de contexto.");
            }
            return null;
        }
        if (value instanceof String text) {
            return sanitizeInline(text, MAX_CTX_STRING, diagnostics, path);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (count >= 16) {
                    diagnostics.add(path + " truncado no limite de 16 entradas aninhadas.");
                    break;
                }
                String key = sanitizeCtxKey(entry.getKey() == null ? null : entry.getKey().toString());
                if (key == null) {
                    continue;
                }
                Object sanitized = sanitizeJsonLikeValue(entry.getValue(), diagnostics, path + "." + key, depth + 1);
                if (sanitized != null) {
                    nested.put(key, sanitized);
                    count++;
                }
            }
            return nested.isEmpty() ? null : Map.copyOf(nested);
        }
        if (value instanceof List<?> list) {
            ArrayList<Object> nested = new ArrayList<>();
            int limit = Math.min(list.size(), 16);
            for (int i = 0; i < limit; i++) {
                Object sanitized = sanitizeJsonLikeValue(list.get(i), diagnostics, path + "[" + i + "]", depth + 1);
                if (sanitized != null) {
                    nested.add(sanitized);
                }
            }
            if (list.size() > limit) {
                diagnostics.add(path + " truncado no limite de 16 itens.");
            }
            return nested.isEmpty() ? null : List.copyOf(nested);
        }
        String serialized = sanitizeInline(String.valueOf(value), MAX_CTX_STRING, diagnostics, path);
        if (!Objects.equals(serialized, null)) {
            diagnostics.add(path + " convertido para representação textual estável.");
        }
        return serialized;
    }

    private static List<String> sanitizeTextList(List<String> source,
                                                 int maxItems,
                                                 int maxItemLength,
                                                 ArrayList<String> diagnostics,
                                                 String field) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        int index = 0;
        for (String item : source) {
            if (out.size() >= maxItems) {
                diagnostics.add(field + " truncado no limite de " + maxItems + " itens.");
                break;
            }
            String sanitized = sanitizeMultiline(item, maxItemLength, diagnostics, field + "[" + index + "]");
            if (hasText(sanitized)) {
                out.add(sanitized);
            }
            index++;
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static BigDecimal normalizeValorCausa(BigDecimal value, ArrayList<String> diagnostics) {
        if (value == null) {
            return null;
        }
        if (value.signum() < 0) {
            diagnostics.add("valorCausa negativo removido por segurança semântica.");
            return null;
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        if (normalized.scale() > 2) {
            normalized = normalized.setScale(2, java.math.RoundingMode.HALF_UP);
            diagnostics.add("valorCausa arredondado para duas casas decimais.");
        }
        return normalized;
    }

    private static String normalizeCep(String value, ArrayList<String> diagnostics, String field) {
        String inline = sanitizeInline(value, 16, diagnostics, field);
        if (!hasText(inline)) {
            return null;
        }
        String digits = inline.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (!CEP_PATTERN.matcher(digits).matches()) {
            diagnostics.add(field + " ignorado por não corresponder a um CEP de 8 dígitos.");
            return null;
        }
        if (!digits.equals(inline)) {
            diagnostics.add(field + " normalizado para dígitos puros.");
        }
        return digits;
    }

    private static String normalizeUf(String value, ArrayList<String> diagnostics, String field) {
        String inline = sanitizeInline(value, 8, diagnostics, field);
        if (!hasText(inline)) {
            return null;
        }
        String normalized = inline.toUpperCase(Locale.ROOT);
        if (!UF_PATTERN.matcher(normalized).matches()) {
            diagnostics.add(field + " ignorado por não corresponder a uma UF válida de 2 letras.");
            return null;
        }
        return normalized;
    }

    private static String normalizeHexColor(String value, ArrayList<String> diagnostics, String field) {
        String inline = sanitizeInline(value, 16, diagnostics, field);
        if (!hasText(inline)) {
            return null;
        }
        if (!HEX_COLOR_PATTERN.matcher(inline).matches()) {
            diagnostics.add(field + " removido por não corresponder a cor hexadecimal segura.");
            return null;
        }
        return inline.toUpperCase(Locale.ROOT);
    }

    private static String normalizeTrustedUri(String value, ArrayList<String> diagnostics, String field) {
        String inline = sanitizeInline(value, 512, diagnostics, field);
        if (!hasText(inline)) {
            return null;
        }
        String lowered = inline.toLowerCase(Locale.ROOT);
        boolean allowed = TRUSTED_URI_PREFIXES.stream().anyMatch(lowered::startsWith);
        if (!allowed) {
            diagnostics.add(field + " removido por não usar origem ou esquema permitido para identidade visual.");
            return null;
        }
        return inline;
    }

    private static String normalizeToken(String value,
                                         int maxLength,
                                         ArrayList<String> diagnostics,
                                         String field,
                                         boolean upperCase) {
        String inline = sanitizeInline(value, maxLength, diagnostics, field);
        if (!hasText(inline)) {
            return null;
        }
        return upperCase ? inline.toUpperCase(Locale.ROOT) : inline;
    }

    private static Long normalizePositiveLong(Long value, ArrayList<String> diagnostics, String field) {
        if (value == null) {
            return null;
        }
        if (value <= 0L) {
            diagnostics.add(field + " removido por não ser identificador positivo.");
            return null;
        }
        return value;
    }

    private static String sanitizeInline(String value, int maxLength, ArrayList<String> diagnostics, String field) {
        String normalized = canonicalize(value, false);
        if (!hasText(normalized)) {
            return null;
        }
        String collapsed = normalized.replaceAll("\\s+", " ").trim();
        if (collapsed.length() > maxLength) {
            diagnostics.add(field + " truncado no limite de " + maxLength + " caracteres.");
            collapsed = collapsed.substring(0, maxLength).trim();
        }
        return collapsed.isEmpty() ? null : collapsed;
    }

    private static String sanitizeMultiline(String value, int maxLength, ArrayList<String> diagnostics, String field) {
        String normalized = canonicalize(value, true);
        if (!hasText(normalized)) {
            return null;
        }
        String collapsed = normalized
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("[ ]{2,}", " ")
                .trim();
        if (collapsed.length() > maxLength) {
            diagnostics.add(field + " truncado no limite de " + maxLength + " caracteres.");
            collapsed = collapsed.substring(0, maxLength).trim();
        }
        return collapsed.isEmpty() ? null : collapsed;
    }

    private static String canonicalize(String value, boolean multiline) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.isISOControl(ch) && ch != '\n' && ch != '\t') {
                continue;
            }
            if (!multiline && (ch == '\n' || ch == '\t')) {
                builder.append(' ');
                continue;
            }
            builder.append(ch);
        }
        return builder.toString();
    }

    private static String sanitizeCtxKey(String key) {
        if (key == null) {
            return null;
        }
        String normalized = canonicalize(key, false);
        if (!hasText(normalized)) {
            return null;
        }
        String compact = normalized.replace(' ', '_');
        return CTX_KEY_PATTERN.matcher(compact).matches() ? compact : null;
    }

    private static String computeFingerprint(PeticionamentoSessaoRequest request) {
        StringBuilder material = new StringBuilder(1024);
        appendFingerprint(material, request.getModo());
        appendFingerprint(material, request.getTituloCaso());
        appendFingerprint(material, request.getParteAutora());
        appendFingerprint(material, request.getParteRe());
        appendFingerprint(material, request.getRamoDireito());
        appendFingerprint(material, request.getRitoProcessual());
        appendFingerprint(material, request.getClasseProcessual());
        appendFingerprint(material, request.getTipoJustica());
        appendFingerprint(material, request.getTextoFatosResumido());
        appendFingerprint(material, request.getTextoPeticaoLivre());
        appendFingerprint(material, request.getCidadeFato());
        appendFingerprint(material, request.getUfFato());
        appendFingerprint(material, request.getCidadeProtocolo());
        appendFingerprint(material, request.getUfProtocolo());
        appendFingerprint(material, request.getNaturezaJuridica());
        appendFingerprint(material, request.getProtocolTitle());
        appendAllFingerprint(material, request.getFatos());
        appendAllFingerprint(material, request.getFundamentosJuridicos());
        appendAllFingerprint(material, request.getPedidos());
        appendAllFingerprint(material, request.getProvasIndicadas());
        appendAllFingerprint(material, request.getDocumentosAnexados());
        if (request.getCtx() != null && !request.getCtx().isEmpty()) {
            request.getCtx().forEach((key, value) -> {
                appendFingerprint(material, key);
                appendFingerprint(material, String.valueOf(value));
            });
        }
        return Hashes.sha256HexPrefix(material.toString(), 32);
    }

    private static void appendFingerprint(StringBuilder builder, String value) {
        builder.append('|');
        if (value != null) {
            builder.append(value);
        }
    }

    private static void appendAllFingerprint(StringBuilder builder, List<String> values) {
        if (values == null || values.isEmpty()) {
            builder.append("|[]");
            return;
        }
        builder.append('|');
        for (String value : values) {
            if (value != null) {
                builder.append(value).append(';');
            }
        }
    }

    private static String defaultIfBlank(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record HardenedPayload(PeticionamentoSessaoRequest request,
                                  List<String> diagnostics,
                                  Map<String, Object> metadata,
                                  String fingerprint) {
    }
}
