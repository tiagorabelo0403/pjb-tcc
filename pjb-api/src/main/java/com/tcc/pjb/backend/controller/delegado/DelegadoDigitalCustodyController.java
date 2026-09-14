package com.tcc.pjb.backend.controller.delegado;

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
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodyLedgerResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySealRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySealResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncBundleResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncEventResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncExportRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncReplayRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncReplayResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.profile.DigitalCustodyChainLedgerService;
import com.tcc.pjb.backend.service.profile.DigitalCustodyChainService;
import com.tcc.pjb.backend.service.profile.DigitalCustodyChainSyncService;

/**
 * Cadeia de custódia digital de provas do delegado — selagem, ledger e sincronização.
 * Extraído de {@link DelegadoPainelController} (recorte de F6: controller com 21
 * dependências de construtor, uma por família de sub-recurso REST).
 */
@RestController
@RequestMapping("/api/v1/delegado")
public class DelegadoDigitalCustodyController {

    private final CapabilityRateLimiter rateLimiter;
    private final DigitalCustodyChainService digitalCustodyChainService;
    private final DigitalCustodyChainLedgerService digitalCustodyChainLedgerService;
    private final DigitalCustodyChainSyncService digitalCustodyChainSyncService;

    public DelegadoDigitalCustodyController(CapabilityRateLimiter rateLimiter,
                                            DigitalCustodyChainService digitalCustodyChainService,
                                            DigitalCustodyChainLedgerService digitalCustodyChainLedgerService,
                                            DigitalCustodyChainSyncService digitalCustodyChainSyncService) {
        this.rateLimiter = rateLimiter;
        this.digitalCustodyChainService = digitalCustodyChainService;
        this.digitalCustodyChainLedgerService = digitalCustodyChainLedgerService;
        this.digitalCustodyChainSyncService = digitalCustodyChainSyncService;
    }

    @PostMapping("/provas/custodia/selar")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<ChainOfCustodySealResponse> selarCustodia(@Valid @RequestBody ChainOfCustodySealRequest request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_custodia_selar", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainService.seal(request));
    }

    @GetMapping("/provas/custodia/{chaveCustodia}/ledger")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<ChainOfCustodyLedgerResponse> ledgerCustodia(@PathVariable String chaveCustodia, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_custodia_ledger", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainLedgerService.ledger(chaveCustodia));
    }

    @PostMapping("/provas/custodia/{chaveCustodia}/sync/export")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<ChainOfCustodySyncBundleResponse> exportarCustodia(@PathVariable String chaveCustodia,
                                                                             @RequestBody(required = false) ChainOfCustodySyncExportRequest request,
                                                                             Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_custodia_sync_export", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainSyncService.exportBundle(chaveCustodia, request));
    }

    @PostMapping("/provas/custodia/sync/replay")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<ChainOfCustodySyncReplayResponse> replayCustodia(@Valid @RequestBody ChainOfCustodySyncReplayRequest request,
                                                                           Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_custodia_sync_replay", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainSyncService.replayVerify(request));
    }

    @GetMapping("/provas/custodia/{chaveCustodia}/sync/events")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<ChainOfCustodySyncEventResponse>> eventosCustodia(@PathVariable String chaveCustodia,
                                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_custodia_sync_events", ApiVersion.V1);
        return ResponseEntity.ok(digitalCustodyChainSyncService.history(chaveCustodia));
    }
}
