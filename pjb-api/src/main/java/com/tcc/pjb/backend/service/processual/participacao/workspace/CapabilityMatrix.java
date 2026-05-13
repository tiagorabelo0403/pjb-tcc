package com.tcc.pjb.backend.service.processual.participacao.workspace;

import com.tcc.pjb.backend.service.processual.participacao.ActionProfile;
import com.tcc.pjb.backend.service.processual.participacao.ProcessualParticipacaoAtivaSupportUtils;

import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import java.util.List;
import java.util.Optional;

public record CapabilityMatrix(List<String> capacities,
                                List<ActionProfile> actions) {
    public Optional<ActionProfile> findAction(String code) {
        return ProcessualParticipacaoAtivaSupportUtils.findByCode(actions, code);
    }

    public Optional<ActionProfile> closestActionFor(WorkItemType workItemType) {
        if (workItemType == null) {
            return Optional.empty();
        }
        return actions.stream().filter(item -> item.workItemType() == workItemType).findFirst();
    }

    public Optional<ActionProfile> closestActionFor(DocumentoProcessual documento) {
        String token = ProcessualParticipacaoAtivaSupportUtils.normalizeToken(ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(documento.getTitulo(), documento.getNomeOriginal()));
        return actions.stream().filter(item -> token.contains(item.code()) || token.contains(ProcessualParticipacaoAtivaSupportUtils.normalizeToken(item.label()))).findFirst()
                .or(() -> actions.stream().filter(item -> item.workItemType() == WorkItemType.LAUDO && token.contains("LAUDO")).findFirst())
                .or(() -> actions.stream().filter(item -> item.workItemType() == WorkItemType.RECURSO && (token.contains("RECURSO") || token.contains("CONTRARRAZOES"))).findFirst())
                .or(() -> actions.stream().findFirst());
    }
}
