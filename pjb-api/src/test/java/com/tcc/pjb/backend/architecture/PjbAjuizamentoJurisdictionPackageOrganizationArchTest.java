package com.tcc.pjb.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class PjbAjuizamentoJurisdictionPackageOrganizationArchTest {

    @ArchTest
    static final ArchRule federal_filing_cluster_must_live_under_federal_ajuizamento_packages =
            classes().that().haveSimpleNameStartingWith("Federalismo")
                    .should().resideInAnyPackage(
                            "..model.dto.ajuizamento.federal..",
                            "..controller.ajuizamento.federal..",
                            "..service.ajuizamento.federal..",
                            "..query.ajuizamento.federal.consumer..");

    @ArchTest
    static final ArchRule juizado_procedural_cluster_must_live_under_juizado_procedural_package =
            classes().that().haveSimpleNameStartingWith("NationalProceduralJuizado")
                    .should().resideInAPackage("..core.processo.juizado.procedural..");

    @ArchTest
    static final ArchRule estadual_vertical_slices_must_live_under_estadual_subpackages =
            classes().that().haveSimpleName("ProcessoVerticalCivelPrimeiroGrauApplicationService")
                    .or().haveSimpleName("ProcessoVerticalPenalCustodiaApplicationService")
                    .or().haveSimpleName("ProcessoVerticalExecucaoFiscalFazendariaApplicationService")
                    .should().resideInAnyPackage(
                            "..core.processo.vertical.estadual.civel.application..",
                            "..core.processo.vertical.estadual.penal.application..",
                            "..core.processo.vertical.estadual.fazenda.application..");
}
