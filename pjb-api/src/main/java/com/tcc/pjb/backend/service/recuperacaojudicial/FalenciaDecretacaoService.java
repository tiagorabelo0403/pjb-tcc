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
            boolean decretacaoViavel,
            CausaFalencia causaConfigurada,
            List<String> analise,
            List<String> impeditivos
    ) {}

    public FalenciaResult avaliar(FalenciaInput input) {
        List<String> analise = new ArrayList<>();
        List<String> impeditivos = new ArrayList<>();

        if (input.pedidoRecuperacaoPendenteOuDeferido()) {
            impeditivos.add("Recuperação judicial pendente ou deferida — stay period ativo (Lei 11.101/2005 art. 6º).");
        }

        switch (input.causa()) {
            case IMPONTUALIDADE -> {
                BigDecimal limiteValor = LIMITE_IMPONTUALIDADE_SALARIOS.multiply(VALOR_SALARIO_MINIMO);
                if (input.valorDividaImpontualidade() != null
                        && input.valorDividaImpontualidade().compareTo(limiteValor) > 0) {
                    analise.add(String.format(
                            "Impontualidade configurada: dívida R$ %.2f > 40 salários mínimos (R$ %.2f) — art. 94 I.",
                            input.valorDividaImpontualidade(), limiteValor));
                } else {
                    impeditivos.add(String.format(
                            "Valor insuficiente para impontualidade: inferior a 40 salários mínimos (R$ %.2f).", limiteValor));
                }
            }
            case EXECUCAO_FRUSTRADA -> {
                if (input.execucaoFrustrada()) {
                    analise.add("Execução frustrada: devedor não pagou, não depositou nem nomeou bens à penhora (art. 94 II).");
                } else {
                    impeditivos.add("Execução frustrada não configurada.");
                }
            }
            case ATO_DE_FALENCIA -> {
                if (input.atoFalencia()) {
                    analise.add("Ato de falência configurado: " + input.descricaoAtoFalencia() + " (art. 94 III).");
                } else {
                    impeditivos.add("Ato de falência não configurado (art. 94 III a–l).");
                }
            }
        }

        return new FalenciaResult(impeditivos.isEmpty(), input.causa(), analise, impeditivos);
    }
}
