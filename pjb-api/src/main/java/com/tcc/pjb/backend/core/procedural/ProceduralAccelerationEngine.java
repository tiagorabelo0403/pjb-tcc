package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ProceduralAccelerationEngine {

    private ProceduralAccelerationEngine() {
    }

    public static ProceduralAccelerationReport analyze(Map<String, Object> payload,
                                                       String actionNature,
                                                       String actionFamily,
                                                       TipoJustica tipoJustica,
                                                       String ritoSugerido,
                                                       String riskLevel,
                                                       ProceduralIntelligenceAdvisoryReport advisory,
                                                       ProceduralDecisionQualityReport quality,
                                                       ProceduralAutomationPolicyReport automationPolicy,
                                                       ProceduralExecutiveExplainabilityReport executiveExplainability) {
        LinkedHashMap<String, Object> source = new LinkedHashMap<>();
        if (payload != null) {
            source.putAll(payload);
        }
        source.put("__actionNature", actionNature);
        source.put("__actionFamily", actionFamily);
        source.put("__tipoJustica", tipoJustica != null ? tipoJustica.name() : null);
        source.put("__ritoSugerido", ritoSugerido);
        source.put("__riskLevel", riskLevel);
        if (advisory != null) {
            source.put("__advisoryRamo", advisory.suggestedRamo() != null ? advisory.suggestedRamo().name() : null);
            source.put("__advisoryRito", advisory.suggestedRito() != null ? advisory.suggestedRito().name() : null);
            source.put("__advisoryMateria", advisory.suggestedMateria() != null ? advisory.suggestedMateria().name() : null);
            source.put("__advisorySigilo", advisory.suggestedSigilo() != null ? advisory.suggestedSigilo().name() : null);
            source.put("__advisoryNatureza", advisory.naturezaPrincipal() != null ? advisory.naturezaPrincipal().name() : null);
        }
        String corpus = buildCorpus(source);
        AccelerationContext context = resolveContext(source, corpus, tipoJustica, ritoSugerido, riskLevel, advisory, quality, automationPolicy, executiveExplainability);
        List<ProceduralAccelerationDirectiveItem> directives = buildDirectives(context, advisory, quality, automationPolicy);
        List<String> evidenceChecklist = buildEvidenceChecklist(context, corpus, advisory);
        List<String> operationalChecklist = buildOperationalChecklist(context, corpus, automationPolicy);
        List<String> legalBases = buildLegalBases(context, corpus);
        List<String> alerts = buildAlerts(context, corpus, advisory, automationPolicy);
        LinkedHashMap<String, Object> metadata = buildMetadata(context, corpus, directives, evidenceChecklist, legalBases, advisory, quality, automationPolicy, executiveExplainability);
        return new ProceduralAccelerationReport(
                Instant.now(),
                context.track(),
                context.lane(),
                context.profile(),
                context.firstReviewTargetMinutes(),
                context.magistrateEscalationTargetMinutes(),
                context.technicalSupportTargetMinutes(),
                context.queueBypassEligible(),
                true,
                true,
                context.natJusPriorityRecommended(),
                context.protectiveUrgencyRecommended(),
                context.victimIdentityShieldRecommended(),
                context.multiChannelEscalation(),
                context.legalClockMonitoring(),
                context.recommendedDecisionBlueprint(),
                context.executivePrioritySummary(),
                List.copyOf(directives),
                List.copyOf(evidenceChecklist),
                List.copyOf(operationalChecklist),
                List.copyOf(legalBases),
                List.copyOf(alerts),
                Collections.unmodifiableMap(metadata)
        );
    }

    private static AccelerationContext resolveContext(Map<String, Object> source,
                                                      String corpus,
                                                      TipoJustica tipoJustica,
                                                      String ritoSugerido,
                                                      String riskLevel,
                                                      ProceduralIntelligenceAdvisoryReport advisory,
                                                      ProceduralDecisionQualityReport quality,
                                                      ProceduralAutomationPolicyReport automationPolicy,
                                                      ProceduralExecutiveExplainabilityReport executiveExplainability) {
        RamoDireito ramo = advisory != null && advisory.suggestedRamo() != null
                ? advisory.suggestedRamo()
                : RamoDireito.fromString(firstNonBlank(text(source.get("ramoDireito")), text(source.get("materia"))));
        RitoProcessual rito = advisory != null && advisory.suggestedRito() != null
                ? advisory.suggestedRito()
                : RitoProcessual.fromString(ritoSugerido);
        MateriaJurisdicao materia = advisory != null && advisory.suggestedMateria() != null
                ? advisory.suggestedMateria()
                : MateriaJurisdicao.fromString(text(source.get("materia")));
        NivelSigilo sigilo = advisory != null ? advisory.suggestedSigilo() : NivelSigilo.fromString(text(source.get("sigilo")));
        NaturezaJuridicaCanonical natureza = advisory != null ? advisory.naturezaPrincipal() : null;
        String normalizedRisk = normalize(riskLevel);
        boolean mariaDaPenha = isMariaDaPenha(corpus, ramo, rito, materia);
        boolean domesticCritical = mariaDaPenha && containsAny(corpus, "RISCO ATUAL", "RISCO IMINENTE", "AMEACA", "AGRESSOR ARMADO", "DESCUMPRIMENTO DE MEDIDA", "PERSEGUICAO", "STALKING", "TENTATIVA DE FEMINICIDIO");
        boolean health = isHealthUrgency(corpus, ramo, materia);
        boolean icu = health && containsAny(corpus, "UTI", "LEITO DE UTI", "TERAPIA INTENSIVA", "LEITO INTENSIVO", "UTI NEONATAL", "UTI PEDIATRICA", "LEITO CRITICO");
        boolean lifeSupport = health && containsAny(corpus, "VENTILACAO MECANICA", "INTUBACAO", "HEMODIALISE DE URGENCIA", "ECMO", "SUPORTE VITAL", "RISCO DE MORTE", "RISCO IMEDIATO DE VIDA", "SEPSIS", "CHOQUE");
        boolean infancyUrgent = containsAny(corpus, "CRIANC", "ADOLESCENT", "ECA", "ACOLHIMENTO INSTITUCIONAL", "DEPOSITO PROTETIVO", "MEDIDA PROTETIVA DA CRIANCA")
                && containsAny(corpus, "RISCO", "VIOLENCIA", "ABUSO", "AMEACA", "TUTELA DE URGENCIA");
        boolean idosoHighRisk = containsAny(corpus, "IDOSO", "PESSOA IDOSA")
                && containsAny(corpus, "RISCO", "VIOLENCIA", "ABANDONO", "LEITO", "UTI", "SUPORTE VITAL");
        boolean vulnerablePriority = containsAny(corpus, "DEFICIENCIA", "GESTANTE DE ALTO RISCO", "PUERPERA", "RECEM-NASCIDO", "PACIENTE ONCOLOGICO", "DOENCA RARA", "PACIENTE IMUNOSSUPRIMIDO");

        ProceduralAccelerationTrack track = ProceduralAccelerationTrack.NENHUMA;
        ProceduralAccelerationLane lane = ProceduralAccelerationLane.STANDARD;
        String profile = "STANDARD";
        int firstReviewTargetMinutes = 240;
        int magistrateEscalationTargetMinutes = 480;
        int technicalSupportTargetMinutes = 360;
        boolean queueBypassEligible = false;
        boolean natJusPriorityRecommended = false;
        boolean protectiveUrgencyRecommended = false;
        boolean victimIdentityShieldRecommended = false;
        boolean multiChannelEscalation = false;
        boolean legalClockMonitoring = false;
        String recommendedDecisionBlueprint = null;
        String executivePrioritySummary = "Fluxo ordinário sem aceleração extraordinária.";

        if (mariaDaPenha) {
            track = domesticCritical ? ProceduralAccelerationTrack.MARIA_DA_PENHA_PROTECAO_URGENTE : ProceduralAccelerationTrack.MARIA_DA_PENHA_REDE_PROTECAO;
            lane = domesticCritical ? ProceduralAccelerationLane.CRITICAL_RED : ProceduralAccelerationLane.PROTECTIVE_PURPLE;
            profile = domesticCritical ? "PROTECAO_IMEDIATA_VIOLENCIA_DOMESTICA" : "PROTECAO_REFORCADA_VIOLENCIA_DOMESTICA";
            firstReviewTargetMinutes = domesticCritical ? 10 : 25;
            magistrateEscalationTargetMinutes = domesticCritical ? 30 : 60;
            technicalSupportTargetMinutes = domesticCritical ? 120 : 180;
            queueBypassEligible = true;
            protectiveUrgencyRecommended = true;
            victimIdentityShieldRecommended = true;
            multiChannelEscalation = true;
            legalClockMonitoring = true;
            recommendedDecisionBlueprint = domesticCritical ? "MEDIDA_PROTETIVA_URGENTE_MARIA_DA_PENHA" : "MEDIDA_PROTETIVA_MARIA_DA_PENHA";
            executivePrioritySummary = domesticCritical
                    ? "Violência doméstica com risco crítico: distribuição imediata, proteção reforçada e conclusão urgente ao magistrado."
                    : "Violência doméstica: fila protetiva reforçada, sigilo operacional e gestão de prazo legal prioritária.";
        }

        if (lifeSupport || icu) {
            track = icu ? ProceduralAccelerationTrack.SAUDE_LEITO_UTI : ProceduralAccelerationTrack.SAUDE_SUPORTE_VITAL;
            lane = ProceduralAccelerationLane.CLINICAL_AMBER;
            profile = icu ? "SAUDE_UTI_CRITICA" : "SAUDE_SUPORTE_VITAL_URGENTE";
            firstReviewTargetMinutes = Math.min(firstReviewTargetMinutes, icu ? 8 : 12);
            magistrateEscalationTargetMinutes = Math.min(magistrateEscalationTargetMinutes, icu ? 20 : 30);
            technicalSupportTargetMinutes = Math.min(technicalSupportTargetMinutes, 45);
            queueBypassEligible = true;
            natJusPriorityRecommended = true;
            multiChannelEscalation = true;
            legalClockMonitoring = true;
            recommendedDecisionBlueprint = icu ? "TUTELA_URGENTE_LEITO_UTI" : "TUTELA_URGENTE_SUPORTE_VITAL";
            executivePrioritySummary = icu
                    ? "Saúde crítica com pedido de leito intensivo: fila clínica acelerada, suporte técnico prioritário e conclusão urgente."
                    : "Saúde crítica com suporte vital: o caso exige resposta jurisdicional assistida e triagem clínica em janela reduzida.";
        } else if (health && containsAny(corpus, "MEDICAMENTO", "CIRURGIA", "TRATAMENTO", "TRANSFERENCIA HOSPITALAR", "ONCOLOG")) {
            track = ProceduralAccelerationTrack.SAUDE_FORNECIMENTO_ESSENCIAL;
            lane = ProceduralAccelerationLane.PRIORITY_BLUE;
            profile = "SAUDE_URGENTE_EVIDENCIARIA";
            firstReviewTargetMinutes = Math.min(firstReviewTargetMinutes, 30);
            magistrateEscalationTargetMinutes = Math.min(magistrateEscalationTargetMinutes, 90);
            technicalSupportTargetMinutes = Math.min(technicalSupportTargetMinutes, 90);
            queueBypassEligible = true;
            natJusPriorityRecommended = true;
            legalClockMonitoring = true;
            recommendedDecisionBlueprint = "TUTELA_URGENTE_SAUDE";
            executivePrioritySummary = "Saúde urgente: a fila deve priorizar prova clínica mínima, apoio técnico e apreciação célere.";
        }

        if (infancyUrgent && lane == ProceduralAccelerationLane.STANDARD) {
            track = ProceduralAccelerationTrack.INFANCIA_PROTECAO_URGENTE;
            lane = ProceduralAccelerationLane.PRIORITY_BLUE;
            profile = "PROTECAO_URGENTE_INFANCIA";
            firstReviewTargetMinutes = 20;
            magistrateEscalationTargetMinutes = 60;
            technicalSupportTargetMinutes = 120;
            queueBypassEligible = true;
            multiChannelEscalation = true;
            legalClockMonitoring = true;
            recommendedDecisionBlueprint = "MEDIDA_PROTETIVA_INFANCIA";
            executivePrioritySummary = "Infância em risco: a tramitação deve operar em faixa prioritária com proteção reforçada.";
        }

        if (idosoHighRisk && lane == ProceduralAccelerationLane.STANDARD) {
            track = ProceduralAccelerationTrack.IDOSO_RISCO_ELEVADO;
            lane = ProceduralAccelerationLane.PRIORITY_BLUE;
            profile = "PROTECAO_IDOSO_RISCO";
            firstReviewTargetMinutes = 25;
            magistrateEscalationTargetMinutes = 70;
            technicalSupportTargetMinutes = 120;
            queueBypassEligible = true;
            legalClockMonitoring = true;
            recommendedDecisionBlueprint = "MEDIDA_URGENTE_IDOSO";
            executivePrioritySummary = "Pessoa idosa em risco elevado: o caso exige prioridade reforçada e apreciação célere.";
        }

        if (vulnerablePriority && lane == ProceduralAccelerationLane.STANDARD) {
            track = ProceduralAccelerationTrack.PRIORIDADE_VULNERABILIDADE;
            lane = ProceduralAccelerationLane.FAST_GREEN;
            profile = "PRIORIDADE_VULNERABILIDADE";
            firstReviewTargetMinutes = 45;
            magistrateEscalationTargetMinutes = 120;
            technicalSupportTargetMinutes = 180;
            queueBypassEligible = true;
            legalClockMonitoring = true;
            recommendedDecisionBlueprint = "TRIAGEM_PRIORITARIA_VULNERABILIDADE";
            executivePrioritySummary = "Caso com vulnerabilidade sensível: a tramitação deve receber fila preferencial e validação rápida.";
        }

        if (automationPolicy != null && automationPolicy.mode() == ProceduralAutomationMode.ADVISORY_ONLY) {
            recommendedDecisionBlueprint = recommendedDecisionBlueprint == null ? null : recommendedDecisionBlueprint + "_ADVISORY_ONLY";
        }
        if (quality != null && quality.reviewPressureScore() >= 0.60d) {
            firstReviewTargetMinutes = Math.min(firstReviewTargetMinutes, 20);
            magistrateEscalationTargetMinutes = Math.min(magistrateEscalationTargetMinutes, 45);
            queueBypassEligible = queueBypassEligible || lane != ProceduralAccelerationLane.STANDARD;
        }
        if (sigilo != null && sigilo.exigeCredencial()) {
            victimIdentityShieldRecommended = victimIdentityShieldRecommended || mariaDaPenha;
        }
        if (containsAny(normalizedRisk, "CRITICO", "ELEVADO") && lane == ProceduralAccelerationLane.STANDARD) {
            lane = ProceduralAccelerationLane.FAST_GREEN;
            profile = "RISCO_ELEVADO_SEM_FAIXA_ESPECIAL";
            firstReviewTargetMinutes = Math.min(firstReviewTargetMinutes, 60);
            magistrateEscalationTargetMinutes = Math.min(magistrateEscalationTargetMinutes, 150);
            queueBypassEligible = true;
            legalClockMonitoring = true;
            executivePrioritySummary = "Risco elevado: fila acelerada e revisão humana obrigatória, sem deslocar o controle jurisdicional.";
        }

        return new AccelerationContext(
                track,
                lane,
                profile,
                firstReviewTargetMinutes,
                magistrateEscalationTargetMinutes,
                technicalSupportTargetMinutes,
                queueBypassEligible,
                natJusPriorityRecommended,
                protectiveUrgencyRecommended,
                victimIdentityShieldRecommended,
                multiChannelEscalation,
                legalClockMonitoring,
                recommendedDecisionBlueprint,
                executivePrioritySummary,
                tipoJustica,
                ramo,
                rito,
                materia,
                natureza,
                sigilo,
                normalizedRisk,
                corpus,
                text(source.get("__actionNature")),
                text(source.get("__actionFamily")),
                mariaDaPenha,
                health,
                icu,
                lifeSupport
        );
    }

    private static boolean isMariaDaPenha(String corpus, RamoDireito ramo, RitoProcessual rito, MateriaJurisdicao materia) {
        return ramo == RamoDireito.PENAL
                && (containsAny(corpus, "MARIA DA PENHA", "VIOLENCIA DOMESTICA", "VIOLENCIA FAMILIAR CONTRA A MULHER", "MEDIDA PROTETIVA", "AGRESSOR", "OFENDIDA", "VIOLENCIA DE GENERO")
                || rito == RitoProcessual.PENAL_MARIA_DA_PENHA
                || materia == MateriaJurisdicao.FAMILIA);
    }

    private static boolean isHealthUrgency(String corpus, RamoDireito ramo, MateriaJurisdicao materia) {
        return ramo == RamoDireito.ADMINISTRATIVO
                || ramo == RamoDireito.CONSTITUCIONAL
                || ramo == RamoDireito.PREVIDENCIARIO
                || ramo == RamoDireito.CIVIL
                || materia == MateriaJurisdicao.SAUDE
                || containsAny(corpus, "LEITO", "UTI", "MEDICAMENTO", "CIRURGIA", "INTERNACAO", "TRANSFERENCIA HOSPITALAR", "VAGA HOSPITALAR", "TRATAMENTO ONCOLOGICO", "SUS", "SAUDE");
    }

    private static List<ProceduralAccelerationDirectiveItem> buildDirectives(AccelerationContext context,
                                                                             ProceduralIntelligenceAdvisoryReport advisory,
                                                                             ProceduralDecisionQualityReport quality,
                                                                             ProceduralAutomationPolicyReport automationPolicy) {
        LinkedHashMap<ProceduralAccelerationDirectiveCode, ProceduralAccelerationDirectiveItem> out = new LinkedHashMap<>();
        if (context.track() == ProceduralAccelerationTrack.NENHUMA) {
            addDirective(out, ProceduralAccelerationDirectiveCode.REVISAO_HUMANA_OBRIGATORIA, false, "POLICY", "Fluxo permanece ordinário com revisão humana normal.");
            addDirective(out, ProceduralAccelerationDirectiveCode.BLOQUEIO_PUBLICACAO_AUTONOMA, true, "POLICY", "A publicação automática continua indisponível em matéria jurisdicional.");
            return List.copyOf(out.values());
        }
        addDirective(out, ProceduralAccelerationDirectiveCode.LIMITACAO_AUTOMACAO_DECISORIA, true, "POLICY", "Aceleração não autoriza decisão autônoma do sistema.");
        addDirective(out, ProceduralAccelerationDirectiveCode.REVISAO_HUMANA_OBRIGATORIA, true, "POLICY", "Ato judicial final reservado a juiz, desembargador ou ministro competente.");
        addDirective(out, ProceduralAccelerationDirectiveCode.BLOQUEIO_PUBLICACAO_AUTONOMA, true, "POLICY", "Minutas permanecem assistivas e bloqueadas para publicação automática.");
        addDirective(out, ProceduralAccelerationDirectiveCode.FILA_ULTRAPRIORITARIA, false, "LANE", "Faixa: " + context.lane().name());
        addDirective(out, ProceduralAccelerationDirectiveCode.DISTRIBUICAO_IMEDIATA, false, "LANE", "Meta inicial: " + context.firstReviewTargetMinutes() + " minutos.");
        addDirective(out, ProceduralAccelerationDirectiveCode.CONCLUSAO_IMEDIATA_MAGISTRADO, false, "LANE", "Escalonamento ao gabinete em até " + context.magistrateEscalationTargetMinutes() + " minutos.");
        addDirective(out, ProceduralAccelerationDirectiveCode.CONTROLE_PRAZO_LEGAL, false, "LEGAL", "Monitoramento da janela crítica do caso urgente.");
        addDirective(out, ProceduralAccelerationDirectiveCode.ESCALONAMENTO_MULTICANAL, false, "OPS", context.multiChannelEscalation() ? "Disparo por fila, alerta e roteamento executivo." : "Escalonamento controlado somente pela fila prioritária.");
        addDirective(out, ProceduralAccelerationDirectiveCode.CHECKLIST_CUMPRIMENTO_URGENTE, false, "OPS", "O cumprimento da ordem urgente precisa nascer com checklist operacional atrelado.");
        if (context.mariaDaPenha()) {
            addDirective(out, ProceduralAccelerationDirectiveCode.PRIORIDADE_ABSOLUTA_VIDA, true, "MARIA_DA_PENHA", "Proteção imediata da ofendida e dependentes quando houver risco atual ou iminente.");
            addDirective(out, ProceduralAccelerationDirectiveCode.FORMULARIO_RISCO_OBRIGATORIO, false, "MARIA_DA_PENHA", "Aplicar e vincular avaliação estruturada de risco ao fluxo.");
            addDirective(out, ProceduralAccelerationDirectiveCode.SIGILO_REFORCADO_OFENDIDA, false, "MARIA_DA_PENHA", "Ocultar identificação da ofendida nas superfícies públicas sensíveis.");
            addDirective(out, ProceduralAccelerationDirectiveCode.REDE_PROTECAO_ACIONAVEL, false, "MARIA_DA_PENHA", "Acionar proteção, assistência e eventual restrição operacional do agressor.");
            addDirective(out, ProceduralAccelerationDirectiveCode.COMUNICACAO_AUTORIDADES_COMPETENTES, false, "MARIA_DA_PENHA", "Encadear ciência institucional conforme necessidade do caso.");
            addDirective(out, ProceduralAccelerationDirectiveCode.MONITORAMENTO_MEDIDA_PROTETIVA, false, "MARIA_DA_PENHA", "Rastrear cumprimento e descumprimento da medida protetiva.");
            addDirective(out, ProceduralAccelerationDirectiveCode.PROTECAO_DADOS_SENSIVEIS, false, "MARIA_DA_PENHA", "Tratar dados pessoais e endereço sob camada reforçada.");
        }
        if (context.health()) {
            addDirective(out, ProceduralAccelerationDirectiveCode.TRIAGEM_SAUDE_CRITICA, false, "SAUDE", "Triagem clínica e probatória em regime acelerado.");
            addDirective(out, ProceduralAccelerationDirectiveCode.PROVA_CLINICA_MINIMA, false, "SAUDE", "Exigir relatório/indicação médica minimamente idônea e atualizada.");
            addDirective(out, ProceduralAccelerationDirectiveCode.REGULACAO_LEITO_VERIFICACAO, false, "SAUDE", context.icu() ? "Verificar regulação e disponibilidade/intensidade do leito requerido." : "Verificar via assistencial, fila regulatória ou alternativa clínica equivalente.");
            if (context.natJusPriorityRecommended()) {
                addDirective(out, ProceduralAccelerationDirectiveCode.NATJUS_CONSULTA_PRIORITARIA, false, "SAUDE", "Subsidiação técnica prioritária em saúde com base em evidência científica.");
            }
            addDirective(out, ProceduralAccelerationDirectiveCode.PROTECAO_DADOS_SENSIVEIS, false, "SAUDE", "Prontuário, laudos e dados clínicos devem circular com exposição mínima.");
        }
        if (advisory != null && advisory.fallbackUsed()) {
            addDirective(out, ProceduralAccelerationDirectiveCode.REVISAO_HUMANA_OBRIGATORIA, true, "ADVISORY", "Inferência advisory utilizou fallback e mantém dependência humana reforçada.");
        }
        if (quality != null && quality.reviewPressureScore() >= 0.60d) {
            addDirective(out, ProceduralAccelerationDirectiveCode.CONCLUSAO_IMEDIATA_MAGISTRADO, false, "QUALITY", "Pressão de revisão elevada exige salto de fila com validação humana imediata.");
        }
        if (automationPolicy != null && automationPolicy.mode() == ProceduralAutomationMode.ADVISORY_ONLY) {
            addDirective(out, ProceduralAccelerationDirectiveCode.LIMITACAO_AUTOMACAO_DECISORIA, true, "AUTOMATION", "O domínio permanece estritamente em modo advisory_only.");
        }
        return List.copyOf(out.values());
    }

    private static List<String> buildEvidenceChecklist(AccelerationContext context,
                                                       String corpus,
                                                       ProceduralIntelligenceAdvisoryReport advisory) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (context.mariaDaPenha()) {
            out.add("Petição, boletim, relato escrito ou declaração idônea da ofendida com elementos mínimos do risco atual ou iminente.");
            out.add("Registro estruturado de risco e notícia de ameaças, perseguição, uso de arma ou descumprimento prévio de medida protetiva, se houver.");
            out.add("Endereço seguro de intimação, meios de contato protegidos e indicação de dependentes eventualmente expostos.");
            out.add("Elementos mínimos para avaliar afastamento do lar, proibição de contato, restrição de porte e apoio policial, se cabíveis.");
        }
        if (context.health()) {
            out.add("Relatório médico atual, legível e individualizado, com diagnóstico, risco, indicação clínica e urgência fundamentada.");
            out.add("Informação clínica sobre gravidade, suporte necessário, fila/regulação, indisponibilidade concreta ou risco de dano irreversível.");
            out.add("Documentos de internação, regulação, negativa administrativa ou prova equivalente do percurso assistencial já tentado.");
            if (context.icu() || context.lifeSupport()) {
                out.add("Dados clínicos mínimos sobre necessidade de leito intensivo ou suporte vital, com temporalidade adequada ao estado do paciente.");
            }
            if (context.natJusPriorityRecommended()) {
                out.add("Consulta técnica priorizada ao NAT-Jus/e-NatJus quando o magistrado ou serventia demandarem base técnico-científica adicional.");
            }
        }
        if (context.track() == ProceduralAccelerationTrack.PRIORIDADE_VULNERABILIDADE) {
            out.add("Comprovação mínima da condição especial de vulnerabilidade que justifique a fila prioritária reforçada.");
        }
        if (advisory != null) {
            out.addAll(advisory.recommendedDocuments().stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).limit(6).toList());
        }
        return List.copyOf(out);
    }

    private static List<String> buildOperationalChecklist(AccelerationContext context,
                                                          String corpus,
                                                          ProceduralAutomationPolicyReport automationPolicy) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (context.track() != ProceduralAccelerationTrack.NENHUMA) {
            out.add("Classificar o feito na faixa de aceleração correspondente e retirar de fila ordinária.");
            out.add("Gerar alerta executivo para gabinete e unidade operacional com relógio de urgência próprio.");
            out.add("Preservar bloqueio de publicação autônoma e trilha de revisão judicial obrigatória.");
        }
        if (context.mariaDaPenha()) {
            out.add("Aplicar mascaramento reforçado da identidade da ofendida nas superfícies operacionais não credenciadas.");
            out.add("Abrir checklist de comunicação institucional, rede de proteção e eventual apoio policial quando cabível.");
            out.add("Rastrear medidas deferidas, restrições, descumprimento e eventos críticos subsequentes.");
        }
        if (context.health()) {
            out.add("Acionar trilha clínica prioritária com verificação rápida de documentos e sinais de gravidade.");
            out.add("Sinalizar necessidade de parecer técnico ou consulta NatJus quando a urgência exigir base científica adicional.");
            out.add("Registrar situação regulatória, leito, vaga, transferência ou alternativa terapêutica já disponível no caso concreto.");
        }
        if (automationPolicy != null && automationPolicy.autoRouteEligible()) {
            out.add("Permitir apenas aceleração operacional do roteamento, sem automatizar o ato decisório final.");
        }
        if (containsAny(corpus, "FINAL DE SEMANA", "MADRUGADA", "PLANTAO", "FERIADO")) {
            out.add("Respeitar regime de plantão ou contingência quando o caso urgente alcançar janelas extraordinárias.");
        }
        return List.copyOf(out);
    }

    private static List<String> buildLegalBases(AccelerationContext context, String corpus) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (context.mariaDaPenha()) {
            out.add("Lei 11.340/2006, art. 18");
            out.add("Lei 11.340/2006, art. 19");
            out.add("Lei 11.340/2006, art. 12-C");
            out.add("Lei 11.340/2006, art. 23");
            out.add("Lei 14.550/2023");
            out.add("Lei 14.857/2024");
            out.add("Lei 14.149/2021");
            out.add("Lei 14.022/2020");
        }
        if (context.health()) {
            out.add("CF, art. 196");
            out.add("Lei 8.080/1990, arts. 2º e 7º");
            out.add("CPC, art. 300");
            out.add("CPC, arts. 497 e 536");
            out.add("CNJ Resolução 238/2016");
            out.add("CNJ Provimento 84/2019");
        }
        if (context.track() == ProceduralAccelerationTrack.INFANCIA_PROTECAO_URGENTE) {
            out.add("Lei 13.431/2017");
            out.add("ECA, princípio da proteção integral");
        }
        if (context.track() == ProceduralAccelerationTrack.IDOSO_RISCO_ELEVADO) {
            out.add("Estatuto da Pessoa Idosa");
        }
        if (!context.mariaDaPenha() && !context.health() && context.track() != ProceduralAccelerationTrack.NENHUMA) {
            out.add("CPC, tutela de urgência e poderes de efetivação");
        }
        if (containsAny(corpus, "SEGREDO", "SIGILO", "DADOS SENSIVEIS")) {
            out.add("LGPD e proteção de dados sensíveis em fluxo jurisdicional");
        }
        return List.copyOf(out);
    }

    private static List<String> buildAlerts(AccelerationContext context,
                                            String corpus,
                                            ProceduralIntelligenceAdvisoryReport advisory,
                                            ProceduralAutomationPolicyReport automationPolicy) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (context.track() != ProceduralAccelerationTrack.NENHUMA) {
            out.add("Aceleração processual não substitui cognição judicial nem dispensa fundamentação individualizada.");
        }
        if (context.mariaDaPenha()) {
            out.add("Fluxo deve privilegiar proteção insuficiente zero, evitando exposição indevida da ofendida.");
            if (containsAny(corpus, "ARMA", "PORTE DE ARMA", "POLICIAL", "MILITAR", "GUARDA MUNICIPAL")) {
                out.add("Há elemento potencialmente sensível envolvendo arma ou agente armado; revisar comunicação institucional e restrições correlatas.");
            }
        }
        if (context.health()) {
            out.add("Fluxo de saúde urgente deve buscar evidência clínica mínima e aderência à situação concreta, evitando decisão descolada do quadro atual.");
            if (context.natJusPriorityRecommended()) {
                out.add("Apoio técnico-científico é recomendável para elevar segurança decisória em matéria de saúde.");
            }
        }
        if (advisory != null && advisory.fallbackUsed()) {
            out.add("Inferência advisory utilizou fallback; recomenda-se cautela adicional na validação humana.");
        }
        if (automationPolicy != null && automationPolicy.mode() == ProceduralAutomationMode.ADVISORY_ONLY) {
            out.add("O domínio do caso não admite automação operacional decisória além do modo assistivo.");
        }
        return List.copyOf(out);
    }

    private static LinkedHashMap<String, Object> buildMetadata(AccelerationContext context,
                                                               String corpus,
                                                               List<ProceduralAccelerationDirectiveItem> directives,
                                                               List<String> evidenceChecklist,
                                                               List<String> legalBases,
                                                               ProceduralIntelligenceAdvisoryReport advisory,
                                                               ProceduralDecisionQualityReport quality,
                                                               ProceduralAutomationPolicyReport automationPolicy,
                                                               ProceduralExecutiveExplainabilityReport executiveExplainability) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("corpusFingerprint", Integer.toHexString(corpus.hashCode()));
        out.put("trackFamily", switch (context.track()) {
            case MARIA_DA_PENHA_PROTECAO_URGENTE, MARIA_DA_PENHA_REDE_PROTECAO -> "PROTECAO_GENERO";
            case SAUDE_LEITO_UTI, SAUDE_SUPORTE_VITAL, SAUDE_FORNECIMENTO_ESSENCIAL -> "SAUDE_URGENTE";
            case INFANCIA_PROTECAO_URGENTE -> "INFANCIA_URGENTE";
            case IDOSO_RISCO_ELEVADO -> "IDOSO_PROTECAO";
            case PRIORIDADE_VULNERABILIDADE -> "VULNERABILIDADE_PRIORITARIA";
            case NENHUMA -> "ORDINARIO";
        });
        out.put("directiveCount", directives.size());
        out.put("blockingDirectiveCount", directives.stream().filter(ProceduralAccelerationDirectiveItem::blocking).count());
        out.put("evidenceChecklistCount", evidenceChecklist.size());
        out.put("legalBasisCount", legalBases.size());
        out.put("queueBypassEligible", context.queueBypassEligible());
        out.put("immediateHumanGate", true);
        out.put("publicationLocked", true);
        out.put("natJusPriorityRecommended", context.natJusPriorityRecommended());
        out.put("victimIdentityShieldRecommended", context.victimIdentityShieldRecommended());
        out.put("multiChannelEscalation", context.multiChannelEscalation());
        out.put("legalClockMonitoring", context.legalClockMonitoring());
        out.put("recommendedDecisionBlueprint", context.recommendedDecisionBlueprint());
        out.put("firstReviewTargetMinutes", context.firstReviewTargetMinutes());
        out.put("magistrateEscalationTargetMinutes", context.magistrateEscalationTargetMinutes());
        out.put("technicalSupportTargetMinutes", context.technicalSupportTargetMinutes());
        out.put("mariaDaPenha", context.mariaDaPenha());
        out.put("healthUrgency", context.health());
        out.put("icuUrgency", context.icu());
        out.put("lifeSupportUrgency", context.lifeSupport());
        out.put("ramoDireito", context.ramo() != null ? context.ramo().name() : null);
        out.put("rito", context.rito() != null ? context.rito().name() : null);
        out.put("materia", context.materia() != null ? context.materia().name() : null);
        out.put("natureza", context.natureza() != null ? context.natureza().name() : null);
        out.put("sigilo", context.sigilo() != null ? context.sigilo().name() : null);
        out.put("riskLevel", context.riskLevel());
        out.put("tipoJustica", context.tipoJustica() != null ? context.tipoJustica().name() : null);
        out.put("actionNature", context.actionNature());
        out.put("actionFamily", context.actionFamily());
        if (advisory != null) {
            out.put("advisoryConfidence", advisory.confidence());
            out.put("advisoryPrimaryReason", advisory.primaryReason());
        }
        if (quality != null) {
            out.put("decisionDeterminism", quality.determinismScore());
            out.put("reviewPressureScore", quality.reviewPressureScore());
            out.put("operatingModeHint", quality.operatingModeHint());
        }
        if (automationPolicy != null) {
            out.put("automationMode", automationPolicy.mode().name());
            out.put("automationDomain", automationPolicy.domain().name());
        }
        if (executiveExplainability != null) {
            out.put("executiveSummary", executiveExplainability.summary());
            out.put("executiveActionFrame", executiveExplainability.actionFrame());
        }
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return out;
    }

    private static void addDirective(Map<ProceduralAccelerationDirectiveCode, ProceduralAccelerationDirectiveItem> out,
                                     ProceduralAccelerationDirectiveCode code,
                                     boolean blocking,
                                     String source,
                                     String detail) {
        if (out.containsKey(code)) {
            return;
        }
        out.put(code, new ProceduralAccelerationDirectiveItem(code, ProceduralAccelerationDirectiveMessages.require(code), blocking, source, detail));
    }

    private static String buildCorpus(Map<String, Object> source) {
        ArrayList<String> parts = new ArrayList<>();
        source.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            if (key.startsWith("__") || key.startsWith("canonical:")) {
                parts.add(text(value));
                return;
            }
            if (value instanceof CharSequence) {
                parts.add(value.toString());
            } else if (value instanceof Collection<?> collection) {
                collection.stream().filter(Objects::nonNull).map(Object::toString).forEach(parts::add);
            }
        });
        return normalize(parts.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).reduce((a, b) -> a + ' ' + b).orElse(""));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return normalized.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static boolean containsAny(String source, String... needles) {
        if (source == null || source.isBlank() || needles == null) {
            return false;
        }
        String normalized = normalize(source);
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && normalized.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record AccelerationContext(
            ProceduralAccelerationTrack track,
            ProceduralAccelerationLane lane,
            String profile,
            int firstReviewTargetMinutes,
            int magistrateEscalationTargetMinutes,
            int technicalSupportTargetMinutes,
            boolean queueBypassEligible,
            boolean natJusPriorityRecommended,
            boolean protectiveUrgencyRecommended,
            boolean victimIdentityShieldRecommended,
            boolean multiChannelEscalation,
            boolean legalClockMonitoring,
            String recommendedDecisionBlueprint,
            String executivePrioritySummary,
            TipoJustica tipoJustica,
            RamoDireito ramo,
            RitoProcessual rito,
            MateriaJurisdicao materia,
            NaturezaJuridicaCanonical natureza,
            NivelSigilo sigilo,
            String riskLevel,
            String corpus,
            String actionNature,
            String actionFamily,
            boolean mariaDaPenha,
            boolean health,
            boolean icu,
            boolean lifeSupport
    ) {
    }
}
