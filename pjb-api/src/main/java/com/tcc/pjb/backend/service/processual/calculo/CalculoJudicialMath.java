package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoIndiceMensalRequest;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CalculoJudicialMath {

    private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private CalculoJudicialMath() {
    }

    public static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static BigDecimal positive(BigDecimal value) {
        return nvl(value).signum() < 0 ? BigDecimal.ZERO : nvl(value);
    }

    public static BigDecimal percent(BigDecimal base, BigDecimal percentual) {
        return money(positive(base).multiply(positive(percentual), MC));
    }

    public static BigDecimal fraction(BigDecimal base, BigDecimal numerador, BigDecimal denominador) {
        if (denominador == null || denominador.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return money(positive(base).multiply(positive(numerador), MC).divide(positive(denominador), 10, RoundingMode.HALF_UP));
    }

    public static BigDecimal money(BigDecimal value) {
        return nvl(value).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return positive(numerator).divide(positive(denominator), 6, RoundingMode.HALF_UP);
    }

    public static long diasAtraso(LocalDate vencimento, LocalDate dataCalculo) {
        if (vencimento == null || dataCalculo == null || !dataCalculo.isAfter(vencimento)) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(vencimento, dataCalculo);
    }

    public static int avosTrabalhistas(LocalDate admissao, LocalDate demissao) {
        if (admissao == null || demissao == null || demissao.isBefore(admissao)) {
            return 0;
        }
        YearMonth current = YearMonth.from(admissao);
        YearMonth end = YearMonth.from(demissao);
        int avos = 0;
        while (!current.isAfter(end)) {
            LocalDate monthStart = current.atDay(1);
            LocalDate monthEnd = current.atEndOfMonth();
            LocalDate workedStart = admissao.isAfter(monthStart) ? admissao : monthStart;
            LocalDate workedEnd = demissao.isBefore(monthEnd) ? demissao : monthEnd;
            long workedDays = ChronoUnit.DAYS.between(workedStart, workedEnd.plusDays(1));
            if (workedDays >= 15) {
                avos++;
            }
            current = current.plusMonths(1);
        }
        return Math.max(0, Math.min(12, avos));
    }

    public static int anosCompletos(LocalDate admissao, LocalDate demissao) {
        if (admissao == null || demissao == null || demissao.isBefore(admissao)) {
            return 0;
        }
        return (int) Math.max(0L, ChronoUnit.YEARS.between(admissao, demissao));
    }

    public static BigDecimal fatorAcumuladoMensal(List<CalculoIndiceMensalRequest> taxasMensais) {
        if (taxasMensais == null || taxasMensais.isEmpty()) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        BigDecimal fator = BigDecimal.ONE;
        List<CalculoIndiceMensalRequest> ordered = taxasMensais.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.competencia() != null && item.taxaPercentualMensal() != null)
                .sorted(Comparator.comparing(item -> YearMonth.parse(item.competencia())))
                .toList();
        for (CalculoIndiceMensalRequest item : ordered) {
            BigDecimal taxa = positive(item.taxaPercentualMensal()).divide(HUNDRED, 10, RoundingMode.HALF_UP);
            fator = fator.multiply(BigDecimal.ONE.add(taxa), MC);
        }
        return fator.subtract(BigDecimal.ONE).setScale(6, RoundingMode.HALF_UP);
    }

    public static String normalizeCode(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
