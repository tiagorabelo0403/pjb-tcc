package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PrazoFatalCalculator {

    private static final ZoneId FUSO_BR = ZoneId.of("America/Sao_Paulo");

    private final CalendarioForenseTribunalService calendarioForenseTribunalService;

    public PrazoFatalCalculator(CalendarioForenseTribunalService calendarioForenseTribunalService) {
        this.calendarioForenseTribunalService = Objects.requireNonNull(calendarioForenseTribunalService);
    }

    public Instant calcular(SecretariaInstitucionalItem item, Processo processo, Instant marco) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(processo, "processo");
        Objects.requireNonNull(marco, "marco");
        int diasUteis = item.getPrazoBaseDias() * (item.isPrazoEmDobro() ? 2 : 1);
        LocalDate dataEvento = marco.atZone(FUSO_BR).toLocalDate();
        CalendarioForenseTribunalService.PrazoCalculado prazo = calendarioForenseTribunalService.calcularPrazo(
                dataEvento, diasUteis, processo.getTribunal(), processo.getUf(), processo.getComarca());
        return prazo.dataVencimento().atTime(LocalTime.MAX).atZone(FUSO_BR).toInstant();
    }
}
