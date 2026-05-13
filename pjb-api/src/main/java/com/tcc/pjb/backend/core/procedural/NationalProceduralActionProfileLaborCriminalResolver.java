package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralActionProfileLaborCriminalResolver {

    private final NationalProceduralActionProfileEconomicRitoResolver economicRitoResolver;
    private final NationalProceduralActionProfileMessages messages;

    public NationalProceduralActionProfileLaborCriminalResolver(NationalProceduralActionProfileEconomicRitoResolver economicRitoResolver,
                                                                NationalProceduralActionProfileMessages messages) {
        this.economicRitoResolver = Objects.requireNonNull(economicRitoResolver);
        this.messages = Objects.requireNonNull(messages);
    }

    Optional<NationalProceduralActionProfile> resolve(NationalProceduralActionProfileContext context) {
        Objects.requireNonNull(context);
        Map<String, Object> payload = context.payload() == null ? Map.of() : context.payload();
        String corpus = context.corpus() == null ? "" : context.corpus();
        NationalProceduralPartyProfile partyProfile = context.partyProfile();
        LinkedHashSet<String> markers = new LinkedHashSet<>();
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        LinkedHashSet<String> legalBases = new LinkedHashSet<>();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        if (partyProfile != null && (partyProfile.trabalho() || NationalProceduralActionProfileSupport.containsAny(corpus, "CLT", "RECLAMACAO TRABALHISTA", "VERBAS RESCISORIAS", "HORAS EXTRAS", "FGTS", "ADICIONAL INSALUBRIDADE", "VINCULO EMPREGATICIO", "DISSIDIO COLETIVO", "INQUERITO JUDICIAL", "FALTA GRAVE", "ACAO DE CUMPRIMENTO"))) {
            String defaultRito = economicRitoResolver.inferTrabalhistaDefaultRito(payload, corpus, partyProfile);
            if (defaultRito.equals("TRABALHISTA_INQUERITO_FALTA_GRAVE")) {
                alerts.add(messages.trabalhistaInqueritoAlert());
                reviewChecklist.add(messages.trabalhistaInqueritoChecklist());
                return Optional.of(rebuilt(profile("INQUERITO_FALTA_GRAVE_TRABALHISTA", "TRABALHISTA", true, defaultRito, "TRABALHO", markers, reasons, legalBases, alerts, reviewChecklist, messages.trabalhistaInqueritoReason(), messages.trabalhistaInqueritoLegalBase()), alerts, reviewChecklist));
            }
            if (defaultRito.equals("TRABALHISTA_ACAO_CUMPRIMENTO")) {
                reviewChecklist.add(messages.trabalhistaAcaoCumprimentoChecklist());
                return Optional.of(rebuilt(profile("ACAO_CUMPRIMENTO_TRABALHISTA", "TRABALHISTA", true, defaultRito, "TRABALHO", markers, reasons, legalBases, alerts, reviewChecklist, messages.trabalhistaAcaoCumprimentoReason(), messages.trabalhistaAcaoCumprimentoLegalBase()), alerts, reviewChecklist));
            }
            if (defaultRito.equals("TRABALHISTA_DISSIDIO_COLETIVO")) {
                return Optional.of(profile("DISSIDIO_COLETIVO_TRABALHISTA", "TRABALHISTA", true, defaultRito, "TRABALHO", markers, reasons, legalBases, alerts, reviewChecklist, messages.varaTrabalhoReason(), messages.trabalhistaLegalBase()));
            }
            String reason = defaultRito.equals("TRABALHISTA_SUMARIO_ALCADA") ? messages.trabalhistaAlcadaReason() : defaultRito.equals("TRABALHISTA_SUMARISSIMO") ? messages.varaTrabalhoSumarissimoReason() : messages.varaTrabalhoReason();
            String legalBase = defaultRito.equals("TRABALHISTA_SUMARIO_ALCADA") ? messages.trabalhistaAlcadaLegalBase() : messages.trabalhistaLegalBase();
            NationalProceduralActionProfile profile = profile("RECLAMACAO_TRABALHISTA", "TRABALHISTA", false, defaultRito, "TRABALHO", markers, reasons, legalBases, alerts, reviewChecklist, reason, legalBase);
            if (defaultRito.equals("TRABALHISTA_SUMARISSIMO")) {
                alerts.add(messages.trabalhistaSumarissimoAlert());
                reviewChecklist.add(messages.trabalhistaSumarissimoChecklist());
            }
            if (defaultRito.equals("TRABALHISTA_SUMARIO_ALCADA")) {
                alerts.add(messages.trabalhistaAlcadaAlert());
                reviewChecklist.add(messages.trabalhistaAlcadaChecklist());
            }
            if (NationalProceduralActionProfileSupport.containsAny(corpus, "ADMINISTRACAO PUBLICA", "ADMINISTRAÇÃO PÚBLICA", "AUTARQUIA", "FUNDACAO PUBLICA", "FUNDAÇÃO PÚBLICA", "MUNICIPIO", "PREFEITURA", "ESTADO", "UNIAO", "UNIÃO")) {
                alerts.add(messages.trabalhistaPublicEntitySumarissimoAlert());
            }
            return Optional.of(rebuilt(profile, alerts, reviewChecklist));
        }
        if (partyProfile != null && (partyProfile.militar() || NationalProceduralActionProfileSupport.containsAny(corpus, "IPM", "CPPM", "CONSELHO JUSTICA", "CRIME MILITAR", "AUDITORIA MILITAR"))) {
            if (NationalProceduralActionProfileSupport.containsAny(corpus, "INQUERITO POLICIAL MILITAR", "IPM", "ENCARREGADO DO IPM")) {
                reviewChecklist.add(messages.militarIpmChecklist());
                return Optional.of(rebuilt(profile("INQUERITO_POLICIAL_MILITAR", "MILITAR", true, "MILITAR_IPM", "MILITAR", markers, reasons, legalBases, alerts, reviewChecklist, messages.militarIpmReason(), messages.militarIpmLegalBase()), alerts, reviewChecklist));
            }
            if (NationalProceduralActionProfileSupport.containsAny(corpus, "CONSELHO DE JUSTICA", "CONSELHO JUSTICA", "ESCABINATO MILITAR", "CONSELHO PERMANENTE", "CONSELHO ESPECIAL")) {
                reviewChecklist.add(messages.militarConselhoJusticaChecklist());
                return Optional.of(rebuilt(profile("CONSELHO_DE_JUSTICA_MILITAR", "MILITAR", true, "MILITAR_CONSELHO_JUSTICA", "MILITAR", markers, reasons, legalBases, alerts, reviewChecklist, messages.militarConselhoJusticaReason(), messages.militarConselhoJusticaLegalBase()), alerts, reviewChecklist));
            }
            return Optional.of(profile("PROCESSO_PENAL_MILITAR", "MILITAR", true, "MILITAR_PROCESSO_PENAL_MILITAR", "MILITAR", markers, reasons, legalBases, alerts, reviewChecklist, messages.militarReason(), messages.militarLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "JURI", "HOMICID", "TENTATIVA HOMICID", "CRIME DOLOSO CONTRA VIDA")) {
            return Optional.of(profile("TRIBUNAL_DO_JURI", "PENAL", true, "TRIBUNAL_JURI", "TRIBUNAL_JURI", markers, reasons, legalBases, alerts, reviewChecklist, messages.tribunalJuriReason(), messages.tribunalJuriLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "EXECUCAO PENAL", "PENA", "PROGRESSAO REGIME", "REMICAO", "LEP")) {
            return Optional.of(profile("EXECUCAO_PENAL", "PENAL", true, "EXECUCAO_PENAL", "EXECUCAO_PENAL", markers, reasons, legalBases, alerts, reviewChecklist, messages.execucaoPenalReason(), messages.execucaoPenalLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "AMEACA", "INJURIA", "PERTURBACAO", "DESACATO LEVE", "LESAO CORPORAL LEVE")
                && !NationalProceduralActionProfileSupport.containsAny(corpus, "TRIBUNAL JURI", "JURI")) {
            return Optional.of(profile("INFRACAO_MENOR_POTENCIAL", "PENAL", false, "JUIZADO_ESPECIAL_CRIMINAL", "JECRIM", markers, reasons, legalBases, alerts, reviewChecklist, messages.jecrimReason(), messages.jecrimLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "DENUNCIA", "QUEIXA CRIME", "FLAGRANTE", "INQUERITO", "CRIME", "CPP", "PRISAO", "AUDIENCIA CUSTODIA")) {
            return Optional.of(profile("ACAO_PENAL", "PENAL", false, "PROCEDIMENTO_PENAL_COMUM", "PENAL", markers, reasons, legalBases, alerts, reviewChecklist, messages.acaoPenalReason(), messages.acaoPenalLegalBase()));
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

    private static NationalProceduralActionProfile rebuilt(NationalProceduralActionProfile profile,
                                                           LinkedHashSet<String> alerts,
                                                           LinkedHashSet<String> reviewChecklist) {
        return NationalProceduralActionProfileSupport.rebuilt(profile, alerts, reviewChecklist);
    }
}
