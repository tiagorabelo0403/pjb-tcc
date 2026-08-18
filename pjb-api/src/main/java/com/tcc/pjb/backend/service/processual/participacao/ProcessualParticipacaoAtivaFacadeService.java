package com.tcc.pjb.backend.service.processual.participacao;

import com.tcc.pjb.backend.core.kernel.process.ProcessEventStore;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventType;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.platform.unified.eventsourcing.JudiciarioEventSourcingEngine;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import com.tcc.pjb.backend.service.processual.guard.DefensoriaInstitutionalCompetenceGuardService;
import com.tcc.pjb.backend.service.processual.participacao.submission.PreparedAttachment;
import com.tcc.pjb.backend.service.processual.participacao.submission.PreparedPrimaryDocument;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionAuditView;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionDocumentView;
import com.tcc.pjb.backend.service.processual.participacao.submission.ProcessualParticipacaoAtivaSubmissionSupport;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionRequest;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionResponse;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionView;
import com.tcc.pjb.backend.service.processual.participacao.workspace.CapabilityMatrix;
import com.tcc.pjb.backend.service.processual.participacao.workspace.DeadlineGuardView;
import com.tcc.pjb.backend.service.processual.participacao.workspace.ExperienceDifferentialView;
import com.tcc.pjb.backend.service.processual.participacao.workspace.PendingView;
import com.tcc.pjb.backend.service.processual.participacao.workspace.ProcessIdentityView;
import com.tcc.pjb.backend.service.processual.participacao.workspace.ProcessualParticipacaoAtivaWorkspaceSupport;
import com.tcc.pjb.backend.service.processual.participacao.workspace.RepresentationGuardView;
import com.tcc.pjb.backend.service.processual.participacao.workspace.RoutingView;
import com.tcc.pjb.backend.service.processual.participacao.workspace.SecurityGuardView;
import com.tcc.pjb.backend.service.processual.participacao.workspace.WorkspaceView;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class ProcessualParticipacaoAtivaFacadeService {

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final ProcessEventStore processEventStore;
    private final EntityManager entityManager;
    private final DefensoriaInstitutionalCompetenceGuardService defensoriaInstitutionalCompetenceGuardService;
    private final ProcessualParticipacaoAtivaWorkspaceSupport workspaceSupport;
    private final ProcessualParticipacaoAtivaSubmissionSupport submissionSupport;

    public ProcessualParticipacaoAtivaFacadeService(ProcessoRepository processoRepository,
                                                    WorkItemRepository workItemRepository,
                                                    CurrentUserService currentUserService,
                                                    PjbAuthorizationService authorizationService,
                                                    ProcessEventStore processEventStore,
                                                    EntityManager entityManager,
                                                    DefensoriaInstitutionalCompetenceGuardService defensoriaInstitutionalCompetenceGuardService,
                                                    ProcessualParticipacaoAtivaWorkspaceSupport workspaceSupport,
                                                    ProcessualParticipacaoAtivaSubmissionSupport submissionSupport) {
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.workItemRepository = Objects.requireNonNull(workItemRepository, "workItemRepository");
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
        this.processEventStore = Objects.requireNonNull(processEventStore, "processEventStore");
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.defensoriaInstitutionalCompetenceGuardService = Objects.requireNonNull(defensoriaInstitutionalCompetenceGuardService, "defensoriaInstitutionalCompetenceGuardService");
        this.workspaceSupport = Objects.requireNonNull(workspaceSupport, "workspaceSupport");
        this.submissionSupport = Objects.requireNonNull(submissionSupport, "submissionSupport");
    }

    @Transactional(readOnly = true)
    public WorkspaceView workspace(Long processoId) {
        Processo processo = loadProcesso(processoId);
        Usuario usuario = currentUserService.getRequired();
        authorizationService.requireWriteProcesso(processo);
        Persona persona = Persona.resolve(usuario.getTipoUsuario());
        if (persona == Persona.NAO_SUPORTADA) {
            throw ProcessualParticipacaoAtivaSupportUtils.validation("Perfil atual não opera peticionamento ativo neste workspace.");
        }
        SignaturePolicy signaturePolicy = workspaceSupport.buildSignaturePolicy(persona, processo);
        CapabilityMatrix capabilityMatrix = workspaceSupport.buildCapabilityMatrix(processo, usuario, persona);
        RoutingView routing = workspaceSupport.buildRouting(processo, persona, usuario);
        RepresentationGuardView representacao = workspaceSupport.buildRepresentationGuard(processo, usuario, persona, null, false);
        List<PendingView> pendencias = workspaceSupport.buildPendingViews(processo, usuario, persona, capabilityMatrix);
        DeadlineGuardView prazo = workspaceSupport.buildDeadlineGuard(processo, capabilityMatrix.actions(), pendencias, false, null);
        SecurityGuardView seguranca = workspaceSupport.buildSecurityGuard(processo, signaturePolicy, null, null);
        List<SubmissionView> recentes = workspaceSupport.buildRecentSubmissions(processo, usuario, capabilityMatrix, 12);
        ExperienceDifferentialView diferencial = workspaceSupport.buildExperienceDifferential(processo, persona, capabilityMatrix, signaturePolicy, routing, pendencias);
        ProcessIdentityView identity = new ProcessIdentityView(
                processo.getId(),
                ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero()),
                processo.getTribunal(),
                processo.getVara(),
                processo.getComarca(),
                processo.getUf(),
                processo.getUnidadeJudiciariaCodigo(),
                ProcessualParticipacaoAtivaSupportUtils.safeName(processo.getRamoDireito()),
                ProcessualParticipacaoAtivaSupportUtils.safeName(processo.getRito()),
                ProcessualParticipacaoAtivaSupportUtils.safeName(processo.getFaseAtual()),
                ProcessualParticipacaoAtivaSupportUtils.safeName(processo.getStatusProcesso()),
                ProcessualParticipacaoAtivaSupportUtils.resolveSigilo(processo).name(),
                processo.getValorCausa(),
                processo.getParteAutoraNome(),
                processo.getParteReuNome()
        );
        return new WorkspaceView(
                identity,
                persona.name(),
                persona.label(),
                usuario.getTipoUsuario() == null ? "NAO_INFORMADO" : usuario.getTipoUsuario().name(),
                capabilityMatrix.capacities(),
                capabilityMatrix.actions(),
                signaturePolicy,
                representacao,
                seguranca,
                prazo,
                routing,
                pendencias,
                recentes,
                diferencial,
                Instant.now()
        );
    }

    @PjbTransactionalBudget(operation = "processual.participacao-ativa.protocolar", maxMillis = 3000)
    @Transactional
    public SubmissionResponse protocolar(Long processoId, @Valid SubmissionRequest request) {
        Objects.requireNonNull(request, "request");
        Processo processo = loadProcesso(processoId);
        Usuario usuario = currentUserService.getRequired();
        authorizationService.requireWriteProcesso(processo);
        Persona persona = Persona.resolve(usuario.getTipoUsuario());
        if (persona == Persona.NAO_SUPORTADA) {
            throw ProcessualParticipacaoAtivaSupportUtils.validation("Perfil atual não pode protocolar dentro deste workspace.");
        }

        defensoriaInstitutionalCompetenceGuardService.requireAllowedForProcessParticipation(processo);

        CapabilityMatrix capabilityMatrix = workspaceSupport.buildCapabilityMatrix(processo, usuario, persona);
        ActionProfile action = capabilityMatrix.findAction(ProcessualParticipacaoAtivaSupportUtils.requireText(request.codigoAcao(), "codigoAcao"))
                .orElseThrow(() -> ProcessualParticipacaoAtivaSupportUtils.validation("A ação solicitada não está disponível para o perfil ou para a fase atual."));

        SignaturePolicy signaturePolicy = workspaceSupport.buildSignaturePolicy(persona, processo);
        RepresentationGuardView representacao = workspaceSupport.buildRepresentationGuard(processo, usuario, persona, request, true);
        SecurityGuardView seguranca = workspaceSupport.buildSecurityGuard(processo, signaturePolicy, action, request);
        validateRepresentation(persona, representacao);
        validateSignature(request, signaturePolicy);
        validateSecurity(request, seguranca, persona);
        validateLinkageToWorkItem(request, processo, capabilityMatrix);

        PreparedPrimaryDocument primary = submissionSupport.preparePrimaryDocument(processo, usuario, persona, action, request);
        List<PreparedAttachment> attachments = submissionSupport.prepareAttachments(processo, usuario, persona, action, request.anexos());
        submissionSupport.ensureNoDuplicate(primary, attachments);

        List<DocumentoProcessual> documentosParaSalvar = new ArrayList<>(1 + attachments.size());
        documentosParaSalvar.add(primary.documento());
        attachments.forEach(item -> documentosParaSalvar.add(item.documento()));
        List<DocumentoProcessual> saved = submissionSupport.saveAll(documentosParaSalvar);

        DocumentoProcessual savedPrimary = saved.getFirst();
        List<JudiciarioEventSourcingEngine.DocumentoMetadata> docsMeta = saved.stream()
                .map(doc -> new JudiciarioEventSourcingEngine.DocumentoMetadata(
                        doc.getId(),
                        doc.getStorageUri(),
                        ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(doc.getSha384(), doc.getSha256()),
                        doc.getTamanhoBytes() == null ? 0L : doc.getTamanhoBytes()))
                .toList();
        JudiciarioEventSourcingEngine.DocumentosJuntados payload = new JudiciarioEventSourcingEngine.DocumentosJuntados(
                UUID.randomUUID(),
                processo.getId(),
                docsMeta,
                Instant.now());
        var appendResult = processEventStore.append(processo.getId(), ProcessEventType.DOCUMENTS_BULK_ADDED, payload);

        Map<String, Object> movimento = new LinkedHashMap<>();
        movimento.put("acao", action.code());
        movimento.put("rotuloAcao", action.label());
        movimento.put("perfilAtor", persona.name());
        movimento.put("usuarioId", usuario.getId());
        movimento.put("usuarioNome", ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(usuario.getNome(), usuario.getEmail(), usuario.getCpf()));
        movimento.put("documentoPrincipalId", savedPrimary.getId().toString());
        movimento.put("documentosLote", saved.size());
        movimento.put("sigilo", ProcessualParticipacaoAtivaSupportUtils.resolveRequestedSigilo(request, processo, action).name());
        movimento.put("urgente", Boolean.TRUE.equals(request.urgente()));
        movimento.put("assinaturaModo", ProcessualParticipacaoAtivaSupportUtils.normalizeToken(request.assinaturaModo()));
        movimento.put("certificadoSerial", ProcessualParticipacaoAtivaSupportUtils.redactCertificate(request.certificadoSerial()));
        movimento.put("certificadoFingerprint", ProcessualParticipacaoAtivaSupportUtils.redactCertificate(request.certificadoFingerprint()));
        movimento.put("representacaoStatus", representacao.status());
        movimento.put("representacaoInstrumento", representacao.resolvedInstrument());
        movimento.put("securityClass", seguranca.classificacao());
        processEventStore.append(processo.getId(), ProcessEventType.MOVEMENT_RECORDED, movimento);

        WorkItem recepcao = openReceptionWorkItem(processo, usuario, persona, action, request, appendResult.getSeq(), savedPrimary);
        List<SubmissionDocumentView> documents = saved.stream()
                .map(doc -> new SubmissionDocumentView(
                        doc.getId().toString(),
                        doc.getTitulo(),
                        doc.getContentType(),
                        doc.getTamanhoBytes(),
                        ProcessualParticipacaoAtivaSupportUtils.safeName(doc.getCategoria()),
                        ProcessualParticipacaoAtivaSupportUtils.safeName(doc.getNivelSigilo()),
                        doc.getStorageUri()))
                .toList();

        return new SubmissionResponse(
                processo.getId(),
                appendResult.getSeq(),
                payload.eventoId().toString(),
                recepcao == null ? null : recepcao.getId(),
                recepcao == null ? null : recepcao.getInboxKey(),
                recepcao == null ? null : recepcao.getQueueCode(),
                action.code(),
                action.label(),
                documents,
                signaturePolicy,
                representacao,
                seguranca,
                workspaceSupport.buildDeadlineGuard(processo, List.of(action), List.of(), Boolean.TRUE.equals(request.urgente()), request.referenciaPrazo()),
                new SubmissionAuditView(
                        usuario.getId(),
                        usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name(),
                        ProcessualParticipacaoAtivaSupportUtils.redactCertificate(request.certificadoSerial()),
                        ProcessualParticipacaoAtivaSupportUtils.normalizeToken(request.assinaturaModo()),
                        payload.timestamp(),
                        submissionSupport.summarizeAck(action, recepcao, saved.size())),
                List.of(
                        "PROTOCOLO_INTERNO_UNIFICADO",
                        "JUNTADA_EM_LOTE_COM_HASH",
                        "TRIAGEM_TOPOLOGICA_AUTOMATICA",
                        "SEGREGACAO_POR_PERFIL_E_FASE",
                        "REPRESENTACAO_E_SIGILO_ENDURECIDOS"
                )
        );
    }

    @Transactional(readOnly = true)
    public List<SubmissionView> listarMinhasSubmissoes(Long processoId, int limit) {
        Processo processo = loadProcesso(processoId);
        Usuario usuario = currentUserService.getRequired();
        authorizationService.requireWriteProcesso(processo);
        Persona persona = Persona.resolve(usuario.getTipoUsuario());
        CapabilityMatrix matrix = workspaceSupport.buildCapabilityMatrix(processo, usuario, persona);
        return workspaceSupport.buildRecentSubmissions(processo, usuario, matrix, Math.max(1, Math.min(50, limit)));
    }

    private WorkItem openReceptionWorkItem(Processo processo,
                                           Usuario usuario,
                                           Persona persona,
                                           ActionProfile action,
                                           SubmissionRequest request,
                                           Long eventSeq,
                                           DocumentoProcessual primary) {
        String templateCode = ProcessualParticipacaoAtivaSupportUtils.receptionTemplateCode(persona, action, primary);
        Optional<WorkItem> existing = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(
                processo.getId(),
                templateCode,
                WorkItemStatus.CANCELADO);
        if (existing.isPresent()) {
            return existing.get();
        }
        RoutingView routing = workspaceSupport.buildRouting(processo, persona, usuario);
        WorkItem item = WorkItem.builder()
                .processo(entityManager.getReference(Processo.class, processo.getId()))
                .faseOrigem(processo.getFaseAtual())
                .templateCode(templateCode)
                .type(action.workItemType())
                .titulo(action.label() + " — recepção inteligente")
                .descricao(submissionSupport.buildReceptionDescription(processo, usuario, persona, action, request, eventSeq, primary))
                .queueCode(routing.queueCode())
                .inboxKey(routing.inboxKey())
                .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(Boolean.TRUE.equals(request.urgente()) ? 1 : action.defaultPriority())
                .blocking(action.blocking())
                .dueAt(ProcessualParticipacaoAtivaSupportUtils.resolveDueAt(Boolean.TRUE.equals(request.urgente()), processo.getFaseAtual()))
                .uf(ProcessualParticipacaoAtivaSupportUtils.normalizeUf(processo.getUf()))
                .comarca(ProcessualParticipacaoAtivaSupportUtils.trimToNull(processo.getComarca()))
                .baseLegal(buildBaseLegal(action, persona, processo))
                .build();
        return workItemRepository.save(item);
    }

    private void validateRepresentation(Persona persona, RepresentationGuardView representacao) {
        if (persona == Persona.ADVOCACIA_PRIVADA && !representacao.regularidadeSuficiente()) {
            String detalhe = representacao.alertas().isEmpty()
                    ? "Procuração e identificação profissional precisam estar conferidas para a advocacia privada."
                    : representacao.alertas().getFirst();
            throw ProcessualParticipacaoAtivaSupportUtils.validation("Representação processual insuficiente para protocolar nesta trilha. " + detalhe);
        }
    }

    private void validateSecurity(SubmissionRequest request, SecurityGuardView security, Persona persona) {
        String mode = ProcessualParticipacaoAtivaSupportUtils.normalizeToken(request.assinaturaModo());
        if (security.restritoAAtuacaoInstitucional() && !isInstitutionalOrPericial(persona, mode, request)) {
            throw ProcessualParticipacaoAtivaSupportUtils.validation("O nível de sigilo solicitado exige atuação institucional/pericial qualificada e assinatura forte rastreável.");
        }
        if (security.canalForteObrigatorio() && !security.modosAssinaturaFortes().contains(mode)) {
            throw ProcessualParticipacaoAtivaSupportUtils.validation("Sigilo ou sensibilidade do ato exige modo de assinatura forte.");
        }
        if (security.certificadoObrigatorio()
                && ProcessualParticipacaoAtivaSupportUtils.trimToNull(ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(request.certificadoSerial(), request.certificadoFingerprint())) == null) {
            throw ProcessualParticipacaoAtivaSupportUtils.validation("Sigilo elevado exige serial ou fingerprint de certificado para protocolar.");
        }
        if (security.stepUpObrigatorio() && !Boolean.TRUE.equals(request.stepUpConfirmado())) {
            throw ProcessualParticipacaoAtivaSupportUtils.validation("Esta submissão exige step-up confirmado antes do protocolo interno.");
        }
        if (mode.startsWith("ASSINATURA_PJB")
                && ProcessualParticipacaoAtivaSupportUtils.trimToNull(ProcessualParticipacaoAtivaSupportUtils.firstNonBlank(request.attestationId(), request.deviceBindingId())) == null) {
            throw ProcessualParticipacaoAtivaSupportUtils.validation("Assinatura PJB governada exige attestation ou device binding informado.");
        }
    }

    private boolean isInstitutionalOrPericial(Persona persona, String mode, SubmissionRequest request) {
        if (persona == Persona.ADVOCACIA_PRIVADA || persona == Persona.NAO_SUPORTADA) {
            return false;
        }
        return "CERTIFICADO_INSTITUCIONAL".equals(mode)
                || "ASSINATURA_PJB_INSTITUCIONAL".equals(mode)
                || (ProcessualParticipacaoAtivaSupportUtils.trimToNull(request.certificadoSerial()) != null && persona != Persona.ADVOCACIA_PRIVADA);
    }

    private void validateSignature(SubmissionRequest request, SignaturePolicy signaturePolicy) {
        String mode = ProcessualParticipacaoAtivaSupportUtils.normalizeToken(request.assinaturaModo());
        if (mode.isBlank()) {
            throw ProcessualParticipacaoAtivaSupportUtils.validation("O modo de assinatura deve ser informado para protocolar no workspace.");
        }
        if (!signaturePolicy.admissibleModes().contains(mode)) {
            throw ProcessualParticipacaoAtivaSupportUtils.validation("Modo de assinatura incompatível com o perfil ou com a política ativa.");
        }
        if (signaturePolicy.certificateRequired() && ProcessualParticipacaoAtivaSupportUtils.trimToNull(request.certificadoSerial()) == null) {
            throw ProcessualParticipacaoAtivaSupportUtils.validation("Este perfil exige serial ou identificador de certificado institucional para protocolar.");
        }
    }

    private void validateLinkageToWorkItem(SubmissionRequest request,
                                           Processo processo,
                                           CapabilityMatrix capabilityMatrix) {
        if (request.workItemVinculadoId() == null) {
            return;
        }
        WorkItem workItem = workItemRepository.findById(request.workItemVinculadoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("WorkItem", request.workItemVinculadoId()));
        if (!Objects.equals(workItem.getProcessoId(), processo.getId())) {
            throw new ErroDeValidacaoException(TipoErroValidacao.VINCULO_INVALIDO, "O work item informado pertence a outro processo.");
        }
        if (workItem.getStatus() == WorkItemStatus.CANCELADO || workItem.getStatus() == WorkItemStatus.CONCLUIDO) {
            throw ProcessualParticipacaoAtivaSupportUtils.validation("O work item vinculado não está mais disponível para resposta ativa.");
        }
        capabilityMatrix.closestActionFor(workItem.getType())
                .orElseThrow(() -> ProcessualParticipacaoAtivaSupportUtils.validation("O work item informado não dialoga com as ações do perfil atual."));
    }

    private String buildBaseLegal(ActionProfile action, Persona persona, Processo processo) {
        List<String> base = new ArrayList<>();
        base.add("Atuação digital dentro do próprio processo com segregação de perfil, fase e sigilo.");
        if (persona == Persona.MINISTERIO_PUBLICO) {
            base.add("Manifestação institucional do Ministério Público em trilha própria.");
        }
        if (persona == Persona.DEFENSORIA) {
            base.add("Defesa técnica e atuação protetiva do assistido com prioridade qualificada.");
        }
        if (persona == Persona.PROCURADORIA) {
            base.add("Atuação fazendária e institucional do ente público com governança própria.");
        }
        if (persona == Persona.PERICIA) {
            base.add("Entrega técnica pericial e esclarecimentos em cadeia documental controlada.");
        }
        base.add("Ação solicitada: " + action.label() + ". Fase: " + ProcessualParticipacaoAtivaSupportUtils.safeName(processo.getFaseAtual()) + '.');
        return String.join(" ", base);
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }
}
