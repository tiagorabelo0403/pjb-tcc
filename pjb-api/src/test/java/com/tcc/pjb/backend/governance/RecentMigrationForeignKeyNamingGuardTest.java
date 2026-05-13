package com.tcc.pjb.backend.governance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class RecentMigrationForeignKeyNamingGuardTest {

    @Test
    void recent_migrations_use_tb_usuario_foreign_keys() throws Exception {
        assertMigration("db/migration/V153__precedente_vinculante_and_escritura_extrajudicial.sql");
        assertMigration("db/migration/V154__marketplace_oauth_inquerito_digital_plenario_avancado.sql");
        assertMigration("db/migration/V155__offline_voice_webrtc_homomorphic_zk.sql");
        assertMigration("db/migration/V164__decision_stepup_and_client_binding.sql");
    }

    private void assertMigration(String path) throws IOException {
        String sql = new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertFalse(sql.contains("REFERENCES usuarios (id)"), path + " não pode referenciar a tabela errada usuarios.");
        assertTrue(sql.contains("REFERENCES tb_usuario (id)"), path + " deve referenciar tb_usuario.");
    }
}
