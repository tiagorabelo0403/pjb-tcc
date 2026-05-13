package com.tcc.pjb.backend.model.dto.oficial_justica;

import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OficialJusticaPessoaRastreioResponse(
        String mode,
        Instant generatedAt,
        Long processoId,
        Long workItemId,
        String processoNumero,
        String mandadoId,
        Target target,
        ProcessoContext processo,
        Map<String, Object> sinaisReceita,
        Map<String, Object> prontuarioNacional,
        PessoaLocalizacaoResponse localizacao,
        Map<String, Object> heuristicaOperacional,
        List<String> alerts
) {
    public OficialJusticaPessoaRastreioResponse {
        sinaisReceita = sinaisReceita == null ? Map.of() : Map.copyOf(sinaisReceita);
        prontuarioNacional = prontuarioNacional == null ? Map.of() : Map.copyOf(prontuarioNacional);
        heuristicaOperacional = heuristicaOperacional == null ? Map.of() : Map.copyOf(heuristicaOperacional);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    public record Target(
            String polo,
            String nome,
            String cpfMascarado,
            boolean cpfDisponivel,
            String fundamentoConsulta,
            String recomendacao
    ) {
    }

    public record ProcessoContext(
            String status,
            String faseAtual,
            String ramoDireito,
            String classeProcessual,
            String assunto,
            Instant prazoFatalEm,
            String comarca,
            String uf
    ) {
    }
}
