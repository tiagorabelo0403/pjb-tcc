package com.tcc.pjb.backend.modules.laiane.api;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.*;
import com.tcc.pjb.backend.modules.laiane.service.LaianeMpService;

@RestController
@RequestMapping("/api/v1/laiane/mp")
@PreAuthorize("hasAnyRole('MEMBRO_MINISTERIO_PUBLICO','PROMOTOR_ELEITORAL','PROMOTOR_TRABALHISTA','PROCURADOR_GERAL_REPUBLICA')")
public class LaianeMpController {

    private final LaianeMpService service;

    public LaianeMpController(LaianeMpService service) {
        this.service = service;
    }

    @GetMapping("/inbox")
    public LaianeMpInboxResponse inbox() {
        return service.inbox();
    }

    
    
    

    @GetMapping("/deadlines/monitor")
    public LaianeMpDeadlineMonitorResponse monitorDeadlines(
            @RequestParam(defaultValue = "14") int horizonDays,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return service.monitorDeadlines(horizonDays, limit);
    }

    @PostMapping("/oficios")
    public LaianeMpOficioResponse createOficio(@Valid @RequestBody LaianeMpOficioCreateRequest req) {
        return service.createOficio(req);
    }

    @GetMapping("/oficios/{trackingCode}")
    public LaianeMpOficioResponse getOficio(@PathVariable UUID trackingCode) {
        return service.getOficio(trackingCode);
    }

    @PutMapping("/oficios/{trackingCode}/status")
    public LaianeMpOficioResponse updateOficioStatus(
            @PathVariable UUID trackingCode,
            @Valid @RequestBody LaianeMpOficioStatusUpdateRequest req
    ) {
        return service.updateOficioStatus(trackingCode, req);
    }

    @GetMapping("/audit")
    public LaianeMpAuditResponse audit(
            @RequestParam(required = false) String referenciaId,
            @RequestParam(required = false) String acao,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return service.audit(referenciaId, acao, page, size);
    }
}
