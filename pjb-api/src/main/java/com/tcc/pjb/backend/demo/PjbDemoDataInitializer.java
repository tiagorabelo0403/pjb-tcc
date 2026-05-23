package com.tcc.pjb.backend.demo;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "pjb.demo.seed-enabled", havingValue = "true")
public class PjbDemoDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(PjbDemoDataInitializer.class);

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public PjbDemoDataInitializer(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        try {
            seedUsuarios();
            seedProcessos();
            log.info("[demo] Seed de dados concluído.");
        } catch (Exception e) {
            log.warn("[demo] Seed falhou (não bloqueante): {}", e.getMessage());
        }
    }

    private static String e(String local) {
        return local + "@pjb.demo";
    }

    private void seedUsuarios() {
        String hash = passwordEncoder.encode("demo123");
        List<Object[]> usuarios = List.of(
            new Object[]{"Juíza Ana Beatriz Costa", e("ana.costa"), "MAGISTRADO", hash, "12345678901", "MAGISTRADO"},
            new Object[]{"Dr. Carlos Mendes (Advogado)", e("carlos.mendes"), "ADVOGADO", hash, "23456789012", "ADVOGADO"},
            new Object[]{"Servidora Paula Rocha", e("paula.rocha"), "SERVIDOR", hash, "34567890123", "SERVIDOR"},
            new Object[]{"Admin Sistema PJB", e("admin"), "ADMINISTRADOR", hash, "45678901234", "ADMINISTRADOR"}
        );
        for (Object[] u : usuarios) {
            Integer exists = jdbc.queryForObject(
                "SELECT COUNT(1) FROM tb_usuario WHERE cpf = ?", Integer.class, u[4]);
            if (exists != null && exists == 0) {
                jdbc.update(
                    "INSERT INTO tb_usuario (nome, email, perfil, senha, cpf, ativo, tipo_usuario) VALUES (?,?,?,?,?,true,?)",
                    u);
            }
        }
    }

    private void seedProcessos() {
        List<Object[]> processos = List.of(
            new Object[]{"0000001-12.2025.8.06.0001", "DISTRIBUIDO"},
            new Object[]{"0000002-24.2025.8.06.0001", "PAUTADO"},
            new Object[]{"0000003-36.2025.1.01.0000", "AUTUADO"}
        );
        for (Object[] p : processos) {
            Integer exists = jdbc.queryForObject(
                "SELECT COUNT(1) FROM tb_processo WHERE numero_processo = ?", Integer.class, p[0]);
            if (exists != null && exists == 0) {
                jdbc.update(
                    "INSERT INTO tb_processo (numero_processo, status_processo) VALUES (?,?)", p);
            }
        }
    }
}
