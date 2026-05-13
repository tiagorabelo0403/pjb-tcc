package com.tcc.pjb.backend.service.processual.recursal.surface;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalSpecializedSurfaceResponse;
import org.springframework.stereotype.Service;

@Service
public class RecursalIntelligenceSurfaceService {

    private final RecursalOperationalSurfaceProjectionSupport projectionSupport;

    public RecursalIntelligenceSurfaceService(RecursalOperationalSurfaceProjectionSupport projectionSupport) {
        this.projectionSupport = projectionSupport;
    }

    public RecursalSpecializedSurfaceResponse buildIntelligenceSurface(RecursalAutomationRequest request) {
        return projectionSupport.buildSpecialized(request, RecursalOperationalSurfaceCatalog.INTELLIGENCE);
    }
}
