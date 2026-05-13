package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.admin.backfill.AdminBackfillCanonicalizeSensitiveRequest;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminBackfillKickoffResponse;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminBackfillStatusResponse;
import com.tcc.pjb.backend.service.admin.surface.AdminBackfillFacadeService;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/backfills")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminBackfillController {

    private final AdminBackfillFacadeService facadeService;

    public AdminBackfillController(AdminBackfillFacadeService facadeService) {
        this.facadeService = Objects.requireNonNull(facadeService);
    }

    @PostMapping("/advocacia/clientes/canonicalize-sensitive")
    public ResponseEntity<AdminBackfillKickoffResponse> kickoffCanonicalizeAdvClientes(
            @Valid @RequestBody AdminBackfillCanonicalizeSensitiveRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Client-Request-Id", required = false) String clientRequestId
    ) {
        return ResponseEntity.accepted().body(facadeService.kickoffCanonicalizeSensitive(request, idempotencyKey, clientRequestId));
    }

    @GetMapping("/advocacia/clientes/canonicalize-sensitive/status")
    public ResponseEntity<AdminBackfillStatusResponse> canonicalizeStatus(
            @RequestParam(value = "jobId", required = false) UUID jobId,
            @RequestParam(value = "inboxKey", required = false) String inboxKey
    ) {
        return facadeService.canonicalizeStatus(jobId, inboxKey)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
