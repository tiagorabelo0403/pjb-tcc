package com.tcc.pjb.backend.service.professional;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantApprovalStatus;
import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantEventType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalProcessAccessVector;
import com.tcc.pjb.backend.core.security.professional.ProfessionalProcessAccessVectorService;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicPanelLinkDto;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantAdminWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantBatchDecisionRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantBatchIssueRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantBatchItemRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantBatchOperationResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantDecisionRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantDetailResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantEventDto;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantGovernanceFilterDto;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantGovernanceSummaryDto;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantGovernanceWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantIssueRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantOperationalQueueResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantTemplateBatchIssueRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantTemplateBatchItemRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantTemplateCatalogResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantTemplateDto;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantProcessTimelineResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantQueueItemDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalAccessGrantTemplate;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrantEvent;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalAccessGrantTemplateRepository;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantEventRepository;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfessionalInstitutionalAccessGrantAdminService {

    private final CurrentUserService currentUserService;
    private final UsuarioRepository usuarioRepository;
    private final ProcessoRepository processoRepository;
    private final ProfessionalInstitutionalAccessGrantRepository grantRepository;
    private final ProfessionalInstitutionalAccessGrantEventRepository eventRepository;
    private final ProfessionalAccessGrantTemplateRepository templateRepository;
    private final ProfessionalProcessAccessVectorService accessVectorService;
    private final AuditLedgerService auditLedgerService;

    public ProfessionalInstitutionalAccessGrantAdminService(CurrentUserService currentUserService,
                                                            UsuarioRepository usuarioRepository,
                                                            ProcessoRepository processoRepository,
                                                            ProfessionalInstitutionalAccessGrantRepository grantRepository,
                                                            ProfessionalInstitutionalAccessGrantEventRepository eventRepository,
                                                            ProfessionalAccessGrantTemplateRepository templateRepository,
                                                            ProfessionalProcessAccessVectorService accessVectorService,
                                                            AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.grantRepository = Objects.requireNonNull(grantRepository);
        this.eventRepository = Objects.requireNonNull(eventRepository);
        this.templateRepository = Objects.requireNonNull(templateRepository);
        this.accessVectorService = Objects.requireNonNull(accessVectorService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public ProfessionalGrantAdminWorkspaceResponse workspace() {
        Usuario actor = currentUserService.getRequired();
        ProfessionalActorClass managerClass = accessVectorService.actorClass(actor);
        List<ProfessionalGrantQueueItemDto> pendingApprovals = grantRepository.findTop50ByApprovalStatusOrderByIdDesc(ProfessionalGrantApprovalStatus.PENDING).stream()
                .filter(item -> canManage(actor, managerClass, item))
                .map(this::toQueueItem)
                .toList();
        List<ProfessionalGrantQueueItemDto> myRecentRequests = grantRepository.findTop50ByRequestedByUserIdOrderByIdDesc(actor.getId()).stream()
                .map(this::toQueueItem)
                .toList();
        List<ProfessionalGrantQueueItemDto> activeProcessScopedGrants = grantRepository.findTop200ByUsuario_IdAndAtivoTrueOrderByIdDesc(actor.getId()).stream()
                .filter(ProfessionalInstitutionalAccessGrant::isApproved)
                .filter(item -> item.getProcesso() != null)
                .limit(20)
                .map(this::toQueueItem)
                .toList();
        List<String> warnings = new ArrayList<>();
        if (!isManager(actor, managerClass)) {
            warnings.add("O perfil atual pode visualizar a trilha de grants próprios, mas não possui competência para aprovar designações ou delegações institucionais.");
        }
        return new ProfessionalGrantAdminWorkspaceResponse(
                LocalDateTime.now(),
                managerClass.name(),
                safe(actor.getNome()),
                manageableGrantTypes(managerClass, actor),
                pendingApprovals,
                myRecentRequests,
                activeProcessScopedGrants,
                List.of(
                        new ProfessionalForensicPanelLinkDto("GRANT_WORKSPACE", "Abrir gestão de grants", "/api/v1/professional/access-grants/workspace", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("GOVERNANCE_DASHBOARD", "Abrir dashboard superior de grants", "/api/v1/professional/access-grants/governance-dashboard", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("GRANT_REQUEST", "Emitir solicitação de grant", "/api/v1/professional/access-grants/requests", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("OPERATIONAL_DASHBOARD", "Abrir fila operacional", "/api/v1/professional/access-grants/operational-dashboard", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("TEMPLATE_CATALOG", "Abrir catálogo de templates", "/api/v1/professional/access-grants/templates", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("TEMPLATE_BATCH_REQUEST", "Emitir grants por template", "/api/v1/professional/access-grants/template-batch-requests", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("BATCH_REQUEST", "Emitir lote de grants", "/api/v1/professional/access-grants/batch-requests", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("FORENSIC_WORKSPACE", "Voltar ao painel forense", "/api/v1/professional/forensic-panel/workspace", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("GRANT_TIMELINE", "Abrir timeline processual de grants", "/api/v1/professional/access-grants/processos/{numero}/timeline", "SECONDARY")
                ),
                warnings
        );
    }

    @Transactional
    public ProfessionalGrantDetailResponse issue(ProfessionalGrantIssueRequest request) {
        Usuario actor = currentUserService.getRequired();
        ProfessionalActorClass managerClass = accessVectorService.actorClass(actor);
        if (request == null || request.targetUserId() == null || request.actorClass() == null || request.grantType() == null || request.accessBasis() == null) {
            throw new IllegalArgumentException("Solicitação de grant incompleta.");
        }
        assertManager(actor, managerClass);
        Usuario targetUser = usuarioRepository.findById(request.targetUserId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", String.valueOf(request.targetUserId())));
        ProfessionalActorClass targetClass = accessVectorService.actorClass(targetUser);
        if (targetClass != request.actorClass()) {
            throw new IllegalArgumentException("Classe profissional do alvo incompatível com o grant solicitado.");
        }
        ProfessionalInstitutionalAccessGrant grant = new ProfessionalInstitutionalAccessGrant();
        Processo processo = resolveProcesso(request.processoNumero());
        grant.setUsuario(targetUser);
        grant.setProcesso(processo);
        grant.setActorClass(request.actorClass());
        grant.setGrantType(request.grantType());
        grant.setAccessBasis(request.accessBasis());
        grant.setUf(normalizeToken(request.uf()));
        grant.setComarca(normalizeText(request.comarca()));
        grant.setTribunal(normalizeToken(request.tribunal()));
        grant.setUnidadeJudiciariaCodigo(normalizeToken(request.unidadeJudiciariaCodigo()));
        grant.setOrgaoColegiadoCodigo(normalizeToken(request.orgaoColegiadoCodigo()));
        grant.setEnteCode(normalizeToken(request.enteCode()));
        grant.setTargetMagistrateUserId(resolveTargetMagistrate(actor, managerClass, request));
        grant.setSourceRef(normalizeText(request.sourceRef()));
        grant.setSourceLabel(normalizeText(request.sourceLabel()));
        grant.setReason(normalizeText(request.reason()));
        grant.setRequiresStepUp(Boolean.TRUE.equals(request.requiresStepUp()));
        grant.setAtivo(Boolean.TRUE);
        grant.setApprovalStatus(ProfessionalGrantApprovalStatus.PENDING);
        grant.setInicioVigencia(request.inicioVigencia() == null ? LocalDateTime.now() : request.inicioVigencia());
        grant.setFimVigencia(request.fimVigencia());
        grant.setRequestedByUserId(actor.getId());
        grant.setRequestedByName(actor.getNome());
        grant.setRequestedAt(LocalDateTime.now());
        assertGrantSemantics(actor, managerClass, grant);
        grant = grantRepository.save(grant);
        appendEvent(grant, ProfessionalGrantEventType.REQUESTED, null, ProfessionalGrantApprovalStatus.PENDING, actor, detailForEvent(grant));
        auditLedgerService.appendSafely("PROFESSIONAL_GRANT_REQUESTED", "PROFESSIONAL_ACCESS_GRANT", String.valueOf(grant.getId()), "{\"grantType\":\"" + grant.getGrantType().name() + "\",\"targetUserId\":" + targetUser.getId() + "}");
        return detail(grant.getId());
    }

    @Transactional
    public ProfessionalGrantDetailResponse approve(Long grantId, ProfessionalGrantDecisionRequest request) {
        ProfessionalInstitutionalAccessGrant grant = requireGrant(grantId);
        Usuario actor = currentUserService.getRequired();
        ProfessionalActorClass managerClass = accessVectorService.actorClass(actor);
        assertCanManage(actor, managerClass, grant);
        if (!grant.isPending()) {
            throw new IllegalStateException("Somente grants pendentes podem ser aprovados.");
        }
        ProfessionalGrantApprovalStatus previous = grant.getApprovalStatus();
        grant.setApprovalStatus(ProfessionalGrantApprovalStatus.APPROVED);
        grant.setAtivo(Boolean.TRUE);
        grant.setApprovedByUserId(actor.getId());
        grant.setApprovedByName(actor.getNome());
        grant.setApprovedAt(LocalDateTime.now());
        grant.setDecisionReason(normalizeText(request == null ? null : request.reason()));
        grantRepository.save(grant);
        appendEvent(grant, ProfessionalGrantEventType.APPROVED, previous, ProfessionalGrantApprovalStatus.APPROVED, actor, normalizeText(request == null ? null : request.reason()));
        auditLedgerService.appendSafely("PROFESSIONAL_GRANT_APPROVED", "PROFESSIONAL_ACCESS_GRANT", String.valueOf(grant.getId()), "{\"grantType\":\"" + grant.getGrantType().name() + "\"}");
        return detail(grantId);
    }

    @Transactional
    public ProfessionalGrantDetailResponse reject(Long grantId, ProfessionalGrantDecisionRequest request) {
        ProfessionalInstitutionalAccessGrant grant = requireGrant(grantId);
        Usuario actor = currentUserService.getRequired();
        ProfessionalActorClass managerClass = accessVectorService.actorClass(actor);
        assertCanManage(actor, managerClass, grant);
        if (!grant.isPending()) {
            throw new IllegalStateException("Somente grants pendentes podem ser rejeitados.");
        }
        ProfessionalGrantApprovalStatus previous = grant.getApprovalStatus();
        grant.setApprovalStatus(ProfessionalGrantApprovalStatus.REJECTED);
        grant.setAtivo(Boolean.FALSE);
        grant.setDecisionReason(normalizeText(request == null ? null : request.reason()));
        grantRepository.save(grant);
        appendEvent(grant, ProfessionalGrantEventType.REJECTED, previous, ProfessionalGrantApprovalStatus.REJECTED, actor, normalizeText(request == null ? null : request.reason()));
        auditLedgerService.appendSafely("PROFESSIONAL_GRANT_REJECTED", "PROFESSIONAL_ACCESS_GRANT", String.valueOf(grant.getId()), "{\"grantType\":\"" + grant.getGrantType().name() + "\"}");
        return detail(grantId);
    }

    @Transactional
    public ProfessionalGrantDetailResponse revoke(Long grantId, ProfessionalGrantDecisionRequest request) {
        ProfessionalInstitutionalAccessGrant grant = requireGrant(grantId);
        Usuario actor = currentUserService.getRequired();
        ProfessionalActorClass managerClass = accessVectorService.actorClass(actor);
        assertCanManage(actor, managerClass, grant);
        if (grant.getApprovalStatus() == ProfessionalGrantApprovalStatus.REVOKED) {
            throw new IllegalStateException("Grant já revogado.");
        }
        ProfessionalGrantApprovalStatus previous = grant.getApprovalStatus();
        grant.setApprovalStatus(ProfessionalGrantApprovalStatus.REVOKED);
        grant.setAtivo(Boolean.FALSE);
        grant.setRevokedByUserId(actor.getId());
        grant.setRevokedByName(actor.getNome());
        grant.setRevokedAt(LocalDateTime.now());
        grant.setFimVigencia(grant.getFimVigencia() == null ? LocalDateTime.now() : grant.getFimVigencia());
        grant.setDecisionReason(normalizeText(request == null ? null : request.reason()));
        grantRepository.save(grant);
        appendEvent(grant, ProfessionalGrantEventType.REVOKED, previous, ProfessionalGrantApprovalStatus.REVOKED, actor, normalizeText(request == null ? null : request.reason()));
        auditLedgerService.appendSafely("PROFESSIONAL_GRANT_REVOKED", "PROFESSIONAL_ACCESS_GRANT", String.valueOf(grant.getId()), "{\"grantType\":\"" + grant.getGrantType().name() + "\"}");
        return detail(grantId);
    }

    @Transactional(readOnly = true)
    public ProfessionalGrantDetailResponse detail(Long grantId) {
        ProfessionalInstitutionalAccessGrant grant = requireGrant(grantId);
        Usuario actor = currentUserService.getRequired();
        ProfessionalActorClass managerClass = accessVectorService.actorClass(actor);
        if (!canManage(actor, managerClass, grant) && !Objects.equals(actor.getId(), grant.getUsuario().getId()) && !Objects.equals(actor.getId(), grant.getRequestedByUserId())) {
            throw new AccessDeniedPjbException("Perfil atual sem autorização para inspecionar o grant solicitado.");
        }
        List<ProfessionalGrantEventDto> timeline = eventRepository.findTop100ByGrant_IdOrderByCreatedAtDesc(grant.getId()).stream()
                .sorted(Comparator.comparing(ProfessionalInstitutionalAccessGrantEvent::getCreatedAt))
                .map(this::toEvent)
                .toList();
        return new ProfessionalGrantDetailResponse(
                LocalDateTime.now(),
                toQueueItem(grant),
                timeline,
                List.of(
                        new ProfessionalForensicPanelLinkDto("GRANT_WORKSPACE", "Voltar à gestão de grants", "/api/v1/professional/access-grants/workspace", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("FORENSIC_WORKSPACE", "Abrir painel forense", "/api/v1/professional/forensic-panel/workspace", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("PROCESS_TIMELINE", "Abrir timeline do processo", processRoute(grant), "SECONDARY")
                ),
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public ProfessionalGrantProcessTimelineResponse processTimeline(String numero) {
        Usuario actor = currentUserService.getRequired();
        Processo processo = resolveProcessoRequired(numero);
        ProfessionalProcessAccessVector vector = accessVectorService.resolve(actor, processo);
        if (!vector.allowed() && !isAdmin(actor)) {
            throw new AccessDeniedPjbException("Perfil atual sem base profissional para inspecionar a timeline de grants deste processo.");
        }
        List<ProfessionalInstitutionalAccessGrant> grants = grantRepository.findTop50ByProcesso_IdOrderByIdDesc(processo.getId());
        List<ProfessionalGrantEventDto> events = eventRepository.findTop200ByGrant_Processo_IdOrderByCreatedAtDesc(processo.getId()).stream()
                .sorted(Comparator.comparing(ProfessionalInstitutionalAccessGrantEvent::getCreatedAt))
                .map(this::toEvent)
                .toList();
        List<String> warnings = new ArrayList<>();
        if (grants.isEmpty()) {
            warnings.add("Nenhum grant formal foi encontrado para o processo informado.");
        }
        return new ProfessionalGrantProcessTimelineResponse(
                LocalDateTime.now(),
                resolveNumero(processo),
                grants.stream().map(this::toQueueItem).toList(),
                events,
                List.of(
                        new ProfessionalForensicPanelLinkDto("PROCESS_DETAIL", "Abrir detalhe profissional do processo", "/api/v1/professional/forensic-panel/processos/" + resolveNumero(processo), "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("ACCESS_MATRIX", "Abrir matriz de acesso", "/api/v1/professional/forensic-panel/processos/" + resolveNumero(processo) + "/access-matrix", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("GRANT_WORKSPACE", "Voltar à gestão de grants", "/api/v1/professional/access-grants/workspace", "SECONDARY")
                ),
                warnings
        );
    }

    @Transactional(readOnly = true)
    public ProfessionalGrantGovernanceWorkspaceResponse governanceDashboard(String status,
                                                                            String actorClass,
                                                                            String grantType,
                                                                            String uf,
                                                                            String comarca,
                                                                            String tribunal,
                                                                            String unidadeJudiciariaCodigo,
                                                                            String orgaoColegiadoCodigo,
                                                                            String enteCode,
                                                                            int limit) {
        Usuario actor = currentUserService.getRequired();
        ProfessionalActorClass managerClass = accessVectorService.actorClass(actor);
        assertManager(actor, managerClass);
        LocalDateTime now = LocalDateTime.now();
        int safeLimit = Math.max(10, Math.min(limit, 120));
        String normalizedStatus = normalizeToken(status);
        String normalizedActorClass = normalizeToken(actorClass);
        String normalizedGrantType = normalizeToken(grantType);
        String normalizedUf = normalizeToken(uf);
        String normalizedComarca = normalizeText(comarca);
        String normalizedTribunal = normalizeToken(tribunal);
        String normalizedUnidade = normalizeToken(unidadeJudiciariaCodigo);
        String normalizedColegiado = normalizeToken(orgaoColegiadoCodigo);
        String normalizedEnte = normalizeToken(enteCode);
        List<ProfessionalInstitutionalAccessGrant> manageable = grantRepository.findTop500ByOrderByIdDesc().stream()
                .filter(item -> canManage(actor, managerClass, item))
                .filter(item -> matchesGovernanceFilters(item, normalizedStatus, normalizedActorClass, normalizedGrantType, normalizedUf, normalizedComarca, normalizedTribunal, normalizedUnidade, normalizedColegiado, normalizedEnte))
                .toList();
        List<ProfessionalGrantQueueItemDto> pendingApprovals = manageable.stream()
                .filter(item -> item.getApprovalStatus() == ProfessionalGrantApprovalStatus.PENDING)
                .limit(safeLimit)
                .map(this::toQueueItem)
                .toList();
        List<ProfessionalGrantQueueItemDto> expiringSoon = manageable.stream()
                .filter(ProfessionalInstitutionalAccessGrant::isApproved)
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .filter(item -> item.getFimVigencia() != null)
                .filter(item -> !item.getFimVigencia().isBefore(now))
                .filter(item -> !item.getFimVigencia().isAfter(now.plusDays(15)))
                .sorted(Comparator.comparing(ProfessionalInstitutionalAccessGrant::getFimVigencia))
                .limit(safeLimit)
                .map(this::toQueueItem)
                .toList();
        List<ProfessionalGrantQueueItemDto> filteredGrants = manageable.stream()
                .limit(safeLimit)
                .map(this::toQueueItem)
                .toList();
        long activeNow = manageable.stream().filter(item -> item.isAtivoNaJanela(now)).count();
        long stepUpRequired = manageable.stream().filter(ProfessionalInstitutionalAccessGrant::requiresStepUp).count();
        long revokedCount = manageable.stream().filter(item -> item.getApprovalStatus() == ProfessionalGrantApprovalStatus.REVOKED).count();
        List<ProfessionalGrantGovernanceSummaryDto> summary = List.of(
                new ProfessionalGrantGovernanceSummaryDto("FILTERED_TOTAL", "Total filtrado", manageable.size(), "ACTIVE_BLUE"),
                new ProfessionalGrantGovernanceSummaryDto("PENDING", "Pendentes de homologação", pendingApprovals.size(), pendingApprovals.isEmpty() ? "STABLE_NEUTRAL" : "ATTENTION_ORANGE"),
                new ProfessionalGrantGovernanceSummaryDto("ACTIVE", "Ativos na janela", activeNow, activeNow == 0 ? "STABLE_NEUTRAL" : "ACTIVE_BLUE"),
                new ProfessionalGrantGovernanceSummaryDto("EXPIRING_SOON", "Expiram em até 15 dias", expiringSoon.size(), expiringSoon.isEmpty() ? "STABLE_NEUTRAL" : "ATTENTION_ORANGE"),
                new ProfessionalGrantGovernanceSummaryDto("STEP_UP", "Exigem step-up", stepUpRequired, stepUpRequired == 0 ? "STABLE_NEUTRAL" : "ATTENTION_ORANGE"),
                new ProfessionalGrantGovernanceSummaryDto("REVOKED", "Revogados no recorte", revokedCount, revokedCount == 0 ? "STABLE_NEUTRAL" : "CRITICAL_RED")
        );
        List<ProfessionalGrantGovernanceFilterDto> filters = List.of(
                new ProfessionalGrantGovernanceFilterDto("STATUS", "Status", safeLabel(normalizedStatus, "TODOS"), !blank(normalizedStatus), "Permite isolar grants pendentes, aprovados, rejeitados ou revogados."),
                new ProfessionalGrantGovernanceFilterDto("ACTOR_CLASS", "Classe alvo", safeLabel(normalizedActorClass, "TODAS"), !blank(normalizedActorClass), "Filtra advocacia, defensoria, procuradoria, magistratura ou apoio judicial."),
                new ProfessionalGrantGovernanceFilterDto("GRANT_TYPE", "Tipo de grant", safeLabel(normalizedGrantType, "TODOS"), !blank(normalizedGrantType), "Delimita relatoria, plantão, delegação de gabinete, representação e designação."),
                new ProfessionalGrantGovernanceFilterDto("UF", "UF", safeLabel(normalizedUf, "BR"), !blank(normalizedUf), "Refina a visão territorial superior do dashboard."),
                new ProfessionalGrantGovernanceFilterDto("COMARCA", "Comarca", safeLabel(normalizedComarca, "TODAS"), !blank(normalizedComarca), "Refina a malha de competência ou de atuação institucional."),
                new ProfessionalGrantGovernanceFilterDto("TRIBUNAL", "Tribunal", safeLabel(normalizedTribunal, "TODOS"), !blank(normalizedTribunal), "Filtra grants vinculados a tribunal específico."),
                new ProfessionalGrantGovernanceFilterDto("UNIDADE", "Unidade judiciária", safeLabel(normalizedUnidade, "TODAS"), !blank(normalizedUnidade), "Filtra vara, gabinete ou unidade de apoio."),
                new ProfessionalGrantGovernanceFilterDto("COLEGIADO", "Órgão colegiado", safeLabel(normalizedColegiado, "TODOS"), !blank(normalizedColegiado), "Refina grants de composição colegiada e instâncias recursais."),
                new ProfessionalGrantGovernanceFilterDto("ENTE", "Ente representado", safeLabel(normalizedEnte, "TODOS"), !blank(normalizedEnte), "Filtra grants de procuradoria por ente público vinculado.")
        );
        List<String> warnings = new ArrayList<>();
        if (manageable.isEmpty()) {
            warnings.add("Nenhum grant caiu no recorte superior informado; amplie o filtro territorial, o tipo de grant ou a classe alvo.");
        }
        if (manageable.size() > safeLimit) {
            warnings.add("O recorte retornou mais grants do que a janela atual do dashboard; refine filtros por órgão, comarca, unidade ou status para leitura operacional mais precisa.");
        }
        if (pendingApprovals.isEmpty()) {
            warnings.add("Não há pendências de homologação no recorte atual.");
        }
        return new ProfessionalGrantGovernanceWorkspaceResponse(
                now,
                managerClass.name(),
                safe(actor.getNome()),
                filters,
                summary,
                pendingApprovals,
                expiringSoon,
                filteredGrants,
                List.of(
                        new ProfessionalForensicPanelLinkDto("GOVERNANCE_DASHBOARD", "Atualizar dashboard superior", "/api/v1/professional/access-grants/governance-dashboard", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("GRANT_WORKSPACE", "Voltar à gestão operacional de grants", "/api/v1/professional/access-grants/workspace", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("BATCH_REQUEST", "Emitir grants em lote", "/api/v1/professional/access-grants/batch-requests", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("OPERATIONAL_DASHBOARD", "Abrir fila operacional", "/api/v1/professional/access-grants/operational-dashboard", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("TEMPLATE_CATALOG", "Abrir catálogo de templates", "/api/v1/professional/access-grants/templates", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("TEMPLATE_BATCH_REQUEST", "Emitir grants por template", "/api/v1/professional/access-grants/template-batch-requests", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("BATCH_APPROVE", "Homologar grants em lote", "/api/v1/professional/access-grants/batch-approve", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("BATCH_REVOKE", "Revogar grants em lote", "/api/v1/professional/access-grants/batch-revoke", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("FORENSIC_WORKSPACE", "Abrir painel forense conectado", "/api/v1/professional/forensic-panel/workspace", "SECONDARY")
                ),
                warnings
        );
    }

    @Transactional
    @PjbTransactionalBudget(operation = "professional.access-grant.issue-batch", maxMillis = 15000)
    public ProfessionalGrantBatchOperationResponse issueBatch(ProfessionalGrantBatchIssueRequest request) {
        Usuario actor = currentUserService.getRequired();
        ProfessionalActorClass managerClass = accessVectorService.actorClass(actor);
        assertManager(actor, managerClass);
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Lote de grants vazio.");
        }
        int requestedCount = request.items().size();
        int safeLimit = Math.min(requestedCount, 50);
        List<ProfessionalGrantQueueItemDto> processed = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int index = 0;
        for (ProfessionalGrantBatchItemRequest item : request.items().stream().limit(safeLimit).toList()) {
            index++;
            try {
                ProfessionalGrantDetailResponse detail = issue(toIssueRequest(item));
                if (request.autoApprove()) {
                    detail = approve(detail.grant().grantId(), new ProfessionalGrantDecisionRequest(firstNonBlank("Lote homologado", request.batchLabel(), item.reason())));
                }
                processed.add(detail.grant());
            } catch (RuntimeException ex) {
                errors.add("Item " + index + ": " + ex.getMessage());
            }
        }
        if (requestedCount > safeLimit) {
            errors.add("Lote truncado no limite operacional de 50 grants por emissão massiva.");
        }
        auditLedgerService.appendSafely("PROFESSIONAL_GRANT_BATCH_REQUESTED", "PROFESSIONAL_ACCESS_GRANT", safe(request.batchLabel()), "{\"requestedCount\":" + requestedCount + ",\"succeededCount\":" + processed.size() + ",\"autoApprove\":" + request.autoApprove() + "}");
        return new ProfessionalGrantBatchOperationResponse(
                LocalDateTime.now(),
                request.autoApprove() ? "BATCH_REQUEST_AND_APPROVE" : "BATCH_REQUEST",
                normalizeText(request.batchLabel()),
                requestedCount,
                processed.size(),
                errors.size(),
                processed,
                errors,
                List.of(
                        new ProfessionalForensicPanelLinkDto("GOVERNANCE_DASHBOARD", "Abrir dashboard superior", "/api/v1/professional/access-grants/governance-dashboard", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("GRANT_WORKSPACE", "Voltar à gestão de grants", "/api/v1/professional/access-grants/workspace", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("FORENSIC_WORKSPACE", "Abrir painel forense", "/api/v1/professional/forensic-panel/workspace", "SECONDARY")
                )
        );
    }

    @Transactional
    public ProfessionalGrantBatchOperationResponse approveBatch(ProfessionalGrantBatchDecisionRequest request) {
        return decideBatch("BATCH_APPROVE", request, true);
    }

    @Transactional
    public ProfessionalGrantBatchOperationResponse revokeBatch(ProfessionalGrantBatchDecisionRequest request) {
        return decideBatch("BATCH_REVOKE", request, false);
    }

    @Transactional(readOnly = true)
    public ProfessionalGrantOperationalQueueResponse operationalDashboard(String gabineteCodigo,
                                                                          String unidadeJudiciariaCodigo,
                                                                          String orgaoColegiadoCodigo,
                                                                          String enteCode,
                                                                          boolean criticalOnly,
                                                                          int limit) {
        Usuario actor = currentUserService.getRequired();
        ProfessionalActorClass managerClass = accessVectorService.actorClass(actor);
        assertManager(actor, managerClass);
        LocalDateTime now = LocalDateTime.now();
        int safeLimit = Math.max(10, Math.min(limit, 80));
        String normalizedGabinete = normalizeToken(gabineteCodigo);
        String normalizedUnidade = normalizeToken(unidadeJudiciariaCodigo);
        String normalizedColegiado = normalizeToken(orgaoColegiadoCodigo);
        String normalizedEnte = normalizeToken(enteCode);
        List<ProfessionalInstitutionalAccessGrant> manageable = grantRepository.findTop500ByOrderByIdDesc().stream()
                .filter(item -> canManage(actor, managerClass, item))
                .filter(item -> matchesOperationalFilters(item, normalizedGabinete, normalizedUnidade, normalizedColegiado, normalizedEnte))
                .toList();
        List<ProfessionalInstitutionalAccessGrant> criticalBase = criticalOnly
                ? manageable.stream().filter(item -> isCriticalPending(item, now) || isExpiringImminent(item, now) || isStepUpQueueItem(item, now)).toList()
                : manageable;
        List<ProfessionalGrantQueueItemDto> criticalPending = criticalBase.stream()
                .filter(item -> isCriticalPending(item, now))
                .sorted(Comparator.comparing((ProfessionalInstitutionalAccessGrant item) -> item.getRequestedAt() == null ? LocalDateTime.MIN : item.getRequestedAt()))
                .limit(safeLimit)
                .map(this::toQueueItem)
                .toList();
        List<ProfessionalGrantQueueItemDto> expiringImminent = criticalBase.stream()
                .filter(item -> isExpiringImminent(item, now))
                .sorted(Comparator.comparing(ProfessionalInstitutionalAccessGrant::getFimVigencia))
                .limit(safeLimit)
                .map(this::toQueueItem)
                .toList();
        List<ProfessionalGrantQueueItemDto> stepUpQueue = criticalBase.stream()
                .filter(item -> isStepUpQueueItem(item, now))
                .sorted(Comparator.comparing((ProfessionalInstitutionalAccessGrant item) -> item.getFimVigencia() == null ? LocalDateTime.MAX : item.getFimVigencia()))
                .limit(safeLimit)
                .map(this::toQueueItem)
                .toList();
        long pendingCount = manageable.stream().filter(item -> item.getApprovalStatus() == ProfessionalGrantApprovalStatus.PENDING).count();
        long criticalCount = manageable.stream().filter(item -> isCriticalPending(item, now)).count();
        long imminentCount = manageable.stream().filter(item -> isExpiringImminent(item, now)).count();
        long stepUpCount = manageable.stream().filter(item -> isStepUpQueueItem(item, now)).count();
        List<ProfessionalGrantGovernanceSummaryDto> summary = List.of(
                new ProfessionalGrantGovernanceSummaryDto("VISIBLE", "Janela operacional visível", manageable.size(), manageable.isEmpty() ? "STABLE_NEUTRAL" : "ACTIVE_BLUE"),
                new ProfessionalGrantGovernanceSummaryDto("PENDING", "Pendências operacionais", pendingCount, pendingCount == 0 ? "STABLE_NEUTRAL" : "ATTENTION_ORANGE"),
                new ProfessionalGrantGovernanceSummaryDto("CRITICAL_PENDING", "Pendências críticas", criticalCount, criticalCount == 0 ? "STABLE_NEUTRAL" : "CRITICAL_RED"),
                new ProfessionalGrantGovernanceSummaryDto("EXPIRING_IMMINENT", "Expiração iminente", imminentCount, imminentCount == 0 ? "STABLE_NEUTRAL" : "ATTENTION_ORANGE"),
                new ProfessionalGrantGovernanceSummaryDto("STEP_UP_QUEUE", "Fila de step-up", stepUpCount, stepUpCount == 0 ? "STABLE_NEUTRAL" : "ATTENTION_ORANGE")
        );
        List<ProfessionalGrantGovernanceFilterDto> filters = List.of(
                new ProfessionalGrantGovernanceFilterDto("GABINETE", "Gabinete", safeLabel(normalizedGabinete, "TODOS"), !blank(normalizedGabinete), "Usa âncora de gabinete, source ref ou unidade correlata para isolar a fila jurisdicional."),
                new ProfessionalGrantGovernanceFilterDto("UNIDADE", "Unidade judiciária", safeLabel(normalizedUnidade, "TODAS"), !blank(normalizedUnidade), "Refina fila operacional por vara, secretaria, gabinete ou unidade institucional."),
                new ProfessionalGrantGovernanceFilterDto("COLEGIADO", "Órgão colegiado", safeLabel(normalizedColegiado, "TODOS"), !blank(normalizedColegiado), "Isola grants de composição colegiada, relatoria recursal e câmara."),
                new ProfessionalGrantGovernanceFilterDto("ENTE", "Ente representado", safeLabel(normalizedEnte, "TODOS"), !blank(normalizedEnte), "Recorta procuradoria por ente e carteira pública representada."),
                new ProfessionalGrantGovernanceFilterDto("CRITICAL_ONLY", "Fila crítica", criticalOnly ? "SOMENTE_CRITICOS" : "FILA_COMPLETA", criticalOnly, "Quando ligado, devolve apenas pendências críticas, expiração iminente e fila de step-up.")
        );
        List<String> warnings = new ArrayList<>();
        if (criticalPending.isEmpty()) {
            warnings.add("Nenhuma pendência crítica caiu na janela operacional atual.");
        }
        if (stepUpQueue.isEmpty()) {
            warnings.add("Não há grants aguardando step-up obrigatório no recorte operacional atual.");
        }
        if (manageable.size() > safeLimit) {
            warnings.add("A fila operacional foi truncada no limite atual; refine gabinete, unidade, colegiado ou ente para leitura mais precisa.");
        }
        return new ProfessionalGrantOperationalQueueResponse(
                now,
                managerClass.name(),
                safe(actor.getNome()),
                filters,
                summary,
                criticalPending,
                expiringImminent,
                stepUpQueue,
                suggestedTemplates(managerClass, actor, 6),
                List.of(
                        new ProfessionalForensicPanelLinkDto("OPERATIONAL_DASHBOARD", "Atualizar fila operacional", "/api/v1/professional/access-grants/operational-dashboard", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("TEMPLATE_CATALOG", "Abrir catálogo de templates", "/api/v1/professional/access-grants/templates", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("TEMPLATE_BATCH_REQUEST", "Emitir grants por template", "/api/v1/professional/access-grants/template-batch-requests", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("GOVERNANCE_DASHBOARD", "Abrir dashboard superior", "/api/v1/professional/access-grants/governance-dashboard", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("GRANT_WORKSPACE", "Voltar à gestão operacional", "/api/v1/professional/access-grants/workspace", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("FORENSIC_WORKSPACE", "Abrir painel forense conectado", "/api/v1/professional/forensic-panel/workspace", "SECONDARY")
                ),
                warnings
        );
    }

    @Transactional(readOnly = true)
    public ProfessionalGrantTemplateCatalogResponse templates() {
        Usuario actor = currentUserService.getRequired();
        ProfessionalActorClass managerClass = accessVectorService.actorClass(actor);
        assertManager(actor, managerClass);
        List<ProfessionalGrantTemplateDto> templates = suggestedTemplates(managerClass, actor, 20);
        List<String> warnings = new ArrayList<>();
        if (templates.isEmpty()) {
            warnings.add("Nenhum template institucional ativo foi encontrado para a classe gestora atual.");
        }
        return new ProfessionalGrantTemplateCatalogResponse(
                LocalDateTime.now(),
                managerClass.name(),
                safe(actor.getNome()),
                templates,
                List.of(
                        new ProfessionalForensicPanelLinkDto("TEMPLATE_CATALOG", "Atualizar catálogo de templates", "/api/v1/professional/access-grants/templates", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("TEMPLATE_BATCH_REQUEST", "Emitir grants por template", "/api/v1/professional/access-grants/template-batch-requests", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("OPERATIONAL_DASHBOARD", "Abrir fila operacional", "/api/v1/professional/access-grants/operational-dashboard", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("GRANT_WORKSPACE", "Voltar à gestão de grants", "/api/v1/professional/access-grants/workspace", "SECONDARY")
                ),
                warnings
        );
    }

    @Transactional
    @PjbTransactionalBudget(operation = "professional.access-grant.issue-batch-from-template", maxMillis = 15000)
    public ProfessionalGrantBatchOperationResponse issueBatchFromTemplate(ProfessionalGrantTemplateBatchIssueRequest request) {
        Usuario actor = currentUserService.getRequired();
        ProfessionalActorClass managerClass = accessVectorService.actorClass(actor);
        assertManager(actor, managerClass);
        if (request == null || blank(request.templateCode()) || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Solicitação por template incompleta.");
        }
        ProfessionalAccessGrantTemplate template = requireTemplate(request.templateCode());
        if (!canUseTemplate(managerClass, actor, template)) {
            throw new AccessDeniedPjbException("Template incompatível com a autoridade institucional do operador autenticado.");
        }
        int requestedCount = request.items().size();
        int safeLimit = Math.min(requestedCount, 50);
        List<ProfessionalGrantQueueItemDto> processed = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int index = 0;
        for (ProfessionalGrantTemplateBatchItemRequest item : request.items().stream().limit(safeLimit).toList()) {
            index++;
            try {
                ProfessionalGrantDetailResponse detail = issue(toIssueRequest(template, item));
                if (request.autoApprove() && Boolean.TRUE.equals(template.getAutoApproveAllowed())) {
                    detail = approve(detail.grant().grantId(), new ProfessionalGrantDecisionRequest(firstNonBlank("Template homologado", request.batchLabel(), item.reason(), template.getLabel())));
                }
                processed.add(detail.grant());
            } catch (RuntimeException ex) {
                errors.add("Item " + index + ": " + ex.getMessage());
            }
        }
        if (requestedCount > safeLimit) {
            errors.add("Lote por template truncado no limite operacional de 50 grants por emissão assistida.");
        }
        auditLedgerService.appendSafely("PROFESSIONAL_GRANT_TEMPLATE_BATCH_REQUESTED", "PROFESSIONAL_ACCESS_GRANT_TEMPLATE", safe(template.getTemplateCode()), "{\"requestedCount\":" + requestedCount + ",\"succeededCount\":" + processed.size() + ",\"templateCode\":\"" + template.getTemplateCode() + "\",\"autoApprove\":" + request.autoApprove() + "}");
        return new ProfessionalGrantBatchOperationResponse(
                LocalDateTime.now(),
                request.autoApprove() && Boolean.TRUE.equals(template.getAutoApproveAllowed()) ? "TEMPLATE_BATCH_REQUEST_AND_APPROVE" : "TEMPLATE_BATCH_REQUEST",
                firstNonBlank(normalizeText(request.batchLabel()), template.getLabel()),
                requestedCount,
                processed.size(),
                errors.size(),
                processed,
                errors,
                List.of(
                        new ProfessionalForensicPanelLinkDto("TEMPLATE_CATALOG", "Abrir catálogo de templates", "/api/v1/professional/access-grants/templates", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("OPERATIONAL_DASHBOARD", "Abrir fila operacional", "/api/v1/professional/access-grants/operational-dashboard", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("GRANT_WORKSPACE", "Voltar à gestão de grants", "/api/v1/professional/access-grants/workspace", "SECONDARY")
                )
        );
    }

    private ProfessionalInstitutionalAccessGrant requireGrant(Long grantId) {
        return grantRepository.findById(grantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Grant profissional", String.valueOf(grantId)));
    }

    private void assertManager(Usuario actor, ProfessionalActorClass managerClass) {
        if (!isManager(actor, managerClass)) {
            throw new AccessDeniedPjbException("Perfil atual não possui autoridade para emitir ou homologar grants institucionais.");
        }
    }

    private void assertCanManage(Usuario actor, ProfessionalActorClass managerClass, ProfessionalInstitutionalAccessGrant grant) {
        if (!canManage(actor, managerClass, grant)) {
            throw new AccessDeniedPjbException("Grant incompatível com a autoridade institucional do operador autenticado.");
        }
    }

    private boolean canManage(Usuario actor, ProfessionalActorClass managerClass, ProfessionalInstitutionalAccessGrant grant) {
        if (grant == null) {
            return false;
        }
        if (isAdmin(actor)) {
            return true;
        }
        return switch (managerClass) {
            case MAGISTRATURA -> grant.getActorClass() == ProfessionalActorClass.MAGISTRATURA && isMagistrateGrant(grant.getGrantType())
                    || grant.getActorClass() == ProfessionalActorClass.APOIO_JUDICIAL && grant.getGrantType() == ProfessionalAccessGrantType.DELEGACAO_GABINETE && matchesMagistrateAnchor(actor, grant);
            case DEFENSORIA -> grant.getActorClass() == ProfessionalActorClass.DEFENSORIA && isDefensoriaGrant(grant.getGrantType());
            case PROCURADORIA -> grant.getActorClass() == ProfessionalActorClass.PROCURADORIA && isProcuradoriaGrant(grant.getGrantType());
            default -> false;
        };
    }

    private void assertGrantSemantics(Usuario actor, ProfessionalActorClass managerClass, ProfessionalInstitutionalAccessGrant grant) {
        if (!canManage(actor, managerClass, grant)) {
            throw new AccessDeniedPjbException("Grant solicitado não se encaixa na cadeia de legitimidade do emissor.");
        }
        if (grant.getGrantType() == ProfessionalAccessGrantType.DELEGACAO_GABINETE && grant.getActorClass() != ProfessionalActorClass.APOIO_JUDICIAL) {
            throw new IllegalArgumentException("Delegação de gabinete exige alvo da classe APOIO_JUDICIAL.");
        }
        if (grant.getGrantType() == ProfessionalAccessGrantType.RELATORIA_PROCESSO && grant.getProcesso() == null) {
            throw new IllegalArgumentException("Grant de relatoria exige processo vinculado.");
        }
        if (grant.getGrantType() == ProfessionalAccessGrantType.COMPOSICAO_COLEGIADO && blank(grant.getOrgaoColegiadoCodigo()) && grant.getProcesso() == null) {
            throw new IllegalArgumentException("Composição colegiada exige órgão colegiado ou processo vinculado.");
        }
        if ((grant.getGrantType() == ProfessionalAccessGrantType.DESIGNACAO_TERRITORIAL || grant.getGrantType() == ProfessionalAccessGrantType.PLANTAO || grant.getGrantType() == ProfessionalAccessGrantType.SUBSTITUICAO)
                && blank(grant.getUf()) && blank(grant.getComarca()) && blank(grant.getTribunal()) && blank(grant.getUnidadeJudiciariaCodigo())) {
            throw new IllegalArgumentException("Grant territorial exige ao menos um marcador territorial ou de unidade judiciária.");
        }
    }

    private Long resolveTargetMagistrate(Usuario actor, ProfessionalActorClass managerClass, ProfessionalGrantIssueRequest request) {
        if (request.grantType() == ProfessionalAccessGrantType.DELEGACAO_GABINETE || request.grantType() == ProfessionalAccessGrantType.RELATORIA_PROCESSO || request.grantType() == ProfessionalAccessGrantType.COMPOSICAO_COLEGIADO || request.grantType() == ProfessionalAccessGrantType.SUBSTITUICAO || request.grantType() == ProfessionalAccessGrantType.PLANTAO || request.grantType() == ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL) {
            if (request.targetMagistrateUserId() != null) {
                return request.targetMagistrateUserId();
            }
            if (managerClass == ProfessionalActorClass.MAGISTRATURA) {
                return actor.getId();
            }
        }
        return request.targetMagistrateUserId();
    }

    private void appendEvent(ProfessionalInstitutionalAccessGrant grant,
                             ProfessionalGrantEventType eventType,
                             ProfessionalGrantApprovalStatus previous,
                             ProfessionalGrantApprovalStatus next,
                             Usuario actor,
                             String detail) {
        ProfessionalInstitutionalAccessGrantEvent event = new ProfessionalInstitutionalAccessGrantEvent();
        event.setGrant(grant);
        event.setEventType(eventType);
        event.setPreviousStatus(previous);
        event.setNewStatus(next);
        event.setActorUserId(actor.getId());
        event.setActorName(safe(actor.getNome()));
        event.setActorClass(accessVectorService.actorClass(actor).name());
        event.setDetail(detail);
        eventRepository.save(event);
    }

    private ProfessionalGrantQueueItemDto toQueueItem(ProfessionalInstitutionalAccessGrant grant) {
        String tone = switch (grant.getApprovalStatus()) {
            case PENDING -> "ATTENTION_ORANGE";
            case APPROVED -> "ACTIVE_BLUE";
            case REJECTED -> "STABLE_NEUTRAL";
            case REVOKED -> "CRITICAL_RED";
        };
        return new ProfessionalGrantQueueItemDto(
                grant.getId(),
                grant.getApprovalStatus().name(),
                grant.getApprovalStatus().displayName(),
                grant.getActorClass().name(),
                grant.getGrantType().name(),
                grant.getGrantType().displayName(),
                grant.getAccessBasis().name(),
                grant.getAccessBasis().displayName(),
                grant.getUsuario() == null ? null : grant.getUsuario().getId(),
                grant.getUsuario() == null ? null : safe(grant.getUsuario().getNome()),
                professionalLabel(grant.getUsuario()),
                resolveNumero(grant.getProcesso()),
                organizationalAnchor(grant),
                grant.requiresStepUp(),
                grant.getRequestedAt(),
                grant.getRequestedByName(),
                grant.getApprovedAt(),
                grant.getApprovedByName(),
                grant.getRevokedAt(),
                grant.getRevokedByName(),
                firstNonBlank(grant.getDecisionReason(), grant.getReason(), grant.getSourceLabel()),
                tone
        );
    }

    private ProfessionalGrantEventDto toEvent(ProfessionalInstitutionalAccessGrantEvent event) {
        String tone = switch (event.getEventType()) {
            case REQUESTED -> "ATTENTION_ORANGE";
            case APPROVED -> "ACTIVE_BLUE";
            case REJECTED -> "STABLE_NEUTRAL";
            case REVOKED -> "CRITICAL_RED";
        };
        return new ProfessionalGrantEventDto(
                event.getId(),
                event.getCreatedAt(),
                event.getEventType().name(),
                event.getEventType().displayName(),
                event.getPreviousStatus() == null ? null : event.getPreviousStatus().name(),
                event.getNewStatus() == null ? null : event.getNewStatus().name(),
                event.getActorUserId(),
                event.getActorName(),
                event.getActorClass(),
                event.getDetail(),
                tone
        );
    }

    private List<String> manageableGrantTypes(ProfessionalActorClass managerClass, Usuario actor) {
        if (isAdmin(actor)) {
            return List.of(ProfessionalAccessGrantType.values()).stream().map(Enum::name).toList();
        }
        return switch (managerClass) {
            case MAGISTRATURA -> List.of(
                    ProfessionalAccessGrantType.RELATORIA_PROCESSO.name(),
                    ProfessionalAccessGrantType.COMPOSICAO_COLEGIADO.name(),
                    ProfessionalAccessGrantType.SUBSTITUICAO.name(),
                    ProfessionalAccessGrantType.PLANTAO.name(),
                    ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL.name(),
                    ProfessionalAccessGrantType.DELEGACAO_GABINETE.name()
            );
            case DEFENSORIA -> List.of(
                    ProfessionalAccessGrantType.DESIGNACAO_PROCESSO.name(),
                    ProfessionalAccessGrantType.DESIGNACAO_TERRITORIAL.name(),
                    ProfessionalAccessGrantType.LOTACAO_UNIDADE.name()
            );
            case PROCURADORIA -> List.of(
                    ProfessionalAccessGrantType.REPRESENTACAO_PROCESSO.name(),
                    ProfessionalAccessGrantType.REPRESENTACAO_ENTE.name(),
                    ProfessionalAccessGrantType.LOTACAO_UNIDADE.name()
            );
            default -> List.of();
        };
    }

    private boolean isManager(Usuario actor, ProfessionalActorClass managerClass) {
        return isAdmin(actor) || managerClass == ProfessionalActorClass.MAGISTRATURA || managerClass == ProfessionalActorClass.DEFENSORIA || managerClass == ProfessionalActorClass.PROCURADORIA;
    }

    private boolean isAdmin(Usuario actor) {
        return actor != null && actor.getTipoUsuario() != null && actor.getTipoUsuario().isAdmin();
    }

    private boolean isMagistrateGrant(ProfessionalAccessGrantType grantType) {
        return grantType == ProfessionalAccessGrantType.RELATORIA_PROCESSO
                || grantType == ProfessionalAccessGrantType.COMPOSICAO_COLEGIADO
                || grantType == ProfessionalAccessGrantType.SUBSTITUICAO
                || grantType == ProfessionalAccessGrantType.PLANTAO
                || grantType == ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL;
    }

    private boolean isDefensoriaGrant(ProfessionalAccessGrantType grantType) {
        return grantType == ProfessionalAccessGrantType.DESIGNACAO_PROCESSO
                || grantType == ProfessionalAccessGrantType.DESIGNACAO_TERRITORIAL
                || grantType == ProfessionalAccessGrantType.LOTACAO_UNIDADE;
    }

    private boolean isProcuradoriaGrant(ProfessionalAccessGrantType grantType) {
        return grantType == ProfessionalAccessGrantType.REPRESENTACAO_PROCESSO
                || grantType == ProfessionalAccessGrantType.REPRESENTACAO_ENTE
                || grantType == ProfessionalAccessGrantType.LOTACAO_UNIDADE;
    }

    private boolean matchesMagistrateAnchor(Usuario actor, ProfessionalInstitutionalAccessGrant grant) {
        return grant.getTargetMagistrateUserId() == null || Objects.equals(grant.getTargetMagistrateUserId(), actor.getId());
    }

    private Processo resolveProcesso(String numero) {
        if (blank(numero)) {
            return null;
        }
        return resolveProcessoRequired(numero);
    }

    private Processo resolveProcessoRequired(String numero) {
        return processoRepository.findByNumeroUnificado(numero)
                .or(() -> processoRepository.findByNumeroProcesso(numero))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", numero));
    }

    private String detailForEvent(ProfessionalInstitutionalAccessGrant grant) {
        List<String> parts = new ArrayList<>();
        parts.add(grant.getGrantType().displayName());
        if (grant.getUsuario() != null) {
            parts.add(safe(grant.getUsuario().getNome()));
        }
        String numero = resolveNumero(grant.getProcesso());
        if (!blank(numero)) {
            parts.add(numero);
        }
        String anchor = organizationalAnchor(grant);
        if (!blank(anchor)) {
            parts.add(anchor);
        }
        if (!blank(grant.getReason())) {
            parts.add(grant.getReason());
        }
        return String.join(" • ", parts);
    }

    private String professionalLabel(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        String reg = firstNonBlank(usuario.getOab(), usuario.getRegistroProfissional(), usuario.getEmail());
        return firstNonBlank(reg, usuario.getPerfil());
    }

    private String processRoute(ProfessionalInstitutionalAccessGrant grant) {
        String numero = resolveNumero(grant.getProcesso());
        if (blank(numero)) {
            return "/api/v1/professional/access-grants/workspace";
        }
        return "/api/v1/professional/access-grants/processos/" + numero + "/timeline";
    }

    private String resolveNumero(Processo processo) {
        if (processo == null) {
            return null;
        }
        return firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso());
    }

    private String organizationalAnchor(ProfessionalInstitutionalAccessGrant grant) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, grant.getUf());
        addIfPresent(parts, grant.getComarca());
        addIfPresent(parts, grant.getTribunal());
        addIfPresent(parts, grant.getUnidadeJudiciariaCodigo());
        addIfPresent(parts, grant.getOrgaoColegiadoCodigo());
        addIfPresent(parts, grant.getEnteCode());
        return String.join(" / ", parts);
    }

    private void addIfPresent(List<String> parts, String value) {
        if (!blank(value)) {
            parts.add(value.trim());
        }
    }

    private ProfessionalGrantBatchOperationResponse decideBatch(String operation,
                                                                ProfessionalGrantBatchDecisionRequest request,
                                                                boolean approveMode) {
        Usuario actor = currentUserService.getRequired();
        ProfessionalActorClass managerClass = accessVectorService.actorClass(actor);
        assertManager(actor, managerClass);
        if (request == null || request.grantIds() == null || request.grantIds().isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um grant para a operação em lote.");
        }
        int requestedCount = request.grantIds().size();
        int safeLimit = Math.min(requestedCount, 80);
        List<ProfessionalGrantQueueItemDto> processed = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int index = 0;
        for (Long grantId : request.grantIds().stream().limit(safeLimit).toList()) {
            index++;
            try {
                ProfessionalGrantDetailResponse detail = approveMode
                        ? approve(grantId, new ProfessionalGrantDecisionRequest(normalizeText(request.reason())))
                        : revoke(grantId, new ProfessionalGrantDecisionRequest(normalizeText(request.reason())));
                processed.add(detail.grant());
            } catch (RuntimeException ex) {
                errors.add("Grant " + grantId + " no índice " + index + ": " + ex.getMessage());
            }
        }
        if (requestedCount > safeLimit) {
            errors.add("Operação em lote truncada no limite operacional de 80 grants por execução.");
        }
        auditLedgerService.appendSafely(operation, "PROFESSIONAL_ACCESS_GRANT", safe(actor.getId() == null ? null : actor.getId().toString()), "{\"requestedCount\":" + requestedCount + ",\"succeededCount\":" + processed.size() + "}");
        return new ProfessionalGrantBatchOperationResponse(
                LocalDateTime.now(),
                operation,
                normalizeText(request.reason()),
                requestedCount,
                processed.size(),
                errors.size(),
                processed,
                errors,
                List.of(
                        new ProfessionalForensicPanelLinkDto("GOVERNANCE_DASHBOARD", "Abrir dashboard superior", "/api/v1/professional/access-grants/governance-dashboard", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("GRANT_WORKSPACE", "Voltar à gestão de grants", "/api/v1/professional/access-grants/workspace", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("FORENSIC_WORKSPACE", "Abrir painel forense", "/api/v1/professional/forensic-panel/workspace", "SECONDARY")
                )
        );
    }

    private ProfessionalGrantIssueRequest toIssueRequest(ProfessionalGrantBatchItemRequest item) {
        return new ProfessionalGrantIssueRequest(
                item.targetUserId(),
                item.actorClass(),
                item.grantType(),
                item.accessBasis(),
                item.processoNumero(),
                item.uf(),
                item.comarca(),
                item.tribunal(),
                item.unidadeJudiciariaCodigo(),
                item.orgaoColegiadoCodigo(),
                item.enteCode(),
                item.targetMagistrateUserId(),
                item.sourceRef(),
                item.sourceLabel(),
                item.reason(),
                item.requiresStepUp(),
                item.inicioVigencia(),
                item.fimVigencia()
        );
    }

    private ProfessionalGrantIssueRequest toIssueRequest(ProfessionalAccessGrantTemplate template,
                                                        ProfessionalGrantTemplateBatchItemRequest item) {
        LocalDateTime start = item.inicioVigencia() == null ? LocalDateTime.now() : item.inicioVigencia();
        Integer defaultDurationDays = template.getDefaultDurationDays();
        LocalDateTime end = item.fimVigencia() != null
                ? item.fimVigencia()
                : defaultDurationDays == null ? null : start.plusDays(defaultDurationDays);
        return new ProfessionalGrantIssueRequest(
                item.targetUserId(),
                template.getActorClass(),
                template.getGrantType(),
                template.getAccessBasis(),
                firstNonBlank(item.processoNumero(), requiresProcessTemplate(template) ? item.processoNumero() : null),
                firstNonBlank(normalizeToken(item.uf()), template.getDefaultUf()),
                firstNonBlank(normalizeText(item.comarca()), template.getDefaultComarca()),
                firstNonBlank(normalizeToken(item.tribunal()), template.getDefaultTribunal()),
                firstNonBlank(normalizeToken(item.unidadeJudiciariaCodigo()), template.getDefaultUnidadeJudiciariaCodigo()),
                firstNonBlank(normalizeToken(item.orgaoColegiadoCodigo()), template.getDefaultOrgaoColegiadoCodigo()),
                firstNonBlank(normalizeToken(item.enteCode()), template.getDefaultEnteCode()),
                item.targetMagistrateUserId(),
                firstNonBlank(normalizeText(item.sourceRef()), template.getTemplateCode()),
                firstNonBlank(normalizeText(item.sourceLabel()), template.getLabel()),
                firstNonBlank(normalizeText(item.reason()), template.getDescription()),
                item.requiresStepUp() != null ? item.requiresStepUp() : Boolean.TRUE.equals(template.getDefaultRequiresStepUp()),
                start,
                end
        );
    }

    private List<ProfessionalGrantTemplateDto> suggestedTemplates(ProfessionalActorClass managerClass, Usuario actor, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return templateRepository.findByAtivoTrueOrderBySequenceOrderAscIdAsc().stream()
                .filter(item -> canUseTemplate(managerClass, actor, item))
                .limit(safeLimit)
                .map(this::toTemplateDto)
                .toList();
    }

    private ProfessionalAccessGrantTemplate requireTemplate(String templateCode) {
        return templateRepository.findByTemplateCodeAndAtivoTrue(normalizeToken(templateCode))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Template de grant profissional", safe(templateCode)));
    }

    private boolean canUseTemplate(ProfessionalActorClass managerClass, Usuario actor, ProfessionalAccessGrantTemplate template) {
        if (template == null) {
            return false;
        }
        if (isAdmin(actor)) {
            return true;
        }
        return switch (managerClass) {
            case MAGISTRATURA -> template.getActorClass() == ProfessionalActorClass.MAGISTRATURA || template.getActorClass() == ProfessionalActorClass.APOIO_JUDICIAL;
            case DEFENSORIA -> template.getActorClass() == ProfessionalActorClass.DEFENSORIA;
            case PROCURADORIA -> template.getActorClass() == ProfessionalActorClass.PROCURADORIA;
            default -> false;
        };
    }

    private ProfessionalGrantTemplateDto toTemplateDto(ProfessionalAccessGrantTemplate template) {
        return new ProfessionalGrantTemplateDto(
                template.getTemplateCode(),
                safe(template.getLabel()),
                safe(template.getDescription()),
                template.getActorClass().name(),
                template.getGrantType().name(),
                template.getAccessBasis().name(),
                Boolean.TRUE.equals(template.getDefaultRequiresStepUp()),
                Boolean.TRUE.equals(template.getAutoApproveAllowed()),
                template.getDefaultDurationDays(),
                safe(template.getTargetMode()),
                templateAnchor(template),
                safe(firstNonBlank(template.getGovernanceTone(), "STABLE_NEUTRAL"))
        );
    }

    private String templateAnchor(ProfessionalAccessGrantTemplate template) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, template.getDefaultUf());
        addIfPresent(parts, template.getDefaultComarca());
        addIfPresent(parts, template.getDefaultTribunal());
        addIfPresent(parts, template.getDefaultUnidadeJudiciariaCodigo());
        addIfPresent(parts, template.getDefaultOrgaoColegiadoCodigo());
        addIfPresent(parts, template.getDefaultEnteCode());
        return String.join(" / ", parts);
    }

    private boolean requiresProcessTemplate(ProfessionalAccessGrantTemplate template) {
        return template != null && (template.getGrantType() == ProfessionalAccessGrantType.RELATORIA_PROCESSO
                || template.getGrantType() == ProfessionalAccessGrantType.DESIGNACAO_PROCESSO
                || template.getGrantType() == ProfessionalAccessGrantType.REPRESENTACAO_PROCESSO);
    }

    private boolean matchesOperationalFilters(ProfessionalInstitutionalAccessGrant grant,
                                              String gabineteCodigo,
                                              String unidadeJudiciariaCodigo,
                                              String orgaoColegiadoCodigo,
                                              String enteCode) {
        boolean gabineteMatches = blank(gabineteCodigo)
                || matchesText(gabineteCodigo, grant.getUnidadeJudiciariaCodigo(), true)
                || matchesText(gabineteCodigo, grant.getSourceRef(), true)
                || matchesText(gabineteCodigo, grant.getSourceLabel(), true);
        return gabineteMatches
                && matchesText(unidadeJudiciariaCodigo, grant.getUnidadeJudiciariaCodigo(), true)
                && matchesText(orgaoColegiadoCodigo, grant.getOrgaoColegiadoCodigo(), true)
                && matchesText(enteCode, grant.getEnteCode(), true);
    }

    private boolean isCriticalPending(ProfessionalInstitutionalAccessGrant grant, LocalDateTime now) {
        if (grant == null || now == null || grant.getApprovalStatus() != ProfessionalGrantApprovalStatus.PENDING) {
            return false;
        }
        LocalDateTime requestedAt = grant.getRequestedAt();
        long ageHours = requestedAt == null ? Long.MAX_VALUE : ChronoUnit.HOURS.between(requestedAt, now);
        boolean stepUp = grant.requiresStepUp();
        boolean expiringWindow = grant.getFimVigencia() != null && !grant.getFimVigencia().isBefore(now) && !grant.getFimVigencia().isAfter(now.plusDays(3));
        return ageHours >= 24 || stepUp || expiringWindow;
    }

    private boolean isExpiringImminent(ProfessionalInstitutionalAccessGrant grant, LocalDateTime now) {
        return grant != null
                && grant.isApproved()
                && Boolean.TRUE.equals(grant.getAtivo())
                && grant.getFimVigencia() != null
                && !grant.getFimVigencia().isBefore(now)
                && !grant.getFimVigencia().isAfter(now.plusDays(5));
    }

    private boolean isStepUpQueueItem(ProfessionalInstitutionalAccessGrant grant, LocalDateTime now) {
        if (grant == null || !grant.requiresStepUp()) {
            return false;
        }
        if (grant.getApprovalStatus() == ProfessionalGrantApprovalStatus.PENDING) {
            return true;
        }
        return grant.isAtivoNaJanela(now);
    }

    private boolean matchesGovernanceFilters(ProfessionalInstitutionalAccessGrant grant,
                                             String status,
                                             String actorClass,
                                             String grantType,
                                             String uf,
                                             String comarca,
                                             String tribunal,
                                             String unidadeJudiciariaCodigo,
                                             String orgaoColegiadoCodigo,
                                             String enteCode) {
        return matchesEnum(status, grant.getApprovalStatus() == null ? null : grant.getApprovalStatus().name())
                && matchesEnum(actorClass, grant.getActorClass() == null ? null : grant.getActorClass().name())
                && matchesEnum(grantType, grant.getGrantType() == null ? null : grant.getGrantType().name())
                && matchesText(uf, grant.getUf(), true)
                && matchesText(comarca, grant.getComarca(), false)
                && matchesText(tribunal, grant.getTribunal(), true)
                && matchesText(unidadeJudiciariaCodigo, grant.getUnidadeJudiciariaCodigo(), true)
                && matchesText(orgaoColegiadoCodigo, grant.getOrgaoColegiadoCodigo(), true)
                && matchesText(enteCode, grant.getEnteCode(), true);
    }

    private boolean matchesEnum(String filter, String current) {
        return blank(filter) || Objects.equals(normalizeToken(current), normalizeToken(filter));
    }

    private boolean matchesText(String filter, String current, boolean tokenized) {
        if (blank(filter)) {
            return true;
        }
        String left = tokenized ? normalizeToken(filter) : normalizeText(filter);
        String right = tokenized ? normalizeToken(current) : normalizeText(current);
        return Objects.equals(left, right);
    }

    private String safeLabel(String value, String fallback) {
        return blank(value) ? fallback : value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!blank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeToken(String value) {
        return blank(value) ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return blank(value) ? null : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
