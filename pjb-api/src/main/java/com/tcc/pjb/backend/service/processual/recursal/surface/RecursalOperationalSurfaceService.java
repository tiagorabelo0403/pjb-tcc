package com.tcc.pjb.backend.service.processual.recursal.surface;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalOperationalSurfaceResponse;
import org.springframework.stereotype.Service;

@Service
public class RecursalOperationalSurfaceService {

    private final RecursalOperationalSurfaceProjectionSupport projectionSupport;

    public RecursalOperationalSurfaceService(RecursalOperationalSurfaceProjectionSupport projectionSupport) {
        this.projectionSupport = projectionSupport;
    }

    public RecursalOperationalSurfaceResponse buildOperationalSurface(RecursalAutomationRequest request) {
        return projectionSupport.buildAggregate(request);
    }
}
