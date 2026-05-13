package com.tcc.pjb.backend.core.plataforma.substituicao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoFlywayVersionUniquenessTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    @Test
    void migration_versions_must_be_unique_and_ordered_for_recent_substituicao_band() throws IOException {
        Map<String, Integer> versions = new LinkedHashMap<>();
        List<String> files = Files.list(MIGRATIONS)
                .map(path -> path.getFileName().toString())
                .filter(name -> name.matches("V\\d+__.*\\.sql"))
                .sorted()
                .toList();
        for (String file : files) {
            String version = file.substring(1, file.indexOf("__"));
            versions.merge(version, 1, Integer::sum);
        }
        assertTrue(versions.containsKey("217"));
        assertTrue(versions.containsKey("220"));
        assertEquals(1, versions.get("217"));
        assertEquals(1, versions.get("218"));
        assertEquals(1, versions.get("219"));
        assertEquals(1, versions.get("220"));
        assertFalse(files.indexOf("V220__pjb_substituicao_nacional_execution_hardening.sql") < files.indexOf("V217__pjb_substituicao_nacional_execucao.sql"));
    }
}
