package com.tcc.pjb.backend.service.processual.recursal.workspace;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshContextRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaResponse;

public record MeshBundle(RecursalPlanningResult plan,
                          RecursalAdmissibilityResponse admissibility,
                          RecursalIaConferenciaResponse aiReview,
                          RecursalMeshContextRequest contextRequest,
                          RecursalMeshSpeciesRequest speciesRequest) {

    public static MeshBundle empty() {
        return new MeshBundle(null, null, null, null, null);
    }
}
