package com.tcc.pjb.backend.core.comunicacao.judicial.hsm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SefazNfeCadastroResolver {

    public record CadastroSefazNfe(
            String cnpj,
            String uf,
            String emailOperacional,
            String telefoneOperacional,
            String enderecoEstabelecimento,
            boolean consultaConfirmada,
            String fonte,
            Instant consultadoEm,
            int statusHttp,
            String hashConsulta
    ) {
        public boolean possuiEmailOperacional() {
            return emailOperacional != null && !emailOperacional.isBlank();
        }

        public boolean possuiContatoDigital() {
            return possuiEmailOperacional() || telefoneOperacional != null && !telefoneOperacional.isBlank();
        }

        public boolean possuiEnderecoFisico() {
            return enderecoEstabelecimento != null && !enderecoEstabelecimento.isBlank();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(SefazNfeCadastroResolver.class);
    private static final String RESOURCE_TYPE = "SEFAZ_NFE_CADASTRO";
    private static final String DOMAIN_CACHE = "SEFAZ_NFE_CADASTRO";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:\\+?55\\s*)?(?:\\(?\\d{2}\\)?\\s*)?9?\\d{4}[-\\s]?\\d{4}");
    private static final Pattern CEP_PATTERN = Pattern.compile("\\b\\d{5}-?\\d{3}\\b");
    private static final List<String> EMAIL_KEYS = List.of("email", "emailprincipal", "emailoperacional", "emailcontato", "destinatario");
    private static final List<String> PHONE_KEYS = List.of("telefone", "celular", "fone", "whatsapp", "telefoneprincipal");
    private static final List<String> ADDRESS_KEYS = List.of("endereco", "logradouro", "rua", "numero", "bairro", "municipio", "cidade", "uf", "cep", "complemento");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ComunicacaoJudicialStateStore stateStore;
    private final AuditLedgerService auditLedger;
    private final SefazNfeProperties properties;
    private final SefazNfeRouteMatrixService routeMatrixService;

    public SefazNfeCadastroResolver(@Qualifier("hsmInterceptacaoHttpClient") HttpClient httpClient,
                                    ObjectMapper objectMapper,
                                    ComunicacaoJudicialStateStore stateStore,
                                    AuditLedgerService auditLedger,
                                    SefazNfeProperties properties,
                                    SefazNfeRouteMatrixService routeMatrixService) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.routeMatrixService = Objects.requireNonNull(routeMatrixService, "routeMatrixService");
    }

    public Optional<CadastroSefazNfe> resolver(String cnpj, String ufHint) {
        return resolver(cnpj, ufHint, null);
    }

    public Optional<CadastroSefazNfe> resolver(String cnpj, String ufHint, String tribunalCodigo) {
        String cnpjNormalizado = digitsOnly(cnpj);
        if (cnpjNormalizado == null || cnpjNormalizado.length() != 14) {
            return Optional.empty();
        }
        String uf = upperTrim(ufHint);
        if (uf == null) {
            return Optional.empty();
        }
        String cacheKey = uf + ":" + cnpjNormalizado;
        if (properties.cacheEnabled()) {
            Optional<CadastroSefazNfe> cache = stateStore.find(DOMAIN_CACHE, cacheKey, CadastroSefazNfe.class);
            if (cache.isPresent()) {
                return cache;
            }
        }
        if (!properties.enabled()) {
            return Optional.empty();
        }
        SefazNfeRouteMatrixService.ResolvedRoute route = routeMatrixService.resolve(uf, tribunalCodigo).orElse(null);
        String endpoint = route != null ? route.endpoint() : null;
        String ufResolved = route != null && route.uf() != null ? route.uf() : uf;
        if (endpoint == null || endpoint.isBlank() || ufResolved == null) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(expandirTemplate(endpoint, cnpjNormalizado, ufResolved));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(properties.timeout())
                    .header("Accept", "application/json, text/html, application/xml, text/xml;q=0.9, */*;q=0.8")
                    .header("User-Agent", properties.userAgent())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Optional<CadastroSefazNfe> cadastroOpt = extrairCadastro(cnpjNormalizado, ufResolved, response.statusCode(), response.body());
            cadastroOpt.ifPresent(cadastro -> {
                if (properties.cacheEnabled()) {
                    stateStore.save(DOMAIN_CACHE, cacheKey, cnpjNormalizado, cadastro, null, null, null, "HTTP_" + cadastro.statusHttp());
                }
                auditLedger.appendSafely(
                        "SEFAZ_NFE_CONSULTA_OK",
                        RESOURCE_TYPE,
                        cnpjNormalizado,
                        cadastro.hashConsulta(),
                        "Cadastro operacional consultado na SEFAZ " + ufResolved + (route != null ? " via " + route.fonte() : "")
                );
            });
            return cadastroOpt;
        } catch (Exception e) {
            if (properties.strictMode()) {
                log.warn("[SEFAZ-NFE] Falha estrita na consulta uf={} cnpj={} motivo={}", ufResolved, mascararCnpj(cnpjNormalizado), e.getMessage());
            } else {
                log.debug("[SEFAZ-NFE] Consulta indisponível uf={} cnpj={} motivo={}", ufResolved, mascararCnpj(cnpjNormalizado), e.getMessage());
            }
            return Optional.empty();
        }
    }

    private Optional<CadastroSefazNfe> extrairCadastro(String cnpj, String uf, int statusHttp, String body) {
        String conteudo = body != null ? body : "";
        String email = extrairEmail(conteudo);
        String telefone = extrairTelefone(conteudo);
        String endereco = extrairEndereco(conteudo, uf);
        if ((email == null || email.isBlank()) && (telefone == null || telefone.isBlank()) && (endereco == null || endereco.isBlank())) {
            return Optional.empty();
        }
        String hash = sha256Hex(cnpj + '|' + uf + '|' + conteudo);
        return Optional.of(new CadastroSefazNfe(
                cnpj,
                uf,
                lowerTrim(email),
                digitsOnly(telefone),
                trimToNull(endereco),
                statusHttp >= 200 && statusHttp < 400,
                "SEFAZ_NFE_PUBLICA_" + uf,
                Instant.now(),
                statusHttp,
                hash
        ));
    }

    private String extrairEmail(String body) {
        JsonNode json = parseJson(body);
        String fromJson = json != null ? extrairPrimeiroValor(json, EMAIL_KEYS, this::pareceEmail) : null;
        if (fromJson != null) {
            return fromJson;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(body);
        return matcher.find() ? matcher.group() : null;
    }

    private String extrairTelefone(String body) {
        JsonNode json = parseJson(body);
        String fromJson = json != null ? extrairPrimeiroValor(json, PHONE_KEYS, this::pareceTelefone) : null;
        if (fromJson != null) {
            return fromJson;
        }
        Matcher matcher = PHONE_PATTERN.matcher(body);
        return matcher.find() ? matcher.group() : null;
    }

    private String extrairEndereco(String body, String uf) {
        JsonNode json = parseJson(body);
        if (json != null) {
            String fromJson = comporEnderecoJson(json, uf);
            if (fromJson != null) {
                return fromJson;
            }
        }
        return comporEnderecoTexto(body, uf);
    }

    private JsonNode parseJson(String body) {
        String text = trimToNull(body);
        if (text == null || (!text.startsWith("{") && !text.startsWith("["))) {
            return null;
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            return null;
        }
    }

    private String extrairPrimeiroValor(JsonNode node,
                                        List<String> aliases,
                                        java.util.function.Predicate<String> validator) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            Map<String, String> local = new LinkedHashMap<>();
            node.fieldNames().forEachRemaining(fieldName -> local.put(normalizarChave(fieldName), scalarValue(node.get(fieldName))));
            for (String alias : aliases) {
                String value = local.get(normalizarChave(alias));
                if (value != null && validator.test(value)) {
                    return value;
                }
            }
            for (JsonNode child : node) {
                String nested = extrairPrimeiroValor(child, aliases, validator);
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String nested = extrairPrimeiroValor(child, aliases, validator);
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }
        String scalar = scalarValue(node);
        return scalar != null && validator.test(scalar) ? scalar : null;
    }

    private String comporEnderecoJson(JsonNode node, String ufPadrao) {
        Map<String, String> collected = new LinkedHashMap<>();
        coletarCamposEndereco(node, collected);
        if (collected.isEmpty()) {
            return null;
        }
        List<String> partes = new ArrayList<>();
        addIfPresent(partes, firstNonBlank(collected.get("endereco"), collected.get("logradouro"), collected.get("rua")));
        addIfPresent(partes, collected.get("numero"));
        addIfPresent(partes, collected.get("complemento"));
        addIfPresent(partes, collected.get("bairro"));
        addIfPresent(partes, firstNonBlank(collected.get("municipio"), collected.get("cidade")));
        addIfPresent(partes, firstNonBlank(collected.get("uf"), ufPadrao));
        addIfPresent(partes, collected.get("cep"));
        return partes.isEmpty() ? null : String.join(", ", partes);
    }

    private void coletarCamposEndereco(JsonNode node, Map<String, String> target) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(fieldName -> {
                JsonNode child = node.get(fieldName);
                String key = normalizarChave(fieldName);
                if (ADDRESS_KEYS.contains(key)) {
                    String value = scalarValue(child);
                    if (value != null && !target.containsKey(key)) {
                        target.put(key, value);
                    }
                }
                coletarCamposEndereco(child, target);
            });
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                coletarCamposEndereco(child, target);
            }
        }
    }

    private String comporEnderecoTexto(String body, String uf) {
        String text = body.replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) {
            return null;
        }
        String email = extrairEmail(text);
        if (email != null) {
            text = text.replace(email, " ");
        }
        String telefone = extrairTelefone(text);
        if (telefone != null) {
            text = text.replace(telefone, " ");
        }
        Matcher cepMatcher = CEP_PATTERN.matcher(text);
        String cep = cepMatcher.find() ? cepMatcher.group() : null;
        Pattern enderecoPattern = Pattern.compile("(?i)(logradouro|endereco|rua|avenida|av\\.|travessa|rodovia)[:\\s-]*([^<>{}\\[\\]|]{8,180})");
        Matcher matcher = enderecoPattern.matcher(text);
        if (!matcher.find()) {
            return cep != null && uf != null ? upperTrim(uf) + ", " + cep : null;
        }
        List<String> partes = new ArrayList<>();
        addIfPresent(partes, trimToNull(matcher.group(2)));
        String ufNormalizada = upperTrim(uf);
        if (ufNormalizada != null) {
            addIfPresent(partes, ufNormalizada);
        }
        addIfPresent(partes, cep);
        return partes.isEmpty() ? null : String.join(", ", partes);
    }

    private String scalarValue(JsonNode node) {
        if (node == null || node.isNull() || node.isContainerNode()) {
            return null;
        }
        return trimToNull(node.asText());
    }

    private boolean pareceEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    private boolean pareceTelefone(String value) {
        String digits = digitsOnly(value);
        return digits != null && digits.length() >= 10;
    }

    private static String expandirTemplate(String template, String cnpj, String uf) {
        return template
                .replace("{cnpj}", cnpj)
                .replace("{uf}", uf.toLowerCase(Locale.ROOT))
                .replace("{UF}", uf.toUpperCase(Locale.ROOT));
    }

    private static String lowerTrim(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static String upperTrim(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String digitsOnly(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String digits = trimmed.replaceAll("\\D+", "");
        return digits.isBlank() ? null : digits;
    }

    private static String normalizarChave(String key) {
        return key == null ? "" : key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private static void addIfPresent(List<String> parts, String value) {
        String trimmed = trimToNull(value);
        if (trimmed != null) {
            parts.add(trimmed);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static String sha256Hex(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(raw));
        }
    }

    private static String mascararCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() < 4) {
            return "***";
        }
        return "**********" + cnpj.substring(cnpj.length() - 4);
    }
}
