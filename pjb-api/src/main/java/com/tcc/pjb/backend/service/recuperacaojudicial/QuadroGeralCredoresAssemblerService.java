package com.tcc.pjb.backend.service.recuperacaojudicial;

import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class QuadroGeralCredoresAssemblerService {

    private final SalarioMinimoNacionalService salarioMinimoNacionalService;

    public QuadroGeralCredoresAssemblerService(SalarioMinimoNacionalService salarioMinimoNacionalService) {
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
    }

    public enum ClasseCredor {
        CLASSE_I_TRABALHISTA,
        CLASSE_II_GARANTIA_REAL,
        CLASSE_III_QUIROGRAFARIO,
        CLASSE_IV_MULTAS_PENAS_PECUNIARIAS,
        SUBQUIROGRAFARIO
    }

    public record Credor(
            String nome,
            String documento,
            BigDecimal valorCredito,
            ClasseCredor classe,
            String origem,
            boolean habilitado
    ) {}

    public record QuadroGeralResult(
            List<Credor> credoresOrdenados,
            BigDecimal totalClasseI,
            BigDecimal totalClasseII,
            BigDecimal totalClasseIII,
            BigDecimal totalClasseIV,
            BigDecimal totalGeral,
            List<String> observacoes
    ) {}

    private static final BigDecimal LIMITE_TRABALHISTA_SM = new BigDecimal("150");

    /**
     * O limite de 150 salários mínimos por credor trabalhista depende de qual salário mínimo rege o
     * caso, e não do salário mínimo de hoje. A data de referência vem do domínio — {@code dataPedido}
     * na mesma forma que {@link FalenciaDecretacaoService} já usa para o limiar de impontualidade.
     */
    public QuadroGeralResult montar(List<Credor> credores, LocalDate dataPedido) {
        Objects.requireNonNull(dataPedido, "dataPedido");
        List<String> obs = new ArrayList<>();
        BigDecimal limiteTrabalhistaValor =
                salarioMinimoNacionalService.multiplicar(LIMITE_TRABALHISTA_SM, dataPedido);

        List<Credor> habilitados = credores.stream()
                .filter(Credor::habilitado)
                .sorted(Comparator.comparing(Credor::classe).thenComparing(Credor::nome))
                .toList();

        BigDecimal totalI = somar(habilitados, ClasseCredor.CLASSE_I_TRABALHISTA);
        BigDecimal totalII = somar(habilitados, ClasseCredor.CLASSE_II_GARANTIA_REAL);
        BigDecimal totalIII = somar(habilitados, ClasseCredor.CLASSE_III_QUIROGRAFARIO);
        BigDecimal totalIV = somar(habilitados, ClasseCredor.CLASSE_IV_MULTAS_PENAS_PECUNIARIAS);
        BigDecimal total = totalI.add(totalII).add(totalIII).add(totalIV);

        obs.add(String.format(
                "Classe I (trabalhistas até %d SM = R$ %.2f): R$ %.2f",
                LIMITE_TRABALHISTA_SM.intValue(), limiteTrabalhistaValor, totalI));
        obs.add(String.format("Classe II (garantia real): R$ %.2f", totalII));
        obs.add(String.format("Classe III (quirografários): R$ %.2f", totalIII));
        obs.add(String.format("Classe IV (multas/penas pecuniárias — últimos a receber): R$ %.2f", totalIV));
        obs.add(String.format("Total geral: R$ %.2f — credores habilitados: %d", total, habilitados.size()));
        obs.add("Fundamento: Lei 11.101/2005 arts. 83 e 149.");

        return new QuadroGeralResult(habilitados, totalI, totalII, totalIII, totalIV, total, obs);
    }

    private BigDecimal somar(List<Credor> credores, ClasseCredor classe) {
        return credores.stream()
                .filter(c -> c.classe() == classe)
                .map(Credor::valorCredito)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
