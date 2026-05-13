package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;

@Service
public class MatrizComunicacaoJudicialResolver {

    public ProceduralCommunicationDecision resolver(Processo processo,
                                                    TipoComunicacaoJudicial tipoComunicacao,
                                                    CitacaoIntimacaoEngine.PerfilDestinatario destinatario) {
        Objects.requireNonNull(tipoComunicacao, "tipoComunicacao");
        ProceduralCommunicationContext context = ProceduralCommunicationContext.from(processo, tipoComunicacao, destinatario);
        NationalPrazoEngine.TipoPrazo tipoPrazo = resolverPrazo(context);
        boolean priorizarRepresentanteDigital = devePriorizarRepresentanteDigital(context, tipoPrazo);
        boolean priorizarOficialJustica = devePriorizarOficial(context, tipoPrazo);
        boolean bloquearPresuncao = deveBloquearPresuncao(context, priorizarOficialJustica);
        boolean admitirHoraCerta = deveAdmitirHoraCerta(context, priorizarRepresentanteDigital, priorizarOficialJustica);
        boolean exigirCurador = context.exigeCuradoriaPotencial() || admitirHoraCerta && context.tipoComunicacao().isCitacao();
        List<String> marcadores = construirMarcadores(context, tipoPrazo, priorizarRepresentanteDigital, priorizarOficialJustica, bloquearPresuncao, admitirHoraCerta, exigirCurador);
        String eixo = construirEixo(context, tipoPrazo);
        String fundamento = construirFundamento(context, tipoPrazo, priorizarRepresentanteDigital, priorizarOficialJustica, bloquearPresuncao, admitirHoraCerta, exigirCurador);
        return new ProceduralCommunicationDecision(
                tipoPrazo,
                priorizarRepresentanteDigital,
                priorizarOficialJustica,
                bloquearPresuncao,
                admitirHoraCerta,
                exigirCurador,
                eixo,
                fundamento,
                List.copyOf(marcadores)
        );
    }

