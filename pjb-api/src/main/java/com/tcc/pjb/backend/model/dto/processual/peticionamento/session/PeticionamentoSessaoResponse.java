package com.tcc.pjb.backend.model.dto.processual.peticionamento.session;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.governance.PeticionamentoAutomacaoResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.governance.PeticionamentoGuardrailResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoProtocolPackageResponse;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
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
    @Builder.Default
    private Map<String, Object> workspace = new LinkedHashMap<>();
}
