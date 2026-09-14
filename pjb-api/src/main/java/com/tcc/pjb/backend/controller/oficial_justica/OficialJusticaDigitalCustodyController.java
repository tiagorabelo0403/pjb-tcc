package com.tcc.pjb.backend.controller.oficial_justica;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncBundleResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncEventResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncExportRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncReplayRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncReplayResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.profile.DigitalCustodyChainSyncService;

/**
 * Sincronização da cadeia de custódia digital de provas do oficial de justiça.
 * Extraído de {@link OficialJusticaCampoController} (recorte de F6).
 */
@RestController
@RequestMapping("/api/v1/oficial-justica")
public class OficialJusticaDigitalCustodyController {

    private final CapabilityRateLimiter rateLimiter;
    private final DigitalCustodyChainSyncService digitalCustodyChainSyncService;

    public OficialJusticaDigitalCustodyController(CapabilityRateLimiter rateLimiter,
                                                   DigitalCustodyChainSyncService digitalCustodyChainSyncService) {
        this.rateLimiter = rateLimiter;
        this.digitalCustodyChainSyncService = digitalCustodyChainSyncService;
    }

    @PostMapping("/custodia/{chaveCustodia}/sync/export")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<ChainOfCustodySyncBundleResponse> exportarCustodia(@PathVariable String chaveCustodia,
                                                                             @RequestBody(required = false) ChainOfCustodySyncExportRequest request,
                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_custodia_sync_export", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainSyncService.exportBundle(chaveCustodia, request));
    }

    @PostMapping("/custodia/sync/replay")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<ChainOfCustodySyncReplayResponse> replayCustodia(@Valid @RequestBody ChainOfCustodySyncReplayRequest request,
                                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_custodia_sync_replay", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainSyncService.replayVerify(request));
    }

    @GetMapping("/custodia/{chaveCustodia}/sync/events")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<ChainOfCustodySyncEventResponse>> eventosCustodia(@PathVariable String chaveCustodia,
                                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_custodia_sync_events", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainSyncService.history(chaveCustodia));
    }
}
