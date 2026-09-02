package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;

class LegalAiCapabilityLifecycleOrchestratorTest {

    private final LegalAiConversationCapabilityRecoveryService recoveryService = mock(LegalAiConversationCapabilityRecoveryService.class);
    private final LegalAiConversationCapabilityCooldownService cooldownService = mock(LegalAiConversationCapabilityCooldownService.class);
    private final LegalAiConversationCapabilityRehabilitationService rehabilitationService = mock(LegalAiConversationCapabilityRehabilitationService.class);
    private final LegalAiConversationCapabilityRecurrenceService recurrenceService = mock(LegalAiConversationCapabilityRecurrenceService.class);
    private final LegalAiConversationCapabilitySuppressionService suppressionService = mock(LegalAiConversationCapabilitySuppressionService.class);
    private final LegalAiCapabilityLifecycleOrchestrator orchestrator = new LegalAiCapabilityLifecycleOrchestrator(
            recoveryService, cooldownService, rehabilitationService, recurrenceService, suppressionService);

    private final LegalAiConversationRequest request = mock(LegalAiConversationRequest.class);
    private final LegalAiConversationMemorySnapshot memory = mock(LegalAiConversationMemorySnapshot.class);
    private final LegalAiConversationDocumentSecuritySnapshot docSec = mock(LegalAiConversationDocumentSecuritySnapshot.class);
    private final LegalAiConversationToolScopeSnapshot toolScope = mock(LegalAiConversationToolScopeSnapshot.class);
    private final LegalAiConversationSessionDoctorSnapshot doctor = mock(LegalAiConversationSessionDoctorSnapshot.class);
    private final LegalAiConversationSessionBootstrapSnapshot bootstrap = mock(LegalAiConversationSessionBootstrapSnapshot.class);

    @Test
    void inspectRecoveryDelega() {
        var expected = mock(LegalAiConversationCapabilityRecoverySnapshot.class);
        when(recoveryService.inspect(request, "CAP", "V1", memory, docSec, toolScope, doctor, bootstrap)).thenReturn(expected);

        assertThat(orchestrator.inspectRecovery(request, "CAP", "V1", memory, docSec, toolScope, doctor, bootstrap)).isSameAs(expected);
    }

    @Test
    void inspectCooldownDelegaComRecoveryComoInput() {
        var recovery = mock(LegalAiConversationCapabilityRecoverySnapshot.class);
        var expected = mock(LegalAiConversationCapabilityCooldownSnapshot.class);
        when(cooldownService.inspect(request, "CAP", "V1", memory, docSec, toolScope, doctor, bootstrap, recovery)).thenReturn(expected);

        assertThat(orchestrator.inspectCooldown(request, "CAP", "V1", memory, docSec, toolScope, doctor, bootstrap, recovery)).isSameAs(expected);
    }

    @Test
    void inspectRehabilitationDelegaComRecoveryECooldown() {
        var recovery = mock(LegalAiConversationCapabilityRecoverySnapshot.class);
        var cooldown = mock(LegalAiConversationCapabilityCooldownSnapshot.class);
        var expected = mock(LegalAiConversationCapabilityRehabilitationSnapshot.class);
        when(rehabilitationService.inspect(request, "CAP", "V1", memory, docSec, toolScope, doctor, bootstrap, recovery, cooldown)).thenReturn(expected);

        assertThat(orchestrator.inspectRehabilitation(request, "CAP", "V1", memory, docSec, toolScope, doctor, bootstrap, recovery, cooldown)).isSameAs(expected);
    }

    @Test
    void inspectRecurrenceDelegaComOs11Argumentos() {
        var recovery = mock(LegalAiConversationCapabilityRecoverySnapshot.class);
        var cooldown = mock(LegalAiConversationCapabilityCooldownSnapshot.class);
        var rehab = mock(LegalAiConversationCapabilityRehabilitationSnapshot.class);
        var expected = mock(LegalAiConversationCapabilityRecurrenceSnapshot.class);
        when(recurrenceService.inspect(request, "CAP", "V1", memory, docSec, toolScope, doctor, bootstrap, recovery, cooldown, rehab)).thenReturn(expected);

        assertThat(orchestrator.inspectRecurrence(request, "CAP", "V1", memory, docSec, toolScope, doctor, bootstrap, recovery, cooldown, rehab)).isSameAs(expected);
    }

    @Test
    void inspectSuppressionNaoUsaMemoryENaoTemRecoveryOuCooldownOuRehab() {
        var recurrence = mock(LegalAiConversationCapabilityRecurrenceSnapshot.class);
        var expected = mock(LegalAiConversationCapabilitySuppressionSnapshot.class);
        when(suppressionService.inspect(request, "CAP", "V1", docSec, toolScope, doctor, bootstrap, recurrence)).thenReturn(expected);

        assertThat(orchestrator.inspectSuppression(request, "CAP", "V1", docSec, toolScope, doctor, bootstrap, recurrence)).isSameAs(expected);
    }
}
