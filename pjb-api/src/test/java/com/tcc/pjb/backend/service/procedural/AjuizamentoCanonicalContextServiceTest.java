package com.tcc.pjb.backend.service.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.core.compiler.LegalCompilerService;
import com.tcc.pjb.backend.core.procedural.ProceduralForumAllocationReport;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AjuizamentoCanonicalContextServiceTest {

    @Test
    void deveConsolidarContextoCanonicoDeFamiliaComSnapshotTerritorial() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("Ação de Alimentos");
        processo.setAssunto("pedido de alimentos para menor");
        processo.setPedidoPrincipal("Fixação de alimentos provisórios para criança");

        LegalCompilerService.CompiledProcess compiled = new LegalCompilerService.CompiledProcess(
                null,
                TipoJustica.ESTADUAL,
                RamoDireito.FAMILIA,
                RitoProcessual.CIVIL_FAMILIA_ALIMENTOS,
                MateriaJurisdicao.CIVIL,
                NivelSigilo.PUBLICO,
                42,
                "CANONICAL_RITO_RESOLVED",
                false,
                List.of(),
                Map.of()
        );

        ProceduralForumAllocationReport forumAllocation = new ProceduralForumAllocationReport(
                Instant.now(),
                "102",
                "Ação de Alimentos",
                "DOMICILIO_ALIMENTANDO",
                "Fortaleza",
                "CE",
                "foro do domicílio do alimentando",
                "SEM_PREVENCAO",
                "SEM_CONEXAO",
                List.of(),
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "VARA-FAM-001",
                "1ª Vara de Família",
                "FAMILIA",
                true,
                true,
                0.97d,
                "PJE",
                true,
                false,
                false,
                true,
                true,
                "READY",
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );

        ProceduralRoutingReport routing = new ProceduralRoutingReport(
                Instant.now(),
                "ALIMENTOS",
                "FAMILIA",
                "ESPECIAL",
                "FAMILIA",
                "ESTADUAL",
                "CIVIL_FAMILIA_ALIMENTOS",
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "PJE",
                "Foro de Fortaleza",
                "Fortaleza",
                "CE",
                "1ª Vara de Família",
                "FAMILIA",
                "MEDIA",
                "DOCUMENTAL",
                true,
                false,
                false,
                0.96d,
                "BAIXO",
                List.of(),
                null,
                forumAllocation,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );

        service.consolidate(processo, compiled, routing);

        assertEquals(TipoJustica.ESTADUAL, processo.getTipoJustica());
        assertEquals(RamoDireito.FAMILIA, processo.getRamoDireito());
        assertEquals(RitoProcessual.CIVIL_FAMILIA_ALIMENTOS, processo.getRito());
        assertEquals(MateriaJurisdicao.FAMILIA, processo.getMateria());
        assertEquals("102", processo.getClasseTpuCodigo());
        assertEquals("CE", processo.getUf());
        assertEquals("Fortaleza", processo.getComarca());
        assertEquals("1ª Vara de Família", processo.getVara());
        assertEquals("Tribunal de Justiça do Ceará", processo.getTribunal());
    }
    @Test
    void deveConsolidarContextoCanonicoEleitoralEspecifico() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("AIJE");
        processo.setAssunto("abuso de poder político em eleição municipal");
        processo.setPedidoPrincipal("ajuizar AIJE por captação ilícita de sufrágio e abuso de poder");

        ProceduralRoutingReport routing = new ProceduralRoutingReport(
                Instant.now(),
                "AIJE",
                "ELEITORAL",
                "ESPECIAL",
                "ELEITORAL",
                "ESTADUAL",
                "ELEITORAL",
                "TRE-CE",
                "Tribunal Regional Eleitoral do Ceará",
                "PJE",
                "Zona Eleitoral de Morada Nova",
                "Morada Nova",
                "CE",
                "1ª Zona Eleitoral",
                "ELEITORAL",
                "ALTA",
                "DOCUMENTAL",
                true,
                false,
                false,
                0.98d,
                "MEDIO",
                List.of(),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );

        service.consolidate(processo, null, routing);

        assertEquals(TipoJustica.ELEITORAL, processo.getTipoJustica());
        assertEquals(RamoDireito.ELEITORAL, processo.getRamoDireito());
        assertEquals(RitoProcessual.ELEITORAL_AIJE, processo.getRito());
        assertEquals(MateriaJurisdicao.ELEITORAL, processo.getMateria());
        assertEquals("902", processo.getClasseTpuCodigo());
    }

    @Test
    void deveConsolidarContextoCanonicoMilitarEspecifico() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("IPM");
        processo.setAssunto("inquérito policial militar sobre crime militar em unidade do Exército");
        processo.setPedidoPrincipal("acompanhar IPM instaurado no âmbito das Forças Armadas");

        ProceduralRoutingReport routing = new ProceduralRoutingReport(
                Instant.now(),
                "IPM",
                "MILITAR",
                "PENAL",
                "MILITAR",
                "MILITAR_FEDERAL",
                "MILITAR",
                "STM",
                "Superior Tribunal Militar",
                "JUSTICA_MILITAR",
                "Auditoria Militar da União",
                "Brasília",
                "DF",
                "Auditoria Militar da União",
                "MILITAR",
                "ALTA",
                "DOCUMENTAL",
                true,
                false,
                false,
                0.97d,
                "ALTO",
                List.of(),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );

        service.consolidate(processo, null, routing);

        assertEquals(TipoJustica.MILITAR_FEDERAL, processo.getTipoJustica());
        assertEquals(RamoDireito.MILITAR, processo.getRamoDireito());
        assertEquals(RitoProcessual.MILITAR_IPM, processo.getRito());
        assertEquals(MateriaJurisdicao.MILITAR, processo.getMateria());
        assertEquals("1000", processo.getClasseTpuCodigo());
    }

    @Test
    void deveConsolidarContextoCanonicoTrabalhistaEspecifico() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("Dissídio Coletivo");
        processo.setAssunto("greve e descumprimento de acordo coletivo de trabalho");
        processo.setPedidoPrincipal("instaurar dissídio coletivo com base na CLT e em convenção coletiva");

        ProceduralRoutingReport routing = new ProceduralRoutingReport(
                Instant.now(),
                "DISSIDIO",
                "TRABALHISTA",
                "COLETIVO",
                "TRABALHISTA",
                "TRABALHO",
                "TRABALHISTA_ORDINARIO",
                "TRT7",
                "Tribunal Regional do Trabalho da 7ª Região",
                "PJE_TRT",
                "Fortaleza",
                "Fortaleza",
                "CE",
                "TRT 7",
                "TRABALHISTA",
                "MEDIA",
                "DOCUMENTAL",
                true,
                false,
                false,
                0.96d,
                "MEDIO",
                List.of(),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );

        service.consolidate(processo, null, routing);

        assertEquals(TipoJustica.TRABALHO, processo.getTipoJustica());
        assertEquals(RamoDireito.TRABALHISTA, processo.getRamoDireito());
        assertEquals(RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO, processo.getRito());
        assertEquals(MateriaJurisdicao.TRABALHISTA, processo.getMateria());
        assertEquals("752", processo.getClasseTpuCodigo());
    }


    @Test
    void deveConsolidarContextoCanonicoPrevidenciarioBpc() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("BPC/LOAS");
        processo.setAssunto("benefício assistencial ao idoso hipossuficiente perante o INSS");
        processo.setPedidoPrincipal("concessão de BPC/LOAS com tutela de urgência no JEF");

        service.consolidate(processo, null, null);

        assertEquals(TipoJustica.FEDERAL, processo.getTipoJustica());
        assertEquals(RamoDireito.PREVIDENCIARIO, processo.getRamoDireito());
        assertEquals(RitoProcessual.PREVIDENCIARIO_BPC_LOAS, processo.getRito());
        assertEquals(MateriaJurisdicao.PREVIDENCIARIA, processo.getMateria());
        assertEquals("852", processo.getClasseTpuCodigo());
    }

    @Test
    void deveConsolidarContextoCanonicoTributarioExecucaoFiscal() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("Execução Fiscal");
        processo.setAssunto("cobrança de dívida ativa fundada em CDA de IPTU");
        processo.setPedidoPrincipal("ajuizar execução fiscal do crédito tributário municipal");

        service.consolidate(processo, null, null);

        assertEquals(RamoDireito.TRIBUTARIO, processo.getRamoDireito());
        assertEquals(RitoProcessual.EXECUCAO_FISCAL, processo.getRito());
        assertEquals(MateriaJurisdicao.EXECUCAO_FISCAL, processo.getMateria());
        assertEquals("453", processo.getClasseTpuCodigo());
    }

    @Test
    void deveConsolidarContextoCanonicoPenalMariaDaPenha() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("Ação Penal");
        processo.setAssunto("violência doméstica e familiar contra a mulher");
        processo.setPedidoPrincipal("medidas da Lei Maria da Penha e persecução penal");

        service.consolidate(processo, null, null);

        assertEquals(RamoDireito.PENAL, processo.getRamoDireito());
        assertEquals(RitoProcessual.PENAL_MARIA_DA_PENHA, processo.getRito());
        assertEquals(MateriaJurisdicao.PENAL, processo.getMateria());
        assertEquals("655", processo.getClasseTpuCodigo());
        assertEquals(NivelSigilo.SEGREDO_JUSTICA, processo.getNivelSigilo());
    }

    @Test
    void deveConsolidarContextoCanonicoInternacionalHomologacao() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("Homologação de Sentença Estrangeira");
        processo.setAssunto("pedido de homologação de sentença estrangeira perante o STJ");
        processo.setPedidoPrincipal("homologar sentença estrangeira com cooperação jurídica internacional");

        service.consolidate(processo, null, null);

        assertEquals(TipoJustica.SUPERIOR, processo.getTipoJustica());
        assertEquals(RitoProcessual.HOMOLOGACAO_SENTENCA_ESTRANGEIRA, processo.getRito());
        assertEquals("1300", processo.getClasseTpuCodigo());
    }

    @Test
    void deveConsolidarContextoCanonicoEmpresarialRecuperacaoJudicial() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("Recuperação Judicial");
        processo.setAssunto("empresa em crise econômico-financeira requer recuperação judicial");
        processo.setPedidoPrincipal("deferimento do processamento da recuperação judicial");

        service.consolidate(processo, null, null);

        assertEquals(RamoDireito.EMPRESARIAL, processo.getRamoDireito());
        assertEquals(RitoProcessual.RECUPERACAO_JUDICIAL, processo.getRito());
        assertEquals(MateriaJurisdicao.FALENCIAS, processo.getMateria());
        assertEquals("1100", processo.getClasseTpuCodigo());
    }


    @Test
    void deveConsolidarContextoCanonicoTrabalhistaInqueritoFaltaGrave() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("Inquérito Judicial para Apuração de Falta Grave");
        processo.setAssunto("empregado estável suspenso por falta grave");
        processo.setPedidoPrincipal("ajuizar inquérito judicial no prazo do art. 853 da CLT");

        service.consolidate(processo, null, null);

        assertEquals(TipoJustica.TRABALHO, processo.getTipoJustica());
        assertEquals(RamoDireito.TRABALHISTA, processo.getRamoDireito());
        assertEquals(RitoProcessual.TRABALHISTA_INQUERITO_FALTA_GRAVE, processo.getRito());
        assertEquals(MateriaJurisdicao.TRABALHISTA, processo.getMateria());
        assertEquals("750", processo.getClasseTpuCodigo());
    }

    @Test
    void deveConsolidarContextoCanonicoTrabalhistaAcaoDeCumprimento() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("Ação de Cumprimento");
        processo.setAssunto("descumprimento de convenção coletiva");
        processo.setPedidoPrincipal("ajuizar ação de cumprimento com fundamento no art. 872 da CLT");

        service.consolidate(processo, null, null);

        assertEquals(TipoJustica.TRABALHO, processo.getTipoJustica());
        assertEquals(RamoDireito.TRABALHISTA, processo.getRamoDireito());
        assertEquals(RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO, processo.getRito());
        assertEquals(MateriaJurisdicao.TRABALHISTA, processo.getMateria());
        assertEquals("757", processo.getClasseTpuCodigo());
    }

    @Test
    void deveConsolidarContextoCanonicoTrabalhistaAlcada() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("Reclamação Trabalhista");
        processo.setAssunto("rito de alçada por verbas rescisórias");
        processo.setPedidoPrincipal("ajuizar reclamação trabalhista de alçada com base na Lei 5.584/70");

        service.consolidate(processo, null, null);

        assertEquals(TipoJustica.TRABALHO, processo.getTipoJustica());
        assertEquals(RamoDireito.TRABALHISTA, processo.getRamoDireito());
        assertEquals(RitoProcessual.TRABALHISTA_SUMARIO_ALCADA, processo.getRito());
        assertEquals(MateriaJurisdicao.TRABALHISTA, processo.getMateria());
        assertEquals("750", processo.getClasseTpuCodigo());
    }



    @Test
    void deveConsolidarContextoCanonicoInfanciaInfracional() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("Ato Infracional");
        processo.setAssunto("apuração de ato infracional com pedido de medida socioeducativa");
        processo.setPedidoPrincipal("representação por ato infracional praticado por adolescente");

        service.consolidate(processo, null, null);

        assertEquals(RamoDireito.INFANCIA_JUVENTUDE, processo.getRamoDireito());
        assertEquals(RitoProcessual.INFANCIA_JUVENTUDE_INFRACIONAL, processo.getRito());
        assertEquals(MateriaJurisdicao.INFANCIA_JUVENTUDE, processo.getMateria());
        assertEquals("662", processo.getClasseTpuCodigo());
    }

    @Test
    void deveConsolidarContextoCanonicoAdministrativoPad() {
        AjuizamentoCanonicalContextService service = new AjuizamentoCanonicalContextService(new ProceduralCatalogService());
        Processo processo = new Processo();
        processo.setClasseProcessual("PAD");
        processo.setAssunto("processo administrativo disciplinar instaurado contra servidor público");
        processo.setPedidoPrincipal("controle judicial de penalidade disciplinar em PAD");

        service.consolidate(processo, null, null);

        assertEquals(RamoDireito.ADMINISTRATIVO, processo.getRamoDireito());
        assertEquals(RitoProcessual.ADMINISTRATIVO_PAD, processo.getRito());
        assertEquals(MateriaJurisdicao.ADMINISTRATIVO, processo.getMateria());
        assertEquals("551", processo.getClasseTpuCodigo());
    }

}
