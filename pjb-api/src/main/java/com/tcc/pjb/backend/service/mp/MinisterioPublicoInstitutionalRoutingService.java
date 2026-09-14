package com.tcc.pjb.backend.service.mp;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.processual.guard.InstitutionalMaterialActionGuardService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MinisterioPublicoInstitutionalRoutingService {

    private final PjbAuthorizationService authorizationService;
    private final InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final PainelServiceCommons commons;
    private final PerfilDashboardContextFactory contextFactory;
    private final InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService;

    public MinisterioPublicoInstitutionalRoutingService(PjbAuthorizationService authorizationService,
                                                        InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService,
                                                        InstitutionalActorRoutingService institutionalActorRoutingService,
                                                        ProcessoRepository processoRepository,
                                                        WorkItemRepository workItemRepository,
                                                        PainelServiceCommons commons,
                                                        PerfilDashboardContextFactory contextFactory,
                                                        InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.institutionalActorTopologyMeshService = Objects.requireNonNull(institutionalActorTopologyMeshService);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.commons = Objects.requireNonNull(commons);
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.institutionalMaterialActionGuardService = Objects.requireNonNull(institutionalMaterialActionGuardService);
    }

    public InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot malhaProcesso(Long processoId) {
        authorizationService.requireVinculoInstitucionalComProcesso(processoId);
        return institutionalActorTopologyMeshService.snapshot(processoId);
    }

    @Transactional
    public Map<String, Object> requisitarDiligencia(Long processoId, Object request) {
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        institutionalMaterialActionGuardService.requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.MINISTERIO_PUBLICO_REQUISICAO_DILIGENCIA);
        Usuario usuario = contextFactory.build().usuario();
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.policeDiligence(processoId);
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("DELEGACIA_DILIGENCIA:" + processoId + ':' + Instant.now().toEpochMilli())
                .type(WorkItemType.DILIGENCIA)
                .titulo("Cumprir diligência requisitada pelo Ministério Público")
                .descricao(String.valueOf(request))
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .dueAt(Instant.now().plus(48, ChronoUnit.HOURS))
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal("Requisição de diligência do Ministério Público")
                .build();
        item = workItemRepository.save(item);
        commons.publishTerritoryHistory(usuario, "DELEGADO", "MP_REQUISITOU_DILIGENCIA", "Nova diligência recebida do MP.", processo, item.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "REQUISITADA");
        out.put("workItemId", item.getId());
        out.put("dueAt", item.getDueAt());
        out.put("encaminhadoPara", route.inboxKey());
        out.put("routeAxis", route.routeAxis());
        return out;
    }
}
