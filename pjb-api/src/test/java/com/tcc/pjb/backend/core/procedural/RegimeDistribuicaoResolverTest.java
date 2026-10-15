package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RegimeDistribuicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.junit.jupiter.api.Test;

class RegimeDistribuicaoResolverTest {

    private final RegimeDistribuicaoResolver resolver = new RegimeDistribuicaoResolver();

    @Test
    void civelNoPrimeiroGrauEhSorteioAleatorio() {
        RegimeDistribuicaoDecisao decisao = resolver.resolver(RitoProcessual.COMUM_ORDINARIO, GrauJurisdicao.PRIMEIRO_GRAU);

        assertEquals(RegimeDistribuicao.ALEATORIO_ALTERNADO, decisao.regime());
        assertEquals(RamoDireito.CIVIL, decisao.ramoDireito());
        assertEquals(TpuClasseCnj.RamoJustica.ESTADUAL, decisao.ramoJustica());
        assertEquals("CPC, art. 285", decisao.fundamentoLegal());
    }

    @Test
    void penalNoPrimeiroGrauEhSorteioAleatorio() {
        RegimeDistribuicaoDecisao decisao = resolver.resolver(RitoProcessual.PROCEDIMENTO_PENAL_COMUM, GrauJurisdicao.PRIMEIRO_GRAU);

        assertEquals(RegimeDistribuicao.ALEATORIO_ALTERNADO, decisao.regime());
        assertEquals(RamoDireito.PENAL, decisao.ramoDireito());
    }

    @Test
    void trabalhistaNoPrimeiroGrauEhSorteioAleatorio() {
        RegimeDistribuicaoDecisao decisao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO, GrauJurisdicao.PRIMEIRO_GRAU);

