package com.tcc.pjb.backend.controller.ui;

import com.tcc.pjb.backend.core.frontend.app.application.PjbFrontendAppApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationDecisionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationFinalApprovalRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationInviteRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedDocumentBatchLinkRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedPetitionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedMultimediaWorkspaceRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedProtocolSubmitRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadBatchCreateRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadFinalizeRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadReserveItemRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeModeUpdateRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeProcessTransferDecisionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeProcessTransferRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeQueueDecisionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceCreateRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceProcessQueryRequest;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceModeService;

@RestController
@RequestMapping("/api/v1/frontend/app")
@PreAuthorize("isAuthenticated()")
public class FrontendAppController {

    private final PjbFrontendAppApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public FrontendAppController(PjbFrontendAppApplicationService applicationService,
                                 ApiResponseFactory apiResponseFactory) {
        this.applicationService = applicationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiQueryResponse<?>> me(Authentication authentication) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.me(authentication), List.of()));
    }

    @GetMapping("/me/capabilities")
    public ResponseEntity<ApiQueryResponse<?>> capabilities() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.capabilities(), List.of()));
    }

    @GetMapping("/me/context")
    public ResponseEntity<ApiQueryResponse<?>> context(Authentication authentication, HttpServletRequest request) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.context(authentication, request), List.of()));
    }

    @GetMapping("/me/menu")
    public ResponseEntity<ApiQueryResponse<?>> menu(Authentication authentication) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.menu(authentication), List.of()));
    }

    @GetMapping("/support/catalogs")
    public ResponseEntity<ApiQueryResponse<?>> supportCatalogs() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.supportCatalogs(), List.of()));
    }

    @GetMapping("/support/catalogs/ramos-direito")
    public ResponseEntity<ApiQueryResponse<?>> ramoDireitoCatalog() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.ramoDireitoCatalog(), List.of()));
    }

    @GetMapping("/offices/mine")
    public ResponseEntity<ApiQueryResponse<?>> myOwnedOffices() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.myOwnedOffices(), List.of()));
    }

    @PostMapping("/offices")
    public ResponseEntity<ApiQueryResponse<?>> createOffice(@Valid @RequestBody FrontendOfficeWorkspaceCreateRequest request,
                                                            HttpServletResponse response) {
        var view = applicationService.createOwnOffice(request);
        writeOfficeCookies(response, view.mode(), view.activeEquipeId());
        return ResponseEntity.ok(apiResponseFactory.queryOk(view, List.of()));
    }

    @PostMapping("/offices/personal/ensure")
    public ResponseEntity<ApiQueryResponse<?>> ensurePersonalOffice(HttpServletResponse response) {
        var view = applicationService.ensurePersonalOffice();
        writeOfficeCookies(response, view.mode(), view.activeEquipeId());
        return ResponseEntity.ok(apiResponseFactory.queryOk(view, List.of()));
    }

    @GetMapping("/offices/workspace/summary")
    public ResponseEntity<ApiQueryResponse<?>> officeWorkspaceSummary(@org.springframework.web.bind.annotation.RequestParam(name = "equipeId", required = false) Long equipeId,
                                                                      HttpServletRequest request) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.officeWorkspaceSummary(equipeId, request), List.of()));
    }


    @GetMapping("/offices/invites/incoming")
    public ResponseEntity<ApiQueryResponse<?>> incomingOfficeInvites() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.myIncomingOfficeInvites(), List.of()));
    }

    @GetMapping("/offices/{equipeId}/invites")
    public ResponseEntity<ApiQueryResponse<?>> officeInvites(@org.springframework.web.bind.annotation.PathVariable Long equipeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.officeInvites(equipeId), List.of()));
    }

    @PostMapping("/offices/invites")
    public ResponseEntity<ApiQueryResponse<?>> createOfficeInvite(@Valid @RequestBody FrontendOfficeAffiliationInviteRequest request) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.createOfficeInvite(request), List.of()));
    }

    @PostMapping("/offices/invites/{inviteId}/accept")
    public ResponseEntity<ApiQueryResponse<?>> acceptOfficeInvite(@org.springframework.web.bind.annotation.PathVariable Long inviteId,
                                                                  @RequestBody(required = false) FrontendOfficeAffiliationDecisionRequest request,
                                                                  HttpServletResponse response) {
        var result = applicationService.acceptOfficeInvite(inviteId, request);
        if (result.activated() && result.officeMode() != null) {
            writeOfficeCookies(response, result.officeMode().mode(), result.officeMode().activeEquipeId());
        }
        return ResponseEntity.ok(apiResponseFactory.queryOk(result, List.of()));
    }

    @PostMapping("/offices/invites/{inviteId}/reject")
    public ResponseEntity<ApiQueryResponse<?>> rejectOfficeInvite(@org.springframework.web.bind.annotation.PathVariable Long inviteId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.rejectOfficeInvite(inviteId), List.of()));
    }

    @DeleteMapping("/offices/invites/{inviteId}")
    public ResponseEntity<ApiQueryResponse<?>> revokeOfficeInvite(@org.springframework.web.bind.annotation.PathVariable Long inviteId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.revokeOfficeInvite(inviteId), List.of()));
    }

    @PostMapping("/offices/invites/{inviteId}/confirm-activation")
    public ResponseEntity<ApiQueryResponse<?>> confirmOfficeInviteActivation(@org.springframework.web.bind.annotation.PathVariable Long inviteId,
                                                                             @RequestBody FrontendOfficeAffiliationFinalApprovalRequest request) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.confirmOfficeInviteActivation(inviteId, request), List.of()));
    }

    @GetMapping("/offices/transfers/incoming")
    public ResponseEntity<ApiQueryResponse<?>> incomingOfficeTransfers() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.myIncomingOfficeTransfers(), List.of()));
    }

    @GetMapping("/offices/{equipeId}/transfers")
    public ResponseEntity<ApiQueryResponse<?>> officeTransfers(@org.springframework.web.bind.annotation.PathVariable Long equipeId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.officeTransfers(equipeId), List.of()));
    }

    @PostMapping("/offices/transfers/preview")
    public ResponseEntity<ApiQueryResponse<?>> previewOfficeTransfer(@Valid @RequestBody FrontendOfficeProcessTransferRequest request) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.previewOfficeTransfer(request), List.of()));
    }

    @PostMapping("/offices/transfers")
    public ResponseEntity<ApiQueryResponse<?>> createOfficeTransfer(@Valid @RequestBody FrontendOfficeProcessTransferRequest request) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.createOfficeTransfer(request), List.of()));
    }

    @PostMapping("/offices/transfers/{transferId}/accept")
    public ResponseEntity<ApiQueryResponse<?>> acceptOfficeTransfer(@org.springframework.web.bind.annotation.PathVariable Long transferId,
                                                                    @RequestBody FrontendOfficeProcessTransferDecisionRequest request) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.acceptOfficeTransfer(transferId, request), List.of()));
    }

    @PostMapping("/offices/transfers/{transferId}/reject")
    public ResponseEntity<ApiQueryResponse<?>> rejectOfficeTransfer(@org.springframework.web.bind.annotation.PathVariable Long transferId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.rejectOfficeTransfer(transferId), List.of()));
    }

    @PostMapping("/offices/workspace/processes/query")
    public ResponseEntity<ApiQueryResponse<?>> officeWorkspaceProcesses(@RequestBody(required = false) FrontendOfficeWorkspaceProcessQueryRequest request,
                                                                        HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.officeWorkspaceProcesses(request, httpServletRequest), List.of()));
    }

    @GetMapping("/offices/workspace/processes/{processoId}/access")
    public ResponseEntity<ApiQueryResponse<?>> officeWorkspaceProcessAccess(@org.springframework.web.bind.annotation.PathVariable Long processoId,
                                                                            @org.springframework.web.bind.annotation.RequestParam(name = "action", required = false) String action,
                                                                            HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.officeWorkspaceProcessAccess(processoId, action, httpServletRequest), List.of()));
    }

    @PostMapping("/offices/workspace/processes/{processoId}/petitions")
    public ResponseEntity<ApiQueryResponse<?>> submitOfficeGovernedPetition(@org.springframework.web.bind.annotation.PathVariable Long processoId,
                                                                             @Valid @RequestBody FrontendOfficeGovernedPetitionRequest request,
                                                                             HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.submitOfficeGovernedPetition(processoId, request, httpServletRequest), List.of()));
    }


    @GetMapping("/offices/workspace/queue")
    public ResponseEntity<ApiQueryResponse<?>> officeWorkspaceQueue(@org.springframework.web.bind.annotation.RequestParam(name = "page", required = false) Integer page,
                                                                    @org.springframework.web.bind.annotation.RequestParam(name = "size", required = false) Integer size,
                                                                    @org.springframework.web.bind.annotation.RequestParam(name = "status", required = false) String status) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.officeWorkspaceQueue(page, size, status), List.of()));
    }

    @PostMapping("/offices/workspace/queue/{queueItemId}/approve")
    public ResponseEntity<ApiQueryResponse<?>> approveOfficeWorkspaceQueue(@org.springframework.web.bind.annotation.PathVariable Long queueItemId,
                                                                           @RequestBody(required = false) FrontendOfficeQueueDecisionRequest request) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.approveOfficeWorkspaceQueue(queueItemId, request), List.of()));
    }

    @PostMapping("/offices/workspace/queue/{queueItemId}/reject")
    public ResponseEntity<ApiQueryResponse<?>> rejectOfficeWorkspaceQueue(@org.springframework.web.bind.annotation.PathVariable Long queueItemId,
                                                                          @RequestBody(required = false) FrontendOfficeQueueDecisionRequest request) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.rejectOfficeWorkspaceQueue(queueItemId, request), List.of()));
    }

    @GetMapping("/offices/workspace/processes/{processoId}/document-batches/{batchId}/preview")
    public ResponseEntity<ApiQueryResponse<?>> previewOfficeGovernedDocumentBatch(@org.springframework.web.bind.annotation.PathVariable Long processoId,
                                                                                   @org.springframework.web.bind.annotation.PathVariable java.util.UUID batchId,
                                                                                   HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.previewOfficeGovernedDocumentBatch(processoId, batchId, httpServletRequest), List.of()));
    }

    @PostMapping("/offices/workspace/processes/{processoId}/document-batches/link")
    public ResponseEntity<ApiQueryResponse<?>> linkOfficeGovernedDocumentBatch(@org.springframework.web.bind.annotation.PathVariable Long processoId,
                                                                                @Valid @RequestBody FrontendOfficeGovernedDocumentBatchLinkRequest request,
                                                                                HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.linkOfficeGovernedDocumentBatch(processoId, request, httpServletRequest), List.of()));
    }

    @PostMapping("/offices/workspace/processes/{processoId}/protocol-packages/{protocolPackageId}/submit")
    public ResponseEntity<ApiQueryResponse<?>> submitOfficeGovernedProtocol(@org.springframework.web.bind.annotation.PathVariable Long processoId,
                                                                            @org.springframework.web.bind.annotation.PathVariable Long protocolPackageId,
                                                                            @RequestBody(required = false) FrontendOfficeGovernedProtocolSubmitRequest request,
                                                                            HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.submitOfficeGovernedProtocol(processoId, protocolPackageId, request, httpServletRequest), List.of()));
    }


    @PostMapping("/offices/workspace/processes/{processoId}/uploads/batches")
    public ResponseEntity<ApiQueryResponse<?>> createOfficeGovernedUploadBatch(@org.springframework.web.bind.annotation.PathVariable Long processoId,
                                                                                @RequestBody(required = false) FrontendOfficeGovernedUploadBatchCreateRequest request,
                                                                                HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.createOfficeGovernedUploadBatch(processoId, request, httpServletRequest), List.of()));
    }

    @GetMapping("/offices/workspace/processes/{processoId}/uploads/batches/{batchId}")
    public ResponseEntity<ApiQueryResponse<?>> officeGovernedUploadBatch(@org.springframework.web.bind.annotation.PathVariable Long processoId,
                                                                          @org.springframework.web.bind.annotation.PathVariable java.util.UUID batchId,
                                                                          HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.officeGovernedUploadBatch(processoId, batchId, httpServletRequest), List.of()));
    }

    @PostMapping("/offices/workspace/processes/{processoId}/uploads/batches/{batchId}/items")
    public ResponseEntity<ApiQueryResponse<?>> reserveOfficeGovernedUploadItem(@org.springframework.web.bind.annotation.PathVariable Long processoId,
                                                                                @org.springframework.web.bind.annotation.PathVariable java.util.UUID batchId,
                                                                                @Valid @RequestBody FrontendOfficeGovernedUploadReserveItemRequest request,
                                                                                HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.reserveOfficeGovernedUploadItem(processoId, batchId, request, httpServletRequest), List.of()));
    }

    @PutMapping(value = "/offices/workspace/processes/{processoId}/uploads/direct/{batchId}/{itemId}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ApiQueryResponse<?>> directOfficeGovernedUpload(@org.springframework.web.bind.annotation.PathVariable Long processoId,
                                                                           @org.springframework.web.bind.annotation.PathVariable java.util.UUID batchId,
                                                                           @org.springframework.web.bind.annotation.PathVariable java.util.UUID itemId,
                                                                           @org.springframework.web.bind.annotation.RequestParam("token") String token,
                                                                           HttpServletRequest httpServletRequest) throws Exception {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.directOfficeGovernedUpload(processoId, batchId, itemId, token, httpServletRequest), List.of()));
    }

    @PostMapping("/offices/workspace/processes/{processoId}/uploads/batches/{batchId}/finalize")
    public ResponseEntity<ApiQueryResponse<?>> finalizeOfficeGovernedUploadBatch(@org.springframework.web.bind.annotation.PathVariable Long processoId,
                                                                                  @org.springframework.web.bind.annotation.PathVariable java.util.UUID batchId,
                                                                                  @RequestBody(required = false) FrontendOfficeGovernedUploadFinalizeRequest request,
                                                                                  HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.finalizeOfficeGovernedUploadBatch(processoId, batchId, request, httpServletRequest), List.of()));
    }

    @PostMapping("/offices/workspace/processes/{processoId}/multimedia/workspace")
    public ResponseEntity<ApiQueryResponse<?>> previewOfficeGovernedMultimediaWorkspace(@org.springframework.web.bind.annotation.PathVariable Long processoId,
                                                                                         @RequestBody(required = false) FrontendOfficeGovernedMultimediaWorkspaceRequest request,
                                                                                         HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.previewOfficeGovernedMultimediaWorkspace(processoId, request, httpServletRequest), List.of()));
    }

    @GetMapping("/office-mode")
    public ResponseEntity<ApiQueryResponse<?>> officeMode(HttpServletRequest request) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.officeMode(request), List.of()));
    }

    @PutMapping("/office-mode")
    public ResponseEntity<ApiQueryResponse<?>> updateOfficeMode(@Valid @RequestBody FrontendOfficeModeUpdateRequest request,
                                                                HttpServletResponse response) {
        var view = applicationService.updateOfficeMode(request);
        writeOfficeCookies(response, view.mode(), view.activeEquipeId());
        return ResponseEntity.ok(apiResponseFactory.queryOk(view, List.of()));
    }

    @DeleteMapping("/office-mode")
    public ResponseEntity<ApiQueryResponse<?>> clearOfficeMode(HttpServletResponse response) {
        var view = applicationService.clearOfficeMode();
        expireOfficeCookies(response);
        return ResponseEntity.ok(apiResponseFactory.queryOk(view, List.of()));
    }


