package com.tcc.pjb.backend.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@AnalyzeClasses(packages = "com.tcc.pjb.backend.modules", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class ModularMonolithArchitectureTest {

    @ArchTest
    static final ArchRule domain_nao_depende_de_spring =
            noClasses().that().resideInAPackage("..modules..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..", "javax.persistence..");

    @ArchTest
    static final ArchRule domain_nao_depende_de_web =
            noClasses().that().resideInAPackage("..modules..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("..web..", "..controller..", "..controllers..", "org.springframework.web..");

    @ArchTest
    static final ArchRule domain_nao_depende_de_infrastructure =
            noClasses().that().resideInAPackage("..modules..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..modules..infrastructure..");

    @ArchTest
    static final ArchRule application_nao_depende_de_web =
            noClasses().that().resideInAPackage("..modules..application..")
                    .should().dependOnClassesThat().resideInAnyPackage("..modules..web..", "..controller..", "..controllers..", "org.springframework.web..");

    @ArchTest
    static final ArchRule application_nao_acessa_repository =
            noClasses().that().resideInAPackage("..modules..application..")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule infrastructure_nao_depende_de_web =
            noClasses().that().resideInAPackage("..modules..infrastructure..")
                    .should().dependOnClassesThat().resideInAnyPackage("..modules..web..", "..controller..", "..controllers..", "org.springframework.web..");

    @ArchTest
    static final ArchRule web_nao_acessa_repository =
            noClasses().that().resideInAPackage("..modules..web..")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule adapters_ficam_em_infrastructure =
            classes().that().resideInAPackage("..modules..")
                    .and().haveSimpleNameEndingWith("Adapter")
                    .should().resideInAPackage("..modules..infrastructure..");

    @ArchTest
    static final ArchRule ports_nao_retornam_entity_legada =
            noMethods().that().areDeclaredInClassesThat().resideInAPackage("..modules..api..")
                    .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Port")
                    .should().haveRawReturnType(resideInAPackage("..model.entity.."));

    @ArchTest
    static final ArchRule controllers_nao_acessam_domain_de_modulos =
            noClasses().that().resideInAnyPackage("..controller..", "..controllers..")
                    .should().dependOnClassesThat().resideInAPackage("..modules..domain..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule modules_nao_devem_ter_ciclos =
            slices().matching("com.tcc.pjb.backend.modules.(*)..")
                    .should().beFreeOfCycles()
                    .ignoreDependency(resideInAPackage("..modules.advocacia.."), resideInAPackage("..modules.laiane.."))
                    .ignoreDependency(resideInAPackage("..modules.laiane.."), resideInAPackage("..modules.advocacia.."))
                    .ignoreDependency(resideInAPackage("..modules.laiane.."), resideInAPackage("..modules.auditoria.."))
                    .ignoreDependency(resideInAPackage("..modules.auditoria.."), resideInAPackage("..modules.advocacia.."));

    @ArchTest
    static final ArchRule classes_de_domain_nao_sao_entity =
            noClasses().that().resideInAPackage("..modules..domain..")
                    .should().beAnnotatedWith(Entity.class);

    @ArchTest
    static final ArchRule classes_de_domain_nao_sao_repository =
            noClasses().that().resideInAPackage("..modules..domain..")
                    .should().beAnnotatedWith(Repository.class);

    @ArchTest
    static final ArchRule application_services_usam_sufixo_application_service =
            classes().that().resideInAPackage("..modules..application..")
                    .and().areAnnotatedWith(Service.class)
                    .and().haveSimpleNameNotEndingWith("BridgeService")
                    .should().haveSimpleNameEndingWith("ApplicationService");
}
