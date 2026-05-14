package com.tcc.pjb.backend.service.conciliation;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AcordoFastTrackService {

    public record FastTrackInput(
            UUID processoId,
            RitoProcessual rito,
            boolean propostaAceitaPorAmbas,
            boolean valorLiquidado,
            boolean advogadoAmbosLadosPresente,
            boolean procuracaoComPoderesParaAcordo
    ) {}

    public record FastTrackResultado(
            UUID processoId,
            boolean aptoParaHomologacaoImediata,
            List<String> pendencias,
            String mensagem
    ) {}

    public FastTrackResultado avaliar(FastTrackInput input) {
        List<String> pendencias = new ArrayList<>();
        if (!input.propostaAceitaPorAmbas()) pendencias.add("Proposta não aceita por ambas as partes.");
        if (!input.valorLiquidado()) pendencias.add("Valor do acordo não liquidado — definir antes de homologar.");
        if (!input.advogadoAmbosLadosPresente()) pendencias.add("Advogado ausente em alguma das partes.");
        if (!input.procuracaoComPoderesParaAcordo()) pendencias.add("Procuração sem poderes expressos para transigir.");

        boolean apto = pendencias.isEmpty();
        String mensagem = apto
                ? "Acordo apto para homologação imediata — prosseguir com termo."
                : pendencias.size() + " pendência(s) antes da homologação.";

        return new FastTrackResultado(input.processoId(), apto, pendencias, mensagem);
    }
}
