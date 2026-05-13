package com.tcc.pjb.backend.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.core.quality.roadmap.application.PjbRoadmapClosureApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/roadmap/closure")
@PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
public class AdminRoadmapClosureController {

    private final PjbRoadmapClosureApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminRoadmapClosureController(PjbRoadmapClosureApplicationService applicationService,
                                         ApiResponseFactory apiResponseFactory) {
        this.applicationService = applicationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiQueryResponse<?>> summary() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.summary(), List.of()));
    }

    @GetMapping("/macroblocks")
    public ResponseEntity<ApiQueryResponse<?>> macroblocks() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.macroblocks(), List.of()));
    }

    @GetMapping("/blockers")
    public ResponseEntity<ApiQueryResponse<?>> blockers() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.blockers(), List.of()));
    }

    @GetMapping("/quality")
    public ResponseEntity<ApiQueryResponse<?>> quality() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.quality(), List.of()));
    }
}
