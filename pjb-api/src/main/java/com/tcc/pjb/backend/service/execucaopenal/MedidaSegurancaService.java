package com.tcc.pjb.backend.service.execucaopenal;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MedidaSegurancaService {

    public enum ModalidadeMedidaSeguranca {
        INTERNACAO,
        TRATAMENTO_AMBULATORIAL
    }

    public record MedidaSegurancaInput(
            String processoId,
            int penaMaximaAbstrataAnos,
            int penaAplicadaAnos,
            boolean inimputavel,
            boolean semiimputavel,
            int anosInternacaoDecorridos,
            boolean laudoPericialAltaPericulosidade
    ) {}

    public record MedidaSegurancaResult(
            ModalidadeMedidaSeguranca modalidade,
            int prazoMinimoAnos,
            int prazoMaximoAnos,
            List<String> orientacoes,
            String sinalizacao
    ) {}

    private static final String SINAL_DESINTERNACAO =
            "Possível requisito para desinternação progressiva identificado — sujeito à validação judicial e laudo pericial.";
    private static final String SINAL_PADRAO =
            "Checklist de medida de segurança montado — não substitui análise do magistrado nem laudo pericial.";

    public MedidaSegurancaResult avaliar(MedidaSegurancaInput input) {
        List<String> orientacoes = new ArrayList<>();

        if (!input.inimputavel() && !input.semiimputavel()) {
            orientacoes.add("Possível requisito a conferir: medida de segurança aplicável apenas a inimputáveis ou semi-imputáveis (CP art. 26).");
            return new MedidaSegurancaResult(ModalidadeMedidaSeguranca.TRATAMENTO_AMBULATORIAL, 0, 0,
                    List.copyOf(orientacoes), SINAL_PADRAO);
        }

        ModalidadeMedidaSeguranca modalidade = input.penaMaximaAbstrataAnos() >= 1
                ? ModalidadeMedidaSeguranca.INTERNACAO
                : ModalidadeMedidaSeguranca.TRATAMENTO_AMBULATORIAL;

        int prazoMinimo = 1;
        int prazoMaximo = input.penaAplicadaAnos() > 0
                ? input.penaAplicadaAnos()
                : input.penaMaximaAbstrataAnos();

        orientacoes.add(String.format(
                "Modalidade sugerida: %s — pena máxima abstrata: %d ano(s) (CP art. 97). Sujeito à validação judicial.",
                modalidade, input.penaMaximaAbstrataAnos()));
        orientacoes.add(String.format(
                "Prazo: mínimo 1–3 anos, máximo equivalente à pena alternativa (%d anos) — Súmula 527 STJ.",
                prazoMaximo));

        boolean desinternacaoIndicada = input.anosInternacaoDecorridos() >= prazoMinimo
                && !input.laudoPericialAltaPericulosidade();

        if (desinternacaoIndicada) {
            orientacoes.add("Possível requisito a conferir: prazo mínimo cumprido e laudo favorável — desinternação progressiva pode ser cabível (LEP art. 97 §3º).");
        } else if (input.laudoPericialAltaPericulosidade()) {
            orientacoes.add("Pendência identificada: laudo pericial indica persistência de periculosidade — manutenção da internação a avaliar.");
        } else {
            orientacoes.add(String.format(
                    "Pendência identificada: %d ano(s) cumprido(s) de mínimo 1 ano — desinternação progressiva não indicada ainda.",
                    input.anosInternacaoDecorridos()));
        }

        orientacoes.add("Perícia anual obrigatória para verificação de cessação de periculosidade (CP art. 97 §2º).");

        return new MedidaSegurancaResult(modalidade, prazoMinimo, prazoMaximo,
                List.copyOf(orientacoes), desinternacaoIndicada ? SINAL_DESINTERNACAO : SINAL_PADRAO);
    }
}
