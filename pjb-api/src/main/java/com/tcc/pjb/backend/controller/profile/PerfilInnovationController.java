package com.tcc.pjb.backend.controller.profile;

import java.util.Objects;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodyLedgerResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySealRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySealResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncBundleResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncEventResponse;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncExportRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncReplayRequest;
import com.tcc.pjb.backend.model.dto.profile.ChainOfCustodySyncReplayResponse;
import com.tcc.pjb.backend.model.dto.profile.DeadlineFatalControlResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationResponse;
import com.tcc.pjb.backend.model.dto.profile.GovBrIdentityShieldResponse;
import com.tcc.pjb.backend.model.dto.profile.LegalTranslatorRequest;
import com.tcc.pjb.backend.model.dto.profile.LegalTranslatorResponse;
import com.tcc.pjb.backend.model.dto.profile.ProfileInnovationCatalogResponse;
import com.tcc.pjb.backend.service.profile.DeadlineFatalControlService;
import com.tcc.pjb.backend.service.profile.DigitalCustodyChainLedgerService;
import com.tcc.pjb.backend.service.profile.DigitalCustodyChainService;
import com.tcc.pjb.backend.service.profile.DigitalCustodyChainSyncService;
import com.tcc.pjb.backend.service.profile.DiligenceRouteOptimizationService;
import com.tcc.pjb.backend.service.profile.GovBrIdentityShieldService;
import com.tcc.pjb.backend.service.profile.LegalPlainLanguageService;
import com.tcc.pjb.backend.service.profile.ProfileInnovationCatalogService;

@RestController
@RequestMapping("/api/v1/profile/innovation")
public class PerfilInnovationController {

    private final ProfileInnovationCatalogService catalogService;
    private final LegalPlainLanguageService legalPlainLanguageService;
    private final DeadlineFatalControlService deadlineFatalControlService;
    private final DiligenceRouteOptimizationService diligenceRouteOptimizationService;
    private final DigitalCustodyChainService digitalCustodyChainService;
    private final DigitalCustodyChainLedgerService digitalCustodyChainLedgerService;
    private final DigitalCustodyChainSyncService digitalCustodyChainSyncService;
    private final GovBrIdentityShieldService govBrIdentityShieldService;

    public PerfilInnovationController(ProfileInnovationCatalogService catalogService,
                                      LegalPlainLanguageService legalPlainLanguageService,
                                      DeadlineFatalControlService deadlineFatalControlService,
                                      DiligenceRouteOptimizationService diligenceRouteOptimizationService,
                                      DigitalCustodyChainService digitalCustodyChainService,
                                      DigitalCustodyChainLedgerService digitalCustodyChainLedgerService,
                                      DigitalCustodyChainSyncService digitalCustodyChainSyncService,
                                      GovBrIdentityShieldService govBrIdentityShieldService) {
        this.catalogService = Objects.requireNonNull(catalogService);
        this.legalPlainLanguageService = Objects.requireNonNull(legalPlainLanguageService);
        this.deadlineFatalControlService = Objects.requireNonNull(deadlineFatalControlService);
        this.diligenceRouteOptimizationService = Objects.requireNonNull(diligenceRouteOptimizationService);
        this.digitalCustodyChainService = Objects.requireNonNull(digitalCustodyChainService);
        this.digitalCustodyChainLedgerService = Objects.requireNonNull(digitalCustodyChainLedgerService);
        this.digitalCustodyChainSyncService = Objects.requireNonNull(digitalCustodyChainSyncService);
        this.govBrIdentityShieldService = Objects.requireNonNull(govBrIdentityShieldService);
    }

    @GetMapping("/catalog")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileInnovationCatalogResponse> catalog(@RequestParam(required = false) String role) {
        return ResponseEntity.ok(catalogService.forCurrentUserOrRole(role));
    }

    @PostMapping("/legal-translator")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LegalTranslatorResponse> legalTranslator(@Valid @RequestBody LegalTranslatorRequest request) {
        return ResponseEntity.ok(legalPlainLanguageService.translate(request));
    }

    @GetMapping("/deadlines/fatal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DeadlineFatalControlResponse> fatalDeadlines(@RequestParam(required = false) Integer horizonDays,
                                                                       @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(deadlineFatalControlService.monitor(horizonDays, limit));
    }

    @PostMapping("/route-optimizer")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR','DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceRouteOptimizationResponse> optimizeRoute(@Valid @RequestBody DiligenceRouteOptimizationRequest request) {
        return ResponseEntity.ok(diligenceRouteOptimizationService.optimize(request));
    }

    @PostMapping("/custody/seal")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR','PERITO','PERITO_CRIMINAL','PERITO_DIGITAL')")
    public ResponseEntity<ChainOfCustodySealResponse> sealCustody(@Valid @RequestBody ChainOfCustodySealRequest request) {
        return ResponseEntity.ok(digitalCustodyChainService.seal(request));
    }

    @GetMapping("/custody/ledger")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR','PERITO','PERITO_CRIMINAL','PERITO_DIGITAL','MEMBRO_MINISTERIO_PUBLICO','DEFENSOR_PUBLICO')")
    public ResponseEntity<ChainOfCustodyLedgerResponse> custodyLedger(@RequestParam String chaveCustodia) {
        return ResponseEntity.ok(digitalCustodyChainLedgerService.ledger(chaveCustodia));
    }

    @GetMapping("/govbr/posture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GovBrIdentityShieldResponse> govBrPosture() {
        return ResponseEntity.ok(govBrIdentityShieldService.currentPosture());
    }


    @PostMapping("/custody/{chaveCustodia}/sync/export")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','PERITO','PERITO_CRIMINAL','PERITO_DIGITAL','OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<ChainOfCustodySyncBundleResponse> exportCustodyBundle(@PathVariable String chaveCustodia,
                                                                                @RequestBody(required = false) ChainOfCustodySyncExportRequest request) {
        return ResponseEntity.ok(digitalCustodyChainSyncService.exportBundle(chaveCustodia, request));
    }

    @PostMapping("/custody/sync/replay")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','PERITO','PERITO_CRIMINAL','PERITO_DIGITAL','OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR','MEMBRO_MINISTERIO_PUBLICO')")
    public ResponseEntity<ChainOfCustodySyncReplayResponse> replayCustodyBundle(@Valid @RequestBody ChainOfCustodySyncReplayRequest request) {
        return ResponseEntity.ok(digitalCustodyChainSyncService.replayVerify(request));
    }

    @GetMapping("/custody/{chaveCustodia}/sync/events")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','PERITO','PERITO_CRIMINAL','PERITO_DIGITAL','OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR','MEMBRO_MINISTERIO_PUBLICO','DEFENSOR_PUBLICO')")
    public ResponseEntity<java.util.List<ChainOfCustodySyncEventResponse>> custodySyncEvents(@PathVariable String chaveCustodia) {
        return ResponseEntity.ok(digitalCustodyChainSyncService.history(chaveCustodia));
    }

}
