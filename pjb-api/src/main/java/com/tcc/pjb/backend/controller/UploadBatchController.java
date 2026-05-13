package com.tcc.pjb.backend.controller;

import com.tcc.pjb.backend.model.dto.jobs.JobCreateResponse;
import com.tcc.pjb.backend.model.dto.upload.UploadBatchCreateRequest;
import com.tcc.pjb.backend.model.dto.upload.UploadBatchCreateResponse;
import com.tcc.pjb.backend.model.dto.upload.UploadIngressResponse;
import com.tcc.pjb.backend.model.dto.upload.UploadItemReserveRequest;
import com.tcc.pjb.backend.model.dto.upload.UploadItemReserveResponse;
import com.tcc.pjb.backend.service.upload.surface.UploadBatchSurfaceFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Validated
@RequestMapping("/api/v1/uploads")
@PreAuthorize("isAuthenticated()")
public class UploadBatchController {

    private final UploadBatchSurfaceFacadeService facadeService;

    public UploadBatchController(UploadBatchSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @PostMapping("/batches")
    public ResponseEntity<UploadBatchCreateResponse> createBatch(@Valid @RequestBody UploadBatchCreateRequest req) {
        return ResponseEntity.ok(facadeService.createBatch(req));
    }

    @PostMapping("/batches/{batchId}/items")
    public ResponseEntity<UploadItemReserveResponse> reserveItem(@PathVariable UUID batchId,
                                                                 @Valid @RequestBody UploadItemReserveRequest req) {
        return ResponseEntity.ok(facadeService.reserveItem(batchId, req));
    }

    @PutMapping(value = "/direct/{batchId}/{itemId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<UploadIngressResponse> directUpload(@PathVariable UUID batchId,
                                                              @PathVariable UUID itemId,
                                                              @RequestParam("token") String token,
                                                              HttpServletRequest request) throws Exception {
        return ResponseEntity.ok(facadeService.directUpload(batchId, itemId, token, request));
    }

    @PostMapping("/batches/{batchId}/finalize")
    public ResponseEntity<JobCreateResponse> finalizeBatch(@PathVariable UUID batchId,
                                                           @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                                           @RequestHeader(value = "X-Client-Request-Id", required = false) String clientRequestId) {
        return ResponseEntity.accepted().body(facadeService.finalizeBatch(batchId, idempotencyKey, clientRequestId));
    }
}