@GetMapping("/offices/workspace/legal-cockpit")
public ResponseEntity<ApiQueryResponse<?>> officeWorkspaceLegalCockpit(Authentication authentication,
                                                                       @org.springframework.web.bind.annotation.RequestParam(name = "processoId", required = false) Long processoId,
                                                                       @org.springframework.web.bind.annotation.RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                       @org.springframework.web.bind.annotation.RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to,
                                                                       HttpServletRequest request) {
    return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.officeWorkspaceLegalCockpit(authentication, request, from, to, processoId), List.of()));
}

@GetMapping("/offices/workspace/processes/{processoId}/reading-mode")
public ResponseEntity<ApiQueryResponse<?>> officeProcessReadingMode(@org.springframework.web.bind.annotation.PathVariable Long processoId,
                                                                    @org.springframework.web.bind.annotation.RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                    @org.springframework.web.bind.annotation.RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to,
                                                                    HttpServletRequest request) {
    return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.officeProcessReadingMode(processoId, request, from, to), List.of()));
}

@GetMapping("/offices/workspace/main-dashboard")
public ResponseEntity<ApiQueryResponse<?>> officeWorkspaceMainDashboard(Authentication authentication,
                                                                        @org.springframework.web.bind.annotation.RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                        @org.springframework.web.bind.annotation.RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to,
                                                                        HttpServletRequest request) {
    return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.officeWorkspaceMainDashboard(authentication, request, from, to), List.of()));
}

