package com.tcc.pjb.backend.service.financeiro.sync;

import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("!test")
@ConditionalOnProperty(name = "pjb.sync.salario-minimo.enabled", havingValue = "true")
public class SalarioMinimoNacionalSyncScheduler {

    private static final String NORMA_REFERENCIA = "Serie 1619 do Banco Central do Brasil";
    private static final String FONTE_OFICIAL = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.1619";

    private final SalarioMinimoBcbClient bcbClient;
    private final SalarioMinimoNacionalService salarioMinimoNacionalService;

    public SalarioMinimoNacionalSyncScheduler(SalarioMinimoBcbClient bcbClient,
                                              SalarioMinimoNacionalService salarioMinimoNacionalService) {
        this.bcbClient = Objects.requireNonNull(bcbClient);
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
    }

    @Scheduled(cron = "${pjb.sync.salario-minimo.cron:0 0 3 * * *}")
    public void sincronizar() {
        try {
            Optional<SalarioMinimoBcbClient.SnapshotSalarioMinimo> snapshot = bcbClient.buscarUltimoValor();
            if (snapshot.isEmpty()) {
                log.warn("Sync salario minimo: BCB indisponivel ou payload rejeitado; mantendo valor persistido");
                return;
            }
            LocalDate dataReferencia = snapshot.get().dataReferencia();
            BigDecimal valorRecebido = snapshot.get().valorMensal();
            int ano = dataReferencia.getYear();
            BigDecimal valorAtual = salarioMinimoNacionalService.valorPorAno(ano);
            if (valorAtual.compareTo(valorRecebido) == 0) {
                log.info("Sync salario minimo: ano {} ja atualizado com R$ {}", ano, valorRecebido);
                return;
            }
            salarioMinimoNacionalService.salvarOuAtualizar(ano, valorRecebido, NORMA_REFERENCIA, FONTE_OFICIAL);
            log.info("Sync salario minimo: ano {} atualizado de R$ {} para R$ {} (vigencia BCB: {})",
                    ano, valorAtual, valorRecebido, dataReferencia);
        } catch (Exception e) {
            log.error("Sync salario minimo: falha inesperada; proxima tentativa no proximo agendamento", e);
        }
    }
}
