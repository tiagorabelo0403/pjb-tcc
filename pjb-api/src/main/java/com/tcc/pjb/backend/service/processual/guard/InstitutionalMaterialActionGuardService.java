package com.tcc.pjb.backend.service.processual.guard;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.EnumText;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalMaterialActionGuardService {

    private static final Set<String> FEDERAL_ENTITY_TOKENS = Set.of(
            "UNIAO",
            "AGU",
            "ADVOCACIA_GERAL_DA_UNIAO",
            "FAZENDA_NACIONAL",
            "INSS",
            "IBAMA",
            "ICMBIO",
            "FUNAI",
            "INCRA",
            "DNIT",
            "PRF",
            "POLICIA_FEDERAL",
            "CAIXA_ECONOMICA_FEDERAL",
            "CEF",
            "CORREIOS",
            "RECEITA_FEDERAL",
            "BANCO_CENTRAL",
            "UNIVERSIDADE_FEDERAL",
            "INSTITUTO_FEDERAL",
            "BNDES",
            "ANVISA",
            "ANATEL",
            "ANEEL",
            "ANAC",
            "ANTAQ",
            "ANTT",
            "ANM",
            "ANP"
    );

    private static final Set<String> ESTADUAL_ENTITY_TOKENS = Set.of(
            "ESTADO",
            "FAZENDA_ESTADUAL",
            "SECRETARIA_ESTADUAL",
            "GOVERNO_DO_ESTADO",
            "POLICIA_CIVIL",
            "POLICIA_MILITAR",
            "DETRAN",
            "DEFENSORIA_PUBLICA_DO_ESTADO",
            "MINISTERIO_PUBLICO_DO_ESTADO",
            "TRIBUNAL_DE_JUSTICA",
            "TJ",
            "FAZENDA_PUBLICA_ESTADUAL"
    );

    private static final Set<String> MUNICIPAL_ENTITY_TOKENS = Set.of(
            "MUNICIPIO",
            "PREFEITURA",
            "PREFEITURA_MUNICIPAL",
            "CAMARA_MUNICIPAL",
            "SECRETARIA_MUNICIPAL",
            "AUTARQUIA_MUNICIPAL",
            "SERVICO_AUTONOMO_DE_AGUA",
            "FAZENDA_PUBLICA_MUNICIPAL"
    );

    private static final Set<String> INVESTIGATIVE_TOKENS = Set.of(
            "INQUERITO",
            "INVESTIGACAO",
            "BOLETIM_DE_OCORRENCIA",
            "BOLETIM_OCCORRENCIA",
            "NOTICIA_CRIME",
            "PROCEDIMENTO_INVESTIGATORIO",
            "PIC",
            "DILIGENCIA",
            "REPRESENTACAO_POLICIAL",
            "IPM"
    );

    private final CurrentUserService currentUserService;
    private final DefensoriaInstitutionalCompetenceGuardService defensoriaGuardService;

    public InstitutionalMaterialActionGuardService(CurrentUserService currentUserService,
                                                   DefensoriaInstitutionalCompetenceGuardService defensoriaGuardService) {
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.defensoriaGuardService = Objects.requireNonNull(defensoriaGuardService, "defensoriaGuardService");
    }

    public GuardDecision analyzeProcessAction(Processo processo, MaterialAction action) {
        Usuario usuario = currentUserService.getRequired();
        ActorBranch actorBranch = ActorBranch.from(usuario);
        if (actorBranch.isDefensoria()) {
            DefensoriaInstitutionalCompetenceGuardService.GuardDecision defensoriaDecision = defensoriaGuardService.analyzeProcessParticipation(processo);
            return adaptDefensoriaDecision(actorBranch, action, MaterialScope.fromProcess(processo), defensoriaDecision);
        }
        return analyze(usuario, action, MaterialScope.fromProcess(processo));
    }

    public GuardDecision analyzeCatalogAction(MaterialAction action, CatalogActionContext context) {
        Usuario usuario = currentUserService.getRequired();
        return analyze(usuario, action, MaterialScope.fromCatalogContext(context));
    }

    public void requireAllowedForProcessAction(Processo processo, MaterialAction action) {
        analyzeProcessAction(processo, action).throwIfBlocked();
    }

    public void requireAllowedForCatalogAction(MaterialAction action, CatalogActionContext context) {
        analyzeCatalogAction(action, context).throwIfBlocked();
    }

    private GuardDecision analyze(Usuario usuario, MaterialAction action, MaterialScope scope) {
        ActorBranch actorBranch = ActorBranch.from(usuario);
        if (actorBranch == ActorBranch.OUTRO) {
            return GuardDecision.allow(actorBranch, action, scope.targetSphere(), List.of(), List.of(), scope.metrics());
        }
        if (actorBranch.isDefensoria()) {
            return GuardDecision.review(actorBranch, action, scope.targetSphere(), List.of("Atuação de defensoria deve ser validada no fluxo processual correspondente."), List.of(), scope.metrics());
        }

        ArrayList<String> reasons = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();

        if (scope.justice() != null) {
            reasons.add("Justiça identificada: " + scope.justice().name());
        }
        if (scope.ramo() != null) {
            reasons.add("Ramo identificado: " + scope.ramo().name());
        }
        if (scope.rito() != null) {
            reasons.add("Rito identificado: " + scope.rito().name());
        }
        if (scope.targetSphere() != TargetSphere.INDETERMINADA) {
            reasons.add("Esfera institucional provável: " + scope.targetSphere().name());
        }

        return switch (actorBranch) {
            case DELEGACIA_ESTADUAL -> decideDelegaciaEstadual(action, scope, actorBranch, reasons, warnings);
            case POLICIA_FEDERAL -> decidePoliciaFederal(action, scope, actorBranch, reasons, warnings);
            case MINISTERIO_PUBLICO_ESTADUAL -> decideMinisterioPublicoEstadual(action, scope, actorBranch, reasons, warnings);
            case MINISTERIO_PUBLICO_ELEITORAL -> decideMinisterioPublicoEleitoral(action, scope, actorBranch, reasons, warnings);
            case MINISTERIO_PUBLICO_TRABALHISTA -> decideMinisterioPublicoTrabalhista(action, scope, actorBranch, reasons, warnings);
            case MINISTERIO_PUBLICO_FEDERAL -> decideMinisterioPublicoFederal(action, scope, actorBranch, reasons, warnings);
            case PROCURADORIA_MUNICIPAL -> decideProcuradoriaMunicipal(action, scope, actorBranch, reasons, warnings);
            case PROCURADORIA_ESTADUAL -> decideProcuradoriaEstadual(action, scope, actorBranch, reasons, warnings);
            case PROCURADORIA_FEDERAL -> decideProcuradoriaFederal(action, scope, actorBranch, reasons, warnings);
            case OUTRO, DEFENSORIA_ESTADUAL, DEFENSORIA_FEDERAL -> GuardDecision.allow(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        };
    }

    private GuardDecision adaptDefensoriaDecision(ActorBranch actorBranch,
                                                   MaterialAction action,
                                                   MaterialScope scope,
                                                   DefensoriaInstitutionalCompetenceGuardService.GuardDecision defensoriaDecision) {
        if (defensoriaDecision == null) {
            return GuardDecision.allow(actorBranch, action, scope.targetSphere(), List.of(), List.of(), scope.metrics());
        }
        return switch (defensoriaDecision.verdict()) {
            case ALLOW -> GuardDecision.allow(actorBranch, action, scope.targetSphere(), defensoriaDecision.reasons(), defensoriaDecision.warnings(), mergeMetrics(scope.metrics(), defensoriaDecision.metrics()));
            case REVIEW -> GuardDecision.review(actorBranch, action, scope.targetSphere(), defensoriaDecision.reasons(), defensoriaDecision.warnings(), mergeMetrics(scope.metrics(), defensoriaDecision.metrics()));
            case BLOCK_WITH_REDIRECT -> GuardDecision.block(actorBranch, action, scope.targetSphere(), defensoriaDecision.reasons(), defensoriaDecision.warnings(), mergeMetrics(scope.metrics(), defensoriaDecision.metrics()));
        };
    }

    private GuardDecision decideDelegaciaEstadual(MaterialAction action,
                                                  MaterialScope scope,
                                                  ActorBranch actorBranch,
                                                  List<String> reasons,
                                                  List<String> warnings) {
        if (!action.isDelegaciaAction()) {
            warnings.add("Ato não pertence à malha material típica da delegacia.");
            return GuardDecision.review(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (!scope.investigative() && scope.ramo() != RamoDireito.PENAL && scope.ramo() != RamoDireito.PROCESSUAL_PENAL && scope.ramo() != RamoDireito.MILITAR) {
            warnings.add("A atuação policial foi solicitada fora de contexto investigativo ou criminal.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (scope.targetSphere() == TargetSphere.FEDERAL || scope.justice() == TipoJustica.FEDERAL) {
            warnings.add("Fluxo federal detectado. Redirecionar para a Polícia Federal ou unidade federal competente.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        return GuardDecision.allow(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
    }

    private GuardDecision decidePoliciaFederal(MaterialAction action,
                                               MaterialScope scope,
                                               ActorBranch actorBranch,
                                               List<String> reasons,
                                               List<String> warnings) {
        if (!action.isDelegaciaAction()) {
            warnings.add("Ato não pertence à malha material típica da Polícia Federal.");
            return GuardDecision.review(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (!scope.investigative()) {
            warnings.add("A Polícia Federal deve atuar em trilha investigativa ou cautelar correlata.");
            return GuardDecision.review(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (scope.targetSphere() == TargetSphere.ESTADUAL && scope.justice() == TipoJustica.ESTADUAL && !scope.hasFederalSignal()) {
            warnings.add("Fluxo estadual puro detectado. Redirecionar para a Polícia Civil local.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        return GuardDecision.allow(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
    }

    private GuardDecision decideMinisterioPublicoEstadual(MaterialAction action,
                                                          MaterialScope scope,
                                                          ActorBranch actorBranch,
                                                          List<String> reasons,
                                                          List<String> warnings) {
        if (!action.isMinisterioPublicoAction()) {
            return GuardDecision.review(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (scope.justice() == TipoJustica.ELEITORAL || scope.isElectoral()) {
            warnings.add("Fluxo eleitoral detectado. Encaminhar para a malha eleitoral do Ministério Público.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (scope.justice() == TipoJustica.TRABALHO || scope.isLabor()) {
            warnings.add("Fluxo trabalhista detectado. Encaminhar para o Ministério Público do Trabalho.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (scope.targetSphere() == TargetSphere.FEDERAL && (scope.justice() == TipoJustica.FEDERAL || scope.hasFederalSignal())) {
            warnings.add("Fluxo federal detectado. Encaminhar para o Ministério Público Federal.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        return GuardDecision.allow(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
    }

    private GuardDecision decideMinisterioPublicoEleitoral(MaterialAction action,
                                                           MaterialScope scope,
                                                           ActorBranch actorBranch,
                                                           List<String> reasons,
                                                           List<String> warnings) {
        if (!action.isMinisterioPublicoAction()) {
            return GuardDecision.review(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (!scope.isElectoral() && scope.justice() != TipoJustica.ELEITORAL) {
            warnings.add("A malha eleitoral do Ministério Público só pode atuar em feitos eleitorais.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        return GuardDecision.allow(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
    }

    private GuardDecision decideMinisterioPublicoTrabalhista(MaterialAction action,
                                                             MaterialScope scope,
                                                             ActorBranch actorBranch,
                                                             List<String> reasons,
                                                             List<String> warnings) {
        if (!action.isMinisterioPublicoAction()) {
            return GuardDecision.review(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (!scope.isLabor() && scope.justice() != TipoJustica.TRABALHO) {
            warnings.add("A malha trabalhista do Ministério Público só pode atuar em feitos trabalhistas.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        return GuardDecision.allow(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
    }

    private GuardDecision decideMinisterioPublicoFederal(MaterialAction action,
                                                         MaterialScope scope,
                                                         ActorBranch actorBranch,
                                                         List<String> reasons,
                                                         List<String> warnings) {
        if (!action.isMinisterioPublicoAction()) {
            return GuardDecision.review(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if ((scope.targetSphere() == TargetSphere.ESTADUAL || scope.targetSphere() == TargetSphere.MUNICIPAL)
                && scope.justice() == TipoJustica.ESTADUAL
                && !scope.hasFederalSignal()) {
            warnings.add("Fluxo estadual puro detectado. Encaminhar para o Ministério Público estadual competente.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (scope.justice() == TipoJustica.TRABALHO || scope.isLabor()) {
            warnings.add("Fluxo trabalhista detectado. Encaminhar para o Ministério Público do Trabalho.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        return GuardDecision.allow(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
    }

    private GuardDecision decideProcuradoriaMunicipal(MaterialAction action,
                                                      MaterialScope scope,
                                                      ActorBranch actorBranch,
                                                      List<String> reasons,
                                                      List<String> warnings) {
        if (!action.isProcuradoriaAction()) {
            return GuardDecision.review(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (scope.targetSphere() == TargetSphere.FEDERAL || scope.targetSphere() == TargetSphere.ESTADUAL) {
            warnings.add("A procuradoria municipal não deve atuar fora da esfera do Município correspondente.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        return GuardDecision.allow(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
    }

    private GuardDecision decideProcuradoriaEstadual(MaterialAction action,
                                                     MaterialScope scope,
                                                     ActorBranch actorBranch,
                                                     List<String> reasons,
                                                     List<String> warnings) {
        if (!action.isProcuradoriaAction()) {
            return GuardDecision.review(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (scope.targetSphere() == TargetSphere.FEDERAL || scope.targetSphere() == TargetSphere.MUNICIPAL) {
            warnings.add("A procuradoria estadual não deve atuar fora da esfera do Estado correspondente.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        return GuardDecision.allow(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
    }

    private GuardDecision decideProcuradoriaFederal(MaterialAction action,
                                                    MaterialScope scope,
                                                    ActorBranch actorBranch,
                                                    List<String> reasons,
                                                    List<String> warnings) {
        if (!action.isProcuradoriaAction()) {
            return GuardDecision.review(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        if (scope.targetSphere() == TargetSphere.ESTADUAL || scope.targetSphere() == TargetSphere.MUNICIPAL) {
            warnings.add("A advocacia pública federal deve atuar em malha da União, autarquia ou fundação federal.");
            return GuardDecision.block(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
        }
        return GuardDecision.allow(actorBranch, action, scope.targetSphere(), reasons, warnings, scope.metrics());
    }

    private static Map<String, Object> mergeMetrics(Map<String, Object> left, Map<String, Object> right) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (left != null) {
            out.putAll(left);
        }
        if (right != null) {
            out.putAll(right);
        }
        return out;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = EnumText.normalizeToken(value);
        return normalized.isBlank() ? null : normalized;
    }

    private static int countSignals(boolean... values) {
        int total = 0;
        if (values == null) {
            return 0;
        }
        for (boolean value : values) {
            if (value) {
                total++;
            }
        }
        return total;
    }

    private static boolean containsAnyToken(String value, Set<String> tokens) {
        String normalized = normalize(value);
        if (normalized == null || tokens == null || tokens.isEmpty()) {
            return false;
        }
        if (tokens.contains(normalized)) {
            return true;
        }
        return tokens.stream().anyMatch(normalized::contains);
    }

    private record MaterialScope(TargetSphere targetSphere,
                                 TipoJustica justice,
                                 RamoDireito ramo,
                                 RitoProcessual rito,
                                 boolean investigative,
                                 boolean labor,
                                 boolean electoral,
                                 boolean military,
                                 boolean federalSignal,
                                 Map<String, Object> metrics) {

        static MaterialScope fromProcess(Processo processo) {
            boolean investigative = isInvestigative(processo);
            boolean labor = isLabor(processo);
            boolean electoral = isElectoral(processo);
            boolean military = isMilitary(processo);
            boolean federalSignal = hasFederalSignal(processo);
            boolean estadualSignal = hasStateSignal(processo);
            boolean municipalSignal = hasMunicipalSignal(processo);
            TargetSphere targetSphere = resolveTargetSphere(processo == null ? null : processo.getTipoJustica(), federalSignal, estadualSignal, municipalSignal);
            LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("processoId", processo == null ? null : processo.getId());
            metrics.put("tipoJustica", processo == null || processo.getTipoJustica() == null ? null : processo.getTipoJustica().name());
            metrics.put("ramo", processo == null || processo.getRamo() == null ? null : processo.getRamo().name());
            metrics.put("rito", processo == null || processo.getRito() == null ? null : processo.getRito().name());
            metrics.put("investigative", investigative);
            metrics.put("labor", labor);
            metrics.put("electoral", electoral);
            metrics.put("military", military);
            metrics.put("federalSignal", federalSignal);
            metrics.put("municipalSignal", municipalSignal);
            metrics.put("estadualSignal", estadualSignal);
            return new MaterialScope(targetSphere,
                    processo == null ? null : processo.getTipoJustica(),
                    processo == null ? null : processo.getRamo(),
                    processo == null ? null : processo.getRito(),
                    investigative,
                    labor,
                    electoral,
                    military,
                    federalSignal,
                    metrics);
        }

        static MaterialScope fromCatalogContext(CatalogActionContext context) {
            CatalogActionContext safe = context == null ? CatalogActionContext.empty() : context;
            boolean federalSignal = safe.targetSphere() == TargetSphere.FEDERAL || containsAnyToken(safe.title(), FEDERAL_ENTITY_TOKENS) || containsAnyToken(safe.description(), FEDERAL_ENTITY_TOKENS);
            boolean estadualSignal = safe.targetSphere() == TargetSphere.ESTADUAL || containsAnyToken(safe.title(), ESTADUAL_ENTITY_TOKENS) || containsAnyToken(safe.description(), ESTADUAL_ENTITY_TOKENS);
            boolean municipalSignal = safe.targetSphere() == TargetSphere.MUNICIPAL || containsAnyToken(safe.title(), MUNICIPAL_ENTITY_TOKENS) || containsAnyToken(safe.description(), MUNICIPAL_ENTITY_TOKENS);
            LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("catalogJustice", safe.justice() == null ? null : safe.justice().name());
            metrics.put("catalogRamo", safe.ramo() == null ? null : safe.ramo().name());
            metrics.put("catalogRito", safe.rito() == null ? null : safe.rito().name());
            metrics.put("investigative", safe.investigative());
            metrics.put("title", safe.title());
            metrics.put("targetSphere", safe.targetSphere().name());
            return new MaterialScope(
                    safe.targetSphere() == TargetSphere.INDETERMINADA ? resolveTargetSphere(safe.justice(), federalSignal, estadualSignal, municipalSignal) : safe.targetSphere(),
                    safe.justice(),
                    safe.ramo(),
                    safe.rito(),
                    safe.investigative(),
                    safe.justice() == TipoJustica.TRABALHO || isLabor(safe.ramo(), safe.rito(), safe.title(), safe.description()),
                    safe.justice() == TipoJustica.ELEITORAL || isElectoral(safe.ramo(), safe.rito(), safe.title(), safe.description()),
                    safe.justice() == TipoJustica.MILITAR_ESTADUAL || safe.justice() == TipoJustica.MILITAR_FEDERAL || isMilitary(safe.ramo(), safe.rito(), safe.title(), safe.description()),
                    federalSignal,
                    metrics
            );
        }

        private static TargetSphere resolveTargetSphere(TipoJustica justice,
                                                        boolean federalSignal,
                                                        boolean estadualSignal,
                                                        boolean municipalSignal) {
            if (justice == TipoJustica.FEDERAL || justice == TipoJustica.SUPERIOR || justice == TipoJustica.MILITAR_FEDERAL || federalSignal) {
                return TargetSphere.FEDERAL;
            }
            if (justice == TipoJustica.ELEITORAL || justice == TipoJustica.TRABALHO) {
                return TargetSphere.FEDERAL;
            }
            if (municipalSignal) {
                return TargetSphere.MUNICIPAL;
            }
            if (justice == TipoJustica.ESTADUAL || justice == TipoJustica.MILITAR_ESTADUAL || estadualSignal) {
                return TargetSphere.ESTADUAL;
            }
            return TargetSphere.INDETERMINADA;
        }

        boolean hasFederalSignal() {
            return federalSignal;
        }

        boolean isLabor() {
            return labor;
        }

        boolean isElectoral() {
            return electoral;
        }

        @SuppressWarnings("unused")
        boolean isMilitary() {
            return military;
        }

        private static boolean isInvestigative(Processo processo) {
            return isInvestigative(
                    processo == null ? null : processo.getRamo(),
                    processo == null ? null : processo.getRito(),
                    processo == null ? null : processo.getClasseProcessual(),
                    processo == null ? null : processo.getAssunto(),
                    processo == null ? null : processo.getObjetoProcessual(),
                    processo == null ? null : processo.getPedidoPrincipal(),
                    processo == null ? null : processo.getResumoIA(),
                    processo == null ? null : processo.getTribunal(),
                    processo == null ? null : processo.getVara()
            );
        }

        private static boolean isInvestigative(RamoDireito ramo,
                                               RitoProcessual rito,
                                               String... texts) {
            if (ramo == RamoDireito.PENAL || ramo == RamoDireito.PROCESSUAL_PENAL || ramo == RamoDireito.MILITAR || ramo == RamoDireito.EXECUCAO_PENAL) {
                return true;
            }
            if (rito != null && (rito.isPenal() || rito.isMilitar())) {
                return true;
            }
            if (texts == null) {
                return false;
            }
            for (String text : texts) {
                if (containsAnyToken(text, INVESTIGATIVE_TOKENS)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isLabor(Processo processo) {
            return isLabor(
                    processo == null ? null : processo.getRamo(),
                    processo == null ? null : processo.getRito(),
                    processo == null ? null : processo.getClasseProcessual(),
                    processo == null ? null : processo.getAssunto(),
                    processo == null ? null : processo.getObjetoProcessual()
            );
        }

        private static boolean isLabor(RamoDireito ramo,
                                       RitoProcessual rito,
                                       String... texts) {
            if (ramo == RamoDireito.TRABALHISTA || ramo == RamoDireito.PROCESSUAL_TRABALHISTA) {
                return true;
            }
            if (rito != null && rito.isTrabalhista()) {
                return true;
            }
            if (texts == null) {
                return false;
            }
            for (String text : texts) {
                String normalized = normalize(text);
                if (normalized != null && (normalized.contains("TRABALHO") || normalized.contains("TRABALHISTA") || normalized.contains("TRT") || normalized.contains("VARA_DO_TRABALHO"))) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isElectoral(Processo processo) {
            return isElectoral(
                    processo == null ? null : processo.getRamo(),
                    processo == null ? null : processo.getRito(),
                    processo == null ? null : processo.getClasseProcessual(),
                    processo == null ? null : processo.getAssunto(),
                    processo == null ? null : processo.getObjetoProcessual(),
                    processo == null ? null : processo.getTribunal(),
                    processo == null ? null : processo.getVara()
            );
        }

        private static boolean isElectoral(RamoDireito ramo,
                                           RitoProcessual rito,
                                           String... texts) {
            if (ramo == RamoDireito.ELEITORAL || ramo == RamoDireito.PROCESSUAL_ELEITORAL) {
                return true;
            }
            if (rito != null && rito.isEleitoral()) {
                return true;
            }
            if (texts == null) {
                return false;
            }
            for (String text : texts) {
                String normalized = normalize(text);
                if (normalized != null && (normalized.contains("ELEITORAL") || normalized.contains("TRE") || normalized.contains("TSE") || normalized.contains("ZONA_ELEITORAL"))) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isMilitary(Processo processo) {
            return isMilitary(
                    processo == null ? null : processo.getRamo(),
                    processo == null ? null : processo.getRito(),
                    processo == null ? null : processo.getClasseProcessual(),
                    processo == null ? null : processo.getAssunto(),
                    processo == null ? null : processo.getObjetoProcessual()
            );
        }

        private static boolean isMilitary(RamoDireito ramo,
                                          RitoProcessual rito,
                                          String... texts) {
            if (ramo == RamoDireito.MILITAR) {
                return true;
            }
            if (rito != null && rito.isMilitar()) {
                return true;
            }
            if (texts == null) {
                return false;
            }
            for (String text : texts) {
                String normalized = normalize(text);
                if (normalized != null && (normalized.contains("MILITAR") || normalized.contains("IPM") || normalized.contains("CPM") || normalized.contains("JUSTICA_MILITAR"))) {
                    return true;
                }
            }
            return false;
        }

        private static boolean hasFederalSignal(Processo processo) {
            return containsAnyToken(processo == null ? null : processo.getParteReuNome(), FEDERAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getParteAutoraNome(), FEDERAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getTribunal(), FEDERAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getVara(), FEDERAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getAssunto(), FEDERAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getObjetoProcessual(), FEDERAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getPedidoPrincipal(), FEDERAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getResumoIA(), FEDERAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getUnidadeJudiciariaCodigo(), FEDERAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getClasseProcessual(), FEDERAL_ENTITY_TOKENS);
        }

        private static boolean hasStateSignal(Processo processo) {
            return containsAnyToken(processo == null ? null : processo.getParteReuNome(), ESTADUAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getParteAutoraNome(), ESTADUAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getTribunal(), ESTADUAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getVara(), ESTADUAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getAssunto(), ESTADUAL_ENTITY_TOKENS);
        }

        private static boolean hasMunicipalSignal(Processo processo) {
            return containsAnyToken(processo == null ? null : processo.getParteReuNome(), MUNICIPAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getParteAutoraNome(), MUNICIPAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getObjetoProcessual(), MUNICIPAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getPedidoPrincipal(), MUNICIPAL_ENTITY_TOKENS)
                    || containsAnyToken(processo == null ? null : processo.getAssunto(), MUNICIPAL_ENTITY_TOKENS);
        }
    }

    public record CatalogActionContext(TargetSphere targetSphere,
                                       TipoJustica justice,
                                       RamoDireito ramo,
                                       RitoProcessual rito,
                                       String title,
                                       String description,
                                       boolean investigative) {
        public static CatalogActionContext empty() {
            return new CatalogActionContext(TargetSphere.INDETERMINADA, null, null, null, null, null, false);
        }
    }

    public enum MaterialAction {
        DELEGADO_DILIGENCIA,
        DELEGADO_PECA_INQUERITO,
        MINISTERIO_PUBLICO_MANIFESTACAO,
        MINISTERIO_PUBLICO_PARECER,
        MINISTERIO_PUBLICO_RECURSO,
        MINISTERIO_PUBLICO_REQUISICAO_DILIGENCIA,
        DEFENSORIA_PETICAO,
        DEFENSORIA_RECURSO,
        DEFENSORIA_GRATUIDADE,
        PROCURADORIA_CONTESTACAO,
        PROCURADORIA_PARECER,
        PROCURADORIA_RECURSO,
        PROCURADORIA_EXECUCAO_FISCAL;

        boolean isDelegaciaAction() {
            return this == DELEGADO_DILIGENCIA || this == DELEGADO_PECA_INQUERITO;
        }

        boolean isMinisterioPublicoAction() {
            return this == MINISTERIO_PUBLICO_MANIFESTACAO
                    || this == MINISTERIO_PUBLICO_PARECER
                    || this == MINISTERIO_PUBLICO_RECURSO
                    || this == MINISTERIO_PUBLICO_REQUISICAO_DILIGENCIA;
        }

        boolean isProcuradoriaAction() {
            return this == PROCURADORIA_CONTESTACAO
                    || this == PROCURADORIA_PARECER
                    || this == PROCURADORIA_RECURSO
                    || this == PROCURADORIA_EXECUCAO_FISCAL;
        }
    }

    public enum Verdict {
        ALLOW,
        REVIEW,
        BLOCK,
        BLOCK_WITH_REDIRECT
    }

    public enum TargetSphere {
        FEDERAL,
        ESTADUAL,
        MUNICIPAL,
        INDETERMINADA
    }

    public enum ActorBranch {
        DELEGACIA_ESTADUAL,
        POLICIA_FEDERAL,
        MINISTERIO_PUBLICO_ESTADUAL,
        MINISTERIO_PUBLICO_ELEITORAL,
        MINISTERIO_PUBLICO_TRABALHISTA,
        MINISTERIO_PUBLICO_FEDERAL,
        DEFENSORIA_ESTADUAL,
        DEFENSORIA_FEDERAL,
        PROCURADORIA_MUNICIPAL,
        PROCURADORIA_ESTADUAL,
        PROCURADORIA_FEDERAL,
        OUTRO;

        static ActorBranch from(Usuario usuario) {
            if (usuario == null || usuario.getTipoUsuario() == null) {
                return OUTRO;
            }
            TipoUsuario tipo = usuario.getTipoUsuario();
            return switch (tipo) {
                case DELEGADO_POLICIA -> DELEGACIA_ESTADUAL;
                case DELEGADO_POLICIA_FEDERAL -> POLICIA_FEDERAL;
                case MEMBRO_MINISTERIO_PUBLICO -> MINISTERIO_PUBLICO_ESTADUAL;
                case PROMOTOR_ELEITORAL -> MINISTERIO_PUBLICO_ELEITORAL;
                case PROMOTOR_TRABALHISTA -> MINISTERIO_PUBLICO_TRABALHISTA;
                case PROCURADOR_GERAL_REPUBLICA -> MINISTERIO_PUBLICO_FEDERAL;
                case DEFENSOR_PUBLICO -> DEFENSORIA_ESTADUAL;
                case DEFENSOR_PUBLICO_FEDERAL -> DEFENSORIA_FEDERAL;
                case PROCURADORIA_MUNICIPAL -> PROCURADORIA_MUNICIPAL;
                case PROCURADORIA_ESTADUAL -> PROCURADORIA_ESTADUAL;
                case PROCURADORIA_FEDERAL, PROCURADOR -> resolvePublicProcuracy(usuario);
                default -> OUTRO;
            };
        }

        private static ActorBranch resolvePublicProcuracy(Usuario usuario) {
            EnteFederativo ente = usuario.getEnteFederativo();
            if (ente == EnteFederativo.MUNICIPIO) {
                return PROCURADORIA_MUNICIPAL;
            }
            if (ente == EnteFederativo.ESTADO) {
                return PROCURADORIA_ESTADUAL;
            }
            if (ente == EnteFederativo.UNIAO || (usuario.getTipoUsuario() != null && usuario.getTipoUsuario().name().contains("FEDERAL"))) {
                return PROCURADORIA_FEDERAL;
            }
            return PROCURADORIA_ESTADUAL;
        }

        boolean isDefensoria() {
            return this == DEFENSORIA_ESTADUAL || this == DEFENSORIA_FEDERAL;
        }
    }

    public record GuardDecision(ActorBranch actorBranch,
                                MaterialAction action,
                                Verdict verdict,
                                TargetSphere targetSphere,
                                List<String> reasons,
                                List<String> warnings,
                                Map<String, Object> metrics) {
        public static GuardDecision allow(ActorBranch actorBranch,
                                          MaterialAction action,
                                          TargetSphere targetSphere,
                                          List<String> reasons,
                                          List<String> warnings,
                                          Map<String, Object> metrics) {
            return new GuardDecision(actorBranch, action, Verdict.ALLOW, targetSphere, immutableList(reasons), immutableList(warnings), immutableMetrics(metrics));
        }

        public static GuardDecision review(ActorBranch actorBranch,
                                           MaterialAction action,
                                           TargetSphere targetSphere,
                                           List<String> reasons,
                                           List<String> warnings,
                                           Map<String, Object> metrics) {
            return new GuardDecision(actorBranch, action, Verdict.REVIEW, targetSphere, immutableList(reasons), immutableList(warnings), immutableMetrics(metrics));
        }

        public static GuardDecision block(ActorBranch actorBranch,
                                          MaterialAction action,
                                          TargetSphere targetSphere,
                                          List<String> reasons,
                                          List<String> warnings,
                                          Map<String, Object> metrics) {
            return new GuardDecision(actorBranch, action, Verdict.BLOCK_WITH_REDIRECT, targetSphere, immutableList(reasons), immutableList(warnings), immutableMetrics(metrics));
        }

        public void throwIfBlocked() {
            if (verdict == Verdict.BLOCK || verdict == Verdict.BLOCK_WITH_REDIRECT) {
                String reason = warnings.isEmpty() ? "A atuação institucional solicitada não é compatível com a malha material deste ator." : warnings.get(0);
                throw new RegraNegocioException(reason);
            }
        }
    }

    private static List<String> immutableList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.trim());
            }
        }
        return List.copyOf(out);
    }

    private static Map<String, Object> immutableMetrics(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            out.put(entry.getKey(), entry.getValue());
        }
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(out));
    }
}
