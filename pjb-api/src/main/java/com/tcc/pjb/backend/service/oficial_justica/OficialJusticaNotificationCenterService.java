package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaCienciaIntimacaoRequest;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaNotificationEnvelope;
import com.tcc.pjb.backend.model.dto.security.OperationalStepUpChallengeResponse;
import com.tcc.pjb.backend.model.entity.NotificationHistory;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.NotificationHistoryRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.secretariat.operational.OperationalNotificationProofService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaNotificationCenterService {

    private static final Logger log = LoggerFactory.getLogger(OficialJusticaNotificationCenterService.class);
    private static final List<TipoUsuario> OFFICIAL_ROLES = List.of(TipoUsuario.OFICIAL_JUSTICA, TipoUsuario.OFICIAL_JUSTICA_AVALIADOR);

    private final PerfilDashboardContextFactory contextFactory;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final WorkItemRepository workItemRepository;
    private final PainelServiceCommons commons;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final OficialJusticaPanelEgressService panelEgressService;
    private final OficialJusticaNotificationEventPublisher notificationEventPublisher;
    private final OperationalNotificationProofService notificationProofService;
    private final OficialJusticaCommunicationFormalModelService communicationFormalModelService;
    private final boolean kafkaEnabled;

    public OficialJusticaNotificationCenterService(PerfilDashboardContextFactory contextFactory,
                                                   NotificationHistoryRepository notificationHistoryRepository,
                                                   WorkItemRepository workItemRepository,
                                                   PainelServiceCommons commons,
                                                   OficialJusticaContextEnvelopeService contextEnvelopeService,
                                                   OficialJusticaPanelEgressService panelEgressService,
                                                   OficialJusticaNotificationEventPublisher notificationEventPublisher,
                                                   OperationalNotificationProofService notificationProofService,
                                                   OficialJusticaCommunicationFormalModelService communicationFormalModelService,
                                                   @Value("${pjb.kafka.enabled:false}") boolean kafkaEnabled) {
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.notificationHistoryRepository = Objects.requireNonNull(notificationHistoryRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.commons = Objects.requireNonNull(commons);
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
        this.panelEgressService = Objects.requireNonNull(panelEgressService);
        this.notificationEventPublisher = Objects.requireNonNull(notificationEventPublisher);
        this.notificationProofService = Objects.requireNonNull(notificationProofService);
        this.communicationFormalModelService = Objects.requireNonNull(communicationFormalModelService);
        this.kafkaEnabled = kafkaEnabled;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> painelResumo() {
        Usuario usuario = contextFactory.build().usuario();
        materializarNomeacoesAtivasUsuario(usuario, 20);
        List<Map<String, Object>> feed = centralizedRows(usuario, 6);
        List<WorkItem> activeNominationItems = activeNominationItemsForUser(usuario, 18);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("mode", "OFICIAL_NOTIFICATION_CENTER_V3");
        out.put("streamPath", "/api/v1/oficial-justica/live/stream");
        out.put("feedPath", "/api/v1/oficial-justica/notificacoes?limit=20");
        out.put("nomeacoesRecentes", activeNominationItems.size());
        out.put("notificacoesNaoLidasEstimadas", notificationHistoryRepository.countByUsuarioIdAndLidoEmIsNull(usuario.getId()));
        out.put("nomeacoesAtivas", activeNominationItems.stream().map(this::toNominationDigest).toList());
        out.put("ultimas", feed);
        out.put("instantDelivery", Boolean.TRUE);
        out.put("centralizadaNoProcesso", Boolean.TRUE);
        out.put("processoEntraNoPainelAoNomear", Boolean.TRUE);
        out.put("scope", usuario.atuaNaUniao() ? "FEDERAL_OU_MISTO" : "ESTADUAL_OU_MISTO");
        out.put("oficialResponsavel", contextEnvelopeService.oficialEnvelope(usuario, null));
        out.put("deliveryBus", deliveryBusStatus());
        return safeCopy(out);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listar(int limit) {
        Usuario usuario = contextFactory.build().usuario();
        materializarNomeacoesAtivasUsuario(usuario, Math.max(6, Math.min(limit, 30)));
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Map<String, Object>> items = centralizedRows(usuario, safeLimit);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", Instant.now());
        out.put("total", items.size());
        out.put("items", items);
        out.put("streamPath", "/api/v1/oficial-justica/live/stream");
        out.put("mode", "OFICIAL_NOTIFICATION_CENTER_V3");
        out.put("centralizadaNoProcesso", Boolean.TRUE);
        out.put("oficialResponsavel", contextEnvelopeService.oficialEnvelope(usuario, null));
        out.put("deliveryBus", deliveryBusStatus());
        return safeCopy(out);
    }

    @Transactional
    public OperationalStepUpChallengeResponse issueCienciaProcessualChallenge(Long processoId) {
        Usuario usuario = requireOfficialUser();
        WorkItem item = loadPrimaryOfficialItem(usuario, processoId);
        return notificationProofService.issueChallenge(item.getProcesso(), processoId, "OFICIAL_CIENTE_INTIMACAO", "OFICIAL_JUSTICA", "OFICIAL_JUSTICA");
    }

    @Transactional
    public Map<String, Object> confirmarCienciaProcessual(Long processoId, OficialJusticaCienciaIntimacaoRequest request) {
        Usuario usuario = requireOfficialUser();
        WorkItem item = loadPrimaryOfficialItem(usuario, processoId);
        Processo processo = item.getProcesso();
        List<NotificationHistory> history = notificationHistoryRepository.findTop50ByUsuarioIdAndProcessoIdOrderByEnviadoEmDesc(usuario.getId(), processoId);
        LocalDateTime now = LocalDateTime.now();
        int updated = 0;
        for (NotificationHistory notificationItem : history) {
            boolean changed = false;
            if (notificationItem.getLidoEm() == null) {
                notificationItem.setLidoEm(now);
                changed = true;
            }
            if (notificationItem.getCienciaConfirmadaEm() == null) {
                notificationItem.setCienciaConfirmadaEm(now);
                changed = true;
            }
            if (!"CIENTE_CONFIRMADO".equalsIgnoreCase(notificationItem.getStatus())) {
                notificationItem.setStatus("CIENTE_CONFIRMADO");
                changed = true;
            }
            if (changed) {
                updated++;
            }
        }
        if (!history.isEmpty()) {
            notificationHistoryRepository.saveAll(history);
        }
        OficialJusticaCienciaIntimacaoRequest safeRequest = request == null
                ? new OficialJusticaCienciaIntimacaoRequest(null, null, "CIENTE_CONFIRMADO", null, null, null, List.of(), null)
                : request;
        OperationalNotificationProofService.GeneratedNotificationProof generatedProof = notificationProofService.materializeProof(
                processo,
                processoId,
                "OFICIAL_CIENTE_INTIMACAO",
                "OFICIAL_JUSTICA",
                "CERTIDAO",
                safeRequest.canal(),
                safeRequest.formaIntimacao(),
                safeRequest.provaResumo(),
                safeRequest.evidenceReferences(),
                buildContactEnvelope(processo),
                safeRequest.challengeId(),
                safeRequest.otpCode(),
                safeRequest.note()
        );
        String message = "Ciência confirmada pelo Oficial de Justiça para o processo " + processNumber(processo) + ".";
        commons.publishUserHistory(usuario, "OFICIAL", "OFICIAL_CIENTE_INTIMACAO", message, processo, item.getId());
        Map<String, Object> formalModel = communicationFormalModelService.buildProfile(processo, item, usuario);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "CIENCIA_CONFIRMADA");
        out.put("processoId", processoId);
        out.put("processoNumero", processNumber(processo));
        out.put("confirmadoEm", now);
        out.put("notificacoesAtualizadas", updated);
        out.put("workItemId", item.getId());
        out.put("workbenchPath", "/api/v1/oficial-justica/processos-nomeados/" + processoId + "/workbench");
        out.put("agendaPath", "/api/v1/oficial-justica/agenda-operacional");
        out.put("balcaoVirtualPath", "/api/v1/oficial-justica/balcao-virtual/processos/" + processoId + "/sala");
        out.put("unidadeContexto", contextEnvelopeService.processEnvelope(usuario, processo, item, null, null));
        out.put("oficialResponsavel", contextEnvelopeService.oficialEnvelope(usuario, null));
        out.put("centralizadaNoProcesso", Boolean.TRUE);
        out.put("documentoConfirmacao", generatedProof.document());
        out.put("assinaturaObrigatoria", Boolean.TRUE);
        out.put("twoFactorValidated", Boolean.TRUE);
        out.put("formalModel", formalModel);
        out.put("manualActions", formalModel.get("manualActions"));
        out.put("automaticActions", formalModel.get("automaticActions"));
        out.put("challengePath", "/api/v1/oficial-justica/processos/" + processoId + "/ciente-intimacao/challenge");
        out.put("botaoCiente", Map.of(
                "enabled", Boolean.FALSE,
                "alreadyConfirmed", Boolean.TRUE,
                "confirmedAt", now,
                "path", "/api/v1/oficial-justica/processos/" + processoId + "/ciente-intimacao"
        ));
        return safeCopy(out);
    }

    private Usuario requireOfficialUser() {
        Usuario usuario = contextFactory.build().usuario();
        if (usuario == null || usuario.getId() == null || !OFFICIAL_ROLES.contains(usuario.getTipoUsuario())) {
            throw new IllegalStateException("Usuário sem contexto válido para confirmação de ciência do Oficial de Justiça.");
        }
        return usuario;
    }

    private WorkItem loadPrimaryOfficialItem(Usuario usuario, Long processoId) {
        List<WorkItem> ativos = activeNominationItemsForUser(usuario, 80).stream()
                .filter(candidate -> candidate.getProcesso() != null && Objects.equals(candidate.getProcesso().getId(), processoId))
                .toList();
        if (ativos.isEmpty()) {
            List<WorkItem> vinculos = panelEgressService.reconcileVisibility(usuario, workItemRepository.findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(
                    processoId,
                    usuario.getId(),
                    OFFICIAL_ROLES,
                    com.tcc.pjb.backend.model.entity.enums.WorkItemStatus.CANCELADO,
                    PageRequest.of(0, 20)
            )).visibleItems();
            if (vinculos.isEmpty()) {
                throw new IllegalStateException("Não existe nomeação/intimação ativa para confirmação de ciência neste processo.");
            }
            ativos = vinculos;
        }
        WorkItem primary = ativos.getFirst();
        return workItemRepository.findLockedDetailedById(primary.getId()).orElse(primary);
    }

    private Map<String, Object> buildContactEnvelope(Processo processo) {
        LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();
        if (processo == null) {
            return envelope;
        }
        envelope.put("autor", compactContact(processo.getParteAutoraNome(), processo.getParteAutoraCpf(), null));
        envelope.put("reu", compactContact(processo.getParteReuNome(), processo.getParteReuCpf(), null));
        return envelope;
    }

    private Map<String, Object> compactContact(String nome, String documento, String contato) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        if (nome != null && !nome.isBlank()) {
            payload.put("nome", nome.trim());
        }
        if (documento != null && !documento.isBlank()) {
            payload.put("documento", documento.trim());
        }
        if (contato != null && !contato.isBlank()) {
            payload.put("contato", contato.trim());
        }
        return payload;
    }

    @Scheduled(initialDelayString = "${pjb.notifications.oficial.nomeacao.initial-delay-ms:15000}", fixedDelayString = "${pjb.notifications.oficial.nomeacao.fixed-delay-ms:30000}")
    @Transactional
    public void varrerNomeacoesRecentes() {
        Instant cut = Instant.now().minus(20, ChronoUnit.MINUTES);
        List<WorkItem> candidatos = workItemRepository.findRecentAssignedUsersByRoles(OFFICIAL_ROLES, cut, PageRequest.of(0, 80));
        for (WorkItem item : candidatos) {
            try {
                dispatchInstantNomination(item);
            } catch (Exception ex) {
                log.debug("falha_notificacao_nomeacao_oficial workItemId={} err={}", item.getId(), ex.getMessage());
            }
        }
    }

    @Transactional
    public boolean dispatchInstantNomination(WorkItem item) {
        if (item == null || item.getId() == null || item.getAssignedUser() == null) {
            return false;
        }
        Usuario destino = item.getAssignedUser();
        if (destino.getId() == null || !OFFICIAL_ROLES.contains(destino.getTipoUsuario())) {
            return false;
        }
        Processo processo = item.getProcesso();
        String titulo = nominationTitle(item);
        Long processoId = processo != null ? processo.getId() : null;
        if (notificationHistoryRepository.existsByUsuarioIdAndProcessoIdAndTitulo(destino.getId(), processoId, titulo)) {
            return false;
        }
        String processoNumero = processNumber(processo);
        String vara = processVara(processo);
        String rito = processo != null && processo.getRito() != null ? processo.getRito().name() : "COMUM_ORDINARIO";
        String prazo = item.getDueAt() != null ? item.getDueAt().toString() : "SEM_PRAZO_FATAL_DEFINIDO";
        String cidade = processo != null ? firstNonBlank(processo.getComarca(), destino.getComarca()) : destino.getComarca();
        String tribunal = processo != null ? firstNonBlank(processo.getTribunal(), "TRIBUNAL_NAO_IDENTIFICADO") : "TRIBUNAL_NAO_IDENTIFICADO";
        String regiao = contextEnvelopeService.resolveRegiaoJudicial(tribunal, processo != null ? processo.getUf() : destino.getUf(), contextEnvelopeService.resolveEsfera(destino, processo, destino.getTipoUsuario()));
        String message = "Nova nomeação operacional recebida no PJB para o processo " + processoNumero
                + " · " + vara
                + " · " + tribunal
                + " · " + firstNonBlank(cidade, "CIDADE_NAO_IDENTIFICADA")
                + " · região " + regiao
                + " · rito " + humanize(rito)
                + ". A notificação já foi centralizada no processo, a agenda viva foi atualizada e o workbench está pronto para cumprimento dentro do prazo " + prazo + '.';
        String path = processoId != null
                ? "/api/v1/oficial-justica/processos-nomeados/" + processoId + "/workbench"
                : "/api/v1/oficial-justica/diligencias/fila-viva";
        notificationEventPublisher.publish(buildEnvelope(
                destino,
                processo,
                item,
                titulo,
                "NOMEACAO_PROCESSUAL_OFICIAL",
                message,
                path,
                true,
                null,
                humanize(rito),
                "NOMEACAO"
        ));
        return true;
    }



    @Transactional
    public boolean dispatchProcessReactivation(WorkItem item, String originLabel) {
        if (item == null || item.getId() == null || item.getAssignedUser() == null) {
            return false;
        }
        Usuario destino = item.getAssignedUser();
        if (destino.getId() == null || !OFFICIAL_ROLES.contains(destino.getTipoUsuario())) {
            return false;
        }
        Processo processo = item.getProcesso();
        Long processoId = processo != null ? processo.getId() : null;
        String titulo = reactivationTitle(item);
        if (notificationHistoryRepository.existsByUsuarioIdAndProcessoIdAndTituloAndStatusAndEnviadoEmAfter(
                destino.getId(),
                processoId,
                titulo,
                "ENVIADO",
                LocalDateTime.now().minusMinutes(10)
        )) {
            return false;
        }
        String processoNumero = processNumber(processo);
        String vara = processVara(processo);
        String tribunal = processo != null ? firstNonBlank(processo.getTribunal(), "TRIBUNAL_NAO_IDENTIFICADO") : "TRIBUNAL_NAO_IDENTIFICADO";
        String cidade = processo != null ? firstNonBlank(processo.getComarca(), destino.getComarca()) : destino.getComarca();
        String regiao = contextEnvelopeService.resolveRegiaoJudicial(tribunal, processo != null ? processo.getUf() : destino.getUf(), contextEnvelopeService.resolveEsfera(destino, processo, destino.getTipoUsuario()));
        String prazo = item.getDueAt() != null ? item.getDueAt().toString() : "SEM_PRAZO_FATAL_DEFINIDO";
        String origem = firstNonBlank(originLabel, "SECRETARIA");
        String message = "O processo " + processoNumero
                + " reapareceu no painel do Oficial por nova intimação/reabertura operacional"
                + " · " + vara
                + " · " + tribunal
                + " · " + firstNonBlank(cidade, "CIDADE_NAO_IDENTIFICADA")
                + " · região " + regiao
                + " · origem " + origem
                + ". A agenda viva, o workbench e o balcão virtual foram reativados com prazo " + prazo + '.';
        String path = processoId != null
                ? "/api/v1/oficial-justica/processos-nomeados/" + processoId + "/workbench"
                : "/api/v1/oficial-justica/diligencias/fila-viva";
        notificationEventPublisher.publish(buildEnvelope(
                destino,
                processo,
                item,
                titulo,
                "REINTIMACAO_PROCESSUAL_OFICIAL",
                message,
                path,
                true,
                null,
                origem,
                "REINTIMACAO"
        ));
        return true;
    }


    @Transactional
    public boolean dispatchJudicialOrder(WorkItem item,
                                         boolean cienciaObrigatoria,
                                         String janelaTerritorial,
                                         String tipoCumprimento,
                                         String originLabel) {
        if (item == null || item.getId() == null || item.getAssignedUser() == null) {
            return false;
        }
        Usuario destino = item.getAssignedUser();
        if (destino.getId() == null || !OFFICIAL_ROLES.contains(destino.getTipoUsuario())) {
            return false;
        }
        Processo processo = item.getProcesso();
        Long processoId = processo != null ? processo.getId() : null;
        String titulo = judicialOrderTitle(item);
        if (notificationHistoryRepository.existsByUsuarioIdAndProcessoIdAndTituloAndStatusAndEnviadoEmAfter(
                destino.getId(),
                processoId,
                titulo,
                "ENVIADO",
                LocalDateTime.now().minusMinutes(10)
        )) {
            return false;
        }
        String processoNumero = processNumber(processo);
        String vara = processVara(processo);
        String tribunal = processo != null ? firstNonBlank(processo.getTribunal(), "TRIBUNAL_NAO_IDENTIFICADO") : "TRIBUNAL_NAO_IDENTIFICADO";
        String cidade = processo != null ? firstNonBlank(processo.getComarca(), destino.getComarca()) : destino.getComarca();
        String regiao = contextEnvelopeService.resolveRegiaoJudicial(tribunal, processo != null ? processo.getUf() : destino.getUf(), contextEnvelopeService.resolveEsfera(destino, processo, destino.getTipoUsuario()));
        String prazo = item.getDueAt() != null ? item.getDueAt().toString() : "SEM_PRAZO_FATAL_DEFINIDO";
        String origem = firstNonBlank(originLabel, "JUIZO_DECISORIO");
        String territory = janelaTerritorial == null || janelaTerritorial.isBlank() ? "janela territorial a confirmar" : janelaTerritorial;
        String cumprimento = tipoCumprimento == null || tipoCumprimento.isBlank() ? "cumprimento judicial" : humanize(tipoCumprimento);
        String message = "Ordem judicial de cumprimento recebida no processo " + processoNumero
                + " · " + vara
                + " · " + tribunal
                + " · " + firstNonBlank(cidade, "CIDADE_NAO_IDENTIFICADA")
                + " · região " + regiao
                + " · origem " + origem
                + " · tipo " + cumprimento
                + ". Janela territorial: " + territory
                + ". Ciência obrigatória: " + (cienciaObrigatoria ? "SIM" : "NAO")
                + ". Encerramento com ofício original governado only. Prazo " + prazo + '.';
        String path = processoId != null
                ? "/api/v1/oficial-justica/processos-nomeados/" + processoId + "/workbench"
                : "/api/v1/oficial-justica/diligencias/fila-viva";
        notificationEventPublisher.publish(buildEnvelope(
                destino,
                processo,
                item,
                titulo,
                "ORDEM_JUDICIAL_CUMPRIMENTO_OFICIAL",
                message,
                path,
                true,
                territory,
                cumprimento,
                origem
        ));
        return true;
    }

    @Transactional
    public int materializarNomeacoesAtivasUsuario(Usuario usuario, int limit) {
        if (usuario == null || usuario.getId() == null || !OFFICIAL_ROLES.contains(usuario.getTipoUsuario())) {
            return 0;
        }
        int dispatched = 0;
        for (WorkItem item : activeNominationItemsForUser(usuario, Math.max(1, Math.min(limit, 40)))) {
            try {
                if (dispatchInstantNomination(item)) {
                    dispatched++;
                }
            } catch (Exception ex) {
                log.debug("falha_materializacao_nomeacao_oficial userId={} workItemId={} err={}", usuario.getId(), item.getId(), ex.getMessage());
            }
        }
        return dispatched;
    }


    @Transactional(readOnly = true)
    public Map<String, Object> runtimeStatus() {
        Usuario usuario = contextFactory.build().usuario();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("mode", "OFICIAL_NOTIFICATION_CENTER_V3");
        out.put("deliveryBus", deliveryBusStatus());
        out.put("streamPath", "/api/v1/oficial-justica/live/stream");
        out.put("feedPath", "/api/v1/oficial-justica/notificacoes?limit=20");
        out.put("oficialResponsavel", contextEnvelopeService.oficialEnvelope(usuario, null));
        return safeCopy(out);
    }

    private OficialJusticaNotificationEnvelope buildEnvelope(Usuario destino,
                                                             Processo processo,
                                                             WorkItem item,
                                                             String titulo,
                                                             String notificationType,
                                                             String message,
                                                             String path,
                                                             boolean highPriority,
                                                             String territorialWindow,
                                                             String complianceType,
                                                             String originLabel) {
        return new OficialJusticaNotificationEnvelope(
                destino == null ? null : destino.getId(),
                processo == null ? null : processo.getId(),
                item == null ? null : item.getId(),
                notificationType + ":" + (item == null || item.getId() == null ? "SEM_WORKITEM" : item.getId()),
                notificationType,
                titulo,
                message,
                path,
                highPriority,
                processNumber(processo),
                territorialWindow,
                complianceType,
                originLabel,
                Instant.now()
        );
    }

    private Map<String, Object> deliveryBusStatus() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("transport", kafkaEnabled ? "KAFKA" : "LOCAL_FALLBACK");
        out.put("kafkaEnabled", kafkaEnabled);
        out.put("topic", OficialJusticaNotificationEventPublisher.TOPIC);
        out.put("consumerGroup", OficialJusticaNotificationEventPublisher.GROUP_ID);
        out.put("runtimeStatus", kafkaEnabled ? "CONFIGURED_KAFKA_PIPELINE" : "LOCAL_DISPATCH_ONLY");
        out.put("streamBackchannel", "SSE+OUTBOX");
        out.put("dedupeByHistory", Boolean.TRUE);
        out.put("instantDelivery", Boolean.TRUE);
        return safeCopy(out);
    }

    private List<Map<String, Object>> centralizedRows(Usuario usuario, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<NotificationHistory> history = notificationHistoryRepository.findByUsuarioIdOrderByEnviadoEmDesc(usuario.getId(), PageRequest.of(0, safeLimit * 2));
        List<WorkItem> activeNominationItems = activeNominationItemsForUser(usuario, safeLimit * 2);
        LinkedHashMap<String, Map<String, Object>> rows = new LinkedHashMap<>();
        for (NotificationHistory item : history) {
            rows.put(key(item), toRow(item));
        }
        for (WorkItem item : activeNominationItems) {
            String key = key(item);
            rows.putIfAbsent(key, toSynthesizedNominationRow(item));
        }
        return rows.values().stream()
                .sorted(Comparator.comparing(this::sortInstant, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();
    }

    private List<WorkItem> activeNominationItemsForUser(Usuario usuario, int limit) {
        if (usuario == null || usuario.getId() == null) {
            return List.of();
        }
        Instant cut = Instant.now().minus(7, ChronoUnit.DAYS);
        List<WorkItem> recent = workItemRepository.findRecentAssignedActiveByUserAndRoles(usuario.getId(), OFFICIAL_ROLES, cut, PageRequest.of(0, Math.max(1, Math.min(limit, 60))));
        return panelEgressService.reconcileVisibility(usuario, recent).visibleItems();
    }

    private Map<String, Object> toRow(NotificationHistory history) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        Long processoId = history.getProcessoId();
        String workbenchPath = processoId != null ? "/api/v1/oficial-justica/processos-nomeados/" + processoId + "/workbench" : "/api/v1/oficial-justica/diligencias/fila-viva";
        putIfNotNull(row, "titulo", history.getTitulo());
        putIfNotNull(row, "mensagem", history.getMensagem());
        putIfNotNull(row, "canal", history.getCanal());
        putIfNotNull(row, "status", history.getStatus());
        putIfNotNull(row, "processoId", processoId);
        putIfNotNull(row, "enviadoEm", history.getEnviadoEm());
        putIfNotNull(row, "trackingToken", history.getTrackingToken());
        putIfNotNull(row, "cienteConfirmadoEm", history.getCienciaConfirmadaEm());
        row.put("tipo", notificationType(history));
        row.put("centralizadaNoProcesso", Boolean.TRUE);
        row.put("agendaPath", "/api/v1/oficial-justica/agenda-operacional");
        putIfNotNull(row, "workbenchPath", workbenchPath);
        putIfNotNull(row, "balcaoVirtualPath", processoId != null ? "/api/v1/oficial-justica/balcao-virtual/processos/" + processoId + "/sala" : null);
        row.put("lida", history.getLidoEm() != null);
        row.put("cienciaPendente", history.getCienciaConfirmadaEm() == null && requiresCiencia(history));
        row.put("cienciaPath", processoId != null && requiresCiencia(history) ? "/api/v1/oficial-justica/processos/" + processoId + "/ciente-intimacao" : null);
        row.put("instant", Boolean.TRUE);
        row.put("unidadeContexto", contextEnvelopeService.oficialEnvelope(contextFactory.build().usuario(), null));
        return safeCopy(row);
    }

    private Map<String, Object> toSynthesizedNominationRow(WorkItem item) {
        Processo processo = item.getProcesso();
        Long processoId = processo != null ? processo.getId() : null;
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("titulo", itemNotificationTitle(item));
        row.put("mensagem", itemNotificationMessage(item));
        row.put("canal", "CENTRAL_PROCESSUAL");
        row.put("status", "ENFILEIRADA_CENTRALIZADA");
        putIfNotNull(row, "processoId", processoId);
        row.put("processoNumero", processNumber(processo));
        putIfNotNull(row, "enviadoEm", toLocalDateTime(item.getUpdatedAt() != null ? item.getUpdatedAt() : item.getCreatedAt()));
        row.put("tipo", itemNotificationType(item));
        row.put("centralizadaNoProcesso", Boolean.TRUE);
        row.put("agendaPath", "/api/v1/oficial-justica/agenda-operacional");
        row.put("workbenchPath", processoId != null ? "/api/v1/oficial-justica/processos-nomeados/" + processoId + "/workbench" : "/api/v1/oficial-justica/diligencias/fila-viva");
        putIfNotNull(row, "balcaoVirtualPath", processoId != null ? "/api/v1/oficial-justica/balcao-virtual/processos/" + processoId + "/sala" : null);
        row.put("lida", Boolean.FALSE);
        row.put("instant", Boolean.TRUE);
        row.put("sintetica", Boolean.TRUE);
        row.put("workItemId", item.getId());
        row.put("unidadeContexto", contextEnvelopeService.processEnvelope(item.getAssignedUser(), processo, item, null, null));
        row.put("reativavelPorReintimacao", Boolean.TRUE);
        row.put("cienciaPendente", Boolean.TRUE);
        row.put("cienciaPath", processoId != null ? "/api/v1/oficial-justica/processos/" + processoId + "/ciente-intimacao" : null);
        return safeCopy(row);
    }

    private Map<String, Object> toNominationDigest(WorkItem item) {
        Processo processo = item.getProcesso();
        Long processoId = processo != null ? processo.getId() : null;
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("workItemId", item.getId());
        putIfNotNull(row, "processoId", processo != null ? processo.getId() : null);
        row.put("processoNumero", processNumber(processo));
        row.put("vara", processVara(processo));
        putIfNotNull(row, "prazoFatalEm", item.getDueAt());
        row.put("agendaPath", "/api/v1/oficial-justica/agenda-operacional");
        row.put("workbenchPath", processo != null && processo.getId() != null ? "/api/v1/oficial-justica/processos-nomeados/" + processo.getId() + "/workbench" : "/api/v1/oficial-justica/diligencias/fila-viva");
        row.put("unidadeContexto", contextEnvelopeService.processEnvelope(item.getAssignedUser(), processo, item, null, null));
        row.put("reativavelPorReintimacao", Boolean.TRUE);
        row.put("cienciaPendente", Boolean.TRUE);
        row.put("cienciaPath", processoId != null ? "/api/v1/oficial-justica/processos/" + processoId + "/ciente-intimacao" : null);
        return safeCopy(row);
    }

    private boolean isNominationNotification(NotificationHistory history) {
        return history != null && history.getTitulo() != null && (history.getTitulo().startsWith("NOMEACAO_OFICIAL:") || history.getTitulo().startsWith("REINTIMACAO_OFICIAL:") || history.getTitulo().startsWith("ORDEM_JUDICIAL_OFICIAL:"));
    }

    private boolean requiresCiencia(NotificationHistory history) {
        return isNominationNotification(history);
    }

    private String notificationType(NotificationHistory history) {
        if (history == null || history.getTitulo() == null) {
            return "GERAL";
        }
        if (history.getTitulo().startsWith("ORDEM_JUDICIAL_OFICIAL:")) {
            return "ORDEM_JUDICIAL_CUMPRIMENTO";
        }
        if (history.getTitulo().startsWith("REINTIMACAO_OFICIAL:")) {
            return "REINTIMACAO_PROCESSUAL";
        }
        return history.getTitulo().startsWith("NOMEACAO_OFICIAL:") ? "NOMEACAO_PROCESSUAL" : "GERAL";
    }

    private String itemNotificationTitle(WorkItem item) {
        if (isJudicialOrderItem(item)) {
            return judicialOrderTitle(item);
        }
        return isReactivationItem(item) ? reactivationTitle(item) : nominationTitle(item);
    }

    private String itemNotificationMessage(WorkItem item) {
        if (isJudicialOrderItem(item)) {
            return judicialOrderMessage(item);
        }
        return isReactivationItem(item) ? reactivationMessage(item) : nominationMessage(item);
    }

    private String itemNotificationType(WorkItem item) {
        if (isJudicialOrderItem(item)) {
            return "ORDEM_JUDICIAL_CUMPRIMENTO";
        }
        return isReactivationItem(item) ? "REINTIMACAO_PROCESSUAL" : "NOMEACAO_PROCESSUAL";
    }

    private boolean isJudicialOrderItem(WorkItem item) {
        return item != null && item.getTitulo() != null && item.getTitulo().startsWith("Ordem judicial de cumprimento — ");
    }

    private boolean isReactivationItem(WorkItem item) {
        return item != null && item.getTemplateCode() != null && item.getTemplateCode().startsWith("REATIVACAO_OFICIAL_REINTIMACAO:");
    }

    private String nominationTitle(WorkItem item) {
        return "NOMEACAO_OFICIAL:" + item.getId();
    }

    private String reactivationTitle(WorkItem item) {
        return "REINTIMACAO_OFICIAL:" + item.getId();
    }

    private String judicialOrderTitle(WorkItem item) {
        return "ORDEM_JUDICIAL_OFICIAL:" + item.getId();
    }

    private String nominationMessage(WorkItem item) {
        Processo processo = item.getProcesso();
        String prazo = item.getDueAt() != null ? item.getDueAt().toString() : "sem prazo fatal definido";
        String rito = processo != null && processo.getRito() != null ? humanize(processo.getRito().name()) : "rito não identificado";
        return "Nomeação centralizada no processo " + processNumber(processo) + " para cumprimento na " + processVara(processo) + ", com " + rito + " e prazo " + prazo + ".";
    }

    private String reactivationMessage(WorkItem item) {
        Processo processo = item.getProcesso();
        String prazo = item.getDueAt() != null ? item.getDueAt().toString() : "sem prazo fatal definido";
        return "Reintimação auditável centralizada no processo " + processNumber(processo) + " para novo cumprimento do Oficial na " + processVara(processo) + ", com reaparição automática no painel e prazo " + prazo + ".";
    }

    private String judicialOrderMessage(WorkItem item) {
        Processo processo = item.getProcesso();
        String prazo = item.getDueAt() != null ? item.getDueAt().toString() : "sem prazo fatal definido";
        return "Ordem judicial centralizada no processo " + processNumber(processo) + " para cumprimento do Oficial na " + processVara(processo) + ", com ciência obrigatória e prazo " + prazo + ".";
    }

    private String processNumber(Processo processo) {
        if (processo == null) {
            return "PROCESSO_NAO_IDENTIFICADO";
        }
        return firstNonBlank(processo.getNumeroProcesso(), processo.getNumero(), processo.getNumeroUnificado(), "PROCESSO_NAO_IDENTIFICADO");
    }

    private String processVara(Processo processo) {
        return processo == null ? "VARA_NAO_IDENTIFICADA" : firstNonBlank(processo.getVara(), processo.getComarca(), "VARA_NAO_IDENTIFICADA");
    }

    private String key(NotificationHistory history) {
        return history.getTitulo() + ':' + history.getProcessoId() + ':' + history.getUsuarioId();
    }

    private String key(WorkItem item) {
        return itemNotificationTitle(item) + ':' + (item.getProcesso() != null ? item.getProcesso().getId() : null) + ':' + item.getAssignedUser().getId();
    }

    private Instant sortInstant(Map<String, Object> row) {
        Object value = row.get("enviadoEm");
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        return null;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Map<String, Object> safeCopy(Map<String, Object> input) {
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && value != null) {
                safe.put(key, value);
            }
        });
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }

    private void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (target != null && key != null && value != null) {
            target.put(key, value);
        }
    }

    private String humanize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "não identificado";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
