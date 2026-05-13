package com.tcc.pjb.backend.service.recursal.mesh;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoInstrumento;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoInstrumentoTrabalhista;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoInterno;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoPeticao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoRecursoEspecial;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoRecursoExtraordinario;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoRecursoRevista;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoRegimental;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ApelacaoCivel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ApelacaoPenal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ConflitoCompetencia;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.CorrecaoParcial;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracaoContradicao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracaoErroMaterial;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracaoGround;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracaoObscuridade;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracaoOmissao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDivergencia;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucaoFiscal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosTerceiro;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalCaseContext;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionCommand;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ReclamacaoConstitucional;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.PedidoUniformizacaoFederal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoEspecial;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoExtraordinario;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoInominadoJuizado;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoOrdinarioConstitucional;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoOrdinarioTrabalhista;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoRevista;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.EmbargosGroundCode;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshContextRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshTransitionRequest;

@Component
public class RecursalMeshRequestMapper {

    public RecursalCaseContext toContext(RecursalMeshContextRequest request) {
        return new RecursalCaseContext(
                request.processoId(),
                request.numeroProcesso(),
                request.tipoJustica(),
                request.ramo(),
                request.rito(),
                request.fase(),
                request.classeProcessual(),
                request.classFamily(),
                request.tribunalOrigem(),
                request.tribunalDetalhadoOrigem(),
                request.instanciaAtual(),
                request.orgaoProlator(),
                request.decisaoMonocratica(),
                request.acordaoColegiado(),
                request.fazendaPublicaOuMp(),
                request.justicaGratuitaOuIsencaoLegal(),
                request.materiaFederalInfraconstitucional(),
                request.materiaConstitucional(),
                request.tempestivo(),
                request.remessaNecessaria(),
                request.requisicaoPublicaPagamento()
        );
    }

