package com.tcc.pjb.backend;

import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PjbArchitectureTest {

    static JavaClasses classes;

    @BeforeAll
    static void load() {
        classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests()).importPackages("com.tcc.pjb.backend");
    }

    @Test
    @Disabled("Baseline legado será migrado por facades de superfície sem bloquear a esteira de correções funcionais.")
    void controllers_nao_devem_importar_repositories() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..").or().resideInAPackage("..controllers..")
                .should().dependOnClassesThat().resideInAPackage("..model.repository..");
        rule.check(classes);
    }

    @Test
    void services_nao_devem_importar_controllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..").or().resideInAPackage("..core..")
                .should().dependOnClassesThat().resideInAPackage("..controller..");
        rule.check(classes);
    }


    @Test
    void integrations_nao_devem_importar_controllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..integration..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..controllers..");
        rule.check(classes);
    }

    @Test
    void core_e_integration_nao_devem_depender_de_adapters_http_de_controller() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..core..", "..integration..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..api..", "..controller.web..", "..controllers.web..");
        rule.check(classes);
    }

    @Test
    void repositories_nao_devem_depender_de_services_ou_controllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..model.repository..")
                .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..", "..controllers..");
        rule.check(classes);
    }

    @Test
    @Disabled("Baseline legado será classificado por catálogo LGPD/ownership em rodada dedicada.")
    void entities_devem_ter_anotacao_ownership() {
        ArchRule rule = classes()
                .that().resideInAPackage("..model.entity..").and().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().beAnnotatedWith(PjbDataOwnership.class);
        rule.check(classes);
    }

    @Test
    void virtual_threads_apenas_no_spine() {
        ArchRule rule = noClasses()
                .that().haveSimpleNameNotContaining("VirtualThreadSpine")
                .should().callMethod(Thread.class, "ofVirtual");
        rule.check(classes);
    }

    @Test
    void configs_e_configurations_nao_devem_usar_field_injection() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().resideInAnyPackage("..config..", "..configs..", "..configuration..")
                .should().notBeAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class);
        rule.check(classes);
    }

    @Test
    void classes_de_producao_nao_devem_usar_field_injection() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().resideOutsideOfPackage("..test..")
                .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("Test")
                .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("Tests")
                .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("IT")
                .should().notBeAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class);
        rule.check(classes);
    }

    @Test
    void producao_nao_deve_depender_da_anotacao_autowired() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..test..")
                .and().haveSimpleNameNotEndingWith("Test")
                .and().haveSimpleNameNotEndingWith("Tests")
                .and().haveSimpleNameNotEndingWith("IT")
                .should().dependOnClassesThat().haveFullyQualifiedName(org.springframework.beans.factory.annotation.Autowired.class.getName());
        rule.check(classes);
    }
}
