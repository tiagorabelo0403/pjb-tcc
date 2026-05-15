package com.tcc.pjb.backend.service.recuperacaojudicial;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FalenciaDecretacaoService {

    private static final BigDecimal LIMITE_IMPONTUALIDADE_SALARIOS = new BigDecimal("40");
    private static final BigDecimal VALOR_SALARIO_MINIMO = new BigDecimal("1412.00");

    public enum CausaFalencia {
        IMPONTUALIDADE,
        EXECUCAO_FRUSTRADA,
        ATO_DE_FALENCIA
    }

    public record FalenciaInput(
            String cnpj,
            CausaFalencia causa,
            BigDecimal valorDividaImpontualidade,
            boolean execucaoFrustrada,
            boolean atoFalencia,
            String descricaoAtoFalencia,
            boolean pedidoRecuperacaoPendenteOuDeferido
    ) {}

    public record FalenciaResult(
            CausaFalencia causaApurada,
            List<String> indicativosIdentificados,
            List<String> pendenciasIdentificadas,
            String sinalizacao
    ) {}

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação judicial. Não substitui análise do magistrado.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir antes de submeter ao magistrado.";

    public FalenciaResult avaliar(FalenciaInput input) {
        List<String> indicativos = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        if (input.pedidoRecuperacaoPendenteOuDeferido()) {
            pendencias.add("Pendência identificada: stay period possivelmente ativo — recuperação judicial pendente ou deferida (Lei 11.101/2005 art. 6º).");
        }

        switch (input.causa()) {
            case IMPONTUALIDADE -> {
                BigDecimal limiteValor = LIMITE_IMPONTUALIDADE_SALARIOS.multiply(VALOR_SALARIO_MINIMO);
                if (input.valorDividaImpontualidade() != null
                        && input.valorDividaImpontualidade().compareTo(limiteValor) > 0) {
                    indicativos.add(String.format(
                            "Possível requisito a conferir: impontualidade — dívida R$ %.2f > 40 salários mínimos (R$ %.2f) — art. 94 I.",
                            input.valorDividaImpontualidade(), limiteValor));
                } else {
                    pendencias.add(String.format(
                            "Pendência identificada: valor inferior a 40 salários mínimos (R$ %.2f) — impontualidade pode não estar configurada.", limiteValor));
                }
            }
            case EXECUCAO_FRUSTRADA -> {
                if (input.execucaoFrustrada()) {
                    indicativos.add("Possível requisito a conferir: execução frustrada — devedor não pagou, não depositou nem nomeou bens à penhora (art. 94 II).");
                } else {
                    pendencias.add("Pendência identificada: execução frustrada não configurada — conferir requisitos do art. 94 II.");
                }
            }
            case ATO_DE_FALENCIA -> {
                if (input.atoFalencia()) {
                    indicativos.add("Possível requisito a conferir: ato de falência — " + input.descricaoAtoFalencia() + " (art. 94 III).");
                } else {
                    pendencias.add("Pendência identificada: ato de falência não configurado — conferir hipóteses do art. 94 III a–l.");
                }
            }
        }

        return new FalenciaResult(input.causa(), List.copyOf(indicativos), List.copyOf(pendencias),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }
}
