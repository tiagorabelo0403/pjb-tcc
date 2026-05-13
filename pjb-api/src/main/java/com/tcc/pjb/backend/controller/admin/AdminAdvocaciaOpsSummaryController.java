package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.admin.AdminAdvocaciaOpsSummaryDto;
import com.tcc.pjb.backend.service.admin.surface.AdminAdvocaciaOpsSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/advocacia/ops")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAdvocaciaOpsSummaryController {

    private final AdminAdvocaciaOpsSurfaceFacadeService facadeService;

    public AdminAdvocaciaOpsSummaryController(AdminAdvocaciaOpsSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminAdvocaciaOpsSummaryDto.OpsSummaryResponse> summary(@RequestParam(required = false) String inboxKey) {
        return ResponseEntity.ok(facadeService.summary(inboxKey));
    }
}
