package com.tcc.pjb.backend.service.processual.recursal.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalFilingBlueprintAssembler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecursalFilingBlueprintAssemblerTest {

    @Test
    void agravoInstrumentoShouldDemandInstrumentalPieces() {
        Processo processo = Processo.builder()
                .id(1L)
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .build();

        Map<String, Object> blueprint = RecursalFilingBlueprintAssembler.assemble(
                processo,
                LegalAppealType.AGRAVO_INSTRUMENTO,
                null,
                true,
                false
        );

        assertThat(blueprint).containsEntry("difereDaPeticaoInicial", true);
        assertThat((String) blueprint.get("atoPrincipal")).isEqualTo("PETICAO_INSTRUMENTAL_AUTONOMA");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) blueprint.get("camposObrigatorios");
        assertThat(fields).anyMatch(item -> "pecasInstrumentaisSelecionadas".equals(item.get("code")));
        List<Map<String, Object>> documents = (List<Map<String, Object>>) blueprint.get("documentosObrigatorios");
        assertThat(documents).anyMatch(item -> "pecasObrigatoriasInstrumento".equals(item.get("code")));
    }

    @Test
    void embargosDeclaracaoShouldFocusOnDecisionDefect() {
        Processo processo = Processo.builder()
                .id(2L)
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .build();

        Map<String, Object> blueprint = RecursalFilingBlueprintAssembler.assemble(
                processo,
                LegalAppealType.EMBARGOS_DECLARACAO,
                null,
                false,
                true
        );

        assertThat((String) blueprint.get("destinoPeticionamento")).isEqualTo("MESMO_GRAU_COM_REDIRECIONAMENTO_PARA_AUTORIDADE_COMPETENTE");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) blueprint.get("camposObrigatorios");
        assertThat(fields).anyMatch(item -> "vicioEmbargado".equals(item.get("code")));
        assertThat(fields).anyMatch(item -> "trechoEmbargado".equals(item.get("code")));
    }

    @Test
    void recursoRevistaShouldDemandTranscendenciaAndAcordaoExcerpt() {
        Processo processo = Processo.builder()
                .id(3L)
                .ramoDireito(RamoDireito.TRABALHISTA)
                .rito(RitoProcessual.TRABALHISTA_ORDINARIO)
                .build();

        Map<String, Object> blueprint = RecursalFilingBlueprintAssembler.assemble(
                processo,
                LegalAppealType.RECURSO_REVISTA,
                null,
                false,
                false
        );

        List<Map<String, Object>> fields = (List<Map<String, Object>>) blueprint.get("camposObrigatorios");
        assertThat(fields).anyMatch(item -> "trechoAcordaoRegional".equals(item.get("code")));
        assertThat(fields).anyMatch(item -> "transcendencia".equals(item.get("code")));
        List<String> gates = (List<String>) blueprint.get("travasDeValidacao");
        assertThat(gates).anyMatch(item -> item.contains("transcendência"));
    }

    @Test
    void recursoInominadoShouldExposeTurmaRecursalFamily() {
        Processo processo = Processo.builder()
                .id(4L)
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.JUIZADO_ESPECIAL_CIVEL)
                .build();

        Map<String, Object> blueprint = RecursalFilingBlueprintAssembler.assemble(
                processo,
                LegalAppealType.RECURSO_INOMINADO,
                null,
                false,
                false
        );

        assertThat(blueprint).containsEntry("familiaOrgaoJulgadorDestino", "TURMA_RECURSAL");
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) blueprint.get("blocosObrigatorios");
        assertThat(blocks).anyMatch(item -> "microssistema_juizados".equals(item.get("code")));
        List<Map<String, Object>> fields = (List<Map<String, Object>>) blueprint.get("camposObrigatorios");
        assertThat(fields).anyMatch(item -> "microssistemaOrigem".equals(item.get("code")));
    }

    @Test
    void respShouldExposeSuperiorCourtFamilyAndFilters() {
        Processo processo = Processo.builder()
                .id(5L)
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .build();

        Map<String, Object> blueprint = RecursalFilingBlueprintAssembler.assemble(
                processo,
                LegalAppealType.RESP,
                null,
                false,
                false
        );

        assertThat(blueprint).containsEntry("familiaOrgaoJulgadorDestino", "STJ_STF");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) blueprint.get("camposObrigatorios");
        assertThat(fields).anyMatch(item -> "fundamentoAcessoTribunalSuperior".equals(item.get("code")));
        List<Map<String, Object>> documents = (List<Map<String, Object>>) blueprint.get("documentosObrigatorios");
        assertThat(documents).anyMatch(item -> "filtroSuperiorDocumental".equals(item.get("code")));
    }


    @Test
    void recursoInominadoShouldExposeJuizadoDocumentDossier() {
        Processo processo = Processo.builder()
                .id(6L)
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.JUIZADO_ESPECIAL_CIVEL)
                .build();

        Map<String, Object> blueprint = RecursalFilingBlueprintAssembler.assemble(
                processo,
                LegalAppealType.RECURSO_INOMINADO,
                null,
                false,
                false
        );

        Map<String, Object> dossier = (Map<String, Object>) blueprint.get("dossieDocumentalEssencial");
        assertThat(dossier).containsEntry("familiaOrgaoJulgadorDestino", "TURMA_RECURSAL");
        assertThat(dossier).containsEntry("integraPronunciamentoImpugnadoObrigatoria", true);
        List<Map<String, Object>> packages = (List<Map<String, Object>>) dossier.get("pacotesDocumentais");
        assertThat(packages).anyMatch(item -> "microssistema_juizados".equals(item.get("code")));
    }

    @Test
    void recursoRevistaShouldExposeLaborCourtDocumentDossier() {
        Processo processo = Processo.builder()
                .id(7L)
                .ramoDireito(RamoDireito.TRABALHISTA)
                .rito(RitoProcessual.TRABALHISTA_ORDINARIO)
                .build();

        Map<String, Object> blueprint = RecursalFilingBlueprintAssembler.assemble(
                processo,
                LegalAppealType.RECURSO_REVISTA,
                null,
                false,
                false
        );

        Map<String, Object> dossier = (Map<String, Object>) blueprint.get("dossieDocumentalEssencial");
        List<Map<String, Object>> packages = (List<Map<String, Object>>) dossier.get("pacotesDocumentais");
        assertThat(packages).anyMatch(item -> "dossie_trabalhista".equals(item.get("code")));
        assertThat(packages).anyMatch(item -> "filtro_tst".equals(item.get("code")));
    }

    @Test
    void respShouldExposeSuperiorCourtDocumentDossier() {
        Processo processo = Processo.builder()
                .id(8L)
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .build();

        Map<String, Object> blueprint = RecursalFilingBlueprintAssembler.assemble(
                processo,
                LegalAppealType.RESP,
                null,
                false,
                false
        );

        Map<String, Object> dossier = (Map<String, Object>) blueprint.get("dossieDocumentalEssencial");
        assertThat(dossier).containsEntry("politicaAcervo", "REMESSA_RECURSAL_COM_DECISAO_INTEGRAL_E_ACERVO_ESSENCIAL");
        List<Map<String, Object>> packages = (List<Map<String, Object>>) dossier.get("pacotesDocumentais");
        assertThat(packages).anyMatch(item -> "filtro_superior".equals(item.get("code")));
        List<String> warnings = (List<String>) dossier.get("avisosDossie");
        assertThat(warnings).anyMatch(item -> item.contains("prequestionamento"));
    }

}
