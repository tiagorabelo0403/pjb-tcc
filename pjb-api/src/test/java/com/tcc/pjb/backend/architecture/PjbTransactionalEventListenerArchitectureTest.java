package com.tcc.pjb.backend.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PjbTransactionalEventListenerArchitectureTest {

    private static final Pattern TRANSACTIONAL_EVENT_LISTENER = Pattern.compile("@TransactionalEventListener");
    private static final Pattern UNSAFE_TRANSACTIONAL = Pattern.compile("@Transactional(\\s*(?:\\r?\\n|$))");
    private static final Pattern SAFE_TRANSACTIONAL = Pattern.compile("@Transactional\\s*\\([^)]*propagation\\s*=\\s*Propagation\\.(REQUIRES_NEW|NOT_SUPPORTED)[^)]*\\)", Pattern.DOTALL);

    @Test
    void transactionalEventListenerNaoPodeUsarTransacaoAmbigua() throws IOException {
        Path root = Path.of("src/main/java");
        List<String> violations = new ArrayList<>();
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> inspect(path, violations));
        }
        assertTrue(violations.isEmpty(), () -> String.join(System.lineSeparator(), violations));
    }

    private static void inspect(Path path, List<String> violations) {
        String source;
        try {
            source = Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        if (!TRANSACTIONAL_EVENT_LISTENER.matcher(source).find()) {
            return;
        }
        String[] lines = source.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].contains("@TransactionalEventListener")) {
                continue;
            }
            String window = window(lines, i, Math.min(lines.length, i + 8));
            if (UNSAFE_TRANSACTIONAL.matcher(window).find() && !SAFE_TRANSACTIONAL.matcher(window).find()) {
                violations.add(path + ":" + (i + 1));
            }
        }
    }

    private static String window(String[] lines, int startInclusive, int endExclusive) {
        StringBuilder builder = new StringBuilder();
        for (int i = startInclusive; i < endExclusive; i++) {
            builder.append(lines[i]).append('\n');
        }
        return builder.toString();
    }
}
