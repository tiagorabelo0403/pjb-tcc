package com.tcc.pjb.backend.service.integration.judicial.surface;

import com.tcc.pjb.backend.integration.judicial.JudicialConnectorAdminOperationRequest;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorAdminOpsService;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorOpsSurfaceFacadeService {

    private final JudicialConnectorAdminOpsService adminOpsService;
    private final SurfaceProjectionSupport projectionSupport;

    public JudicialConnectorOpsSurfaceFacadeService(JudicialConnectorAdminOpsService adminOpsService,
                                                    SurfaceProjectionSupport projectionSupport) {
        this.adminOpsService = Objects.requireNonNull(adminOpsService);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    public SurfaceCollectionResponse history() {
        return projectionSupport.collection("admin.judicial-connector.ops.history", adminOpsService.recentOperations());
    }

    public SurfaceActionResponse execute(JudicialConnectorAdminOperationRequest request) {
        return projectionSupport.action("admin.judicial-connector.ops", "execute", null, adminOpsService.execute(request).toMap());
    }
}
