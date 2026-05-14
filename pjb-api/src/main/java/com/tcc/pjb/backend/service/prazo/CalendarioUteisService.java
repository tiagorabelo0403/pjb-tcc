package com.tcc.pjb.backend.service.prazo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CalendarioUteisService {

    public record CalendarioInput(
            String comarcaId,
            String uf,
            List<LocalDate> feriadosLocais,
            List<LocalDate> feriadosNacionais,
            boolean suspensoFerias,
            LocalDate inicioFerias,
            LocalDate fimFerias
    ) {}

    private static final Set<DayOfWeek> FINS_SEMANA =
            EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    public LocalDate proximoDiaUtil(LocalDate data, CalendarioInput cal) {
        LocalDate d = data;
        while (naoEDiaUtil(d, cal)) {
            d = d.plusDays(1);
        }
        return d;
    }

    public LocalDate calcularVencimento(LocalDate inicio, int diasUteis,
            CalendarioInput cal) {
        LocalDate d = inicio;
        int contagem = 0;
        while (contagem < diasUteis) {
            d = d.plusDays(1);
            if (isDiaUtil(d, cal)) {
                contagem++;
            }
        }
        return proximoDiaUtil(d, cal);
    }

    public long diasUteisEntre(LocalDate inicio, LocalDate fim,
            CalendarioInput cal) {
        long count = 0;
        LocalDate d = inicio.plusDays(1);
        while (!d.isAfter(fim)) {
            if (isDiaUtil(d, cal)) {
                count++;
            }
            d = d.plusDays(1);
        }
        return count;
    }

    public boolean isDiaUtil(LocalDate d, CalendarioInput cal) {
        return !naoEDiaUtil(d, cal);
    }

    private boolean naoEDiaUtil(LocalDate d, CalendarioInput cal) {
        if (FINS_SEMANA.contains(d.getDayOfWeek())) return true;
        if (cal.feriadosNacionais() != null && cal.feriadosNacionais().contains(d)) return true;
        if (cal.feriadosLocais() != null && cal.feriadosLocais().contains(d)) return true;
        if (cal.suspensoFerias() && cal.inicioFerias() != null && cal.fimFerias() != null
                && !d.isBefore(cal.inicioFerias()) && !d.isAfter(cal.fimFerias())) return true;
        return false;
    }
}
