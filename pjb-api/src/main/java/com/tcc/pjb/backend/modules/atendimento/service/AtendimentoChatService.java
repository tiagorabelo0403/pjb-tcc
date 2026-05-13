package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.core.moderation.ContentBlockedException;
import com.tcc.pjb.backend.core.moderation.TextModerationService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoAdvogadoDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoCreateThreadRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoMarkDeliveredRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoMarkReadRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoMessageDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoSendMessageRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoThreadDto;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoDeliveryState;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessage;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachmentId;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageStatus;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReadState;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThreadPolicy;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoThreadStatus;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoDeliveryStateRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageReceiptRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoReadStateRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadPolicyRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadRepository;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtendimentoChatService {

    private final CurrentUserService currentUser;
    private final Clock clock;
    private final ProcessoRepository processoRepository;
    private final LaianeProcuracaoRepository procuracaoRepository;
    private final ClienteRepository clienteRepository;
    private final AtendimentoThreadRepository threadRepository;
    private final AtendimentoMessageRepository messageRepository;
    private final AtendimentoReadStateRepository readStateRepository;
    private final AtendimentoDeliveryStateRepository deliveryStateRepository;
    private final TextModerationService moderationService;
    private final AtendimentoModerationEventService moderationEventService;
    private final AtendimentoTosService tosService;
    private final AtendimentoMessageAttachmentRepository messageAttachmentRepository;
    private final AtendimentoMessageReceiptRepository receiptRepository;
    private final AtendimentoThreadPolicyRepository policyRepository;
    private final AtendimentoChatAccessSupport accessSupport;
    private final AtendimentoChatThreadViewSupport threadViewSupport;
    private final AtendimentoChatMessagingSupport messagingSupport;
    private final boolean attachmentsEnabled;
    private final int attachmentMaxPerMessage;
    private final long attachmentMaxTotalBytesPerMessage;

    public AtendimentoChatService(CurrentUserService currentUser,
                                  Clock clock,
                                  ProcessoRepository processoRepository,
                                  LaianeProcuracaoRepository procuracaoRepository,
                                  ClienteRepository clienteRepository,
                                  AtendimentoThreadRepository threadRepository,
                                  AtendimentoMessageRepository messageRepository,
                                  AtendimentoReadStateRepository readStateRepository,
                                  AtendimentoDeliveryStateRepository deliveryStateRepository,
                                  TextModerationService moderationService,
                                  AtendimentoModerationEventService moderationEventService,
                                  AtendimentoTosService tosService,
                                  AtendimentoMessageAttachmentRepository messageAttachmentRepository,
                                  AtendimentoMessageReceiptRepository receiptRepository,
                                  AtendimentoThreadPolicyRepository policyRepository,
                                  AtendimentoChatAccessSupport accessSupport,
                                  AtendimentoChatThreadViewSupport threadViewSupport,
                                  AtendimentoChatMessagingSupport messagingSupport,
                                  @Value("${pjb.atendimento.attachments.enabled:false}") boolean attachmentsEnabled,
                                  @Value("${pjb.atendimento.attachments.maxPerMessage:3}") int attachmentMaxPerMessage,
                                  @Value("${pjb.atendimento.attachments.maxTotalBytesPerMessage:20971520}") long attachmentMaxTotalBytesPerMessage) {
        this.currentUser = Objects.requireNonNull(currentUser);
        this.clock = Objects.requireNonNull(clock);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.procuracaoRepository = Objects.requireNonNull(procuracaoRepository);
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
        this.threadRepository = Objects.requireNonNull(threadRepository);
        this.messageRepository = Objects.requireNonNull(messageRepository);
        this.readStateRepository = Objects.requireNonNull(readStateRepository);
        this.deliveryStateRepository = Objects.requireNonNull(deliveryStateRepository);
        this.moderationService = Objects.requireNonNull(moderationService);
        this.moderationEventService = Objects.requireNonNull(moderationEventService);
        this.tosService = Objects.requireNonNull(tosService);
        this.messageAttachmentRepository = Objects.requireNonNull(messageAttachmentRepository);
        this.receiptRepository = Objects.requireNonNull(receiptRepository);
        this.policyRepository = Objects.requireNonNull(policyRepository);
        this.accessSupport = Objects.requireNonNull(accessSupport);
        this.threadViewSupport = Objects.requireNonNull(threadViewSupport);
        this.messagingSupport = Objects.requireNonNull(messagingSupport);
        this.attachmentsEnabled = attachmentsEnabled;
        this.attachmentMaxPerMessage = Math.max(0, attachmentMaxPerMessage);
        this.attachmentMaxTotalBytesPerMessage = Math.max(0L, attachmentMaxTotalBytesPerMessage);
    }

    @Transactional(readOnly = true)
    public AtendimentoThread requireThreadAccess(Long threadId) {
        tosService.requireAccepted();
        AtendimentoThread thread = threadRepository.findById(threadId).orElseThrow();
        accessSupport.enforceThreadAccess(currentUser.getRequired(), thread);
        return thread;
    }

    @Transactional(readOnly = true)
    public Page<AtendimentoThreadDto> listThreads(Pageable pageable) {
        tosService.requireAccepted();
        return threadViewSupport.listThreads(currentUser.getRequired(), pageable);
    }

    @Transactional(readOnly = true)
    public List<AtendimentoThreadDto> listThreadsForProcesso(Long processoId) {
        tosService.requireAccepted();
        if (processoId == null) {
            throw new IllegalArgumentException("processoId");
        }
        Usuario actor = currentUser.getRequired();
        Processo processo = processoRepository.findById(processoId).orElseThrow();
        accessSupport.enforceRead(actor, processo);
        return threadViewSupport.listThreadsForProcesso(actor, processo, processoId);
    }

    @Transactional(readOnly = true)
    public List<AtendimentoAdvogadoDto> listAdvogadosForProcesso(Long processoId) {
        tosService.requireAccepted();
        if (processoId == null) {
            throw new IllegalArgumentException("processoId");
        }
        Usuario actor = currentUser.getRequired();
        Processo processo = processoRepository.findById(processoId).orElseThrow();
        accessSupport.enforceRead(actor, processo);
        return threadViewSupport.listAdvogadosForProcesso(actor, processoId);
    }

    @Transactional
    public AtendimentoThreadDto createThread(AtendimentoCreateThreadRequest request) {
        tosService.requireAccepted();
        if (request == null) {
            throw new IllegalArgumentException("req");
        }
        Long processoId = request.processoId();
        if (processoId == null) {
            throw new IllegalArgumentException("processoId");
        }

        Usuario actor = currentUser.getRequired();
        Processo processo = processoRepository.findById(processoId).orElseThrow();

        if (actor.getTipoUsuario() == TipoUsuario.CIDADAO) {
            Long advogadoId = request.advogadoId();
            if (advogadoId == null) {
                throw new IllegalArgumentException("advogadoId");
            }
            accessSupport.enforceRead(actor, processo);

            String cpfHash = AtendimentoChatSupportUtils.cpfHash(actor.getCpf());
            boolean isClient = clienteRepository.existsByCpfHashAndAdvogado_Id(cpfHash, advogadoId);
            if (!isClient) {
                throw new AccessDeniedException("Acesso negado");
            }
            boolean hasProcesso = procuracaoRepository.existsByAdvogado_IdAndProcessoIdAndStatus(advogadoId, processoId, LaianeProcuracaoStatus.ATIVA);
            if (!hasProcesso) {
                throw new AccessDeniedException("Acesso negado");
            }
            AtendimentoThread thread = threadRepository.findByProcessoIdAndAdvogadoIdAndCidadaoUsuarioId(processoId, advogadoId, actor.getId())
                    .orElseGet(() -> createNewThread(processoId, advogadoId, actor, cpfHash));
            return threadViewSupport.toThreadDto(actor, thread, processo);
        }

        if (actor.getTipoUsuario() == TipoUsuario.ADVOGADO) {
            boolean hasProcesso = procuracaoRepository.existsByAdvogado_IdAndProcessoIdAndStatus(actor.getId(), processoId, LaianeProcuracaoStatus.ATIVA);
            if (!hasProcesso) {
                throw new AccessDeniedException("Acesso negado");
            }
            Usuario cidadao = accessSupport.resolveCidadao(request);
            if (cidadao.getTipoUsuario() != TipoUsuario.CIDADAO) {
                throw new AccessDeniedException("Acesso negado");
            }
            String cpfNormalizado = AtendimentoChatSupportUtils.digitsOnly(cidadao.getCpf());
            boolean isParte = Objects.equals(cpfNormalizado, AtendimentoChatSupportUtils.digitsOnly(processo.getParteAutoraCpf()))
                    || Objects.equals(cpfNormalizado, AtendimentoChatSupportUtils.digitsOnly(processo.getParteReuCpf()));
            if (!isParte) {
                throw new AccessDeniedException("Acesso negado");
            }
            String cpfHash = AtendimentoChatSupportUtils.cpfHash(cpfNormalizado);
            boolean isClient = clienteRepository.existsByCpfHashAndAdvogado_Id(cpfHash, actor.getId());
            if (!isClient) {
                throw new AccessDeniedException("Acesso negado");
            }
            AtendimentoThread thread = threadRepository.findByProcessoIdAndAdvogadoIdAndCidadaoUsuarioId(processoId, actor.getId(), cidadao.getId())
                    .orElseGet(() -> createNewThread(processoId, actor.getId(), cidadao, cpfHash));
            return threadViewSupport.toThreadDto(actor, thread, processo);
        }

        throw new AccessDeniedException("Acesso negado");
    }

    @Transactional(readOnly = true)
    public Page<AtendimentoMessageDto> listMessages(Long threadId, Long afterId, Long beforeId, int limit) {
        tosService.requireAccepted();
        AtendimentoThread thread = threadRepository.findById(threadId).orElseThrow();
        Usuario actor = currentUser.getRequired();
        accessSupport.enforceThreadAccess(actor, thread);

        Long otherUserId = AtendimentoChatSupportUtils.otherUserId(actor, thread);
        Long otherLastRead = resolveOtherLastRead(threadId, otherUserId);
        Long otherLastDelivered = resolveOtherLastDelivered(threadId, otherUserId);

        int pageSize = Math.min(Math.max(limit, 1), 200);
        Pageable pageable = PageRequest.of(0, pageSize);
        Page<AtendimentoMessage> page;
        if (afterId != null) {
            page = messageRepository.findByThreadIdAndIdGreaterThanOrderByIdAsc(threadId, afterId, pageable);
        } else if (beforeId != null) {
            page = messageRepository.findByThreadIdAndIdLessThanOrderByIdDesc(threadId, beforeId, pageable);
        } else {
            page = messageRepository.findByThreadIdOrderByIdDesc(threadId, pageable);
        }

        List<AtendimentoMessageDto> output = messagingSupport.toDtosWithAttachmentsAndReply(page.getContent(), actor.getId(), otherUserId, otherLastDelivered, otherLastRead);
        if (beforeId != null || afterId == null) {
            output = AtendimentoChatSupportUtils.reverse(output);
        }
        return new PageImpl<>(output, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<AtendimentoMessageDto> listMessagesAround(Long threadId, Long messageId, int beforeCount, int afterCount) {
        tosService.requireAccepted();
        if (threadId == null) {
            throw new IllegalArgumentException("threadId");
        }
        if (messageId == null) {
            throw new IllegalArgumentException("messageId");
        }

        AtendimentoThread thread = threadRepository.findById(threadId).orElseThrow();
        Usuario actor = currentUser.getRequired();
        accessSupport.enforceThreadAccess(actor, thread);

        AtendimentoMessage center = messageRepository.findById(messageId).orElseThrow();
        if (!threadId.equals(center.getThreadId())) {
            throw new AccessDeniedException("Acesso negado");
        }

        int before = Math.min(Math.max(0, beforeCount), 200);
        int after = Math.min(Math.max(0, afterCount), 200);
        Page<AtendimentoMessage> beforePage = before == 0
                ? Page.empty()
                : messageRepository.findByThreadIdAndIdLessThanOrderByIdDesc(threadId, messageId, PageRequest.of(0, before));
        List<AtendimentoMessage> previous = new ArrayList<>(beforePage.getContent());
        java.util.Collections.reverse(previous);
        Page<AtendimentoMessage> afterPage = after == 0
                ? Page.empty()
                : messageRepository.findByThreadIdAndIdGreaterThanOrderByIdAsc(threadId, messageId, PageRequest.of(0, after));
        List<AtendimentoMessage> next = new ArrayList<>(afterPage.getContent());

        List<AtendimentoMessage> all = new ArrayList<>(previous.size() + 1 + next.size());
        all.addAll(previous);
        all.add(center);
        all.addAll(next);

        Long otherUserId = AtendimentoChatSupportUtils.otherUserId(actor, thread);
        Long otherLastRead = resolveOtherLastRead(threadId, otherUserId);
        Long otherLastDelivered = resolveOtherLastDelivered(threadId, otherUserId);
        return messagingSupport.toDtosWithAttachmentsAndReply(all, actor.getId(), otherUserId, otherLastDelivered, otherLastRead);
    }

    @Transactional
    public AtendimentoMessageDto sendMessage(Long threadId, AtendimentoSendMessageRequest request) {
        tosService.requireAccepted();
        String body = AtendimentoChatSupportUtils.normalizeBody(request != null ? request.body() : null);
        List<Long> attachmentIds = request != null && request.attachmentIds() != null ? request.attachmentIds() : List.of();
        Long replyToId = request != null ? request.replyToMessageId() : null;
        UUID clientMessageId = request != null ? request.clientMessageId() : null;
        if ((body == null || body.isBlank()) && attachmentIds.isEmpty()) {
            throw new IllegalArgumentException("body/attachmentIds");
        }

        AtendimentoThread thread = threadRepository.findByIdForUpdate(threadId).orElseThrow();
        Usuario actor = currentUser.getRequired();
        accessSupport.enforceThreadAccess(actor, thread);
        enforceCidadaoSendWindow(actor, threadId);

        Long otherUserId = AtendimentoChatSupportUtils.otherUserId(actor, thread);
        Long otherLastRead = resolveOtherLastRead(threadId, otherUserId);
        Long otherLastDelivered = resolveOtherLastDelivered(threadId, otherUserId);

        if (clientMessageId != null) {
            Optional<AtendimentoMessage> existing = messageRepository.findByThreadIdAndClientMessageId(threadId, clientMessageId);
            if (existing.isPresent()) {
                return messagingSupport.toDtosWithAttachmentsAndReply(List.of(existing.get()), actor.getId(), otherUserId, otherLastDelivered, otherLastRead).get(0);
            }
        }

        AtendimentoMessage replyTarget = resolveReplyTarget(threadId, replyToId, actor.getId());
        if (body != null) {
            try {
                body = moderationService.validateMessage(body);
            } catch (ContentBlockedException exception) {
                moderationEventService.recordBlockedAttempt(actor, thread, exception.reason(), body);
                throw exception;
            }
        }
        if (thread.getStatus() == AtendimentoThreadStatus.ENCERRADO) {
            throw new AccessDeniedException("Thread encerrada");
        }

        List<AtendimentoAttachment> attachments = messagingSupport.validateAttachments(
                threadId,
                attachmentIds,
                attachmentsEnabled,
                attachmentMaxPerMessage,
                attachmentMaxTotalBytesPerMessage
        );
        boolean allReady = attachments.stream().allMatch(att -> att.getStatus() == com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachmentStatus.READY);
        Instant now = Instant.now(clock);
        String prevHash = messageRepository.findTopByThreadIdOrderByIdDesc(threadId).map(AtendimentoMessage::getMsgHash).orElse(null);
        String senderTipo = actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : "UNKNOWN";
        String messageHash = AtendimentoChatSupportUtils.computeMsgHash(prevHash, threadId, actor.getId(), senderTipo, now, body, replyToId, attachmentIds);

        AtendimentoMessageStatus status = allReady ? AtendimentoMessageStatus.DELIVERED : AtendimentoMessageStatus.QUARANTINED;
        AtendimentoMessage message = messageRepository.save(AtendimentoMessage.builder()
                .threadId(threadId)
                .senderUsuarioId(actor.getId())
                .senderTipo(senderTipo)
                .body(body != null ? body : "")
                .replyToMessageId(replyToId)
                .clientMessageId(clientMessageId)
                .status(status)
                .prevHash(prevHash)
                .msgHash(messageHash)
                .createdAt(now)
                .build());

        if (!attachments.isEmpty()) {
            List<AtendimentoMessageAttachment> links = new ArrayList<>(attachments.size());
            for (AtendimentoAttachment attachment : attachments) {
                AtendimentoMessageAttachment link = new AtendimentoMessageAttachment();
                link.setId(new AtendimentoMessageAttachmentId(message.getId(), attachment.getId()));
                link.setThreadId(threadId);
                links.add(link);
            }
            messageAttachmentRepository.saveAll(links);
        }

        thread.setUpdatedAt(now);
        if (status == AtendimentoMessageStatus.DELIVERED) {
            thread.setLastMessageId(message.getId());
        }
        threadRepository.save(thread);
        upsertReadInternal(threadId, actor.getId(), message.getId(), now);

        if (otherUserId != null && message.getId() != null) {
            try {
                receiptRepository.save(com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageReceipt.builder()
                        .messageId(message.getId())
                        .threadId(threadId)
                        .usuarioId(otherUserId)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            } catch (Exception ignored) {
            }
        }

        if (status == AtendimentoMessageStatus.DELIVERED) {
            messagingSupport.publishInboxEvent(thread, message);
            messagingSupport.publishUiInboxNotificationNewMessage(thread, message, actor, otherUserId);
        }

        AtendimentoMessageDto dto = messagingSupport.toDtosWithAttachmentsAndReply(List.of(message), actor.getId(), otherUserId, otherLastDelivered, otherLastRead).get(0);
        if (replyTarget != null && dto.replyTo() == null) {
            return messagingSupport.toDtosWithAttachmentsAndReply(List.of(message), actor.getId(), otherUserId, otherLastDelivered, otherLastRead).get(0);
        }
        return dto;
    }

    @Transactional
    public void markRead(Long threadId, AtendimentoMarkReadRequest request) {
        tosService.requireAccepted();
        AtendimentoThread thread = threadRepository.findById(threadId).orElseThrow();
        Usuario actor = currentUser.getRequired();
        accessSupport.enforceThreadAccess(actor, thread);

        Long lastRead = request != null ? request.lastReadMessageId() : null;
        if (lastRead == null) {
            lastRead = thread.getLastMessageId();
        }
        if (lastRead == null) {
            return;
        }
        Long cap = thread.getLastMessageId();
        if (cap != null && lastRead > cap) {
            lastRead = cap;
        }

        Instant now = Instant.now(clock);
        AtendimentoReadState readState = readStateRepository.findByThreadIdAndUsuarioId(threadId, actor.getId())
                .orElseGet(() -> {
                    AtendimentoReadState state = new AtendimentoReadState();
                    state.setThreadId(threadId);
                    state.setUsuarioId(actor.getId());
                    return state;
                });
        Long previous = readState.getLastReadMessageId();
        boolean advanced;
        if (previous != null && lastRead <= previous) {
            readState.setUpdatedAt(now);
            readStateRepository.save(readState);
            advanced = false;
        } else {
            readState.setLastReadMessageId(lastRead);
            readState.setUpdatedAt(now);
            readStateRepository.save(readState);
            advanced = true;
        }
        if (advanced) {
            long fromId = previous == null ? 0L : previous + 1L;
            long toId = lastRead;
            if (toId >= fromId) {
                receiptRepository.ensureReceiptsForRange(threadId, actor.getId(), fromId, toId, now);
                receiptRepository.markReadRange(threadId, actor.getId(), fromId, toId, now);
            }
            messagingSupport.notifyReadState(thread, actor, lastRead, now);
        }
    }

    @Transactional
    public void markDelivered(Long threadId, AtendimentoMarkDeliveredRequest request) {
        tosService.requireAccepted();
        AtendimentoThread thread = threadRepository.findById(threadId).orElseThrow();
        Usuario actor = currentUser.getRequired();
        accessSupport.enforceThreadAccess(actor, thread);

        Long lastDelivered = request != null ? request.lastDeliveredMessageId() : null;
        if (lastDelivered == null) {
            lastDelivered = thread.getLastMessageId();
        }
        if (lastDelivered == null) {
            return;
        }
        Long cap = thread.getLastMessageId();
        if (cap != null && lastDelivered > cap) {
            lastDelivered = cap;
        }

        Instant now = Instant.now(clock);
        AtendimentoDeliveryState deliveryState = deliveryStateRepository.findByThreadIdAndUsuarioId(threadId, actor.getId())
                .orElseGet(() -> {
                    AtendimentoDeliveryState state = new AtendimentoDeliveryState();
                    state.setThreadId(threadId);
                    state.setUsuarioId(actor.getId());
                    return state;
                });
        Long previous = deliveryState.getLastDeliveredMessageId();
        boolean advanced;
        if (previous != null && lastDelivered <= previous) {
            deliveryState.setUpdatedAt(now);
            deliveryStateRepository.save(deliveryState);
            advanced = false;
        } else {
            deliveryState.setLastDeliveredMessageId(lastDelivered);
            deliveryState.setUpdatedAt(now);
            deliveryStateRepository.save(deliveryState);
            advanced = true;
        }
        if (advanced) {
            long fromId = previous == null ? 0L : previous + 1L;
            long toId = lastDelivered;
            if (toId >= fromId) {
                receiptRepository.ensureReceiptsForRange(threadId, actor.getId(), fromId, toId, now);
                receiptRepository.markDeliveredRange(threadId, actor.getId(), fromId, toId, now);
            }
            messagingSupport.notifyDeliveredState(thread, actor, lastDelivered, now);
        }
    }

    @Transactional(readOnly = true)
    public void typing(Long threadId, boolean typing) {
        tosService.requireAccepted();
        AtendimentoThread thread = threadRepository.findById(threadId).orElseThrow();
        Usuario actor = currentUser.getRequired();
        accessSupport.enforceThreadAccess(actor, thread);
        enforceCidadaoSendWindow(actor, threadId);
        messagingSupport.notifyTyping(thread, actor, typing);
    }

    public String inboxTopicForCurrentUser() {
        tosService.requireAccepted();
        return messagingSupport.inboxTopicForUser(currentUser.getRequired().getId());
    }

    private AtendimentoThread createNewThread(Long processoId, Long advogadoId, Usuario cidadao, String cpfHash) {
        Instant now = Instant.now(clock);
        AtendimentoThread thread = AtendimentoThread.builder()
                .processoId(processoId)
                .advogadoId(advogadoId)
                .cidadaoUsuarioId(cidadao.getId())
                .cidadaoCpfHash(cpfHash)
                .status(AtendimentoThreadStatus.ATIVO)
                .createdAt(now)
                .updatedAt(now)
                .lastMessageId(null)
                .version(0L)
                .build();
        return threadRepository.save(thread);
    }

    private void enforceCidadaoSendWindow(Usuario actor, Long threadId) {
        if (actor.getTipoUsuario() != TipoUsuario.CIDADAO) {
            return;
        }
        AtendimentoThreadPolicy policy = policyRepository.findById(threadId).orElse(null);
        Instant disabledUntil = policy != null ? policy.getCidadaoSendDisabledUntil() : null;
        if (disabledUntil != null && Instant.now(clock).isBefore(disabledUntil)) {
            throw new AccessDeniedException("cidadao_send_disabled");
        }
    }

    private AtendimentoMessage resolveReplyTarget(Long threadId, Long replyToId, Long viewerUserId) {
        if (replyToId == null) {
            return null;
        }
        AtendimentoMessage target = messageRepository.findById(replyToId).orElseThrow(() -> new IllegalArgumentException("replyToMessageId"));
        if (!Objects.equals(target.getThreadId(), threadId)) {
            throw new IllegalArgumentException("replyToMessageId");
        }
        if (AtendimentoChatMessageMapper.isHiddenForViewer(target, viewerUserId)) {
            throw new IllegalArgumentException("reply_to_hidden");
        }
        return target;
    }

    private Long resolveOtherLastRead(Long threadId, Long otherUserId) {
        if (otherUserId == null) {
            return null;
        }
        return readStateRepository.findByThreadIdAndUsuarioId(threadId, otherUserId)
                .map(AtendimentoReadState::getLastReadMessageId)
                .orElse(null);
    }

    private Long resolveOtherLastDelivered(Long threadId, Long otherUserId) {
        if (otherUserId == null) {
            return null;
        }
        return deliveryStateRepository.findByThreadIdAndUsuarioId(threadId, otherUserId)
                .map(AtendimentoDeliveryState::getLastDeliveredMessageId)
                .orElse(null);
    }

    private void upsertReadInternal(Long threadId, Long usuarioId, Long lastReadId, Instant now) {
        AtendimentoReadState readState = readStateRepository.findByThreadIdAndUsuarioId(threadId, usuarioId)
                .orElseGet(() -> AtendimentoReadState.builder().threadId(threadId).usuarioId(usuarioId).updatedAt(now).build());
        readState.setLastReadMessageId(lastReadId);
        readState.setUpdatedAt(now);
        readStateRepository.save(readState);
    }
}
