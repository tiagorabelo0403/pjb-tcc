package com.tcc.pjb.backend.model.dto.oficial_justica;

import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record OficialJusticaPessoaRastreioResponse(
        String mode,
        Instant generatedAt,
        Long processoId,
        Long workItemId,
        String processoNumero,
        String mandadoId,
        Target target,
        ProcessoContext processo,
        @Schema(description = "Sinais da Receita Federal para rastreio — estrutura definida pelo sistema externo", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> sinaisReceita,
        @Schema(description = "Prontuario nacional do rastreado — dados provenientes de sistemas federais", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> prontuarioNacional,
        PessoaLocalizacaoResponse localizacao,
        @Schema(description = "Heuristica operacional de localizacao — parametros variam por tipo de pessoa", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
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

