package com.tcc.pjb.backend.controller.demo;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
public class PjbDemoStatusController {

    private final JdbcTemplate jdbc;

    public PjbDemoStatusController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("sistema", "PJB — Plataforma Judicial Brasileira");
        body.put("timestamp", Instant.now().toString());
        body.put("stats", collectStats());
        return body;
    }

    private Map<String, Object> collectStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            stats.put("usuarios", count("SELECT COUNT(1) FROM tb_usuario"));
            stats.put("processos", count("SELECT COUNT(1) FROM tb_processo"));
            stats.put("documentos", count("SELECT COUNT(1) FROM tb_documento_processual"));
        } catch (Exception e) {
            stats.put("erro", "Dados não disponíveis: " + e.getMessage());
        }
        return stats;
    }

    private long count(String sql) {
        Long result = jdbc.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }
}
