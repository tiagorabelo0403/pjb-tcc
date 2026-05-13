package com.tcc.pjb.backend.core.kernel.process;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.process.events.ProcessEventAppendedEvent;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;
import com.tcc.pjb.backend.model.repository.ProcessEventRepository;

@Service
public class ProcessEventStore {

    private final ProcessEventRepository repository;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher publisher;

    public ProcessEventStore(ProcessEventRepository repository,
                             ObjectMapper objectMapper,
                             CurrentUserService currentUserService,
                             ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
        this.publisher = publisher;
    }

    @Transactional
    public ProcessEventEnvelope append(Long processoId, ProcessEventType type, Object payload) {
        Objects.requireNonNull(processoId, "processoId é obrigatório");
        Objects.requireNonNull(type, "type é obrigatório");
        Objects.requireNonNull(payload, "payload é obrigatório");

        String json = toJson(payload);
        String hash = sha256Hex(json);

        ProcessEventEnvelope existing = repository.findFirstByProcessoIdAndPayloadHash(processoId, hash).orElse(null);
        if (existing != null) return existing;

        Usuario actor = currentUserService.getOptional().orElse(null);
        Long actorId = actor != null ? actor.getId() : null;
        String actorRole = actor != null && actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : null;

        int attempts = 0;
        while (true) {
            attempts++;
            try {
                long nextSeq = repository.findMaxSeq(processoId).orElse(0L) + 1L;

                String prevChain = repository.findLastChainHash(processoId).orElse(null);
                if (prevChain == null) {
                    ProcessEventEnvelope last = repository.findLastEnvelope(processoId).orElse(null);
                    if (last != null && last.getPayloadHash() != null) {
                        prevChain = sha384Hex("v0|" + processoId + "|" + last.getSeq() + "|" + last.getPayloadHash());
                    }
                }
                String prevForHash = prevChain != null ? prevChain : "0".repeat(96);
                String chain = sha384Hex(prevForHash + "|" + hash + "|" + type.name() + "|" + nextSeq);

                ProcessEventEnvelope env = ProcessEventEnvelope.builder()
                        .processoId(processoId)
                        .seq(nextSeq)
                        .eventType(type.name())
                        .payload(json)
                        .payloadHash(hash)
                        .prevChainHash(prevChain)
                        .chainHash(chain)
                        .actorUserId(actorId)
                        .actorRole(actorRole)
                        .createdAt(Instant.now())
                        .build();

                ProcessEventEnvelope saved = repository.save(env);
                publisher.publishEvent(new ProcessEventAppendedEvent(
                        saved.getProcessoId(),
                        saved.getSeq(),
                        saved.getEventType(),
                        saved.getPayload(),
                        saved.getPayloadHash(),
                        saved.getPrevChainHash(),
                        saved.getChainHash(),
                        saved.getCreatedAt(),
                        saved.getActorUserId(),
                        saved.getActorRole()
                ));
                return saved;
            } catch (DataIntegrityViolationException race) {
                ProcessEventEnvelope nowExisting = repository.findFirstByProcessoIdAndPayloadHash(processoId, hash).orElse(null);
                if (nowExisting != null) return nowExisting;
                if (attempts >= 3) throw race;
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ProcessEventEnvelope> stream(Long processoId) {
        Objects.requireNonNull(processoId, "processoId é obrigatório");
        return repository.findAllByProcessoIdOrderBySeqAsc(processoId);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar payload do evento", e);
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static String sha384Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-384");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-384 indisponível", e);
        }
    }
}
