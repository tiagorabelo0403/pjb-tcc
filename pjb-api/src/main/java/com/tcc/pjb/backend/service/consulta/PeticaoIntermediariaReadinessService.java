package com.tcc.pjb.backend.service.consulta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PeticaoIntermediariaReadinessService {

    public record PeticaoInput(
            UUID processoId,
            String tipoPeticao,
            boolean processoAtivo,
            boolean advogadoHabilitado,
            boolean procuracaoNosAutos,
            boolean peticaoAssinada,
            boolean documentosAnexados,
            boolean pagamentoCustasRecolhido,
            boolean prazoVigilancia,
            boolean decisaoAbertaParaImpugnacao
    ) {}

    public record PeticaoReadinessResult(
            boolean apta,
            List<String> pendencias,
            String fundamentacao
    ) {}

    public PeticaoReadinessResult avaliar(PeticaoInput input) {
        List<String> pendencias = new ArrayList<>();

        if (!input.processoAtivo()) {
            return new PeticaoReadinessResult(false,
                    List.of("Processo inativo ou arquivado — petição intermediária não admitida."),
                    "Processo sem movimentação ativa.");
        }
        if (!input.advogadoHabilitado()) {
            pendencias.add("Advogado não habilitado nos autos.");
        }
        if (!input.procuracaoNosAutos()) {
            pendencias.add("Procuração não localizada nos autos (art. 105, CPC).");
        }
        if (!input.peticaoAssinada()) {
            pendencias.add("Petição não assinada digitalmente.");
        }
        if (!input.pagamentoCustasRecolhido()) {
            pendencias.add("Custas do ato processual não recolhidas.");
        }
        if (input.decisaoAbertaParaImpugnacao() && !input.prazoVigilancia()) {
            pendencias.add("Prazo para impugnação em curso — verificar tempestividade.");
        }

        boolean apta = pendencias.isEmpty();
        return new PeticaoReadinessResult(apta, List.copyOf(pendencias),
                apta ? "Petição intermediária — apta para protocolo."
                     : "Petição com " + pendencias.size() + " pendência(s).");
    }
}
