package com.tcc.pjb.backend.service.execucaopenal;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DetracaoRemicaoService {

    private static final int DIAS_TRABALHO_POR_REMICAO = 3;
    private static final int HORAS_ESTUDO_POR_REMICAO = 12;
    private static final int DIAS_LEITURA_POR_LIVRO = 4;

    public record DetracaoRemicaoInput(
            int penaTotalDias,
            int diasPrisaoProvisoria,
            int diasInternacaoProvisoria,
            int diasTrabalho,
            int horasEstudo,
            int livrosLidos,
            boolean remicaoHomologadaPeloJuiz
    ) {}

    public record DetracaoRemicaoResult(
            int penaTotalOriginal,
            int diasDetracaoTotal,
            int diasRemicaoTrabalho,
            int diasRemicaoEstudo,
            int diasRemicaoLeitura,
            int diasRemicaoTotal,
            int penaSaldo,
            String fundamentacao,
            List<String> observacoes
    ) {}

    public DetracaoRemicaoResult calcular(DetracaoRemicaoInput input) {
        List<String> obs = new ArrayList<>();

        int detracao = input.diasPrisaoProvisoria() + input.diasInternacaoProvisoria();
        obs.add(String.format(
                "Detração: %d dias (prisão provisória: %d + internação: %d) — CP art. 42, LEP art. 111.",
                detracao, input.diasPrisaoProvisoria(), input.diasInternacaoProvisoria()));

        int remicaoTrabalho = input.diasTrabalho() / DIAS_TRABALHO_POR_REMICAO;
        if (input.diasTrabalho() > 0) {
            obs.add(String.format(
                    "Remição por trabalho: %d dias trabalhados / 3 = %d dias remidos (LEP art. 126).",
                    input.diasTrabalho(), remicaoTrabalho));
        }

        int remicaoEstudo = input.horasEstudo() / HORAS_ESTUDO_POR_REMICAO;
        if (input.horasEstudo() > 0) {
            obs.add(String.format(
                    "Remição por estudo: %d horas / 12 = %d dias remidos (LEP art. 126 §1º).",
                    input.horasEstudo(), remicaoEstudo));
        }

        int remicaoLeitura = input.livrosLidos() * DIAS_LEITURA_POR_LIVRO;
        if (input.livrosLidos() > 0) {
            obs.add(String.format(
                    "Remição por leitura: %d livros × 4 dias = %d dias remidos (LEP art. 126 §6º).",
                    input.livrosLidos(), remicaoLeitura));
        }

        int remicaoTotal = remicaoTrabalho + remicaoEstudo + remicaoLeitura;
        if (!input.remicaoHomologadaPeloJuiz() && remicaoTotal > 0) {
            obs.add("Atenção: remição depende de homologação judicial (LEP art. 126 §2º).");
        }

        int penaAposDetracao = Math.max(0, input.penaTotalDias() - detracao);
        int penaFinal = Math.max(0, penaAposDetracao - remicaoTotal);

        return new DetracaoRemicaoResult(
                input.penaTotalDias(), detracao,
                remicaoTrabalho, remicaoEstudo, remicaoLeitura,
                remicaoTotal, penaFinal,
                "Detração (CP art. 42) + Remição (LEP arts. 126-128).",
                obs);
    }
}
