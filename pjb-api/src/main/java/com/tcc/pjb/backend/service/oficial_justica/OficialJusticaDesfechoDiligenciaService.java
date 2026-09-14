package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaDesfechoDiligenciaService {

    private final WorkItemRepository workItemRepository;
    private final ProcessoRepository processoRepository;
    private final PainelServiceCommons commons;
    private final PerfilDashboardContextFactory contextFactory;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService;
    private final OficialJusticaCommunicationFormalModelService communicationFormalModelService;

    public OficialJusticaDesfechoDiligenciaService(WorkItemRepository workItemRepository,
                                                   ProcessoRepository processoRepository,
                                                   PainelServiceCommons commons,
                                                   PerfilDashboardContextFactory contextFactory,
                                                   InstitutionalActorRoutingService institutionalActorRoutingService,
                                                   InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService,
                                                   OficialJusticaCommunicationFormalModelService communicationFormalModelService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository, "workItemRepository");
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.commons = Objects.requireNonNull(commons, "commons");
        this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory");
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService, "institutionalActorRoutingService");
        this.institutionalMultimediaWorkspaceService = Objects.requireNonNull(institutionalMultimediaWorkspaceService, "institutionalMultimediaWorkspaceService");
        this.communicationFormalModelService = Objects.requireNonNull(communicationFormalModelService, "communicationFormalModelService");
    }

    @Transactional
    public Map<String, Object> registrarCumprimento(String mandadoId, Object request) {
        Usuario usuario = contextFactory.build().usuario();
        WorkItem item = resolveMandado(mandadoId);
        Map<String, Object> formalization = communicationFormalModelService.formalizeOutcome(item.getProcesso(), item, usuario, request, false);
        item.setStatus(WorkItemStatus.CONCLUIDO);
        item.setDescricao(communicationFormalModelService.appendFormalTrace(item.getDescricao(), item.getProcesso(), item, usuario, request, false, formalization));
        item = workItemRepository.save(item);
        commons.publishTerritoryHistory(usuario, "OFICIAL", "MANDADO_CUMPRIDO", "Mandado cumprido registrado.", item.getProcesso(), item.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.putAll(commons.mapWorkItem(item));
        out.put("formalization", formalization);
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "OFICIAL_JUSTICA",
                        "CERTIDAO_OFICIAL_JUSTICA",
                        item.getProcesso() != null ? item.getProcesso().getId() : null,
                        usuario.getTipoUsuario(),
                        request,
                        true,
                        false,
                        false
                )
        ));
        return out;
    }

    @Transactional
    public Map<String, Object> registrarFrustracao(String mandadoId, Object request) {
        Usuario usuario = contextFactory.build().usuario();
        WorkItem item = resolveMandado(mandadoId);
        Map<String, Object> formalization = communicationFormalModelService.formalizeOutcome(item.getProcesso(), item, usuario, request, true);
        item.setStatus(WorkItemStatus.CONCLUIDO);
        item.setDescricao(communicationFormalModelService.appendFormalTrace(item.getDescricao(), item.getProcesso(), item, usuario, request, true, formalization));
        item = workItemRepository.save(item);
        Processo processo = item.getProcesso();
        InstitutionalActorRoutingService.InstitutionalRoute route = processo != null
                ? institutionalActorRoutingService.secretaryExecution(processo.getId(), "CERTIDAO_NEGATIVA")
                : new InstitutionalActorRoutingService.InstitutionalRoute("SECRETARIA_CUMPRIMENTO", "SECRETARIA_CUMPRIMENTO", TipoUsuario.SERVIDOR_FORUM, "CERTIDAO_NEGATIVA", null, "Fallback operacional sem processo associado.", Map.of());
        WorkItem followup = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo != null ? processo.getFaseAtual() : null)
                .templateCode("CERTIFICAR_NEGATIVO:" + item.getId())
                .type(WorkItemType.EXPEDICAO)
                .titulo("Certificar negativo e aguardar nova ordem")
                .descricao("Fluxo automático após cumprimento frustrado do oficial de justiça")
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .blocking(false)
                .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal("Certidão negativa e nova ordem judicial")
                .build();
        followup = workItemRepository.save(followup);
        commons.publishTerritoryHistory(usuario, "ASSESSOR", "CUMPRIMENTO_FRUSTRADO", "Nova tarefa de certificação negativa enviada à fila da secretaria.", processo, followup.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mandado", commons.mapWorkItem(item));
        out.put("followup", commons.mapWorkItem(followup));
        out.put("formalization", formalization);
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "OFICIAL_JUSTICA",
                        "CERTIDAO_OFICIAL_JUSTICA",
                        processo != null ? processo.getId() : null,
                        usuario.getTipoUsuario(),
                        request,
                        true,
                        false,
                        false
                )
        ));
        return out;
    }

    @Transactional
    public Map<String, Object> registrarAvaliacao(Long processoId, Object request) {
        Usuario usuario = contextFactory.build().usuario();
        Processo processo = resolveProcesso(processoId);
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.officialJustice(processoId, true, "AVALIACAO_PENHORA");
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo != null ? processo.getFaseAtual() : null)
                .templateCode("AVALIACAO_PENHORA:" + processoId + ':' + Instant.now().toEpochMilli())
                .type(WorkItemType.CALCULO)
                .titulo("Avaliação de bens penhorados registrada")
                .descricao(String.valueOf(request))
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .assignedUser(usuario)
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(2)
                .dueAt(Instant.now())
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .build();
        item = workItemRepository.save(item);
        commons.publishUserHistory(usuario, "OFICIAL", "AVALIACAO_REGISTRADA", "Avaliação patrimonial registrada.", item.getProcesso(), item.getId());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.putAll(commons.mapWorkItem(item));
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "OFICIAL_JUSTICA",
                        "AVALIACAO_OFICIAL_JUSTICA",
                        processoId,
                        usuario.getTipoUsuario(),
                        request,
                        true,
                        false,
                        false
                )
        ));
        return out;
    }

    private WorkItem resolveMandado(String mandadoId) {
        try {
            Long id = Long.parseLong(mandadoId);
            return workItemRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("WorkItem", id));
        } catch (NumberFormatException ex) {
            throw new RecursoNaoEncontradoException("WorkItem", mandadoId);
        }
    }

    private Processo resolveProcesso(Long processoId) {
        if (processoId == null) {
            return null;
        }
        return processoRepository.findById(processoId).orElse(null);
    }
}
