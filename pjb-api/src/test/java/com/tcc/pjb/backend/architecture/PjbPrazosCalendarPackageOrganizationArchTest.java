package com.tcc.pjb.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class PjbPrazosCalendarPackageOrganizationArchTest {

    @ArchTest
    static final ArchRule prazo_application_service_must_live_under_application_package =
            classes().that().haveSimpleName("PrazoApplicationService")
                    .should().resideInAPackage("..core.prazos.application..");

    @ArchTest
    static final ArchRule prazo_calendar_kernel_must_live_under_calendario_package =
            classes().that().haveSimpleNameStartingWith("CalendarioForense")
                    .or().haveSimpleNameStartingWith("PrazoCalendario")
                    .or().haveSimpleNameStartingWith("PrazoFeriado")
                    .should().resideInAnyPackage(
                            "..core.prazos.calendario..",
                            "..core.prazos.calendario.domain..",
                            "..core.prazos.auditoria.domain..",
                            "..tribunal.calendario..");

    @ArchTest
    static final ArchRule prazo_calculation_kernel_must_live_under_calculo_package =
            classes().that().haveSimpleName("PrazosEngine")
                    .or().haveSimpleNameStartingWith("PrazoCalculation")
                    .or().haveSimpleNameStartingWith("PrazoWindow")
                    .or().haveSimpleNameStartingWith("PrazoExecution")
                    .or().haveSimpleNameStartingWith("PrazoHealth")
                    .or().haveSimpleName("PrazoBudgetView")
                    .or().haveSimpleName("PrazoConsistencyView")
                    .or().haveSimpleName("PrazoRegimeView")
                    .should().resideInAnyPackage(
                            "..core.prazos.calculo..",
                            "..core.prazos.calculo.domain..",
                            "..core.prazos.auditoria.domain..");

    @ArchTest
    static final ArchRule prazo_audit_kernel_must_live_under_auditoria_package =
            classes().that().haveSimpleNameStartingWith("PrazoAudit")
                    .or().haveSimpleNameStartingWith("PrazoTimeline")
                    .or().haveSimpleName("PrazoAuditTrail")
                    .or().haveSimpleName("PrazoAuditTrailService")
                    .should().resideInAnyPackage(
                            "..core.prazos.auditoria..",
                            "..core.prazos.auditoria.domain..");

    @ArchTest
    static final ArchRule prazo_policy_kernel_must_live_under_policy_package =
            classes().that().haveSimpleNameStartingWith("PrazoPolicy")
                    .or().haveSimpleName("PrazoParser")
                    .or().haveSimpleName("PrazoParseResult")
                    .or().haveSimpleName("PrazoInteligenteService")
                    .should().resideInAnyPackage(
                            "..core.prazos.policy..",
                            "..core.prazos.policy.domain..",
                            "..core.prazos.auditoria.domain..");
}
