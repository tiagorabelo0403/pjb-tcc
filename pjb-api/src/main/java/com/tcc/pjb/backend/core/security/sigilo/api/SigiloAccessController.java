package com.tcc.pjb.backend.core.security.sigilo.api;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.sigilo.SigiloAccessRequest;
import com.tcc.pjb.backend.core.security.sigilo.api.dto.*;
import com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService;
import com.tcc.pjb.backend.model.entity.Usuario;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/sigilo/acesso")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SigiloAccessController {

    private final SigiloAccessService sigiloAccessService;
    private final CurrentUserService currentUserService;

    @PostMapping("/solicitacoes")
    public SigiloSolicitacaoResponse criar(@Valid @RequestBody SigiloSolicitacaoCreateRequest req) {
        SigiloAccessRequest saved = sigiloAccessService.criarSolicitacao(req.getProcessoId(), req.getMotivo());
        return toView(saved, currentUserService.get());
    }

    @GetMapping("/solicitacoes/me")
    public List<SigiloSolicitacaoResponse> minhasSolicitacoes() {
        Usuario viewer = currentUserService.get();
        return sigiloAccessService.listarMinhasSolicitacoes().stream().map(r -> toView(r, viewer)).toList();
    }

    @GetMapping("/solicitacoes/{id}")
    public SigiloSolicitacaoResponse detalhe(@PathVariable UUID id) {
        Usuario viewer = currentUserService.get();
        SigiloAccessRequest r = sigiloAccessService.buscarPorId(id);

        
        boolean backoffice = sigiloAccessService.viewerPodeVerAprovador(viewer);
        if (!backoffice && (viewer.getId() == null || !viewer.getId().equals(r.getAdvogadoId()))) {
            throw new SecurityException("Acesso negado.");
        }
        return toView(r, viewer);
    }

    @PostMapping("/solicitacoes/{id}/aprovar")
    public SigiloAprovacaoResponse aprovar(@PathVariable UUID id) {
        var result = sigiloAccessService.aprovarSolicitacao(id);
        SigiloAccessRequest r = result.request();
        return SigiloAprovacaoResponse.builder()
                .id(r.getId())
                .processoId(r.getProcessoId())
                .status(r.getStatus().name())
                .approvedAt(r.getApprovedAt())
                .expiresAt(r.getExpiresAt())
                .senhaTemporaria(result.plainPassword())
                .build();
    }

    @PostMapping("/solicitacoes/{id}/rejeitar")
    public SigiloSolicitacaoResponse rejeitar(@PathVariable UUID id, @RequestBody SigiloDecisaoRequest body) {
        SigiloAccessRequest r = sigiloAccessService.rejeitarSolicitacao(id, body != null ? body.getMotivo() : null);
        return toView(r, currentUserService.get());
    }

    @PostMapping("/solicitacoes/{id}/revogar")
    public SigiloSolicitacaoResponse revogar(@PathVariable UUID id, @RequestBody SigiloDecisaoRequest body) {
        SigiloAccessRequest r = sigiloAccessService.revogarSolicitacao(id, body != null ? body.getMotivo() : null);
        return toView(r, currentUserService.get());
    }

    private SigiloSolicitacaoResponse toView(SigiloAccessRequest r, Usuario viewer) {
        boolean canSeeApprover = sigiloAccessService.viewerPodeVerAprovador(viewer);
        String aprovador = null;
        if (r.getApprovedBy() != null) {
            aprovador = canSeeApprover ? String.valueOf(r.getApprovedBy()) : "OCULTO";
        }

        return SigiloSolicitacaoResponse.builder()
                .id(r.getId())
                .processoId(r.getProcessoId())
                .status(r.getStatus().name())
                .requestedAt(r.getRequestedAt())
                .approvedAt(r.getApprovedAt())
                .expiresAt(r.getExpiresAt())
                .aprovadoPor(aprovador)
                .rejectedReason(r.getRejectedReason())
                .build();
    }
}
