package com.tcc.pjb.backend.service.financeiro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VerbaRescisorialService {

    public PlanilhaRescisoria calcular(VerbaRescisorialRequest request) {
        BigDecimal salario = positive(request.salarioBase());
        long meses = Math.max(0L, ChronoUnit.MONTHS.between(request.admissao(), request.demissao()));
        BigDecimal horasExtrasBase = positive(request.valorHoraExtraBase());
        LinkedList<VerbaCalculada> verbas = new LinkedList<>();

        verbas.add(calcularSaldoSalario(salario, request.diasTrabalhadosNoMes()));
        verbas.add(calcularDecimoTerceiro(salario, meses));
        BigDecimal ferias = calcularFeriasProporcionaisValor(salario, meses);
        verbas.add(new VerbaCalculada("Ferias proporcionais", salario, percentual(meses, 12), ferias, meses + "/12 x salario"));
        verbas.add(new VerbaCalculada("Terco constitucional", ferias, new BigDecimal("0.333333"), ferias.divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP), "ferias / 3"));
        verbas.add(calcularFgts(salario, meses));
        verbas.add(calcularAvisoPrevio(salario, request.tipoDispensa(), meses));
        if (horasExtrasBase.signum() > 0 && request.quantidadeHorasExtras() > 0) {
            verbas.add(calcularHorasExtras(horasExtrasBase, request.quantidadeHorasExtras(), request.percentualHoraExtra()));
            verbas.add(calcularDsr(verbas.getLast().valor()));
        }
        if (request.grauInsalubridade() != null && !request.grauInsalubridade().isBlank()) {
            verbas.addFirst(calcularInsalubridade(salario, request.grauInsalubridade()));
        }
        BigDecimal total = verbas.stream().map(VerbaCalculada::valor).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        return new PlanilhaRescisoria(List.copyOf(verbas), total, LocalDate.now());
    }

    private VerbaCalculada calcularSaldoSalario(BigDecimal salario, int diasTrabalhados) {
        BigDecimal valor = salario.multiply(new BigDecimal(Math.max(0, Math.min(30, diasTrabalhados)))).divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
        return new VerbaCalculada("Saldo de salario", salario, percentual(Math.max(0, Math.min(30, diasTrabalhados)), 30), valor, diasTrabalhados + "/30 x salario");
    }

    private VerbaCalculada calcularDecimoTerceiro(BigDecimal salario, long meses) {
        BigDecimal valor = salario.multiply(new BigDecimal(Math.max(0L, Math.min(12L, meses)))).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        return new VerbaCalculada("13o proporcional", salario, percentual(meses, 12), valor, meses + "/12 x salario");
    }

    private BigDecimal calcularFeriasProporcionaisValor(BigDecimal salario, long meses) {
        return salario.multiply(new BigDecimal(Math.max(0L, Math.min(12L, meses)))).divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
    }

    private VerbaCalculada calcularFgts(BigDecimal salario, long meses) {
        BigDecimal base = salario.multiply(new BigDecimal(Math.max(1L, meses))).setScale(2, RoundingMode.HALF_UP);
        BigDecimal valor = base.multiply(new BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP);
        return new VerbaCalculada("FGTS", base, new BigDecimal("0.08"), valor, "base x 8% ");
    }

    private VerbaCalculada calcularAvisoPrevio(BigDecimal salario, String tipoDispensa, long meses) {
        BigDecimal fator = "JUSTA_CAUSA".equalsIgnoreCase(tipoDispensa) ? BigDecimal.ZERO : BigDecimal.ONE;
        long anos = Math.max(0L, meses / 12L);
        BigDecimal adicional = new BigDecimal(Math.min(60L, anos * 3L)).divide(new BigDecimal("30"), 4, RoundingMode.HALF_UP);
        BigDecimal percentual = fator.add(adicional);
        BigDecimal valor = salario.multiply(percentual).setScale(2, RoundingMode.HALF_UP);
        return new VerbaCalculada("Aviso previo", salario, percentual, valor, "salario x fator legal do aviso");
    }

    private VerbaCalculada calcularHorasExtras(BigDecimal baseHora, int horas, BigDecimal percentual) {
        BigDecimal adicional = percentual == null ? new BigDecimal("0.50") : percentual;
        BigDecimal valor = baseHora.multiply(BigDecimal.valueOf(horas)).multiply(BigDecimal.ONE.add(adicional)).setScale(2, RoundingMode.HALF_UP);
        return new VerbaCalculada("Horas extras", baseHora, adicional, valor, horas + " horas x valor-hora x (1 + adicional)");
    }

    private VerbaCalculada calcularDsr(BigDecimal horasExtras) {
        BigDecimal valor = horasExtras.multiply(new BigDecimal("0.166666")).setScale(2, RoundingMode.HALF_UP);
        return new VerbaCalculada("DSR sobre horas extras", horasExtras, new BigDecimal("0.166666"), valor, "horas extras / 6");
    }

    private VerbaCalculada calcularInsalubridade(BigDecimal salario, String grau) {
        BigDecimal percentual = switch (grau == null ? "" : grau.trim().toUpperCase()) {
            case "MAXIMO" -> new BigDecimal("0.40");
            case "MEDIO" -> new BigDecimal("0.20");
            default -> new BigDecimal("0.10");
        };
        BigDecimal valor = salario.multiply(percentual).setScale(2, RoundingMode.HALF_UP);
        return new VerbaCalculada("Insalubridade", salario, percentual, valor, "salario x percentual NR-15");
    }

    private BigDecimal positive(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private BigDecimal percentual(long numerador, long denominador) {
        if (denominador <= 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(Math.max(0L, numerador)).divide(new BigDecimal(denominador), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal percentual(int numerador, int denominador) {
        if (denominador <= 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(Math.max(0, numerador)).divide(new BigDecimal(denominador), 6, RoundingMode.HALF_UP);
    }

    public record VerbaRescisorialRequest(
            BigDecimal salarioBase,
            LocalDate admissao,
            LocalDate demissao,
            int diasTrabalhadosNoMes,
            String tipoDispensa,
            BigDecimal valorHoraExtraBase,
            int quantidadeHorasExtras,
            BigDecimal percentualHoraExtra,
            String grauInsalubridade
    ) {
    }

    public record VerbaCalculada(
            String nome,
            BigDecimal base,
            BigDecimal percentual,
            BigDecimal valor,
            String formula
    ) {
    }

    public record PlanilhaRescisoria(
            List<VerbaCalculada> verbas,
            BigDecimal total,
            LocalDate calculadoEm
    ) {
    }
}
