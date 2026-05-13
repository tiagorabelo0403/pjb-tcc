package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PjbJuizadoAdjuntoPjeProtocolBridge {

    private final PjbJuizadoAdjuntoNucleoOptionService optionService;

    public PjbJuizadoAdjuntoPjeProtocolBridge() {
        this(new PjbJuizadoAdjuntoNucleoOptionService());
    }

    public PjbJuizadoAdjuntoPjeProtocolBridge(PjbJuizadoAdjuntoNucleoOptionService optionService) {
        this.optionService = optionService;
    }

    public Map<String, Object> materializeMoradaNovaProtocolContext(LocalDate protocolDate,
                                                                     boolean optionSelectedInPjeRegistration,
                                                                     boolean optionOnlyMentionedInInitialPetition,
                                                                     boolean distributionCompleted) {
        PjbJuizadoAdjuntoNucleoOptionDecision decision = optionService.moradaNovaPreview(
                protocolDate,
                optionSelectedInPjeRegistration,
                optionOnlyMentionedInInitialPetition,
                distributionCompleted);
        PjbJuizadoAdjuntoPublicGuidance guidance = PjbJuizadoAdjuntoPublicGuidance.moradaNova();
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("tribunal", guidance.tribunal());
        payload.put("comarca", guidance.comarca());
        payload.put("nucleo", guidance.nucleoName());
        payload.put("inicioFuncionamento", guidance.startsAt().toString());
        payload.put("sistemaTramitacao", guidance.systemOfRecord());
        payload.put("statusRoteamento", decision.status());
        payload.put("destino", decision.targetLane());
        payload.put("etapa", decision.stageCode());
        payload.put("opcaoFacultativa", guidance.optionalElection());
        payload.put("opcaoSomenteNoProtocolo", guidance.electionAtProtocolOnly());
        payload.put("mencaoPeticaoInsuficiente", guidance.petitionMentionIsInsufficient());
        payload.put("imutavelAposDistribuicao", guidance.immutableAfterDistribution());
        payload.put("semRedistribuicaoAutomatica", guidance.noAutomaticRedistribution());
        payload.put("escolhaParteAutoraRespeitada", decision.authorChoiceRespected());
        payload.put("motivos", decision.reasons());
        payload.put("alertas", decision.warnings());
        payload.put("beneficios", guidance.benefits());
        payload.put("baseLegal", decision.legalBasis());
        payload.put("canalApoio", guidance.supportChannel());
        return Map.copyOf(payload);
    }
}
