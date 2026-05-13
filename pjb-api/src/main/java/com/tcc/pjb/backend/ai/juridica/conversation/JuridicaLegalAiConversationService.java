package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JuridicaLegalAiConversationService {

    private final LegalAiConversationOrchestrator orchestrator;

    public JuridicaLegalAiConversationService(LegalAiConversationOrchestrator orchestrator) {
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
    }

    public LegalAiConversationResponse converse(LegalAiConversationRequest request) {
        return orchestrator.converse(request);
    }
}
