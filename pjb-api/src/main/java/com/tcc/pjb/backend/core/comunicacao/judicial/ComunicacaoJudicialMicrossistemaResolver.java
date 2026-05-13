package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public final class ComunicacaoJudicialMicrossistemaResolver {

    private ComunicacaoJudicialMicrossistemaResolver() {
    }

    public static ComunicacaoJudicialMicrossistema resolver(ProceduralCommunicationContext context) {
        if (context == null) {
            return ComunicacaoJudicialMicrossistema.CIVEL_COMUM;
        }
        if (context.isInternacionalOuCooperacao()) {
            return ComunicacaoJudicialMicrossistema.INTERNACIONAL_COOPERACAO;
        }
        if (context.isTribunalSuperiorOuConstitucional()) {
            ComunicacaoJudicialTribunalSuperior tribunalSuperior = context.tribunalSuperior();
            if (context.isConstitucionalOriginario() && tribunalSuperior.isConstitucional()) {
                return ComunicacaoJudicialMicrossistema.CONSTITUCIONAL_ORIGINARIO;
            }
            return switch (tribunalSuperior) {
                case STF, CONSTITUCIONAL_GENERICO -> ComunicacaoJudicialMicrossistema.CONSTITUCIONAL_RECURSAL;
                case TST -> ComunicacaoJudicialMicrossistema.TRIBUNAL_SUPERIOR_TRABALHISTA;
                case TSE -> ComunicacaoJudicialMicrossistema.TRIBUNAL_SUPERIOR_ELEITORAL;
                case STM -> ComunicacaoJudicialMicrossistema.TRIBUNAL_SUPERIOR_MILITAR;
                case STJ, SUPERIOR_GENERICO, NENHUM -> ComunicacaoJudicialMicrossistema.TRIBUNAIS_SUPERIORES_CIVEIS;
            };
        }
        if (context.isConstitucionalOriginario() || context.isMandadoConstitucionalOuRemedioHeroico()) {
            return ComunicacaoJudicialMicrossistema.CONSTITUCIONAL_ORIGINARIO;
        }
        if (context.isEleitoralEspecial()) {
            return ComunicacaoJudicialMicrossistema.ELEITORAL_ESPECIAL;
        }
        if (context.rito() != null && context.rito().isEleitoral()) {
            return ComunicacaoJudicialMicrossistema.ELEITORAL_COMUM;
        }
        if (context.isMilitarEspecial()) {
            return ComunicacaoJudicialMicrossistema.MILITAR_ESPECIAL;
        }
        if (context.rito() != null && context.rito().isMilitar()) {
            return ComunicacaoJudicialMicrossistema.MILITAR_COMUM;
        }
        if (context.isExecucaoPenal()) {
            return ComunicacaoJudicialMicrossistema.EXECUCAO_PENAL;
        }
        if (context.isPenalEspecial()) {
            return ComunicacaoJudicialMicrossistema.PENAL_ESPECIAL;
        }
        if (context.rito() != null && context.rito().isPenal()) {
            return ComunicacaoJudicialMicrossistema.PENAL_COMUM;
        }
        if (context.isTrabalhistaEspecial() || context.rito() != null && context.rito().isTrabalhista()) {
            return ComunicacaoJudicialMicrossistema.TRABALHISTA;
        }
        if (context.isFazendaPublica()) {
            return ComunicacaoJudicialMicrossistema.FAZENDA_PUBLICA;
        }
        if (context.isTributario()) {
            return ComunicacaoJudicialMicrossistema.TRIBUTARIO;
        }
        if (context.isPrevidenciario()) {
            return ComunicacaoJudicialMicrossistema.PREVIDENCIARIO;
        }
        if (context.isInfanciaOuMenorista()) {
            return ComunicacaoJudicialMicrossistema.INFANCIA_JUVENTUDE;
        }
        if (context.isEmpresarialRecuperacional()) {
            return ComunicacaoJudicialMicrossistema.EMPRESARIAL_RECUPERACIONAL;
        }
        if (context.isAmbiental()) {
            return ComunicacaoJudicialMicrossistema.AMBIENTAL;
        }
        if (context.isAgrario()) {
            return ComunicacaoJudicialMicrossistema.AGRARIO;
        }
        if (context.isAdministrativoImprobidade()) {
            return ComunicacaoJudicialMicrossistema.ADMINISTRATIVO_IMPROBIDADE;
        }
        if (context.isAutocompositivo()) {
            return ComunicacaoJudicialMicrossistema.AUTOCOMPOSICAO;
        }
        if (context.isJuizadoOuRitoSimplificado()) {
            return ComunicacaoJudicialMicrossistema.JUIZADOS;
        }
        if (context.isCivilEspecial()) {
            return ComunicacaoJudicialMicrossistema.CIVEL_ESPECIAL;
        }
        return ComunicacaoJudicialMicrossistema.CIVEL_COMUM;
    }

    public static ComunicacaoJudicialMicrossistema resolver(Processo processo) {
        if (processo == null) {
            return ComunicacaoJudicialMicrossistema.CIVEL_COMUM;
        }
        RitoProcessual rito = processo.getRito();
        if (rito == null) {
            return ComunicacaoJudicialMicrossistema.CIVEL_COMUM;
        }
        if (rito.isInternacional()) {
            return ComunicacaoJudicialMicrossistema.INTERNACIONAL_COOPERACAO;
        }
        if (processo.getJurisdicao() != null && processo.getJurisdicao().getGrau() != null) {
            switch (processo.getJurisdicao().getGrau()) {
                case CONSTITUCIONAL:
                    return ComunicacaoJudicialMicrossistema.CONSTITUCIONAL_RECURSAL;
                case SUPERIOR:
                    if (processo.getRamoDireito() == RamoDireito.TRABALHISTA || rito.isTrabalhista()) {
                        return ComunicacaoJudicialMicrossistema.TRIBUNAL_SUPERIOR_TRABALHISTA;
                    }
                    if (processo.getRamoDireito() == RamoDireito.ELEITORAL || rito.isEleitoral()) {
                        return ComunicacaoJudicialMicrossistema.TRIBUNAL_SUPERIOR_ELEITORAL;
                    }
                    if (processo.getRamoDireito() == RamoDireito.MILITAR || rito.isMilitar()) {
                        return ComunicacaoJudicialMicrossistema.TRIBUNAL_SUPERIOR_MILITAR;
                    }
                    return ComunicacaoJudicialMicrossistema.TRIBUNAIS_SUPERIORES_CIVEIS;
                default:
                    break;
            }
        }
        if (rito.isEspecialConstitucional()
                && rito != RitoProcessual.ESPECIAL_HABEAS_CORPUS
                && rito != RitoProcessual.ESPECIAL_MANDADO_SEGURANCA
                && rito != RitoProcessual.ESPECIAL_MANDADO_SEGURANCA_COLETIVO) {
            return ComunicacaoJudicialMicrossistema.CONSTITUCIONAL_ORIGINARIO;
        }
        if (rito.isEleitoral()) {
            return switch (rito) {
                case ELEITORAL_AIRC,
                     ELEITORAL_AIJE,
                     ELEITORAL_AIME,
                     ELEITORAL_RCED,
                     ELEITORAL_PROPAGANDA,
                     ELEITORAL_DIREITO_RESPOSTA,
                     ELEITORAL_PRESTACAO_CONTAS,
                     ELEITORAL_INELEGIBILIDADE,
                     ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO -> ComunicacaoJudicialMicrossistema.ELEITORAL_ESPECIAL;
                default -> ComunicacaoJudicialMicrossistema.ELEITORAL_COMUM;
            };
        }
        if (rito == RitoProcessual.EXECUCAO_PENAL) {
            return ComunicacaoJudicialMicrossistema.EXECUCAO_PENAL;
        }
        if (rito.isMilitar()) {
            return switch (rito) {
                case MILITAR_IPM,
                     MILITAR_PROCESSO_PENAL_MILITAR,
                     MILITAR_CONSELHO_JUSTICA,
                     MILITAR_HABEAS_CORPUS_MILITAR -> ComunicacaoJudicialMicrossistema.MILITAR_ESPECIAL;
                default -> ComunicacaoJudicialMicrossistema.MILITAR_COMUM;
            };
        }
        if (rito.isPenal()) {
            return switch (rito) {
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
                     TRIBUNAL_JURI -> ComunicacaoJudicialMicrossistema.PENAL_ESPECIAL;
                default -> ComunicacaoJudicialMicrossistema.PENAL_COMUM;
            };
        }
        if (rito.isTrabalhista()) {
            return ComunicacaoJudicialMicrossistema.TRABALHISTA;
        }
        if (rito.isPrevidenciario()) {
            return ComunicacaoJudicialMicrossistema.PREVIDENCIARIO;
        }
        if (rito.isTribFazenda()) {
            return rito.name().startsWith("TRIBUTARIO") || rito == RitoProcessual.EXECUCAO_FISCAL
                    ? ComunicacaoJudicialMicrossistema.TRIBUTARIO
                    : ComunicacaoJudicialMicrossistema.FAZENDA_PUBLICA;
        }
        if (rito.isInfancia()) {
            return ComunicacaoJudicialMicrossistema.INFANCIA_JUVENTUDE;
        }
        if (rito.isEmpresarial()) {
            return ComunicacaoJudicialMicrossistema.EMPRESARIAL_RECUPERACIONAL;
        }
        if (rito.isAmbiental()) {
            return ComunicacaoJudicialMicrossistema.AMBIENTAL;
        }
        if (rito.isAgrario()) {
            return ComunicacaoJudicialMicrossistema.AGRARIO;
        }
        if (rito.isAdministrativo()) {
            return ComunicacaoJudicialMicrossistema.ADMINISTRATIVO_IMPROBIDADE;
        }
        if (rito.isAutocompositivo()) {
            return ComunicacaoJudicialMicrossistema.AUTOCOMPOSICAO;
        }
        if (rito == RitoProcessual.JUIZADO_ESPECIAL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_CIVEL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA
                || rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL
                || rito == RitoProcessual.SUMARIO
                || rito == RitoProcessual.SUMARIO_ESPECIAL) {
            return ComunicacaoJudicialMicrossistema.JUIZADOS;
        }
        return switch (rito) {
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
                 CIVIL_NUNCIACAO_OBRA_NOVA -> ComunicacaoJudicialMicrossistema.CIVEL_ESPECIAL;
            default -> ComunicacaoJudicialMicrossistema.CIVEL_COMUM;
        };
    }
}
