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
            boolean desinternacaoProgressivaCabivel,
            List<String> orientacoes
    ) {}

    public MedidaSegurancaResult avaliar(MedidaSegurancaInput input) {
        List<String> orientacoes = new ArrayList<>();

        if (!input.inimputavel() && !input.semiimputavel()) {
            orientacoes.add("Medida de segurança aplicável apenas a inimputáveis ou semi-imputáveis (CP art. 26).");
            return new MedidaSegurancaResult(ModalidadeMedidaSeguranca.TRATAMENTO_AMBULATORIAL, 0, 0, false, orientacoes);
        }

        ModalidadeMedidaSeguranca modalidade = input.penaMaximaAbstrataAnos() >= 1
                ? ModalidadeMedidaSeguranca.INTERNACAO
                : ModalidadeMedidaSeguranca.TRATAMENTO_AMBULATORIAL;

        int prazoMinimo = 1;
        int prazoMaximo = input.penaAplicadaAnos() > 0
                ? input.penaAplicadaAnos()
                : input.penaMaximaAbstrataAnos();

        orientacoes.add(String.format(
                "Modalidade: %s — pena máxima abstrata: %d ano(s) (CP art. 97).",
                modalidade, input.penaMaximaAbstrataAnos()));
        orientacoes.add(String.format(
                "Prazo: mínimo 1–3 anos, máximo equivalente à pena alternativa (%d anos) — Súmula 527 STJ.",
                prazoMaximo));

        boolean desinternacaoCabivel = input.anosInternacaoDecorridos() >= prazoMinimo
                && !input.laudoPericialAltaPericulosidade();

        if (desinternacaoCabivel) {
            orientacoes.add("Desinternação progressiva cabível: prazo mínimo cumprido e laudo favorável (LEP art. 97 §3º).");
        } else if (input.laudoPericialAltaPericulosidade()) {
            orientacoes.add("Manutenção da internação: laudo pericial indica persistência de periculosidade.");
        } else {
            orientacoes.add(String.format(
                    "Desinternação ainda não cabível: %d ano(s) cumprido(s) de mínimo 1 ano.",
                    input.anosInternacaoDecorridos()));
        }

        orientacoes.add("Perícia anual obrigatória para verificação de cessação de periculosidade (CP art. 97 §2º).");

        return new MedidaSegurancaResult(modalidade, prazoMinimo, prazoMaximo, desinternacaoCabivel, orientacoes);
    }
}
