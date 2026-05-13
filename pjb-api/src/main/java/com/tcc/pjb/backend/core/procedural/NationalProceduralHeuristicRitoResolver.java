package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralHeuristicRitoResolver {

    private final NationalProceduralActionProfileResolver actionProfileResolver;

    public NationalProceduralHeuristicRitoResolver(NationalProceduralActionProfileResolver actionProfileResolver) {
        this.actionProfileResolver = java.util.Objects.requireNonNull(actionProfileResolver);
    }

    RitoProcessual resolve(Map<String, Object> payload,
                           String corpus,
                           NationalProceduralPartyProfile partyProfile) {
        String explicit = firstNonBlank(text(payload.get("rito")), text(payload.get("rito_processual")));
        if (!isBlank(explicit)) {
            return RitoProcessual.tryParse(explicit).orElse(null);
        }
        if (containsAny(corpus, "MANDADO SEGURANCA")) {
            return RitoProcessual.ESPECIAL_MANDADO_SEGURANCA;
        }
        if (containsAny(corpus, "HABEAS CORPUS")) {
            return partyProfile.militar() ? RitoProcessual.MILITAR_HABEAS_CORPUS_MILITAR : RitoProcessual.ESPECIAL_HABEAS_CORPUS;
        }
        if (containsAny(corpus, "EXECUCAO FISCAL")) {
            return RitoProcessual.EXECUCAO_FISCAL;
        }
        if (containsAny(corpus, "RECURSO CONTRA EXPEDICAO DO DIPLOMA", "RECURSO CONTRA EXPEDIÇÃO DO DIPLOMA", "RCED")) {
            return RitoProcessual.ELEITORAL_RCED;
        }
        if (containsAny(corpus, "CAPTACAO ILICITA SUFRAGIO", "CAPTAÇÃO ILÍCITA DE SUFRÁGIO", "ART 41 A", "COMPRA DE VOTOS")) {
            return RitoProcessual.ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO;
        }
        if (containsAny(corpus, "ACAO DE DESCUMPRIMENTO DE OBRIGACAO", "AÇÃO DE DESCUMPRIMENTO DE OBRIGAÇÃO", "DESCUMPRIMENTO DE OBRIGACAO ESPECIFICA")) {
            return RitoProcessual.ESPECIAL_ACAO_DESCUMPRIMENTO_OBRIGACAO;
        }
        if (partyProfile.eleitoral()) {
            return RitoProcessual.ELEITORAL;
        }
        if (partyProfile.trabalho()) {
            String ritoTrabalhista = actionProfileResolver.inferTrabalhistaDefaultRito(payload, corpus, partyProfile);
            if (containsAny(corpus, "INQUERITO JUDICIAL", "FALTA GRAVE", "ART 853", "ART. 853")) {
                return RitoProcessual.TRABALHISTA_INQUERITO_FALTA_GRAVE;
            }
            if (containsAny(corpus, "ACAO DE CUMPRIMENTO", "AÇÃO DE CUMPRIMENTO", "ART 872", "ART. 872")) {
                return RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO;
            }
            if (containsAny(corpus, "SUMARIO", "ALCADA", "ALÇADA", "LEI 5.584", "LEI 5584")) {
                return RitoProcessual.TRABALHISTA_SUMARIO_ALCADA;
            }
            return switch (ritoTrabalhista) {
                case "TRABALHISTA_SUMARISSIMO" -> RitoProcessual.TRABALHISTA_SUMARISSIMO;
                case "TRABALHISTA_SUMARIO_ALCADA" -> RitoProcessual.TRABALHISTA_SUMARIO_ALCADA;
                case "TRABALHISTA_DISSIDIO_COLETIVO" -> RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO;
                case "TRABALHISTA_INQUERITO_FALTA_GRAVE" -> RitoProcessual.TRABALHISTA_INQUERITO_FALTA_GRAVE;
                case "TRABALHISTA_ACAO_CUMPRIMENTO" -> RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO;
                default -> RitoProcessual.TRABALHISTA_ORDINARIO;
            };
        }
        if (partyProfile.militar()) {
            if (containsAny(corpus, "INQUERITO POLICIAL MILITAR", "IPM", "ENCARREGADO DO IPM")) {
                return RitoProcessual.MILITAR_IPM;
            }
            if (containsAny(corpus, "CONSELHO DE JUSTICA", "CONSELHO JUSTICA", "ESCABINATO MILITAR", "CONSELHO PERMANENTE", "CONSELHO ESPECIAL")) {
                return RitoProcessual.MILITAR_CONSELHO_JUSTICA;
            }
            return RitoProcessual.MILITAR_PROCESSO_PENAL_MILITAR;
        }
        if (containsAny(corpus, "JURI", "HOMICID", "CRIME DOLOSO CONTRA VIDA")) {
            return RitoProcessual.TRIBUNAL_JURI;
        }
        if (containsAny(corpus, "INQUERITO", "DENUNCIA", "QUEIXA CRIME", "PRISAO", "CPP")) {
            return RitoProcessual.PROCEDIMENTO_PENAL_COMUM;
        }
        if (containsAny(corpus, "PROCESSO ADMINISTRATIVO DISCIPLINAR", "PAD", "SINDICANCIA", "SINDICÂNCIA", "COMISSAO PROCESSANTE", "COMISSÃO PROCESSANTE")
                && !containsAny(corpus, "MILITAR", "IPM", "CPPM")) {
            return RitoProcessual.ADMINISTRATIVO_PAD;
        }
        if (containsAny(corpus, "CONCURSO PUBLICO", "EDITAL", "NOMEACAO", "NOMEAÇÃO", "POSSE EM CARGO PUBLICO", "POSSE EM CARGO PÚBLICO")) {
            return RitoProcessual.ADMINISTRATIVO_CONCURSO_PUBLICO;
        }
        if (containsAny(corpus, "SERVIDOR PUBLICO", "SERVIDOR PÚBLICO", "REENQUADRAMENTO FUNCIONAL", "PROGRESSAO FUNCIONAL", "PROGRESSÃO FUNCIONAL")) {
            return RitoProcessual.ADMINISTRATIVO_SERVIDORES;
        }
        if (containsAny(corpus, "ATO INFRACIONAL", "MEDIDA SOCIOEDUCATIVA", "APURACAO DE ATO INFRACIONAL", "APURAÇÃO DE ATO INFRACIONAL", "SEMILIBERDADE", "INTERNACAO", "INTERNAÇÃO")) {
            return RitoProcessual.INFANCIA_JUVENTUDE_INFRACIONAL;
        }
        if (containsAny(corpus, "ADOCAO", "ADOÇÃO", "HABILITACAO A ADOCAO", "HABILITAÇÃO À ADOÇÃO", "ESTAGIO DE CONVIVENCIA", "ESTÁGIO DE CONVIVÊNCIA")) {
            return RitoProcessual.INFANCIA_JUVENTUDE_ADOCAO;
        }
        if (containsAny(corpus, "GUARDA", "TUTELA", "CURATELA") && containsAny(corpus, "MENOR", "CRIANCA", "CRIANÇA", "ADOLESCENTE", "ECA", "INFANCIA", "INFÂNCIA")) {
            return RitoProcessual.INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR;
        }
        if (containsAny(corpus, "ECA", "CONSELHO TUTELAR", "ACOLHIMENTO INSTITUCIONAL", "MEDIDA PROTETIVA")
                || (containsAny(corpus, "CRIANCA", "CRIANÇA", "ADOLESCENTE") && containsAny(corpus, "PROTECAO", "PROTEÇÃO", "MENOR", "RISCO", "ACOLHIMENTO"))) {
            return RitoProcessual.INFANCIA_JUVENTUDE_ECA;
        }
        if (containsAny(corpus, "ALIMENTOS")) {
            return RitoProcessual.CIVIL_FAMILIA_ALIMENTOS;
        }
        if (containsAny(corpus, "DIVORCIO", "UNIAO ESTAVEL")) {
            return RitoProcessual.CIVIL_FAMILIA_DIVORCIO;
        }
        if (containsAny(corpus, "INVENTARIO", "ARROLAMENTO")) {
            return RitoProcessual.CIVIL_INVENTARIO_ARROLAMENTO;
        }
        if (containsAny(corpus, "USUCAPIAO")) {
            return RitoProcessual.CIVIL_USUCAPIAO;
        }
        if (containsAny(corpus, "POSSESSORIA", "REINTEGRACAO POSSE", "INTERDITO PROIBITORIO")) {
            return containsAny(corpus, "INTERDITO PROIBITORIO") ? RitoProcessual.CIVIL_INTERDITO_PROIBITORIO : RitoProcessual.CIVIL_POSSESSORIA;
        }
        if (containsAny(corpus, "MONITORIA")) {
            return RitoProcessual.CIVIL_ACAO_MONITORIA;
        }
        if (containsAny(corpus, "CONSIGNACAO PAGAMENTO")) {
            return RitoProcessual.CIVIL_CONSIGNACAO_PAGAMENTO;
        }
        if (containsAny(corpus, "RECUPERACAO JUDICIAL")) {
            return RitoProcessual.RECUPERACAO_JUDICIAL;
        }
        if (containsAny(corpus, "FALENCIA")) {
            return RitoProcessual.FALENCIA;
        }
        if (containsAny(corpus, "BPC", "LOAS", "BENEFICIO DE PRESTACAO CONTINUADA", "BENEFÍCIO DE PRESTAÇÃO CONTINUADA")) {
            return RitoProcessual.PREVIDENCIARIO_BPC_LOAS;
        }
        if (containsAny(corpus, "AUXILIO POR INCAPACIDADE", "AUXILIO DOENCA", "AUXÍLIO-DOENÇA", "AUXILIO ACIDENTE", "INCAPACIDADE LABORATIVA")) {
            return RitoProcessual.PREVIDENCIARIO_AUXILIO_INCAPACIDADE;
        }
        if (containsAny(corpus, "APOSENTADORIA ESPECIAL", "PPP", "LTCAT", "AGENTES NOCIVOS")) {
            return RitoProcessual.PREVIDENCIARIO_ESPECIAL;
        }
        if (containsAny(corpus, "APOSENTADORIA", "TEMPO DE CONTRIBUICAO", "TEMPO DE CONTRIBUIÇÃO", "INVALIDEZ PREVIDENCIARIA")) {
            return RitoProcessual.PREVIDENCIARIO_APOSENTADORIA;
        }
        if (containsAny(corpus, "REVISAO DE BENEFICIO", "REVISIONAL PREVIDENCIARIA")) {
            return RitoProcessual.PREVIDENCIARIO_REVISAO_BENEFICIO;
        }
        if (containsAny(corpus, "RESTABELECIMENTO DE BENEFICIO", "CESSACAO INDEVIDA DE BENEFICIO", "CESSAÇÃO INDEVIDA DE BENEFÍCIO")) {
            return RitoProcessual.PREVIDENCIARIO_RESTABELECIMENTO;
        }
        if (containsAny(corpus, "SALARIO MATERNIDADE", "SALÁRIO-MATERNIDADE")) {
            return RitoProcessual.PREVIDENCIARIO_SALARIO_MATERNIDADE;
        }
        if (containsAny(corpus, "PENSAO POR MORTE", "PENSÃO POR MORTE")) {
            return RitoProcessual.PREVIDENCIARIO_PENSAO_MORTE;
        }
        if (containsAny(corpus, "SEGURADO ESPECIAL", "TRABALHADOR RURAL", "RURAL PREVIDENCIARIO")) {
            return RitoProcessual.PREVIDENCIARIO_RURAL;
        }
        if (containsAny(corpus, "RPPS", "REGIME PROPRIO DE PREVIDENCIA", "REGIME PRÓPRIO DE PREVIDÊNCIA")) {
            return RitoProcessual.PREVIDENCIARIO_RPPS;
        }
        if (containsAny(corpus, "BENEFICIO", "INSS", "PREVIDENCIARIO")) {
            return "PREVIDENCIARIO_JEF".equals(actionProfileResolver.inferPrevidenciarioDefaultRito(payload)) ? RitoProcessual.PREVIDENCIARIO_JEF : RitoProcessual.PREVIDENCIARIO_COMUM;
        }
        return RitoProcessual.COMUM_ORDINARIO;
    }

    private static String firstNonBlank(String... values) {
        return NationalProceduralRoutingSupport.firstNonBlank(values);
    }

    private static boolean containsAny(String value, String... keys) {
        return NationalProceduralRoutingSupport.containsAny(value, keys);
    }

    private static boolean isBlank(String value) {
        return NationalProceduralRoutingSupport.isBlank(value);
    }

    private static String text(Object value) {
        return NationalProceduralRoutingSupport.text(value);
    }
}
