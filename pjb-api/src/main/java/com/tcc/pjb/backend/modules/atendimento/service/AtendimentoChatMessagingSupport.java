package com.tcc.pjb.backend.modules.atendimento.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoMessageDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoMessageReplyPreviewDto;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachmentStatus;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessage;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageReceipt;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThreadMemberSettings;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageReceiptRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadMemberSettingsRepository;
import com.tcc.pjb.backend.modules.atendimento.util.AtendimentoParticipantLabelUtils;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
class AtendimentoChatMessagingSupport {

    private final Clock clock;
    private final ObjectMapper mapper;
    private final AtendimentoInboxLiveHub liveHub;
    private final UsuarioRepository usuarioRepository;
    private final ProcessoRepository processoRepository;
    private final UiHistoryService uiHistoryService;
    private final AtendimentoThreadMemberSettingsRepository settingsRepository;
    private final AtendimentoAttachmentRepository attachmentRepository;
    private final AtendimentoMessageAttachmentRepository messageAttachmentRepository;
    private final AtendimentoMessageReceiptRepository receiptRepository;
    private final AtendimentoMessageRepository messageRepository;

    AtendimentoChatMessagingSupport(Clock clock,
                                    ObjectMapper mapper,
                                    AtendimentoInboxLiveHub liveHub,
                                    UsuarioRepository usuarioRepository,
                                    ProcessoRepository processoRepository,
                                    UiHistoryService uiHistoryService,
                                    AtendimentoThreadMemberSettingsRepository settingsRepository,
                                    AtendimentoAttachmentRepository attachmentRepository,
                                    AtendimentoMessageAttachmentRepository messageAttachmentRepository,
                                    AtendimentoMessageReceiptRepository receiptRepository,
                                    AtendimentoMessageRepository messageRepository) {
        this.clock = Objects.requireNonNull(clock);
        this.mapper = Objects.requireNonNull(mapper);
        this.liveHub = Objects.requireNonNull(liveHub);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.uiHistoryService = Objects.requireNonNull(uiHistoryService);
        this.settingsRepository = Objects.requireNonNull(settingsRepository);
        this.attachmentRepository = Objects.requireNonNull(attachmentRepository);
        this.messageAttachmentRepository = Objects.requireNonNull(messageAttachmentRepository);
        this.receiptRepository = Objects.requireNonNull(receiptRepository);
        this.messageRepository = Objects.requireNonNull(messageRepository);
    }

