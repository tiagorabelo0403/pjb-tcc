package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalManagedUnitResponse;
import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalUnitGovernanceResponse(
        String snapshotId,
        String affiliationId,
        String orgaoSigla,
        String orgaoNome,
        String organizationScope,
        String status,
        int totalUnits,
        int totalBoxes,
        int totalLotacoes,
        List<NationalCommunicationInstitutionalManagedUnitResponse> units,
        List<NationalCommunicationInstitutionalLotationGovernanceResponse> lotacoes,
        List<String> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}