    public RecursalSpecies toSpecies(RecursalMeshSpeciesRequest request) {
        return switch (request.type()) {
            case EDCL -> new EmbargosDeclaracao(
                    grounds(request),
                    request.efeitosInfringentesPretendidos(),
                    request.contraDecisaoMonocratica(),
                    request.interrompePrazoRecursalPrincipal()
            );
            case AGINT -> new AgravoInterno(
                    request.contraDecisaoMonocratica(),
                    request.contraFiltroPresidencial(),
                    request.interpostoNoMesmoOrgaoFracionario()
            );
            case APCIV -> new ApelacaoCivel(
                    request.contraSentenca(),
                    request.sujeitoAoReexameNecessario(),
                    request.materiaFazendaria(),
                    request.sentencaParcialMerito()
            );
            case APCRIM -> new ApelacaoPenal(
                    request.contraSentenca(),
                    request.tribunalDoJuri(),
                    request.recorrenteMinisterioPublico(),
                    request.impugnaPronunciaOuDosimetria()
            );
            case AGINST -> new AgravoInstrumento(
                    request.impugnaDecisaoInterlocutoria(),
                    request.tutelaUrgenciaOuEvidencia(),
                    request.versandoSobreCompetencia(),
                    request.riscoLesaoGraveOuDificilReparacao()
            );
            case AGITRAB -> new AgravoInstrumentoTrabalhista(
                    request.decisaoDenegatoriaRecursoTrabalhista(),
                    request.impugnacaoEspecificaFundamentos(),
                    request.execucaoGarantidaOuDispensaLegal() || request.contraSentenca()
            );
            case AGREG -> new AgravoRegimental(
                    request.contraDecisaoMonocratica(),
                    request.interpostoNoMesmoOrgaoFracionario(),
                    request.regimentoInternoAutoriza()
            );
            case ROC -> new RecursoOrdinarioConstitucional(
                    request.contraDenegacaoMandadoSeguranca(),
                    request.origemEmTribunal(),
                    request.demonstracaoQuestaoConstitucional() || request.prequestionamentoExpresso()
            );
            case ROT -> new RecursoOrdinarioTrabalhista(
                    request.contraSentenca(),
                    true,
                    true
            );
            case RR -> new RecursoRevista(
                    request.transcendenciaFundamentada(),
                    request.violacaoLiteralDispositivoOuDivergencia(),
                    request.prequestionamentoExpresso(),
                    request.fundadoEmDissidioJurisprudencial()
            );
            case AIRR -> new AgravoRecursoRevista(
                    request.decisaoDenegatoriaRecursoTrabalhista(),
                    request.impugnacaoEspecificaFundamentos(),
                    request.decisaoViceOuPresidencia(),
                    request.transcendenciaFundamentada()
            );
            case AGPET -> new AgravoPeticao(
                    request.execucaoGarantidaOuDispensaLegal(),
                    request.delimitacaoJustificadaMateriasValores(),
                    request.contraDecisaoExecucao(),
                    true
            );
            case EEXEC -> new EmbargosExecucao(
                    request.execucaoGarantidaOuDispensaLegal(),
                    true,
                    request.contraDecisaoExecucao() || request.impugnaDecisaoInterlocutoria(),
                    true
            );
            case EEFISC -> new EmbargosExecucaoFiscal(
                    request.execucaoGarantidaOuDispensaLegal(),
                    true,
                    request.creditoFiscalConstituido(),
                    request.execucaoGarantidaOuDispensaLegal()
            );
            case ETERC -> new EmbargosTerceiro(
                    request.terceiroEstranhoRelacaoProcessual(),
                    request.constricaoSobreBemDeTerceiro(),
                    request.posseOuDominioComprovavel(),
                    true
            );
            case RESP -> new RecursoEspecial(
                    request.demonstracaoViolacaoLeiFederal(),
                    request.prequestionamentoExpresso(),
                    request.potencialRepetitivo(),
                    request.fundadoEmDissidioJurisprudencial()
            );
            case RE -> new RecursoExtraordinario(
                    request.demonstracaoQuestaoConstitucional(),
                    request.repercussaoGeralFundamentada(),
                    request.temaConstitucionalPrequestionado(),
                    request.paradigmaRepercussaoGeralVinculante()
            );
            case ARESP -> new AgravoRecursoEspecial(
                    request.impugnaNegativaSeguimento(),
                    request.impugnaInadmissao(),
                    request.impugnacaoEspecificaFundamentos(),
                    request.decisaoViceOuPresidencia(),
                    request.demonstracaoViolacaoLeiFederal()
            );
            case ARE -> new AgravoRecursoExtraordinario(
                    request.impugnaNegativaSeguimento(),
                    request.impugnaInadmissao(),
                    request.impugnacaoEspecificaFundamentos(),
                    request.decisaoViceOuPresidencia(),
                    request.demonstracaoQuestaoConstitucional(),
                    request.repercussaoGeralFundamentada()
            );
            case EDIV -> new EmbargosDivergencia(
                    request.divergenciaEntreOrgaosFracionarios(),
                    request.paradigmaComprovado(),
                    request.meritoDoParadigmaConhecido(),
                    request.acordaoEmbargadoEmCompetenciaSuperior()
            );
            case RCL -> new ReclamacaoConstitucional(
                    request.preservaCompetenciaTribunal(),
                    request.garanteAutoridadePrecedente(),
                    request.atoContrarioPrecedenteVinculante(),
                    request.origemEmTribunal()
            );
            case CC -> new ConflitoCompetencia(
                    request.orgaosDistintosEmColisao(),
                    request.conflitoPositivoOuNegativo(),
                    request.ausenciaHierarquiaComumImediata(),
                    true
            );
            case CPARCIAL -> new CorrecaoParcial(
                    request.errorInProcedendoOuTumulto(),
                    request.regimentoInternoAutoriza(),
                    request.semRecursoProprioEficaz(),
                    !request.contraDecisaoMonocratica()
            );
            case RINOM -> new RecursoInominadoJuizado(
                    request.contraSentencaJuizado(),
                    true,
                    true,
                    true
            );
            case PUILF -> new PedidoUniformizacaoFederal(
                    request.divergenciaEntreOrgaosFracionarios(),
                    request.contrariedadeJurisprudenciaDominante(),
                    request.paradigmaComprovado(),
                    request.impugnacaoEspecificaFundamentos()
            );
        };
    }

    public RecursalTransitionCommand toCommand(RecursalMeshTransitionRequest request, RecursalStateSnapshot snapshot) {
        RecursalCaseContext context = toContext(request.context());
        RecursalSpecies species = toSpecies(request.species());
        return new RecursalTransitionCommand(
                snapshot,
                context,
                species,
                request.event(),
                request.actor(),
                request.occurredAt() == null ? Instant.now() : request.occurredAt(),
                request.details()
        );
    }

    private Set<EmbargosDeclaracaoGround> grounds(RecursalMeshSpeciesRequest request) {
        return request.embargosGrounds().stream().map(code -> switch (code) {
            case OMISSAO -> new EmbargosDeclaracaoOmissao(request.embargosDetalhe(), request.efeitosInfringentesPretendidos());
            case CONTRADICAO -> new EmbargosDeclaracaoContradicao(request.embargosDetalhe(), request.efeitosInfringentesPretendidos());
            case OBSCURIDADE -> new EmbargosDeclaracaoObscuridade(request.embargosDetalhe());
            case ERRO_MATERIAL -> new EmbargosDeclaracaoErroMaterial(request.embargosDetalhe(), request.erroMaterialAritmetico());
        }).collect(Collectors.toUnmodifiableSet());
    }
}
