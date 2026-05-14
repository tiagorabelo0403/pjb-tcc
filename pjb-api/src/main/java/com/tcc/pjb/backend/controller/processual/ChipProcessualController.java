package com.tcc.pjb.backend.controller.processual;

import com.tcc.pjb.backend.core.api.PjbApiResponseEnvelope;
import com.tcc.pjb.backend.service.processual.chip.ProcessoChipAutoDetectionService;
import com.tcc.pjb.backend.service.processual.chip.ProcessoChipTipo;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chips")
public class ChipProcessualController {

    private final ProcessoChipAutoDetectionService detectionService;

    public ChipProcessualController(ProcessoChipAutoDetectionService detectionService) {
        this.detectionService = detectionService;
    }

    @PostMapping("/detectar")
    @PreAuthorize("hasAnyRole('SERVIDOR_JUDICIARIO','SUPERVISOR','MAGISTRADO')")
    public ResponseEntity<PjbApiResponseEnvelope<Set<ProcessoChipTipo>>> detectar(
            @RequestBody ProcessoChipAutoDetectionService.DetectionInput input) {
        Set<ProcessoChipTipo> chips = detectionService.detectar(input);
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(chips));
    }

    @PostMapping("/urgentes")
    @PreAuthorize("hasAnyRole('SERVIDOR_JUDICIARIO','SUPERVISOR')")
    public ResponseEntity<PjbApiResponseEnvelope<Boolean>> temUrgente(
            @RequestBody ProcessoChipAutoDetectionService.DetectionInput input) {
        Set<ProcessoChipTipo> chips = detectionService.detectar(input);
        boolean temUrgente = chips.stream().anyMatch(ProcessoChipTipo::geraAlertaUrgente);
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(temUrgente));
    }
}