@GetMapping("/offices/workspace/executive-dashboard")
public ResponseEntity<ApiQueryResponse<?>> officeWorkspaceExecutiveDashboard(Authentication authentication,
                                                                             @org.springframework.web.bind.annotation.RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                             @org.springframework.web.bind.annotation.RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to,
                                                                             HttpServletRequest request) {
    return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.officeWorkspaceExecutiveDashboard(authentication, request, from, to), List.of()));
}

@GetMapping("/professional/workspace/executive-dashboard")
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','MAGISTRADO','SERVIDOR','SERVIDOR_FORUM','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
public ResponseEntity<ApiQueryResponse<?>> professionalWorkspaceExecutiveDashboard(Authentication authentication,
                                                                                   @org.springframework.web.bind.annotation.RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                                   @org.springframework.web.bind.annotation.RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
    return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.professionalWorkspaceExecutiveDashboard(authentication, from, to), List.of()));
}

@GetMapping("/professional/workspace/magistrature-executive-dashboard")
@PreAuthorize("hasAnyRole('JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','MAGISTRADO','SERVIDOR','SERVIDOR_FORUM','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
public ResponseEntity<ApiQueryResponse<?>> magistratureExecutiveDashboard(Authentication authentication,
                                                                          @org.springframework.web.bind.annotation.RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                          @org.springframework.web.bind.annotation.RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
    return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.magistratureExecutiveDashboard(authentication, from, to), List.of()));
}

