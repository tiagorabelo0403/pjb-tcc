package com.tcc.pjb.backend.service.processual.guard;

import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.EnumText;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DefensoriaInstitutionalCompetenceGuardService {

    private static final Set<String> FEDERAL_ENTITY_TOKENS = Set.of(
            "UNIAO",
            "FAZENDA_NACIONAL",
            "ADVOCACIA_GERAL_DA_UNIAO",
            "AGU",
            "DPU",
            "INSS",
            "IBAMA",
            "ICMBIO",
            "ANVISA",
            "ANS",
            "ANEEL",
            "ANATEL",
            "ANAC",
            "ANTAQ",
            "ANTT",
            "ANM",
            "ANP",
            "FUNAI",
            "INCRA",
            "DNIT",
            "PRF",
            "POLICIA_FEDERAL",
            "CEF",
            "CAIXA_ECONOMICA_FEDERAL",
            "EMPRESA_BRASILEIRA_DE_CORREIOS_E_TELEGRAFOS",
            "CORREIOS",
            "RECEITA_FEDERAL",
            "BANCO_CENTRAL",
            "BNDES",
            "UNIVERSIDADE_FEDERAL",
            "IFCE",
            "IFRN",
            "IFBA",
            "INSTITUTO_FEDERAL"
    );

    private static final Set<String> FEDERAL_TEXT_TOKENS = Set.of(
            "UNIAO",
            "AUTARQUIA_FEDERAL",
            "EMPRESA_PUBLICA_FEDERAL",
            "JUSTICA_FEDERAL",
            "JUIZADO_ESPECIAL_FEDERAL",
            "TRF",
            "TRIBUNAL_REGIONAL_FEDERAL",
            "TNU",
            "STJ",
            "STF",
            "PREVIDENCIARIO",
            "BENEFICIO_PREVIDENCIARIO",
            "LOAS",
            "BPC",
            "FGTS",
            "PIS",
            "PASEP",
            "SISBAJUD",
            "RECEITA_FEDERAL"
    );

    private final CurrentUserService currentUserService;

    public DefensoriaInstitutionalCompetenceGuardService(CurrentUserService currentUserService) {
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
    }

    public GuardDecision analyzeInitialFiling(PeticionamentoSessaoRequest request, ProceduralSubmissionBlueprintReport blueprint) {
        Usuario usuario = currentUserService.getRequired();
        return analyze(usuario, FilingSignals.fromInitialRequest(request, blueprint));
    }

    public GuardDecision analyzeDraftProtocol(LaianePeticaoInicialDraftSession draft, String tipoJustica) {
        Usuario usuario = currentUserService.getRequired();
        return analyze(usuario, FilingSignals.fromDraft(draft, tipoJustica));
    }

    public GuardDecision analyzeProcessParticipation(Processo processo) {
        Usuario usuario = currentUserService.getRequired();
        return analyze(usuario, FilingSignals.fromProcess(processo));
    }

    public void requireAllowedForInitialFiling(PeticionamentoSessaoRequest request, ProceduralSubmissionBlueprintReport blueprint) {
        analyzeInitialFiling(request, blueprint).throwIfBlocked();
    }

    public void requireAllowedForDraftProtocol(LaianePeticaoInicialDraftSession draft, String tipoJustica) {
        analyzeDraftProtocol(draft, tipoJustica).throwIfBlocked();
    }

    public void requireAllowedForProcessParticipation(Processo processo) {
        analyzeProcessParticipation(processo).throwIfBlocked();
    }

    private GuardDecision analyze(Usuario usuario, FilingSignals signals) {
        InstitutionalBranch actorBranch = InstitutionalBranch.from(usuario);
        if (!actorBranch.isDefensoria()) {
            return GuardDecision.allow(actorBranch, signals.targetSphere(), List.of(), List.of(), signals.metrics());
        }

        ArrayList<String> reasons = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        if (signals.explicitFederalJustice()) {
            reasons.add("Competência ou justiça federal identificada no contexto de protocolo.");
        }
        if (signals.explicitFederalDefendant()) {
            reasons.add("Parte ré ou ente federal identificado no polo passivo.");
        }
        if (signals.strongFederalTribunalSignal()) {
            reasons.add("Tribunal ou unidade federal detectados na rota jurisdicional.");
        }
        if (signals.federalSignalCount() > 0 && reasons.isEmpty()) {
            reasons.add("Sinais federais relevantes detectados no caso.");
        }

        if (actorBranch == InstitutionalBranch.DEFENSORIA_ESTADUAL && signals.targetSphere() == TargetSphere.FEDERAL) {
            warnings.add("Fluxo federal incompatível com a malha estadual da Defensoria.");
            warnings.add("Redirecionar o caso para a Defensoria Pública da União ou para a unidade federal competente.");
            return GuardDecision.block(actorBranch, signals.targetSphere(), reasons, warnings, signals.metrics());
        }

        if (actorBranch == InstitutionalBranch.DEFENSORIA_FEDERAL && signals.targetSphere() == TargetSphere.ESTADUAL) {
            warnings.add("Fluxo aparenta ser estadual. Revisar atribuição institucional antes do protocolo final.");
            return GuardDecision.review(actorBranch, signals.targetSphere(), reasons, warnings, signals.metrics());
        }

        if (actorBranch == InstitutionalBranch.DEFENSORIA_ESTADUAL && signals.targetSphere() == TargetSphere.INDETERMINADA && signals.federalSignalCount() > 0) {
            warnings.add("Foram detectados sinais federais, mas a competência ainda não está fechada. Revisar antes do protocolo.");
            return GuardDecision.review(actorBranch, signals.targetSphere(), reasons, warnings, signals.metrics());
        }

        return GuardDecision.allow(actorBranch, signals.targetSphere(), reasons, warnings, signals.metrics());
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = EnumText.normalizeToken(value);
        return normalized.isBlank() ? null : normalized;
    }

    private static boolean containsFederalEntityToken(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return false;
        }
        if (FEDERAL_ENTITY_TOKENS.contains(normalized)) {
            return true;
        }
        return FEDERAL_ENTITY_TOKENS.stream().anyMatch(normalized::contains);
    }

    private static boolean containsFederalTextToken(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return false;
        }
        if (FEDERAL_TEXT_TOKENS.contains(normalized)) {
            return true;
        }
        return FEDERAL_TEXT_TOKENS.stream().anyMatch(normalized::contains);
    }

    private record FilingSignals(TargetSphere targetSphere,
                                 boolean explicitFederalJustice,
                                 boolean explicitFederalDefendant,
                                 boolean strongFederalTribunalSignal,
                                 int federalSignalCount,
                                 Map<String, Object> metrics) {
        static FilingSignals fromInitialRequest(PeticionamentoSessaoRequest request,
                                                ProceduralSubmissionBlueprintReport blueprint) {
            TipoJustica tipoJustica = TipoJustica.fromString(request == null ? null : request.getTipoJustica());
            boolean justiceFederal = isFederalJustice(tipoJustica)
                    || isFederalTribunal(blueprint == null ? null : blueprint.tribunalCodigo(), blueprint == null ? null : blueprint.tribunalNome())
                    || isFederalJudicialSystem(blueprint == null ? null : blueprint.judicialSystem(), blueprint == null ? null : blueprint.tribunalCodigo(), blueprint == null ? null : blueprint.unidadeJudiciariaNome());
            boolean defendantFederal = containsFederalEntityToken(request == null ? null : request.getParteRe())
                    || containsFederalTextToken(request == null ? null : request.getTituloCaso())
                    || containsFederalTextToken(request == null ? null : request.getTextoFatosResumido())
                    || containsFederalTextToken(request == null ? null : request.getMateriaPrincipal())
                    || containsFederalCtx(request == null ? null : request.getCtx());
            boolean tribunalFederal = isFederalTribunal(blueprint == null ? null : blueprint.tribunalCodigo(), blueprint == null ? null : blueprint.tribunalNome())
                    || containsFederalTextToken(blueprint == null ? null : blueprint.unidadeJudiciariaNome())
                    || containsFederalTextToken(request == null ? null : request.getClasseProcessual())
                    || containsFederalTextToken(request == null ? null : request.getAssuntoTpu());
            int signalCount = countSignals(justiceFederal, defendantFederal, tribunalFederal);
            TargetSphere targetSphere = justiceFederal || defendantFederal || signalCount >= 2 ? TargetSphere.FEDERAL : TargetSphere.INDETERMINADA;
            LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("requestedJustice", tipoJustica == null ? null : tipoJustica.name());
            metrics.put("blueprintTribunal", blueprint == null ? null : blueprint.tribunalCodigo());
            metrics.put("federalSignalCount", signalCount);
            metrics.put("explicitFederalJustice", justiceFederal);
            metrics.put("explicitFederalDefendant", defendantFederal);
            metrics.put("strongFederalTribunalSignal", tribunalFederal);
            return new FilingSignals(targetSphere, justiceFederal, defendantFederal, tribunalFederal, signalCount, immutableMetrics(metrics));
        }

        static FilingSignals fromDraft(LaianePeticaoInicialDraftSession draft, String tipoJustica) {
            TipoJustica parsedTipoJustica = TipoJustica.fromString(tipoJustica);
            boolean justiceFederal = isFederalJustice(parsedTipoJustica)
                    || containsFederalTextToken(draft == null ? null : draft.getTituloCaso())
                    || containsFederalTextToken(draft == null ? null : draft.getClasseSugerida())
                    || containsFederalTextToken(draft == null ? null : draft.getFundamentosJson())
                    || containsFederalTextToken(draft == null ? null : draft.getFatosJson());
            boolean defendantFederal = containsFederalTextToken(draft == null ? null : draft.getTituloCaso())
                    || containsFederalTextToken(draft == null ? null : draft.getFatosJson())
                    || containsFederalTextToken(draft == null ? null : draft.getFundamentosJson());
            int signalCount = countSignals(justiceFederal, defendantFederal, false);
            TargetSphere targetSphere = justiceFederal || defendantFederal || signalCount >= 2 ? TargetSphere.FEDERAL : TargetSphere.INDETERMINADA;
            LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("requestedJustice", parsedTipoJustica == null ? null : parsedTipoJustica.name());
            metrics.put("federalSignalCount", signalCount);
            metrics.put("explicitFederalJustice", justiceFederal);
            metrics.put("explicitFederalDefendant", defendantFederal);
            return new FilingSignals(targetSphere, justiceFederal, defendantFederal, false, signalCount, immutableMetrics(metrics));
        }

        static FilingSignals fromProcess(Processo processo) {
            boolean justiceFederal = isFederalJustice(processo == null ? null : processo.getTipoJustica())
                    || containsFederalTextToken(processo == null ? null : processo.getTribunal())
                    || containsFederalTextToken(processo == null ? null : processo.getVara());
            boolean defendantFederal = containsFederalEntityToken(processo == null ? null : processo.getParteReuNome())
                    || containsFederalTextToken(processo == null ? null : processo.getPedidoPrincipal())
                    || containsFederalTextToken(processo == null ? null : processo.getAssunto())
                    || containsFederalTextToken(processo == null ? null : processo.getObjetoProcessual());
            boolean tribunalFederal = isFederalTribunal(processo == null ? null : processo.getTribunal(), processo == null ? null : processo.getVara())
                    || containsFederalTextToken(processo == null ? null : processo.getUnidadeJudiciariaCodigo());
            int signalCount = countSignals(justiceFederal, defendantFederal, tribunalFederal);
            TargetSphere targetSphere = justiceFederal || defendantFederal || signalCount >= 2 ? TargetSphere.FEDERAL : TargetSphere.ESTADUAL;
            LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("processJustice", processo == null || processo.getTipoJustica() == null ? null : processo.getTipoJustica().name());
            metrics.put("federalSignalCount", signalCount);
            metrics.put("explicitFederalJustice", justiceFederal);
            metrics.put("explicitFederalDefendant", defendantFederal);
            metrics.put("strongFederalTribunalSignal", tribunalFederal);
            return new FilingSignals(targetSphere, justiceFederal, defendantFederal, tribunalFederal, signalCount, immutableMetrics(metrics));
        }

        private static boolean containsFederalCtx(Map<String, Object> ctx) {
            if (ctx == null || ctx.isEmpty()) {
                return false;
            }
            for (Map.Entry<String, Object> entry : ctx.entrySet()) {
                if (containsFederalTextToken(entry.getKey())) {
                    return true;
                }
                Object value = entry.getValue();
                if (value instanceof String s && containsFederalTextToken(s)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isFederalJustice(TipoJustica tipoJustica) {
            return tipoJustica == TipoJustica.FEDERAL || tipoJustica == TipoJustica.MILITAR_FEDERAL || tipoJustica == TipoJustica.SUPERIOR;
        }

        private static boolean isFederalJudicialSystem(Object judicialSystem, String tribunalCodigo, String unidadeNome) {
            return containsFederalTextToken(judicialSystem == null ? null : judicialSystem.toString())
                    || isFederalTribunal(tribunalCodigo, unidadeNome);
        }

        private static boolean isFederalTribunal(String first, String second) {
            String a = normalize(first);
            String b = normalize(second);
            return (a != null && (a.startsWith("TRF") || a.startsWith("JF") || a.startsWith("JEF") || a.startsWith("STJ") || a.startsWith("STF") || a.startsWith("TNU")))
                    || (b != null && (b.contains("JUSTICA_FEDERAL") || b.contains("JUIZADO_ESPECIAL_FEDERAL") || b.contains("TRIBUNAL_REGIONAL_FEDERAL") || b.contains("TURMA_RECURSAL_FEDERAL")));
        }

        private static int countSignals(boolean... flags) {
            int count = 0;
            for (boolean flag : flags) {
                if (flag) {
                    count++;
                }
            }
            return count;
        }

        private static Map<String, Object> immutableMetrics(Map<String, Object> metrics) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            metrics.forEach((key, value) -> {
                if (key != null && value != null) {
                    out.put(key, value);
                }
            });
            return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
        }
    }

    public enum Verdict {
        ALLOW,
        REVIEW,
        BLOCK_WITH_REDIRECT
    }

    public enum InstitutionalBranch {
        OUTRO,
        DEFENSORIA_ESTADUAL,
        DEFENSORIA_FEDERAL;

        static InstitutionalBranch from(Usuario usuario) {
            TipoUsuario tipoUsuario = usuario == null ? null : usuario.getTipoUsuario();
            if (tipoUsuario == TipoUsuario.DEFENSOR_PUBLICO_FEDERAL) {
                return DEFENSORIA_FEDERAL;
            }
            if (tipoUsuario == TipoUsuario.DEFENSOR_PUBLICO) {
                return DEFENSORIA_ESTADUAL;
            }
            EnteFederativo enteFederativo = usuario == null ? null : usuario.getEnteFederativo();
            if (tipoUsuario != null && tipoUsuario.isDefensoriaPublica() && enteFederativo == EnteFederativo.UNIAO) {
                return DEFENSORIA_FEDERAL;
            }
            if (tipoUsuario != null && tipoUsuario.isDefensoriaPublica()) {
                return DEFENSORIA_ESTADUAL;
            }
            return OUTRO;
        }

        boolean isDefensoria() {
            return this == DEFENSORIA_ESTADUAL || this == DEFENSORIA_FEDERAL;
        }
    }

    public enum TargetSphere {
        ESTADUAL,
        FEDERAL,
        INDETERMINADA
    }

    public record GuardDecision(Verdict verdict,
                                InstitutionalBranch actorBranch,
                                TargetSphere targetSphere,
                                List<String> reasons,
                                List<String> warnings,
                                Map<String, Object> metrics) {
        static GuardDecision allow(InstitutionalBranch actorBranch,
                                   TargetSphere targetSphere,
                                   List<String> reasons,
                                   List<String> warnings,
                                   Map<String, Object> metrics) {
            return new GuardDecision(Verdict.ALLOW, actorBranch, targetSphere, immutableList(reasons), immutableList(warnings), metrics == null ? Map.of() : metrics);
        }

        static GuardDecision review(InstitutionalBranch actorBranch,
                                    TargetSphere targetSphere,
                                    List<String> reasons,
                                    List<String> warnings,
                                    Map<String, Object> metrics) {
            return new GuardDecision(Verdict.REVIEW, actorBranch, targetSphere, immutableList(reasons), immutableList(warnings), metrics == null ? Map.of() : metrics);
        }

        static GuardDecision block(InstitutionalBranch actorBranch,
                                   TargetSphere targetSphere,
                                   List<String> reasons,
                                   List<String> warnings,
                                   Map<String, Object> metrics) {
            return new GuardDecision(Verdict.BLOCK_WITH_REDIRECT, actorBranch, targetSphere, immutableList(reasons), immutableList(warnings), metrics == null ? Map.of() : metrics);
        }

        public boolean blocked() {
            return verdict == Verdict.BLOCK_WITH_REDIRECT;
        }

        public String redirectTarget() {
            return blocked() ? "DEFENSORIA_PUBLICA_DA_UNIAO" : null;
        }

        public String publicMessage() {
            if (blocked()) {
                return "Fluxo federal identificado para perfil de defensoria estadual. Encaminhe o caso para a DPU ou para a unidade federal competente.";
            }
            if (verdict == Verdict.REVIEW) {
                return "Há indícios de incompatibilidade institucional. Revise a atribuição da Defensoria antes do protocolo final.";
            }
            return "Compatibilidade institucional confirmada para o protocolo.";
        }

        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("verdict", verdict.name());
            out.put("actorBranch", actorBranch.name());
            out.put("targetSphere", targetSphere.name());
            if (redirectTarget() != null) {
                out.put("redirectTarget", redirectTarget());
            }
            out.put("message", publicMessage());
            out.put("reasons", reasons);
            out.put("warnings", warnings);
            out.put("metrics", metrics);
            return Collections.unmodifiableMap(out);
        }

        public void throwIfBlocked() {
            if (!blocked()) {
                return;
            }
            throw new RegraNegocioException(publicMessage());
        }

        private static List<String> immutableList(List<String> values) {
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            LinkedHashSet<String> out = new LinkedHashSet<>();
            for (String value : values) {
                if (value != null) {
                    String trimmed = value.trim();
                    if (!trimmed.isEmpty()) {
                        out.add(trimmed);
                    }
                }
            }
            return List.copyOf(out);
        }
    }
}
