package com.tcc.pjb.backend.core.kernel.recursal.template.impl;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.ProceedingKeyFactory;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalFactType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalRelationType;
import com.tcc.pjb.backend.core.kernel.recursal.SecrecyPolicyEngine;
import com.tcc.pjb.backend.core.kernel.recursal.context.ProceduralContext;
import com.tcc.pjb.backend.core.kernel.recursal.model.*;
import com.tcc.pjb.backend.core.kernel.recursal.plan.*;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRitePlatformPolicy;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRitePlatformPolicy.RecursalPlatformProfile;
import com.tcc.pjb.backend.core.kernel.recursal.template.RecursalTemplate;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;

@Component
public final class DefaultRecursalTemplate implements RecursalTemplate {

    private final SecrecyPolicyEngine secrecy = SecrecyPolicyEngine.standard();

    @Override
    public boolean supports(ProceduralContext ctx) {
        return true;
    }

    @Override
    public int priority() {
        return -100;
    }

    @Override
    public RecursalPlan plan(CanonicalFact fact, GraphSnapshot snapshot, ProceduralContext ctx) {
        if (fact == null || snapshot == null || ctx == null) {
            return RecursalPlan.builder().note("fact/snapshot/ctx ausente").build();
        }

        if (fact.type() == RecursalFactType.APPEAL_FILED && fact.payload() instanceof AppealFiledPayload p) {
            return planAppealFiled(fact, p, snapshot, ctx);
        }

        if (fact.type() == RecursalFactType.AUTUATED_IN_TARGET && fact.payload() instanceof AutuationPayload p) {
            return planAutuation(fact, p, snapshot, ctx);
        }

        if (fact.type() == RecursalFactType.CONFIDENTIALITY_CHANGED && fact.payload() instanceof SecrecyChangedPayload p) {
            return planSecrecyChange(fact, p, snapshot, ctx);
        }

        return RecursalPlan.builder()
                .note("Sem regra no DefaultRecursalTemplate para: " + fact.type())
                .build();
    }

    private RecursalPlan planAppealFiled(CanonicalFact fact,
                                        AppealFiledPayload payload,
                                        GraphSnapshot snapshot,
                                        ProceduralContext ctx) {

        LegalAppealType appeal = payload.appealType();
        String rootKey = snapshot.anchorProceedingKey();
        if (rootKey == null || rootKey.isBlank()) {
            return RecursalPlan.builder().note("rootProceedingKey ausente").build();
        }


        if (appeal == LegalAppealType.EMBARGOS_DECLARACAO || appeal == LegalAppealType.AGRAVO_INTERNO || appeal == LegalAppealType.AGRAVO_REGIMENTAL || appeal == LegalAppealType.CORREICAO_PARCIAL) {
            return RecursalPlan.builder()
                    .addEdge(new EdgeUpsert(rootKey, rootKey, RecursalRelationType.INCIDENT_WITHIN, appeal))
                    .addWorkItem(new WorkItemDirective(
                            "RECURSAL/INCIDENTES",
                            "Incidente recursal: " + appeal.name(),
                            payload.notes(),
                            null
                    ))
                    .note("Incidente dentro dos autos: " + appeal)
                    .build();
        }

        if (appeal == LegalAppealType.EMBARGOS_EXECUCAO
                || appeal == LegalAppealType.EMBARGOS_EXECUCAO_FISCAL
                || appeal == LegalAppealType.EMBARGOS_TERCEIRO) {
            String sameGradeApartadoKey = ProceedingKeyFactory.realKey(snapshot.caseFileId(), ctx.currentInstance(), ctx.tribunal(), payload.protocolNumber().isBlank() ? (ctx.numeroUnificado() + '-' + appeal.name()) : payload.protocolNumber());
            NivelSigilo derivedSigilo = secrecy.derive(ctx.currentSecrecy(), null, appeal, ctx.currentInstance());
            String proceedingNumber = payload.protocolNumber().isBlank() ? ctx.numeroUnificado() : payload.protocolNumber();
            return RecursalPlan.builder()
                    .addProceeding(new ProceedingUpsert(
                            sameGradeApartadoKey,
                            false,
                            CaseProceedingStatus.ACTIVE,
                            ctx.currentInstance(),
                            ctx.tribunal(),
                            proceedingNumber,
                            null,
                            derivedSigilo,
                            fact.sourceSystem()
                    ))
                    .addEdge(new EdgeUpsert(rootKey, sameGradeApartadoKey, RecursalRelationType.APPEAL_DERIVED, appeal))
                    .addWorkItem(new WorkItemDirective(
                            "RECURSAL/APARTADOS_DEPENDENCIA",
                            "Autuação dependente: " + appeal.name(),
                            payload.notes(),
                            null
                    ))
                    .note("Incidente autuado em apartado por dependência: " + appeal)
                    .build();
        }

        InstanceLevel target = resolveTargetInstance(appeal, payload.targetInstanceHint(), ctx);
        String targetCourtHint = !payload.targetCourtHint().isBlank() ? payload.targetCourtHint() : ctx.tribunal();

        String shadowKey = ProceedingKeyFactory.shadowKey(snapshot.caseFileId(), target, targetCourtHint, appeal, ctx.numeroUnificado());
        NivelSigilo derivedSigilo = secrecy.derive(ctx.currentSecrecy(), null, appeal, target);

        RecursalPlan.Builder b = RecursalPlan.builder();

        b.addProceeding(new ProceedingUpsert(
                shadowKey,
                true,
                CaseProceedingStatus.PREDICTED,
                target,
                targetCourtHint,
                "",
                null,
                derivedSigilo,
                fact.sourceSystem()
        ));

        b.addEdge(new EdgeUpsert(rootKey, shadowKey, RecursalRelationType.APPEAL_DERIVED, appeal));

        b.addSync(new SyncDirective(
                fact.sourceSystem(),
                shadowKey,
                ctx.numeroUnificado(),
                target,
                targetCourtHint,
                85
        ));

        b.addWorkItem(new WorkItemDirective(
                "RECURSAL/ACOMPANHAR",
                "Recurso interposto: " + appeal.name(),
                payload.protocolNumber().isBlank() ? payload.notes() : ("Protocolo: " + payload.protocolNumber() + ". " + payload.notes()),
                null
        ));


        b.note("ShadowProceeding previsto para " + target + " (" + targetCourtHint + ") via " + appeal.name());


        if (appeal == LegalAppealType.APELACAO || appeal == LegalAppealType.AGRAVO_INSTRUMENTO || appeal == LegalAppealType.RECURSO_INOMINADO) {
            b.note("Watcher: aguardar sinais de RESP/RE/agravo nos superiores");
        }

        return b.build();
    }