@GetMapping("/professional/workspace/defensoria-executive-dashboard")
@PreAuthorize("hasAnyRole('DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL')")
public ResponseEntity<ApiQueryResponse<?>> defensoriaExecutiveDashboard(Authentication authentication,
                                                                        @org.springframework.web.bind.annotation.RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                        @org.springframework.web.bind.annotation.RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
    return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.defensoriaExecutiveDashboard(authentication, from, to), List.of()));
}

@GetMapping("/professional/workspace/procuradoria-executive-dashboard")
@PreAuthorize("hasAnyRole('PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL')")
public ResponseEntity<ApiQueryResponse<?>> procuradoriaExecutiveDashboard(Authentication authentication,
                                                                          @org.springframework.web.bind.annotation.RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                          @org.springframework.web.bind.annotation.RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
    return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.procuradoriaExecutiveDashboard(authentication, from, to), List.of()));
}

@GetMapping("/professional/workspace/organizational-executive-dashboard")
@PreAuthorize("hasAnyRole('DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','MAGISTRADO','SERVIDOR','SERVIDOR_FORUM','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
public ResponseEntity<ApiQueryResponse<?>> professionalOrganizationalExecutiveDashboard(Authentication authentication,
                                                                                        @org.springframework.web.bind.annotation.RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                                        @org.springframework.web.bind.annotation.RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
    return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.professionalOrganizationalExecutiveDashboard(authentication, from, to), List.of()));
}

