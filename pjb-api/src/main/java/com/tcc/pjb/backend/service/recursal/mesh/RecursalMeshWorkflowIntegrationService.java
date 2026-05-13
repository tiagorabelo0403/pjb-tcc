package com.tcc.pjb.backend.service.recursal.mesh;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRoutePlan;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;

@Service
public class RecursalMeshWorkflowIntegrationService {

    private final MovimentacaoProcessualRepository movimentacaoProcessualRepository;
    private final AuditLedgerService auditLedgerService;

    public RecursalMeshWorkflowIntegrationService(
            MovimentacaoProcessualRepository movimentacaoProcessualRepository,
            AuditLedgerService auditLedgerService) {
        this.movimentacaoProcessualRepository = movimentacaoProcessualRepository;
        this.auditLedgerService = auditLedgerService;
    }

    @Transactional
    public void onAggregateOpened(Processo processo, RecursalPlanningResult planning, String actor, String payloadHash) {
        if (processo == null) {
            return;
        }
        registrarMovimentacao(processo, descricaoAbertura(planning.species(), planning.routePlan(), actor));
        auditLedgerService.appendSafely(
                "RECURSAL_MESH_AGGREGATE_OPENED",
                "PROCESSO",
                processo.getId() == null ? null : processo.getId().toString(),
                payloadHash,
                descricaoAbertura(planning.species(), planning.routePlan(), actor)
        );
    }

    @Transactional
    public void onTransition(Processo processo, RecursalSpecies species, RecursalTransitionEvent event, RecursalTransitionResult result, String actor, String payloadHash) {
        if (processo == null) {
            return;
        }
        registrarMovimentacao(processo, descricaoTransicao(species, event, result.routePlan(), result.current().revision(), actor));
        auditLedgerService.appendSafely(
                "RECURSAL_MESH_TRANSITIONED",
                "PROCESSO",
                processo.getId() == null ? null : processo.getId().toString(),
                payloadHash,
                descricaoTransicao(species, event, result.routePlan(), result.current().revision(), actor)
        );
    }

    private void registrarMovimentacao(Processo processo, String descricao) {
        MovimentacaoProcessual movimentacao = MovimentacaoProcessual.builder()
                .processo(processo)
                .faseDe(processo.getFaseAtual())
                .fasePara(FaseProcessual.RECURSAL)
                .descricao(descricao)
                .build();
        movimentacaoProcessualRepository.save(movimentacao);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setDataUltimaMovimentacao(LocalDateTime.now());
    }

    private String descricaoAbertura(RecursalSpecies species, RecursalRoutePlan routePlan, String actor) {
        String sufixoAtor = actor == null || actor.isBlank() ? "" : " por " + actor.trim();
        return species.formalName()
                + " aberto na malha recursal "
                + routePlan.tribunalDetalhadoOrigem().name()
                + " -> "
                + routePlan.tribunalDetalhadoDestino().name()
                + sufixoAtor;
    }

    private String descricaoTransicao(RecursalSpecies species, RecursalTransitionEvent event, RecursalRoutePlan routePlan, int revisao, String actor) {
        String sufixoAtor = actor == null || actor.isBlank() ? "" : " por " + actor.trim();
        return species.formalName()
                + " transitado no evento "
                + event.name()
                + " revisão "
                + revisao
                + " rota "
                + routePlan.tribunalDetalhadoOrigem().name()
                + " -> "
                + routePlan.tribunalDetalhadoDestino().name()
                + sufixoAtor;
    }
}
