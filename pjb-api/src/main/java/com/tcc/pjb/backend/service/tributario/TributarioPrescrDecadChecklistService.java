package com.tcc.pjb.backend.service.tributario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TributarioPrescrDecadChecklistService {

    public enum ModalidadeLancamento {
        HOMOLOGACAO,
        OFICIO,
        DECLARACAO
    }

    public record TributarioPrescrDecadInput(
            String contribuinteCpfCnpj,
            LocalDate dataFatoGerador,
            LocalDate dataConstituicaoDefinitiva,
            LocalDate dataUltimoAtoCausaInterruptiva,
            ModalidadeLancamento modalidade,
            BigDecimal valorDebito,
            boolean execucaoFiscalAjuizada
    ) {}

    public record RequisitoCreditoTributario(
            String descricao,
            String fundamentoLegal,
            String observacao
    ) {}

    public record TributarioPrescrDecadResult(
            boolean decadenciaIdentificada,
            boolean prescricaoIdentificada,
            long diasRestantesDecadencia,
            long diasRestantesPrescricao,
            List<RequisitoCreditoTributario> requisitosIndicados,
            List<String> pendenciasIdentificadas,
            List<String> requisitosVerificados,
            String sinalizacao
    ) {}

    private static final long ANOS_DECADENCIA = 5;
    private static final long ANOS_PRESCRICAO = 5;

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação jurídica. Não substitui análise do advogado tributarista nem do contador.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado tributarista antes de qualquer pagamento, impugnação ou defesa em execução fiscal.";

    public TributarioPrescrDecadResult avaliar(TributarioPrescrDecadInput input) {
        List<RequisitoCreditoTributario> requisitos = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> verificados = new ArrayList<>();

        long diasDecadencia = verificarDecadencia(input, requisitos, pendencias, verificados);
        boolean decadenciaIdentificada = diasDecadencia <= 0;

        long diasPrescricao = verificarPrescricao(input, requisitos, pendencias, verificados);
        boolean prescricaoIdentificada = diasPrescricao <= 0 && input.dataConstituicaoDefinitiva() != null;

        adicionarInformacoesGerais(input, requisitos, verificados);

        return new TributarioPrescrDecadResult(
                decadenciaIdentificada,
                prescricaoIdentificada,
                Math.max(0, diasDecadencia),
                Math.max(0, diasPrescricao),
                List.copyOf(requisitos),
                List.copyOf(pendencias),
                List.copyOf(verificados),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }

    private long verificarDecadencia(TributarioPrescrDecadInput input,
            List<RequisitoCreditoTributario> requisitos, List<String> pendencias, List<String> verificados) {
        if (input.dataFatoGerador() == null) {
            pendencias.add("Pendência identificada: data do fato gerador não informada — necessária para apuração do prazo decadencial (CTN art. 150 §4º e art. 173).");
            return Long.MAX_VALUE;
        }

        LocalDate dataInicio = calcularInicioDecadencia(input);
        LocalDate dataLimite = dataInicio.plusYears(ANOS_DECADENCIA);
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), dataLimite);

        String fundamento = input.modalidade() == ModalidadeLancamento.HOMOLOGACAO
                ? "CTN art. 150 §4º"
                : "CTN art. 173 I";

        if (diasRestantes <= 0) {
            pendencias.add(String.format(
                    "Pendência identificada: crédito tributário possivelmente atingido pela decadência — prazo de 5 anos" +
                    " pode ter expirado em %s (%s). Conferir com urgência se houve constituição definitiva antes do término.",
                    dataLimite, fundamento));
            requisitos.add(new RequisitoCreditoTributario(
                    "Decadência — prazo possivelmente expirado",
                    fundamento,
                    "Sugestão sujeita à validação judicial — decadência extingue o crédito e não admite renúncia (CTN art. 156 V)."));
        } else if (diasRestantes <= 180) {
            pendencias.add(String.format(
                    "Possível requisito a conferir: %d dias restantes para decadência do crédito tributário (%s)" +
                    " — conferir urgência para lançamento ou constituição definitiva.", diasRestantes, fundamento));
        } else {
            verificados.add(String.format(
                    "Prazo decadencial: %d dias restantes de %d anos (%s) — dentro do prazo de constituição.",
                    diasRestantes, ANOS_DECADENCIA, fundamento));
        }

        return diasRestantes;
    }

    private LocalDate calcularInicioDecadencia(TributarioPrescrDecadInput input) {
        if (input.modalidade() == ModalidadeLancamento.HOMOLOGACAO) {
            return input.dataFatoGerador();
        }
        return LocalDate.of(input.dataFatoGerador().getYear() + 1, 1, 1);
    }

    private long verificarPrescricao(TributarioPrescrDecadInput input,
            List<RequisitoCreditoTributario> requisitos, List<String> pendencias, List<String> verificados) {
        if (input.dataConstituicaoDefinitiva() == null) {
            if (input.execucaoFiscalAjuizada()) {
                pendencias.add("Pendência identificada: execução fiscal ajuizada mas data de constituição definitiva do crédito" +
                        " não informada — necessária para apurar o prazo prescricional (CTN art. 174).");
            } else {
                verificados.add("Data de constituição definitiva não informada — prazo prescricional (CTN art. 174) não apurável neste checklist.");
            }
            return Long.MAX_VALUE;
        }

        LocalDate dataInicio = input.dataUltimoAtoCausaInterruptiva() != null
                ? input.dataUltimoAtoCausaInterruptiva()
                : input.dataConstituicaoDefinitiva();
        LocalDate dataLimite = dataInicio.plusYears(ANOS_PRESCRICAO);
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), dataLimite);

        if (input.dataUltimoAtoCausaInterruptiva() != null) {
            verificados.add(String.format(
                    "Causa interruptiva identificada em %s — prazo prescricional reiniciado (CTN art. 174 parágrafo único).",
                    input.dataUltimoAtoCausaInterruptiva()));
        }

        if (diasRestantes <= 0) {
            pendencias.add(String.format(
                    "Pendência identificada: crédito tributário possivelmente atingido pela prescrição — prazo de 5 anos" +
                    " pode ter expirado em %s (CTN art. 174). Conferir causas suspensivas ou reconhecimento judicial.",
                    dataLimite));
            requisitos.add(new RequisitoCreditoTributario(
                    "Prescrição — prazo possivelmente expirado",
                    "CTN art. 174",
                    "Sugestão sujeita à validação judicial — prescrição extingue o crédito (CTN art. 156 V). Verificar com advogado tributarista."));
        } else if (diasRestantes <= 180) {
            pendencias.add(String.format(
                    "Possível requisito a conferir: %d dias restantes para prescrição do crédito tributário (CTN art. 174)" +
                    " — conferir urgência para ajuizamento ou reconhecimento de pagamento.", diasRestantes));
        } else {
            verificados.add(String.format(
                    "Prazo prescricional: %d dias restantes de %d anos (CTN art. 174) — dentro do prazo.",
                    diasRestantes, ANOS_PRESCRICAO));
        }

        return diasRestantes;
    }

    private void adicionarInformacoesGerais(TributarioPrescrDecadInput input,
            List<RequisitoCreditoTributario> requisitos, List<String> verificados) {
        if (input.valorDebito() != null) {
            verificados.add(String.format(
                    "Valor indicado do débito: R$ %s — sujeito à confirmação junto à autoridade fiscal e à certidão de dívida ativa.",
                    input.valorDebito().setScale(2, RoundingMode.HALF_UP).toPlainString()));
        }

        if (input.execucaoFiscalAjuizada()) {
            requisitos.add(new RequisitoCreditoTributario(
                    "Execução fiscal ajuizada — verificar citação e penhora",
                    "Lei 6.830/80 art. 8º",
                    "Possível requisito a conferir: verificar se a citação foi regularmente realizada e se há bens penhoráveis." +
                    " A citação interrompe a prescrição (CTN art. 174 I)."));
        }

        requisitos.add(new RequisitoCreditoTributario(
                "Verificar certidão de regularidade fiscal (CND/CPEN)",
                "CTN art. 205 e 206",
                "Possível requisito a conferir: Certidão Negativa de Débitos (CND) ou Certidão Positiva com Efeitos de Negativa" +
                " (CPEN) — necessária para participação em licitações e operações que exijam regularidade fiscal."));
    }
}
