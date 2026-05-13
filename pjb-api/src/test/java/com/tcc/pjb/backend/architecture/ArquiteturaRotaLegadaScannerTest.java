package com.tcc.pjb.backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArquiteturaRotaLegadaScannerTest {

    @Test
    void naoDeveManterDuplicidadesDeNomeSimplesEntreClassesJava() throws IOException {
        Path root = Path.of("src/main/java");
        assertThat(Files.exists(root)).isTrue();

        Map<String, List<String>> repeated = ArquiteturaSourceScanSupport.duplicateSimpleClassNames(root);

        assertThat(repeated).isEmpty();
    }
}
