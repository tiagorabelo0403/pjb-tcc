package com.tcc.pjb.backend.controller.execucaofiscal;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.acceleration.execucao.ExecucaoFiscalBatchActService;
import com.tcc.pjb.backend.service.processual.acceleration.execucao.ExecucaoFiscalPrescricaoIntercorrenteRadarService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/execucaofiscal")
public class ExecucaoFiscalController {

    private final ExecucaoFiscalPrescricaoIntercorrenteRadarService radarService;
    private final ExecucaoFiscalBatchActService batchService;
    private final CapabilityRateLimiter rateLimiter;

    public ExecucaoFiscalController(ExecucaoFiscalPrescricaoIntercorrenteRadarService radarService,
                                    ExecucaoFiscalBatchActService batchService,
                                    CapabilityRateLimiter rateLimiter) {
        this.radarService = radarService;
        this.batchService = batchService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/prescricao-intercorrente/radar")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<List<ExecucaoFiscalPrescricaoIntercorrenteRadarService.AlertaPrescricao>> radar(
            @Valid @RequestBody List<ExecucaoFiscalPrescricaoIntercorrenteRadarService.ExecucaoFiscalItem> processos,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "execucaofiscal_prescricao_radar", ApiVersion.V1);
        return ResponseEntity.ok(radarService.radar(processos));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','PROMOTOR_MINISTERIO_PUBLICO')")
    public ResponseEntity<List<ExecucaoFiscalBatchActService.LoteFiscal>> batch(
            @Valid @RequestBody List<ExecucaoFiscalBatchActService.ExecucaoBatchItem> processos,
            Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.JURIDICA, authentication, "execucaofiscal_batch", ApiVersion.V1);
        return ResponseEntity.ok(batchService.detectarLotes(processos));
    }
}
