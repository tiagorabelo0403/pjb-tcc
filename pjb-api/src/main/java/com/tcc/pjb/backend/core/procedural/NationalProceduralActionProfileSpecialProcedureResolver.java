package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralActionProfileSpecialProcedureResolver {

    private final NationalProceduralActionProfileMessages messages;

    public NationalProceduralActionProfileSpecialProcedureResolver(NationalProceduralActionProfileMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    Optional<NationalProceduralActionProfile> resolve(NationalProceduralActionProfileContext context) {
        Objects.requireNonNull(context);
        String corpus = context.corpus() == null ? "" : context.corpus();
        String explicitTipoAcao = NationalProceduralActionProfileSupport.normalize(
                NationalProceduralActionProfileSupport.firstNonBlank(
                        NationalProceduralActionProfileSupport.text(context.payload() == null ? null : context.payload().get("tipoAcao")),
                        NationalProceduralActionProfileSupport.text(context.payload() == null ? null : context.payload().get("kind"))
                )
        );
        ProceduralCanonicalResolver.CanonicalContext canonical = context.canonical();
        NationalProceduralPartyProfile partyProfile = context.partyProfile();
        LinkedHashSet<String> markers = new LinkedHashSet<>();
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        LinkedHashSet<String> legalBases = new LinkedHashSet<>();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        if (NationalProceduralActionProfileSupport.containsAny(explicitTipoAcao, "MANDADO SEGURANCA", "MS")) {
            return Optional.of(profile("MANDADO_SEGURANCA", "CONSTITUCIONAL", true, "ESPECIAL_MANDADO_SEGURANCA", "MANDADO_SEGURANCA", markers, reasons, legalBases, alerts, reviewChecklist, messages.mandadoSegurancaReason(), messages.mandadoSegurancaLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(explicitTipoAcao, "HABEAS CORPUS", "HC")) {
            return Optional.of(profile("HABEAS_CORPUS", "CONSTITUCIONAL", true, canonical != null && canonical.rito() != null && canonical.rito().isMilitar() ? "MILITAR_HABEAS_CORPUS_MILITAR" : "ESPECIAL_HABEAS_CORPUS", "HABEAS_CORPUS", markers, reasons, legalBases, alerts, reviewChecklist, messages.habeasCorpusReason(), messages.habeasCorpusLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "EXECUCAO FISCAL", "CDA", "CERTIDAO DIVIDA ATIVA")) {
            return Optional.of(profile("EXECUCAO_FISCAL", "FAZENDA_PUBLICA", true, "EXECUCAO_FISCAL", "EXECUCAO_FISCAL", markers, reasons, legalBases, alerts, reviewChecklist, messages.execucaoFiscalReason(), messages.execucaoFiscalLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "AIJE", "ABUSO PODER ECONOMICO", "ABUSO PODER POLITICO", "LC 64")) {
            return Optional.of(profile("AIJE", "ELEITORAL", true, "ELEITORAL_AIJE", "AIJE", markers, reasons, legalBases, alerts, reviewChecklist, messages.eleitoralZonaOuTreReason(), messages.aijeLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "CAPTACAO ILICITA SUFRAGIO", "CAPTAÇÃO ILÍCITA DE SUFRÁGIO", "ART 41 A", "ART. 41 A", "COMPRA DE VOTOS")) {
            reviewChecklist.add(messages.eleitoralCaptacaoSufragioChecklist());
            return Optional.of(rebuilt(profile("CAPTACAO_ILICITA_SUFRAGIO", "ELEITORAL", true, "ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO", "ELEITORAL", markers, reasons, legalBases, alerts, reviewChecklist, messages.eleitoralCaptacaoSufragioReason(), messages.eleitoralCaptacaoSufragioLegalBase()), alerts, reviewChecklist));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "AIME", "IMPUGNACAO MANDATO ELETIVO")) {
            return Optional.of(profile("AIME", "ELEITORAL", true, "ELEITORAL_AIME", "AIME", markers, reasons, legalBases, alerts, reviewChecklist, messages.eleitoralZonaOuTreReason(), messages.aimeLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "RCED", "RECURSO CONTRA EXPEDICAO DO DIPLOMA", "RECURSO CONTRA EXPEDIÇÃO DO DIPLOMA")) {
            reviewChecklist.add(messages.eleitoralRcedChecklist());
            return Optional.of(rebuilt(profile("RCED", "ELEITORAL", true, "ELEITORAL_RCED", "ELEITORAL", markers, reasons, legalBases, alerts, reviewChecklist, messages.eleitoralRcedReason(), messages.eleitoralRcedLegalBase()), alerts, reviewChecklist));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "AIRC", "REGISTRO CANDIDATURA", "INELEGIBILIDADE CANDIDATURA")) {
            return Optional.of(profile("REGISTRO_CANDIDATURA", "ELEITORAL", true, "ELEITORAL_AIRC", "AIRC", markers, reasons, legalBases, alerts, reviewChecklist, messages.eleitoralZonaOuTreReason(), messages.aijeLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "DIREITO RESPOSTA", "PROPAGANDA ELEITORAL")) {
            return Optional.of(profile("DIREITO_RESPOSTA_ELEITORAL", "ELEITORAL", true, NationalProceduralActionProfileSupport.containsAny(corpus, "DIREITO RESPOSTA") ? "ELEITORAL_DIREITO_RESPOSTA" : "ELEITORAL_PROPAGANDA", "ELEITORAL", markers, reasons, legalBases, alerts, reviewChecklist, messages.direitoRespostaEleitoralReason(), messages.direitoRespostaEleitoralLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "PRESTACAO CONTAS", "CONTA ELEITORAL", "ARRECADACAO CAMPANHA")) {
            return Optional.of(profile("PRESTACAO_CONTAS", "ELEITORAL", true, "ELEITORAL_PRESTACAO_CONTAS", "ELEITORAL", markers, reasons, legalBases, alerts, reviewChecklist, messages.prestacaoContasReason(), messages.prestacaoContasLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(explicitTipoAcao, "DESCUMPRIMENTO OBRIGACAO", "AÇÃO DESCUMPRIMENTO OBRIGAÇÃO", "ACAO DESCUMPRIMENTO OBRIGACAO")
                || NationalProceduralActionProfileSupport.containsAny(corpus, "ACAO DE DESCUMPRIMENTO DE OBRIGACAO", "AÇÃO DE DESCUMPRIMENTO DE OBRIGAÇÃO", "DESCUMPRIMENTO DE OBRIGACAO ESPECIFICA", "DESCUMPRIMENTO DE OBRIGAÇÃO ESPECÍFICA")) {
            reviewChecklist.add(messages.especialDescumprimentoObrigacaoChecklist());
            return Optional.of(rebuilt(profile("DESCUMPRIMENTO_OBRIGACAO", "ESPECIAL", true, "ESPECIAL_ACAO_DESCUMPRIMENTO_OBRIGACAO", "CIVEL", markers, reasons, legalBases, alerts, reviewChecklist, messages.especialDescumprimentoObrigacaoReason(), messages.especialDescumprimentoObrigacaoLegalBase()), alerts, reviewChecklist));
        }
        if (partyProfile != null && (partyProfile.eleitoral() || NationalProceduralActionProfileSupport.containsAny(corpus, "TSE", "TRE", "ZONA ELEITORAL", "ELEICAO", "PARTIDO POLITICO", "CANDIDATO", "CAPTACAO ILICITA SUFRAGIO"))) {
            return Optional.of(profile("ELEITORAL_GERAL", "ELEITORAL", true, NationalProceduralActionProfileSupport.firstNonBlank(canonical != null && canonical.rito() != null ? canonical.rito().name() : null, "ELEITORAL"), "ELEITORAL", markers, reasons, legalBases, alerts, reviewChecklist, messages.eleitoralGeralReason(), messages.eleitoralGeralLegalBase()));
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
