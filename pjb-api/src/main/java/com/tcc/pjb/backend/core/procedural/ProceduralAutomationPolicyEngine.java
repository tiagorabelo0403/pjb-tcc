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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ProceduralAutomationPolicyEngine {

    private ProceduralAutomationPolicyEngine() {
    }

    public static ProceduralAutomationPolicyReport analyze(Map<String, Object> payload,
                                                           String actionNature,
                                                           String actionFamily,
                                                           TipoJustica routingTipoJustica,
                                                           String routingRito,
                                                           String riskLevel,
                                                           ProceduralIntelligenceAdvisoryReport advisory,
                                                           ProceduralDecisionQualityReport quality) {
        LinkedHashMap<String, Object> source = new LinkedHashMap<>();
        if (payload != null) {
            source.putAll(payload);
        }
        source.put("__routingActionNature", actionNature);
        source.put("__routingActionFamily", actionFamily);
        source.put("__routingTipoJustica", routingTipoJustica != null ? routingTipoJustica.name() : null);
        source.put("__routingRito", routingRito);
        source.put("__riskLevel", riskLevel);

        String corpus = buildCorpus(source, advisory);
        PolicyContext context = resolveContext(source, corpus, advisory);
        List<ProceduralAutomationGate> gates = collectGates(source, corpus, context, advisory, quality);
        Set<ProceduralAutomationCapability> allowed = resolveAllowedCapabilities(context, advisory, quality, gates);
        EnumSet<ProceduralAutomationCapability> blocked = EnumSet.allOf(ProceduralAutomationCapability.class);
        blocked.removeAll(allowed);
        if (context.domain() == ProceduralAutomationDomain.HIGH_SECRECY
                || context.domain() == ProceduralAutomationDomain.PENAL_SENSITIVE
                || context.domain() == ProceduralAutomationDomain.ELECTORAL_SENSITIVE
                || context.domain() == ProceduralAutomationDomain.MILITARY_SENSITIVE
                || context.domain() == ProceduralAutomationDomain.INTERNATIONAL_COOPERATION) {
            blocked.add(ProceduralAutomationCapability.ROUTING_AUTOMATION);
        }

        List<String> rationale = collectRationale(context, advisory, quality, gates);
        List<String> riskFactors = collectRiskFactors(context, advisory, quality, gates);
        ProceduralAutomationMode mode = resolveMode(context, advisory, quality, gates, allowed);

        boolean autoClassifyEligible = allowed.contains(ProceduralAutomationCapability.AXIS_CLASSIFICATION)
                && mode != ProceduralAutomationMode.ADVISORY_ONLY;
        boolean autoRouteEligible = allowed.contains(ProceduralAutomationCapability.ROUTING_AUTOMATION)
                && mode == ProceduralAutomationMode.AUTOMATE_SAFE;
        boolean autoProtocolBlueprintEligible = allowed.contains(ProceduralAutomationCapability.PROTOCOL_BLUEPRINT_PREPARATION)
                && mode != ProceduralAutomationMode.ADVISORY_ONLY;
        boolean autoSigiloSuggestionEligible = allowed.contains(ProceduralAutomationCapability.SIGILO_SUGGESTION);
        boolean autoDispatchHintEligible = allowed.contains(ProceduralAutomationCapability.INTERNAL_DISPATCH_HINTING)
                && mode != ProceduralAutomationMode.HUMAN_GATE_REQUIRED;

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("domainSensitivity", domainSensitivity(context.domain()));
        metadata.put("advisoryConfidence", advisory != null ? advisory.confidence() : null);
        metadata.put("decisionDeterminism", quality != null ? quality.determinismScore() : null);
        metadata.put("decisionConvergence", quality != null ? quality.convergenceScore() : null);
        metadata.put("reviewPressure", quality != null ? quality.reviewPressureScore() : null);
        metadata.put("routingRiskLevel", text(source.get("__riskLevel")));
        metadata.put("gatesCount", gates.size());
        metadata.put("hardGatesCount", gates.stream().filter(ProceduralAutomationGate::blocking).count());
        metadata.put("softGatesCount", gates.stream().filter(g -> !g.blocking()).count());
        metadata.put("context", context.toMap());
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new ProceduralAutomationPolicyReport(
                Instant.now(),
                context.domain(),
                mode,
                autoClassifyEligible,
                autoRouteEligible,
                autoProtocolBlueprintEligible,
                autoSigiloSuggestionEligible,
                autoDispatchHintEligible,
                allowed.stream().sorted(Comparator.comparing(Enum::name)).toList(),
                blocked.stream().sorted(Comparator.comparing(Enum::name)).toList(),
                List.copyOf(gates),
                rationale,
                riskFactors,
                Collections.unmodifiableMap(metadata)
        );
    }

    private static PolicyContext resolveContext(Map<String, Object> source,
                                                String corpus,
                                                ProceduralIntelligenceAdvisoryReport advisory) {
        TipoJustica tipoJustica = advisory != null && advisory.suggestedTipoJustica() != null
                ? advisory.suggestedTipoJustica()
                : TipoJustica.fromString(firstNonBlank(text(source.get("tipoJustica")), text(source.get("__routingTipoJustica"))));
        RamoDireito ramo = advisory != null && advisory.suggestedRamo() != null
                ? advisory.suggestedRamo()
                : RamoDireito.fromString(firstNonBlank(text(source.get("ramoDireito")), text(source.get("materia")), text(source.get("__routingActionFamily"))));
        RitoProcessual rito = advisory != null && advisory.suggestedRito() != null
                ? advisory.suggestedRito()
                : RitoProcessual.fromString(firstNonBlank(text(source.get("rito")), text(source.get("__routingRito"))));
        MateriaJurisdicao materia = advisory != null && advisory.suggestedMateria() != null
                ? advisory.suggestedMateria()
                : MateriaJurisdicao.fromString(firstNonBlank(text(source.get("materia")), text(source.get("assunto"))));
        NaturezaJuridicaCanonical natureza = advisory != null ? advisory.naturezaPrincipal() : null;
        NivelSigilo sigilo = advisory != null && advisory.suggestedSigilo() != null
                ? advisory.suggestedSigilo()
                : NivelSigilo.fromString(text(source.get("nivelSigilo")));
        ProceduralAutomationDomain domain = resolveDomain(tipoJustica, ramo, rito, materia, natureza, sigilo, corpus);
        return new PolicyContext(domain, tipoJustica, ramo, rito, materia, natureza, sigilo);
    }

    private static ProceduralAutomationDomain resolveDomain(TipoJustica tipoJustica,
                                                            RamoDireito ramo,
                                                            RitoProcessual rito,
                                                            MateriaJurisdicao materia,
                                                            NaturezaJuridicaCanonical natureza,
                                                            NivelSigilo sigilo,
                                                            String corpus) {
        if (sigilo != null && sigilo.exigeCredencial() && sigilo.nivel() >= NivelSigilo.SIGILO_N3.nivel()) {
            return ProceduralAutomationDomain.HIGH_SECRECY;
        }
        if (tipoJustica == TipoJustica.ELEITORAL || ramo == RamoDireito.ELEITORAL || containsAny(corpus, "AIJE", "AIME", "AIRC", "RCED", "REGISTRO DE CANDIDATURA")) {
            return ProceduralAutomationDomain.ELECTORAL_SENSITIVE;
        }
        if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL || ramo == RamoDireito.MILITAR || containsAny(corpus, "IPM", "CRIME MILITAR", "AUDITORIA MILITAR")) {
            return ProceduralAutomationDomain.MILITARY_SENSITIVE;
        }
        if (ramo == RamoDireito.PENAL || containsAny(corpus, "HABEAS CORPUS", "DENUNCIA", "TRIBUNAL DO JURI", "LEI DE DROGAS", "MARIA DA PENHA")) {
            return ProceduralAutomationDomain.PENAL_SENSITIVE;
        }
        if (ramo == RamoDireito.INTERNACIONAL || tipoJustica == TipoJustica.SUPERIOR || containsAny(corpus, "SENTENCA ESTRANGEIRA", "CARTA ROGATORIA", "COOPERACAO JURIDICA INTERNACIONAL")) {
            return ProceduralAutomationDomain.INTERNATIONAL_COOPERATION;
        }
        if (natureza == NaturezaJuridicaCanonical.ESTRUTURAL_COLETIVA || containsAny(corpus, "ACAO CIVIL PUBLICA", "POLITICA PUBLICA", "DIREITOS DIFUSOS", "ESTRUTURAL")) {
            return ProceduralAutomationDomain.COLLECTIVE_STRUCTURAL;
        }
        if (natureza == NaturezaJuridicaCanonical.MANDAMENTAL || ramo == RamoDireito.CONSTITUCIONAL || containsAny(corpus, "MANDADO DE SEGURANCA", "MANDADO DE INJUNCAO", "HABEAS DATA", "ADI", "ADC", "ADPF")) {
            return ProceduralAutomationDomain.CONSTITUTIONAL_MANDAMENTAL;
        }
        if (natureza == NaturezaJuridicaCanonical.JURISDICAO_VOLUNTARIA || natureza == NaturezaJuridicaCanonical.HOMOLOGATORIA || containsAny(corpus, "INVENTARIO", "ARROLAMENTO", "PARTILHA AMIGAVEL", "HOMOLOGACAO DE ACORDO", "ALVARA JUDICIAL")) {
            return ProceduralAutomationDomain.VOLUNTARY_HOMOLOGATORY;
        }
        if (natureza == NaturezaJuridicaCanonical.EXECUTIVA || ramo == RamoDireito.TRIBUTARIO || containsAny(corpus, "EXECUCAO", "CUMPRIMENTO DE SENTENCA", "CDA", "PENHORA", "EXPROPRIACAO")) {
            return ramo == RamoDireito.TRIBUTARIO
                    ? ProceduralAutomationDomain.TAX_AND_PUBLIC_FINANCE
                    : ProceduralAutomationDomain.EXECUTION_AND_ENFORCEMENT;
        }
        if (ramo == RamoDireito.TRABALHISTA || ramo == RamoDireito.PREVIDENCIARIO || tipoJustica == TipoJustica.TRABALHO || containsAny(corpus, "CLT", "INSS", "BPC", "LOAS", "APOSENTADORIA")) {
            return ProceduralAutomationDomain.LABOR_AND_SOCIAL;
        }
        if (ramo == RamoDireito.ADMINISTRATIVO || containsAny(corpus, "IMPROBIDADE", "PAD", "CONCURSO PUBLICO", "SERVIDOR PUBLICO")) {
            return ProceduralAutomationDomain.ADMINISTRATIVE_CONTROL;
        }
        if (ramo == RamoDireito.FAMILIA || containsAny(corpus, "ALIMENTOS", "DIVORCIO", "GUARDA", "CURATELA", "INTERDICAO", "HERANCA")) {
            return ProceduralAutomationDomain.FAMILY_AND_SUCCESSIONS;
        }
        return ProceduralAutomationDomain.LOW_SENSITIVITY_CIVIL;
    }

    private static List<ProceduralAutomationGate> collectGates(Map<String, Object> source,
                                                               String corpus,
                                                               PolicyContext context,
                                                               ProceduralIntelligenceAdvisoryReport advisory,
                                                               ProceduralDecisionQualityReport quality) {
        LinkedHashSet<ProceduralAutomationGate> gates = new LinkedHashSet<>();
        String riskLevel = normalize(text(source.get("__riskLevel")));
        if ("CRITICO".equals(riskLevel)) {
            gates.add(new ProceduralAutomationGate.HardGate("RISK_CRITICAL", "ROUTING", "Risco crítico no roteamento", "Nível de risco crítico bloqueia automação operacional"));
        } else if ("ALTO".equals(riskLevel) || "ELEVADO".equals(riskLevel)) {
            gates.add(new ProceduralAutomationGate.SoftGate("RISK_HIGH", "ROUTING", "Risco elevado exige cautela", "Manter fluxo assistido ou revisão adicional"));
        }
        if (advisory != null && advisory.reviewRequired()) {
            gates.add(new ProceduralAutomationGate.HardGate("ADVISORY_REVIEW_REQUIRED", "ADVISORY", "Camada advisory exige revisão humana", advisory.primaryReason()));
        }
        if (advisory != null && advisory.fallbackUsed()) {
            gates.add(new ProceduralAutomationGate.SoftGate("ADVISORY_FALLBACK", "ADVISORY", "A inferência utilizou fallback", advisory.primaryReason()));
        }
        if (quality != null && !quality.conflicts().isEmpty()) {
            String detail = String.join(" | ", quality.conflicts().stream().limit(3).toList());
            gates.add(new ProceduralAutomationGate.HardGate("AXIS_CONFLICT", "QUALITY", "Há conflito entre eixos canônicos", detail));
        }
        if (quality != null && quality.reviewPressureScore() >= 0.60d) {
            gates.add(new ProceduralAutomationGate.HardGate("REVIEW_PRESSURE_HIGH", "QUALITY", "Pressão de revisão humana elevada", text(quality.operatingModeHint())));
        } else if (quality != null && quality.reviewPressureScore() >= 0.35d) {
            gates.add(new ProceduralAutomationGate.SoftGate("REVIEW_PRESSURE_MODERATE", "QUALITY", "Pressão moderada de revisão", text(quality.operatingModeHint())));
        }
        if (quality != null && quality.determinismScore() < 0.48d) {
            gates.add(new ProceduralAutomationGate.HardGate("LOW_DETERMINISM", "QUALITY", "Determinismo insuficiente", "A decisão não alcançou previsibilidade suficiente"));
        } else if (quality != null && quality.determinismScore() < 0.68d) {
            gates.add(new ProceduralAutomationGate.SoftGate("DETERMINISM_TRANSITIONAL", "QUALITY", "Determinismo intermediário", "Convém operar em modo assistido"));
        }
        if (context.sigilo() != null && context.sigilo().exigeCredencial()) {
            gates.add(new ProceduralAutomationGate.HardGate("RESTRICTED_SECRECY", "SIGILO", "Caso com sigilo reforçado", context.sigilo().name()));
        }
        if (context.domain() == ProceduralAutomationDomain.COLLECTIVE_STRUCTURAL) {
            gates.add(new ProceduralAutomationGate.HardGate("STRUCTURAL_COLLECTIVE", "DOMAIN", "Demanda estrutural coletiva", "Não é elegível para automação operacional direta"));
        }
        if (context.domain() == ProceduralAutomationDomain.PENAL_SENSITIVE
                || context.domain() == ProceduralAutomationDomain.ELECTORAL_SENSITIVE
                || context.domain() == ProceduralAutomationDomain.MILITARY_SENSITIVE
                || context.domain() == ProceduralAutomationDomain.INTERNATIONAL_COOPERATION) {
            gates.add(new ProceduralAutomationGate.HardGate("SENSITIVE_DOMAIN", "DOMAIN", "Domínio sensível", context.domain().label()));
        }
        if (context.domain() == ProceduralAutomationDomain.CONSTITUTIONAL_MANDAMENTAL) {
            gates.add(new ProceduralAutomationGate.SoftGate("MANDAMENTAL_DOMAIN", "DOMAIN", "Domínio mandamental exige cautela", context.domain().label()));
        }
        if (advisory != null && advisory.urgent()) {
            gates.add(new ProceduralAutomationGate.SoftGate("URGENT_CASE", "ADVISORY", "Caso urgente", "Urgência recomenda confirmação humana adicional"));
        }
        if (advisory != null && advisory.riskFlags().stream().anyMatch(flag -> containsAny(normalize(flag), "SIGILO", "FRAUDE", "COMPETENCIA_INCERTA", "URGENCIA_DOCUMENTAL"))) {
            String detail = String.join(" | ", advisory.riskFlags().stream().limit(4).toList());
            gates.add(new ProceduralAutomationGate.SoftGate("ADVISORY_RISK_FLAGS", "ADVISORY", "Risk flags relevantes", detail));
        }
        if (containsAny(corpus, "SEGREDO DE ESTADO", "SEGREDO ESTADO", "OPERACAO SIGILOSA", "INFORMACAO CLASSIFICADA")) {
            gates.add(new ProceduralAutomationGate.HardGate("STATE_SECRET_CONTEXT", "TEXT", "Contexto de segredo máximo", "Fluxo deve permanecer estritamente controlado"));
        }
        return List.copyOf(gates);
    }

    private static Set<ProceduralAutomationCapability> resolveAllowedCapabilities(PolicyContext context,
                                                                                  ProceduralIntelligenceAdvisoryReport advisory,
                                                                                  ProceduralDecisionQualityReport quality,
                                                                                  List<ProceduralAutomationGate> gates) {
        EnumSet<ProceduralAutomationCapability> allowed = EnumSet.of(
                ProceduralAutomationCapability.AXIS_CLASSIFICATION,
                ProceduralAutomationCapability.DOCUMENT_CHECKLIST_ENRICHMENT,
                ProceduralAutomationCapability.RISK_SCORING,
                ProceduralAutomationCapability.MANUAL_REVIEW_ESCALATION
        );
        if (context.domain() != ProceduralAutomationDomain.HIGH_SECRECY) {
            allowed.add(ProceduralAutomationCapability.TPU_CLASS_SUGGESTION);
            allowed.add(ProceduralAutomationCapability.COMPETENCE_SUGGESTION);
            allowed.add(ProceduralAutomationCapability.TERRITORIAL_SNAPSHOT_ENRICHMENT);
            allowed.add(ProceduralAutomationCapability.ECONOMIC_SCREENING);
            allowed.add(ProceduralAutomationCapability.PRE_PROTOCOL_SCREENING);
            allowed.add(ProceduralAutomationCapability.PROTOCOL_BLUEPRINT_PREPARATION);
        }
        if (context.sigilo() == null || !context.sigilo().exigeCredencial()) {
            allowed.add(ProceduralAutomationCapability.SIGILO_SUGGESTION);
        }
        if (quality != null && quality.safeAutomationEligible() && quality.determinismScore() >= 0.78d && gates.stream().noneMatch(ProceduralAutomationGate::blocking)) {
            allowed.add(ProceduralAutomationCapability.ROUTING_AUTOMATION);
            allowed.add(ProceduralAutomationCapability.DISTRIBUTION_PRECHECK);
            allowed.add(ProceduralAutomationCapability.INTERNAL_DISPATCH_HINTING);
        }
        if (context.domain() == ProceduralAutomationDomain.VOLUNTARY_HOMOLOGATORY || context.domain() == ProceduralAutomationDomain.LOW_SENSITIVITY_CIVIL) {
            allowed.add(ProceduralAutomationCapability.INTERNAL_DISPATCH_HINTING);
        }
        if (context.domain() == ProceduralAutomationDomain.EXECUTION_AND_ENFORCEMENT || context.domain() == ProceduralAutomationDomain.TAX_AND_PUBLIC_FINANCE) {
            allowed.add(ProceduralAutomationCapability.DISTRIBUTION_PRECHECK);
        }
        if (advisory != null && advisory.confidence() < 0.62d) {
            allowed.remove(ProceduralAutomationCapability.ROUTING_AUTOMATION);
            allowed.remove(ProceduralAutomationCapability.INTERNAL_DISPATCH_HINTING);
        }
        if (context.domain() == ProceduralAutomationDomain.COLLECTIVE_STRUCTURAL
                || context.domain() == ProceduralAutomationDomain.PENAL_SENSITIVE
                || context.domain() == ProceduralAutomationDomain.ELECTORAL_SENSITIVE
                || context.domain() == ProceduralAutomationDomain.MILITARY_SENSITIVE
                || context.domain() == ProceduralAutomationDomain.INTERNATIONAL_COOPERATION
                || context.domain() == ProceduralAutomationDomain.HIGH_SECRECY) {
            allowed.remove(ProceduralAutomationCapability.ROUTING_AUTOMATION);
            allowed.remove(ProceduralAutomationCapability.DISTRIBUTION_PRECHECK);
            allowed.remove(ProceduralAutomationCapability.INTERNAL_DISPATCH_HINTING);
        }
        if (gates.stream().anyMatch(ProceduralAutomationGate::blocking)) {
            allowed.remove(ProceduralAutomationCapability.ROUTING_AUTOMATION);
            allowed.remove(ProceduralAutomationCapability.DISTRIBUTION_PRECHECK);
            allowed.remove(ProceduralAutomationCapability.INTERNAL_DISPATCH_HINTING);
        }
        return Set.copyOf(allowed);
    }

    private static ProceduralAutomationMode resolveMode(PolicyContext context,
                                                        ProceduralIntelligenceAdvisoryReport advisory,
                                                        ProceduralDecisionQualityReport quality,
                                                        List<ProceduralAutomationGate> gates,
                                                        Set<ProceduralAutomationCapability> allowed) {
        long hardGates = gates.stream().filter(ProceduralAutomationGate::blocking).count();
        double determinism = quality != null ? quality.determinismScore() : 0d;
        double convergence = quality != null ? quality.convergenceScore() : 0d;
        double reviewPressure = quality != null ? quality.reviewPressureScore() : 1d;
        double confidence = advisory != null ? advisory.confidence() : 0d;
        if (context.domain() == ProceduralAutomationDomain.HIGH_SECRECY
                || context.domain() == ProceduralAutomationDomain.PENAL_SENSITIVE
                || context.domain() == ProceduralAutomationDomain.ELECTORAL_SENSITIVE
                || context.domain() == ProceduralAutomationDomain.MILITARY_SENSITIVE
                || context.domain() == ProceduralAutomationDomain.INTERNATIONAL_COOPERATION) {
            return ProceduralAutomationMode.ADVISORY_ONLY;
        }
        if (hardGates >= 2 || reviewPressure >= 0.60d || determinism < 0.45d) {
            return ProceduralAutomationMode.HUMAN_GATE_REQUIRED;
        }
        if (allowed.contains(ProceduralAutomationCapability.ROUTING_AUTOMATION)
                && confidence >= 0.78d
                && convergence >= 0.72d
                && determinism >= 0.78d
                && reviewPressure <= 0.30d) {
            return ProceduralAutomationMode.AUTOMATE_SAFE;
        }
        if (allowed.contains(ProceduralAutomationCapability.AXIS_CLASSIFICATION)
                && confidence >= 0.56d
                && convergence >= 0.48d) {
            return ProceduralAutomationMode.ASSISTED_DECISION;
        }
        return ProceduralAutomationMode.ADVISORY_ONLY;
    }

    private static List<String> collectRationale(PolicyContext context,
                                                 ProceduralIntelligenceAdvisoryReport advisory,
                                                 ProceduralDecisionQualityReport quality,
                                                 List<ProceduralAutomationGate> gates) {
        LinkedHashSet<String> rationale = new LinkedHashSet<>();
        rationale.add("Domínio operacional: " + context.domain().label());
        if (context.natureza() != null) {
            rationale.add("Natureza canônica: " + context.natureza().label());
        }
        if (advisory != null && advisory.primaryReason() != null && !advisory.primaryReason().isBlank()) {
            rationale.add(advisory.primaryReason());
        }
        if (quality != null) {
            if (!quality.axisConsensus().isEmpty()) {
                rationale.add("Consenso: " + String.join(" | ", quality.axisConsensus().stream().limit(3).toList()));
            }
            if (!quality.strongSignals().isEmpty()) {
                rationale.add("Sinais fortes: " + String.join(" | ", quality.strongSignals().stream().limit(3).toList()));
            }
        }
        if (gates.stream().noneMatch(ProceduralAutomationGate::blocking)) {
            rationale.add("Nenhum bloqueio operacional rígido identificado");
        }
        return List.copyOf(rationale);
    }

    private static List<String> collectRiskFactors(PolicyContext context,
                                                   ProceduralIntelligenceAdvisoryReport advisory,
                                                   ProceduralDecisionQualityReport quality,
                                                   List<ProceduralAutomationGate> gates) {
        LinkedHashSet<String> risks = new LinkedHashSet<>();
        risks.add("SENSIBILIDADE_" + context.domain().name());
        if (advisory != null) {
            risks.addAll(advisory.riskFlags());
            if (advisory.urgent()) {
                risks.add("URGENTE");
            }
            if (advisory.reviewRequired()) {
                risks.add("REVISAO_OBRIGATORIA");
            }
            if (advisory.fallbackUsed()) {
                risks.add("FALLBACK_ADVISORY");
            }
        }
        if (quality != null) {
            risks.addAll(quality.riskSignals());
            if (!quality.conflicts().isEmpty()) {
                risks.add("CONFLITO_ENTRE_EIXOS");
            }
            if (quality.reviewPressureScore() >= 0.35d) {
                risks.add("PRESSAO_REVISAO_ELEVADA");
            }
        }
        risks.addAll(gates.stream().map(ProceduralAutomationGate::code).toList());
        return List.copyOf(risks.stream().filter(Objects::nonNull).map(ProceduralAutomationPolicyEngine::normalize).filter(s -> !s.isBlank()).toList());
    }

    private static String buildCorpus(Map<String, Object> source, ProceduralIntelligenceAdvisoryReport advisory) {
        ArrayList<String> tokens = new ArrayList<>();
        source.forEach((key, value) -> appendTokens(tokens, value));
        if (advisory != null) {
            appendTokens(tokens, advisory.naturezaPrincipal() != null ? advisory.naturezaPrincipal().name() : null);
            appendTokens(tokens, advisory.qualifiers().stream().map(Enum::name).toList());
            appendTokens(tokens, advisory.suggestedTipoJustica() != null ? advisory.suggestedTipoJustica().name() : null);
            appendTokens(tokens, advisory.suggestedRamo() != null ? advisory.suggestedRamo().name() : null);
            appendTokens(tokens, advisory.suggestedRito() != null ? advisory.suggestedRito().name() : null);
            appendTokens(tokens, advisory.suggestedMateria() != null ? advisory.suggestedMateria().name() : null);
            appendTokens(tokens, advisory.suggestedSigilo() != null ? advisory.suggestedSigilo().name() : null);
            appendTokens(tokens, advisory.supportingSignals());
            appendTokens(tokens, advisory.riskFlags());
            appendTokens(tokens, advisory.recommendedDocuments());
        }
        return normalize(String.join(" ", tokens));
    }

    private static void appendTokens(List<String> tokens, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof CharSequence sequence) {
            String textual = sequence.toString();
            if (!textual.isBlank()) {
                tokens.add(textual);
            }
            return;
        }
        if (value instanceof Enum<?> enumeration) {
            tokens.add(enumeration.name());
            return;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> appendTokens(tokens, item));
            return;
        }
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(item -> appendTokens(tokens, item));
            return;
        }
        tokens.add(String.valueOf(value));
    }

    private static String domainSensitivity(ProceduralAutomationDomain domain) {
        return switch (domain) {
            case HIGH_SECRECY, PENAL_SENSITIVE, ELECTORAL_SENSITIVE, MILITARY_SENSITIVE, INTERNATIONAL_COOPERATION -> "MAXIMA";
            case COLLECTIVE_STRUCTURAL, CONSTITUTIONAL_MANDAMENTAL, TAX_AND_PUBLIC_FINANCE, ADMINISTRATIVE_CONTROL -> "ALTA";
            case FAMILY_AND_SUCCESSIONS, LABOR_AND_SOCIAL, EXECUTION_AND_ENFORCEMENT -> "MODERADA";
            case VOLUNTARY_HOMOLOGATORY, LOW_SENSITIVITY_CIVIL -> "CONTROLADA";
        };
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean containsAny(String text, String... terms) {
        if (text == null || text.isBlank() || terms == null || terms.length == 0) {
            return false;
        }
        for (String term : terms) {
            if (term != null && !term.isBlank() && text.contains(normalize(term))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replace('/', ' ')
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private record PolicyContext(
            ProceduralAutomationDomain domain,
            TipoJustica tipoJustica,
            RamoDireito ramo,
            RitoProcessual rito,
            MateriaJurisdicao materia,
            NaturezaJuridicaCanonical natureza,
            NivelSigilo sigilo
    ) {
        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("domain", domain != null ? domain.name() : null);
            out.put("tipoJustica", tipoJustica != null ? tipoJustica.name() : null);
            out.put("ramo", ramo != null ? ramo.name() : null);
            out.put("rito", rito != null ? rito.name() : null);
            out.put("materia", materia != null ? materia.name() : null);
            out.put("natureza", natureza != null ? natureza.name() : null);
            out.put("sigilo", sigilo != null ? sigilo.name() : null);
            out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
            return Collections.unmodifiableMap(out);
        }
    }
}
