package com.tcc.pjb.backend.model.dto.processual.peticionamento.session;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.governance.PeticionamentoAutomacaoResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.governance.PeticionamentoGuardrailResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoProtocolPackageResponse;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
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
public class PeticionamentoSessaoResponse {
    private String modoSolicitado;
    private String modoResolvido;
    private String papelArquitetural;
    private String status;
    private String sessionKey;
    private PeticionamentoAutomacaoResponse automacao;
    private PeticionamentoGuardrailResponse guardrails;
    private LaianePeticaoInicialDraftService.DraftView manualDraft;
    private LaianePeticaoAssistResponse assistiveAnalysis;
    private LaianePeticaoProtocolPackageResponse protocolPackage;
    @Builder.Default
    private List<String> passosSugeridos = new ArrayList<>();
    @Schema(description = "Estado do workspace da sessão — jurisprudência, guardrails, AI verifier e payloads acumulados (Categoria D: estado dinâmico de sessão)")
    @Size(max = 50)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Builder.Default
    private Map<String, Object> workspace = new LinkedHashMap<>();
}
