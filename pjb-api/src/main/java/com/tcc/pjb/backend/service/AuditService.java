package com.tcc.pjb.backend.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.EssenceResult;
import com.tcc.pjb.backend.model.dto.IARunResult;
import com.tcc.pjb.backend.model.dto.SavedAudit;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    public void recordIARun(UUID processId, IARunResult runResult, EssenceResult decision) {
        logger.info("Audit: IA Run registrada | ProcessID={} | Decision={} | ResultPresent={}",
                processId,
                decision,
                runResult != null
        );
    }

    public void recordIADecision(UUID processId, Long auditId, boolean approved, String justification) {
        logger.info("Audit: Decisão IA | ProcessID={} | AuditID={} | Approved={} | JustificationLength={}",
                processId,
                auditId,
                approved,
                justification != null ? justification.length() : 0
        );
    }

    public void recordProcessJuntada(UUID processId, String provider, String hash) {
        logger.info("Audit: Juntada registrada | ProcessID={} | Provider={} | HashPresent={}",
                processId,
                provider,
                hash != null
        );
    }

    public void recordPetitionSubmission(UUID processId, String provider, String hash) {
        logger.info("Audit: Petição enviada | ProcessID={} | Provider={} | HashPresent={}",
                processId,
                provider,
                hash != null
        );
    }

    public void recordAgreementHomologation(UUID processId, SavedAudit auditData) {
        logger.info("Audit: Homologação registrada | ProcessID={} | SavedAuditPresent={}",
                processId,
                auditData != null
        );
    }
}
