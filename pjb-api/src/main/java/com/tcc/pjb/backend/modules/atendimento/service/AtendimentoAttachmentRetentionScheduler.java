package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.modules.atendimento.config.AtendimentoRetentionProperties;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachmentStatus;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoAttachmentRepository;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtendimentoAttachmentRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AtendimentoAttachmentRetentionScheduler.class);

    private final AtendimentoAttachmentRepository repo;
    private final ObjectStoragePort storage;
    private final AtendimentoRetentionProperties props;

    public AtendimentoAttachmentRetentionScheduler(AtendimentoAttachmentRepository repo, ObjectStoragePort storage, AtendimentoRetentionProperties props) {
        this.repo = Objects.requireNonNull(repo);
        this.storage = Objects.requireNonNull(storage);
        this.props = Objects.requireNonNull(props);
    }

    @Scheduled(cron = "0 12 3 * * *")
    @Transactional
    public void run() {
        int days = Math.max(props.getAttachmentDays(), 1);
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        List<AtendimentoAttachment> candidates = repo.findForRetentionCleanup(cutoff);
        for (AtendimentoAttachment a : candidates) {
            if (a.getStatus() == AtendimentoAttachmentStatus.EXPIRED) {
                continue;
            }
            try {
                storage.delete(a.getStorageKey());
                a.setStatus(AtendimentoAttachmentStatus.EXPIRED);
                repo.save(a);
            } catch (IOException e) {
                log.warn("Falha ao expirar anexo de atendimento id={} storageKey={}", a.getId(), a.getStorageKey(), e);
            }
        }
    }
}
