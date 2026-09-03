package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAccessContextMaterializationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalDocumentSecurityGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.processual.destinatario.application.DestinatarioProcessualResolverApplicationService;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioCartorioAckRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioChannelAckRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioConfirmationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioReconciliationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRetryRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concentra o ciclo completo de ofícios do oficial de justiça: catálogo, emissão/resposta
 * direto no processo e o acompanhamento rastreável da execução (ledger). Extraído de
 * {@link OficialJusticaPainelService} porque esses colaboradores são usados exclusivamente
 * por esse subconjunto de métodos -- nenhum é tocado por bootstrapPainel() ou pelos demais
 * métodos do painel.
 */
@Service
public class OficialJusticaOficioDispatchService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService;
    private final DestinatarioProcessualResolverApplicationService destinatarioResolverApplicationService;
    private final OficialJusticaOficioCatalogService oficioCatalogService;
    private final OficialJusticaTraceableCommunicationLedgerService traceableCommunicationLedgerService;
    private final OficialJusticaOficioSecurityService oficioSecurityService;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final InstitutionalDocumentSecurityGateApplicationService institutionalDocumentSecurityGateApplicationService;
    private final InstitutionalAccessContextMaterializationApplicationService institutionalAccessContextMaterializationApplicationService;

    public OficialJusticaOficioDispatchService(PerfilDashboardContextFactory contextFactory,
                                               PainelServiceCommons commons,
                                               ProcessoRepository processoRepository,
                                               WorkItemRepository workItemRepository,
                                               InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService,
                                               DestinatarioProcessualResolverApplicationService destinatarioResolverApplicationService,
                                               OficialJusticaOficioCatalogService oficioCatalogService,
                                               OficialJusticaTraceableCommunicationLedgerService traceableCommunicationLedgerService,
                                               OficialJusticaOficioSecurityService oficioSecurityService,
                                               InstitutionalActorRoutingService institutionalActorRoutingService,
                                               InstitutionalDocumentSecurityGateApplicationService institutionalDocumentSecurityGateApplicationService,
                                               InstitutionalAccessContextMaterializationApplicationService institutionalAccessContextMaterializationApplicationService) {
        this.contextFactory = Objects.requireNonNull(contextFactory, "contextFactory");
        this.commons = Objects.requireNonNull(commons, "commons");
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.workItemRepository = Objects.requireNonNull(workItemRepository, "workItemRepository");
        this.institutionalMultimediaWorkspaceService = Objects.requireNonNull(institutionalMultimediaWorkspaceService, "institutionalMultimediaWorkspaceService");
        this.destinatarioResolverApplicationService = Objects.requireNonNull(destinatarioResolverApplicationService, "destinatarioResolverApplicationService");
        this.oficioCatalogService = Objects.requireNonNull(oficioCatalogService, "oficioCatalogService");
        this.traceableCommunicationLedgerService = Objects.requireNonNull(traceableCommunicationLedgerService, "traceableCommunicationLedgerService");
        this.oficioSecurityService = Objects.requireNonNull(oficioSecurityService, "oficioSecurityService");
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService, "institutionalActorRoutingService");
        this.institutionalDocumentSecurityGateApplicationService = Objects.requireNonNull(institutionalDocumentSecurityGateApplicationService, "institutionalDocumentSecurityGateApplicationService");
        this.institutionalAccessContextMaterializationApplicationService = Objects.requireNonNull(institutionalAccessContextMaterializationApplicationService, "institutionalAccessContextMaterializationApplicationService");
    }

    public Map<String, Object> catalogo() {
        return oficioCatalogService.catalogo(contextFactory.build().usuario().getTipoUsuario());
    }

    public Map<String, Object> listarExecucoes(int limit) {
        return traceableCommunicationLedgerService.recentExecutions(contextFactory.build().usuario().getTipoUsuario(), limit);
    }

    public Map<String, Object> statusExecucao(String executionId) {
        return traceableCommunicationLedgerService.executionStatus(contextFactory.build().usuario().getTipoUsuario(), executionId);
    }

    public Map<String, Object> confirmarEntrega(String executionId, OficialJusticaOficioConfirmationRequest request) {
        return traceableCommunicationLedgerService.confirmDelivery(contextFactory.build().usuario().getTipoUsuario(), executionId, request);
    }

    public Map<String, Object> confirmarCanal(String executionId, OficialJusticaOficioChannelAckRequest request) {
        return traceableCommunicationLedgerService.confirmChannelDelivery(contextFactory.build().usuario().getTipoUsuario(), executionId, request);
    }

    public Map<String, Object> ackCartorio(String executionId, OficialJusticaOficioCartorioAckRequest request) {
        return traceableCommunicationLedgerService.acknowledgeCartorio(contextFactory.build().usuario().getTipoUsuario(), executionId, request);
    }

    public Map<String, Object> reconciliar(String executionId, OficialJusticaOficioReconciliationRequest request) {
        return traceableCommunicationLedgerService.reconcileExecution(contextFactory.build().usuario().getTipoUsuario(), executionId, request);
    }

    public Map<String, Object> malhaExterna(String executionId) {
        return traceableCommunicationLedgerService.externalMeshStatus(contextFactory.build().usuario().getTipoUsuario(), executionId);
    }

    public Map<String, Object> retentar(String executionId, OficialJusticaOficioRetryRequest request) {
        return traceableCommunicationLedgerService.retryExecution(contextFactory.build().usuario().getTipoUsuario(), executionId, request);
    }

    @Transactional
    public Map<String, Object> emitir(Long processoId, OficialJusticaOficioRequest request) {
        Processo processo = resolveProcessoObrigatorio(processoId);
        Usuario usuario = contextFactory.build().usuario();
        oficioSecurityService.enforceCanSendIntoProcess(processo, usuario, "OFICIO_OFICIAL_JUSTICA");
        OficialJusticaOficioRequest safe = request == null
                ? new OficialJusticaOficioRequest("Ofício do oficial de justiça", "Destinatário institucional", "Conteúdo não informado", "Fundamento não informado", null, null, null, null, java.util.List.of(), Map.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), Boolean.TRUE, Boolean.FALSE, Boolean.TRUE)
                : request;
        var destinatario = OficialJusticaOficioWorkflowSupport.resolveDestinatario(destinatarioResolverApplicationService, safe);
        OficialJusticaOficioCatalogService.OficioTypeDefinition oficioType = oficioCatalogService.resolveType(safe.tipoOficioCode(), false);
        OficialJusticaOficioCatalogService.TemplateDefinition template = oficioCatalogService.resolveTemplate(safe.minutaCode(), oficioType);
        Map<String, Object> destinatarioMap = OficialJusticaOficioWorkflowSupport.buildDestinatarioMap(destinatario, safe);
        Map<String, Object> minutaGovernada = oficioCatalogService.renderMinutaGovernada(safe, processo, usuario, destinatarioMap, oficioType, template, false);
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.officialJustice(processoId, usuario.getTipoUsuario() == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR, "OFICIO_OFICIAL_JUSTICA");
        var institutionalSignatureGate = institutionalDocumentSecurityGateApplicationService.enforce(
                null,
                null,
                InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO,
                "OFICIO_OFICIAL_JUSTICA",
                true);
        WorkItem oficio = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("OFICIO_OFICIAL:" + processoId + ':' + usuario.getId() + ':' + Instant.now().toEpochMilli())
                .type(WorkItemType.EXPEDICAO)
                .titulo("Ofício do oficial de justiça — " + safe.assunto())
                .descricao(OficialJusticaOficioWorkflowSupport.composeOficioDescricao(safe, false, oficioType, template, destinatarioMap, minutaGovernada))
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .assignedUser(usuario)
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(1)
                .dueAt(Instant.now())
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(OficialJusticaOficioWorkflowSupport.normalizeFundamento(safe.fundamento()))
                .build();
        oficio = workItemRepository.save(oficio);
        oficioSecurityService.enforceOriginalOnlyForDirectProcessSubmission(safe, minutaGovernada, true);
        WorkItem juntadaDireta = OficialJusticaOficioWorkflowSupport.criarJuntadaDiretaNoProcesso(workItemRepository, processo, usuario, oficio, safe, minutaGovernada, false);
        Map<String, Object> traceableExecution = traceableCommunicationLedgerService.registerExecution(
                usuario.getTipoUsuario(),
                processoId,
                "OFICIO_OFICIAL_JUSTICA",
                "JUNTADA_DIRETA_PROCESSO_ORIGINAL",
                template.code(),
                oficioType.asMap(),
                destinatarioMap,
                minutaGovernada,
                false
        );
        String executionId = String.valueOf(traceableExecution.get("executionId"));
        Map<String, Object> dispatchTopology = OficialJusticaOficioWorkflowSupport.directProcessDispatchTopology(processo, usuario, juntadaDireta, executionId, destinatarioMap, minutaGovernada, false);
        traceableExecution = traceableCommunicationLedgerService.attachDispatchTopology(usuario.getTipoUsuario(), executionId, dispatchTopology);
        commons.publishUserHistory(usuario, "OFICIAL", "OFICIO_REGISTRADO", "Ofício do oficial de justiça registrado e protocolado diretamente no processo dentro do PJB.", processo, oficio.getId());
        commons.publishTerritoryHistory(usuario, "OFICIAL", "OFICIO_OFICIAL_JUNTADA_DIRETA", "Ofício original do oficial juntado diretamente no processo sem balcão intermediário.", processo, juntadaDireta.getId());
        Map<String, Object> securityEnvelope = oficioSecurityService.envelope(processo, usuario, "OFICIO_OFICIAL_JUSTICA");
        Map<String, Object> originalOnlyEnvelope = oficioSecurityService.originalOnlyEnvelope(safe, minutaGovernada, true);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "OFICIO_REGISTRADO_DIRETO_NO_PROCESSO");
        out.put("processoId", processoId);
        out.put("workItemId", oficio.getId());
        out.put("workflowAxis", route.routeAxis());
        out.put("oficioType", oficioType.asMap());
        out.put("minutaGovernada", minutaGovernada);
        out.put("destinatarioResolvido", destinatarioMap);
        out.put("traceableExecution", traceableExecution);
        out.put("executionId", traceableExecution.get("executionId"));
        out.put("institutionalDispatch", dispatchTopology);
        out.put("registeredInsideNamedProcess", Boolean.TRUE);
        out.put("protocoladoDiretoNoProcesso", Boolean.TRUE);
        out.put("securityEnvelope", securityEnvelope);
        out.put("originalOnlyEnvelope", originalOnlyEnvelope);
        out.put("institutionalSignatureGate", institutionalSignatureGate.asMap());
        out.put("institutionalAccessContext", institutionalAccessContextMaterializationApplicationService.materializar(institutionalSignatureGate.affiliationId(), institutionalSignatureGate.nominationId()).asMap());
        out.put("oficio", commons.mapWorkItem(oficio));
        out.put("juntadaDiretaProcesso", commons.mapWorkItem(juntadaDireta));
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "OFICIAL_JUSTICA",
                        "OFICIO_OFICIAL_JUSTICA",
                        processoId,
                        usuario.getTipoUsuario(),
                        safe,
                        safe.prepararPacoteProtocoloResolvido(),
                        safe.sigiloSensivelResolvido(),
                        false
                )
        ));
        return out;
    }

    @Transactional
    public Map<String, Object> responder(Long processoId, OficialJusticaOficioRequest request) {
        Processo processo = resolveProcessoObrigatorio(processoId);
        Usuario usuario = contextFactory.build().usuario();
        oficioSecurityService.enforceCanSendIntoProcess(processo, usuario, "RESPOSTA_OFICIO_OFICIAL_JUSTICA");
        OficialJusticaOficioRequest safe = request == null
                ? new OficialJusticaOficioRequest("Resposta a ofício", "Destinatário institucional", "Resposta não informada", "Fundamento não informado", null, null, null, null, java.util.List.of(), Map.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), Boolean.TRUE, Boolean.FALSE, Boolean.TRUE)
                : request;
        var destinatario = OficialJusticaOficioWorkflowSupport.resolveDestinatario(destinatarioResolverApplicationService, safe);
        OficialJusticaOficioCatalogService.OficioTypeDefinition oficioType = oficioCatalogService.resolveType(safe.tipoOficioCode(), true);
        OficialJusticaOficioCatalogService.TemplateDefinition template = oficioCatalogService.resolveTemplate(safe.minutaCode(), oficioType);
        Map<String, Object> destinatarioMap = OficialJusticaOficioWorkflowSupport.buildDestinatarioMap(destinatario, safe);
        Map<String, Object> minutaGovernada = oficioCatalogService.renderMinutaGovernada(safe, processo, usuario, destinatarioMap, oficioType, template, true);
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.officialJustice(processoId, usuario.getTipoUsuario() == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR, "RESPOSTA_OFICIO_OFICIAL_JUSTICA");
        var institutionalSignatureGate = institutionalDocumentSecurityGateApplicationService.enforce(
                null,
                null,
                InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO,
                "RESPOSTA_OFICIO_OFICIAL_JUSTICA",
                true);
        WorkItem resposta = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("RESPOSTA_OFICIO_OFICIAL:" + processoId + ':' + usuario.getId() + ':' + Instant.now().toEpochMilli())
                .type(WorkItemType.EXPEDICAO)
                .titulo("Resposta a ofício pelo oficial de justiça — " + safe.assunto())
                .descricao(OficialJusticaOficioWorkflowSupport.composeOficioDescricao(safe, true, oficioType, template, destinatarioMap, minutaGovernada))
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .assignedUser(usuario)
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(1)
                .dueAt(Instant.now())
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(OficialJusticaOficioWorkflowSupport.normalizeFundamento(safe.fundamento()))
                .build();
        resposta = workItemRepository.save(resposta);
        oficioSecurityService.enforceOriginalOnlyForDirectProcessSubmission(safe, minutaGovernada, true);
        WorkItem juntadaDireta = OficialJusticaOficioWorkflowSupport.criarJuntadaDiretaNoProcesso(workItemRepository, processo, usuario, resposta, safe, minutaGovernada, true);
        Map<String, Object> traceableExecution = traceableCommunicationLedgerService.registerExecution(
                usuario.getTipoUsuario(),
                processoId,
                "RESPOSTA_OFICIO_OFICIAL_JUSTICA",
                "JUNTADA_DIRETA_PROCESSO_ORIGINAL",
                template.code(),
                oficioType.asMap(),
                destinatarioMap,
                minutaGovernada,
                false
        );
        String executionId = String.valueOf(traceableExecution.get("executionId"));
        Map<String, Object> dispatchTopology = OficialJusticaOficioWorkflowSupport.directProcessDispatchTopology(processo, usuario, juntadaDireta, executionId, destinatarioMap, minutaGovernada, true);
        traceableExecution = traceableCommunicationLedgerService.attachDispatchTopology(usuario.getTipoUsuario(), executionId, dispatchTopology);
        commons.publishUserHistory(usuario, "OFICIAL", "RESPOSTA_OFICIO_REGISTRADA", "Resposta a ofício registrada e protocolada diretamente no processo dentro do PJB.", processo, resposta.getId());
        commons.publishTerritoryHistory(usuario, "OFICIAL", "RESPOSTA_OFICIO_JUNTADA_DIRETA", "Resposta a ofício do oficial juntada diretamente no processo sem balcão intermediário.", processo, juntadaDireta.getId());
        Map<String, Object> securityEnvelope = oficioSecurityService.envelope(processo, usuario, "RESPOSTA_OFICIO_OFICIAL_JUSTICA");
        Map<String, Object> originalOnlyEnvelope = oficioSecurityService.originalOnlyEnvelope(safe, minutaGovernada, true);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "RESPOSTA_OFICIO_REGISTRADA_DIRETO_NO_PROCESSO");
        out.put("processoId", processoId);
        out.put("workItemId", resposta.getId());
        out.put("workflowAxis", route.routeAxis());
        out.put("oficioType", oficioType.asMap());
        out.put("minutaGovernada", minutaGovernada);
        out.put("destinatarioResolvido", destinatarioMap);
        out.put("traceableExecution", traceableExecution);
        out.put("executionId", traceableExecution.get("executionId"));
        out.put("institutionalDispatch", dispatchTopology);
        out.put("registeredInsideNamedProcess", Boolean.TRUE);
        out.put("protocoladoDiretoNoProcesso", Boolean.TRUE);
        out.put("securityEnvelope", securityEnvelope);
        out.put("originalOnlyEnvelope", originalOnlyEnvelope);
        out.put("institutionalSignatureGate", institutionalSignatureGate.asMap());
        out.put("institutionalAccessContext", institutionalAccessContextMaterializationApplicationService.materializar(institutionalSignatureGate.affiliationId(), institutionalSignatureGate.nominationId()).asMap());
        out.put("respostaOficio", commons.mapWorkItem(resposta));
        out.put("juntadaDiretaProcesso", commons.mapWorkItem(juntadaDireta));
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "OFICIAL_JUSTICA",
                        "RESPOSTA_OFICIO_OFICIAL_JUSTICA",
                        processoId,
                        usuario.getTipoUsuario(),
                        safe,
                        safe.prepararPacoteProtocoloResolvido(),
                        safe.sigiloSensivelResolvido(),
                        false
                )
        ));
        return out;
    }

    private Processo resolveProcessoObrigatorio(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado: " + processoId));
    }
}
