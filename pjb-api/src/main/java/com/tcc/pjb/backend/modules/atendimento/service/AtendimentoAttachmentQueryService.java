package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachment;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageAttachmentRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AtendimentoAttachmentQueryService {

    private final AtendimentoMessageAttachmentRepository msgAttRepo;
    private final AtendimentoAttachmentRepository attachmentRepo;

    public AtendimentoAttachmentQueryService(AtendimentoMessageAttachmentRepository msgAttRepo,
                                             AtendimentoAttachmentRepository attachmentRepo) {
        this.msgAttRepo = Objects.requireNonNull(msgAttRepo);
        this.attachmentRepo = Objects.requireNonNull(attachmentRepo);
    }

    public record AttachmentBatch(Map<Long, List<Long>> attIdsByMsg, Map<Long, AtendimentoAttachment> attMap) {
    }

    public AttachmentBatch batchAttachments(List<Long> msgIds) {
        Map<Long, List<Long>> attIdsByMsg = new HashMap<>();
        for (AtendimentoMessageAttachment ma : msgAttRepo.findByMessageIds(msgIds)) {
            attIdsByMsg.computeIfAbsent(ma.getId().getMessageId(), k -> new ArrayList<>()).add(ma.getId().getAttachmentId());
        }
        Set<Long> allAttIds = attIdsByMsg.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, AtendimentoAttachment> attMap = allAttIds.isEmpty() ? Map.of() : attachmentRepo.findAllById(allAttIds).stream().collect(Collectors.toMap(AtendimentoAttachment::getId, x -> x));
        return new AttachmentBatch(attIdsByMsg, attMap);
    }

    public List<AtendimentoAttachment> attachmentsForMessage(Long messageId) {
        List<AtendimentoMessageAttachment> links = msgAttRepo.findByMessageIds(List.of(messageId));
        if (links.isEmpty()) return List.of();
        List<Long> ids = links.stream().map(x -> x.getId().getAttachmentId()).toList();
        return attachmentRepo.findAllById(ids);
    }
}
