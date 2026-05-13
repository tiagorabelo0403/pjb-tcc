package com.tcc.pjb.backend.controller;

import com.tcc.pjb.backend.model.dto.workitem.WorkItemDoneRequest;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import com.tcc.pjb.backend.service.workitem.surface.WorkItemSurfaceFacadeService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/workitems")
@Validated
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR','ROLE_SERVIDOR_FORUM','ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMIN','ROLE_ADMINISTRADOR','ROLE_MAGISTRADO','ROLE_JUIZ','ROLE_ASSESSOR_JUDICIAL','ROLE_ASSESSOR_DESEMBARGADOR','ROLE_ASSESSOR_MINISTRO')")
public class WorkItemController {

    private final WorkItemSurfaceFacadeService workItemSurfaceFacadeService;

    public WorkItemController(WorkItemSurfaceFacadeService workItemSurfaceFacadeService) {
        this.workItemSurfaceFacadeService = workItemSurfaceFacadeService;
    }

    @GetMapping("/inbox")
    public Page<WorkItemDto> inbox(@RequestParam(defaultValue = "0") @Min(0) int page,
                                   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return workItemSurfaceFacadeService.inbox(page, size);
    }

    @GetMapping("/{id}")
    public WorkItemDto get(@PathVariable @Positive Long id) {
        return workItemSurfaceFacadeService.get(id);
    }

    @PostMapping("/{id}/claim")
    public WorkItemDto claim(@PathVariable @Positive Long id) {
        return workItemSurfaceFacadeService.claim(id);
    }

    @PostMapping("/{id}/done")
    public WorkItemDto done(@PathVariable @Positive Long id, @RequestBody(required = false) WorkItemDoneRequest req) {
        return workItemSurfaceFacadeService.done(id, req != null ? req.observacao() : null);
    }
}
