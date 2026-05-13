package com.tcc.pjb.backend.ai.juridica.api;

import com.tcc.pjb.backend.ai.juridica.conversation.JuridicaLegalAiConversationService;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationResponse;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/ai/legal/conversation", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@PreAuthorize("isAuthenticated()")
public class LegalAiConversationController {

    private final JuridicaLegalAiConversationService conversationService;

    public LegalAiConversationController(JuridicaLegalAiConversationService conversationService) {
        this.conversationService = Objects.requireNonNull(conversationService, "conversationService");
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LegalAiConversationResponse> converse(@Valid @RequestBody LegalAiConversationRequest request) {
        return ResponseEntity.ok(conversationService.converse(request));
    }
}