@GetMapping("/professional/workspace/magistrature-organ-dashboard")
@PreAuthorize("hasAnyRole('JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','MAGISTRADO','SERVIDOR','SERVIDOR_FORUM','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
public ResponseEntity<ApiQueryResponse<?>> magistratureOrganExecutiveDashboard(Authentication authentication,
                                                                                @org.springframework.web.bind.annotation.RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                                @org.springframework.web.bind.annotation.RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
    return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.magistratureOrganExecutiveDashboard(authentication, from, to), List.of()));
}

@GetMapping("/professional/workspace/defensoria-organ-dashboard")
@PreAuthorize("hasAnyRole('DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL')")
public ResponseEntity<ApiQueryResponse<?>> defensoriaOrganExecutiveDashboard(Authentication authentication,
                                                                              @org.springframework.web.bind.annotation.RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                              @org.springframework.web.bind.annotation.RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
    return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.defensoriaOrganExecutiveDashboard(authentication, from, to), List.of()));
}

@GetMapping("/professional/workspace/procuradoria-organ-dashboard")
@PreAuthorize("hasAnyRole('PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL')")
public ResponseEntity<ApiQueryResponse<?>> procuradoriaOrganExecutiveDashboard(Authentication authentication,
                                                                                @org.springframework.web.bind.annotation.RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                                @org.springframework.web.bind.annotation.RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
    return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.procuradoriaOrganExecutiveDashboard(authentication, from, to), List.of()));
}

