package com.tcc.pjb.backend.ai.sync;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Profile("!test")
@ConditionalOnProperty(prefix = "pjb.sync.ibge", name = "enabled", havingValue = "true")
public class IbgeSyncService {

    private final RestTemplate restTemplate;

    public IbgeSyncService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }

    @PostConstruct
    @Transactional
    public void syncData() {
        String url = "https://servicodados.ibge.gov.br/api/v1/localidades/municipios";
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> municipios = restTemplate.getForObject(url, List.class);

            if (municipios == null || municipios.isEmpty()) {
                log.warn("IBGE sync: resposta vazia em {}", url);
                return;
            }

            int ok = 0;
            int skipped = 0;

            for (Map<String, Object> m : municipios) {
                
                Map<String, Object> microrregiao = safeMap(m.get("microrregiao"));
                Map<String, Object> mesorregiao = microrregiao != null ? safeMap(microrregiao.get("mesorregiao")) : null;
                Map<String, Object> uf = mesorregiao != null ? safeMap(mesorregiao.get("UF")) : null;

                if (uf == null) {
                    skipped++;
                    continue;
                }

                ok++;
                
                
                if (ok <= 5) {
                    log.info("IBGE sync sample: {} - {}", m.get("nome"), uf.get("sigla"));
                }
            }

            log.info("IBGE sync concluído: total={}, processados={}, ignorados={}.", municipios.size(), ok, skipped);
        } catch (RestClientException e) {
            log.error("IBGE sync falhou ao chamar {}: {}", url, e.getMessage(), e);
        } catch (Exception e) {
            log.error("IBGE sync falhou (erro inesperado): {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return null;
    }
}
