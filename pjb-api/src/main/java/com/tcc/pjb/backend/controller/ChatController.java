package com.tcc.pjb.backend.controller;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.ChatMensagemRequest;
import com.tcc.pjb.backend.model.dto.ChatMensagemResponse;
import com.tcc.pjb.backend.model.dto.acordo.ChatAcordoAbrirSalaRequest;
import com.tcc.pjb.backend.model.dto.acordo.ChatAcordoConvidarParticipanteRequest;
import com.tcc.pjb.backend.model.dto.acordo.ChatAcordoSalaResponse;
import com.tcc.pjb.backend.model.dto.intelligence.AgreementChatAttachmentRequest;
import com.tcc.pjb.backend.model.dto.intelligence.AgreementChatContextResponse;
import com.tcc.pjb.backend.service.ChatService;
import com.tcc.pjb.backend.service.intelligence.AgreementChatContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class ChatController {

    private final ChatService chatService;
    private final AgreementChatContextService agreementChatContextService;

    
    @PostMapping
    public ResponseEntity<ChatMensagemResponse> enviarMensagem(
            @Valid @RequestBody ChatMensagemRequest dto
    ) {
        ChatMensagemResponse response = chatService.enviarMensagem(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    
    @GetMapping("/processo/{processoId}")
    public ResponseEntity<List<ChatMensagemResponse>> buscarHistorico(
            @PathVariable @Positive Long processoId
    ) {
        List<ChatMensagemResponse> historico = chatService.buscarHistoricoDoProcesso(processoId);
        return ResponseEntity.ok(historico);
    }

    @GetMapping("/processo/{processoId}/acordo-contexto")
    public ResponseEntity<AgreementChatContextResponse> buscarContextoAcordo(
            @PathVariable @Positive Long processoId
    ) {
        return ResponseEntity.ok(agreementChatContextService.analyze(processoId));
    }

    @GetMapping("/processo/{processoId}/acordo/sala")
    public ResponseEntity<ChatAcordoSalaResponse> buscarSalaAcordo(
            @PathVariable @Positive Long processoId
    ) {
        return ResponseEntity.ok(chatService.obterSalaAcordoDoProcesso(processoId));
    }

    @PostMapping("/processo/{processoId}/acordo/sala")
    public ResponseEntity<ChatAcordoSalaResponse> abrirSalaAcordo(
            @PathVariable @Positive Long processoId,
            @RequestBody(required = false) ChatAcordoAbrirSalaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.abrirSalaAcordo(processoId, request));
    }

    @PostMapping("/acordo/salas/{sessaoId}/participantes")
    public ResponseEntity<ChatAcordoSalaResponse> convidarParticipanteAcordo(
            @PathVariable @Positive Long sessaoId,
            @Valid @RequestBody ChatAcordoConvidarParticipanteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.convidarParticipanteAcordo(sessaoId, request));
    }

    @PostMapping("/acordo/salas/{sessaoId}/aceite")
    public ResponseEntity<ChatAcordoSalaResponse> aceitarParticipacaoAcordo(
            @PathVariable @Positive Long sessaoId
    ) {
        return ResponseEntity.ok(chatService.aceitarParticipacaoAcordo(sessaoId));
    }

    @PostMapping("/processo/{processoId}/acordo/anexos")
    public ResponseEntity<ChatMensagemResponse> registrarAnexoNegocial(
            @PathVariable @Positive Long processoId,
            @RequestBody AgreementChatAttachmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.registrarAnexoNegocial(processoId, request));
    }
}
