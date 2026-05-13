package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Objects;

public final class NationalRecursalMeshEngine {

    private final RecursalRuleCatalog ruleCatalog;
    private final NationalRecursalStateMachine stateMachine;
    private final RecursalCompatibilityMatrix compatibilityMatrix;
    private final RecursalRouteIntegrityValidator routeIntegrityValidator;

    public NationalRecursalMeshEngine() {
        this(
                RecursalRuleCatalog.defaultCatalog(),
                new NationalRecursalStateMachine(),
                new RecursalCompatibilityMatrix(),
                new RecursalRouteIntegrityValidator()
        );
    }

    public NationalRecursalMeshEngine(
            RecursalRuleCatalog ruleCatalog,
            NationalRecursalStateMachine stateMachine,
            RecursalCompatibilityMatrix compatibilityMatrix,
            RecursalRouteIntegrityValidator routeIntegrityValidator) {
        this.ruleCatalog = Objects.requireNonNull(ruleCatalog, "ruleCatalog");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.compatibilityMatrix = Objects.requireNonNull(compatibilityMatrix, "compatibilityMatrix");
        this.routeIntegrityValidator = Objects.requireNonNull(routeIntegrityValidator, "routeIntegrityValidator");
    }

    public RecursalPlanningResult plan(RecursalCaseContext context, RecursalSpecies species, String recursoId) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(species, "species");
        String id = recursoId == null || recursoId.isBlank() ? context.numeroProcesso() + ':' + species.code() : recursoId;
        RecursalRoutePlan routePlan = ruleCatalog.route(context, species);
        compatibilityMatrix.validate(context, species, routePlan);
        routeIntegrityValidator.validate(context, species, routePlan);
        RecursalStateSnapshot draft = stateMachine.initialSnapshot(id, context, species, routePlan);
        return new RecursalPlanningResult(
                species,
                context,
                routePlan,
                draft,
                stateMachine.availableEvents(draft, species, routePlan)
        );
    }

    public RecursalTransitionResult transition(RecursalTransitionCommand command) {
        Objects.requireNonNull(command, "command");
        RecursalRoutePlan routePlan = ruleCatalog.route(command.context(), command.species());
        compatibilityMatrix.validate(command.context(), command.species(), routePlan);
        routeIntegrityValidator.validate(command.context(), command.species(), routePlan);
        RecursalStateSnapshot nextSnapshot = stateMachine.transition(command, routePlan);
        return new RecursalTransitionResult(
                command.snapshot(),
                nextSnapshot,
                command.species(),
                routePlan,
                stateMachine.availableEvents(nextSnapshot, command.species(), routePlan)
        );
    }

    public java.util.Set<RecursalTransitionEvent> planForSnapshot(RecursalStateSnapshot snapshot, RecursalSpecies species, RecursalRoutePlan routePlan) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(species, "species");
        Objects.requireNonNull(routePlan, "routePlan");
        return stateMachine.availableEvents(snapshot, species, routePlan);
    }

    public RecursalStateSnapshot newDraft(String recursoId, RecursalCaseContext context) {
        return RecursalStateSnapshot.newDraft(recursoId, context);
    }
}
