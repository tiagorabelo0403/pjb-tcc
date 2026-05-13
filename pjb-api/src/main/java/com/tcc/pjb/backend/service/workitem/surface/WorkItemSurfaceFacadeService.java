package com.tcc.pjb.backend.service.workitem.surface;

import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import com.tcc.pjb.backend.service.workitem.WorkItemService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class WorkItemSurfaceFacadeService {

    private final WorkItemService workItemService;

    public WorkItemSurfaceFacadeService(WorkItemService workItemService) {
        this.workItemService = workItemService;
    }

    public Page<WorkItemDto> inbox(int page, int size) {
        return workItemService.inbox(page, size);
    }

    public WorkItemDto get(Long id) {
        return workItemService.get(id);
    }

    public WorkItemDto claim(Long id) {
        return workItemService.claim(id);
    }

    public WorkItemDto done(Long id, String observacao) {
        return workItemService.done(id, observacao);
    }
}
