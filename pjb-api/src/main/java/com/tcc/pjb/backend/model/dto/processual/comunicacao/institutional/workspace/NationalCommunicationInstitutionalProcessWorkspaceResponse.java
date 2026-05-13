package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalProcessAuthorityBandResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalProcessQueueSectionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessSeparatorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessVisualLaneResponse;
import java.util.List;

public record NationalCommunicationInstitutionalProcessWorkspaceResponse(
        String profileCode,
        String displayName,
        String panel,
        String processProfile,
        String trustFloor,
        String accentColor,
        String ritoProcessual,
        String faseProcessual,
        String statusProcessual,
        String ramoDireito,
        List<String> tabs,
        List<String> quickFilters,
        List<String> recursosHabilitados,
        List<String> embargosHabilitados,
        List<NationalCommunicationInstitutionalProcessActionResponse> actions,
        List<NationalCommunicationInstitutionalProcessQueueSectionResponse> sections,
        List<NationalCommunicationInstitutionalProcessVisualLaneResponse> visualLanes,
        List<NationalCommunicationInstitutionalProcessAuthorityBandResponse> authorityBands,
        List<NationalCommunicationInstitutionalProcessSeparatorResponse> separators,
        List<String> fundamentos
) {
}