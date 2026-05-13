package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralActionProfilePublicEntityResolver {

    private final NationalProceduralActionProfileEconomicRitoResolver economicRitoResolver;
    private final NationalProceduralActionProfileMessages messages;

    public NationalProceduralActionProfilePublicEntityResolver(NationalProceduralActionProfileEconomicRitoResolver economicRitoResolver,
                                                               NationalProceduralActionProfileMessages messages) {
        this.economicRitoResolver = Objects.requireNonNull(economicRitoResolver);
        this.messages = Objects.requireNonNull(messages);
    }

    Optional<NationalProceduralActionProfile> resolve(NationalProceduralActionProfileContext context) {
        Objects.requireNonNull(context);
        Map<String, Object> payload = context.payload() == null ? Map.of() : context.payload();
        String corpus = context.corpus() == null ? "" : context.corpus();
        LinkedHashSet<String> markers = new LinkedHashSet<>();
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        LinkedHashSet<String> legalBases = new LinkedHashSet<>();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        if (NationalProceduralActionProfileSupport.containsAny(corpus, "IMPROBIDADE")) {
            NationalProceduralActionProfile profile = profile("IMPROBIDADE_ADMINISTRATIVA", "ADMINISTRATIVO", true, "IMPROBIDADE_ADMINISTRATIVA", "ADMINISTRATIVO", markers, reasons, legalBases, alerts, reviewChecklist, messages.improbidadeReason(), messages.improbidadeLegalBase());
            alerts.add(messages.improbidadeJuizadoAlert());
            return Optional.of(rebuilt(profile, alerts, reviewChecklist));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "ACAO CIVIL PUBLICA", "ACP", "DANO COLETIVO", "INTERESSE DIFUSO", "INTERESSE COLETIVO")) {
            return Optional.of(profile("ACAO_CIVIL_PUBLICA", "COLETIVO", true, NationalProceduralActionProfileSupport.containsAny(corpus, "AMBIENTAL") ? "AMBIENTAL_ACP" : "CIVIL_ACAO_CIVIL_PUBLICA", "COLETIVO", markers, reasons, legalBases, alerts, reviewChecklist, messages.acaoCivilPublicaReason(), messages.acaoCivilPublicaLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "DESAPROPRIACAO")) {
            NationalProceduralActionProfile profile = profile("DESAPROPRIACAO", "AGRARIO_ADMINISTRATIVO", true, "AGRARIO_DESAPROPRIACAO", "AGRARIO", markers, reasons, legalBases, alerts, reviewChecklist, messages.desapropriacaoReason(), messages.desapropriacaoLegalBase());
            alerts.add(messages.desapropriacaoJuizadoAlert());
            return Optional.of(rebuilt(profile, alerts, reviewChecklist));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "BENEFICIO", "INSS", "APOSENTADORIA", "BPC", "LOAS", "AUXILIO DOENCA", "SALARIO MATERNIDADE", "PENSAO MORTE", "PREVIDENCIARIO", "RPPS", "REGIME PROPRIO DE PREVIDENCIA")) {
            String rito = inferPrevidenciarioRito(payload, corpus);
            String actionNature = switch (rito) {
                case "PREVIDENCIARIO_BPC_LOAS" -> "BPC_LOAS";
                case "PREVIDENCIARIO_AUXILIO_INCAPACIDADE" -> "AUXILIO_INCAPACIDADE";
                case "PREVIDENCIARIO_APOSENTADORIA" -> "APOSENTADORIA";
                case "PREVIDENCIARIO_REVISAO_BENEFICIO" -> "REVISAO_BENEFICIO";
                case "PREVIDENCIARIO_RESTABELECIMENTO" -> "RESTABELECIMENTO_BENEFICIO";
                case "PREVIDENCIARIO_SALARIO_MATERNIDADE" -> "SALARIO_MATERNIDADE";
                case "PREVIDENCIARIO_PENSAO_MORTE" -> "PENSAO_MORTE";
                case "PREVIDENCIARIO_RURAL" -> "PREVIDENCIARIO_RURAL";
                case "PREVIDENCIARIO_ESPECIAL" -> "APOSENTADORIA_ESPECIAL";
                case "PREVIDENCIARIO_RPPS" -> "PREVIDENCIARIO_RPPS";
                default -> "PREVIDENCIARIO";
            };
            String reason = switch (rito) {
                case "PREVIDENCIARIO_BPC_LOAS" -> messages.previdenciarioBpcReason();
                case "PREVIDENCIARIO_AUXILIO_INCAPACIDADE" -> messages.previdenciarioAuxilioReason();
                case "PREVIDENCIARIO_APOSENTADORIA" -> messages.previdenciarioAposentadoriaReason();
                case "PREVIDENCIARIO_REVISAO_BENEFICIO" -> messages.previdenciarioRevisaoReason();
                case "PREVIDENCIARIO_RESTABELECIMENTO" -> messages.previdenciarioRestabelecimentoReason();
                case "PREVIDENCIARIO_SALARIO_MATERNIDADE" -> messages.previdenciarioSalarioMaternidadeReason();
                case "PREVIDENCIARIO_PENSAO_MORTE" -> messages.previdenciarioPensaoReason();
                case "PREVIDENCIARIO_RURAL" -> messages.previdenciarioRuralReason();
                case "PREVIDENCIARIO_ESPECIAL" -> messages.previdenciarioEspecialReason();
                case "PREVIDENCIARIO_RPPS" -> messages.previdenciarioRppsReason();
                case "PREVIDENCIARIO_JEF" -> messages.previdenciarioJefReason();
                default -> messages.previdenciarioComumReason();
            };
            NationalProceduralActionProfile profile = profile(actionNature, "PREVIDENCIARIO", false, rito, "PREVIDENCIARIO", markers, reasons, legalBases, alerts, reviewChecklist, reason, messages.previdenciarioLegalBase());
            appendPrevidenciarioChecklist(rito, reviewChecklist);
            return Optional.of(rebuilt(profile, alerts, reviewChecklist));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus,
                "PROCESSO ADMINISTRATIVO DISCIPLINAR",
                "PAD",
                "SINDICANCIA",
                "SINDICÂNCIA",
                "COMISSAO PROCESSANTE",
                "COMISSÃO PROCESSANTE",
                "PENALIDADE DISCIPLINAR",
                "DEMISSAO DE SERVIDOR",
                "DEMISSÃO DE SERVIDOR",
                "ADVERTENCIA DISCIPLINAR",
                "ADVERTÊNCIA DISCIPLINAR",
                "SUSPENSAO DISCIPLINAR",
                "SUSPENSÃO DISCIPLINAR")
                && !NationalProceduralActionProfileSupport.containsAny(corpus, "MILITAR", "IPM", "CPPM", "CONSELHO JUSTICA", "CONSELHO DE JUSTICA")) {
            alerts.add(messages.administrativoPadAlert());
            reviewChecklist.add(messages.administrativoPadChecklist());
            return Optional.of(rebuilt(profile("PROCESSO_ADMINISTRATIVO_DISCIPLINAR", "ADMINISTRATIVO", true, "ADMINISTRATIVO_PAD", "ADMINISTRATIVO", markers, reasons, legalBases, alerts, reviewChecklist, messages.administrativoPadReason(), messages.administrativoPadLegalBase()), alerts, reviewChecklist));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "MUNICIPIO", "PREFEITURA", "ESTADO", "SECRETARIA", "SERVIDOR PUBLICO", "CONCURSO PUBLICO", "FORNECIMENTO MEDICAMENTO", "LEITO HOSPITALAR")) {
            boolean concurso = NationalProceduralActionProfileSupport.containsAny(corpus, "CONCURSO PUBLICO", "EDITAL", "NOMEACAO", "NOMEAÇÃO", "POSSE EM CARGO PUBLICO", "POSSE EM CARGO PÚBLICO");
            boolean servidor = NationalProceduralActionProfileSupport.containsAny(corpus, "SERVIDOR PUBLICO", "SERVIDOR PÚBLICO", "REENQUADRAMENTO FUNCIONAL", "PROGRESSAO FUNCIONAL", "PROGRESSÃO FUNCIONAL", "REMUNERACAO DE SERVIDOR", "REMUNERAÇÃO DE SERVIDOR");
            boolean saude = NationalProceduralActionProfileSupport.containsAny(corpus, "FORNECIMENTO MEDICAMENTO", "LEITO HOSPITALAR", "CIRURGIA", "TRATAMENTO", "UTI", "HOME CARE");
            String nature = concurso
                    ? "CONCURSO_PUBLICO_ADMINISTRATIVO"
                    : servidor
                    ? "ADMINISTRATIVO_SERVIDOR"
                    : saude
                    ? "SAUDE_PUBLICA"
                    : "FAZENDA_PUBLICA";
            String defaultRito = concurso
                    ? "ADMINISTRATIVO_CONCURSO_PUBLICO"
                    : servidor
                    ? "ADMINISTRATIVO_SERVIDORES"
                    : "FAZENDA_PUBLICA_CONHECIMENTO";
            NationalProceduralActionProfile profile = profile(nature, concurso || servidor ? "ADMINISTRATIVO" : "FAZENDA_PUBLICA", false, defaultRito, concurso || servidor ? "ADMINISTRATIVO" : "FAZENDA", markers, reasons, legalBases, alerts, reviewChecklist, messages.fazendaReason(), messages.fazendaLegalBase());
            if (concurso || servidor) {
                reviewChecklist.add(messages.fazendaServidorChecklist());
            }
            return Optional.of(rebuilt(profile, alerts, reviewChecklist));
        }
        return Optional.empty();
    }

    private static NationalProceduralActionProfile profile(String actionNature,
                                                           String actionFamily,
                                                           boolean specialProcedure,
                                                           String defaultRito,
                                                           String varaFamily,
                                                           LinkedHashSet<String> markers,
                                                           LinkedHashSet<String> reasons,
                                                           LinkedHashSet<String> legalBases,
                                                           LinkedHashSet<String> alerts,
                                                           LinkedHashSet<String> reviewChecklist,
                                                           String reason,
                                                           String legalBase) {
        return NationalProceduralActionProfileSupport.profile(actionNature, actionFamily, specialProcedure, defaultRito, varaFamily, markers, reasons, legalBases, alerts, reviewChecklist, reason, legalBase);
    }


    private String inferPrevidenciarioRito(Map<String, Object> payload,
                                           String corpus) {
        String normalizedCorpus = NationalProceduralActionProfileSupport.normalize(corpus);
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "BPC", "LOAS", "BENEFICIO PRESTACAO CONTINUADA", "BENEFÍCIO PRESTAÇÃO CONTINUADA")) {
            return "PREVIDENCIARIO_BPC_LOAS";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "AUXILIO POR INCAPACIDADE", "AUXILIO DOENCA", "AUXÍLIO-DOENÇA", "AUXILIO ACIDENTE", "INCAPACIDADE LABORATIVA")) {
            return "PREVIDENCIARIO_AUXILIO_INCAPACIDADE";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "APOSENTADORIA ESPECIAL", "PPP", "LTCAT", "AGENTES NOCIVOS")) {
            return "PREVIDENCIARIO_ESPECIAL";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "APOSENTADORIA", "TEMPO DE CONTRIBUICAO", "TEMPO DE CONTRIBUIÇÃO", "INVALIDEZ PREVIDENCIARIA")) {
            return "PREVIDENCIARIO_APOSENTADORIA";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "REVISAO DE BENEFICIO", "REVISIONAL PREVIDENCIARIA")) {
            return "PREVIDENCIARIO_REVISAO_BENEFICIO";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "RESTABELECIMENTO DE BENEFICIO", "CESSACAO INDEVIDA DE BENEFICIO", "CESSAÇÃO INDEVIDA DE BENEFÍCIO")) {
            return "PREVIDENCIARIO_RESTABELECIMENTO";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "SALARIO MATERNIDADE", "SALÁRIO-MATERNIDADE")) {
            return "PREVIDENCIARIO_SALARIO_MATERNIDADE";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "PENSAO POR MORTE", "PENSÃO POR MORTE")) {
            return "PREVIDENCIARIO_PENSAO_MORTE";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "SEGURADO ESPECIAL", "TRABALHADOR RURAL", "RURAL PREVIDENCIARIO")) {
            return "PREVIDENCIARIO_RURAL";
        }
        if (NationalProceduralActionProfileSupport.containsAny(normalizedCorpus, "RPPS", "REGIME PROPRIO DE PREVIDENCIA", "REGIME PRÓPRIO DE PREVIDÊNCIA")) {
            return "PREVIDENCIARIO_RPPS";
        }
        return economicRitoResolver.inferPrevidenciarioDefaultRito(payload);
    }

    private void appendPrevidenciarioChecklist(String rito,
                                               LinkedHashSet<String> reviewChecklist) {
        switch (rito) {
            case "PREVIDENCIARIO_BPC_LOAS" -> reviewChecklist.add(messages.previdenciarioBpcChecklist());
            case "PREVIDENCIARIO_AUXILIO_INCAPACIDADE" -> reviewChecklist.add(messages.previdenciarioAuxilioChecklist());
            case "PREVIDENCIARIO_APOSENTADORIA" -> reviewChecklist.add(messages.previdenciarioAposentadoriaChecklist());
            case "PREVIDENCIARIO_REVISAO_BENEFICIO" -> reviewChecklist.add(messages.previdenciarioRevisaoChecklist());
            case "PREVIDENCIARIO_RESTABELECIMENTO" -> reviewChecklist.add(messages.previdenciarioRestabelecimentoChecklist());
            case "PREVIDENCIARIO_SALARIO_MATERNIDADE" -> reviewChecklist.add(messages.previdenciarioSalarioMaternidadeChecklist());
            case "PREVIDENCIARIO_PENSAO_MORTE" -> reviewChecklist.add(messages.previdenciarioPensaoChecklist());
            case "PREVIDENCIARIO_RURAL" -> reviewChecklist.add(messages.previdenciarioRuralChecklist());
            case "PREVIDENCIARIO_ESPECIAL" -> reviewChecklist.add(messages.previdenciarioEspecialChecklist());
            case "PREVIDENCIARIO_RPPS" -> reviewChecklist.add(messages.previdenciarioRppsChecklist());
            default -> { }
        }
    }

    private static NationalProceduralActionProfile rebuilt(NationalProceduralActionProfile profile,
                                                           LinkedHashSet<String> alerts,
                                                           LinkedHashSet<String> reviewChecklist) {
        return NationalProceduralActionProfileSupport.rebuilt(profile, alerts, reviewChecklist);
    }
}