    private NationalPrazoEngine.TipoPrazo resolverPrazo(ProceduralCommunicationContext context) {
        if (context.isHabeasCorpus()) {
            return NationalPrazoEngine.TipoPrazo.HABEAS_CORPUS;
        }
        if (context.isMandadoSeguranca()) {
            return NationalPrazoEngine.TipoPrazo.MANDADO_SEGURANCA;
        }
        if (context.isAcaoRescisoria()) {
            return NationalPrazoEngine.TipoPrazo.ACAO_RESCISORIA;
        }
        if (context.isEmbargosDeclaracao()) {
            return NationalPrazoEngine.TipoPrazo.EMBARGOS_DECLARACAO;
        }
        if (context.isEmbargosDivergencia()) {
            return NationalPrazoEngine.TipoPrazo.EMBARGOS_DIVERGENCIA;
        }
        if (context.isEmbargosInfringentesOuNulidade()) {
            return NationalPrazoEngine.TipoPrazo.EMBARGOS_INFRINGENTES_NULIDADE;
        }
        if (context.isEmbargosExecucao() || context.isEmbargosTerceiro()) {
            return NationalPrazoEngine.TipoPrazo.EMBARGOS_EXECUCAO;
        }
        if (context.isCumprimentoSentenca()) {
            return NationalPrazoEngine.TipoPrazo.IMPUGNACAO_CUMPRIMENTO;
        }
        if (context.isPericial() && context.tipoComunicacao().isIntimacao()) {
            return NationalPrazoEngine.TipoPrazo.PRAZO_PERICIA;
        }
        if (context.isReclamacaoConstitucional()) {
            return NationalPrazoEngine.TipoPrazo.RECLAMACAO_CONSTITUCIONAL;
        }
        if (context.isConflitoCompetencia()) {
            return NationalPrazoEngine.TipoPrazo.CONFLITO_COMPETENCIA;
        }
        if (context.isIncidenteRepetitivoOuAssuncao()) {
            return NationalPrazoEngine.TipoPrazo.INCIDENTE_REPETITIVO_ASSUNCAO;
        }
        if (context.isSuspensaoSegurancaOuLiminar()) {
            return NationalPrazoEngine.TipoPrazo.SUSPENSAO_SEGURANCA_LIMINAR;
        }
        if (context.isAlegacoesFinaisPenal()) {
            return NationalPrazoEngine.TipoPrazo.ALEGACOES_FINAIS_PENAL;
        }
        if (context.isContrarrazoes()) {
            return context.tribunalSuperior().isSuperior()
                    ? NationalPrazoEngine.TipoPrazo.CONTRARRAZOES_SUPERIOR
                    : NationalPrazoEngine.TipoPrazo.CONTRARRAZOES_APELACAO;
        }
        if (context.isRecursal()) {
            if (context.isAgravoInterno()) {
                return NationalPrazoEngine.TipoPrazo.AGRAVO_INTERNO;
            }
            if (context.isRecursoEspecialEstrito()) {
                return NationalPrazoEngine.TipoPrazo.RECURSO_ESPECIAL;
            }
            if (context.isRecursoExtraordinarioEstrito()) {
                return NationalPrazoEngine.TipoPrazo.RECURSO_EXTRAORDINARIO;
            }
            if (context.isAgravoEmRecursoSuperior()) {
                return NationalPrazoEngine.TipoPrazo.AGRAVO_RECURSO_SUPERIOR;
            }
            if (context.isAgravoInstrumentoOuRegimental()) {
                return NationalPrazoEngine.TipoPrazo.AGRAVO_INSTRUMENTO;
            }
            if (context.isRecursoOrdinarioConstitucional()) {
                return context.rito() != null && context.rito().isEleitoral()
                        ? NationalPrazoEngine.TipoPrazo.RECURSO_ELEITORAL
                        : NationalPrazoEngine.TipoPrazo.RECURSO_ORDINARIO_CONSTITUCIONAL;
            }
            if (context.rito() != null && context.rito().isTrabalhista()) {
                return NationalPrazoEngine.TipoPrazo.RECURSO_TRABALHISTA;
            }
            if (context.rito() != null && context.rito().isEleitoral()) {
                return NationalPrazoEngine.TipoPrazo.RECURSO_ELEITORAL;
            }
            if (context.rito() != null && context.rito().isMilitar()) {
                return NationalPrazoEngine.TipoPrazo.RECURSO_MILITAR;
            }
            return NationalPrazoEngine.TipoPrazo.APELACAO;
        }
        if (context.isExecucao()) {
            return NationalPrazoEngine.TipoPrazo.EMBARGOS_EXECUCAO;
        }
        if (context.isExecucaoPenal()) {
            return NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO;
        }
        if (context.rito() != null && context.rito().isPenal()) {
            return NationalPrazoEngine.TipoPrazo.APRESENTACAO_DEFESA_PENAL;
        }
        if (context.rito() != null && context.rito().isMilitar()) {
            return context.isMilitarEspecial() ? NationalPrazoEngine.TipoPrazo.APRESENTACAO_DEFESA_PENAL : NationalPrazoEngine.TipoPrazo.RECURSO_MILITAR;
        }
        if (context.rito() != null && context.rito().isTrabalhista()) {
            return NationalPrazoEngine.TipoPrazo.RESPOSTA_TRABALHISTA;
        }
        if (context.rito() != null && context.rito().isEleitoral()) {
            return NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO;
        }
        if (context.isMandadoConstitucionalOuRemedioHeroico()) {
            return NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO;
        }
        return NationalPrazoEngine.TipoPrazo.CONTESTACAO;
    }

