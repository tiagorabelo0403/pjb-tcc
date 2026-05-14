package com.tcc.pjb.backend.service.processual.celerity;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CeleridadeRitoPolicyRegistry {

    public record RitoSla(
            Duration slaNormal,
            Duration slaAtencao,
            Duration slaUrgente,
            boolean audienciaUnaPreferencial,
            boolean reuPresoPrioridade
    ) {}

    private static final Map<RitoProcessual, RitoSla> SLAS = buildSlas();

    public Optional<RitoSla> buscarSla(RitoProcessual rito) {
        return Optional.ofNullable(SLAS.get(rito));
    }

    public Duration slaDaFase(RitoProcessual rito, FaseProcessual fase) {
        return buscarSla(rito)
                .map(sla -> switch (fase) {
                    case CITACAO -> sla.slaNormal().dividedBy(4);
                    case SANEAMENTO -> sla.slaNormal().dividedBy(3);
                    case JULGAMENTO -> sla.slaNormal().dividedBy(2);
                    default -> sla.slaNormal();
                })
                .orElse(Duration.ofDays(90));
    }

    public CeleridadeNivel avaliarNivel(RitoProcessual rito, Duration tempoParado) {
        RitoSla sla = SLAS.getOrDefault(rito, slaDefault());
        if (tempoParado.compareTo(sla.slaUrgente()) > 0) return CeleridadeNivel.CRITICO;
        if (tempoParado.compareTo(sla.slaAtencao()) > 0) return CeleridadeNivel.URGENTE;
        if (tempoParado.compareTo(sla.slaNormal().multipliedBy(80).dividedBy(100)) > 0)
            return CeleridadeNivel.PRIORITARIO;
        if (tempoParado.compareTo(sla.slaNormal().dividedBy(2)) > 0)
            return CeleridadeNivel.ATENCAO;
        return CeleridadeNivel.NORMAL;
    }

    private static RitoSla slaDefault() {
        return new RitoSla(Duration.ofDays(180), Duration.ofDays(270), Duration.ofDays(365), false, false);
    }

    private static Map<RitoProcessual, RitoSla> buildSlas() {
        return Map.ofEntries(
            Map.entry(RitoProcessual.JUIZADO_ESPECIAL_CIVEL,
                new RitoSla(Duration.ofDays(90), Duration.ofDays(120), Duration.ofDays(180), true, false)),
            Map.entry(RitoProcessual.PROCEDIMENTO_PENAL_SUMARISSIMO,
                new RitoSla(Duration.ofDays(60), Duration.ofDays(90), Duration.ofDays(120), true, false)),
            Map.entry(RitoProcessual.TRABALHISTA_SUMARISSIMO,
                new RitoSla(Duration.ofDays(45), Duration.ofDays(60), Duration.ofDays(90), true, false)),
            Map.entry(RitoProcessual.TRABALHISTA_ORDINARIO,
                new RitoSla(Duration.ofDays(180), Duration.ofDays(270), Duration.ofDays(365), false, false)),
            Map.entry(RitoProcessual.PROCEDIMENTO_PENAL_COMUM,
                new RitoSla(Duration.ofDays(120), Duration.ofDays(180), Duration.ofDays(240), false, true)),
            Map.entry(RitoProcessual.EXECUCAO_FISCAL,
                new RitoSla(Duration.ofDays(360), Duration.ofDays(540), Duration.ofDays(720), false, false)),
            Map.entry(RitoProcessual.COMUM_ORDINARIO,
                new RitoSla(Duration.ofDays(365), Duration.ofDays(547), Duration.ofDays(730), false, false)),
            Map.entry(RitoProcessual.ESPECIAL_MANDADO_SEGURANCA,
                new RitoSla(Duration.ofDays(30), Duration.ofDays(45), Duration.ofDays(60), false, false)),
            Map.entry(RitoProcessual.CIVIL_FAMILIA_ALIMENTOS,
                new RitoSla(Duration.ofDays(60), Duration.ofDays(90), Duration.ofDays(120), true, false))
        );
    }
}
