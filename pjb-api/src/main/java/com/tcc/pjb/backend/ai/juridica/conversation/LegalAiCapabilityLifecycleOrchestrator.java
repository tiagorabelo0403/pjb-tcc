package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityCooldownSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecoverySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecurrenceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRehabilitationSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilitySuppressionSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de LegalAiConversationOrchestrator: agrupa os 5 servicos que compoem
 * o ciclo de vida de capability -- recovery -> cooldown -> rehabilitation -> recurrence
 * -> suppression. Cada um consome os anteriores mais o toolScope atual. converse()
 * ainda controla o enrichWith* que executa entre cada inspecao.
 */
@Service
public class LegalAiCapabilityLifecycleOrchestrator {

    private final LegalAiConversationCapabilityRecoveryService recoveryService;
    private final LegalAiConversationCapabilityCooldownService cooldownService;
    private final LegalAiConversationCapabilityRehabilitationService rehabilitationService;
    private final LegalAiConversationCapabilityRecurrenceService recurrenceService;
    private final LegalAiConversationCapabilitySuppressionService suppressionService;

    public LegalAiCapabilityLifecycleOrchestrator(LegalAiConversationCapabilityRecoveryService recoveryService,
                                                   LegalAiConversationCapabilityCooldownService cooldownService,
                                                   LegalAiConversationCapabilityRehabilitationService rehabilitationService,
                                                   LegalAiConversationCapabilityRecurrenceService recurrenceService,
                                                   LegalAiConversationCapabilitySuppressionService suppressionService) {
        this.recoveryService = Objects.requireNonNull(recoveryService);
        this.cooldownService = Objects.requireNonNull(cooldownService);
        this.rehabilitationService = Objects.requireNonNull(rehabilitationService);
        this.recurrenceService = Objects.requireNonNull(recurrenceService);
        this.suppressionService = Objects.requireNonNull(suppressionService);
    }

    public LegalAiConversationCapabilityRecoverySnapshot inspectRecovery(LegalAiConversationRequest request,
                                                                          String capability,
                                                                          String versionName,
                                                                          LegalAiConversationMemorySnapshot memory,
                                                                          LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                          LegalAiConversationToolScopeSnapshot toolScope,
                                                                          LegalAiConversationSessionDoctorSnapshot sessionDoctor,
                                                                          LegalAiConversationSessionBootstrapSnapshot sessionBootstrap) {
        return recoveryService.inspect(request, capability, versionName, memory, documentSecurity, toolScope, sessionDoctor, sessionBootstrap);
    }

    public LegalAiConversationCapabilityCooldownSnapshot inspectCooldown(LegalAiConversationRequest request,
                                                                          String capability,
                                                                          String versionName,
                                                                          LegalAiConversationMemorySnapshot memory,
                                                                          LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                          LegalAiConversationToolScopeSnapshot toolScope,
                                                                          LegalAiConversationSessionDoctorSnapshot sessionDoctor,
                                                                          LegalAiConversationSessionBootstrapSnapshot sessionBootstrap,
                                                                          LegalAiConversationCapabilityRecoverySnapshot capabilityRecovery) {
        return cooldownService.inspect(request, capability, versionName, memory, documentSecurity, toolScope, sessionDoctor, sessionBootstrap, capabilityRecovery);
    }

    public LegalAiConversationCapabilityRehabilitationSnapshot inspectRehabilitation(LegalAiConversationRequest request,
                                                                                      String capability,
                                                                                      String versionName,
                                                                                      LegalAiConversationMemorySnapshot memory,
                                                                                      LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                                      LegalAiConversationToolScopeSnapshot toolScope,
                                                                                      LegalAiConversationSessionDoctorSnapshot sessionDoctor,
                                                                                      LegalAiConversationSessionBootstrapSnapshot sessionBootstrap,
                                                                                      LegalAiConversationCapabilityRecoverySnapshot capabilityRecovery,
                                                                                      LegalAiConversationCapabilityCooldownSnapshot capabilityCooldown) {
        return rehabilitationService.inspect(request, capability, versionName, memory, documentSecurity, toolScope, sessionDoctor, sessionBootstrap, capabilityRecovery, capabilityCooldown);
    }

    public LegalAiConversationCapabilityRecurrenceSnapshot inspectRecurrence(LegalAiConversationRequest request,
                                                                              String capability,
                                                                              String versionName,
                                                                              LegalAiConversationMemorySnapshot memory,
                                                                              LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                              LegalAiConversationToolScopeSnapshot toolScope,
                                                                              LegalAiConversationSessionDoctorSnapshot sessionDoctor,
                                                                              LegalAiConversationSessionBootstrapSnapshot sessionBootstrap,
                                                                              LegalAiConversationCapabilityRecoverySnapshot capabilityRecovery,
                                                                              LegalAiConversationCapabilityCooldownSnapshot capabilityCooldown,
                                                                              LegalAiConversationCapabilityRehabilitationSnapshot capabilityRehabilitation) {
        return recurrenceService.inspect(request, capability, versionName, memory, documentSecurity, toolScope, sessionDoctor, sessionBootstrap, capabilityRecovery, capabilityCooldown, capabilityRehabilitation);
    }

    public LegalAiConversationCapabilitySuppressionSnapshot inspectSuppression(LegalAiConversationRequest request,
                                                                                String capability,
                                                                                String versionName,
                                                                                LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                                LegalAiConversationToolScopeSnapshot toolScope,
                                                                                LegalAiConversationSessionDoctorSnapshot sessionDoctor,
                                                                                LegalAiConversationSessionBootstrapSnapshot sessionBootstrap,
                                                                                LegalAiConversationCapabilityRecurrenceSnapshot capabilityRecurrence) {
        return suppressionService.inspect(request, capability, versionName, documentSecurity, toolScope, sessionDoctor, sessionBootstrap, capabilityRecurrence);
    }
}
