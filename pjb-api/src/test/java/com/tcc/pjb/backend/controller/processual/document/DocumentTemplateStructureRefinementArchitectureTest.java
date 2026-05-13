package com.tcc.pjb.backend.controller.processual.document;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DocumentTemplateStructureRefinementArchitectureTest {

    private static final Path CONTROLLER_ROOT = Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/document");
    private static final Path CONTROLLER_TEMPLATE = CONTROLLER_ROOT.resolve("template");
    private static final Path DTO_ROOT = Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/document");
    private static final Path DTO_TEMPLATE = DTO_ROOT.resolve("template");

    @Test
    void controllerAndDtoTemplatePackagesMustExistAndRootsMustBeClean() throws IOException {
        assertTrue(Files.isDirectory(CONTROLLER_TEMPLATE), "controller template package must exist");
        assertTrue(Files.isDirectory(DTO_TEMPLATE), "dto template package must exist");
        assertFalse(hasJavaFiles(CONTROLLER_ROOT), "controller document root must not keep java files");
        assertFalse(hasJavaFiles(DTO_ROOT), "dto document root must not keep java files");
    }

    private boolean hasJavaFiles(Path root) throws IOException {
        try (Stream<Path> stream = Files.list(root)) {
            return stream.anyMatch(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java"));
        }
    }
}
