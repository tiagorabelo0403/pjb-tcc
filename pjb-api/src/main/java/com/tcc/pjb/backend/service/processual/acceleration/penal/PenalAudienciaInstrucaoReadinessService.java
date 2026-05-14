package com.tcc.pjb.backend.service.processual.acceleration.penal;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PenalAudienciaInstrucaoReadinessService {

    public record ReadinessInput(
            boolean acusacaoRecebida,
            boolean reuCitado,
            boolean defesaPrelimiinarApresentada,
            boolean laudoPericialJuntado,
            boolean testemunhasArroladas,
            boolean intimacaoTestemunhasExpedida,
            boolean reuPresoPodeSairParaAudiencia
    ) {}

    public record ReadinessSnapshot(
            List<String> pendencias,
            boolean apto,
            String mensagem
    ) {}

    public ReadinessSnapshot avaliar(ReadinessInput input) {
        List<String> pendencias = new ArrayList<>();
        if (!input.acusacaoRecebida()) pendencias.add("Acusação ainda não recebida pelo juiz.");
        if (!input.reuCitado()) pendencias.add("Réu não citado.");
        if (!input.defesaPrelimiinarApresentada()) pendencias.add("Defesa preliminar não apresentada.");
        if (!input.laudoPericialJuntado()) pendencias.add("Laudo pericial pendente.");
        if (!input.intimacaoTestemunhasExpedida() && input.testemunhasArroladas())
            pendencias.add("Intimação de testemunhas não expedida.");
        if (input.reuPresoPodeSairParaAudiencia() == false && input.reuCitado())
            pendencias.add("Réu preso sem autorização de saída — requisitar ao presídio.");

        boolean apto = pendencias.isEmpty();
        String mensagem = apto
                ? "Processo apto para audiência de instrução e julgamento."
                : pendencias.size() + " pendência(s) antes da audiência.";

        return new ReadinessSnapshot(pendencias, apto, mensagem);
    }
}
