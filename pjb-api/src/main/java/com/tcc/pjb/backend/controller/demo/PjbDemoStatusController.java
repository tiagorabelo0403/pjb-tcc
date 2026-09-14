package com.tcc.pjb.backend.controller.demo;

import com.tcc.pjb.backend.service.demo.DemoAcervoContagemService;
import com.tcc.pjb.backend.service.demo.DemoAcervoContagemService.ContagensDoAcervo;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
public class PjbDemoStatusController {

    /** Texto fixo, sem causa. O motivo da indisponibilidade fica no log do servidor. */
    private static final String STATS_INDISPONIVEIS = "Dados não disponíveis";

    private final DemoAcervoContagemService contagemService;

    public PjbDemoStatusController(DemoAcervoContagemService contagemService) {
        this.contagemService = Objects.requireNonNull(contagemService);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Optional<ContagensDoAcervo> contagens = contagemService.contar();

        Map<String, Object> body = new LinkedHashMap<>();
        // "UP" so quando o acervo foi lido. Antes o campo era constante: com o banco fora do ar o
        // painel continuava anunciando UP e escondia a indisponibilidade dentro de stats.
        body.put("status", contagens.isPresent() ? "UP" : "DEGRADED");
        body.put("sistema", "PJB — Plataforma Judicial Brasileira");
        body.put("timestamp", Instant.now().toString());
        body.put("stats", contagens.map(PjbDemoStatusController::comContagens)
                .orElseGet(PjbDemoStatusController::indisponivel));
        return body;
    }

    private static Map<String, Object> comContagens(ContagensDoAcervo contagens) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("usuarios", contagens.usuarios());
        stats.put("processos", contagens.processos());
        stats.put("documentos", contagens.documentos());
        return stats;
    }

    private static Map<String, Object> indisponivel() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("erro", STATS_INDISPONIVEIS);
        return stats;
    }
}
