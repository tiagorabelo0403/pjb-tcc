package com.tcc.pjb.backend.model.dto.profile;

public record DiligenceInstitutionalMeshReplayResponse(
        DiligenceInstitutionalMeshDispatchResponse originalDispatch,
        DiligenceInstitutionalMeshDispatchResponse replayDispatch
) {
}
