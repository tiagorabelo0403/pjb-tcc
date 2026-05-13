package com.tcc.pjb.backend.ai.juridica.v3.core;

import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.ai.common.deeprun.DeepRunBudget;
import com.tcc.pjb.backend.ai.common.deeprun.DeepRunJob;
import com.tcc.pjb.backend.ai.common.deeprun.DeepRunJobType;
import com.tcc.pjb.backend.ai.common.deeprun.DeepRunService;

@Service
public class LegalDeepReasoningService {

    private final DeepRunService deepRunService;

    public LegalDeepReasoningService(DeepRunService deepRunService) {
        this.deepRunService = deepRunService;
    }

    public DeepRunJob start48hJob() {
        return deepRunService.create(DeepRunJobType.LEGAL, DeepRunBudget.default48h());
    }
}
