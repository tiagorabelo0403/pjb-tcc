package com.tcc.pjb.backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArquiteturaCodigoFonteScannerTest {

    @Test
    void naoDeveExistirDuplicidadeDeNomeSimplesNoCodigoFontePrincipal() throws IOException {
        Path root = Path.of("src/main/java");

        Map<String, List<String>> duplicated = ArquiteturaSourceScanSupport.duplicateFileNames(root);

        assertThat(duplicated).isEmpty();
    }
}
