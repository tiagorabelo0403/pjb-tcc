package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.EnumSet;
import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalClassFamily;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDivergencia;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucaoFiscal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosTerceiro;

public final class RecursalCompatibilityMatrix {

    public void validate(RecursalCaseContext context, RecursalSpecies species, RecursalRoutePlan routePlan) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(species, "species");
        Objects.requireNonNull(routePlan, "routePlan");
        validateSpeciesContext(context, species);
        validateRouteContext(context, species, routePlan);
    }

    private void validateSpeciesContext(RecursalCaseContext context, RecursalSpecies species) {
        switch (species) {
            case null -> throw new RecursalConstraintViolationException("Espécie recursal ausente");
            case EmbargosDeclaracao embargos -> validateEmbargosDeclaracao(context, embargos);
            case AgravoInterno agravo -> validateAgravoInterno(context, agravo);
            case ApelacaoCivel apelacao -> validateApelacaoCivel(context, apelacao);
            case ApelacaoPenal apelacao -> validateApelacaoPenal(context, apelacao);
            case AgravoInstrumento agravo -> validateAgravoInstrumento(context, agravo);
            case AgravoInstrumentoTrabalhista agravo -> validateAgravoInstrumentoTrabalhista(context, agravo);
            case AgravoRegimental agravo -> validateAgravoRegimental(context, agravo);
            case RecursoOrdinarioConstitucional recurso -> validateRecursoOrdinarioConstitucional(context, recurso);
            case RecursoOrdinarioTrabalhista recurso -> validateRecursoOrdinarioTrabalhista(context, recurso);
            case RecursoRevista recurso -> validateRecursoRevista(context, recurso);
            case AgravoRecursoRevista agravo -> validateAgravoRecursoRevista(context, agravo);
            case AgravoPeticao agravo -> validateAgravoPeticao(context, agravo);
            case EmbargosExecucao embargos -> validateEmbargosExecucao(context, embargos);
            case EmbargosExecucaoFiscal embargos -> validateEmbargosExecucaoFiscal(context, embargos);
            case EmbargosTerceiro embargos -> validateEmbargosTerceiro(context, embargos);
            case RecursoEspecial recurso -> validateRecursoEspecial(context, recurso);
            case RecursoExtraordinario recurso -> validateRecursoExtraordinario(context, recurso);
            case AgravoRecursoEspecial agravo -> validateAgravoRecursoEspecial(context, agravo);
            case AgravoRecursoExtraordinario agravo -> validateAgravoRecursoExtraordinario(context, agravo);
            case EmbargosDivergencia embargos -> validateEmbargosDivergencia(context, embargos);
            case ReclamacaoConstitucional reclamacao -> validateReclamacao(context, reclamacao);
            case ConflitoCompetencia conflito -> validateConflitoCompetencia(context, conflito);
            case CorrecaoParcial correcao -> validateCorrecaoParcial(context, correcao);
            case RecursoInominadoJuizado recurso -> validateRecursoInominado(context, recurso);
            case PedidoUniformizacaoFederal pedido -> validatePedidoUniformizacaoFederal(context, pedido);
            default -> throw new RecursalConstraintViolationException("Espécie recursal não suportada: " + species.getClass().getSimpleName());
        }
    }

    private void validateEmbargosDeclaracao(RecursalCaseContext context, EmbargosDeclaracao embargos) {
        require(context.fase() != FaseProcessual.RECURSAL || context.instanciaAtual() != InstanceLevel.FIRST_INSTANCE || context.tribunalOrigem().secondInstanceCourt(),
                "Embargos de declaração exigem aderência entre fase recursal, instância e órgão prolator");
        if (embargos.contraDecisaoMonocratica()) {
            require(context.autoridadeAtual().decisaoMonocratica(), "Embargos contra decisão monocrática exigem autoridade individual");
        }
    }

    private void validateAgravoInterno(RecursalCaseContext context, AgravoInterno agravo) {
        require(context.autoridadeAtual().decisaoMonocratica(), "Agravo interno exige decisão monocrática antecedente");
        require(context.instanciaAtual() != InstanceLevel.FIRST_INSTANCE, "Agravo interno não é cabível contra sentença de primeiro grau");
        if (agravo.contraFiltroPresidencial()) {
            require(context.autoridadeAtual().presidencia(), "Filtro presidencial exige ato da presidência ou vice-presidência");
        }
    }

    private void validateApelacaoCivel(RecursalCaseContext context, ApelacaoCivel apelacao) {
        require(context.instanciaAtual() == InstanceLevel.FIRST_INSTANCE, "Apelação cível exige decisão de primeiro grau");
        require(context.tipoJustica() != TipoJustica.TRABALHO && context.tipoJustica() != TipoJustica.ELEITORAL, "Apelação cível não se aplica a ramos trabalhista ou eleitoral neste motor");
        if (apelacao.materiaFazendaria()) {
            require(context.ramo() == RamoDireito.ADMINISTRATIVO || context.ramo() == RamoDireito.TRIBUTARIO || context.ramo() == RamoDireito.PREVIDENCIARIO || context.fazendaPublicaOuMp(),
                    "Apelação fazendária exige aderência material ou polo institucional compatível");
        }
    }

    private void validateApelacaoPenal(RecursalCaseContext context, ApelacaoPenal apelacao) {
        require(context.instanciaAtual() == InstanceLevel.FIRST_INSTANCE, "Apelação penal exige decisão de primeiro grau");
        require(EnumSet.of(RamoDireito.PENAL, RamoDireito.MILITAR).contains(context.ramo()), "Apelação penal exige ramo penal ou militar");
        if (apelacao.tribunalDoJuri()) {
            require(context.fase().exigeRitoJuri(), "Apelação do júri exige fase penal compatível");
        }
    }

    private void validateAgravoInstrumento(RecursalCaseContext context, AgravoInstrumento agravo) {
        require(context.instanciaAtual() == InstanceLevel.FIRST_INSTANCE, "Agravo de instrumento exige decisão de primeiro grau");
        require(context.tipoJustica() != TipoJustica.TRABALHO, "Agravo de instrumento comum não se aplica ao fluxo trabalhista principal nesta malha");
        require(agravo.impugnaDecisaoInterlocutoria(), "Agravo de instrumento exige decisão interlocutória impugnada");
    }

    private void validateAgravoInstrumentoTrabalhista(RecursalCaseContext context, AgravoInstrumentoTrabalhista agravo) {
        require(context.tipoJustica() == TipoJustica.TRABALHO || context.ramo() == RamoDireito.TRABALHISTA, "Agravo de instrumento trabalhista exige contexto trabalhista");
        require(agravo.decisaoDenegatoriaRecursoTrabalhista(), "Agravo de instrumento trabalhista exige despacho denegatório");
    }

    private void validateAgravoRegimental(RecursalCaseContext context, AgravoRegimental agravo) {
        require(context.autoridadeAtual().decisaoMonocratica(), "Agravo regimental exige decisão unipessoal");
        require(context.instanciaAtual() != InstanceLevel.FIRST_INSTANCE, "Agravo regimental exige âmbito colegiado");
        require(agravo.regimentoInternoAutoriza(), "Agravo regimental exige autorização regimental");
    }

    private void validateRecursoOrdinarioConstitucional(RecursalCaseContext context, RecursoOrdinarioConstitucional recurso) {
        require(context.tribunalOrigem().secondInstanceCourt() || context.tribunalOrigem().superiorCourt(), "Recurso ordinário constitucional exige origem em tribunal");
        require(recurso.origemEmTribunal(), "Recurso ordinário constitucional exige origem em tribunal");
    }

    private void validateRecursoOrdinarioTrabalhista(RecursalCaseContext context, RecursoOrdinarioTrabalhista recurso) {
        require(context.tipoJustica() == TipoJustica.TRABALHO || context.ramo() == RamoDireito.TRABALHISTA, "Recurso ordinário trabalhista exige contexto trabalhista");
        require(context.instanciaAtual() == InstanceLevel.FIRST_INSTANCE, "Recurso ordinário trabalhista exige origem em primeiro grau");
        require(recurso.contraSentencaOuAcordaoOriginario(), "Recurso ordinário trabalhista exige decisão recorrível");
    }

    private void validateRecursoRevista(RecursalCaseContext context, RecursoRevista recurso) {
        require(context.tribunalOrigem() == RecursalTribunal.TRT || context.tribunalOrigem() == RecursalTribunal.TST, "Recurso de revista exige origem na justiça do trabalho");
        require(context.instanciaAtual() == InstanceLevel.SECOND_INSTANCE || context.instanciaAtual() == InstanceLevel.SUPERIOR, "Recurso de revista exige decisão colegiada trabalhista");
        require(recurso.prequestionamentoExpresso(), "Recurso de revista exige prequestionamento");
    }

    private void validateAgravoRecursoRevista(RecursalCaseContext context, AgravoRecursoRevista agravo) {
        require(context.tribunalOrigem() == RecursalTribunal.TRT || context.tribunalOrigem() == RecursalTribunal.TST, "Agravo em recurso de revista exige origem trabalhista");
        require(context.autoridadeAtual().presidencia() || agravo.decisaoViceOuPresidencia(), "Agravo em recurso de revista exige decisão de presidência ou vice-presidência");
    }

    private void validateAgravoPeticao(RecursalCaseContext context, AgravoPeticao agravo) {
        require(context.tipoJustica() == TipoJustica.TRABALHO || context.ramo() == RamoDireito.TRABALHISTA, "Agravo de petição exige execução trabalhista");
        require(agravo.contraDecisaoExecucao(), "Agravo de petição exige decisão de execução");
    }

    private void validateEmbargosExecucao(RecursalCaseContext context, EmbargosExecucao embargos) {
        require(embargos.execucaoGarantidaOuDispensaLegal(), "Embargos à execução exigem garantia do juízo ou dispensa legal");
        require(context.fase().isCivilExecutoria(), "Embargos à execução exigem fase executiva");
    }

    private void validateEmbargosExecucaoFiscal(RecursalCaseContext context, EmbargosExecucaoFiscal embargos) {
        require(context.ramo() == RamoDireito.TRIBUTARIO || context.classFamily() == RecursalClassFamily.TRIBUTARIO_FISCAL, "Embargos à execução fiscal exigem contexto fiscal");
        require(embargos.creditoFiscalConstituido(), "Embargos à execução fiscal exigem crédito fiscal constituído");
    }

    private void validateEmbargosTerceiro(RecursalCaseContext context, EmbargosTerceiro embargos) {
        require(context.fase().isCivilExecutoria(), "Embargos de terceiro exigem fase de constrição patrimonial");
        require(embargos.terceiroEstranhoRelacaoProcessual(), "Embargos de terceiro exigem terceiro estranho à relação processual");
    }

    private void validateRecursoEspecial(RecursalCaseContext context, RecursoEspecial recurso) {
        require(context.instanciaAtual() == InstanceLevel.SECOND_INSTANCE || context.instanciaAtual() == InstanceLevel.SUPERIOR,
                "Recurso especial exige acórdão de segundo grau ou superior");
        require(context.acordaoColegiado() || context.autoridadeAtual().colegiado(), "Recurso especial exige acórdão colegiado");
        require(context.materiaFederalInfraconstitucional() || recurso.fundadoEmDissidioJurisprudencial(), "Recurso especial exige matéria federal infraconstitucional ou dissídio jurisprudencial");
    }

    private void validateRecursoExtraordinario(RecursalCaseContext context, RecursoExtraordinario recurso) {
        require(context.instanciaAtual() == InstanceLevel.SECOND_INSTANCE || context.instanciaAtual() == InstanceLevel.SUPERIOR,
                "Recurso extraordinário exige acórdão de segundo grau ou superior");
        require(context.acordaoColegiado() || context.autoridadeAtual().colegiado(), "Recurso extraordinário exige acórdão colegiado");
        require(context.materiaConstitucional() && recurso.repercussaoGeralFundamentada(), "Recurso extraordinário exige matéria constitucional e repercussão geral fundamentada");
    }

    private void validateAgravoRecursoEspecial(RecursalCaseContext context, AgravoRecursoEspecial agravo) {
        require(context.instanciaAtual() == InstanceLevel.SECOND_INSTANCE || context.instanciaAtual() == InstanceLevel.SUPERIOR,
                "Agravo em recurso especial exige decisão de filtro em segundo grau ou corte superior");
        require(context.autoridadeAtual().presidencia() || agravo.decisaoViceOuPresidencia(), "Agravo em recurso especial exige decisão da presidência ou vice-presidência");
        require(context.materiaFederalInfraconstitucional() || agravo.demonstraViolacaoLeiFederal(), "Agravo em recurso especial exige matéria federal infraconstitucional");
    }

    private void validateAgravoRecursoExtraordinario(RecursalCaseContext context, AgravoRecursoExtraordinario agravo) {
        require(context.instanciaAtual() == InstanceLevel.SECOND_INSTANCE || context.instanciaAtual() == InstanceLevel.SUPERIOR,
                "Agravo em recurso extraordinário exige decisão de filtro em segundo grau ou corte superior");
        require(context.autoridadeAtual().presidencia() || agravo.decisaoViceOuPresidencia(), "Agravo em recurso extraordinário exige decisão da presidência ou vice-presidência");
        require(context.materiaConstitucional() && agravo.repercussaoGeralFundamentada(), "Agravo em recurso extraordinário exige questão constitucional e repercussão geral");
    }

    private void validateEmbargosDivergencia(RecursalCaseContext context, EmbargosDivergencia embargos) {
        require(context.tribunalOrigem().superiorCourt() || context.tribunalOrigem().constitutionalCourt(), "Embargos de divergência exigem corte superior ou constitucional");
        require(context.acordaoColegiado() || context.autoridadeAtual().colegiado(), "Embargos de divergência exigem acórdão colegiado");
        require(context.instanciaAtual() == InstanceLevel.SUPERIOR || context.instanciaAtual() == InstanceLevel.EXTRAORDINARY,
                "Embargos de divergência exigem instância superior ou extraordinária");
        require(embargos.acordaoEmbargadoEmCompetenciaSuperior(), "Embargos de divergência exigem acórdão embargado em competência superior");
    }

    private void validateReclamacao(RecursalCaseContext context, ReclamacaoConstitucional reclamacao) {
        require(context.instanciaAtual() != InstanceLevel.FIRST_INSTANCE, "Reclamação exige ato proveniente de órgão jurisdicional já hierarquizado");
        require(reclamacao.preservaCompetenciaTribunal() || reclamacao.garanteAutoridadePrecedente() || reclamacao.atoContrarioPrecedenteVinculante(), "Reclamação exige fundamento constitucional ou institucional típico");
    }

    private void validateConflitoCompetencia(RecursalCaseContext context, ConflitoCompetencia conflito) {
        require(conflito.orgaosDistintosEmColisao(), "Conflito de competência exige órgãos distintos");
        require(context.instanciaAtual() != InstanceLevel.FIRST_INSTANCE || conflito.ausenciaHierarquiaComumImediata(), "Conflito de competência exige incerteza real de definição de competência");
    }

    private void validateCorrecaoParcial(RecursalCaseContext context, CorrecaoParcial correcao) {
        require(correcao.regimentoInternoAutoriza(), "Correição parcial exige autorização regimental");
        require(context.instanciaAtual() != InstanceLevel.EXTRAORDINARY, "Correição parcial não compõe, em regra, a via extraordinária nesta malha");
    }

    private void validateRecursoInominado(RecursalCaseContext context, RecursoInominadoJuizado recurso) {
        require(context.instanciaAtual() == InstanceLevel.FIRST_INSTANCE, "Recurso inominado exige sentença de primeiro grau");
        require(context.classFamily() == RecursalClassFamily.JUIZADO_ESPECIAL || context.rito().isJuizado(), "Recurso inominado exige microssistema dos juizados");
        require(recurso.contraSentencaJuizado(), "Recurso inominado exige sentença do juizado");
    }

    private void validatePedidoUniformizacaoFederal(RecursalCaseContext context, PedidoUniformizacaoFederal pedido) {
        require(context.tipoJustica() == com.tcc.pjb.backend.domain.enums.TipoJustica.FEDERAL, "Pedido de uniformização federal exige microssistema federal");
        require(context.classFamily() == RecursalClassFamily.JUIZADO_ESPECIAL || context.rito().isJuizado(), "Pedido de uniformização exige juizado especial");
        require(context.instanciaAtual() == InstanceLevel.SECOND_INSTANCE, "Pedido de uniformização exige acórdão de turma recursal ou regional");
        require(context.acordaoColegiado() || context.autoridadeAtual().colegiado(), "Pedido de uniformização exige acórdão colegiado antecedente");
        require(context.materiaFederalInfraconstitucional(), "Pedido de uniformização exige questão federal infraconstitucional de direito material");
        require(pedido.divergenciaEntreTurmasRecursaisOuRegionais() || pedido.contrariedadeJurisprudenciaDominante(), "Pedido de uniformização exige divergência ou contrariedade jurisprudencial dominante");
        require(pedido.impugnacaoEspecificaFundamentos(), "Pedido de uniformização exige impugnação específica dos fundamentos do acórdão recorrido");
        if (pedido.divergenciaEntreTurmasRecursaisOuRegionais()) {
            require(pedido.paradigmaComprovado(), "Pedido de uniformização por divergência exige paradigma válido e demonstrado");
        }
    }

    private void validateRouteContext(RecursalCaseContext context, RecursalSpecies species, RecursalRoutePlan routePlan) {
        if (species.sameCaseAutos()) {
            require(routePlan.mesmaCorte(), "Recurso nos mesmos autos não pode alterar o tribunal de processamento");
            require(routePlan.remessa().mesmosAutos(), "Recurso nos mesmos autos deve permanecer sem autuação externa");
        }
        if (routePlan.remessa().externa()) {
            require(routePlan.tribunalDestino().instanceLevel().ordinal() >= context.instanciaAtual().ordinal(), "Remessa externa não pode reduzir a instância do recurso");
        }
        if (!routePlan.admissibilidade().juizoOrigem()) {
            require(routePlan.admissibilidade().autoridadeOrigem() == null, "Sem juízo de admissibilidade na origem não pode haver autoridade de origem");
        }
        if (!routePlan.admissibilidade().juizoDestino()) {
            require(routePlan.admissibilidade().autoridadeDestino() == null, "Sem juízo de admissibilidade no destino não pode haver autoridade de destino");
        }
    }

    private void require(boolean expression, String message) {
        if (!expression) {
            throw new RecursalConstraintViolationException(message);
        }
    }
}
