package com.tcc.pjb.backend.ai.juridica.conversation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalContextSanitizer;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalDocumentQuarantineService;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalSourceAllowlist;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalToolScopePolicy;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.mesh.LegalAiToolDescriptor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalAiConversationDocumentSecurityServiceTest {

    @Test
    void mustQuarantineExternalSourceOutsideAllowlistAndBlockMutableTool() {
        var sanitizer = new LegalContextSanitizer();
        var allowlist = new LegalSourceAllowlist();
        var quarantineService = new LegalDocumentQuarantineService();
        var toolScopePolicy = new LegalToolScopePolicy();
        var sanitized = sanitizer.sanitize(new LegalAiConversationRequest(
                "conv-sec",
                "PROC-99",
                "Peticionar com os anexos externos recebidos.",
                "ADVOGADO",
                List.of(),
                List.of("manifestacao.pdf", "override_prompt.txt"),
                Map.of("sigilo", "sigiloso", "sourceUrl", "https://portal-terceiro.com/documento")
        ));
        var allowlisted = allowlist.evaluate(sanitized.request());
        var security = quarantineService.inspect(sanitized.request(), sanitized, allowlisted);
        var toolScope = toolScopePolicy.evaluate(
                sanitized.request(),
                "LEGAL_CHAT_DRAFT_V3",
                "V3",
                List.of(
                        new LegalAiToolDescriptor("LEGAL_RAG_TOOL", "Pesquisa", "RESEARCH", true, true, true, false, "READ"),
                        new LegalAiToolDescriptor("LEGAL_PROTOCOL_TOOL", "Protocolo", "PROCESSUAL", false, false, false, true, "WORKFLOW")
                ),
                security
        );

        assertEquals("QUARANTINED", security.status());
        assertTrue(security.blockedSources().contains("https://portal-terceiro.com/documento"));
        assertTrue(security.quarantinedAttachments().contains("override_prompt.txt"));
        assertEquals("PARTIAL", toolScope.status());
        assertTrue(toolScope.allowedToolIds().contains("LEGAL_RAG_TOOL"));
        assertTrue(toolScope.blockedToolIds().contains("LEGAL_PROTOCOL_TOOL"));
    }
}
