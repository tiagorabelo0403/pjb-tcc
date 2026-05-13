package com.tcc.pjb.backend.modules.atendimento.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachmentStatus;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessage;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageStatus;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtendimentoQuarantineReleaseScheduler {

    private final AtendimentoMessageRepository messageRepo;
    private final AtendimentoMessageAttachmentRepository msgAttRepo;
    private final AtendimentoAttachmentRepository attachmentRepo;
    private final AtendimentoThreadRepository threadRepo;
    private final AtendimentoInboxLiveHub liveHub;
    private final ObjectMapper mapper;

    public AtendimentoQuarantineReleaseScheduler(AtendimentoMessageRepository messageRepo,
                                                AtendimentoMessageAttachmentRepository msgAttRepo,
                                                AtendimentoAttachmentRepository attachmentRepo,
                                                AtendimentoThreadRepository threadRepo,
                                                AtendimentoInboxLiveHub liveHub,
                                                ObjectMapper mapper) {
        this.messageRepo = Objects.requireNonNull(messageRepo);
        this.msgAttRepo = Objects.requireNonNull(msgAttRepo);
        this.attachmentRepo = Objects.requireNonNull(attachmentRepo);
        this.threadRepo = Objects.requireNonNull(threadRepo);
        this.liveHub = Objects.requireNonNull(liveHub);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Scheduled(fixedDelayString = "${pjb.atendimento.quarantine.releaseMs:15000}")
    @Transactional
    public void run() {
        List<AtendimentoMessage> candidates = messageRepo.findTop200ByStatusOrderByIdAsc(AtendimentoMessageStatus.QUARANTINED);
        if (candidates.isEmpty()) return;

        List<Long> msgIds = candidates.stream().map(AtendimentoMessage::getId).filter(Objects::nonNull).toList();
        Map<Long, List<Long>> attIdsByMsg = new HashMap<>();
        for (AtendimentoMessageAttachment ma : msgAttRepo.findByMessageIds(msgIds)) {
            attIdsByMsg.computeIfAbsent(ma.getId().getMessageId(), k -> new ArrayList<>()).add(ma.getId().getAttachmentId());
        }

        Set<Long> allAttIds = attIdsByMsg.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, AtendimentoAttachment> attMap = allAttIds.isEmpty() ? Map.of() : attachmentRepo.findAllById(allAttIds).stream().collect(Collectors.toMap(AtendimentoAttachment::getId, x -> x));

        Instant now = Instant.now();
        for (AtendimentoMessage m : candidates) {
            List<Long> ids = attIdsByMsg.getOrDefault(m.getId(), List.of());
            boolean ok = true;
            for (Long id : ids) {
                AtendimentoAttachment a = attMap.get(id);
                if (a == null || a.getStatus() != AtendimentoAttachmentStatus.READY) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;

            m.setStatus(AtendimentoMessageStatus.DELIVERED);
            messageRepo.save(m);

            AtendimentoThread t = threadRepo.findById(m.getThreadId()).orElse(null);
            if (t != null) {
                t.setUpdatedAt(now);
                if (t.getLastMessageId() == null || t.getLastMessageId() < m.getId()) {
                    t.setLastMessageId(m.getId());
                }
                threadRepo.save(t);
                publish(t, m);
            }
        }
    }

    private void publish(AtendimentoThread t, AtendimentoMessage m) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ATENDIMENTO_NEW_MESSAGE");
        payload.put("threadId", t.getId());
        payload.put("processoId", t.getProcessoId());
        payload.put("messageId", m.getId());
        payload.put("at", m.getCreatedAt().toString());
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (Exception e) {
            return;
        }
        liveHub.enqueue("ATEND:USR:" + t.getAdvogadoId(), json);
        liveHub.enqueue("ATEND:USR:" + t.getCidadaoUsuarioId(), json);
    }
}
