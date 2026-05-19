package com.tcc.pjb.backend.modules.acordo;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "com.tcc.pjb.backend.modules.acordo", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class AcordoArchitectureTest {

    @ArchTest
    static final ArchRule domain_nao_depende_de_spring_ou_jpa =
            noClasses().that().resideInAPackage("..modules.acordo.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule application_nao_depende_de_web =
            noClasses().that().resideInAPackage("..modules.acordo.application..")
                    .should().dependOnClassesThat().resideInAnyPackage("..modules.acordo.web..", "org.springframework.web..");

    @ArchTest
    static final ArchRule controller_nao_acessa_repository_se_existir =
            noClasses().that().resideInAPackage("..modules.acordo.web..")
                    .and().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                    .allowEmptyShould(true);
}
