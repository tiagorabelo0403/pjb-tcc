package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.util.List;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class PjbNoIdInCreateRequestTest {

    @ArchTest
    static void create_requests_nao_devem_ter_campo_id(JavaClasses classes) {
        List<JavaClass> createRequests = classes.stream()
                .filter(c -> {
                    String name = c.getSimpleName();
                    return name.endsWith("CreateRequest") || name.endsWith("CriarRequest");
                })
                .toList();

        assertThat(createRequests)
                .as("nenhum CreateRequest/CriarRequest encontrado: a regra abaixo passaria sem verificar nada")
                .isNotEmpty();

        List<String> violacoes = createRequests.stream()
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
