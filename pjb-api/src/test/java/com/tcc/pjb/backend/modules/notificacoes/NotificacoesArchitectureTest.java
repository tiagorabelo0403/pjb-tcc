package com.tcc.pjb.backend.modules.notificacoes;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.tcc.pjb.backend.modules.notificacoes", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class NotificacoesArchitectureTest {

    @ArchTest
    static final ArchRule domain_nao_depende_de_spring_ou_jpa =
            noClasses().that().resideInAPackage("..modules.notificacoes.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..", "javax.persistence..");

    @ArchTest
    static final ArchRule application_nao_depende_de_web =
            noClasses().that().resideInAPackage("..modules.notificacoes.application..")
                    .should().dependOnClassesThat().resideInAnyPackage("..modules.notificacoes.web..", "org.springframework.web..");

    @ArchTest
    static final ArchRule application_nao_acessa_repository =
            noClasses().that().resideInAPackage("..modules.notificacoes.application..")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule infrastructure_nao_depende_de_web =
            noClasses().that().resideInAPackage("..modules.notificacoes.infrastructure..")
                    .should().dependOnClassesThat().resideInAnyPackage("..modules.notificacoes.web..", "org.springframework.web..");

    @ArchTest
    static final ArchRule ports_nao_retornam_entity_legada =
            noMethods().that().areDeclaredInClassesThat().resideInAPackage("..modules.notificacoes.api..")
                    .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Port")
                    .should().haveRawReturnType(com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage("..model.entity.."));
}
