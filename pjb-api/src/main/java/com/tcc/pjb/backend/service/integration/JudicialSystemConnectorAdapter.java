package com.tcc.pjb.backend.service.integration;

import com.tcc.pjb.backend.service.secretariat.ingest.SistemaProcessualOrigem;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class JudicialSystemConnectorAdapter {

    public record ConectorInput(
            SistemaProcessualOrigem sistema,
            String endpoint,
            Map<String, String> cabecalhos,
            Object payload
    ) {}

    public record ConectorResponse(
            boolean sucesso,
            int statusHttp,
            Object body,
            String mensagem
    ) {}

    public List<SistemaProcessualOrigem> sistemasSuportados() {
        return List.of(
                SistemaProcessualOrigem.MNI,
                SistemaProcessualOrigem.PDPJ,
                SistemaProcessualOrigem.PJE,
                SistemaProcessualOrigem.PJE_2X);
    }

    public ConectorResponse conectar(ConectorInput input) {
        if (!sistemasSuportados().contains(input.sistema())) {
            return new ConectorResponse(false, 400, null,
                    "Sistema " + input.sistema().name() + " não suportado para integração direta.");
        }
        return new ConectorResponse(false, 501, null,
                "Conector " + input.sistema().name() + " — implementação real via HTTP client externo.");
    }
}
