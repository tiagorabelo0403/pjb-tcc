package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalContextSanitizer.LegalConversationSanitizationResult;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalSensitiveActionApprovalService;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationApprovalSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTraceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiApprovalDescriptor;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalAiConversationApprovalService {

    private final LegalSensitiveActionApprovalService sensitiveActionApprovalService;

    public LegalAiConversationApprovalService(LegalSensitiveActionApprovalService sensitiveActionApprovalService) {
        this.sensitiveActionApprovalService = Objects.requireNonNull(sensitiveActionApprovalService, "sensitiveActionApprovalService");
    }

    public LegalAiConversationApprovalSnapshot evaluate(LegalAiConversationRequest request,
                                                        String capability,
                                                        String version,
                                                        LegalAiApprovalDescriptor descriptor,
                                                        List<LegalAiToolDescriptor> tools,
                                                        LegalAiConversationMemorySnapshot memorySnapshot,
                                                        LegalAiConversationTraceSnapshot traceSnapshot,
                                                        LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                        LegalAiConversationToolScopeSnapshot toolScope,
                                                        LegalConversationSanitizationResult sanitization) {
        int retainedTurnCount = memorySnapshot == null || memorySnapshot.retainedTurns() == null ? 0 : memorySnapshot.retainedTurns().size();
        return sensitiveActionApprovalService.evaluate(
                request,
                capability,
                version,
                descriptor,
                traceSnapshot,
                documentSecurity,
                toolScope,
                sanitization,
                retainedTurnCount
        );
    }
}
