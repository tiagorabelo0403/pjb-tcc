package com.tcc.pjb.backend.controller;

import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.acordo.AcordoIntelligenceDto;
import com.tcc.pjb.backend.model.dto.intelligence.JudgeAgreementApprovalPromptResponse;
import com.tcc.pjb.backend.model.dto.intelligence.JudgeAgreementApprovalRequest;
import com.tcc.pjb.backend.model.dto.intelligence.JudgeAgreementDecisionRequest;
import com.tcc.pjb.backend.model.dto.intelligence.JudgeAgreementDecisionResponse;
import com.tcc.pjb.backend.service.AcordoIntelligenceOrchestrator;
import com.tcc.pjb.backend.service.AcordoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/processos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AcordoIntelligenceController {

    private final AcordoIntelligenceOrchestrator acordoIntelligenceOrchestrator;
    private final AcordoService acordoService;

    @GetMapping("/{processoId}/acordo/intelligence")
    public ResponseEntity<AcordoIntelligenceDto> intelligence(@PathVariable Long processoId,
                                                              @RequestParam(required = false) BigDecimal valorPretendido) {
        return ResponseEntity.ok(acordoIntelligenceOrchestrator.analisarInteligenciaNegocial(processoId, valorPretendido));
    }

    @PostMapping("/acordo/propostas/{propostaId}/solicitar-homologacao")
    public ResponseEntity<JudgeAgreementApprovalPromptResponse> solicitarHomologacao(@PathVariable Long propostaId,
                                                                                      @RequestBody(required = false) JudgeAgreementApprovalRequest request) {
        return ResponseEntity.ok(acordoService.solicitarHomologacaoJudicial(propostaId, request != null ? request.resumoExecutivo() : null));
    }

    @PostMapping("/acordo/propostas/{propostaId}/decisao-homologacao")
    public ResponseEntity<JudgeAgreementDecisionResponse> decidirHomologacao(@PathVariable Long propostaId,
                                                                             @RequestBody JudgeAgreementDecisionRequest request) {
        return ResponseEntity.ok(acordoService.decidirHomologacaoJudicial(
                propostaId,
                request != null ? request.action() : null,
                request != null ? request.justification() : null,
                request != null ? request.hashAssinaturaJuiz() : null,
                request != null && request.notifyParties()
        ));
    }
}

