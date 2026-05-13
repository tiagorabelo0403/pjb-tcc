package com.tcc.pjb.backend.service.processual.peticionamento;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoEnderecoRequest;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoEnderecoResponse;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class PeticionamentoEnderecoAutomationService {

    private final boolean enabled;
    private final RestClient restClient;
    private final Cache<String, PeticionamentoEnderecoResponse> cache = Caffeine.newBuilder()
            .maximumSize(20000)
            .expireAfterAccess(Duration.ofHours(12))
            .build();

    public PeticionamentoEnderecoAutomationService(@Value("${pjb.peticionamento.cep.enabled:true}") boolean enabled,
                                                   @Value("${pjb.peticionamento.cep.base-url:https://viacep.com.br/ws}") String baseUrl,
                                                   @Value("${pjb.peticionamento.cep.read-timeout:3s}") Duration readTimeout,
                                                   @Qualifier("pjbSharedHttpClient") HttpClient httpClient) {
        this.enabled = enabled;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(Objects.requireNonNull(httpClient, "httpClient"));
        requestFactory.setReadTimeout(readTimeout == null ? Duration.ofSeconds(3) : readTimeout);
        this.restClient = RestClient.builder()
                .baseUrl(normalizeBaseUrl(baseUrl))
                .requestFactory(requestFactory)
                .build();
    }

    public PeticionamentoEnderecoResponse resolve(PeticionamentoEnderecoRequest request, boolean autoResolve) {
        PeticionamentoEnderecoRequest safe = request == null ? new PeticionamentoEnderecoRequest() : request;
        String cep = digitsOnly(safe.getCep());
        if (!autoResolve) {
            return manual(safe, "manual");
        }
        if (cep == null) {
            return manual(safe, "manual_sem_cep");
        }
        if (cep.length() != 8) {
            PeticionamentoEnderecoResponse manual = manual(safe, "cep_invalido");
            manual.getAvisos().add("CEP informado sem 8 dígitos; manter preenchimento manual guiado.");
            return manual;
        }
        if (!enabled) {
            PeticionamentoEnderecoResponse manual = manual(safe, "cep_lookup_desabilitado");
            manual.getAvisos().add("Autopreenchimento por CEP desabilitado na configuração do ambiente.");
            return manual;
        }
        PeticionamentoEnderecoResponse cached = cache.getIfPresent(cep);
        if (cached != null) {
            return merge(cached, safe, true, "viacep_cache");
        }
        try {
            ViaCepPayload payload = restClient.get()
                    .uri(uriBuilder -> uriBuilder.pathSegment(cep, "json").build())
                    .retrieve()
                    .body(ViaCepPayload.class);
            if (payload == null || Boolean.TRUE.equals(payload.erro)) {
                PeticionamentoEnderecoResponse manual = manual(safe, "cep_nao_encontrado");
                manual.getAvisos().add("CEP não encontrado na malha de autopreenchimento.");
                return manual;
            }
            PeticionamentoEnderecoResponse resolved = PeticionamentoEnderecoResponse.builder()
                    .cep(defaultText(digitsOnly(payload.cep), cep))
                    .logradouro(trimToNull(payload.logradouro))
                    .numero(null)
                    .complemento(trimToNull(payload.complemento))
                    .bairro(trimToNull(payload.bairro))
                    .cidade(trimToNull(payload.localidade))
                    .uf(normalizeUf(payload.uf))
                    .referencia(null)
                    .autoPreenchido(true)
                    .valido(true)
                    .origem("viacep")
                    .avisos(new ArrayList<>())
                    .build();
            PeticionamentoEnderecoResponse immutableResolved = immutableResponse(resolved);
            cache.put(cep, immutableResolved);
            return merge(immutableResolved, safe, true, "viacep");
        } catch (RestClientException ex) {
            PeticionamentoEnderecoResponse manual = manual(safe, "cep_lookup_erro");
            manual.getAvisos().add("Falha transitória no autopreenchimento por CEP; seguir com preenchimento manual guiado.");
            return manual;
        }
    }

    private PeticionamentoEnderecoResponse merge(PeticionamentoEnderecoResponse base,
                                                 PeticionamentoEnderecoRequest input,
                                                 boolean autoPreenchido,
                                                 String origem) {
        ArrayList<String> avisos = new ArrayList<>();
        if (base.getAvisos() != null && !base.getAvisos().isEmpty()) {
            avisos.addAll(base.getAvisos());
        }
        return PeticionamentoEnderecoResponse.builder()
                .cep(defaultText(digitsOnly(input.getCep()), base.getCep()))
                .logradouro(defaultText(input.getLogradouro(), base.getLogradouro()))
                .numero(trimToNull(input.getNumero()))
                .complemento(defaultText(input.getComplemento(), base.getComplemento()))
                .bairro(defaultText(input.getBairro(), base.getBairro()))
                .cidade(defaultText(input.getCidade(), base.getCidade()))
                .uf(defaultText(normalizeUf(input.getUf()), base.getUf()))
                .referencia(trimToNull(input.getReferencia()))
                .autoPreenchido(autoPreenchido)
                .valido(isEnderecoSuficiente(base, input))
                .origem(origem)
                .avisos(avisos)
                .build();
    }

    private PeticionamentoEnderecoResponse manual(PeticionamentoEnderecoRequest safe, String origem) {
        return PeticionamentoEnderecoResponse.builder()
                .cep(digitsOnly(safe.getCep()))
                .logradouro(trimToNull(safe.getLogradouro()))
                .numero(trimToNull(safe.getNumero()))
                .complemento(trimToNull(safe.getComplemento()))
                .bairro(trimToNull(safe.getBairro()))
                .cidade(trimToNull(safe.getCidade()))
                .uf(normalizeUf(safe.getUf()))
                .referencia(trimToNull(safe.getReferencia()))
                .autoPreenchido(false)
                .valido(hasText(safe.getLogradouro()) || hasText(safe.getCidade()) || hasText(safe.getUf()) || hasText(digitsOnly(safe.getCep())))
                .origem(origem)
                .avisos(new ArrayList<>())
                .build();
    }

    private static boolean isEnderecoSuficiente(PeticionamentoEnderecoResponse base, PeticionamentoEnderecoRequest input) {
        return hasText(base == null ? null : base.getLogradouro())
                || hasText(input == null ? null : input.getLogradouro())
                || hasText(base == null ? null : base.getCidade())
                || hasText(input == null ? null : input.getCidade())
                || hasText(base == null ? null : base.getUf())
                || hasText(input == null ? null : input.getUf());
    }

    private static PeticionamentoEnderecoResponse immutableResponse(PeticionamentoEnderecoResponse source) {
        if (source == null) {
            return null;
        }
        return PeticionamentoEnderecoResponse.builder()
                .cep(trimToNull(source.getCep()))
                .logradouro(trimToNull(source.getLogradouro()))
                .numero(trimToNull(source.getNumero()))
                .complemento(trimToNull(source.getComplemento()))
                .bairro(trimToNull(source.getBairro()))
                .cidade(trimToNull(source.getCidade()))
                .uf(normalizeUf(source.getUf()))
                .referencia(trimToNull(source.getReferencia()))
                .autoPreenchido(source.isAutoPreenchido())
                .valido(source.isValido())
                .origem(trimToNull(source.getOrigem()))
                .avisos(source.getAvisos() == null ? List.of() : List.copyOf(source.getAvisos()))
                .build();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String normalized = trimToNull(baseUrl);
        if (normalized == null) {
            return "https://viacep.com.br/ws";
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isEmpty() ? "https://viacep.com.br/ws" : normalized;
    }

    private String digitsOnly(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String defaultText(String preferred, String fallback) {
        String first = trimToNull(preferred);
        return first != null ? first : trimToNull(fallback);
    }

    private static String normalizeUf(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return trimToNull(value) != null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ViaCepPayload {
        public String cep;
        public String logradouro;
        public String complemento;
        public String bairro;
        public String localidade;
        public String uf;
        public Boolean erro;
    }
}
