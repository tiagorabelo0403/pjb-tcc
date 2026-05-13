package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecursalMeshSpeciesRequest(
        @NotNull RecursalMeshSpeciesType type,
        Set<EmbargosGroundCode> embargosGrounds,
        @Size(max = 500) String embargosDetalhe,
        boolean erroMaterialAritmetico,
        boolean efeitosInfringentesPretendidos,
        boolean contraDecisaoMonocratica,
        boolean interrompePrazoRecursalPrincipal,
        boolean contraFiltroPresidencial,
        boolean interpostoNoMesmoOrgaoFracionario,
        boolean contraSentenca,
        boolean sujeitoAoReexameNecessario,
        boolean materiaFazendaria,
        boolean sentencaParcialMerito,
        boolean tribunalDoJuri,
        boolean recorrenteMinisterioPublico,
        boolean impugnaPronunciaOuDosimetria,
        boolean demonstracaoViolacaoLeiFederal,
        boolean prequestionamentoExpresso,
        boolean potencialRepetitivo,
        boolean fundadoEmDissidioJurisprudencial,
        boolean demonstracaoQuestaoConstitucional,
        boolean repercussaoGeralFundamentada,
        boolean temaConstitucionalPrequestionado,
        boolean paradigmaRepercussaoGeralVinculante,
        boolean impugnaNegativaSeguimento,
        boolean impugnaInadmissao,
        boolean impugnacaoEspecificaFundamentos,
        boolean decisaoViceOuPresidencia,
        boolean divergenciaEntreOrgaosFracionarios,
        boolean paradigmaComprovado,
        boolean meritoDoParadigmaConhecido,
        boolean acordaoEmbargadoEmCompetenciaSuperior,
        boolean impugnaDecisaoInterlocutoria,
        boolean tutelaUrgenciaOuEvidencia,
        boolean versandoSobreCompetencia,
        boolean riscoLesaoGraveOuDificilReparacao,
        boolean contraDenegacaoMandadoSeguranca,
        boolean origemEmTribunal,
        boolean transcendenciaFundamentada,
        boolean violacaoLiteralDispositivoOuDivergencia,
        boolean decisaoDenegatoriaRecursoTrabalhista,
        boolean execucaoGarantidaOuDispensaLegal,
        boolean delimitacaoJustificadaMateriasValores,
        boolean creditoFiscalConstituido,
        boolean terceiroEstranhoRelacaoProcessual,
        boolean constricaoSobreBemDeTerceiro,
        boolean posseOuDominioComprovavel,
        boolean preservaCompetenciaTribunal,
        boolean garanteAutoridadePrecedente,
        boolean atoContrarioPrecedenteVinculante,
        boolean orgaosDistintosEmColisao,
        boolean conflitoPositivoOuNegativo,
        boolean ausenciaHierarquiaComumImediata,
        boolean errorInProcedendoOuTumulto,
        boolean regimentoInternoAutoriza,
        boolean semRecursoProprioEficaz,
        boolean contraDecisaoExecucao,
        boolean contrariedadeJurisprudenciaDominante,
        boolean contraSentencaJuizado) {

    public RecursalMeshSpeciesRequest {
        embargosGrounds = embargosGrounds == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(embargosGrounds));
        embargosDetalhe = embargosDetalhe == null ? "fundamento-recursal" : embargosDetalhe.strip();
    }
}
