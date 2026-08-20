package com.tcc.pjb.backend.controller.auth;

import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.security.webauthn.TermosAceiteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/termos")
@PreAuthorize("permitAll()")
public class TermosAceiteController {

    private final TermosAceiteService termosAceiteService;
    private final ClientIpResolver clientIpResolver;

    public TermosAceiteController(TermosAceiteService termosAceiteService, ClientIpResolver clientIpResolver) {
        this.termosAceiteService = Objects.requireNonNull(termosAceiteService);
        this.clientIpResolver = Objects.requireNonNull(clientIpResolver);
    }

    @PostMapping("/aceitar")
    public ResponseEntity<Map<String, Object>> aceitar(@RequestBody AceitarTermosRequest request, HttpServletRequest servletRequest) {
        termosAceiteService.confirmarAceitePorToken(request.token(), request.versao(), clientIpResolver.resolve(servletRequest));
        return ResponseEntity.ok(Map.of(
                "status", "TERMOS_ACEITOS",
                "versao", termosAceiteService.versaoAtual()
        ));
    }

    public record AceitarTermosRequest(@NotBlank String token, String versao) {
    }
}
