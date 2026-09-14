package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de LegalAiConversationOrchestrator: pipeline de inspecao de sessao --
 * sessionDoctor (diagnostica) seguido de sessionBootstrap (prepara). converse() ainda
 * controla o enrichWith que roda entre eles.
 */
@Service
public class LegalAiConversationSessionInspectionOrchestrator {

    private final LegalAiConversationSessionDoctorService sessionDoctorService;
    private final LegalAiConversationSessionBootstrapService sessionBootstrapService;

    public LegalAiConversationSessionInspectionOrchestrator(LegalAiConversationSessionDoctorService sessionDoctorService,
                                                             LegalAiConversationSessionBootstrapService sessionBootstrapService) {
        this.sessionDoctorService = Objects.requireNonNull(sessionDoctorService);
        this.sessionBootstrapService = Objects.requireNonNull(sessionBootstrapService);
    }

    public LegalAiConversationSessionDoctorSnapshot inspectDoctor(LegalAiConversationRequest request,
                                                                   String capability,
                                                                   String versionName,
                                                                   LegalAiConversationMemorySnapshot memory,
                                                                   LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                   LegalAiConversationToolScopeSnapshot toolScope,
                                                                   LegalValidationResponse validation,
                                                                   LegalHallucinationGuardResponse guard) {
        return sessionDoctorService.inspect(request, capability, versionName, memory, documentSecurity, toolScope, validation, guard);
    }

    public LegalAiConversationSessionBootstrapSnapshot inspectBootstrap(LegalAiConversationRequest request,
                                                                         String capability,
                                                                         String versionName,
                                                                         LegalAiConversationMemorySnapshot memory,
                                                                         LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                         LegalAiConversationToolScopeSnapshot toolScope,
                                                                         LegalAiConversationSessionDoctorSnapshot sessionDoctor) {
        return sessionBootstrapService.inspect(request, capability, versionName, memory, documentSecurity, toolScope, sessionDoctor);
    }
}
