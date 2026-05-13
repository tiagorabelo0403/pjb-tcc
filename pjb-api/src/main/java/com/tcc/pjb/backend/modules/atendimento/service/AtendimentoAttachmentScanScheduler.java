package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.core.storage.ObjectReadResult;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachmentStatus;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoAttachmentRepository;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtendimentoAttachmentScanScheduler {

    private final AtendimentoAttachmentRepository repo;
    private final ObjectStoragePort storage;

    public AtendimentoAttachmentScanScheduler(AtendimentoAttachmentRepository repo, ObjectStoragePort storage) {
        this.repo = Objects.requireNonNull(repo);
        this.storage = Objects.requireNonNull(storage);
    }

    @Scheduled(fixedDelayString = "PT30S")
    @Transactional
    public void scanPending() {
        Instant min = Instant.now().minus(Duration.ofDays(7));
        List<AtendimentoAttachment> list = repo.findPendingSince(AtendimentoAttachmentStatus.PENDING_SCAN, min);
        for (AtendimentoAttachment a : list) {
            try {
                ObjectReadResult r = storage.get(a.getStorageKey());
                ScanResult sr = scanPdf(r);
                if (!sr.isPdf) {
                    a.setStatus(AtendimentoAttachmentStatus.REJECTED);
                    a.setRejectionReason("NOT_PDF");
                } else {
                    a.setSha256(sr.sha256);
                    a.setStatus(AtendimentoAttachmentStatus.READY);
                    a.setRejectionReason(null);
                }
            } catch (Exception e) {
                a.setStatus(AtendimentoAttachmentStatus.REJECTED);
                a.setRejectionReason("SCAN_ERROR");
            }
            repo.save(a);
        }
    }

    private static ScanResult scanPdf(ObjectReadResult r) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        int n;
        byte[] head = new byte[4];
        int headPos = 0;
        try (InputStream in = r.resource().getInputStream()) {
            while ((n = in.read(buf)) > 0) {
                if (headPos < 4) {
                    int copy = Math.min(4 - headPos, n);
                    System.arraycopy(buf, 0, head, headPos, copy);
                    headPos += copy;
                }
                md.update(buf, 0, n);
            }
        }
        boolean isPdf = headPos == 4 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
        return new ScanResult(isPdf, toHex(md.digest()));
    }

    private static String toHex(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private record ScanResult(boolean isPdf, String sha256) {
    }
}
