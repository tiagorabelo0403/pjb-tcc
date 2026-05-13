package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PjbInstitutionalDuplicateRouteGuardTest {

    private static final Pattern METHOD_MAPPING = Pattern.compile("@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)\\(([^\\)]*)\\)");

    @Test
    void institutionalControllersMustNotExposeDuplicateExactMethodMappings() throws IOException {
        Map<String, Path> seen = new LinkedHashMap<>();
        for (Path controller : ApiSurfaceTestSupport.controllerFiles().stream()
                .filter(path -> path.getFileName().toString().startsWith("NationalCommunicationInstitutional"))
                .toList()) {
            String content = ApiSurfaceTestSupport.read(controller);
            Matcher matcher = METHOD_MAPPING.matcher(content);
            while (matcher.find()) {
                String signature = matcher.group(1) + "::" + matcher.group(2).trim();
                Path previous = seen.putIfAbsent(signature, controller);
                assertEquals(null, previous, () -> "Mapeamento institucional duplicado: " + signature + " em " + previous + " e " + controller);
            }
        }
    }
}
