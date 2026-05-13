package com.tcc.pjb.backend.controller.institutional;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.processual.guard.InstitutionalMaterialActionGuardService;
import com.tcc.pjb.backend.service.processo.ProcessoAccessApplicationService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/institucional/material-guard")
@PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','MEMBRO_MINISTERIO_PUBLICO','PROMOTOR_ELEITORAL','PROMOTOR_TRABALHISTA','PROCURADOR_GERAL_REPUBLICA','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL')")
public class InstitutionalMaterialActionGuardController {

    private final ProcessoAccessApplicationService processoAccessApplicationService;
    private final InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService;

    public InstitutionalMaterialActionGuardController(ProcessoAccessApplicationService processoAccessApplicationService,
                                                      InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService) {
        this.processoAccessApplicationService = processoAccessApplicationService;
        this.institutionalMaterialActionGuardService = institutionalMaterialActionGuardService;
    }

    @GetMapping("/processos/{processoId}")
    public ResponseEntity<Map<String, Object>> analyzeProcessAction(@PathVariable Long processoId,
                                                                    @RequestParam InstitutionalMaterialActionGuardService.MaterialAction action) {
        Processo processo = processoAccessApplicationService.load(processoId);
        InstitutionalMaterialActionGuardService.GuardDecision decision = institutionalMaterialActionGuardService.analyzeProcessAction(processo, action);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", processoId);
        out.put("numeroProcesso", processo.getNumeroProcesso());
        out.put("action", action.name());
        out.put("verdict", decision.verdict().name());
        out.put("actorBranch", decision.actorBranch().name());
        out.put("targetSphere", decision.targetSphere().name());
        out.put("reasons", decision.reasons());
        out.put("warnings", decision.warnings());
        out.put("metrics", decision.metrics());
        return ResponseEntity.ok(out);
    }
}
