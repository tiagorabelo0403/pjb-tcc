package com.tcc.pjb.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class PjbProcessualPackageOrganizationArchTest {

    @ArchTest
    static final ArchRule jurisdiction_enums_must_live_under_jurisdicao_package =
            classes().that().haveSimpleName("TipoJurisdicao")
                    .or().haveSimpleName("NaturezaJurisdicao")
                    .or().haveSimpleName("EsferaJurisdicao")
                    .or().haveSimpleName("MateriaJurisdicao")
                    .or().haveSimpleName("RegiaoBrasil")
                    .or().haveSimpleName("GrauJurisdicao")
                    .should().resideInAPackage("..model.entity.enums.jurisdicao..");

    @ArchTest
    static final ArchRule processual_enums_must_live_under_processual_enum_package =
            classes().that().haveSimpleName("RitoGrupoPrincipal")
                    .or().haveSimpleName("RitoWithGroup")
                    .or().haveSimpleName("FaseProcessual")
                    .or().haveSimpleName("RitoProcessual")
                    .or().haveSimpleName("RitoProcessualLegacy")
                    .or().haveSimpleName("RitoRuleProposalStatus")
                    .should().resideInAPackage("..model.entity.enums.processual..");

    @ArchTest
    static final ArchRule recursal_preclusion_type_must_live_in_recursal_domain =
            classes().that().haveSimpleName("PreclusaoTipo")
                    .should().resideInAPackage("..core.processo.recursal.domain..");

    @ArchTest
    static final ArchRule embargos_species_must_live_in_embargos_mesh_package =
            classes().that().haveSimpleNameStartingWith("Embargos")
                    .and().resideOutsideOfPackage("..controller..")
                    .and().resideOutsideOfPackage("..dto..")
                    .should().resideInAnyPackage(
                            "..core.kernel.recursal.mesh.embargos..",
                            "..core.processo.recursal.domain.foundation..",
                            "..service.processual.recursal.embargos..");

    @ArchTest
    static final ArchRule recursal_admissibility_cluster_must_live_in_subpackage =
            classes().that().haveSimpleNameStartingWith("RecursalAdmissibility")
                    .should().resideInAnyPackage(
                            "..model.dto.processual.recursal.admissibilidade..",
                            "..service.processual.recursal.admissibilidade..",
                            "..controller.processual.recursal.admissibilidade..");

    @ArchTest
    static final ArchRule recursal_formalization_cluster_must_live_in_subpackage =
            classes().that().haveSimpleNameStartingWith("RecursalFormalizacao")
                    .should().resideInAnyPackage(
                            "..model.dto.processual.recursal.formalizacao..",
                            "..service.processual.recursal.formalizacao..");

    @ArchTest
    static final ArchRule recursal_ai_cluster_must_live_in_subpackage =
            classes().that().haveSimpleNameStartingWith("RecursalIa")
                    .should().resideInAnyPackage(
                            "..model.dto.processual.recursal.ia..",
                            "..service.processual.recursal.ia..",
                            "..controller.processual.recursal.ia..");

    @ArchTest
    static final ArchRule recursal_pdf_cluster_must_live_in_subpackage =
            classes().that().haveSimpleNameStartingWith("RecursalPdf")
                    .or().haveSimpleName("RecursalNativePdfSignatureProperties")
                    .or().haveSimpleName("RecursalTimestampAuthorityProperties")
                    .or().haveSimpleName("RecursalTimestampAuthorityService")
                    .should().resideInAnyPackage(
                            "..model.dto.processual.recursal.pdf..",
                            "..service.processual.recursal.pdf..");

    @ArchTest
    static final ArchRule recursal_protocol_cluster_must_live_in_subpackage =
            classes().that().haveSimpleNameStartingWith("RecursalProtocol")
                    .or().haveSimpleNameStartingWith("RecursalTransmission")
                    .should().resideInAnyPackage(
                            "..model.dto.processual.recursal.protocolo..",
                            "..service.processual.recursal.protocolo..");




    @ArchTest
    static final ArchRule processual_substituicao_architecture_dtos_must_live_in_arquitetura_subpackage =
            classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("PjbArquiteturaSubstituicao")
                    .should().resideInAPackage("..model.dto.processual.substituicao.arquitetura..");

    @ArchTest
    static final ArchRule processual_substituicao_sync_dtos_must_live_in_comunicacao_subpackage =
            classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("PjbSubstituicaoComunicacaoSync")
                    .should().resideInAPackage("..model.dto.processual.substituicao.comunicacao..");

    @ArchTest
    static final ArchRule processual_substituicao_federativa_dtos_must_live_in_specific_federative_subpackages =
            classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("PjbSubstituicaoFederativa")
                    .should().resideInAnyPackage(
                            "..model.dto.processual.substituicao.federativa.common..",
                            "..model.dto.processual.substituicao.federativa.centrocomando..",
                            "..model.dto.processual.substituicao.federativa.cutover..",
                            "..model.dto.processual.substituicao.federativa.malhajulgadora..",
                            "..model.dto.processual.substituicao.federativa.nucleoduro..",
                            "..model.dto.processual.substituicao.federativa.poscoletiva..",
                            "..model.dto.processual.substituicao.federativa.precedentes..",
                            "..model.dto.processual.substituicao.federativa.tutelacoletiva..",
                            "..model.dto.processual.substituicao.federativa.warroom..");

    @ArchTest
    static final ArchRule processual_substituicao_homologacao_dtos_must_live_in_homologacao_subpackage =
            classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("PjbSubstituicaoHomologacao")
                    .should().resideInAPackage("..model.dto.processual.substituicao.homologacao..");

    @ArchTest
    static final ArchRule processual_substituicao_migracao_dtos_must_live_in_migracao_subpackage =
            classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("PjbSubstituicaoMigracao")
                    .should().resideInAPackage("..model.dto.processual.substituicao.migracao..");

    @ArchTest
    static final ArchRule processual_substituicao_legados_dtos_must_live_in_legados_subpackage =
            classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("PjbSubstituicaoLegados")
                    .should().resideInAPackage("..model.dto.processual.substituicao.legados..");

    @ArchTest
    static final ArchRule processual_substituicao_nacional_cockpit_dtos_must_live_in_cockpit_subpackage =
            classes().that().haveSimpleNameStartingWith("PjbSubstituicaoNacionalCockpit")
                    .or().haveSimpleName("PjbSubstituicaoNacionalOperacionalResumoResponse")
                    .should().resideInAPackage("..model.dto.processual.substituicao.nacional.cockpit..");

    @ArchTest
    static final ArchRule processual_substituicao_nacional_execution_dtos_must_live_in_execucao_subpackage =
            classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("PjbSubstituicaoNacionalExecucao")
                    .should().resideInAPackage("..model.dto.processual.substituicao.nacional.execucao..");

    @ArchTest
    static final ArchRule processual_substituicao_national_program_dtos_must_live_in_programa_subpackage =
            classes().that().haveSimpleName("PjbSubstituicaoNacionalOndaResponse")
                    .or().haveSimpleName("PjbSubstituicaoNacionalProgramaResponse")
                    .should().resideInAPackage("..model.dto.processual.substituicao.nacional.programa..");

    @ArchTest
    static final ArchRule processual_substituicao_tribunal_dtos_must_live_in_tribunal_subpackage =
            classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("PjbSubstituicaoTribunal")
                    .should().resideInAPackage("..model.dto.processual.substituicao.tribunal..");




    @ArchTest
    static final ArchRule processual_substituicao_architecture_service_must_live_in_arquitetura_subpackage =
            classes().that().haveSimpleName("PjbArquiteturaSubstituicaoNacionalFacadeService")
                    .should().resideInAPackage("..service.processual.substituicao.arquitetura..");

    @ArchTest
    static final ArchRule processual_substituicao_federativa_services_must_live_in_specific_federative_subpackages =
            classes().that().haveSimpleNameStartingWith("PjbSubstituicaoFederativa")
                    .and().haveSimpleNameEndingWith("FacadeService")
                    .should().resideInAnyPackage(
                            "..service.processual.substituicao.federativa.centrocomando..",
                            "..service.processual.substituicao.federativa.cutover..",
                            "..service.processual.substituicao.federativa.malhajulgadora..",
                            "..service.processual.substituicao.federativa.nucleoduro..",
                            "..service.processual.substituicao.federativa.poscoletiva..",
                            "..service.processual.substituicao.federativa.precedentes..",
                            "..service.processual.substituicao.federativa.tutelacoletiva..",
                            "..service.processual.substituicao.federativa.warroom..");

    @ArchTest
    static final ArchRule processual_substituicao_legados_service_must_live_in_legados_subpackage =
            classes().that().haveSimpleName("PjbSubstituicaoLegadosFacadeService")
                    .should().resideInAPackage("..service.processual.substituicao.legados..");

    @ArchTest
    static final ArchRule processual_substituicao_national_execution_service_must_live_in_execucao_subpackage =
            classes().that().haveSimpleName("PjbSubstituicaoNacionalExecutionFacadeService")
                    .should().resideInAPackage("..service.processual.substituicao.nacional.execucao..");

    @ArchTest
    static final ArchRule processual_substituicao_national_program_service_must_live_in_programa_subpackage =
            classes().that().haveSimpleName("PjbSubstituicaoNacionalProgramaFacadeService")
                    .should().resideInAPackage("..service.processual.substituicao.nacional.programa..");

    @ArchTest
    static final ArchRule processual_peticionamento_journey_services_must_live_in_journey_subpackage =
            classes().that().haveSimpleName("PeticionamentoJourneyIntelligenceAssembler")
                    .or().haveSimpleName("PeticionamentoJourneyIntelligenceService")
                    .or().haveSimpleName("PeticionamentoJourneyPayloadSupport")
                    .or().haveSimpleName("PeticionamentoProtocolProgressSupport")
                    .or().haveSimpleName("PeticionamentoSimpleProtocolWizardService")
                    .should().resideInAPackage("..service.processual.peticionamento.journey..");

    @ArchTest
    static final ArchRule processual_peticionamento_studio_services_must_live_in_studio_subpackage =
            classes().that().haveSimpleName("PeticionamentoStudioWorkspaceService")
                    .or().haveSimpleName("PeticionamentoStudioCaseTimelineService")
                    .or().haveSimpleName("PeticionamentoStudioDocumentGapService")
                    .or().haveSimpleName("PeticionamentoStudioDraftAssemblerService")
                    .or().haveSimpleName("PeticionamentoStudioDraftDiffService")
                    .or().haveSimpleName("PeticionamentoStudioEvidenceSummaryService")
                    .or().haveSimpleName("PeticionamentoStudioGovernedReviewService")
                    .or().haveSimpleName("PeticionamentoStudioProcedureLensService")
                    .or().haveSimpleName("PeticionamentoStudioProofRequestMatrixService")
                    .or().haveSimpleName("PeticionamentoStudioProtocolChecklistService")
                    .or().haveSimpleName("PeticionamentoStudioRiskEngineService")
                    .should().resideInAPackage("..service.processual.peticionamento.studio..");


    @ArchTest
    static final ArchRule processual_calculation_controllers_must_live_in_calculo_subpackage =
            classes().that().haveSimpleName("CalculoJudicialController")
                    .or().haveSimpleName("TrabalhistaCalculoLegacyController")
                    .should().resideInAPackage("..controller.processual.calculo..");

    @ArchTest
    static final ArchRule processual_deadline_controller_must_live_in_prazos_subpackage =
            classes().that().haveSimpleName("PrazoProcessualNacionalController")
                    .should().resideInAPackage("..controller.processual.prazos..");

    @ArchTest
    static final ArchRule processual_catalog_and_document_controllers_must_live_in_specific_subpackages =
            classes().that().haveSimpleName("NationalProceduralCatalogController")
                    .should().resideInAPackage("..controller.processual.catalogo..");

    @ArchTest
    static final ArchRule processual_official_document_controller_must_live_in_document_template_subpackage =
            classes().that().haveSimpleName("OfficialDocumentTemplateController")
                    .should().resideInAPackage("..controller.processual.document.template..");

    @ArchTest
    static final ArchRule processual_recursal_controllers_must_live_in_recursal_subpackage =
            classes().that().haveSimpleName("RecursalAdmissibilityController")
                    .or().haveSimpleName("RecursalIaConferenciaController")
                    .should().resideInAnyPackage("..controller.processual.recursal.admissibilidade..", "..controller.processual.recursal.ia..");


    @ArchTest
    static final ArchRule processual_lifecycle_controller_must_live_in_lifecycle_subpackage =
            classes().that().haveSimpleName("CaseContinuityController")
                    .should().resideInAPackage("..controller.processual.lifecycle..");

    @ArchTest
    static final ArchRule processual_integration_controllers_must_live_in_integration_subpackage =
            classes().that().haveSimpleName("LitispendenciaIntertribunalController")
                    .or().haveSimpleName("NationalExternalIntegrationGatewayController")
                    .should().resideInAPackage("..controller.processual.integration..");

    @ArchTest
    static final ArchRule processual_validation_controller_must_live_in_validation_subpackage =
            classes().that().haveSimpleName("MaterialLegalValidationController")
                    .should().resideInAPackage("..controller.processual.validation..");

    @ArchTest
    static final ArchRule processual_movimentacao_controller_must_live_in_movimentacao_subpackage =
            classes().that().haveSimpleName("MovimentacaoAdjustmentController")
                    .should().resideInAPackage("..controller.processual.movimentacao..");

    @ArchTest
    static final ArchRule processual_pendencia_controller_must_live_in_pendencia_subpackage =
            classes().that().haveSimpleName("OperationalPendingDashboardController")
                    .should().resideInAPackage("..controller.processual.pendencia..");

    @ArchTest
    static final ArchRule processual_pauta_controller_must_live_in_pauta_subpackage =
            classes().that().haveSimpleName("PautaAudienciaNacionalController")
                    .should().resideInAPackage("..controller.processual.pauta..");

    @ArchTest
    static final ArchRule processual_peticionamento_controller_must_live_in_peticionamento_subpackage =
            classes().that().haveSimpleName("PeticionamentoController")
                    .should().resideInAPackage("..controller.processual.peticionamento..");

    @ArchTest
    static final ArchRule processual_substituicao_controllers_and_routes_must_live_in_substituicao_subpackage =
            classes().that().haveSimpleName("PjbArquiteturaSubstituicaoNacionalController")
                    .or().haveSimpleName("PjbSubstituicaoLegadosController")
                    .or().haveSimpleName("PjbSubstituicaoNacionalExecutionController")
                    .or().haveSimpleName("PjbSubstituicaoNacionalRoutes")
                    .should().resideInAPackage("..controller.processual.substituicao..");

    @ArchTest
    static final ArchRule processual_observability_controller_must_live_in_observability_subpackage =
            classes().that().haveSimpleName("ProcessBusinessObservabilityController")
                    .should().resideInAPackage("..controller.processual.observability..");

    @ArchTest
    static final ArchRule processual_cobertura_controller_must_live_in_cobertura_subpackage =
            classes().that().haveSimpleName("ProcessoCoberturaRitosDireitosController")
                    .should().resideInAPackage("..controller.processual.cobertura..");

    @ArchTest
    static final ArchRule processual_completude_controller_must_live_in_completude_subpackage =
            classes().that().haveSimpleName("ProcessoCompletudeArquiteturalController")
                    .should().resideInAPackage("..controller.processual.completude..");

    @ArchTest
    static final ArchRule processual_surface_controllers_must_live_in_surface_subpackage =
            classes().that().haveSimpleName("ProcessoFatiasSensivelController")
                    .or().haveSimpleName("ProcessoFechamentoAvancadoController")
                    .or().haveSimpleName("ProcessoGovernancaVersionadaController")
                    .or().haveSimpleName("ProcessoOrquestracaoUnificadaController")
                    .or().haveSimpleName("ProcessoPlataformaNacionalController")
                    .or().haveSimpleName("ProcessoSigiloInteligenteController")
                    .or().haveSimpleName("ProcessoUnificadoNacionalController")
                    .or().haveSimpleName("ProcessoEvolucaoOperacionalController")
                    .should().resideInAPackage("..controller.processual.surface..");

    @ArchTest
    static final ArchRule processual_linkage_controller_must_live_in_linkage_subpackage =
            classes().that().haveSimpleName("ProcessoLinkageGovernanceController")
                    .should().resideInAPackage("..controller.processual.linkage..");

    @ArchTest
    static final ArchRule processual_malha_controller_must_live_in_malha_subpackage =
            classes().that().haveSimpleName("ProcessoMalhaAssistidaController")
                    .should().resideInAPackage("..controller.processual.malha..");

    @ArchTest
    static final ArchRule processual_painel_controllers_must_live_in_painel_subpackage =
            classes().that().haveSimpleName("ProcessoPainelContextualController")
                    .or().haveSimpleName("ProcessoPainelContextualDetalheController")
                    .should().resideInAPackage("..controller.processual.painel..");

    @ArchTest
    static final ArchRule processual_playbook_operational_controller_must_live_in_operational_subpackage =
            classes().that().haveSimpleName("ProcessoPlaybookOperacionalController")
                    .should().resideInAPackage("..controller.processual.playbook.operational..");

    @ArchTest
    static final ArchRule processual_playbook_variation_controller_must_live_in_variation_subpackage =
            classes().that().haveSimpleName("ProcessoPlaybookTribunalVariationController")
                    .should().resideInAPackage("..controller.processual.playbook.variation..");


    @ArchTest
    static final ArchRule processual_guard_controller_must_live_in_guard_subpackage =
            classes().that().haveSimpleName("ProcessualOperationGuardController")
                    .should().resideInAPackage("..controller.processual.guard..");

    @ArchTest
    static final ArchRule processual_participacao_controller_must_live_in_dedicated_subpackages =
            classes().that().haveSimpleName("ProcessualParticipacaoWorkspaceController")
                    .or().haveSimpleName("ProcessualParticipacaoSubmissionController")
                    .or().haveSimpleName("ProcessualParticipacaoControllerRateLimitSupport")
                    .should().resideInAPackage("..controller.processual.participacao..");

    @ArchTest
    static final ArchRule processual_routing_controller_must_live_in_routing_subpackage =
            classes().that().haveSimpleName("ProcessualRoutingController")
                    .should().resideInAPackage("..controller.processual.routing..");

    @ArchTest
    static final ArchRule processual_govbr_controller_must_live_in_govbr_subpackage =
            classes().that().haveSimpleName("GovBrGovernancaController")
                    .should().resideInAPackage("..controller.processual.govbr..");



    @ArchTest
    static final ArchRule postarchive_visibility_policy_types_must_live_in_visibility_subpackage =
            classes().that().haveSimpleNameStartingWith("ArchivedProcessVisibility")
                    .should().resideInAPackage("..service.processual.postarchive.visibility..");

    @ArchTest
    static final ArchRule postarchive_tombstone_policy_types_must_live_in_tombstone_subpackage =
            classes().that().haveSimpleNameStartingWith("ProcessoTombstone")
                    .should().resideInAPackage("..service.processual.postarchive.tombstone..");

}
