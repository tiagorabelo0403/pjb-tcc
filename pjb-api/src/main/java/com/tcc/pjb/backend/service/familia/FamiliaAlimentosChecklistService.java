package com.tcc.pjb.backend.service.familia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FamiliaAlimentosChecklistService {

    public enum VinculoFamiliar { PARENTESCO, CONJUGE_COMPANHEIRO, EX_CONJUGE }
    public enum ModalidadeAlimentos { PROVISORIOS, DEFINITIVOS, GRAVIDICOS }

    public record FamiliaAlimentosInput(
            String cpfAlimentando,
            VinculoFamiliar vinculo,
            ModalidadeAlimentos modalidade,
            BigDecimal rendaBrutaAlimentante,
            BigDecimal despesasFixasAlimentando,
            LocalDate dataUltimaFixacao,
            BigDecimal ultimoValorFixado,
            LocalDate dataInadimplenciaInicial,
            boolean execucaoEmAndamento
    ) {}

    public record OrientacaoAlimentar(
            String descricao,
            String fundamentoLegal,
            String observacao
    ) {}

    public record FamiliaAlimentosResult(
            String faixaOrientativaPercentual,
            boolean prescricaoParcelasIdentificada,
            long mesesParcelasNaoPrescritas,
            List<OrientacaoAlimentar> orientacoesIndicadas,
            List<String> pendenciasIdentificadas,
            List<String> requisitosVerificados,
            String sinalizacao
    ) {}

    private static final int MESES_PRESCRICAO_PARCELAS = 24;
    private static final int ANOS_REVISAO_RECOMENDADA = 3;

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação jurídica. Não substitui análise do advogado de família nem decisão judicial.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado especialista em direito de família antes de qualquer ato processual.";

    public FamiliaAlimentosResult avaliar(FamiliaAlimentosInput input) {
        List<OrientacaoAlimentar> orientacoes = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> verificados = new ArrayList<>();

        String faixaPercentual = avaliarFaixaOrientativa(input, verificados);
        verificarBinomio(input, orientacoes, pendencias, verificados);
        verificarPrescricaoParcelas(input, pendencias, verificados);
        verificarRevisaoAlimentos(input, pendencias, verificados);
        verificarModalidadeEVinculo(input, orientacoes, verificados);

        if (input.execucaoEmAndamento()) {
            orientacoes.add(new OrientacaoAlimentar(
                    "Coerção pessoal por inadimplemento",
                    "CPC art. 528 §3º",
                    "Possível requisito a conferir: inadimplemento de 3 prestações consecutivas pode ensejar prisão civil" +
                    " por até 90 dias (CPC art. 528 §3º) — sujeito à análise judicial da situação financeira do devedor."));
            orientacoes.add(new OrientacaoAlimentar(
                    "Desconto em folha de pagamento",
                    "Lei 5.478/68 art. 16; CPC art. 529",
                    "Possível requisito a conferir: se o alimentante é empregado, possível requerer desconto em folha" +
                    " de até 50% do salário líquido (CPC art. 529) — verificar vínculo empregatício atual."));
        }

        return new FamiliaAlimentosResult(
                faixaPercentual,
                verificarSePrescricaoAtingiu(input),
                calcularMesesNaoPrescritos(input),
                List.copyOf(orientacoes),
                List.copyOf(pendencias),
                List.copyOf(verificados),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }

    private String avaliarFaixaOrientativa(FamiliaAlimentosInput input, List<String> verificados) {
        if (input.rendaBrutaAlimentante() == null) {
            return "Não calculável — renda do alimentante não informada; apurar por outros meios (declaração de IR, contracheque, INFOJUD).";
        }
        String faixa = switch (input.vinculo()) {
            case PARENTESCO -> "15% a 30% da renda líquida — parâmetro orientativo STJ; não vincula o juízo";
            case CONJUGE_COMPANHEIRO -> "Variável conforme necessidade e possibilidade (CC art. 1.694) — sem percentual padrão para cônjuge/companheiro";
            case EX_CONJUGE -> "Variável conforme prova de necessidade e alteração de situação (CC art. 1.694 e 1.699) — sem percentual padrão";
        };
        verificados.add(String.format(
                "Renda bruta informada do alimentante: R$ %s — faixa orientativa: %s.",
                input.rendaBrutaAlimentante().toPlainString(), faixa));
        return faixa;
    }

    private void verificarBinomio(FamiliaAlimentosInput input,
            List<OrientacaoAlimentar> orientacoes, List<String> pendencias, List<String> verificados) {
        orientacoes.add(new OrientacaoAlimentar(
                "Binômio necessidade-possibilidade",
                "CC art. 1.694 §1º",
                "Possível requisito a conferir: os alimentos devem ser fixados na proporção da necessidade do alimentando" +
                " e da possibilidade do alimentante — ambos devem ser demonstrados com documentação (CC art. 1.694 §1º)."));

        if (input.despesasFixasAlimentando() == null) {
            pendencias.add("Pendência identificada: despesas fixas do alimentando não informadas — essenciais para demonstrar a necessidade no binômio (CC art. 1.694 §1º).");
        } else {
            verificados.add(String.format(
                    "Despesas fixas do alimentando: R$ %s — conferir documentação de suporte (faturas, recibos, declaração).",
                    input.despesasFixasAlimentando().toPlainString()));
        }

        if (input.rendaBrutaAlimentante() == null) {
            pendencias.add("Pendência identificada: renda do alimentante não informada — necessária para apurar a possibilidade (CC art. 1.694 §1º). Verificar por INFOJUD, BACENJUD ou declaração de IR.");
        }
    }

    private void verificarPrescricaoParcelas(FamiliaAlimentosInput input,
            List<String> pendencias, List<String> verificados) {
        if (input.dataInadimplenciaInicial() == null) return;

        long mesesInadimplencia = ChronoUnit.MONTHS.between(input.dataInadimplenciaInicial(), LocalDate.now());

        if (mesesInadimplencia > MESES_PRESCRICAO_PARCELAS) {
            pendencias.add(String.format(
                    "Pendência identificada: parcelas de alimentos com mais de 24 meses de inadimplência podem estar prescritas" +
                    " (%d meses desde o início da inadimplência — CC art. 206 §2º). Apenas parcelas dos últimos 24 meses" +
                    " são cobráveis em execução. Conferir com advogado.",
                    mesesInadimplencia));
        } else if (mesesInadimplencia > 18) {
            pendencias.add(String.format(
                    "Possível requisito a conferir: %d meses desde o início da inadimplência — prazo prescricional" +
                    " de 24 meses para parcelas vencidas se aproxima (CC art. 206 §2º). Conferir urgência.",
                    mesesInadimplencia));
        } else {
            verificados.add(String.format(
                    "Parcelas vencidas: %d meses de inadimplência — dentro do prazo de 24 meses (CC art. 206 §2º).",
                    mesesInadimplencia));
        }
    }

    private void verificarRevisaoAlimentos(FamiliaAlimentosInput input,
            List<String> pendencias, List<String> verificados) {
        if (input.dataUltimaFixacao() == null) {
            verificados.add("Alimentos ainda não fixados judicialmente — primeira fixação a requerer.");
            return;
        }

        long anosDesdeFixacao = ChronoUnit.YEARS.between(input.dataUltimaFixacao(), LocalDate.now());

        if (anosDesdeFixacao >= ANOS_REVISAO_RECOMENDADA) {
            pendencias.add(String.format(
                    "Possível requisito a conferir: alimentos fixados há %d ano(s) (em %s) — avaliar revisão por" +
                    " mudança na situação financeira das partes (CC art. 1.699). Sujeito à prova de alteração de fortuna.",
                    anosDesdeFixacao, input.dataUltimaFixacao()));
        } else {
            verificados.add(String.format(
                    "Última fixação: %s (%d ano(s) atrás) — dentro do período sem revisão obrigatória.",
                    input.dataUltimaFixacao(), anosDesdeFixacao));
        }

        if (input.ultimoValorFixado() != null) {
            verificados.add(String.format(
                    "Valor de referência da última fixação: R$ %s — conferir se houve reajuste por índice acordado ou determinado judicialmente.",
                    input.ultimoValorFixado().toPlainString()));
        }
    }

    private void verificarModalidadeEVinculo(FamiliaAlimentosInput input,
            List<OrientacaoAlimentar> orientacoes, List<String> verificados) {
        switch (input.modalidade()) {
            case PROVISORIOS -> {
                orientacoes.add(new OrientacaoAlimentar(
                        "Alimentos provisórios — tutela de urgência",
                        "Lei 5.478/68 art. 4º; CPC art. 300",
                        "Possível requisito a conferir: alimentos provisórios podem ser fixados inaudita altera parte" +
                        " mediante prova do vínculo e necessidade. Sujeito à decisão do juízo competente."));
                verificados.add("Modalidade: alimentos provisórios — decisão liminar possível sem audiência da parte contrária (Lei 5.478/68 art. 4º).");
            }
            case GRAVIDICOS -> {
                orientacoes.add(new OrientacaoAlimentar(
                        "Alimentos gravídicos — Lei 11.804/08",
                        "Lei 11.804/08 art. 2º",
                        "Possível requisito a conferir: alimentos gravídicos abrangem despesas da gestação (pré-natal, parto, alimentação," +
                        " plano de saúde) — fixados desde a concepção até o nascimento. Exige indícios de paternidade."));
                verificados.add("Modalidade: alimentos gravídicos — vigência até o nascimento; convertidos em pensão alimentícia (Lei 11.804/08 art. 6º).");
            }
            case DEFINITIVOS -> verificados.add("Modalidade: alimentos definitivos — fixados em sentença após instrução probatória (CC art. 1.694).");
        }

        if (input.vinculo() == VinculoFamiliar.EX_CONJUGE) {
            orientacoes.add(new OrientacaoAlimentar(
                    "Alimentos entre ex-cônjuges — caráter excepcional",
                    "CC art. 1.704 e 1.708",
                    "Possível requisito a conferir: alimentos entre ex-cônjuges têm caráter excepcional e transitório" +
                    " — exigem prova de necessidade real e impossibilidade de sustento próprio (CC art. 1.704 parágrafo único)."));
        }
    }

    private boolean verificarSePrescricaoAtingiu(FamiliaAlimentosInput input) {
        if (input.dataInadimplenciaInicial() == null) return false;
        long meses = ChronoUnit.MONTHS.between(input.dataInadimplenciaInicial(), LocalDate.now());
        return meses > MESES_PRESCRICAO_PARCELAS;
    }

    private long calcularMesesNaoPrescritos(FamiliaAlimentosInput input) {
        if (input.dataInadimplenciaInicial() == null) return MESES_PRESCRICAO_PARCELAS;
        long meses = ChronoUnit.MONTHS.between(input.dataInadimplenciaInicial(), LocalDate.now());
        return Math.max(0, MESES_PRESCRICAO_PARCELAS - meses);
    }
}
