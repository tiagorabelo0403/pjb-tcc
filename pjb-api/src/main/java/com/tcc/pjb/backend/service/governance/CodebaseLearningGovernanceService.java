package com.tcc.pjb.backend.service.governance;

import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseLearningApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.presentation.PjbCodebaseLearningResponseMapper;
import com.tcc.pjb.backend.model.dto.governance.CodebaseLearningResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CodebaseLearningGovernanceService {

    private final PjbCodebaseLearningApplicationService applicationService;

    public CodebaseLearningGovernanceService(PjbCodebaseLearningApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public CodebaseLearningResponse report() {
        return report(false);
    }

    public CodebaseLearningResponse report(boolean forceRefresh) {
        return PjbCodebaseLearningResponseMapper.toGovernance(applicationService.aprender(forceRefresh));
    }
}
