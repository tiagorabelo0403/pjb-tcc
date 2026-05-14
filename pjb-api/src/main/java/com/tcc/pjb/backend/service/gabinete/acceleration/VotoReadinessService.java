package com.tcc.pjb.backend.service.gabinete.acceleration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class VotoReadinessService {

    public record VotoInput(
            UUID processoId,
            boolean relatorioPronto,
            boolean fundamentacaoRedigida,
            boolean dispositivoDefinido,
            boolean revisorCiente,
            boolean sessaoDesignada
    ) {}

    public record VotoReadiness(
            UUID processoId,
            boolean apto,
            List<String> pendencias,
            String mensagem
    ) {}

    public VotoReadiness avaliar(VotoInput input) {
        List<String> pendencias = new ArrayList<>();
        if (!input.relatorioPronto()) pendencias.add("Relatório do voto não finalizado.");
        if (!input.fundamentacaoRedigida()) pendencias.add("Fundamentação não redigida.");
        if (!input.dispositivoDefinido()) pendencias.add("Dispositivo do voto não definido.");
        if (!input.revisorCiente()) pendencias.add("Revisor não ciente da inclusão em pauta.");
        if (!input.sessaoDesignada()) pendencias.add("Sessão de julgamento não designada.");

        boolean apto = pendencias.isEmpty();
        String mensagem = apto
                ? "Voto apto para inclusão em pauta de julgamento colegiado."
                : pendencias.size() + " pendência(s) antes da inclusão em pauta.";

        return new VotoReadiness(input.processoId(), apto, pendencias, mensagem);
    }
}
