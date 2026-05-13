package com.tcc.pjb.backend.core.comunicacao.judicial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import org.junit.jupiter.api.Test;

class MatrizComunicacaoJudicialResolverTest {

    private final MatrizComunicacaoJudicialResolver resolver = new MatrizComunicacaoJudicialResolver();

    @Test
    void devePriorizarRepresentanteDigitalEmContextoRecursalEspecial() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
        processo.setClasseTpuCodigo("RESP");

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.INTIMACAO_ADVOGADO,
                new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                        "11122233344",
                        "Parte Recorrente",
                        null,
                        "parte@example.com",
                        null,
                        true,
                        true
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.RECURSO_ESPECIAL, decisao.tipoPrazo());
        assertTrue(decisao.priorizarRepresentanteDigital());
        assertFalse(decisao.priorizarOficialJustica());
        assertTrue(decisao.marcadores().contains("fase=RECURSAL"));
        assertTrue(decisao.marcadores().contains("viaRepresentanteDigital"));
        assertTrue(decisao.marcadores().contains("materializacao=representante_digital"));
    }

    @Test
    void deveReforcarPessoalidadeEmFluxoPenalSensivel() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.PROCEDIMENTO_PENAL_COMUM);
        processo.setFaseAtual(FaseProcessual.AUDIENCIA_CUSTODIA);
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        processo.setObjetoProcessual("pedido de prisão e busca");
        processo.setMaterialProbatorioResumo("cadeia de custodia e pericia tecnica");

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.CITACAO_PESSOAL_REU,
                new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                        "11122233344",
                        "Réu",
                        null,
                        null,
                        null,
                        false,
                        false
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.APRESENTACAO_DEFESA_PENAL, decisao.tipoPrazo());
        assertTrue(decisao.priorizarOficialJustica());
        assertTrue(decisao.bloquearPresuncao());
        assertFalse(decisao.admitirHoraCerta());
        assertTrue(decisao.marcadores().contains("pessoalidadeReforcada"));
        assertTrue(decisao.marcadores().contains("semPresuncaoAutomatica"));
        assertTrue(decisao.marcadores().contains("materializacao=oficial_justica"));
    }

    @Test
    void deveMapearCumprimentoDeSentencaParaImpugnacao() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.CUMPRIMENTO_SENTENCA);
        processo.setStatusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.INTIMACAO_PESSOAL_REU,
                new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                        "11122233344",
                        "Executado",
                        null,
                        null,
                        null,
                        false,
                        false
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.IMPUGNACAO_CUMPRIMENTO, decisao.tipoPrazo());
        assertFalse(decisao.priorizarRepresentanteDigital());
        assertTrue(decisao.eixoProcedimental().contains("CUMPRIMENTO_SENTENCA"));
        assertTrue(decisao.marcadores().contains("materializacao=executiva"));
    }

    @Test
    void deveMapearExecucaoParaEmbargosExecucaoEHoraCertaSubsidiaria() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.EXECUCAO_TITULO_EXTRAJUDICIAL);
        processo.setFaseAtual(FaseProcessual.EXECUCAO);
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        processo.setObjetoProcessual("penhora online e busca de bens do executado");
        processo.setPedidoPrincipal("execução e penhora");

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.CITACAO_PESSOAL_EXECUTADO,
                new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                        "11122233344",
                        "Executado",
                        null,
                        null,
                        null,
                        false,
                        false
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.EMBARGOS_EXECUCAO, decisao.tipoPrazo());
        assertTrue(decisao.priorizarOficialJustica());
        assertTrue(decisao.admitirHoraCerta());
        assertTrue(decisao.marcadores().contains("materializacao=executiva"));
        assertTrue(decisao.marcadores().contains("materializacao=hora_certa"));
    }

    @Test
    void deveIdentificarMandadoSegurancaSemPresuncaoAutomatica() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.ESPECIAL_MANDADO_SEGURANCA);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        processo.setClasseProcessual("Mandado de Segurança");
        processo.setRamoDireito(RamoDireito.CONSTITUCIONAL);
        processo.setMateria(MateriaJurisdicao.CONSTITUCIONAL);

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.INTIMACAO_PESSOAL_REU,
                new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                        "11122233344",
                        "Autoridade coatora",
                        null,
                        null,
                        null,
                        false,
                        false
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.MANDADO_SEGURANCA, decisao.tipoPrazo());
        assertTrue(decisao.bloquearPresuncao());
        assertFalse(decisao.admitirHoraCerta());
        assertTrue(decisao.fundamentoSintetico().contains("presunção automática bloqueada"));
    }

    @Test
    void deveSinalizarCuradoriaEmFluxoDeFamiliaSensivel() {
        Processo processo = new Processo();
        processo.setRamoDireito(RamoDireito.FAMILIA);
        processo.setMateria(MateriaJurisdicao.FAMILIA);
        processo.setRito(RitoProcessual.CIVIL_TUTELA_CURATELA);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        processo.setObjetoProcessual("interdição de incapaz e nomeação de curador");
        processo.setPedidoPrincipal("curatela e tutela provisória");

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.EDITAL_CITACAO,
                new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                        "11122233344",
                        "Interessado",
                        null,
                        null,
                        null,
                        false,
                        false
                )
        );

        assertTrue(decisao.exigirCuradorSeFrustrado());
        assertTrue(decisao.bloquearPresuncao());
        assertTrue(decisao.marcadores().contains("curadoriaPotencial"));
    }


    @Test
    void deveClassificarRecursoEleitoralEspecialComPrazoRecursalCurto() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.ELEITORAL_AIJE);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
        processo.setClasseProcessual("Recurso eleitoral em AIJE");
        processo.setRamoDireito(RamoDireito.ELEITORAL);

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.INTIMACAO_ADVOGADO,
                new CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab(
                        "11122233344",
                        "12345",
                        "CE",
                        "adv@example.com",
                        true,
                        "PJB"
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.RECURSO_ELEITORAL, decisao.tipoPrazo());
        assertTrue(decisao.priorizarRepresentanteDigital());
        assertTrue(decisao.marcadores().contains("microssistema=ELEITORAL_ESPECIAL"));
        assertTrue(decisao.marcadores().contains("faixa=recursal"));
    }

    @Test
    void deveReforcarMilitarEspecialSemPresuncaoAutomatica() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.MILITAR_PROCESSO_PENAL_MILITAR);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        processo.setClasseProcessual("Processo Penal Militar");
        processo.setRamoDireito(RamoDireito.MILITAR);
        processo.setObjetoProcessual("acusacao penal militar e conselho de justica");

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.CITACAO_PESSOAL_REU,
                new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                        "11122233344",
                        "Acusado Militar",
                        null,
                        null,
                        null,
                        false,
                        false
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.APRESENTACAO_DEFESA_PENAL, decisao.tipoPrazo());
        assertTrue(decisao.priorizarOficialJustica());
        assertTrue(decisao.bloquearPresuncao());
        assertTrue(decisao.marcadores().contains("microssistema=MILITAR_ESPECIAL"));
    }

    @Test
    void deveReconhecerAgravoInternoEmGrauSuperior() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
        processo.setClasseProcessual("Agravo Interno no Recurso Especial");
        processo.setClasseTpuCodigo("AGINT");

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.INTIMACAO_ADVOGADO,
                new CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab(
                        "11122233344",
                        "12345",
                        "CE",
                        "adv@example.com",
                        true,
                        "PJB"
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.AGRAVO_INTERNO, decisao.tipoPrazo());
        assertTrue(decisao.priorizarRepresentanteDigital());
        assertTrue(decisao.marcadores().contains("recurso=agravo_interno"));
    }

    @Test
    void deveReconhecerAcaoRescisoriaTrabalhista() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.TRABALHISTA_ACAO_RESCISORIA);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        processo.setClasseProcessual("Ação Rescisória Trabalhista");
        processo.setRamoDireito(RamoDireito.TRABALHISTA);

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.INTIMACAO_ADVOGADO,
                new CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab(
                        "11122233344",
                        "12345",
                        "CE",
                        "adv@example.com",
                        true,
                        "PJB"
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.ACAO_RESCISORIA, decisao.tipoPrazo());
        assertTrue(decisao.marcadores().contains("microssistema=TRABALHISTA"));
        assertTrue(decisao.fundamentoSintetico().contains("Matriz procedimental nacional"));
    }


    @Test
    void deveMapearContrarrazoesEmTribunalSuperior() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
        processo.setClasseProcessual("Contrarrazões ao Recurso Especial");
        processo.setClasseTpuCodigo("RESP");
        processo.setRamoDireito(RamoDireito.CIVIL);
        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setCodigo("STJ");
        jurisdicao.setGrau(GrauJurisdicao.SUPERIOR);
        processo.setJurisdicao(jurisdicao);

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.INTIMACAO_ADVOGADO,
                new CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab(
                        "11122233344",
                        "12345",
                        "CE",
                        "adv@example.com",
                        true,
                        "PJB"
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.CONTRARRAZOES_SUPERIOR, decisao.tipoPrazo());
        assertTrue(decisao.marcadores().contains("tribunalSuperior=STJ"));
        assertTrue(decisao.marcadores().contains("movimento=contrarrazoes"));
    }

    @Test
    void deveMapearEmbargosDivergenciaEmTribunalSuperiorCivel() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
        processo.setClasseProcessual("Embargos de Divergência em Recurso Especial");
        processo.setClasseTpuCodigo("ERESP");
        processo.setRamoDireito(RamoDireito.CIVIL);
        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setCodigo("STJ");
        jurisdicao.setGrau(GrauJurisdicao.SUPERIOR);
        processo.setJurisdicao(jurisdicao);

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.INTIMACAO_ADVOGADO,
                new CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab(
                        "11122233344",
                        "12345",
                        "CE",
                        "adv@example.com",
                        true,
                        "PJB"
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.EMBARGOS_DIVERGENCIA, decisao.tipoPrazo());
        assertTrue(decisao.marcadores().contains("tribunalSuperior=STJ"));
        assertTrue(decisao.marcadores().contains("microssistema=TRIBUNAIS_SUPERIORES_CIVEIS"));
        assertTrue(decisao.marcadores().contains("revisaoRegimentalTribunal"));
    }

    @Test
    void deveMapearAgravoInternoEleitoralEmTribunalSuperior() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.ELEITORAL_AIRC);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
        processo.setClasseProcessual("Agravo Interno em Recurso Eleitoral");
        processo.setClasseTpuCodigo("AGRAVO INTERNO");
        processo.setRamoDireito(RamoDireito.ELEITORAL);
        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setCodigo("TSE");
        jurisdicao.setGrau(GrauJurisdicao.SUPERIOR);
        processo.setJurisdicao(jurisdicao);

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.INTIMACAO_ADVOGADO,
                new CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab(
                        "11122233344",
                        "12345",
                        "CE",
                        "adv@example.com",
                        true,
                        "PJB"
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.AGRAVO_INTERNO, decisao.tipoPrazo());
        assertTrue(decisao.marcadores().contains("tribunalSuperior=TSE"));
        assertTrue(decisao.marcadores().contains("microssistema=TRIBUNAL_SUPERIOR_ELEITORAL"));
        assertTrue(decisao.marcadores().contains("recurso=agravo_interno"));
    }

    @Test
    void deveMapearEmbargosNulidadeMilitarEmTribunalSuperior() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.MILITAR_PROCESSO_PENAL_MILITAR);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
        processo.setClasseProcessual("Embargos de Nulidade");
        processo.setClasseTpuCodigo("EMBNUL");
        processo.setRamoDireito(RamoDireito.MILITAR);
        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setCodigo("STM");
        jurisdicao.setGrau(GrauJurisdicao.SUPERIOR);
        processo.setJurisdicao(jurisdicao);

        ProceduralCommunicationDecision decisao = resolver.resolver(
                processo,
                TipoComunicacaoJudicial.INTIMACAO_ADVOGADO,
                new CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab(
                        "11122233344",
                        "12345",
                        "CE",
                        "adv@example.com",
                        true,
                        "PJB"
                )
        );

        assertEquals(NationalPrazoEngine.TipoPrazo.EMBARGOS_INFRINGENTES_NULIDADE, decisao.tipoPrazo());
        assertTrue(decisao.marcadores().contains("tribunalSuperior=STM"));
        assertTrue(decisao.marcadores().contains("microssistema=TRIBUNAL_SUPERIOR_MILITAR"));
        assertTrue(decisao.marcadores().contains("embargos=infringentes_nulidade"));
    }

}
