package com.tcc.pjb.backend.service.processual.peticionamento;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoEditorBlueprintCatalogServiceTest {

    @Test
    void deveResolverBlueprintTrabalhistaComModeloProprio() {
        PeticionamentoEditorBlueprintCatalogService service = new PeticionamentoEditorBlueprintCatalogService();

        var result = service.resolve(new PeticionamentoEditorBlueprintCatalogService.ResolveRequest(
                "TRABALHISTA",
                "TRABALHISTA_ORDINARIO",
                "TRABALHISTA",
                "RECLAMACAO TRABALHISTA",
                null,
                "VERBAS RESCISORIAS E HORAS EXTRAS",
                null,
                TipoUsuario.ADVOGADO,
                false,
                false,
                false,
                Map.of("nomeExibicao", "Escritório Atlas")
        ));

        assertEquals("TRABALHISTA", result.editorBlueprint().get("resolvedTrack"));
        assertEquals(Boolean.TRUE, result.editorBlueprint().get("supportsFactsFirstIntake"));
        assertEquals(Boolean.TRUE, result.editorBlueprint().get("technicalSelectionOptional"));
        assertFalse(result.petitionModels().isEmpty());
        assertTrue(result.petitionModels().stream().anyMatch(item -> "TRABALHISTA_RECLAMACAO".equals(item.get("code"))));
        assertTrue(result.specializedQuestionBlocks().stream().anyMatch(item -> "TRABALHO_CONTRATO_VERBAS".equals(item.get("code"))));
    }

    @Test
    void deveResolverBlueprintPenalCustodiaComBlocosCriminais() {
        PeticionamentoEditorBlueprintCatalogService service = new PeticionamentoEditorBlueprintCatalogService();

        var result = service.resolve(new PeticionamentoEditorBlueprintCatalogService.ResolveRequest(
                "PENAL",
                "CUSTODIA",
                "ESTADUAL",
                "HABEAS CORPUS",
                null,
                "LIBERDADE PROVISORIA",
                null,
                TipoUsuario.DEFENSOR_PUBLICO,
                true,
                true,
                false,
                Map.of()
        ));

        assertEquals("PENAL_CUSTODIA", result.editorBlueprint().get("resolvedTrack"));
        assertTrue(result.editorBlueprint().containsKey("signaturePolicy"));
        assertTrue(result.requiredDocuments().stream().anyMatch(item -> item.contains("materialidade") || item.contains("auto")));
        assertTrue(result.specializedQuestionBlocks().stream().anyMatch(item -> "PENAL_FATO_AUTORIA".equals(item.get("code"))));
    }

    @Test
    void deveHabilitarModeloInstitucionalQuandoPeticionanteForOrgaoPublico() {
        PeticionamentoEditorBlueprintCatalogService service = new PeticionamentoEditorBlueprintCatalogService();

        var result = service.resolve(new PeticionamentoEditorBlueprintCatalogService.ResolveRequest(
                "ADMINISTRATIVO",
                "FAZENDA_PUBLICA_CONHECIMENTO",
                "ESTADUAL",
                "ACAO ANULATORIA",
                null,
                "CONTROLE DE LEGALIDADE",
                null,
                TipoUsuario.PROCURADORIA_ESTADUAL,
                false,
                false,
                false,
                Map.of("nomeInstituicao", "PGE")
        ));

        assertTrue(result.petitionModels().stream().anyMatch(item -> "MODELO_INSTITUCIONAL_PADRONIZADO".equals(item.get("code"))));
        assertTrue(result.editorBlueprint().containsKey("brandingPolicy"));
        assertTrue(result.specializedQuestionBlocks().stream().anyMatch(item -> "FAZENDA_E_ADMINISTRACAO".equals(item.get("code"))));
    }


    @Test
    void devePriorizarModeloDeAlimentosQuandoClasseIndicarProcedimentoFamiliarEspecifico() {
        PeticionamentoEditorBlueprintCatalogService service = new PeticionamentoEditorBlueprintCatalogService();

        var result = service.resolve(new PeticionamentoEditorBlueprintCatalogService.ResolveRequest(
                "FAMILIA",
                "CIVIL_FAMILIA_ALIMENTOS",
                "ESTADUAL",
                "ACAO DE ALIMENTOS",
                null,
                "ALIMENTOS PARA MENOR",
                "OBRIGACAO ALIMENTAR",
                TipoUsuario.ADVOGADO,
                false,
                true,
                false,
                Map.of()
        ));

        assertEquals("FAMILIA_ALIMENTOS", result.editorBlueprint().get("resolvedProcedureFamily"));
        assertEquals("FAMILIA_ALIMENTOS", result.editorBlueprint().get("recommendedModelCode"));
        assertTrue(result.petitionModels().stream().anyMatch(item -> "FAMILIA_ALIMENTOS".equals(item.get("code")) && Boolean.TRUE.equals(item.get("recommended"))));
        assertTrue(result.specializedQuestionBlocks().stream().anyMatch(item -> "FAMILIA_ALIMENTOS_BINOMIO".equals(item.get("code"))));
    }

    @Test
    void devePriorizarModeloDeExecucaoFiscalQuandoClasseOuRitoApontaremCobrancaFiscal() {
        PeticionamentoEditorBlueprintCatalogService service = new PeticionamentoEditorBlueprintCatalogService();

        var result = service.resolve(new PeticionamentoEditorBlueprintCatalogService.ResolveRequest(
                "TRIBUTARIO",
                "EXECUCAO_FISCAL",
                "ESTADUAL",
                "EXECUCAO FISCAL",
                null,
                "COBRANCA DE CDA",
                "CREDITO FISCAL INSCRITO",
                TipoUsuario.PROCURADORIA_ESTADUAL,
                false,
                false,
                false,
                Map.of()
        ));

        assertEquals("FAZENDA_EXECUCAO_FISCAL", result.editorBlueprint().get("resolvedProcedureFamily"));
        assertEquals("FAZENDA_EXECUCAO_FISCAL", result.editorBlueprint().get("recommendedModelCode"));
        assertTrue(result.petitionModels().stream().anyMatch(item -> "FAZENDA_EXECUCAO_FISCAL".equals(item.get("code")) && Boolean.TRUE.equals(item.get("recommended"))));
        assertTrue(result.requiredDocuments().stream().anyMatch(item -> item.contains("CDA") || item.contains("débito") || item.contains("debito")));
    }

    @Test
    void devePriorizarMandadoDeSegurancaQuandoClasseEProvaPreconstituidaEstiveremIndicadas() {
        PeticionamentoEditorBlueprintCatalogService service = new PeticionamentoEditorBlueprintCatalogService();

        var result = service.resolve(new PeticionamentoEditorBlueprintCatalogService.ResolveRequest(
                "CONSTITUCIONAL",
                "ESPECIAL_MANDADO_SEGURANCA",
                "FEDERAL",
                "MANDADO DE SEGURANCA",
                null,
                "DIREITO LIQUIDO E CERTO",
                "ATO DE AUTORIDADE COATORA",
                TipoUsuario.ADVOGADO,
                true,
                true,
                false,
                Map.of()
        ));

        assertEquals("CONSTITUCIONAL_MANDADO_SEGURANCA", result.editorBlueprint().get("resolvedProcedureFamily"));
        assertEquals("CONSTITUCIONAL_MANDADO_SEGURANCA", result.editorBlueprint().get("recommendedModelCode"));
        assertTrue(result.specializedQuestionBlocks().stream().anyMatch(item -> "MANDADO_SEGURANCA_AUTORIDADE".equals(item.get("code"))));
        assertTrue(result.requiredDocuments().stream().anyMatch(item -> item.toLowerCase().contains("pré-constitu") || item.toLowerCase().contains("pre-constitu")));
    }

    @Test
    void deveEvitarPerguntasTecnicasComoPontoDePartidaNoFluxoFederalERecursal() {
        PeticionamentoEditorBlueprintCatalogService service = new PeticionamentoEditorBlueprintCatalogService();

        var result = service.resolve(new PeticionamentoEditorBlueprintCatalogService.ResolveRequest(
                "PREVIDENCIARIO",
                "PREVIDENCIARIO_COMUM",
                "FEDERAL",
                "ACAO PREVIDENCIARIA",
                null,
                "BENEFICIO NEGADO PELO INSS",
                null,
                TipoUsuario.ADVOGADO,
                false,
                false,
                false,
                Map.of()
        ));

        assertTrue(result.specializedQuestionBlocks().stream().anyMatch(item -> "FEDERAL_SECAO_SUBSECAO".equals(item.get("code"))));
        var federalBlock = result.specializedQuestionBlocks().stream()
                .filter(item -> "FEDERAL_SECAO_SUBSECAO".equals(item.get("code")))
                .findFirst()
                .orElseThrow();
        @SuppressWarnings("unchecked")
        var prompts = (java.util.List<java.util.Map<String, Object>>) federalBlock.get("prompts");
        assertTrue(prompts.stream().anyMatch(item -> "vinculoTerritorialFederal".equals(item.get("field"))));
        assertFalse(prompts.stream().anyMatch(item -> "secaoSubsecao".equals(item.get("field"))));
    }



    @Test
    void deveExporRotulosEmLinguagemSimplesNosPromptsFederais() {
        PeticionamentoEditorBlueprintCatalogService service = new PeticionamentoEditorBlueprintCatalogService();

        var result = service.resolve(new PeticionamentoEditorBlueprintCatalogService.ResolveRequest(
                "PREVIDENCIARIO",
                "PREVIDENCIARIO_COMUM",
                "FEDERAL",
                "ACAO PREVIDENCIARIA",
                null,
                "BENEFICIO NEGADO PELO INSS",
                null,
                TipoUsuario.ADVOGADO,
                false,
                false,
                false,
                Map.of()
        ));

        var federalBlock = result.specializedQuestionBlocks().stream()
                .filter(item -> "FEDERAL_SECAO_SUBSECAO".equals(item.get("code")))
                .findFirst()
                .orElseThrow();
        assertFalse(String.valueOf(federalBlock.get("label")).contains("Seção/Subseção"));
        @SuppressWarnings("unchecked")
        var prompts = (java.util.List<java.util.Map<String, Object>>) federalBlock.get("prompts");
        var territorialPrompt = prompts.stream()
                .filter(item -> "vinculoTerritorialFederal".equals(item.get("field")))
                .findFirst()
                .orElseThrow();
        assertEquals("Onde está a conexão federal do caso?", territorialPrompt.get("label"));
        assertEquals(Boolean.TRUE, territorialPrompt.get("plainLanguage"));
        assertTrue(String.valueOf(territorialPrompt.get("helperText")).contains("não precisa saber seção ou subseção"));
    }

}
