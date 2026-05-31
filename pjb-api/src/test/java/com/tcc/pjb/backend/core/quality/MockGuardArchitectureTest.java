package com.tcc.pjb.backend.core.quality;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.tcc.pjb.backend",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class MockGuardArchitectureTest {

    @ArchTest
    static final ArchRule guard_nao_deve_depender_de_modules =
            noClasses().that().resideInAPackage("..core.guard..")
                    .should().dependOnClassesThat().resideInAPackage("..modules..")
                    .because("core.guard é infraestrutura transversal e não pode depender de módulos de domínio");

    @ArchTest
    static final ArchRule guard_query_nao_deve_ser_subclassada_na_main_tree =
            classes().that().areAssignableTo(MockGuardEnvironmentQuery.class)
                    .should().be(MockGuardEnvironmentQuery.class)
                    .because("subclasses de MockGuardEnvironmentQuery na main-tree bypassam isRealEnvironment() e comprometem o guard de produção");
}
