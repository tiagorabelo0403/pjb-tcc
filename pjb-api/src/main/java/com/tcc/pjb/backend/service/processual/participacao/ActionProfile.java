package com.tcc.pjb.backend.service.processual.participacao;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import java.util.List;

public record ActionProfile(String code,
                             String label,
                             WorkItemType workItemType,
                             int defaultPriority,
                             boolean blocking,
                             boolean highSensitivity,
                             List<FaseProcessual> phases,
                             List<String> tags,
                             List<String> innovations,
                             List<String> checklist,
                             String signatureMode) {
    public ActionProfile withSignature(String signatureMode) {
        return new ActionProfile(code, label, workItemType, defaultPriority, blocking, highSensitivity, phases, tags, innovations, checklist, signatureMode);
    }
}