@GetMapping("/offices/team-members/{userId}/avatar")
public ResponseEntity<Resource> officeTeamMemberAvatar(@org.springframework.web.bind.annotation.PathVariable Long userId,
                                                       HttpServletRequest request) throws java.io.IOException {
    var result = applicationService.officeTeamMemberAvatar(userId, request);

    String etag = (result.etag() != null && !result.etag().isBlank()) ? '"' + result.etag() + '"' : null;

    String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
    if (etag != null && etag.equals(ifNoneMatch)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .eTag(etag)
                .cacheControl(CacheControl.noCache().cachePrivate())
                .build();
    }

    String ct = (result.contentType() == null || result.contentType().isBlank())
            ? MediaType.IMAGE_JPEG_VALUE
            : result.contentType();

    ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(ct))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .header("X-Content-Type-Options", "nosniff");

    if (etag != null) {
        builder = builder.eTag(etag).cacheControl(CacheControl.noCache().cachePrivate());
    } else {
        builder = builder.cacheControl(CacheControl.noStore());
    }

    return builder.body(new ByteArrayResource(result.bytes()));
}

    @GetMapping("/bootstrap")
    public ResponseEntity<ApiQueryResponse<?>> bootstrap(Authentication authentication, HttpServletRequest request) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.bootstrap(authentication, request), List.of()));
    }

    private void writeOfficeCookies(HttpServletResponse response, String mode, Long equipeId) {
        response.addHeader("Set-Cookie", ResponseCookie.from(OfficeWorkspaceModeService.COOKIE_MODE, mode)
                .httpOnly(false)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .build()
                .toString());
        if (equipeId != null) {
            response.addHeader("Set-Cookie", ResponseCookie.from(OfficeWorkspaceModeService.COOKIE_EQUIPE, Long.toString(equipeId))
                    .httpOnly(false)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .build()
                    .toString());
        } else {
            response.addHeader("Set-Cookie", ResponseCookie.from(OfficeWorkspaceModeService.COOKIE_EQUIPE, "")
                    .httpOnly(false)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(0)
                    .build()
                    .toString());
        }
    }

    private void expireOfficeCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", ResponseCookie.from(OfficeWorkspaceModeService.COOKIE_MODE, "")
                .httpOnly(false)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build()
                .toString());
        response.addHeader("Set-Cookie", ResponseCookie.from(OfficeWorkspaceModeService.COOKIE_EQUIPE, "")
                .httpOnly(false)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build()
                .toString());
    }
}
