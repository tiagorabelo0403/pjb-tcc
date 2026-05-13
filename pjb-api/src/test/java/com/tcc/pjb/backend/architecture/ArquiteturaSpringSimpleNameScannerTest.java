package com.tcc.pjb.backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ArquiteturaSpringSimpleNameScannerTest {

    private static final Pattern STEREOTYPE = Pattern.compile("@(Service|Component|Repository|Controller|RestController|Configuration)\\b");

    @Test
    void naoDeveHaverColisaoDeNomeSimplesEntreBeansSpring() throws IOException {
        Path root = Path.of("src/main/java");
        assertThat(Files.exists(root)).isTrue();

        Map<String, List<String>> repeated = ArquiteturaSourceScanSupport.duplicateSimpleClassNames(root, this::containsSpringStereotype);

        assertThat(repeated).isEmpty();
    }

    private boolean containsSpringStereotype(Path path) {
        try {
            return STEREOTYPE.matcher(Files.readString(path)).find();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