    List<AtendimentoAttachment> validateAttachments(Long threadId,
                                                    List<Long> attachmentIds,
                                                    boolean attachmentsEnabled,
                                                    int attachmentMaxPerMessage,
                                                    long attachmentMaxTotalBytesPerMessage) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return List.of();
        }
        if (!attachmentsEnabled || attachmentMaxPerMessage <= 0) {
            throw new AccessDeniedException("attachments_disabled");
        }
        List<Long> uniqueIds = attachmentIds.stream().filter(Objects::nonNull).distinct().limit(attachmentMaxPerMessage + 1L).toList();
        if (uniqueIds.size() > attachmentMaxPerMessage) {
            throw new IllegalArgumentException("attachments_limit");
        }
        List<AtendimentoAttachment> attachments = new ArrayList<>(uniqueIds.size());
        long totalBytes = 0L;
        for (Long attachmentId : uniqueIds) {
            AtendimentoAttachment attachment = attachmentRepository.findByIdAndThreadId(attachmentId, threadId)
                    .orElseThrow(() -> new IllegalArgumentException("attachment"));
            if (attachment.getStatus() == AtendimentoAttachmentStatus.REJECTED || attachment.getStatus() == AtendimentoAttachmentStatus.EXPIRED) {
                throw new IllegalArgumentException("attachment_rejected");
            }
            attachments.add(attachment);
            totalBytes += Math.max(0L, attachment.getSizeBytes());
        }
        if (attachmentMaxTotalBytesPerMessage > 0 && totalBytes > attachmentMaxTotalBytesPerMessage) {
            throw new IllegalArgumentException("attachments_total_size");
        }
        return attachments;
    }

    List<AtendimentoMessageDto> toDtosWithAttachmentsAndReply(List<AtendimentoMessage> messages,
                                                              Long viewerUserId,
                                                              Long otherUserId,
                                                              Long otherLastDelivered,
                                                              Long otherLastRead) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<Long> messageIds = messages.stream().map(AtendimentoMessage::getId).filter(Objects::nonNull).toList();
        Set<Long> senderIds = messages.stream().map(AtendimentoMessage::getSenderUsuarioId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> replyIds = messages.stream().map(AtendimentoMessage::getReplyToMessageId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Usuario> senders = senderIds.isEmpty()
                ? Map.of()
                : usuarioRepository.findAllById(senderIds).stream().collect(Collectors.toMap(Usuario::getId, Function.identity()));
        Map<Long, AtendimentoMessage> replyTargets = replyIds.isEmpty()
                ? Map.of()
                : messageRepository.findAllById(new ArrayList<>(replyIds)).stream().collect(Collectors.toMap(AtendimentoMessage::getId, Function.identity()));
        Set<Long> replySenderIds = replyTargets.values().stream().map(AtendimentoMessage::getSenderUsuarioId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Usuario> replySenders = replySenderIds.isEmpty()
                ? Map.of()
                : usuarioRepository.findAllById(replySenderIds).stream().collect(Collectors.toMap(Usuario::getId, Function.identity()));
        Map<Long, AtendimentoMessageReceipt> receiptsByMessageId = (otherUserId == null || messageIds.isEmpty())
                ? Map.of()
                : receiptRepository.findByThreadIdAndUsuarioIdAndMessageIdIn(messages.get(0).getThreadId(), otherUserId, messageIds).stream()
                .collect(Collectors.toMap(AtendimentoMessageReceipt::getMessageId, Function.identity(), (left, right) -> left));

        Map<Long, List<AtendimentoAttachment>> attachmentsByMessageId = loadAttachmentsByMessageId(messageIds);

        List<AtendimentoMessageDto> output = new ArrayList<>(messages.size());
        for (AtendimentoMessage message : messages) {
            Usuario sender = senders.get(message.getSenderUsuarioId());
            AtendimentoMessage target = replyTargets.get(message.getReplyToMessageId());
            Usuario replySender = target != null ? replySenders.get(target.getSenderUsuarioId()) : null;
            AtendimentoMessageReplyPreviewDto preview = target == null
                    ? null
                    : AtendimentoChatMessageMapper.toReplyPreview(
                    target,
                    viewerUserId,
                    AtendimentoParticipantLabelUtils.displayName(replySender),
                    AtendimentoParticipantLabelUtils.participantLabel(replySender)
            );
            output.add(AtendimentoChatMessageMapper.toDto(
                    message,
                    attachmentsByMessageId.getOrDefault(message.getId(), List.of()),
                    viewerUserId,
                    preview,
                    otherLastDelivered,
                    otherLastRead,
                    receiptsByMessageId.get(message.getId()),
                    AtendimentoChatMessageMapper.senderDisplayName(message, sender),
                    AtendimentoChatMessageMapper.senderLabel(message, sender),
                    AtendimentoChatMessageMapper.senderOab(message, sender)
            ));
        }
        return output;
    }

    void publishInboxEvent(AtendimentoThread thread, AtendimentoMessage message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ATENDIMENTO_NEW_MESSAGE");
        payload.put("threadId", thread.getId());
        payload.put("processoId", thread.getProcessoId());
        payload.put("messageId", message.getId());
        payload.put("at", message.getCreatedAt() != null ? message.getCreatedAt().toString() : null);
        enqueuePayload(AtendimentoChatSupportUtils.topicForUser(thread.getAdvogadoId()), payload);
        enqueuePayload(AtendimentoChatSupportUtils.topicForUser(thread.getCidadaoUsuarioId()), payload);
    }

    void publishUiInboxNotificationNewMessage(AtendimentoThread thread, AtendimentoMessage message, Usuario sender, Long recipientUserId) {
        if (thread == null || message == null || sender == null || recipientUserId == null) {
            return;
        }
        Usuario recipient = usuarioRepository.findById(recipientUserId).orElse(null);
        String inboxKey;
        if (recipient != null && recipient.getTipoUsuario() == TipoUsuario.CIDADAO) {
            String cpf = recipient.getCpf();
            inboxKey = cpf != null && !cpf.isBlank() ? "CIDCPF:" + cpf.trim() : "USR:" + recipientUserId;
        } else {
            inboxKey = "USR:" + recipientUserId;
        }
        Processo processo = processoRepository.findById(thread.getProcessoId()).orElse(null);
        String numero = processo != null ? processo.getNumeroUnificado() : null;
        String mensagem = "NOTIFICADO: nova mensagem no chat" + (numero == null ? "" : " • Processo " + numero);
        try {
            uiHistoryService.recordInboxEvent(
                    inboxKey,
                    thread.getProcessoId(),
                    UiHistoryService.EVT_ATENDIMENTO_NEW_MESSAGE,
                    resolveInboxTokens(thread.getId(), recipientUserId, false),
                    sender.getId(),
                    sender.getTipoUsuario() == null ? null : sender.getTipoUsuario().name(),
                    mensagem
            );
        } catch (Exception ignored) {
        }
    }

    void notifyReadState(AtendimentoThread thread, Usuario actor, Long lastReadMessageId, Instant at) {
        Long otherUserId = AtendimentoChatSupportUtils.otherUserId(actor, thread);
        if (otherUserId == null) {
            return;
        }
        enqueuePayload(AtendimentoChatSupportUtils.topicForUser(otherUserId), PayloadMaps.ofEntries(
                "type", "ATENDIMENTO_READ",
                "threadId", thread.getId(),
                "userId", actor.getId(),
                "lastReadMessageId", lastReadMessageId,
                "at", at != null ? at.toString() : null
        ));
    }

    void notifyDeliveredState(AtendimentoThread thread, Usuario actor, Long lastDeliveredMessageId, Instant at) {
        Long otherUserId = AtendimentoChatSupportUtils.otherUserId(actor, thread);
        if (otherUserId == null) {
            return;
        }
        enqueuePayload(AtendimentoChatSupportUtils.topicForUser(otherUserId), PayloadMaps.ofEntries(
                "type", "ATENDIMENTO_DELIVERED",
                "threadId", thread.getId(),
                "userId", actor.getId(),
                "lastDeliveredMessageId", lastDeliveredMessageId,
                "at", at != null ? at.toString() : null
        ));
    }

    void notifyTyping(AtendimentoThread thread, Usuario actor, boolean typing) {
        Long otherUserId = AtendimentoChatSupportUtils.otherUserId(actor, thread);
        if (otherUserId == null) {
            return;
        }
        enqueuePayload(AtendimentoChatSupportUtils.topicForUser(otherUserId), PayloadMaps.ofEntries(
                "type", "ATENDIMENTO_TYPING",
                "threadId", thread.getId(),
                "userId", actor.getId(),
                "typing", typing,
                "at", Instant.now(clock).toString()
        ));
    }

    String inboxTopicForUser(Long userId) {
        return AtendimentoChatSupportUtils.topicForUser(userId);
    }

    private Map<Long, List<AtendimentoAttachment>> loadAttachmentsByMessageId(List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        List<AtendimentoMessageAttachment> links = messageAttachmentRepository.findByMessageIds(messageIds);
        if (links == null || links.isEmpty()) {
            return Map.of();
        }
        List<Long> attachmentIds = links.stream()
                .map(AtendimentoMessageAttachment::getId)
                .filter(Objects::nonNull)
                .map(id -> id.getAttachmentId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, AtendimentoAttachment> attachmentsById = attachmentIds.isEmpty()
                ? Map.of()
                : attachmentRepository.findAllById(attachmentIds).stream().collect(Collectors.toMap(AtendimentoAttachment::getId, Function.identity()));
        Map<Long, List<AtendimentoAttachment>> output = new HashMap<>();
        for (AtendimentoMessageAttachment link : links) {
            if (link == null || link.getId() == null || link.getId().getMessageId() == null || link.getId().getAttachmentId() == null) {
                continue;
            }
            AtendimentoAttachment attachment = attachmentsById.get(link.getId().getAttachmentId());
            if (attachment == null) {
                continue;
            }
            output.computeIfAbsent(link.getId().getMessageId(), ignored -> new ArrayList<>()).add(attachment);
        }
        return output;
    }

    private EnumSet<UiToken> resolveInboxTokens(Long threadId, Long recipientUserId, boolean forceNotify) {
        if (threadId == null || recipientUserId == null) {
            return EnumSet.of(UiToken.INFO);
        }
        AtendimentoThreadMemberSettings settings = null;
        try {
            settings = settingsRepository.findByThreadIdAndUsuarioId(threadId, recipientUserId).orElse(null);
        } catch (Exception ignored) {
        }
        boolean muted = !forceNotify && AtendimentoChatSupportUtils.isMutedNow(settings, Instant.now(clock));
        return muted ? EnumSet.of(UiToken.INFO) : EnumSet.of(UiToken.NOTIFICADO);
    }

    private void enqueuePayload(String topic, Object payload) {
        if (topic == null || payload == null) {
            return;
        }
        try {
            liveHub.enqueue(topic, mapper.writeValueAsString(payload));
        } catch (Exception ignored) {
        }
    }
}
