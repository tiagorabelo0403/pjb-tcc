package com.tcc.pjb.backend.ai.legalai;

import com.tcc.pjb.backend.ai.legalai.dreaming.application.DreamingNaoPermitidoException;
import com.tcc.pjb.backend.ai.legalai.dreaming.application.DreamingOrchestrator;
import com.tcc.pjb.backend.ai.legalai.security.PromptInjectionException;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.Dream;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamId;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamInput;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamResult;
import com.tcc.pjb.backend.ai.legalai.dto.DreamingSessionRequest;
import com.tcc.pjb.backend.ai.legalai.dto.DreamingSessionResponse;
import com.tcc.pjb.backend.ai.legalai.dto.DreamStatusResponse;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/legal-ai/dreaming", produces = MediaType.APPLICATION_JSON_VALUE)
public class DreamingController {

    private final DreamingOrchestrator orchestrator;

    public DreamingController(DreamingOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping(path = "/sessions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DreamingSessionResponse> iniciarSessao(
            @Valid @RequestBody DreamingSessionRequest request) {
        try {
            DreamInput input = construirInput(request);
            String modelo = request.modelo() != null ? request.modelo() : "claude-sonnet-4-6";
            DreamId dreamId = orchestrator.iniciarDreaming(input, request.instrucoes(), modelo);
            return ResponseEntity.accepted()
                    .body(new DreamingSessionResponse(dreamId.value().toString(), "PENDING"));
        } catch (PromptInjectionException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (DreamingNaoPermitidoException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{dreamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DreamStatusResponse> consultarStatus(@PathVariable UUID dreamId) {
        return orchestrator.consultar(DreamId.de(dreamId))
                .map(this::toDreamStatusResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{dreamId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DreamStatusResponse> cancelar(@PathVariable UUID dreamId) {
        try {
            Dream cancelado = orchestrator.cancelar(DreamId.de(dreamId));
            return ResponseEntity.ok(toDreamStatusResponse(cancelado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private DreamInput construirInput(DreamingSessionRequest request) {
        MemoryStoreId storeId = MemoryStoreId.de(request.inputStoreId());
        if (request.sessionIds() != null && !request.sessionIds().isEmpty()) {
            return new DreamInput.SessionsInput(storeId, request.sessionIds());
        }
        return new DreamInput.MemoryStoreInput(storeId);
    }

    private DreamStatusResponse toDreamStatusResponse(Dream dream) {
        DreamResult result = dream.result();
        String outputStoreId = result != null && result.outputStoreId() != null
                ? result.outputStoreId().value().toString()
                : null;
        Long inputTokens = result != null ? result.inputTokens() : null;
        Long outputTokens = result != null ? result.outputTokens() : null;
        return new DreamStatusResponse(
                dream.id().value().toString(),
                dream.status().name(),
                outputStoreId,
                inputTokens,
                outputTokens,
                dream.erro()
        );
    }
}