        assertEquals(RegimeDistribuicao.ALEATORIO_ALTERNADO, decisao.regime());
        assertEquals(TpuClasseCnj.RamoJustica.TRABALHO, decisao.ramoJustica());
    }

    @Test
    void eleitoralNoPrimeiroGrauEhDesignacaoLegal() {
        RegimeDistribuicaoDecisao decisao = resolver.resolver(RitoProcessual.ELEITORAL, GrauJurisdicao.PRIMEIRO_GRAU);

        assertEquals(RegimeDistribuicao.DESIGNACAO_LEGAL, decisao.regime());
        assertEquals(TpuClasseCnj.RamoJustica.ELEITORAL, decisao.ramoJustica());
    }

    @Test
    void eleitoralNoSegundoGrauEhSorteioDeRelatoria() {
        RegimeDistribuicaoDecisao decisao = resolver.resolver(RitoProcessual.ELEITORAL, GrauJurisdicao.SEGUNDO_GRAU);

        assertEquals(RegimeDistribuicao.SORTEIO_RELATORIA, decisao.regime());
    }

    @Test
    void qualquerRitoNoSegundoGrauEhSorteioDeRelatoria() {
        assertEquals(RegimeDistribuicao.SORTEIO_RELATORIA,
                resolver.resolver(RitoProcessual.COMUM_ORDINARIO, GrauJurisdicao.SEGUNDO_GRAU).regime());
        assertEquals(RegimeDistribuicao.SORTEIO_RELATORIA,
                resolver.resolver(RitoProcessual.PROCEDIMENTO_PENAL_COMUM, GrauJurisdicao.SUPERIOR).regime());
        assertEquals(RegimeDistribuicao.SORTEIO_RELATORIA,
                resolver.resolver(RitoProcessual.COMUM_ORDINARIO, GrauJurisdicao.CONSTITUCIONAL).regime());
    }

    @Test
    void classeTpuDefineRamoEJurisdicaoQuandoPresente() {
        RegimeDistribuicaoDecisao decisao = resolver.resolver(
                RitoProcessual.COMUM_ORDINARIO,
                GrauJurisdicao.PRIMEIRO_GRAU,
                TpuClasseCnj.RECLAMACAO_TRABALHISTA_INDIVIDUAL);

        assertEquals(RamoDireito.TRABALHISTA, decisao.ramoDireito());
        assertEquals(TpuClasseCnj.RamoJustica.TRABALHO, decisao.ramoJustica());
        assertEquals(RegimeDistribuicao.ALEATORIO_ALTERNADO, decisao.regime());
    }

    @Test
    void precatorioTemPrecedenciaCronologicaSobreOSorteio() {
        RegimeDistribuicaoSinais sinais = new RegimeDistribuicaoSinais(false, false, false, true, false);

        RegimeDistribuicaoDecisao decisao = resolver.resolver(RitoProcessual.COMUM_ORDINARIO, GrauJurisdicao.PRIMEIRO_GRAU, null, sinais);

        assertEquals(RegimeDistribuicao.CRONOLOGICO, decisao.regime());
    }

    @Test
    void temaAfetadoSobrestaAntesDoSorteio() {
        RegimeDistribuicaoSinais sinais = new RegimeDistribuicaoSinais(false, false, true, false, false);

        RegimeDistribuicaoDecisao decisao = resolver.resolver(RitoProcessual.COMUM_ORDINARIO, GrauJurisdicao.SUPERIOR, null, sinais);

        assertEquals(RegimeDistribuicao.AFETACAO_SOBRESTAMENTO, decisao.regime());
    }

    @Test
    void dependenciaVinculaPorPrevencao() {
        RegimeDistribuicaoSinais sinais = new RegimeDistribuicaoSinais(true, false, false, false, false);

        RegimeDistribuicaoDecisao decisao = resolver.resolver(RitoProcessual.COMUM_ORDINARIO, GrauJurisdicao.PRIMEIRO_GRAU, null, sinais);

        assertEquals(RegimeDistribuicao.PREVENCAO, decisao.regime());
    }

    @Test
    void rescisoriaNoTribunalExcluiOJulgadorPorExclusaoLegal() {
        RegimeDistribuicaoSinais sinais = new RegimeDistribuicaoSinais(false, true, false, false, false);

        RegimeDistribuicaoDecisao decisao = resolver.resolver(RitoProcessual.COMUM_ORDINARIO, GrauJurisdicao.SEGUNDO_GRAU, null, sinais);

        assertEquals(RegimeDistribuicao.EXCLUSAO_LEGAL, decisao.regime());
    }

    @Test
    void unidadeUnicaDispensaSorteioNoPrimeiroGrau() {
        RegimeDistribuicaoSinais sinais = new RegimeDistribuicaoSinais(false, false, false, false, true);

        RegimeDistribuicaoDecisao decisao = resolver.resolver(RitoProcessual.COMUM_ORDINARIO, GrauJurisdicao.PRIMEIRO_GRAU, null, sinais);

        assertEquals(RegimeDistribuicao.UNIDADE_UNICA, decisao.regime());
    }

    @Test
    void resolucaoPorCodigosUsaClasseTpuEGrauPadraoPrimeiroGrau() {
        RegimeDistribuicaoDecisao decisao = resolver.resolverPorCodigos("ELEITORAL", "900", null);

        assertEquals(TpuClasseCnj.RamoJustica.ELEITORAL, decisao.ramoJustica());
        assertEquals(GrauJurisdicao.PRIMEIRO_GRAU, decisao.grau());
        assertEquals(RegimeDistribuicao.DESIGNACAO_LEGAL, decisao.regime());
    }

    @Test
    void acaoDeControleConcentradoRecebeRelatoriaNoGrauConstitucional() {
        RegimeDistribuicaoDecisao decisao = resolver.resolverPorCodigos("ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE", "401", null);

        assertEquals(GrauJurisdicao.CONSTITUCIONAL, decisao.grau());
        assertEquals(RegimeDistribuicao.SORTEIO_RELATORIA, decisao.regime());
        assertEquals("CPC, art. 930", decisao.fundamentoLegal());
    }

    @Test
    void classeExclusivaDeTribunalSuperiorRecebeRelatoria() {
        RegimeDistribuicaoDecisao decisao = resolver.resolverPorCodigos("HOMOLOGACAO_SENTENCA_ESTRANGEIRA", "1300", null);

        assertEquals(GrauJurisdicao.SUPERIOR, decisao.grau());
        assertEquals(RegimeDistribuicao.SORTEIO_RELATORIA, decisao.regime());
    }

    @Test
    void acaoRescisoriaEhSempreOriginariaDeTribunal() {
        RegimeDistribuicaoDecisao decisao = resolver.resolverPorCodigos("COMUM_ORDINARIO", "404", null);

        assertEquals(GrauJurisdicao.SEGUNDO_GRAU, decisao.grau());
        assertEquals(RegimeDistribuicao.SORTEIO_RELATORIA, decisao.regime());
    }

    @Test
    void revisaoCriminalEhSempreOriginariaDeTribunal() {
        RegimeDistribuicaoDecisao decisao = resolver.resolverPorCodigos("PENAL_REVISAO_CRIMINAL", "659", null);

        assertEquals(GrauJurisdicao.SEGUNDO_GRAU, decisao.grau());
        assertEquals(RegimeDistribuicao.SORTEIO_RELATORIA, decisao.regime());
    }

    @Test
    void mandadoDeSegurancaSemGrauInformadoAssumePrimeiroGrau() {
        RegimeDistribuicaoDecisao decisao = resolver.resolverPorCodigos("ESPECIAL_MANDADO_SEGURANCA", "330", null);

        assertEquals(GrauJurisdicao.PRIMEIRO_GRAU, decisao.grau());
        assertEquals(RegimeDistribuicao.ALEATORIO_ALTERNADO, decisao.regime());
    }

    @Test
    void entradaNulaCaiNoPadraoCivelPrimeiroGrau() {
        RegimeDistribuicaoDecisao decisao = resolver.resolver(null, null);

        assertEquals(RitoProcessual.COMUM_ORDINARIO, decisao.rito());
        assertEquals(GrauJurisdicao.PRIMEIRO_GRAU, decisao.grau());
        assertEquals(RegimeDistribuicao.ALEATORIO_ALTERNADO, decisao.regime());
    }
}
