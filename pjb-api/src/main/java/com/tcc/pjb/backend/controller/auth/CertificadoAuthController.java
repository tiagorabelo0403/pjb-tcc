package com.tcc.pjb.backend.controller.auth;

import com.tcc.pjb.backend.model.dto.security.CertificadoAuthDtos;
import com.tcc.pjb.backend.service.auth.surface.CertificadoAuthFacadeService;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/certificado")
@PreAuthorize("permitAll()")
public class CertificadoAuthController {

    private final CertificadoAuthFacadeService facadeService;

    public CertificadoAuthController(CertificadoAuthFacadeService facadeService) {
        this.facadeService = Objects.requireNonNull(facadeService);
    }

    @PostMapping("/desafio")
    public ResponseEntity<CertificadoAuthDtos.DesafioResponse> desafio(
            @Valid @RequestBody CertificadoAuthDtos.DesafioRequest request
    ) {
        CertificadoAuthDtos.DesafioResponse response = facadeService.emitirDesafio(request);
        return ResponseEntity.status(status(response.status())).body(response);
    }

    @PostMapping("/resposta")
    public ResponseEntity<CertificadoAuthDtos.Resposta> resposta(
            @Valid @RequestBody CertificadoAuthDtos.RespostaRequest request
    ) {
        CertificadoAuthDtos.Resposta response = facadeService.responder(request);
        return ResponseEntity.status(status(response.status())).body(response);
    }

    private static HttpStatus status(CertificadoAuthDtos.Status status) {
        return switch (status) {
            case DESAFIO_EMITIDO, AUTENTICADO, PENDENTE_SELECAO -> HttpStatus.OK;
            case INDISPONIVEL -> HttpStatus.SERVICE_UNAVAILABLE;
            case NEGADO -> HttpStatus.UNAUTHORIZED;
        };
    }
}
