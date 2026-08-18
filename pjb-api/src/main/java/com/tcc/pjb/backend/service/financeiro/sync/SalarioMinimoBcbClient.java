package com.tcc.pjb.backend.service.financeiro.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class SalarioMinimoBcbClient {

    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final DateTimeFormatter DATA_BCB = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String URL_DEFAULT =
            "https://api.bcb.gov.br/dados/serie/bcdata.sgs.1619/dados/ultimos/1?formato=json";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String url;

    public SalarioMinimoBcbClient(@Qualifier("pjbSharedHttpClient") HttpClient httpClient,
                                  ObjectMapper objectMapper,
                                  @Value("${pjb.sync.salario-minimo.bcb-url:" + URL_DEFAULT + "}") String url) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.url = Objects.requireNonNull(url);
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(Objects.requireNonNull(httpClient));
        factory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @CircuitBreaker(name = "salario-minimo-bcb", fallbackMethod = "buscarUltimoValorFallback")
    public Optional<SnapshotSalarioMinimo> buscarUltimoValor() {
        String body = restClient.get().uri(url).retrieve().body(String.class);
        return parse(objectMapper, body);
    }

    Optional<SnapshotSalarioMinimo> buscarUltimoValorFallback(Throwable t) {
        log.warn("Sync salario minimo: falha ao consultar BCB em {}: {}", url, t.getMessage());
        return Optional.empty();
    }

    static Optional<SnapshotSalarioMinimo> parse(ObjectMapper objectMapper, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isArray() || root.isEmpty()) {
                return Optional.empty();
            }
            JsonNode ultimo = root.get(root.size() - 1);
            JsonNode dataNode = ultimo.path("data");
            JsonNode valorNode = ultimo.path("valor");
            if (!dataNode.isTextual() || !valorNode.isTextual()) {
                return Optional.empty();
            }
            String dataTexto = dataNode.asText().trim();
            String valorTexto = valorNode.asText().trim();
            if (dataTexto.isEmpty() || valorTexto.isEmpty()) {
                return Optional.empty();
            }
            LocalDate data = LocalDate.parse(dataTexto, DATA_BCB);
            BigDecimal valor = new BigDecimal(valorTexto);
            if (valor.signum() <= 0) {
                return Optional.empty();
            }
            return Optional.of(new SnapshotSalarioMinimo(data, valor));
        } catch (Exception e) {
            log.warn("Sync salario minimo: payload BCB rejeitado ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    public record SnapshotSalarioMinimo(LocalDate dataReferencia, BigDecimal valorMensal) {}
}
