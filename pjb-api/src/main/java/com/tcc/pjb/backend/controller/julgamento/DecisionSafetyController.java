package com.tcc.pjb.backend.controller.julgamento;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.julgamento.safety.DecisionFocusOpenRequest;
import com.tcc.pjb.backend.model.dto.julgamento.safety.DecisionFocusHeartbeatRequest;
import com.tcc.pjb.backend.model.dto.julgamento.safety.DecisionFocusResponse;
import com.tcc.pjb.backend.model.dto.julgamento.safety.DecisionPreflightRequest;
import com.tcc.pjb.backend.model.dto.julgamento.safety.DecisionPreflightResponse;
import com.tcc.pjb.backend.service.julgamento.safety.DecisionSafetyService;

@RestController
@RequestMapping("/api/v1/julgamento/seguranca")
@PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO')")
public class DecisionSafetyController {

    private final DecisionSafetyService decisionSafetyService;

    public DecisionSafetyController(DecisionSafetyService decisionSafetyService) {
        this.decisionSafetyService = decisionSafetyService;
    }

    @PostMapping("/processos/{processoId}/foco")
    public ResponseEntity<DecisionFocusResponse> openFocus(@PathVariable Long processoId,
                                                           @RequestBody(required = false) DecisionFocusOpenRequest request) {
        String binding = request == null ? null : request.windowBinding();
        return ResponseEntity.ok(decisionSafetyService.openFocus(processoId, binding, request == null ? null : request.tabBinding(), request == null ? null : request.routeBinding()));
    }

    @PostMapping("/foco/{sessionId}/armar")
    public ResponseEntity<DecisionFocusResponse> armFocus(@PathVariable Long sessionId) {
        return ResponseEntity.ok(decisionSafetyService.armFocus(sessionId));
    }

    @PostMapping("/foco/{sessionId}/heartbeat")
    public ResponseEntity<DecisionFocusResponse> heartbeat(@PathVariable Long sessionId,
                                                           @Valid @RequestBody DecisionFocusHeartbeatRequest request) {
        return ResponseEntity.ok(decisionSafetyService.heartbeat(sessionId, request.windowBinding(), request.tabBinding(), request.routeBinding()));
    }

    @GetMapping("/foco/atual")
    public ResponseEntity<DecisionFocusResponse> currentFocus() {
        DecisionFocusResponse response = decisionSafetyService.currentFocus();
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    @PostMapping("/foco/{sessionId}/encerrar")
    public ResponseEntity<Void> releaseFocus(@PathVariable Long sessionId) {
        decisionSafetyService.releaseFocus(sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/processos/{processoId}/preflight")
    public ResponseEntity<DecisionPreflightResponse> preflight(@PathVariable Long processoId,
                                                               @Valid @RequestBody DecisionPreflightRequest request) {
        return ResponseEntity.ok(decisionSafetyService.preflight(processoId, request.actType(), request.primaryText(), request.reasoningText()));
    }
}
