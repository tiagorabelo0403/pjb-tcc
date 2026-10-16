package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.DescongelamentoContaService;
import com.tcc.pjb.backend.model.dto.security.DualApprovalResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SecurityDualApprovalRequest;
import com.tcc.pjb.backend.service.security.device.TrustedDeviceVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/security/contas-congeladas")
@PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
public class AdminContaCongeladaController {

    private final CurrentUserService currentUserService;
    private final DescongelamentoContaService descongelamentoContaService;
    private final TrustedDeviceVerificationService deviceVerificationService;

    public AdminContaCongeladaController(CurrentUserService currentUserService,
                                         DescongelamentoContaService descongelamentoContaService,
                                         TrustedDeviceVerificationService deviceVerificationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.descongelamentoContaService = Objects.requireNonNull(descongelamentoContaService);
        this.deviceVerificationService = Objects.requireNonNull(deviceVerificationService);
    }

    @PostMapping("/{usuarioId}/descongelamento/solicitacao")
    public ResponseEntity<DualApprovalResponse> solicitar(@PathVariable Long usuarioId, HttpServletRequest request) {
        Usuario administrador = currentUserService.getRequired();
        Long dispositivoId = deviceVerificationService.requireStrongAuthOnTrustedDevice(administrador, request);
        SecurityDualApprovalRequest r = descongelamentoContaService.solicitar(administrador, dispositivoId, usuarioId, request.getRequestURI());
        return ResponseEntity.ok(DualApprovalResponse.builder()
                .id(r.getId())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .action(r.getAction())
                .method(r.getMethod())
                .path(r.getPath())
                .ruleId(r.getRuleId())
                .createdAt(r.getCreatedAt())
                .expiresAt(r.getExpiresAt())
                .requesterUserId(administrador.getId())
                .requesterDeviceId(r.getRequesterDeviceId())
                .build());
    }

    @PostMapping("/{usuarioId}/descongelamento")
    public ResponseEntity<Void> executar(@PathVariable Long usuarioId,
                                         @RequestParam("aprovacao") Long aprovacaoId,
                                         HttpServletRequest request) {
        Usuario administrador = currentUserService.getRequired();
        deviceVerificationService.requireStrongAuthOnTrustedDevice(administrador, request);
        descongelamentoContaService.executar(administrador, aprovacaoId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
