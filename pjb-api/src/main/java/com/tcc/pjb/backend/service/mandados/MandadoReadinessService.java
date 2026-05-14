package com.tcc.pjb.backend.service.mandados;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MandadoReadinessService {

    public record MandadoInput(
            UUID mandadoId,
            String processoNumero,
            boolean enderecoConfirmado,
            boolean parteLocalizada,
            boolean tipoMandadoDefinido,
            boolean conteudoRedigido,
            boolean assinadoJuiz,
            boolean prazoDefinido
    ) {}

    public record MandadoReadiness(
            UUID mandadoId,
            boolean apto,
            List<String> pendencias,
            String mensagem
    ) {}

    public MandadoReadiness avaliar(MandadoInput input) {
        List<String> pendencias = new ArrayList<>();
        if (!input.enderecoConfirmado()) pendencias.add("Endereço da parte não confirmado — risco de devolução.");
        if (!input.parteLocalizada()) pendencias.add("Parte não localizada — investigar antes de expedir.");
        if (!input.tipoMandadoDefinido()) pendencias.add("Tipo de mandado não definido.");
        if (!input.conteudoRedigido()) pendencias.add("Conteúdo do mandado não redigido.");
        if (!input.assinadoJuiz()) pendencias.add("Mandado sem assinatura do juiz.");
        if (!input.prazoDefinido()) pendencias.add("Prazo para cumprimento não definido.");

        boolean apto = pendencias.isEmpty();
        String mensagem = apto
                ? "Mandado apto para expedição."
                : pendencias.size() + " pendência(s) antes da expedição.";

        return new MandadoReadiness(input.mandadoId(), apto, pendencias, mensagem);
    }
}
