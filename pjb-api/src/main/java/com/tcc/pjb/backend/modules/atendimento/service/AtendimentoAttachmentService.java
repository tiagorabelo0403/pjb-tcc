package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.storage.ObjectReadResult;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoAttachmentDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoAttachmentDownloadDto;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachmentStatus;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoAttachmentRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AtendimentoAttachmentService {

    private final AtendimentoTosService tos;
    private final CurrentUserService currentUser;
    private final AtendimentoChatService chat;
    private final AtendimentoAttachmentRepository repo;
    private final ObjectStoragePort storage;
    private final boolean attachmentsEnabled;
    private final long maxBytes;

    public AtendimentoAttachmentService(AtendimentoTosService tos,
                                       CurrentUserService currentUser,
                                       AtendimentoChatService chat,
                                       AtendimentoAttachmentRepository repo,
                                       ObjectStoragePort storage,
                                       @Value("${pjb.atendimento.attachments.enabled:false}") boolean attachmentsEnabled,
                                       @Value("${pjb.atendimento.attachments.maxBytes:10485760}") long maxBytes) {
        this.tos = Objects.requireNonNull(tos);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.chat = Objects.requireNonNull(chat);
        this.repo = Objects.requireNonNull(repo);
        this.storage = Objects.requireNonNull(storage);
        this.attachmentsEnabled = attachmentsEnabled;
        this.maxBytes = Math.max(0L, maxBytes);
    }

    @Transactional
    public AtendimentoAttachmentDto upload(Long threadId, MultipartFile file) {
        tos.requireAccepted();
        if (!attachmentsEnabled) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "attachments_disabled");
        }
        if (threadId == null) throw new IllegalArgumentException("threadId");
        if (file == null) throw new IllegalArgumentException("file");
        if (file.getSize() <= 0 || (maxBytes > 0 && file.getSize() > maxBytes)) throw new IllegalArgumentException("file_size");
        String contentType = normalizeContentType(file.getContentType());
        if (!MediaType.APPLICATION_PDF_VALUE.equals(contentType)) throw new IllegalArgumentException("only_pdf");
        chat.requireThreadAccess(threadId);

        String key = "atendimento/" + threadId + "/" + UUID.randomUUID() + ".pdf";
        AtendimentoAttachment a = new AtendimentoAttachment();
        a.setThreadId(threadId);
        a.setUploaderUserId(currentUser.getRequired().getId());
        a.setStorageKey(key);
        a.setFileName(safeName(file.getOriginalFilename()));
        a.setContentType(contentType);
        a.setSizeBytes(file.getSize());
        a.setStatus(AtendimentoAttachmentStatus.PENDING_SCAN);
        a.setCreatedAt(Instant.now());
        repo.save(a);

        try (InputStream in = file.getInputStream()) {
            storage.put(key, in, file.getSize(), contentType, Map.of("threadId", String.valueOf(threadId), "uploader", String.valueOf(a.getUploaderUserId())));
        } catch (IOException e) {
            repo.deleteById(a.getId());
            throw new IllegalStateException(e);
        }

        return toDto(a);
    }

    @Transactional(readOnly = true)
    public AtendimentoAttachmentDownloadDto downloadUrl(Long threadId, Long attachmentId) {
        tos.requireAccepted();
        chat.requireThreadAccess(threadId);
        AtendimentoAttachment a = repo.findByIdAndThreadId(attachmentId, threadId).orElseThrow();
        if (a.getStatus() != AtendimentoAttachmentStatus.READY) throw new IllegalArgumentException("attachment_not_ready");
        Duration exp = Duration.ofMinutes(5);
        URI uri = storage.presignGet(a.getStorageKey(), exp);
        return new AtendimentoAttachmentDownloadDto(a.getId(), uri.toString(), Instant.now().plus(exp));
    }

    @Transactional(readOnly = true)
    public AtendimentoAttachmentDto meta(Long threadId, Long attachmentId) {
        tos.requireAccepted();
        chat.requireThreadAccess(threadId);
        AtendimentoAttachment a = repo.findByIdAndThreadId(attachmentId, threadId).orElseThrow();
        return toDto(a);
    }

    @Transactional(readOnly = true)
    public ObjectReadResult downloadSecure(Long threadId, Long attachmentId) {
        tos.requireAccepted();
        chat.requireThreadAccess(threadId);
        AtendimentoAttachment a = repo.findByIdAndThreadId(attachmentId, threadId).orElseThrow();
        if (a.getStatus() != AtendimentoAttachmentStatus.READY) throw new IllegalArgumentException("attachment_not_ready");
        ObjectReadResult r;
        try {
            r = storage.get(a.getStorageKey());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if (r == null || r.resource() == null) throw new IllegalStateException("attachment_missing");
        return r;
    }

    static AtendimentoAttachmentDto toDto(AtendimentoAttachment a) {
        return new AtendimentoAttachmentDto(a.getId(), a.getFileName(), a.getContentType(), a.getSizeBytes(), a.getStatus() != null ? a.getStatus().name() : null);
    }

    static String normalizeContentType(String ct) {
        if (ct == null) return "";
        String c = ct.trim().toLowerCase();
        if (c.startsWith("application/pdf")) return MediaType.APPLICATION_PDF_VALUE;
        return c;
    }

    static String safeName(String name) {
        if (name == null || name.isBlank()) return "arquivo.pdf";
        String n = name.replaceAll("[\\r\\n\\t]", " ").trim();
        if (n.length() > 180) n = n.substring(0, 180);
        return n;
    }

}
