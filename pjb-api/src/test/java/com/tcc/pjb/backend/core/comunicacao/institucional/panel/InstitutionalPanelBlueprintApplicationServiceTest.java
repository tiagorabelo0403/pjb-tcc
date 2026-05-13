package com.tcc.pjb.backend.core.comunicacao.institucional.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalPanelBlueprintApplicationServiceTest {

    @Test
    void mustExpandForumSecretariatBlueprintWithDigitalFlowAndClosureTracks() {
        InstitutionalPanelBlueprintApplicationService service = new InstitutionalPanelBlueprintApplicationService();

        List<InstitutionalPanelBlueprintSpec> specs = service.listar("FORUM", "PAINEL_SECRETARIA_FORUM");

        assertTrue(specs.stream().anyMatch(item -> item.codigo().equals("FORUM_SECRETARIA")));
        assertTrue(specs.stream().anyMatch(item -> item.codigo().equals("FORUM_SECRETARIA_FLUXO_DIGITAL")));
        assertTrue(specs.stream().anyMatch(item -> item.codigo().equals("FORUM_SECRETARIA_ATOS_E_COMUNICACOES")));
        assertTrue(specs.stream().anyMatch(item -> item.codigo().equals("FORUM_SECRETARIA_CONCLUSOES_E_BAIXA")));
        assertTrue(specs.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("subfluxos_e_filas_paralelas"::equals));
        assertTrue(specs.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("publicacoes_intimacoes_e_prazo"::equals));
        assertTrue(specs.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("conclusos_devolucoes_e_baixa"::equals));
        assertTrue(specs.stream().flatMap(item -> item.acoesRapidas().stream()).anyMatch("saneamento_filas_e_subfluxos"::equals));
        assertTrue(specs.stream().flatMap(item -> item.acoesRapidas().stream()).anyMatch("preparar_baixa_ou_arquivamento"::equals));
    }

    @Test
    void mustExposeMandadosCejuscAndContadoriaOperationalBlocks() {
        InstitutionalPanelBlueprintApplicationService service = new InstitutionalPanelBlueprintApplicationService();

        List<InstitutionalPanelBlueprintSpec> mandados = service.listar("CENTRAL_MANDADOS", "PAINEL_UNIDADE");
        List<InstitutionalPanelBlueprintSpec> cejusc = service.listar("CEJUSC", "PAINEL_AUDIENCIAS_CONCILIACAO");
        List<InstitutionalPanelBlueprintSpec> contadoria = service.listar("CONTADORIA", "PAINEL_TECNICO_JUDICIAL");

        assertTrue(mandados.stream().anyMatch(item -> item.codigo().equals("CENTRAL_MANDADOS_OPERACIONAL")));
        assertTrue(mandados.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("mandados_por_grupo"::equals));
        assertTrue(cejusc.stream().anyMatch(item -> item.codigo().equals("CEJUSC_GESTAO_OPERACIONAL")));
        assertTrue(cejusc.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("sessoes_e_pre_sessoes"::equals));
        assertTrue(contadoria.stream().anyMatch(item -> item.codigo().equals("CONTADORIA_MEMORIA_E_LIQUIDACAO")));
        assertTrue(contadoria.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("versoes_de_calculo"::equals));
    }

    @Test
    void mustExposeConcreteUnitBlueprintsForProtocolCabinetUpjAndSecondDegree() {
        InstitutionalPanelBlueprintApplicationService service = new InstitutionalPanelBlueprintApplicationService();

        List<InstitutionalPanelBlueprintSpec> forumSecretariat = service.listar("FORUM", "PAINEL_SECRETARIA_FORUM");
        List<InstitutionalPanelBlueprintSpec> forumUnit = service.listar("FORUM", "PAINEL_UNIDADE");

        assertTrue(forumSecretariat.stream().anyMatch(item -> item.codigo().equals("FORUM_PROTOCOLO_DISTRIBUICAO")));
        assertTrue(forumSecretariat.stream().anyMatch(item -> item.codigo().equals("FORUM_UPJ_COORDENACAO")));
        assertTrue(forumSecretariat.stream().anyMatch(item -> item.codigo().equals("FORUM_SECRETARIA_JUIZADOS")));
        assertTrue(forumUnit.stream().anyMatch(item -> item.codigo().equals("FORUM_GABINETE_MAGISTRADO")));
        assertTrue(forumUnit.stream().anyMatch(item -> item.codigo().equals("FORUM_GABINETE_ASSESSORIA")));
        assertTrue(forumUnit.stream().anyMatch(item -> item.codigo().equals("FORUM_SEGUNDO_GRAU_SECRETARIA")));
        assertTrue(forumSecretariat.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("autuacao_prevencao_dependencia"::equals));
        assertTrue(forumSecretariat.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("varas_origem"::equals));
        assertTrue(forumUnit.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("conclusos_do_dia"::equals));
        assertTrue(forumUnit.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("pauta_colegiada"::equals));
    }

    @Test
    void mustExposeBranchSpecificSecretariatCabinetAndSecondDegreeBlueprintsWithoutDuplicateCodes() {
        InstitutionalPanelBlueprintApplicationService service = new InstitutionalPanelBlueprintApplicationService();

        List<InstitutionalPanelBlueprintSpec> all = service.listar(null, null);
        List<InstitutionalPanelBlueprintSpec> forumSecretariat = service.listar("FORUM", "PAINEL_SECRETARIA_FORUM");
        List<InstitutionalPanelBlueprintSpec> forumUnit = service.listar("FORUM", "PAINEL_UNIDADE");

        assertTrue(forumSecretariat.stream().anyMatch(item -> item.codigo().equals("FORUM_SECRETARIA_CIVEL_CUMPRIMENTO")));
        assertTrue(forumSecretariat.stream().anyMatch(item -> item.codigo().equals("FORUM_SECRETARIA_PENAL_CUSTODIA")));
        assertTrue(forumUnit.stream().anyMatch(item -> item.codigo().equals("FORUM_GABINETE_DECISOES_E_PRECEDENTES")));
        assertTrue(forumUnit.stream().anyMatch(item -> item.codigo().equals("FORUM_SEGUNDO_GRAU_ACORDAOS_PUBLICACOES")));
        assertTrue(forumSecretariat.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("cumprimento_de_sentenca"::equals));
        assertTrue(forumSecretariat.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("mandados_penais"::equals));
        assertTrue(forumUnit.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("precedentes_e_memoria"::equals));
        assertTrue(forumUnit.stream().flatMap(item -> item.secoesPrimarias().stream()).anyMatch("acordaos_pendentes"::equals));
        assertEquals(all.size(), all.stream().map(InstitutionalPanelBlueprintSpec::codigo).distinct().count());
    }


    @Test
    void mustFallbackToDefaultCatalogsWhenInjectedCatalogListIsEmpty() {
        InstitutionalPanelBlueprintApplicationService service = new InstitutionalPanelBlueprintApplicationService(List.of());

        List<InstitutionalPanelBlueprintSpec> specs = service.listar(null, null);

        assertTrue(specs.stream().anyMatch(item -> item.codigo().equals("FORUM_DIRETORIA")));
        assertTrue(specs.stream().anyMatch(item -> item.codigo().equals("PROMOTORIA_TITULAR")));
        assertTrue(specs.stream().anyMatch(item -> item.codigo().equals("DEFENSORIA_TITULAR")));
        assertTrue(specs.stream().anyMatch(item -> item.codigo().equals("CENTRAL_MANDADOS_OPERACIONAL")));
    }

}
