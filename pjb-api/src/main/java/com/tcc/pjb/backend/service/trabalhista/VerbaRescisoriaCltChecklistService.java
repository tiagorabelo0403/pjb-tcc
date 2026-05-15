package com.tcc.pjb.backend.service.trabalhista;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VerbaRescisoriaCltChecklistService {

    public enum MotivoRescisao {
        DISPENSA_SEM_JUSTA_CAUSA,
        DISPENSA_COM_JUSTA_CAUSA,
        PEDIDO_DEMISSAO,
        RESCISAO_INDIRETA,
        ACORDO_INDIVIDUAL
    }

    public record VerbaRescisoriaCltInput(
            String cpf,
            LocalDate dataAdmissao,
            LocalDate dataRescisao,
            MotivoRescisao motivo,
            BigDecimal salarioMensal,
            int diasFeriasVencidas,
            boolean avisoPrevioTrabalhado,
            boolean fgtsDepositadoCorretamente
    ) {}

    public record VerbaEstimada(
            String descricao,
            String fundamentoLegal,
            String valorEstimado,
            String observacao
    ) {}

    public record VerbaRescisoriaCltResult(
            MotivoRescisao motivo,
            int mesesContrato,
            int diasAvisoPrevioIndicado,
            List<VerbaEstimada> verbasIndicadas,
            List<String> pendenciasIdentificadas,
            List<String> requisitosVerificados,
            String sinalizacao
    ) {}

    private static final int AVISO_PREVIO_BASE_DIAS = 30;
    private static final int AVISO_PREVIO_ACRESCIMO_POR_ANO = 3;
    private static final int AVISO_PREVIO_MAXIMO_DIAS = 90;
    private static final int MESES_PRESCRICAO_POS_CONTRATO = 24;
    private static final int ANOS_PRESCRICAO_DURANTE_CONTRATO = 5;

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação pericial e trabalhista. Não substitui análise do advogado nem do sindicato.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado trabalhista antes de assinar qualquer documento.";

    public VerbaRescisoriaCltResult avaliar(VerbaRescisoriaCltInput input) {
        List<VerbaEstimada> verbas = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> verificados = new ArrayList<>();

        long mesesContrato = input.dataAdmissao() != null && input.dataRescisao() != null
                ? ChronoUnit.MONTHS.between(input.dataAdmissao(), input.dataRescisao())
                : 0;
        long anosCompletos = mesesContrato / 12;

        if (input.dataAdmissao() == null || input.dataRescisao() == null) {
            pendencias.add("Pendência identificada: datas de admissão ou rescisão não informadas — necessárias para apuração de verbas.");
        } else if (input.dataRescisao().isBefore(input.dataAdmissao())) {
            pendencias.add("Pendência identificada: data de rescisão anterior à admissão — verificar informações do contrato.");
        } else {
            verificados.add(String.format("Contrato de trabalho: %d meses (%d ano(s) completo(s)) — conferido (CLT art. 477).", mesesContrato, anosCompletos));
        }

        int diasAviso = calcularAvisoPrevio((int) anosCompletos);

        switch (input.motivo()) {
            case DISPENSA_SEM_JUSTA_CAUSA -> avaliarDispensaSemJustaCausa(input, verbas, verificados, pendencias, mesesContrato, anosCompletos, diasAviso);
            case DISPENSA_COM_JUSTA_CAUSA -> avaliarDispensaComJustaCausa(input, verbas, verificados, pendencias, mesesContrato);
            case PEDIDO_DEMISSAO -> avaliarPedidoDemissao(input, verbas, verificados, pendencias, mesesContrato);
            case RESCISAO_INDIRETA -> avaliarRescisaoIndireta(input, verbas, verificados, pendencias, mesesContrato, anosCompletos, diasAviso);
            case ACORDO_INDIVIDUAL -> avaliarAcordoIndividual(input, verbas, verificados, pendencias, mesesContrato, anosCompletos, diasAviso);
        }

        verificarPrescricao(input, pendencias, verificados, mesesContrato);

        if (!input.fgtsDepositadoCorretamente()) {
            pendencias.add("Pendência identificada: FGTS possivelmente não depositado corretamente — conferir extrato FGTS junto à Caixa Econômica Federal (Lei 8.036/90 art. 13).");
        }

        return new VerbaRescisoriaCltResult(
                input.motivo(),
                (int) mesesContrato,
                diasAviso,
                List.copyOf(verbas),
                List.copyOf(pendencias),
                List.copyOf(verificados),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }

    private int calcularAvisoPrevio(int anosCompletos) {
        int dias = AVISO_PREVIO_BASE_DIAS + (anosCompletos * AVISO_PREVIO_ACRESCIMO_POR_ANO);
        return Math.min(dias, AVISO_PREVIO_MAXIMO_DIAS);
    }

    private void avaliarDispensaSemJustaCausa(VerbaRescisoriaCltInput input, List<VerbaEstimada> verbas,
            List<String> verificados, List<String> pendencias, long meses, long anos, int diasAviso) {
        verificados.add(String.format("Motivo: dispensa sem justa causa — aviso prévio proporcional indicado: %d dias (CLT art. 487 + Lei 12.506/2011).", diasAviso));

        if (!input.avisoPrevioTrabalhado()) {
            verbas.add(new VerbaEstimada(
                    "Aviso prévio indenizado",
                    "CLT art. 487 § 1º; Lei 12.506/2011",
                    estimarAvisoPrevio(input.salarioMensal(), diasAviso),
                    String.format("Estimativa para %d dias — sujeita à conferência do salário base e projeção de benefícios.", diasAviso)));
        } else {
            verificados.add("Aviso prévio trabalhado — sem indenização por este item.");
        }

        adicionarSaldoSalario(input, verbas);
        adicionarDecimoterceiroProporcional(input, verbas, meses);
        adicionarFeriasProporcional(input, verbas, meses);

        if (input.diasFeriasVencidas() > 0) {
            adicionarFeriasVencidas(input, verbas);
        }

        verbas.add(new VerbaEstimada(
                "Multa FGTS (40%)",
                "Lei 8.036/90 art. 18 § 1º",
                "Calculada sobre saldo total do FGTS — verificar extrato CEF",
                "Pendente de conferência do saldo do FGTS junto à Caixa Econômica Federal."));

        verificados.add("Verbas rescisórias de dispensa sem justa causa mapeadas — sujeitas à conferência do sindicato (CLT art. 477 § 1º).");
    }

    private void avaliarDispensaComJustaCausa(VerbaRescisoriaCltInput input, List<VerbaEstimada> verbas,
            List<String> verificados, List<String> pendencias, long meses) {
        verificados.add("Motivo: dispensa com justa causa — verbas restritas ao saldo de salário e férias vencidas (CLT art. 482).");
        pendencias.add("Possível requisito a conferir: justa causa deve estar devidamente documentada e enquadrada nos incisos do CLT art. 482 — risco de reversão judicial se não comprovada.");

        adicionarSaldoSalario(input, verbas);

        if (input.diasFeriasVencidas() > 0) {
            adicionarFeriasVencidas(input, verbas);
        } else {
            verificados.add("Férias vencidas: não indicadas — confirmar se há período aquisitivo completo sem gozo.");
        }

        verificados.add("Ausência de multa FGTS 40%, aviso prévio, 13º proporcional e férias proporcionais — restrito à modalidade de dispensa por justa causa.");
    }

    private void avaliarPedidoDemissao(VerbaRescisoriaCltInput input, List<VerbaEstimada> verbas,
            List<String> verificados, List<String> pendencias, long meses) {
        verificados.add("Motivo: pedido de demissão — sem multa FGTS 40% (CLT art. 18); aviso prévio pode ser cumprido ou indenizado pelo empregado.");
        pendencias.add("Possível requisito a conferir: empregado que não cumpre aviso prévio pode ter desconto proporcional nas verbas (CLT art. 487 § 2º).");

        adicionarSaldoSalario(input, verbas);
        adicionarDecimoterceiroProporcional(input, verbas, meses);
        adicionarFeriasProporcional(input, verbas, meses);

        if (input.diasFeriasVencidas() > 0) {
            adicionarFeriasVencidas(input, verbas);
        }

        verificados.add("Levantamento de FGTS sem multa no pedido de demissão — saldo disponível apenas em casos excepcionais (Lei 8.036/90 art. 20).");
    }

    private void avaliarRescisaoIndireta(VerbaRescisoriaCltInput input, List<VerbaEstimada> verbas,
            List<String> verificados, List<String> pendencias, long meses, long anos, int diasAviso) {
        verificados.add(String.format("Motivo: rescisão indireta — equiparada à dispensa sem justa causa (CLT art. 483); aviso prévio indicado: %d dias.", diasAviso));
        pendencias.add("Possível requisito a conferir: rescisão indireta exige caracterização de falta grave do empregador — conferir enquadramento nos incisos do CLT art. 483 antes de protocolar.");

        adicionarSaldoSalario(input, verbas);
        adicionarDecimoterceiroProporcional(input, verbas, meses);
        adicionarFeriasProporcional(input, verbas, meses);

        if (input.diasFeriasVencidas() > 0) {
            adicionarFeriasVencidas(input, verbas);
        }

        verbas.add(new VerbaEstimada(
                "Aviso prévio indenizado (rescisão indireta)",
                "CLT art. 483 § 1º; Lei 12.506/2011",
                estimarAvisoPrevio(input.salarioMensal(), diasAviso),
                "Estimativa sujeita à homologação judicial — rescisão indireta depende de reconhecimento pelo juízo trabalhista."));

        verbas.add(new VerbaEstimada(
                "Multa FGTS (40%)",
                "Lei 8.036/90 art. 18 § 1º; CLT art. 483 § 1º",
                "Calculada sobre saldo total do FGTS — verificar extrato CEF",
                "Sujeita ao reconhecimento judicial da rescisão indireta."));
    }

    private void avaliarAcordoIndividual(VerbaRescisoriaCltInput input, List<VerbaEstimada> verbas,
            List<String> verificados, List<String> pendencias, long meses, long anos, int diasAviso) {
        verificados.add(String.format("Motivo: acordo individual (CLT art. 484-A) — metade do aviso prévio (%d dias) e metade da multa FGTS (20%%).", diasAviso / 2));
        pendencias.add("Possível requisito a conferir: acordo individual deve ser formalizado por escrito; empregado pode levantar 80% do FGTS mas perde seguro-desemprego (CLT art. 484-A § 1º).");

        adicionarSaldoSalario(input, verbas);
        adicionarDecimoterceiroProporcional(input, verbas, meses);
        adicionarFeriasProporcional(input, verbas, meses);

        if (input.diasFeriasVencidas() > 0) {
            adicionarFeriasVencidas(input, verbas);
        }

        verbas.add(new VerbaEstimada(
                "Aviso prévio indenizado (metade)",
                "CLT art. 484-A I",
                estimarAvisoPrevio(input.salarioMensal(), diasAviso / 2),
                "Metade do aviso prévio proporcional — o restante é cumprido ou negociado."));

        verbas.add(new VerbaEstimada(
                "Multa FGTS reduzida (20%)",
                "CLT art. 484-A II",
                "Calculada sobre 20% do saldo total do FGTS — verificar extrato CEF",
                "Metade da multa padrão; empregado pode sacar 80% do saldo do FGTS."));
    }

    private void adicionarSaldoSalario(VerbaRescisoriaCltInput input, List<VerbaEstimada> verbas) {
        verbas.add(new VerbaEstimada(
                "Saldo de salário",
                "CLT art. 477 caput",
                "Proporcional aos dias trabalhados no último mês — apurar junto ao RH",
                "Verificar holerite do último mês e dias efetivamente trabalhados."));
    }

    private void adicionarDecimoterceiroProporcional(VerbaRescisoriaCltInput input, List<VerbaEstimada> verbas, long meses) {
        int mesesNoAno = meses > 0 ? (int) (meses % 12) : 0;
        if (mesesNoAno == 0 && meses >= 12) mesesNoAno = 12;
        if (mesesNoAno == 0) return;

        String estimativa = input.salarioMensal() != null
                ? "R$ " + input.salarioMensal().multiply(BigDecimal.valueOf(mesesNoAno)).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                : "Verificar com base no salário contratual";

        verbas.add(new VerbaEstimada(
                String.format("13º salário proporcional (%d/12)", mesesNoAno),
                "CLT art. 7 CF; Lei 4.090/62",
                estimativa,
                "Estimativa sobre salário base — conferir avos de 13º já pagos no ano."));
    }

    private void adicionarFeriasProporcional(VerbaRescisoriaCltInput input, List<VerbaEstimada> verbas, long meses) {
        int mesesNoAno = meses > 0 ? (int) (meses % 12) : 0;
        if (mesesNoAno == 0 && meses >= 12) mesesNoAno = 12;
        if (mesesNoAno == 0) return;

        String estimativa = input.salarioMensal() != null
                ? "R$ " + input.salarioMensal().multiply(BigDecimal.valueOf(mesesNoAno)).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("1.333")).setScale(2, RoundingMode.HALF_UP)
                : "Verificar com base no salário contratual";

        verbas.add(new VerbaEstimada(
                String.format("Férias proporcionais + 1/3 (%d/12)", mesesNoAno),
                "CLT art. 134, 143, 146 e 147",
                estimativa,
                "Estimativa com acréscimo de 1/3 constitucional (CF art. 7 XVII) — sujeita à conferência do período aquisitivo."));
    }

    private void adicionarFeriasVencidas(VerbaRescisoriaCltInput input, List<VerbaEstimada> verbas) {
        String estimativa = input.salarioMensal() != null
                ? "R$ " + input.salarioMensal().multiply(new BigDecimal("1.333")).setScale(2, RoundingMode.HALF_UP)
                : "Verificar com base no salário contratual";

        verbas.add(new VerbaEstimada(
                "Férias vencidas + 1/3",
                "CLT art. 134 e 146",
                estimativa,
                String.format("%d dias de férias vencidas indicados — conferir período aquisitivo e ausências que possam ter reduzido o direito.", input.diasFeriasVencidas())));
    }

    private void verificarPrescricao(VerbaRescisoriaCltInput input, List<String> pendencias,
            List<String> verificados, long meses) {
        if (input.dataRescisao() == null) return;

        long mesesDesdeRescisao = ChronoUnit.MONTHS.between(input.dataRescisao(), LocalDate.now());

        if (mesesDesdeRescisao > MESES_PRESCRICAO_POS_CONTRATO) {
            pendencias.add(String.format(
                    "Pendência identificada: possível prescrição — %d meses desde a rescisão; prazo de 2 anos pós-extinção do contrato pode estar expirado (CF art. 7 XXIX). Verificar com urgência.",
                    mesesDesdeRescisao));
        } else if (mesesDesdeRescisao > 18) {
            pendencias.add(String.format(
                    "Possível requisito a conferir: %d meses desde a rescisão — prazo prescricional de 2 anos se aproxima (CF art. 7 XXIX). Conferir urgência para ajuizamento.",
                    mesesDesdeRescisao));
        } else {
            verificados.add(String.format(
                    "Prazo prescricional: %d meses desde a rescisão — dentro do prazo de 2 anos (CF art. 7 XXIX).", mesesDesdeRescisao));
        }
    }

    private String estimarAvisoPrevio(BigDecimal salario, int dias) {
        if (salario == null) return "Verificar com base no salário contratual";
        BigDecimal valorDiario = salario.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        return "R$ " + valorDiario.multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
    }
}
