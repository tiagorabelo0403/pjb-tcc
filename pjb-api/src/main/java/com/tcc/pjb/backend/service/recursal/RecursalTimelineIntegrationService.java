package com.tcc.pjb.backend.service.recursal;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalFactType;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTramitationMode;
import com.tcc.pjb.backend.core.kernel.recursal.model.AdmissibilityPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.AppealFiledPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.AutuationPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.model.JudgmentPublishedPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.MovementRecordedPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.SecrecyChangedPayload;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class RecursalTimelineIntegrationService {

    private static final String PREFIX = "PJB 2.0 • Recursal";

    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;

    public RecursalTimelineIntegrationService(ProcessoRepository processoRepository,
                                             MovimentacaoProcessualRepository movimentacaoRepository) {
        this.processoRepository = processoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Transactional
    public Long appendTimelineEntry(Long processoId, CanonicalFact fact, RecursalPlan plan) {
        Objects.requireNonNull(processoId, "processoId é obrigatório");
        Objects.requireNonNull(fact, "fact é obrigatório");

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));

        RecursalTramitationMode tramitationMode = resolveTramitationMode(fact);
        if (!shouldPersistOnSourceTimeline(fact, tramitationMode)) {
            return null;
        }

        FaseProcessual faseDe = processo.getFaseAtual();
        FaseProcessual fasePara = computeFasePara(faseDe, fact, tramitationMode);

        if (fasePara != null && fasePara != faseDe) {
            processo.setFaseAtual(fasePara);
            processoRepository.save(processo);
        }

        String descricao = buildDescricao(fact, plan, tramitationMode);

        MovimentacaoProcessual mov = MovimentacaoProcessual.builder()
                .processo(processo)
                .faseDe(faseDe)
                .fasePara(fasePara)
                .descricao(descricao)
                .build();

        MovimentacaoProcessual saved = movimentacaoRepository.save(mov);
        return saved.getId();
    }

    private static FaseProcessual computeFasePara(FaseProcessual faseDe, CanonicalFact fact, RecursalTramitationMode tramitationMode) {
        if (fact == null || fact.type() == null) {
            return faseDe;
        }
        return switch (fact.type()) {
            case APPEAL_FILED, REMITTED_TO_TARGET, AUTUATED_IN_TARGET -> tramitationMode.freezeSourceTimeline() ? FaseProcessual.RECURSAL : faseDe;
            case ADMISSIBILITY_GRANTED, ADMISSIBILITY_DENIED, JUDGMENT_PUBLISHED, MOVEMENT_RECORDED, CONFIDENTIALITY_CHANGED -> faseDe;
        };
    }

    private static String buildDescricao(CanonicalFact fact, RecursalPlan plan, RecursalTramitationMode tramitationMode) {
        String base = PREFIX + " — " + fact.type().name();

        String details = switch (fact.payload()) {
            case AppealFiledPayload p -> {
                String tipo = p.appealType() != null ? p.appealType().name() : "RECURSO";
                String alvo = p.targetInstanceHint() != null ? p.targetInstanceHint().name() : "";
                String court = p.targetCourtHint() != null && !p.targetCourtHint().isBlank() ? (" • " + p.targetCourtHint()) : "";
                String prot = p.protocolNumber() != null && !p.protocolNumber().isBlank() ? (" • protocolo " + p.protocolNumber()) : "";
                String mode = switch (tramitationMode) {
                    case SAME_AUTOS_SAME_GRADE -> " • tramitação permanece no mesmo grau e nos mesmos autos";
                    case APARTADO_DEPENDENCIA_SAME_GRADE -> " • incidente autuado em apartado por dependência";
                    case HIGHER_GRADE_SAME_NUMBERING -> " • processo remetido em grau recursal com mesma numeração CNJ";
                    case HIGHER_GRADE_AUTONOMOUS -> " • procedimento autônomo vinculado no grau de destino";
                };
                yield "Recurso interposto: " + tipo + (alvo.isBlank() ? "" : (" → " + alvo)) + court + prot + mode;
            }
            case AutuationPayload p -> {
                String num = p.targetProceedingNumber() != null && !p.targetProceedingNumber().isBlank()
                        ? p.targetProceedingNumber()
                        : fact.sourceProceedingNumber();
                String inst = p.targetInstance() != null ? p.targetInstance().name() : "";
                String court = p.targetCourt() != null && !p.targetCourt().isBlank() ? (" • " + p.targetCourt()) : "";
                String mode = fact.sourceProceedingNumber() != null && fact.sourceProceedingNumber().equals(num)
                        ? " • mesma numeração CNJ"
                        : " • autuação vinculada";
                String scope = tramitationMode.freezeSourceTimeline() ? " • tramitação ativa no grau de destino" : " • controle interno de apartado/incidente";
                yield "Registro no destino: " + (num == null || num.isBlank() ? "(número não informado)" : num) + (inst.isBlank() ? "" : (" • " + inst)) + court + mode + scope;
            }
            case AdmissibilityPayload p -> "Juízo de admissibilidade: " + (p.granted() ? "DEFERIDO/ADMITIDO" : "INDEFERIDO/INADMITIDO")
                    + (p.authority() != null && !p.authority().isBlank() ? (" • " + p.authority()) : "");
            case SecrecyChangedPayload p -> "Sigilo ajustado: " + (p.newLevel() != null ? p.newLevel().name() : "N/A")
                    + (p.reason() != null && !p.reason().isBlank() ? (" • " + p.reason()) : "");
            case JudgmentPublishedPayload p -> {
                String where = p.court() != null && !p.court().isBlank() ? (" • " + p.court()) : "";
                String sum = p.resultSummary() != null && !p.resultSummary().isBlank() ? (": " + p.resultSummary()) : "";
                yield "Julgamento publicado" + where + sum;
            }
            case MovementRecordedPayload p -> "Movimentação registrada: " + (p.movementText() != null ? p.movementText() : "");
            default -> "";
        };

        if (plan == null) {
            return base + (details.isBlank() ? "" : (" • " + details));
        }

        String notes = (plan.notes() == null || plan.notes().isEmpty()) ? "" : (" • " + String.join(" | ", plan.notes()));
        int newNodes = plan.proceedings() != null ? plan.proceedings().size() : 0;
        int newEdges = plan.edges() != null ? plan.edges().size() : 0;
        String planInfo = " • Plano aplicado: nodes=" + newNodes + ", edges=" + newEdges;
        return base + (details.isBlank() ? "" : (" • " + details)) + planInfo + notes;
    }

    private static boolean shouldPersistOnSourceTimeline(CanonicalFact fact, RecursalTramitationMode tramitationMode) {
        if (fact == null || fact.type() == null) {
            return false;
        }
        return switch (fact.type()) {
            case APPEAL_FILED, REMITTED_TO_TARGET, AUTUATED_IN_TARGET, CONFIDENTIALITY_CHANGED -> true;
            case ADMISSIBILITY_GRANTED, ADMISSIBILITY_DENIED, JUDGMENT_PUBLISHED, MOVEMENT_RECORDED -> !tramitationMode.freezeSourceTimeline();
        };
    }

    private static RecursalTramitationMode resolveTramitationMode(CanonicalFact fact) {
        if (fact != null && fact.payload() instanceof AppealFiledPayload payload && payload.appealType() != null) {
            LegalAppealType appealType = payload.appealType();
            if (appealType == LegalAppealType.EMBARGOS_DECLARACAO || appealType == LegalAppealType.AGRAVO_INTERNO || appealType == LegalAppealType.AGRAVO_REGIMENTAL || appealType == LegalAppealType.CORREICAO_PARCIAL) {
                return RecursalTramitationMode.SAME_AUTOS_SAME_GRADE;
            }
            if (appealType == LegalAppealType.EMBARGOS_EXECUCAO || appealType == LegalAppealType.EMBARGOS_EXECUCAO_FISCAL || appealType == LegalAppealType.EMBARGOS_TERCEIRO) {
                return RecursalTramitationMode.APARTADO_DEPENDENCIA_SAME_GRADE;
            }
            if (payload.autosApartadosLikely()) {
                return RecursalTramitationMode.HIGHER_GRADE_AUTONOMOUS;
            }
            return RecursalTramitationMode.HIGHER_GRADE_SAME_NUMBERING;
        }
        if (fact != null && fact.payload() instanceof AutuationPayload payload) {
            return fact.sourceProceedingNumber() != null && fact.sourceProceedingNumber().equals(payload.targetProceedingNumber())
                    ? RecursalTramitationMode.HIGHER_GRADE_SAME_NUMBERING
                    : RecursalTramitationMode.HIGHER_GRADE_AUTONOMOUS;
        }
        return RecursalTramitationMode.SAME_AUTOS_SAME_GRADE;
    }
}
