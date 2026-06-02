package com.tcc.pjb.backend.model.dto.processual.peticionamento.governance;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoEnderecoResponse;
import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionamentoAutomacaoResponse {
    private PeticionamentoEnderecoResponse enderecoAutor;
    private PeticionamentoEnderecoResponse enderecoReu;
    private RepresentacaoProcessualPolicyResponse representacao;
    private String nivelSigiloSugerido;
    @Builder.Default
    private List<String> sigiloRecomendacoes = new ArrayList<>();
    @Builder.Default
    private List<String> automacoesAplicadas = new ArrayList<>();
    @Builder.Default
    private List<String> pendenciasDeterministicas = new ArrayList<>();
    @Schema(description = "Envelope de automação — profile, juizoSugerido, factCount, groundCount, requestCount e métricas de automação (Categoria D)")
    @Size(max = 30)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Builder.Default
    private Map<String, Object> envelope = new LinkedHashMap<>();
}