    private boolean devePriorizarRepresentanteDigital(ProceduralCommunicationContext context,
                                                      NationalPrazoEngine.TipoPrazo tipoPrazo) {
        if (!context.hasRepresentanteDigitalNatural()) {
            return false;
        }
        return context.isRecursal()
                || context.isContrarrazoes()
                || context.isTribunalSuperiorOuConstitucional()
                || tipoPrazo == NationalPrazoEngine.TipoPrazo.APELACAO
                || tipoPrazo == NationalPrazoEngine.TipoPrazo.CONTRARRAZOES_APELACAO
                || tipoPrazo == NationalPrazoEngine.TipoPrazo.EMBARGOS_DECLARACAO
                || tipoPrazo == NationalPrazoEngine.TipoPrazo.AGRAVO_INSTRUMENTO
                || tipoPrazo == NationalPrazoEngine.TipoPrazo.AGRAVO_INTERNO
                || tipoPrazo == NationalPrazoEngine.TipoPrazo.RECURSO_ESPECIAL
                || tipoPrazo == NationalPrazoEngine.TipoPrazo.RECURSO_EXTRAORDINARIO
                || context.isInternacionalOuCooperacao()
                || context.tipoComunicacao() == TipoComunicacaoJudicial.INTIMACAO_ADVOGADO
                || context.tipoComunicacao() == TipoComunicacaoJudicial.INTIMACAO_DIGITAL_MNI;
    }

    private boolean devePriorizarOficial(ProceduralCommunicationContext context,
                                         NationalPrazoEngine.TipoPrazo tipoPrazo) {
        return context.tipoComunicacao().isExigePessoalidade()
                || context.isPenalSensivel()
                || context.isMilitarEspecial() && context.tipoComunicacao().isCitacao()
                || context.isEleitoralEspecial() && context.tipoComunicacao().isCitacao()
                || context.isExecucaoPenal()
                || context.isFamiliaOuInfanciaSensivel() && context.tipoComunicacao().isCitacao()
                || context.isExecucao() && context.tipoComunicacao().isMandado()
                || context.fase() == FaseProcessual.PENHORA
                || tipoPrazo == NationalPrazoEngine.TipoPrazo.APRESENTACAO_DEFESA_PENAL
                || context.destinatario() instanceof CitacaoIntimacaoEngine.PerfilDestinatario.MinisterioPublico
                || context.destinatario() instanceof CitacaoIntimacaoEngine.PerfilDestinatario.DefensorPublico;
    }

    private boolean deveBloquearPresuncao(ProceduralCommunicationContext context,
                                          boolean priorizarOficialJustica) {
        return priorizarOficialJustica
                || context.tipoComunicacao().isBloqueiaPresuncaoRebeldia()
                || context.isPenalSensivel()
                || context.isEleitoralEspecial()
                || context.isMilitarEspecial()
                || context.isExecucaoPenal()
                || context.hasSensitiveMaterial()
                || context.isFamiliaOuInfanciaSensivel()
                || context.isMandadoConstitucionalOuRemedioHeroico()
                || context.isTribunalSuperiorOuConstitucional();
    }

    private boolean deveAdmitirHoraCerta(ProceduralCommunicationContext context,
                                         boolean priorizarRepresentanteDigital,
                                         boolean priorizarOficialJustica) {
        if (!context.tipoComunicacao().isCitacao() || context.tipoComunicacao().isEdital()) {
            return false;
        }
        if (priorizarRepresentanteDigital
                || context.isMandadoConstitucionalOuRemedioHeroico()
                || context.isInternacionalOuCooperacao()
                || context.isTribunalSuperiorOuConstitucional()
                || context.isMilitarEspecial()
                || context.isEleitoralEspecial()
                || context.isExecucaoPenal()) {
            return false;
        }
        if (!priorizarOficialJustica) {
            return context.fase() == null
                    || context.fase() == FaseProcessual.CONHECIMENTO
                    || context.fase() == FaseProcessual.EXECUCAO
                    || context.fase() == FaseProcessual.CUMPRIMENTO_SENTENCA;
        }
        return context.isExecucao()
                || context.isCumprimentoSentenca()
                || context.isJuizadoOuRitoSimplificado()
                || context.microssistema() == ComunicacaoJudicialMicrossistema.CIVEL_COMUM
                || context.microssistema() == ComunicacaoJudicialMicrossistema.CIVEL_ESPECIAL;
    }

