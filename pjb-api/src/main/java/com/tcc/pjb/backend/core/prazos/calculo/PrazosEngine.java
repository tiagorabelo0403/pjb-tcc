package com.tcc.pjb.backend.core.prazos.calculo;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoCalculationView;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoHealthQuery;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoHealthResult;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoRegimeView;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoWindowQuery;
import com.tcc.pjb.backend.core.prazos.calculo.domain.PrazoWindowResult;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseEntry;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import com.tcc.pjb.backend.core.prazos.calendario.domain.PrazoCalendarioHealthView;
import com.tcc.pjb.backend.core.prazos.policy.domain.PrazoPolicyView;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PrazosEngine {
    private final CalendarioForenseRepository calendarioRepository;
    private final String defaultTimezone;
    public PrazosEngine(CalendarioForenseRepository calendarioRepository, @Value("${pjb.prazos.timezone:America/Sao_Paulo}") String defaultTimezone) { this.calendarioRepository = calendarioRepository; this.defaultTimezone = defaultTimezone; }
    public LocalDateTime calcularTermino(LocalDateTime inicio, int quantidade, PrazoRegime regime, String uf, String comarca) {
        if (inicio == null) throw new IllegalArgumentException("inicio obrigatório");
        if (quantidade < 0) throw new IllegalArgumentException("quantidade inválida");
        if (regime == null) regime = PrazoRegime.UTEIS;
        ZoneId zone = safeZone(defaultTimezone);
        ZonedDateTime z = inicio.atZone(zone);
        if (quantidade == 0) return z.toLocalDateTime();
        if (regime == PrazoRegime.DOBRO_UTEIS) return calcularTermino(inicio, quantidade * 2, PrazoRegime.UTEIS, uf, comarca);
        if (regime == PrazoRegime.HORAS) return z.plusHours(quantidade).toLocalDateTime();
        if (regime == PrazoRegime.ECA) return z.plusDays(quantidade).toLocalDateTime();
        if (regime == PrazoRegime.CLT_HORAS_UTEIS) return calcularTermino(inicio, quantidade, PrazoRegime.UTEIS, uf, comarca);
        if (regime == PrazoRegime.CORRIDOS) return z.plusDays(quantidade).toLocalDateTime();
        LocalDate start = z.toLocalDate();
        LocalDate fimEstimado = start.plusDays(Math.max(10, quantidade * 3L + 30));
        List<CalendarioForenseEntry> entries = calendarioRepository.findApplicableBetween(safeUf(uf), safeComarca(comarca), start.minusDays(2), fimEstimado);
        Set<LocalDate> bloqueios = new HashSet<>();
        for (CalendarioForenseEntry e : entries) if (e.getDia() != null) bloqueios.add(e.getDia());
        int adicionados = 0; ZonedDateTime cur = z;
        while (adicionados < quantidade) { cur = cur.plusDays(1); LocalDate d = cur.toLocalDate(); if (isWeekend(d)) continue; if (bloqueios.contains(d)) continue; adicionados++; }
        return cur.toLocalDateTime();
    }
    public LocalDateTime calcularFim(LocalDateTime inicio, int quantidade, PrazoRegime regime, String uf, String comarca) { return calcularTermino(inicio, quantidade, regime, uf, comarca); }
    public LocalDateTime calcularDataFim(LocalDateTime inicio, int quantidade, PrazoRegime regime, String uf, String comarca) { return calcularTermino(inicio, quantidade, regime, uf, comarca); }
    public PrazoHealthResult health(PrazoHealthQuery query) { long total = calendarioRepository.findApplicableBetween(safeUf(query.uf()), safeComarca(query.comarca()), LocalDate.now().minusDays(30), LocalDate.now().plusDays(30)).size(); return new PrazoHealthResult(query.uf(), query.comarca(), total > 0, Instant.now()); }
    public PrazoWindowResult window(PrazoWindowQuery query) { LocalDateTime fim = calcularTermino(query.inicio(), query.quantidade(), query.regime(), query.uf(), query.comarca()); return new PrazoWindowResult(query.inicio(), fim, query.regime()); }
    public PrazoCalendarioHealthView calendarioHealthView(String uf, String comarca) { long total = calendarioRepository.findApplicableBetween(safeUf(uf), safeComarca(comarca), LocalDate.now().minusDays(365), LocalDate.now().plusDays(365)).size(); return new PrazoCalendarioHealthView(uf, comarca, total); }
    public PrazoRegimeView regimeView(PrazoRegime regime) { return new PrazoRegimeView(regime, regime == null ? null : regime.name()); }
    public PrazoCalculationView calculationView(LocalDateTime inicio, int quantidade, PrazoRegime regime, String uf, String comarca) { return new PrazoCalculationView(inicio, calcularTermino(inicio, quantidade, regime, uf, comarca), quantidade, regime); }
    public PrazoPolicyView policyView(String ramo, String rito, PrazoRegime regime) { return new PrazoPolicyView(ramo, rito, regime); }
    private static boolean isWeekend(LocalDate d) { DayOfWeek dow = d.getDayOfWeek(); return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY; }
    private static ZoneId safeZone(String zone) { try { return ZoneId.of(zone); } catch (Exception e) { return ZoneId.of("America/Sao_Paulo"); } }
    private static String safeUf(String uf) { if (uf == null) return null; String v = uf.trim().toUpperCase(); return v.isBlank() ? null : v; }
    private static String safeComarca(String comarca) { if (comarca == null) return null; String v = comarca.trim(); return v.isBlank() ? null : v; }
}
