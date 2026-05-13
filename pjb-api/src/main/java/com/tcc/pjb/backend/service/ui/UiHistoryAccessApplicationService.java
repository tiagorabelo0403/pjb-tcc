package com.tcc.pjb.backend.service.ui;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class UiHistoryAccessApplicationService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final PjbAuthorizationService authorizationService;

    public UiHistoryAccessApplicationService(ProcessoRepository processoRepository,
                                             WorkItemRepository workItemRepository,
                                             PjbAuthorizationService authorizationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    public void authorizeWorkItemHistoryIfPresent(Long workItemId) {
        workItemRepository.findById(workItemId)
                .map(WorkItem::getProcesso)
                .ifPresent(authorizationService::requireReadProcesso);
    }

    public void authorizeProcessHistoryIfPresent(Long processoId) {
        processoRepository.findById(processoId)
                .ifPresent(authorizationService::requireReadProcesso);
    }
}
