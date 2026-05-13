package com.tcc.pjb.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class PjbMonolithModularConventionsArchTest {

    @ArchTest
    static final ArchRule util_packages_must_not_host_services =
            classes().that().resideInAPackage("..util..").should().haveSimpleNameNotEndingWith("Service");

    @ArchTest
    static final ArchRule service_packages_must_not_host_utils =
            classes().that().resideInAPackage("..service..")
                    .and().haveSimpleNameNotEndingWith("SupportUtils")
                    .should().haveSimpleNameNotEndingWith("Utils");

    @ArchTest
    static final ArchRule top_level_enums_should_not_live_in_controller_packages =
            classes().that().areTopLevelClasses()
                    .and().areEnums()
                    .should().resideOutsideOfPackage("..controller..");
}
