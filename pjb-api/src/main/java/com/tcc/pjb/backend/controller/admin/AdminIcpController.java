package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.core.icp.IcpBrasilApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/icp")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminIcpController {

    private final IcpBrasilApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminIcpController(IcpBrasilApplicationService applicationService,
                              ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @GetMapping("/policy")
    public ResponseEntity<ApiQueryResponse<?>> policy() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.policy(), List.of()));
    }

    @GetMapping("/validation/health")
    public ResponseEntity<ApiQueryResponse<?>> validationHealth() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.validationHealth(), List.of()));
    }

    @GetMapping("/trust-anchors/{acSigla}")
    public ResponseEntity<ApiQueryResponse<?>> trustAnchor(@PathVariable String acSigla) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.trustAnchor(acSigla), List.of()));
    }

    @GetMapping("/trust-anchors/health")
    public ResponseEntity<ApiQueryResponse<?>> trustAnchorHealth(@RequestParam(value = "alias", required = false) String alias,
                                                                 @RequestParam("acSigla") String acSigla) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.trustAnchorHealth(alias, acSigla), List.of()));
    }

    @GetMapping("/certificates/health")
    public ResponseEntity<ApiQueryResponse<?>> certificateHealth(@RequestParam("issuerDn") String issuerDn,
                                                                 @RequestParam("serialHex") String serialHex) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.certificateHealth(issuerDn, serialHex), List.of()));
    }

    @GetMapping("/ocsp/health")
    public ResponseEntity<ApiQueryResponse<?>> ocspHealth() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.ocspHealth(), List.of()));
    }

    @GetMapping("/signer/selection")
    public ResponseEntity<ApiQueryResponse<?>> signerSelection(@RequestParam("source") String source,
                                                               @RequestParam("keyAlias") String keyAlias,
                                                               @RequestParam(value = "available", defaultValue = "true") boolean available,
                                                               @RequestParam(value = "pkcs11", defaultValue = "false") boolean pkcs11) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.signerSelection(source, keyAlias, available, pkcs11), List.of()));
    }

    @GetMapping("/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@RequestParam(value = "docHash", required = false) String docHash) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(docHash), List.of()));
    }
}
