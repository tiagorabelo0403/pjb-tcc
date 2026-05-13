package com.tcc.pjb.backend.service.ui.surface;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.dto.ui.WcagAaaAuditRequest;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import com.tcc.pjb.backend.service.ui.accessibility.WcagAaaAuditService;
import org.springframework.stereotype.Service;

@Service
public class UiAccessibilitySurfaceFacadeService {

    private final WcagAaaAuditService service;
    private final SurfaceProjectionSupport projectionSupport;

    public UiAccessibilitySurfaceFacadeService(WcagAaaAuditService service,
                                               SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse auditarWcagAaa(WcagAaaAuditRequest request) {
        return projectionSupport.snapshot("ui-wcag-aaa", service.auditar(new WcagAaaAuditService.WcagAaaAuditRequest(
                request.contrastRatio(),
                request.keyboardShortcutCoverage(),
                request.ariaLiveCoverage(),
                request.vLibrasAtivo(),
                request.modoDislexiaAtivo(),
                request.focusAppearanceVisible(),
                request.readingLevelSimplified()
        )));
    }
}