    private List<String> construirMarcadores(ProceduralCommunicationContext context,
                                             NationalPrazoEngine.TipoPrazo tipoPrazo,
                                             boolean priorizarRepresentanteDigital,
                                             boolean priorizarOficialJustica,
                                             boolean bloquearPresuncao,
                                             boolean admitirHoraCerta,
                                             boolean exigirCurador) {
        List<String> marcadores = new ArrayList<>();
        marcadores.add("microssistema=" + context.microssistema().name());
        if (context.rito() != null) {
            marcadores.add("rito=" + context.rito().name());
        }
        if (context.fase() != null) {
            marcadores.add("fase=" + context.fase().name());
        }
        if (context.status() != null) {
            marcadores.add("status=" + context.status().name());
        }
        if (context.ramo() != null) {
            marcadores.add("ramo=" + context.ramo().name());
        }
        if (context.materia() != null) {
            marcadores.add("materia=" + context.materia().name());
        }
        if (context.grau() != null) {
            marcadores.add("grau=" + context.grau().name());
        }
        if (context.tribunalCodigo() != null) {
            marcadores.add("tribunal=" + context.tribunalCodigo());
        }
        marcadores.add("prazo=" + tipoPrazo.name());
        if (context.isRecursal()) {
            marcadores.add("faixa=recursal");
        }
        if (context.tribunalSuperior().isSuperior()) {
            marcadores.add("tribunalSuperior=" + context.tribunalSuperior().name());
        }
        if (context.isContrarrazoes()) {
            marcadores.add("movimento=contrarrazoes");
        }
        if (context.isEmbargosDeclaracao()) {
            marcadores.add("embargos=declaracao");
        }
        if (context.isEmbargosExecucao()) {
            marcadores.add("embargos=execucao");
        }
        if (context.isEmbargosTerceiro()) {
            marcadores.add("embargos=terceiro");
        }
        if (context.isEmbargosDivergencia()) {
            marcadores.add("embargos=divergencia");
        }
        if (context.isEmbargosInfringentesOuNulidade()) {
            marcadores.add("embargos=infringentes_nulidade");
        }
        if (context.isAgravoInterno()) {
            marcadores.add("recurso=agravo_interno");
        }
        if (context.isAgravoEmRecursoSuperior()) {
            marcadores.add("recurso=agravo_recurso_superior");
        }
        if (context.isAgravoInstrumentoOuRegimental()) {
            marcadores.add("recurso=agravo");
        }
        if (context.isRecursoOrdinarioConstitucional()) {
            marcadores.add("recurso=ordinario_constitucional");
        }
        if (context.isRecursoEspecialEstrito()) {
            marcadores.add("recurso=especial");
        }
        if (context.isRecursoExtraordinarioEstrito()) {
            marcadores.add("recurso=extraordinario");
        }
        if (context.isReclamacaoConstitucional()) {
            marcadores.add("incidente=reclamacao_constitucional");
        }
        if (context.isConflitoCompetencia()) {
            marcadores.add("incidente=conflito_competencia");
        }
        if (context.isIncidenteRepetitivoOuAssuncao()) {
            marcadores.add("incidente=repetitivo_assuncao");
        }
        if (context.isSuspensaoSegurancaOuLiminar()) {
            marcadores.add("incidente=suspensao_seguranca_liminar");
        }
        if (context.exigeRevisaoRegimentalHumana()) {
            marcadores.add("revisaoRegimentalTribunal");
        }
        if (priorizarRepresentanteDigital) {
            marcadores.add("viaRepresentanteDigital");
            marcadores.add("materializacao=representante_digital");
        }
        if (priorizarOficialJustica) {
            marcadores.add("pessoalidadeReforcada");
            marcadores.add("materializacao=oficial_justica");
        }
        if (!priorizarRepresentanteDigital && !priorizarOficialJustica && context.tipoComunicacao().isAdmiteDigital()) {
            marcadores.add("materializacao=digital_direta");
        }
        if (bloquearPresuncao) {
            marcadores.add("semPresuncaoAutomatica");
        }
        if (admitirHoraCerta) {
            marcadores.add("horaCertaHabilitada");
            marcadores.add("materializacao=hora_certa");
        }
        if (context.isInternacionalOuCooperacao()) {
            marcadores.add("materializacao=cooperacao_externa");
        }
        if (context.isExecucao() || context.isCumprimentoSentenca() || context.isExecucaoPenal()) {
            marcadores.add("materializacao=executiva");
        }
        if (context.isPericial()) {
            marcadores.add("materialSensivel=pericial");
        }
        if (exigirCurador) {
            marcadores.add("curadoriaPotencial");
        }
        return marcadores;
    }

