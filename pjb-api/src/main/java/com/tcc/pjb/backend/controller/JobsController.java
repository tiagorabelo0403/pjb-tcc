package com.tcc.pjb.backend.controller;

import com.tcc.pjb.backend.core.jobs.domain.JobStatus;
import com.tcc.pjb.backend.model.dto.jobs.JobCreateRequest;
import com.tcc.pjb.backend.model.dto.jobs.JobCreateResponse;
import com.tcc.pjb.backend.model.dto.jobs.JobItemResponse;
import com.tcc.pjb.backend.model.dto.jobs.JobResponse;
import com.tcc.pjb.backend.service.jobs.surface.JobsSurfaceFacadeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/jobs")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class JobsController {

    private final JobsSurfaceFacadeService jobsSurfaceFacadeService;

    public JobsController(JobsSurfaceFacadeService jobsSurfaceFacadeService) {
        this.jobsSurfaceFacadeService = Objects.requireNonNull(jobsSurfaceFacadeService);
    }

    @PostMapping
    public ResponseEntity<JobCreateResponse> create(@Valid @RequestBody JobCreateRequest req,
                                                    @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
                                                    @RequestHeader(value = "X-Client-Request-Id", required = false) String clientRequestId) {
        return ResponseEntity.accepted().body(jobsSurfaceFacadeService.create(req, idempotencyKey, clientRequestId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(jobsSurfaceFacadeService.get(id));
    }

    @GetMapping
    public ResponseEntity<Page<JobResponse>> list(@RequestParam(value = "inboxKey", required = false) String inboxKey,
                                                  @RequestParam(value = "status", required = false) JobStatus status,
                                                  @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                                  @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(jobsSurfaceFacadeService.list(inboxKey, status, page, size));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<JobResponse> pause(@PathVariable UUID id,
                                             @RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(jobsSurfaceFacadeService.pause(id, reason));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<JobResponse> resume(@PathVariable UUID id) {
        return ResponseEntity.ok(jobsSurfaceFacadeService.resume(id));
    }

    @PostMapping("/{id}/force-retry")
    public ResponseEntity<JobResponse> forceRetry(@PathVariable UUID id) {
        return ResponseEntity.ok(jobsSurfaceFacadeService.forceRetry(id));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<JobItemResponse>> items(@PathVariable UUID id) {
        return ResponseEntity.ok(jobsSurfaceFacadeService.items(id));
    }
}
