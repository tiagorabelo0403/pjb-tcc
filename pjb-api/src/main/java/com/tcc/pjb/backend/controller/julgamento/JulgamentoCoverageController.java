package com.tcc.pjb.backend.controller.julgamento;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.julgamento.coverage.JulgamentoCoverageAuditView;
import com.tcc.pjb.backend.model.dto.julgamento.coverage.JulgamentoCoverageRequest;
import com.tcc.pjb.backend.model.dto.julgamento.coverage.JulgamentoCoverageResponse;
import com.tcc.pjb.backend.service.julgamento.coverage.JulgamentoCoverageIntelligenceService;

@RestController
@RequestMapping("/api/v1/julgamento/cobertura")
@PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
public class JulgamentoCoverageController {

    private final JulgamentoCoverageIntelligenceService service;

    public JulgamentoCoverageController(JulgamentoCoverageIntelligenceService service) {
        this.service = service;
    }

    @PostMapping("/processos/{processoId}/analisar")
    public ResponseEntity<JulgamentoCoverageResponse> analisar(@PathVariable Long processoId,
                                                               @Valid @RequestBody JulgamentoCoverageRequest request) {
        return ResponseEntity.ok(service.analisar(processoId, request));
    }

    @GetMapping("/processos/{processoId}/historico")
    public ResponseEntity<List<JulgamentoCoverageAuditView>> historico(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.historico(processoId));
    }
}
