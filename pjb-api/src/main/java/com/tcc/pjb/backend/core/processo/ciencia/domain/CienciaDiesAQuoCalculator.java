package com.tcc.pjb.backend.core.processo.ciencia.domain;

import com.tcc.pjb.backend.model.entity.enums.CanalCiencia;
import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import com.tcc.pjb.backend.service.prazo.CalendarioUteisService.CalendarioInput;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class CienciaDiesAQuoCalculator {

    private static final ZoneId FUSO_BR = ZoneId.of("America/Sao_Paulo");

    private final CalendarioUteisService calendarioUteisService;
    private final CalendarioInput calendarioPadrao;

    public CienciaDiesAQuoCalculator(CalendarioUteisService calendarioUteisService) {
        this.calendarioUteisService = calendarioUteisService;
        this.calendarioPadrao = new CalendarioInput(
                null, null, List.of(), List.of(), false, null, null);
    }

    public Instant calcularDiesAQuo(CanalCiencia canal,
                                    Instant dataDisponibilizacao,
                                    Instant dataPublicacaoDje) {
        if (canal.diesAQuoNoDiaUtilSeguinte() && dataPublicacaoDje != null) {
            return proximoDiaUtil(dataPublicacaoDje);
        }
        return dataDisponibilizacao;
    }

    public Instant calcularDataExpiracao(Instant diesAQuo,
                                         int prazoRespostaDias,
                                         boolean emDiasUteis,
                                         boolean prazoEmHoras) {
        if (prazoEmHoras) {
            return diesAQuo.plus(prazoRespostaDias, ChronoUnit.HOURS);
        }
        if (emDiasUteis) {
            return adicionarDiasUteis(diesAQuo, prazoRespostaDias);
        }
        return diesAQuo.plus(prazoRespostaDias, ChronoUnit.DAYS);
    }

    public Instant proximoDiaUtil(Instant base) {
        LocalDate data = base.atZone(FUSO_BR).toLocalDate().plusDays(1);
        LocalDate util = calendarioUteisService.proximoDiaUtil(data, calendarioPadrao);
        return util.atStartOfDay(FUSO_BR).toInstant();
    }

    public Instant adicionarDiasUteis(Instant base, int dias) {
        LocalDate inicio = base.atZone(FUSO_BR).toLocalDate();
        LocalDate fim = calendarioUteisService.calcularVencimento(inicio, dias, calendarioPadrao);
        return fim.atStartOfDay(FUSO_BR).toInstant();
    }
}
