package com.tcc.pjb.backend.service.financeiro.sync;

import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.time.LocalDate;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("!test")
public class SalarioMinimoStalenessWatchdogService {

    private final SalarioMinimoNacionalService salarioMinimoNacionalService;
    private final int limiarAnos;

    public SalarioMinimoStalenessWatchdogService(SalarioMinimoNacionalService salarioMinimoNacionalService,
                                                 @Value("${pjb.observability.salario-minimo.staleness-limiar-anos:1}") int limiarAnos) {
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
        this.limiarAnos = limiarAnos;
    }

    @Scheduled(cron = "${pjb.observability.salario-minimo.watchdog-cron:0 0 4 * * *}")
    public boolean verificarDefasagem() {
        int anoAtual = LocalDate.now().getYear();
        int anoConhecido = salarioMinimoNacionalService.anoMaisRecenteConhecido();
        int defasagemAnos = anoAtual - anoConhecido;
        boolean defasado = defasagemAnos > limiarAnos;
        if (defasado) {
            log.warn("Salario minimo desatualizado: ano_conhecido={} ano_atual={} defasagem_anos={} limiar_anos={}",
                    anoConhecido, anoAtual, defasagemAnos, limiarAnos);
        }
        return defasado;
    }
}
