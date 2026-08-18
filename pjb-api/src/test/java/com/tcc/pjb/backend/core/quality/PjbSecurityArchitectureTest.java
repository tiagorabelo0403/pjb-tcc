package com.tcc.pjb.backend.core.quality;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.tcc.pjb.backend.modules.laiane",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class PjbSecurityArchitectureTest {

    @ArchTest
    static final ArchRule laiane_controllers_nao_devem_injetar_repository =
            noClasses().that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .because("Controllers LAIANE devem acessar dados exclusivamente via services");

    @ArchTest
    static final ArchRule laiane_entidades_nao_devem_usar_uuid_random =
            noClasses().that().resideInAPackage("..modules.laiane.entity..")
                    .should().callMethod(java.util.UUID.class, "randomUUID")
                    .because("Entidades LAIANE devem usar PjbUuidV7Generator.generate() para UUIDs rastreáveis (UUIDv7)");
}
