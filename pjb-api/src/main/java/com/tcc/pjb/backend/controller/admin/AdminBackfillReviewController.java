package com.tcc.pjb.backend.controller.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminDuplicateClienteResponse;
import com.tcc.pjb.backend.service.admin.surface.AdminBackfillReviewFacadeService;

@RestController
@RequestMapping("/api/v1/admin/backfills/advocacia/clientes/canonicalize-sensitive")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminBackfillReviewController {

    private final AdminBackfillReviewFacadeService facadeService;

    public AdminBackfillReviewController(AdminBackfillReviewFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/duplicates")
    public ResponseEntity<Page<AdminDuplicateClienteResponse>> duplicates(@RequestParam(required = false) Long advogadoId,
                                                                          Pageable pageable) {
        return ResponseEntity.ok(facadeService.duplicates(advogadoId, pageable));
    }
}
