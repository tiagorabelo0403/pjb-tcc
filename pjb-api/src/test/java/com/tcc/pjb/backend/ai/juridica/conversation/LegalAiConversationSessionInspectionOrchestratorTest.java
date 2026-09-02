package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import org.junit.jupiter.api.Test;

class LegalAiConversationSessionInspectionOrchestratorTest {

    private final LegalAiConversationSessionDoctorService doctorService = mock(LegalAiConversationSessionDoctorService.class);
    private final LegalAiConversationSessionBootstrapService bootstrapService = mock(LegalAiConversationSessionBootstrapService.class);
    private final LegalAiConversationSessionInspectionOrchestrator orchestrator = new LegalAiConversationSessionInspectionOrchestrator(doctorService, bootstrapService);

    @Test
    void inspectDoctorDelegaComOs8Argumentos() {
        var request = mock(LegalAiConversationRequest.class);
        var memory = mock(LegalAiConversationMemorySnapshot.class);
        var docSec = mock(LegalAiConversationDocumentSecuritySnapshot.class);
        var toolScope = mock(LegalAiConversationToolScopeSnapshot.class);
        var validation = mock(LegalValidationResponse.class);
        var guard = mock(LegalHallucinationGuardResponse.class);
        var expected = mock(LegalAiConversationSessionDoctorSnapshot.class);
        when(doctorService.inspect(request, "CAP", "V1", memory, docSec, toolScope, validation, guard)).thenReturn(expected);

        assertThat(orchestrator.inspectDoctor(request, "CAP", "V1", memory, docSec, toolScope, validation, guard)).isSameAs(expected);
    }

    @Test
    void inspectBootstrapDelegaComOs7Argumentos() {
        var request = mock(LegalAiConversationRequest.class);
        var memory = mock(LegalAiConversationMemorySnapshot.class);
        var docSec = mock(LegalAiConversationDocumentSecuritySnapshot.class);
        var toolScope = mock(LegalAiConversationToolScopeSnapshot.class);
        var doctor = mock(LegalAiConversationSessionDoctorSnapshot.class);
        var expected = mock(LegalAiConversationSessionBootstrapSnapshot.class);
        when(bootstrapService.inspect(request, "CAP", "V1", memory, docSec, toolScope, doctor)).thenReturn(expected);

        assertThat(orchestrator.inspectBootstrap(request, "CAP", "V1", memory, docSec, toolScope, doctor)).isSameAs(expected);
    }
}
