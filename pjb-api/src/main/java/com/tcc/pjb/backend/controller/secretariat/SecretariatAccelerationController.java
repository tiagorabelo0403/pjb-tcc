package com.tcc.pjb.backend.controller.secretariat;

import com.tcc.pjb.backend.core.api.PjbApiResponseEnvelope;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelItemDto;
import com.tcc.pjb.backend.service.secretariat.acceleration.SecretariatQueueBottleneckService;
import com.tcc.pjb.backend.service.secretariat.acceleration.SecretariatQueueNextBestActionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/secretariat/acceleration")
public class SecretariatAccelerationController {

    private final SecretariatQueueBottleneckService bottleneckService;
    private final SecretariatQueueNextBestActionService nextBestActionService;

    public SecretariatAccelerationController(
            SecretariatQueueBottleneckService bottleneckService,
            SecretariatQueueNextBestActionService nextBestActionService) {
        this.bottleneckService = bottleneckService;
        this.nextBestActionService = nextBestActionService;
    }

    @PostMapping("/bottlenecks")
    @PreAuthorize("hasAnyRole('SERVIDOR_JUDICIARIO','DIRETOR_SECRETARIA','SUPERVISOR')")
    public ResponseEntity<PjbApiResponseEnvelope<List<SecretariatQueueBottleneckService.GargaloCartorarioItem>>>
            bottlenecks(@RequestBody List<SecretariatQueuePanelItemDto> fila) {
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(bottleneckService.diagnosticar(fila)));
    }

    @PostMapping("/next-actions")
    @PreAuthorize("hasAnyRole('SERVIDOR_JUDICIARIO','DIRETOR_SECRETARIA')")
    public ResponseEntity<PjbApiResponseEnvelope<List<SecretariatQueueNextBestActionService.ProximaAcaoCartoraria>>>
            nextActions(@RequestBody List<SecretariatQueuePanelItemDto> fila) {
        return ResponseEntity.ok(PjbApiResponseEnvelope.ok(
                nextBestActionService.sugerirParaFila(fila)));
    }
}
