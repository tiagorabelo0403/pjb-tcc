package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

public record ProceduralCommunicationContext(
        TipoComunicacaoJudicial tipoComunicacao,
        CitacaoIntimacaoEngine.PerfilDestinatario destinatario,
        RitoProcessual rito,
        FaseProcessual fase,
        StatusProcesso status,
        RamoDireito ramo,
        MateriaJurisdicao materia,
        GrauJurisdicao grau,
        String classeTpu,
        String classeProcessual,
        String objeto,
        String pedido,
        String pedidosConsolidados,
        String materialProbatorio,
        Integer materialProbatorioScore,
        String tribunalCodigo
) {

    public static ProceduralCommunicationContext from(Processo processo,
                                                      TipoComunicacaoJudicial tipoComunicacao,
                                                      CitacaoIntimacaoEngine.PerfilDestinatario destinatario) {
        Objects.requireNonNull(tipoComunicacao, "tipoComunicacao");
        return new ProceduralCommunicationContext(
                tipoComunicacao,
                destinatario,
                processo != null ? processo.getRito() : null,
                processo != null ? processo.getFaseAtual() : null,
                processo != null ? processo.getStatusProcesso() : null,
                processo != null ? processo.getRamoDireito() : null,
                processo != null ? processo.getMateria() : null,
                processo != null && processo.getJurisdicao() != null ? processo.getJurisdicao().getGrau() : null,
                normalize(processo != null ? processo.getClasseTpuCodigo() : null),
                normalize(processo != null ? processo.getClasseProcessual() : null),
                normalize(processo != null ? processo.getObjetoProcessual() : null),
                normalize(processo != null ? processo.getPedidoPrincipal() : null),
                normalize(processo != null ? processo.getPedidosConsolidados() : null),
                normalize(processo != null ? processo.getMaterialProbatorioResumo() : null),
                processo != null ? processo.getMaterialProbatorioScore() : null,
                normalize(processo != null ? processo.getTribunalCodigoRoteado() : null)
        );
    }

    public boolean isRecursal() {
        return fase == FaseProcessual.RECURSAL
                || status == StatusProcesso.RECURSO_INTERPOSTO
                || isAgravoInterno()
                || isAgravoInstrumentoOuRegimental()
                || isRecursoEspecialEstrito()
                || isRecursoExtraordinarioEstrito()
                || isContrarrazoes();
    }

    public boolean isEmbargosDeclaracao() {
        return status == StatusProcesso.EMBARGOS_DECLARACAO
                || containsAny(objeto, "EMBARGOS DE DECLARACAO", "EMBARGOS DECLARATORIOS", "EDCL")
                || containsAny(pedido, "EMBARGOS DE DECLARACAO", "EMBARGOS DECLARATORIOS", "EDCL")
                || containsAny(pedidosConsolidados, "EMBARGOS DE DECLARACAO", "EMBARGOS DECLARATORIOS", "EDCL")
                || containsAny(classeProcessual, "EMBARGOS DE DECLARACAO", "EMBARGOS DECLARATORIOS", "EDCL")
                || containsAny(classeTpu, "EMBARGOS DECLARACAO", "EMBARGOS", "EDCL");
    }

    public boolean isEmbargosExecucao() {
        return containsAny(objeto, "EMBARGOS A EXECUCAO", "EMBARGOS EXECUCAO", "EMBARGOS A EXECUCAO FISCAL")
                || containsAny(pedido, "EMBARGOS A EXECUCAO", "EMBARGOS EXECUCAO", "EMBARGOS A EXECUCAO FISCAL")
                || containsAny(classeProcessual, "EMBARGOS A EXECUCAO", "EMBARGOS EXECUCAO", "EMBARGOS A EXECUCAO FISCAL")
                || containsAny(classeTpu, "EMBARGOS EXECUCAO", "EMB EXEC")
                || rito == RitoProcessual.TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL;
    }

    public boolean isEmbargosTerceiro() {
        return containsAny(objeto, "EMBARGOS DE TERCEIRO")
                || containsAny(pedido, "EMBARGOS DE TERCEIRO")
                || containsAny(classeProcessual, "EMBARGOS DE TERCEIRO")
                || containsAny(classeTpu, "EMBARGOS DE TERCEIRO");
    }

    public boolean isEmbargosDivergencia() {
        return containsAny(objeto, "EMBARGOS DE DIVERGENCIA")
                || containsAny(pedido, "EMBARGOS DE DIVERGENCIA")
                || containsAny(classeProcessual, "EMBARGOS DE DIVERGENCIA")
                || containsAny(classeTpu, "EMBARGOS DE DIVERGENCIA", "ERESP", "ERE")
                || exactAny(classeTpu, "ERESP", "ERE");
    }

    public boolean isExecucao() {
        return fase == FaseProcessual.EXECUCAO
                || rito == RitoProcessual.EXECUCAO_TITULO_EXTRAJUDICIAL
                || rito == RitoProcessual.EXECUCAO_TITULO_JUDICIAL
                || rito == RitoProcessual.EXECUCAO_FISCAL
                || rito == RitoProcessual.TRABALHISTA_EXECUCAO
                || containsAny(objeto, "EXECUCAO", "PENHORA", "BLOQUEIO", "ARRESTO", "SEQUESTRO")
                || containsAny(pedido, "EXECUCAO", "PENHORA", "BLOQUEIO", "ARRESTO", "SEQUESTRO")
                || containsAny(classeProcessual, "EXECUCAO");
    }

    public boolean isExecucaoPenal() {
        return rito == RitoProcessual.EXECUCAO_PENAL
                || containsAny(classeProcessual, "EXECUCAO PENAL")
                || containsAny(objeto, "REGIME PRISIONAL", "PROGRESSAO DE REGIME", "REMICAO", "LIVRAMENTO CONDICIONAL")
                || containsAny(pedido, "PROGRESSAO DE REGIME", "REMICAO", "LIVRAMENTO CONDICIONAL");
    }

    public boolean isCumprimentoSentenca() {
        return fase == FaseProcessual.CUMPRIMENTO_SENTENCA
                || status == StatusProcesso.CUMPRIMENTO_SENTENCA
                || rito == RitoProcessual.CUMPRIMENTO_SENTENCA
                || rito == RitoProcessual.CUMPRIMENTO_PROVISORIO
                || rito == RitoProcessual.TRABALHISTA_CUMPRIMENTO_SENTENCA
                || containsAny(classeProcessual, "CUMPRIMENTO DE SENTENCA")
                || containsAny(objeto, "CUMPRIMENTO DE SENTENCA")
                || containsAny(pedido, "CUMPRIMENTO DE SENTENCA");
    }

    public boolean isMandadoConstitucionalOuRemedioHeroico() {
        return isHabeasCorpus()
                || isMandadoSeguranca()
                || rito == RitoProcessual.ESPECIAL_HABEAS_DATA
                || rito == RitoProcessual.ESPECIAL_MANDADO_INJUNCAO
                || rito == RitoProcessual.ESPECIAL_MANDADO_INJUNCAO_COLETIVO
                || rito == RitoProcessual.ESPECIAL_ACAO_POPULAR
                || rito == RitoProcessual.ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE
                || rito == RitoProcessual.ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE
                || rito == RitoProcessual.ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL;
    }

    public boolean isMandadoSeguranca() {
        return rito == RitoProcessual.ESPECIAL_MANDADO_SEGURANCA
                || rito == RitoProcessual.ESPECIAL_MANDADO_SEGURANCA_COLETIVO
                || rito == RitoProcessual.TRIBUTARIO_MANDADO_SEGURANCA
                || rito == RitoProcessual.TRABALHISTA_MANDADO_SEGURANCA
                || containsAny(classeTpu, "MS", "MANDADO DE SEGURANCA")
                || containsAny(classeProcessual, "MANDADO DE SEGURANCA");
    }

    public boolean isHabeasCorpus() {
        return rito == RitoProcessual.ESPECIAL_HABEAS_CORPUS
                || rito == RitoProcessual.MILITAR_HABEAS_CORPUS_MILITAR
                || rito == RitoProcessual.PENAL_HABEAS_CORPUS_PREVENTIVO
                || containsAny(classeTpu, "HC", "HABEAS CORPUS")
                || containsAny(classeProcessual, "HABEAS CORPUS");
    }

    public boolean isAcaoRescisoria() {
        return rito == RitoProcessual.TRABALHISTA_ACAO_RESCISORIA
                || containsAny(classeProcessual, "ACAO RESCISORIA")
                || containsAny(classeTpu, "ACAO RESCISORIA", "AR")
                || containsAny(objeto, "DESCONSTITUICAO DE ACORDAO", "DESCONSTITUICAO DE SENTENCA");
    }

    public boolean isPericial() {
        return fase == FaseProcessual.PERICIA_TECNICA
                || containsAny(objeto, "PERICIA", "ASSISTENTE TECNICO")
                || containsAny(pedido, "PERICIA", "EXAME TECNICO")
                || containsAny(materialProbatorio, "PERICIA", "LAUDO", "DNA", "EXAME", "CADEIA DE CUSTODIA");
    }

    public boolean isPenalSensivel() {
        return rito != null && (rito.isPenal() || rito.isMilitar() || rito == RitoProcessual.TRIBUNAL_JURI || rito == RitoProcessual.EXECUCAO_PENAL)
                || fase == FaseProcessual.AUDIENCIA_CUSTODIA
                || fase == FaseProcessual.PRONUNCIA
                || fase == FaseProcessual.PLENARIO_JURI
                || containsAny(objeto, "PRISAO", "APREENSAO", "BUSCA", "INTERDICAO", "MEDIDA PROTETIVA")
                || containsAny(pedido, "PRISAO", "BUSCA", "MEDIDA PROTETIVA", "INTERNA")
                || containsAny(materialProbatorio, "CADEIA DE CUSTODIA", "DNA", "MIDIA", "PERICIA");
    }

    public boolean isFamiliaOuInfanciaSensivel() {
        return ramo == RamoDireito.FAMILIA
                || ramo == RamoDireito.INFANCIA_JUVENTUDE
                || materia == MateriaJurisdicao.FAMILIA
                || materia == MateriaJurisdicao.INFANCIA_JUVENTUDE
                || containsAny(objeto, "ALIMENTOS", "GUARDA", "ADOCAO", "PATERNIDADE", "CURATELA", "TUTELA")
                || containsAny(pedido, "ALIMENTOS", "GUARDA", "ADOCAO", "PATERNIDADE", "CURATELA", "TUTELA");
    }

    public boolean isInternacionalOuCooperacao() {
        return ramo == RamoDireito.INTERNACIONAL
                || rito == RitoProcessual.CARTA_ROGATORIA
                || rito == RitoProcessual.COOPERACAO_JURIDICA_INTERNACIONAL
                || rito == RitoProcessual.HOMOLOGACAO_SENTENCA_ESTRANGEIRA
                || containsAny(classeProcessual, "CARTA ROGATORIA", "COOPERACAO JURIDICA")
                || containsAny(objeto, "EXTERIOR", "AUTORIDADE CENTRAL", "CARTA ROGATORIA");
    }

    public boolean isJuizadoOuRitoSimplificado() {
        return rito == RitoProcessual.JUIZADO_ESPECIAL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_CIVEL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA
                || rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL
                || rito == RitoProcessual.SUMARIO
                || rito == RitoProcessual.SUMARIO_ESPECIAL
                || rito == RitoProcessual.TRABALHISTA_SUMARISSIMO
                || rito == RitoProcessual.TRABALHISTA_SUMARIO_ALCADA;
    }

    public boolean isCivilEspecial() {
        return rito != null && switch (rito) {
            case CIVIL_TUTELA_URGENTE,
                 CIVIL_TUTELA_CAUTELAR_ANTECEDENTE,
                 CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE,
                 CIVIL_FAMILIA_ALIMENTOS,
                 CIVIL_FAMILIA_DIVORCIO,
                 CIVIL_INVENTARIO_ARROLAMENTO,
                 CIVIL_ACAO_CIVIL_PUBLICA,
                 CIVIL_ACAO_MONITORIA,
                 CIVIL_USUCAPIAO,
                 CIVIL_POSSESSORIA,
                 CIVIL_CONSIGNACAO_PAGAMENTO,
                 CIVIL_DISSOLUCAO_CASAMENTO,
                 CIVIL_INVESTIGACAO_PATERNIDADE,
                 CIVIL_RECONHECIMENTO_PATERNIDADE,
                 CIVIL_ADOCAO,
                 CIVIL_TUTELA_CURATELA,
                 CIVIL_INTERDITO_PROIBITORIO,
                 CIVIL_RETIFICACAO_REGISTRO,
                 CIVIL_NUNCIACAO_OBRA_NOVA -> true;
            default -> false;
        };
    }

    public boolean isPenalEspecial() {
        return rito != null && switch (rito) {
            case PENAL_LEI_DROGAS,
                 PENAL_MARIA_DA_PENHA,
                 PENAL_CRIMES_TRANSITO,
                 PENAL_ESTATUTO_IDOSO,
                 PENAL_ECA_INFRACIONAL,
                 PENAL_LAVAGEM_DINHEIRO,
                 PENAL_ORGANIZACAO_CRIMINOSA,
                 PENAL_VIOLENCIA_POLITICA,
                 PENAL_CRIMES_CIBERNETICOS,
                 PENAL_RACISMO,
                 PENAL_TORTURA,
                 PENAL_TERRORISMO,
                 PENAL_CRIMES_CONTRA_HONRA,
                 PENAL_HABEAS_CORPUS_PREVENTIVO,
                 PENAL_REVISAO_CRIMINAL,
                 PENAL_RECLAMACAO_CRIMINAL,
                 TRIBUNAL_JURI -> true;
            default -> false;
        };
    }

    public boolean isMilitarEspecial() {
        return rito != null && switch (rito) {
            case MILITAR_IPM,
                 MILITAR_PROCESSO_PENAL_MILITAR,
                 MILITAR_CONSELHO_JUSTICA,
                 MILITAR_HABEAS_CORPUS_MILITAR -> true;
            default -> false;
        };
    }

    public boolean isEleitoralEspecial() {
        return rito != null && switch (rito) {
            case ELEITORAL_AIRC,
                 ELEITORAL_AIJE,
                 ELEITORAL_AIME,
                 ELEITORAL_RCED,
                 ELEITORAL_PROPAGANDA,
                 ELEITORAL_DIREITO_RESPOSTA,
                 ELEITORAL_PRESTACAO_CONTAS,
                 ELEITORAL_INELEGIBILIDADE,
                 ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO -> true;
            default -> false;
        };
    }

    public boolean isTrabalhistaEspecial() {
        return rito != null && switch (rito) {
            case TRABALHISTA_DISSIDIO_COLETIVO,
                 TRABALHISTA_ACAO_RESCISORIA,
                 TRABALHISTA_MANDADO_SEGURANCA,
                 TRABALHISTA_TUTELA_CAUTELAR,
                 TRABALHISTA_ACIDENTE_TRABALHO,
                 TRABALHISTA_SUMARISSIMO,
                 TRABALHISTA_SUMARIO_ALCADA,
                 TRABALHISTA_INQUERITO_FALTA_GRAVE,
                 TRABALHISTA_ACAO_CUMPRIMENTO -> true;
            default -> false;
        };
    }

    public boolean isFazendaPublica() {
        return rito == RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO
                || rito == RitoProcessual.FAZENDA_PUBLICA_EXECUCAO
                || rito == RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA;
    }

    public boolean isTributario() {
        return rito != null && (rito.name().startsWith("TRIBUTARIO") || rito == RitoProcessual.EXECUCAO_FISCAL);
    }

    public boolean isPrevidenciario() {
        return rito != null && rito.isPrevidenciario();
    }

    public boolean isInfanciaOuMenorista() {
        return rito != null && rito.isInfancia()
                || ramo == RamoDireito.INFANCIA_JUVENTUDE
                || materia == MateriaJurisdicao.INFANCIA_JUVENTUDE;
    }

    public boolean isEmpresarialRecuperacional() {
        return rito != null && rito.isEmpresarial();
    }

    public boolean isAmbiental() {
        return rito != null && rito.isAmbiental() || ramo == RamoDireito.AMBIENTAL;
    }

    public boolean isAgrario() {
        return rito != null && rito.isAgrario() || ramo == RamoDireito.AGRARIO;
    }

    public boolean isAdministrativoImprobidade() {
        return rito != null && rito.isAdministrativo() || ramo == RamoDireito.ADMINISTRATIVO;
    }

    public boolean isAutocompositivo() {
        return rito != null && rito.isAutocompositivo();
    }

    public boolean isConstitucionalOriginario() {
        return rito != null && rito.isEspecialConstitucional() && rito != RitoProcessual.ESPECIAL_HABEAS_CORPUS && rito != RitoProcessual.ESPECIAL_MANDADO_SEGURANCA && rito != RitoProcessual.ESPECIAL_MANDADO_SEGURANCA_COLETIVO
                || grau == GrauJurisdicao.CONSTITUCIONAL;
    }

    public boolean isAgravoInterno() {
        return containsAny(classeTpu, "AGRAVO INTERNO", "AGINT")
                || containsAny(classeProcessual, "AGRAVO INTERNO", "AGRAVO REGIMENTAL")
                || containsAny(objeto, "AGRAVO INTERNO", "AGRAVO REGIMENTAL")
                || containsAny(pedido, "AGRAVO INTERNO", "AGRAVO REGIMENTAL");
    }

    public boolean isAgravoInstrumentoOuRegimental() {
        return containsAny(classeTpu, "AGRAVO INSTRUMENTO", "ARESP", "ARE")
                || exactAny(classeTpu, "AI", "ARESP", "ARE")
                || containsAny(classeProcessual, "AGRAVO DE INSTRUMENTO", "AGRAVO EM RECURSO ESPECIAL", "AGRAVO EM RECURSO EXTRAORDINARIO")
                || containsAny(objeto, "AGRAVO DE INSTRUMENTO", "AGRAVO")
                || containsAny(pedido, "AGRAVO DE INSTRUMENTO", "AGRAVO");
    }

    public boolean isAgravoEmRecursoSuperior() {
        return containsAny(classeProcessual, "AGRAVO EM RECURSO ESPECIAL", "AGRAVO EM RECURSO EXTRAORDINARIO")
                || containsAny(classeTpu, "ARESP", "ARE")
                || exactAny(classeTpu, "ARESP", "ARE");
    }

    public boolean isRecursoOrdinarioConstitucional() {
        return containsAny(classeProcessual, "RECURSO ORDINARIO CONSTITUCIONAL", "RECURSO ORDINARIO")
                || containsAny(classeTpu, "ROC", "RO")
                || exactAny(classeTpu, "ROC");
    }

    public boolean isReclamacaoConstitucional() {
        return containsAny(classeProcessual, "RECLAMACAO")
                || containsAny(classeTpu, "RCL")
                || containsAny(objeto, "RECLAMACAO CONSTITUCIONAL", "USURPACAO DE COMPETENCIA")
                || containsAny(pedido, "RECLAMACAO CONSTITUCIONAL", "GARANTIA DE AUTORIDADE DE DECISAO");
    }

    public boolean isConflitoCompetencia() {
        return containsAny(classeProcessual, "CONFLITO DE COMPETENCIA")
                || containsAny(classeTpu, "CC")
                || containsAny(objeto, "CONFLITO DE COMPETENCIA")
                || containsAny(pedido, "CONFLITO DE COMPETENCIA");
    }

    public boolean isIncidenteRepetitivoOuAssuncao() {
        return containsAny(classeProcessual, "IRDR", "INCIDENTE DE RESOLUCAO DE DEMANDAS REPETITIVAS", "IAC", "INCIDENTE DE ASSUNCAO DE COMPETENCIA")
                || containsAny(classeTpu, "IRDR", "IAC", "SIRDR")
                || containsAny(objeto, "DEMANDA REPETITIVA", "ASSUNCAO DE COMPETENCIA", "TEMA REPETITIVO")
                || containsAny(pedido, "AFETACAO", "SUSPENSAO NACIONAL DE PROCESSOS");
    }

    public boolean isSuspensaoSegurancaOuLiminar() {
        return containsAny(classeProcessual, "SUSPENSAO DE SEGURANCA", "SUSPENSAO DE LIMINAR", "SUSPENSAO DE SENTENCA")
                || containsAny(classeTpu, "SS", "SLS", "SL", "SUSPENSAO")
                || containsAny(objeto, "SUSPENSAO DE SEGURANCA", "SUSPENSAO DE LIMINAR", "SUSPENSAO DE SENTENCA")
                || containsAny(pedido, "SUSTAR EFICACIA", "SUSPENDER LIMINAR", "SUSPENDER DECISAO");
    }

    public boolean isEmbargosInfringentesOuNulidade() {
        return containsAny(classeProcessual, "EMBARGOS INFRINGENTES", "EMBARGOS DE NULIDADE")
                || containsAny(classeTpu, "EIN", "EMBNUL")
                || containsAny(objeto, "EMBARGOS INFRINGENTES", "EMBARGOS DE NULIDADE")
                || containsAny(pedido, "EMBARGOS INFRINGENTES", "EMBARGOS DE NULIDADE");
    }

    public boolean isPedidoProvidenciaOuCorrecaoParcial() {
        return containsAny(classeProcessual, "PEDIDO DE PROVIDENCIAS", "CORREICAO PARCIAL", "CORRIGENDA")
                || containsAny(classeTpu, "PP", "CP")
                || containsAny(objeto, "PEDIDO DE PROVIDENCIAS", "CORREICAO PARCIAL")
                || containsAny(pedido, "PEDIDO DE PROVIDENCIAS", "CORREICAO PARCIAL");
    }

    public boolean isRecursoEspecialEstrito() {
        return containsAny(classeTpu, "RECURSO ESPECIAL")
                || exactAny(classeTpu, "RESP")
                || containsAny(classeProcessual, "RECURSO ESPECIAL")
                || containsAny(objeto, "RECURSO ESPECIAL")
                || containsAny(pedido, "RECURSO ESPECIAL");
    }

    public boolean isRecursoExtraordinarioEstrito() {
        return containsAny(classeTpu, "RECURSO EXTRAORDINARIO", "R EXTRAORDINARIO")
                || exactAny(classeTpu, "RE")
                || containsAny(classeProcessual, "RECURSO EXTRAORDINARIO")
                || containsAny(objeto, "RECURSO EXTRAORDINARIO")
                || containsAny(pedido, "RECURSO EXTRAORDINARIO");
    }

    public boolean isContrarrazoes() {
        return containsAny(classeProcessual, "CONTRARRAZOES", "CONTRA-RAZOES")
                || containsAny(objeto, "CONTRARRAZOES", "CONTRA-RAZOES")
                || containsAny(pedido, "CONTRARRAZOES", "CONTRA-RAZOES");
    }

    public boolean isAlegacoesFinaisPenal() {
        return containsAny(classeProcessual, "ALEGACOES FINAIS")
                || containsAny(objeto, "ALEGACOES FINAIS")
                || containsAny(pedido, "ALEGACOES FINAIS");
    }

    public boolean isTribunalSuperiorOuConstitucional() {
        return grau == GrauJurisdicao.SUPERIOR
                || grau == GrauJurisdicao.CONSTITUCIONAL
                || tribunalSuperiorCode();
    }

    public boolean hasSensitiveMaterial() {
        return isPericial()
                || isPenalSensivel()
                || materialProbatorioScore != null && materialProbatorioScore >= 85;
    }

    public boolean hasRepresentanteDigitalNatural() {
        return destinatario instanceof CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab
                || destinatario instanceof CitacaoIntimacaoEngine.PerfilDestinatario.DefensorPublico
                || destinatario instanceof CitacaoIntimacaoEngine.PerfilDestinatario.MinisterioPublico
                || destinatario instanceof CitacaoIntimacaoEngine.PerfilDestinatario.FazendaPublica
                || destinatario instanceof CitacaoIntimacaoEngine.PerfilDestinatario.JuizoDeprecado
                || destinatario instanceof CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica pessoaFisica && pessoaFisica.possuiAdvogado();
    }

    public boolean exigeCuradoriaPotencial() {
        return tipoComunicacao.isEdital()
                || containsAny(objeto, "INCAPAZ", "MENOR", "INTERDITANDO", "INTERDITO")
                || containsAny(pedido, "CURADOR", "CURATELA", "TUTELA")
                || isFamiliaOuInfanciaSensivel();
    }

    public ComunicacaoJudicialTribunalSuperior tribunalSuperior() {
        return ComunicacaoJudicialTribunalSuperiorResolver.resolver(this);
    }

    public boolean exigeRevisaoRegimentalHumana() {
        return isEmbargosDivergencia()
                || isAgravoEmRecursoSuperior()
                || isRecursoOrdinarioConstitucional()
                || isReclamacaoConstitucional()
                || isConflitoCompetencia()
                || isIncidenteRepetitivoOuAssuncao()
                || isSuspensaoSegurancaOuLiminar()
                || isEmbargosInfringentesOuNulidade()
                || isPedidoProvidenciaOuCorrecaoParcial();
    }

    public ComunicacaoJudicialMicrossistema microssistema() {
        return ComunicacaoJudicialMicrossistemaResolver.resolver(this);
    }

    public boolean containsAny(String source, String... tokens) {
        return containsAnyStatic(source, tokens);
    }

    private boolean tribunalSuperiorCode() {
        return tribunalSuperior().isSuperior();
    }

    private static boolean containsAnyStatic(String source, String... tokens) {
        if (source == null || source.isBlank() || tokens == null || tokens.length == 0) {
            return false;
        }
        String normalized = normalize(source);
        for (String token : tokens) {
            String tokenNormalized = normalize(token);
            if (tokenNormalized == null) {
                continue;
            }
            if (normalized.contains(tokenNormalized)) {
                return true;
            }
        }
        return false;
    }

    private static boolean exactAny(String source, String... tokens) {
        String normalized = normalize(source);
        if (normalized == null || tokens == null || tokens.length == 0) {
            return false;
        }
        for (String token : tokens) {
            String tokenNormalized = normalize(token);
            if (normalized.equals(tokenNormalized)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('Ç', 'C')
                .trim()
                .toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
