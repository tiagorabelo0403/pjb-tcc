package com.tcc.pjb.backend.service.secretariat.aggregation;

import java.time.Duration;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SecretariatAgrupadorQueryService {

    public record ProcessoAgrupadorInput(
            UUID processoId,
            SecretariatAgrupadorRules.AgrupadorInput sinais
    ) {}

    public record AgrupadorSnapshot(
            Map<String, Long> contagens,
            Map<String, String> rotulos,
            int totalUrgentes
    ) {}

    private final SecretariatAgrupadorRules rules;

    public SecretariatAgrupadorQueryService(SecretariatAgrupadorRules rules) {
        this.rules = rules;
    }

    public AgrupadorSnapshot construirSnapshot(List<ProcessoAgrupadorInput> processos) {
        Map<SecretariatAgrupadorTipo, Long> acumulado = new java.util.EnumMap<>(SecretariatAgrupadorTipo.class);
        for (SecretariatAgrupadorTipo tipo : SecretariatAgrupadorTipo.values()) {
            acumulado.put(tipo, 0L);
        }
        for (ProcessoAgrupadorInput item : processos) {
            Set<SecretariatAgrupadorTipo> tipos = rules.avaliar(item.sinais());
            for (SecretariatAgrupadorTipo tipo : tipos) {
                acumulado.merge(tipo, 1L, Long::sum);
            }
        }
        Map<String, Long> contagens = new LinkedHashMap<>();
        Map<String, String> rotulos = new LinkedHashMap<>();
        int totalUrgentes = 0;
        for (SecretariatAgrupadorTipo tipo : SecretariatAgrupadorTipo.values()) {
            long count = acumulado.get(tipo);
            if (count > 0) {
                contagens.put(tipo.name(), count);
                rotulos.put(tipo.name(), tipo.rotulo());
                if (tipo.eUrgente()) totalUrgentes += count;
            }
        }
        return new AgrupadorSnapshot(contagens, rotulos, totalUrgentes);
    }

    public SecretariatAgrupadorRules.AgrupadorInput inputVazio() {
        return new SecretariatAgrupadorRules.AgrupadorInput(
                false, false, false, false, false, false, false,
                false, false, false, Duration.ZERO, null, false);
    }
}
