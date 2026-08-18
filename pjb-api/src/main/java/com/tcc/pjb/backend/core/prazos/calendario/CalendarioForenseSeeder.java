package com.tcc.pjb.backend.core.prazos.calendario;

import java.time.LocalDate;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;

@Component
public class CalendarioForenseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CalendarioForenseSeeder.class);

    private final CalendarioForenseRepository repo;
    private final CalendarioForenseTribunalService calendarioForenseTribunalService;
    private final NationalPrazoEngine prazoEngine;
    private final boolean enabled;

    public CalendarioForenseSeeder(CalendarioForenseRepository repo,
                                   CalendarioForenseTribunalService calendarioForenseTribunalService,
                                   NationalPrazoEngine prazoEngine,
                                   @Value("${pjb.prazos.seed.enabled:true}") boolean enabled) {
        this.repo = repo;
        this.calendarioForenseTribunalService = calendarioForenseTribunalService;
        this.prazoEngine = prazoEngine;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        try {
            seedYear(Year.now().getValue());
            seedYear(Year.now().plusYears(1).getValue());
        } catch (Exception e) {
            log.warn("CalendarioForenseSeeder falhou (não bloqueante): {}", e.getMessage());
        }
    }

    @Transactional
    public void seedYear(int year) {
        int inserted = 0;
        for (Map.Entry<LocalDate, String> it : feriadosNacionais(year).entrySet()) {
            inserted += persistir(null, null, it.getKey(), "FERIADO", it.getValue()) ? 1 : 0;
        }

        List<CalendarioForenseTribunalService.EntradaCalendario> exemplos = calendarioForenseTribunalService.construirFeriadosEstaduaisExemplo(year);
        for (CalendarioForenseTribunalService.EntradaCalendario entrada : exemplos) {
            inserted += persistir(entrada.uf(), entrada.comarca(), entrada.data(), entrada.tipo().name(), entrada.descricao()) ? 1 : 0;
            prazoEngine.adicionarFeriadoTribunal(entrada.tribunalCodigo(), year, entrada.data());
        }

        if (inserted > 0) {
            log.info("Calendario forense: {} entradas inseridas para {}", inserted, year);
        }
    }

    private boolean persistir(String uf, String comarca, LocalDate dia, String tipo, String descricao) {
        if (repo.existsByUfAndComarcaAndDiaAndTipo(uf, comarca, dia, tipo)) {
            return false;
        }
        CalendarioForenseEntry entry = new CalendarioForenseEntry();
        entry.setUf(uf);
        entry.setComarca(comarca);
        entry.setDia(dia);
        entry.setTipo(tipo);
        entry.setDescricao(descricao);
        repo.save(entry);
        return true;
    }

    
    static Map<LocalDate, String> feriadosNacionais(int year) {
        Map<LocalDate, String> m = new LinkedHashMap<>();
        
        m.put(LocalDate.of(year, 1, 1), "Confraternização Universal");
        m.put(LocalDate.of(year, 4, 21), "Tiradentes");
        m.put(LocalDate.of(year, 5, 1), "Dia do Trabalho");
        m.put(LocalDate.of(year, 9, 7), "Independência do Brasil");
        m.put(LocalDate.of(year, 10, 12), "Nossa Senhora Aparecida");
        m.put(LocalDate.of(year, 11, 2), "Finados");
        m.put(LocalDate.of(year, 11, 15), "Proclamação da República");
        m.put(LocalDate.of(year, 11, 20), "Dia da Consciência Negra");
        m.put(LocalDate.of(year, 12, 25), "Natal");

        LocalDate pascoa = easterSunday(year);
        m.put(pascoa.minusDays(48), "Carnaval (segunda-feira)");
        m.put(pascoa.minusDays(47), "Carnaval (terça-feira)");
        m.put(pascoa.minusDays(2), "Sexta-feira Santa");
        m.put(pascoa.plusDays(60), "Corpus Christi");
        return m;
    }

    
    static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31; 
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
