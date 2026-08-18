package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbNoIdInCreateRequestTest {

    @Test
    void create_requests_nao_devem_ter_campo_id() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.tcc.pjb.backend");

        List<String> violacoes = classes.stream()
                .filter(c -> {
                    String name = c.getSimpleName();
                    return name.endsWith("CreateRequest") || name.endsWith("CriarRequest");
                })
                .filter(c -> c.getFields().stream().anyMatch(f -> "id".equals(f.getName())))
                .map(JavaClass::getSimpleName)
                .sorted()
                .toList();

        assertThat(violacoes)
                .as("CreateRequests com campo 'id' detectados. " +
                    "O identificador deve ser gerado pelo servidor — use idempotencyKey se precisar de chave de idempotência.")
                .isEmpty();
    }
}
