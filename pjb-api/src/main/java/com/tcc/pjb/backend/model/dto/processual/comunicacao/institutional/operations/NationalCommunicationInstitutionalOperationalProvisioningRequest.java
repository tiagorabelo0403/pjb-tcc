package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import java.util.List;

public record NationalCommunicationInstitutionalOperationalProvisioningRequest(
        Boolean persistExpandedBoxes,
        List<String> fundamentos
) {
}
