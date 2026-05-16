package com.tcc.pjb.backend.service.penalprescricao;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PenalPrescriacaoChecklistService {

    public enum FasePrescricao { PRETENSAO_PUNITIVA, PRETENSAO_EXECUTORIA }
    public enum ClasseEtaria { MENOR_21_ANOS_CRIME, MAIOR_70_ANOS_SENTENCA, NENHUMA }

    public record PenalPrescriacaoInput(
            double penaMinimaAnos,
            double penaMaximaAnos,
            FasePrescricao fase,
            ClasseEtaria classeEtaria,
            LocalDate dataFatoOuRecebitoQueixa,
            LocalDate dataTransitoJulgado,
            LocalDate dataUltimoAtoCausaInterruptiva,
            boolean causaEspecialReducao
    ) {}

    public record MarcoPrescrional(
            String descricao,
            String fundamentoLegal,
            LocalDate data
    ) {}

    public record PenalPrescriacaoResult(
            int prazoPrescriacaoAnos,
            boolean prescricaoExpirada,
            long diasParaPrescricao,
            List<MarcoPrescrional> marcosRelevantes,
            List<String> pendenciasIdentificadas,
            List<String> orientacoes,
            String sinalizacao
    ) {}

    private static final String SINAL_NAO_PRESCRITO =
            "Sem prescrição identificada com os dados fornecidos — checklist orientativo. Não substitui análise de advogado criminalista.";
    private static final String SINAL_PRESCRITO =
            "Possível extinção da punibilidade por prescrição — verificar com advogado criminalista e requerer ao juízo (CP art. 107 IV / art. 61 CPP).";
    private static final String SINAL_PROXIMO =
            "Prazo prescricional se aproxima — providenciar ato interruptivo ou verificar causa impeditiva com urgência.";
    private static final int DIAS_AVISO = 90;

    public PenalPrescriacaoResult avaliar(PenalPrescriacaoInput input) {
        List<MarcoPrescrional> marcos = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> orientacoes = new ArrayList<>();

        int prazo = calcularPrazoAnos(input);
        verificarDadosObrigatorios(input, pendencias);
        construirMarcos(input, marcos);
        verificarCausasEspeciais(input, prazo, orientacoes);

        boolean prescrito = avaliarPrescricao(input, prazo, pendencias, orientacoes);
        long dias = calcularDiasRestantes(input, prazo);

        String sinal = prescrito ? SINAL_PRESCRITO
                     : dias >= 0 && dias < DIAS_AVISO ? SINAL_PROXIMO
                     : SINAL_NAO_PRESCRITO;

        return new PenalPrescriacaoResult(
                prazo,
                prescrito,
                Math.max(0, dias),
                List.copyOf(marcos),
                List.copyOf(pendencias),
                List.copyOf(orientacoes),
                sinal);
    }

    private int calcularPrazoAnos(PenalPrescriacaoInput input) {
        double pena = input.fase() == FasePrescricao.PRETENSAO_PUNITIVA
                ? input.penaMaximaAnos()
                : input.penaMinimaAnos();

        int prazo;
        if (pena > 12) prazo = 20;
        else if (pena > 8) prazo = 16;
        else if (pena > 4) prazo = 12;
        else if (pena > 2) prazo = 8;
        else if (pena >= 1) prazo = 4;
        else prazo = 3;

        if (input.classeEtaria() != ClasseEtaria.NENHUMA) {
            prazo = prazo / 2;
        }
        if (input.causaEspecialReducao()) {
            prazo = prazo / 2;
        }

        return Math.max(prazo, 3);
    }

    private void verificarDadosObrigatorios(PenalPrescriacaoInput input, List<String> pendencias) {
        if (input.penaMaximaAnos() <= 0) {
            pendencias.add("Pendência: pena máxima cominada não informada — necessária para calcular prescrição da pretensão punitiva (CP art. 109).");
        }
        if (input.fase() == FasePrescricao.PRETENSAO_EXECUTORIA && input.dataTransitoJulgado() == null) {
            pendencias.add("Pendência: data do trânsito em julgado não informada — necessária para calcular prescrição da pretensão executória (CP art. 110 §1º).");
        }
        if (input.dataFatoOuRecebitoQueixa() == null) {
            pendencias.add("Pendência: data do fato ou do recebimento da queixa/denúncia não informada — necessária para fixar o termo inicial (CP art. 111).");
        }
    }

    private void construirMarcos(PenalPrescriacaoInput input, List<MarcoPrescrional> marcos) {
        if (input.dataFatoOuRecebitoQueixa() != null) {
            marcos.add(new MarcoPrescrional(
                    "Termo inicial — data do fato / recebimento da denúncia",
                    "CP art. 111 I",
                    input.dataFatoOuRecebitoQueixa()));
        }
        if (input.dataUltimoAtoCausaInterruptiva() != null) {
            marcos.add(new MarcoPrescrional(
                    "Último ato interruptivo da prescrição",
                    "CP art. 117",
                    input.dataUltimoAtoCausaInterruptiva()));
        }
        if (input.dataTransitoJulgado() != null) {
            marcos.add(new MarcoPrescrional(
                    "Trânsito em julgado — início da prescrição executória",
                    "CP art. 112 I",
                    input.dataTransitoJulgado()));
        }
    }

    private void verificarCausasEspeciais(PenalPrescriacaoInput input, int prazo, List<String> orientacoes) {
        if (input.classeEtaria() == ClasseEtaria.MENOR_21_ANOS_CRIME) {
            orientacoes.add("Redução de metade aplicada: réu menor de 21 anos na data do crime (CP art. 115). Verificar prova da idade na época do fato.");
        }
        if (input.classeEtaria() == ClasseEtaria.MAIOR_70_ANOS_SENTENCA) {
            orientacoes.add("Redução de metade aplicada: réu maior de 70 anos na data da sentença (CP art. 115). Verificar data exata da sentença.");
        }
        if (input.causaEspecialReducao()) {
            orientacoes.add("Causa especial de redução aplicada (ex.: tentativa — CP art. 14 II; arrependimento posterior — CP art. 16). Confirmar qual causa reduz a pena concreta.");
        }
        orientacoes.add(String.format(
                "Prazo prescricional calculado: %d ano(s) — baseado na %s (CP art. 109).",
                prazo, input.fase() == FasePrescricao.PRETENSAO_PUNITIVA ? "pena máxima cominada" : "pena mínima cominada"));
        orientacoes.add("Causas interruptivas (CP art. 117): recebimento da denúncia, pronúncia, decisão confirmatória, sentença condenatória recorrível. Cada interrupção reinicia o prazo.");
    }

    private boolean avaliarPrescricao(PenalPrescriacaoInput input, int prazo,
            List<String> pendencias, List<String> orientacoes) {
        if (input.dataFatoOuRecebitoQueixa() == null) return false;

        LocalDate termoInicial = input.dataUltimoAtoCausaInterruptiva() != null
                ? input.dataUltimoAtoCausaInterruptiva()
                : (input.fase() == FasePrescricao.PRETENSAO_EXECUTORIA && input.dataTransitoJulgado() != null
                        ? input.dataTransitoJulgado()
                        : input.dataFatoOuRecebitoQueixa());

        LocalDate vencimento = termoInicial.plusYears(prazo);
        boolean prescrito = vencimento.isBefore(LocalDate.now());

        if (!prescrito) {
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), vencimento);
            if (dias < DIAS_AVISO) {
                pendencias.add(String.format(
                        "Atenção: %d dias para a prescrição — providenciar ato interruptivo (CP art. 117) ou requerer ao juízo com urgência.",
                        dias));
            }
        }
        return prescrito;
    }

    private long calcularDiasRestantes(PenalPrescriacaoInput input, int prazo) {
        if (input.dataFatoOuRecebitoQueixa() == null) return 0;

        LocalDate termoInicial = input.dataUltimoAtoCausaInterruptiva() != null
                ? input.dataUltimoAtoCausaInterruptiva()
                : (input.fase() == FasePrescricao.PRETENSAO_EXECUTORIA && input.dataTransitoJulgado() != null
                        ? input.dataTransitoJulgado()
                        : input.dataFatoOuRecebitoQueixa());

        return ChronoUnit.DAYS.between(LocalDate.now(), termoInicial.plusYears(prazo));
    }
}