    private String construirEixo(ProceduralCommunicationContext context,
                                 NationalPrazoEngine.TipoPrazo tipoPrazo) {
        StringBuilder sb = new StringBuilder(128);
        appendSegment(sb, context.microssistema().name());
        appendSegment(sb, context.grau() != null ? context.grau().name() : null);
        appendSegment(sb, context.rito() != null ? context.rito().name() : null);
        appendSegment(sb, context.fase() != null ? context.fase().name() : null);
        appendSegment(sb, context.status() != null ? context.status().name() : null);
        appendSegment(sb, context.ramo() != null ? context.ramo().name() : null);
        appendSegment(sb, tipoPrazo.name());
        return sb.toString();
    }

    private String construirFundamento(ProceduralCommunicationContext context,
                                       NationalPrazoEngine.TipoPrazo tipoPrazo,
                                       boolean priorizarRepresentanteDigital,
                                       boolean priorizarOficialJustica,
                                       boolean bloquearPresuncao,
                                       boolean admitirHoraCerta,
                                       boolean exigirCurador) {
        List<String> partes = new ArrayList<>();
        partes.add("Matriz procedimental nacional aplicada ao fluxo comunicacional");
        partes.add("microssistema=" + context.microssistema().name());
        if (context.grau() != null) {
            partes.add("grau=" + context.grau().name());
        }
        if (context.rito() != null) {
            partes.add("rito=" + context.rito().name());
        }
        if (context.fase() != null) {
            partes.add("fase=" + context.fase().name());
        }
        if (context.status() != null) {
            partes.add("status=" + context.status().name());
        }
        if (context.ramo() != null) {
            partes.add("ramo=" + context.ramo().name());
        }
        if (context.materia() != null) {
            partes.add("materia=" + context.materia().name());
        }
        partes.add("prazo=" + tipoPrazo.name());
        if (context.isRecursal()) {
            partes.add("camada recursal ou pós-sentença identificada");
        }
        if (context.tribunalSuperior().isSuperior()) {
            partes.add("tribunalSuperior=" + context.tribunalSuperior().name());
        }
        if (context.isEmbargosDeclaracao() || context.isEmbargosExecucao() || context.isEmbargosTerceiro() || context.isEmbargosDivergencia() || context.isEmbargosInfringentesOuNulidade()) {
            partes.add("subespécie de embargos reconhecida para reforço de rito e competência");
        }
        if (priorizarRepresentanteDigital) {
            partes.add("preferência por ciência via representante constituído em ambiente interoperável");
        }
        if (priorizarOficialJustica) {
            partes.add("pessoalidade reforçada e coleta física priorizada por sensibilidade do ato");
        }
        if (bloquearPresuncao) {
            partes.add("presunção automática bloqueada por risco processual, grau de jurisdição ou material probatório sensível");
        }
        if (admitirHoraCerta) {
            partes.add("hora certa habilitada como trilha subsidiária antes do edital");
        }
        if (exigirCurador) {
            partes.add("rastreamento preventivo de curadoria especial");
        }
        if (context.isInternacionalOuCooperacao()) {
            partes.add("cooperação jurídica especializada requerida");
        }
        if (context.exigeRevisaoRegimentalHumana()) {
            partes.add("classe rara, incidente superior ou subespécie recursal exige leitura regimental do tribunal competente antes da automação final");
        }
        return String.join(" | ", partes);
    }

    private void appendSegment(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('/');
        }
        sb.append(value);
    }
}
