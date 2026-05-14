package com.tcc.pjb.backend.service.intimacao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IntimacaoPessoalReadinessService {

    public record IntimacaoPessoalInput(
            UUID processoId,
            IntimacaoModalidadeEspecial modalidade,
            boolean enderecoLocalizado,
            boolean oficialJusticaDisponivel,
            boolean mandadoExpedido,
            boolean destinatarioInformado,
            boolean domicilioJudicialCadastrado,
            boolean requerenteIdentificado,
            boolean prazoCompativel
    ) {}

    public record IntimacaoPessoalResult(
            boolean apta,
            List<String> pendencias,
            String fundamentacao
    ) {}

    public IntimacaoPessoalResult avaliar(IntimacaoPessoalInput input) {
        List<String> pendencias = new ArrayList<>();

        if (input.modalidade().ehEletronica() && !input.domicilioJudicialCadastrado()) {
            pendencias.add("Domicílio judicial eletrônico não cadastrado — intimação eletrônica inviável.");
        }
        if (input.modalidade().exigeOficialJustica()) {
            if (!input.enderecoLocalizado()) {
                pendencias.add("Endereço do destinatário não localizado para cumprimento por oficial.");
            }
            if (!input.oficialJusticaDisponivel()) {
                pendencias.add("Oficial de justiça não disponível para diligência.");
            }
            if (!input.mandadoExpedido()) {
                pendencias.add("Mandado de intimação não expedido.");
            }
        }
        if (!input.destinatarioInformado()) {
            pendencias.add("Dados do destinatário da intimação não informados.");
        }
        if (!input.requerenteIdentificado()) {
            pendencias.add("Requerente da intimação não identificado.");
        }
        if (!input.prazoCompativel()) {
            pendencias.add("Prazo para cumprimento da intimação incompatível com a urgência processual.");
        }

        boolean apta = pendencias.isEmpty();
        return new IntimacaoPessoalResult(apta, List.copyOf(pendencias),
                apta ? "Intimação pessoal via " + input.modalidade() + " — apta para expedição."
                     : "Intimação com " + pendencias.size() + " pendência(s).");
    }
}