    private RecursalPlan planAutuation(CanonicalFact fact,
                                      AutuationPayload payload,
                                      GraphSnapshot snapshot,
                                      ProceduralContext ctx) {

        String proceedingNumber = payload.targetProceedingNumber().isBlank()
                ? Objects.toString(ctx.numeroUnificado(), "").trim()
                : payload.targetProceedingNumber();
        if (proceedingNumber.isBlank()) {
            return RecursalPlan.builder().note("Registro de destino sem numeração disponível").build();
        }

        String court = !payload.targetCourt().isBlank() ? payload.targetCourt() : ctx.tribunal();
        String realKey = ProceedingKeyFactory.realKey(snapshot.caseFileId(), payload.targetInstance(), court, proceedingNumber);

        RecursalPlan.Builder b = RecursalPlan.builder();
        b.addProceeding(new ProceedingUpsert(
                realKey,
                false,
                CaseProceedingStatus.ACTIVE,
                payload.targetInstance(),
                court,
                proceedingNumber,
                null,
                secrecy.derive(ctx.currentSecrecy(), null, null, payload.targetInstance()),
                fact.sourceSystem()
        ));


        String candidateShadow = pickSingleShadowCandidate(snapshot.proceedings(), payload.targetInstance(), court);
        if (candidateShadow != null) {
            b.addEdge(new EdgeUpsert(candidateShadow, realKey, RecursalRelationType.SHADOW_RECONCILED, LegalAppealType.OUTRO));
            b.addProceeding(new ProceedingUpsert(
                    candidateShadow,
                    true,
                    CaseProceedingStatus.RECONCILED,
                    payload.targetInstance(),
                    court,
                    "",
                    null,
                    ctx.currentSecrecy(),
                    fact.sourceSystem()
            ));
            b.note("Reconciliação: shadow->real aplicada");
        } else {

            if (snapshot.anchorProceedingKey() != null) {
                b.addEdge(new EdgeUpsert(snapshot.anchorProceedingKey(), realKey, RecursalRelationType.APPEAL_DERIVED, LegalAppealType.OUTRO));
                b.note("Autuação real sem sombra detectada; edge root->real criado");
            }
        }

        b.addSync(new SyncDirective(
                fact.sourceSystem(),
                realKey,
                proceedingNumber,
                payload.targetInstance(),
                court,
                95
        ));

        return b.build();
    }

    private RecursalPlan planSecrecyChange(CanonicalFact fact,
                                          SecrecyChangedPayload payload,
                                          GraphSnapshot snapshot,
                                          ProceduralContext ctx) {

        String rootKey = snapshot.anchorProceedingKey();
        if (rootKey == null || rootKey.isBlank()) {
            return RecursalPlan.builder().note("rootProceedingKey ausente em sigilo").build();
        }

        NivelSigilo next = secrecy.derive(ctx.currentSecrecy(), payload.newLevel(), null, ctx.currentInstance());

        return RecursalPlan.builder()
                .addProceeding(new ProceedingUpsert(
                        rootKey,
                        false,
                        CaseProceedingStatus.ACTIVE,
                        ctx.currentInstance(),
                        ctx.tribunal(),
                        ctx.numeroUnificado(),
                        ctx.processoId(),
                        next,
                        fact.sourceSystem()
                ))
                .note("Sigilo atualizado (herança+max): " + next.name())
                .build();
    }

    private static InstanceLevel resolveTargetInstance(LegalAppealType appeal,
                                                      InstanceLevel hint,
                                                      ProceduralContext ctx) {
        if (ctx == null) {
            return hint != null ? hint : InstanceLevel.FIRST_INSTANCE;
        }
        RecursalPlatformProfile profile = RecursalRitePlatformPolicy.resolve(ctx.ramoDireito(), ctx.rito(), ctx.tipoJustica());
        return RecursalRitePlatformPolicy.targetInstanceFor(appeal, hint, profile, ctx.currentInstance());
    }

    private static String pickSingleShadowCandidate(List<ProceedingView> proceedings,
                                                    InstanceLevel instance,
                                                    String court) {
        if (proceedings == null || proceedings.isEmpty()) return null;
        String normalizedCourt = court == null ? "" : court.trim();
        String found = null;
        for (ProceedingView p : proceedings) {
            if (p == null) continue;
            if (!p.shadow()) continue;
            if (p.instanceLevel() != instance) continue;
            if (!normalizedCourt.isBlank() && p.court() != null && !p.court().isBlank() && !p.court().trim().equalsIgnoreCase(normalizedCourt)) {
                continue;
            }
            if (p.status() != CaseProceedingStatus.PREDICTED) continue;
            if (found != null) return null;
            found = p.proceedingKey();
        }
        return found;
    }
}
