package com.tcc.pjb.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class PjbSecretariatAndLifecyclePackageOrganizationArchTest {

    @ArchTest
    static final ArchRule competencia_enum_must_live_under_jurisdicao_package =
            classes().that().haveSimpleName("Competencia")
                    .should().resideInAPackage("..model.entity.enums.jurisdicao..");

    @ArchTest
    static final ArchRule lifecycle_packs_must_live_under_ramo_subpackages =
            classes().that().haveSimpleNameEndingWith("LifecyclePack")
                    .and().haveSimpleNameNotContaining("Abstract")
                    .and().haveSimpleNameNotContaining("Rito")
                    .should().resideInAnyPackage(
                            "..core.processo.lifecycle.civel..",
                            "..core.processo.lifecycle.penal..",
                            "..core.processo.lifecycle.eleitoral..",
                            "..core.processo.lifecycle.militar..",
                            "..core.processo.lifecycle.trabalhista..",
                            "..core.processo.lifecycle.previdenciario..",
                            "..core.processo.lifecycle.tributario..");

    @ArchTest
    static final ArchRule secretariat_governance_dtos_must_live_under_governance_package =
            classes().that().haveSimpleName("SecretariatCoverageSnapshotDto")
                    .or().haveSimpleName("SecretariatExceptionDeskSnapshotDto")
                    .or().haveSimpleName("SecretariatFormalCatalogSnapshotDto")
                    .or().haveSimpleName("SecretariatGovernanceSnapshotDto")
                    .or().haveSimpleName("SecretariatInboxSummaryDto")
                    .should().resideInAPackage("..model.dto.secretariat.governance..");

    @ArchTest
    static final ArchRule secretariat_queue_dtos_must_live_under_queue_package =
            classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("SecretariatQueue")
                    .should().resideInAPackage("..model.dto.secretariat.queue..");

    @ArchTest
    static final ArchRule official_secretariat_requests_must_live_under_official_dto_package =
            classes().that().haveSimpleName("ForumOfficialReturnReactivationRequest")
                    .or().haveSimpleName("SecretariaOficialCumprimentoMaterializacaoRequest")
                    .or().haveSimpleName("SecretariaOficialCumprimentoReclassificacaoRequest")
                    .should().resideInAPackage("..model.dto.secretariat.oficial..");

    @ArchTest
    static final ArchRule official_secretariat_services_must_live_under_official_service_package =
            classes().that().haveSimpleName("SecretariaOficialCumprimentoRoutingService")
                    .or().haveSimpleName("SecretariatOfficialActsDrawerService")
                    .or().resideInAPackage("..service.secretariat.oficial..").and().haveSimpleName("Classification")
                    .or().resideInAPackage("..service.secretariat.oficial..").and().haveSimpleName("MaterializationAct")
                    .should().resideInAPackage("..service.secretariat.oficial..");

    @ArchTest
    static final ArchRule specialized_routing_service_must_live_under_routing_package =
            classes().that().haveSimpleName("SecretariaEspecializadaRoutingService")
                    .should().resideInAPackage("..service.secretariat.routing..");

    @ArchTest
    static final ArchRule secretariat_command_center_must_live_under_orchestration_package =
            classes().that().haveSimpleName("SecretariadoCommandCenter")
                    .should().resideInAPackage("..service.secretariat.orchestration..");

    @ArchTest
    static final ArchRule processo_visibility_service_must_live_under_access_package =
            classes().that().haveSimpleName("SecretariatProcessoVisibilidadePessoalService")
                    .should().resideInAPackage("..service.secretariat.access..");


    @ArchTest
    static final ArchRule secretariat_queue_controllers_must_live_under_queue_controller_package =
            classes().that().haveSimpleName("SecretariatQueueController")
                    .or().haveSimpleName("SecretariatSseController")
                    .should().resideInAPackage("..controller.secretariat.queue..");

    @ArchTest
    static final ArchRule secretariat_security_controllers_must_live_under_security_controller_package =
            classes().that().haveSimpleName("SecretariatBreakGlassController")
                    .or().haveSimpleName("SecretariatCredentialSecurityController")
                    .should().resideInAPackage("..controller.secretariat.security..");

    @ArchTest
    static final ArchRule secretariat_access_controllers_must_live_under_access_controller_package =
            classes().that().haveSimpleName("SecretariatProcessoVisibilidadePessoalController")
                    .should().resideInAPackage("..controller.secretariat.access..");

    @ArchTest
    static final ArchRule secretariat_operational_controllers_must_live_under_operational_controller_package =
            classes().that().haveSimpleName("SecretariaEspecializadaController")
                    .or().haveSimpleName("SecretariatDossieController")
                    .or().haveSimpleName("SecretariatJulgamentoController")
                    .or().haveSimpleName("SecretariatMinutaJuntadaController")
                    .or().haveSimpleName("ServidorSecretariaOperacionalController")
                    .should().resideInAPackage("..controller.secretariat.operational..");

}
