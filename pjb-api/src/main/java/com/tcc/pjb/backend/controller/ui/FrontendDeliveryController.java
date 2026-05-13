package com.tcc.pjb.backend.controller.ui;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.core.frontend.delivery.application.PjbFrontendDeliveryApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/frontend/delivery")
@PreAuthorize("isAuthenticated()")
public class FrontendDeliveryController {

    private final PjbFrontendDeliveryApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public FrontendDeliveryController(PjbFrontendDeliveryApplicationService applicationService,
                                      ApiResponseFactory apiResponseFactory) {
        this.applicationService = applicationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiQueryResponse<?>> summary() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.summary(), List.of()));
    }

    @GetMapping("/routes")
    public ResponseEntity<ApiQueryResponse<?>> routes() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.routes(), List.of()));
    }

    @GetMapping("/domains")
    public ResponseEntity<ApiQueryResponse<?>> domains() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.domains(), List.of()));
    }

    @GetMapping("/blockers")
    public ResponseEntity<ApiQueryResponse<?>> blockers() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.blockers(), List.of()));
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<ApiQueryResponse<?>> bootstrap() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.bootstrap(), List.of()));
    }
}
