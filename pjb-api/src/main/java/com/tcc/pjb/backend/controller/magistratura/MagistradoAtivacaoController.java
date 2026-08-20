package com.tcc.pjb.backend.controller.magistratura;

import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.service.magistratura.MagistradoAtivacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/magistratura/ativacao")
@PreAuthorize("permitAll()")
public class MagistradoAtivacaoController {

    public record ConfirmarAtivacaoRequest(
            @NotNull Long usuarioId,
            @NotNull Long challengeId,
            @NotBlank String codigo
    ) {}

    public record AtivacaoConfirmadaResponse(String token, LocalDateTime expiresAt, boolean termosPendentes) {}

    private final MagistradoAtivacaoService service;

    public MagistradoAtivacaoController(MagistradoAtivacaoService service) {
        this.service = Objects.requireNonNull(service);
    }

    @PostMapping("/confirmar")
    public ResponseEntity<AtivacaoConfirmadaResponse> confirmar(@Valid @RequestBody ConfirmarAtivacaoRequest request,
                                                                  HttpServletRequest servletRequest) {
        PasskeySessionService.IssuedPasskeySession sessao = service.confirmarAtivacao(
                request.usuarioId(), request.challengeId(), request.codigo(), servletRequest.getRemoteAddr());
        return ResponseEntity.ok(new AtivacaoConfirmadaResponse(sessao.token(), sessao.expiresAt(), sessao.termosPendentes()));
    }
}
