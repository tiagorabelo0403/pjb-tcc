package com.tcc.pjb.backend.service.magistratura.acts;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.casefile.CaseContinuityDecisionGateService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.julgamento.safety.DecisionSafetyService;
import com.tcc.pjb.backend.service.processual.document.template.RecursalQualifiedDocumentMaterializerService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MagistraturaJudicialActRelatoriaFormalizationSupport {

    private final WorkItemRepository workItemRepository;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService;
    private final CaseContinuityDecisionGateService caseContinuityDecisionGateService;
    private final DecisionSafetyService decisionSafetyService;
    private final PainelServiceCommons commons;
    private final MagistraturaJudicialActProjectionSupport projectionSupport;

    public MagistraturaJudicialActRelatoriaFormalizationSupport(WorkItemRepository workItemRepository,
                                                                InstitutionalActorRoutingService institutionalActorRoutingService,
                                                                RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService,
                                                                CaseContinuityDecisionGateService caseContinuityDecisionGateService,
                                                                DecisionSafetyService decisionSafetyService,
                                                                PainelServiceCommons commons,
                                                                MagistraturaJudicialActProjectionSupport projectionSupport) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
        this.recursalQualifiedDocumentMaterializerService = Objects.requireNonNull(recursalQualifiedDocumentMaterializerService);
        this.caseContinuityDecisionGateService = Objects.requireNonNull(caseContinuityDecisionGateService);
        this.decisionSafetyService = Objects.requireNonNull(decisionSafetyService);
        this.commons = Objects.requireNonNull(commons);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    public Map<String, Object> registrarDespachoRelatoria(Processo processo,
                                                          Usuario usuario,
                                                          String conteudo,
                                                          String fundamentacao) {
        caseContinuityDecisionGateService.requireAllowed(processo.getId(), ProcessoLifecycleAction.ASSINAR_DESPACHO);
        decisionSafetyService.requireSafeDecisionContext(processo, usuario, "DESPACHO_RELATOR", conteudo, fundamentacao);
        String dedupKey = deterministicKey("DESPACHO_RELATOR", processo.getId(), usuario.getId());
        InstitutionalActorRoutingService.InstitutionalRoute route = usuario.getTipoUsuario() == TipoUsuario.MINISTRO
                ? institutionalActorRoutingService.superiorCourt(processo.getId(), "DESPACHO_RELATORIA", false)
                : institutionalActorRoutingService.colegiado(processo.getId(), "DESPACHO_RELATORIA");
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.DESPACHO)
                .titulo("Despacho da relatoria — " + projectionSupport.processNumber(processo))
                .descricao(conteudo)
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(0)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(fundamentacao)
                .dueAt(Instant.now().plus(4, ChronoUnit.HOURS))
                .build();
        workItemRepository.save(item);
        commons.publishUserHistory(usuario, projectionSupport.actorHistoryLane(usuario), "DESPACHO_RELATORIA_ASSINADO", "Despacho da relatoria registrado no processo.", processo, processo.getId());
        Map<String, Object> documento = recursalQualifiedDocumentMaterializerService.materializarPronunciamentoRelatoria(
                processo.getId(),
                item.getTitulo(),
                projectionSupport.defaultText(fundamentacao, "Fundamentação relatorial lançada."),
                projectionSupport.defaultText(conteudo, "Determinação relatorial registrada."),
                projectionSupport.actorCourtLabel(usuario),
                projectionSupport.actorInstanceLabel(usuario),
                "DESPACHO_RELATORIA",
                null
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "DESPACHO_RELATORIA_REGISTRADO");
        out.put("processoId", processo.getId());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("encaminhadoPara", route.inboxKey());
        out.put("documentoFormalAssinado", documento);
        return projectionSupport.safeMap(out);
    }

    public Map<String, Object> registrarDecisaoMonocraticaRelatoria(Processo processo,
                                                                     Usuario usuario,
                                                                     String relatorio,
                                                                     String fundamentacao,
                                                                     String dispositivo) {
        decisionSafetyService.requireSafeDecisionContext(processo, usuario, "DECISAO_MONOCRATICA", dispositivo, fundamentacao);
        String dedupKey = deterministicKey("DECISAO_MONOCRATICA_RELATORIA", processo.getId(), usuario.getId());
        InstitutionalActorRoutingService.InstitutionalRoute route = usuario.getTipoUsuario() == TipoUsuario.MINISTRO
                ? institutionalActorRoutingService.superiorCourt(processo.getId(), "DECISAO_MONOCRATICA", true)
                : institutionalActorRoutingService.colegiadoPublication(processo.getId(), "DECISAO_MONOCRATICA");
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.DECISAO)
                .titulo("Decisão monocrática — " + projectionSupport.processNumber(processo))
                .descricao("RELATÓRIO: " + relatorio + " | DISPOSITIVO: " + dispositivo)
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(0)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(fundamentacao)
                .dueAt(Instant.now().plus(6, ChronoUnit.HOURS))
                .build();
        workItemRepository.save(item);
        commons.publishUserHistory(usuario, projectionSupport.actorHistoryLane(usuario), "DECISAO_MONOCRATICA_RELATORIA", "Decisão monocrática registrada na relatoria.", processo, processo.getId());
        Map<String, Object> documento = recursalQualifiedDocumentMaterializerService.materializarDecisaoMonocratica(
                processo.getId(),
                item.getTitulo(),
                relatorio,
                fundamentacao,
                dispositivo,
                projectionSupport.actorCourtLabel(usuario),
                projectionSupport.actorInstanceLabel(usuario)
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "DECISAO_MONOCRATICA_REGISTRADA");
        out.put("processoId", processo.getId());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("encaminhadoPara", route.inboxKey());
        out.put("documentoFormalAssinado", documento);
        return projectionSupport.safeMap(out);
    }

    private String deterministicKey(String axis, Long processoId, Long usuarioId) {
        return UUID.nameUUIDFromBytes((axis + ':' + processoId + ':' + usuarioId).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
