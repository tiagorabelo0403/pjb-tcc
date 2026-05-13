package com.tcc.pjb.backend.ai.legalai.dreaming.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.legalai.audit.application.LegalAiAuditService;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditAction;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditLog;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.Dream;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamId;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamInput;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamOutboxPort;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamPolicy;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamRepository;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamStatus;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreRepository;
import com.tcc.pjb.backend.ai.legalai.security.AnthropicInputSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DreamingOrchestrator {

    private final DreamRepository dreamRepository;
    private final DreamOutboxPort dreamOutboxPort;
    private final MemoryStoreRepository memoryStoreRepository;
    private final DreamPolicy dreamPolicy;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final AnthropicInputSanitizer inputSanitizer;
    private final LegalAiAuditService auditService;

    public DreamingOrchestrator(
            DreamRepository dreamRepository,
            DreamOutboxPort dreamOutboxPort,
            MemoryStoreRepository memoryStoreRepository,
            DreamPolicy dreamPolicy,
            Clock clock,
            ObjectMapper objectMapper,
            AnthropicInputSanitizer inputSanitizer,
            LegalAiAuditService auditService) {
        this.dreamRepository = dreamRepository;
        this.dreamOutboxPort = dreamOutboxPort;
        this.memoryStoreRepository = memoryStoreRepository;
        this.dreamPolicy = dreamPolicy;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.inputSanitizer = inputSanitizer;
        this.auditService = auditService;
    }

    @Transactional
    public DreamId iniciarDreaming(DreamInput input, String instrucoes, String modelo) {
        inputSanitizer.validar(instrucoes, "instrucoes");

        MemoryStore inputStore = memoryStoreRepository
                .buscarPorId(input.inputStoreId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Memory store não encontrado: " + input.inputStoreId().value()));

        if (!dreamPolicy.podeIniciarDreaming(inputStore)) {
            throw new DreamingNaoPermitidoException(
                    "Dreaming não permitido para store com sigilo " + inputStore.sigiloNivel());
        }

        DreamId dreamId = DreamId.gerar();
        Instant agora = Instant.now(clock);
        Dream dream = Dream.criar(dreamId, input, instrucoes, modelo, agora);
        dreamRepository.salvar(dream);
        registrarOutbox(dream, agora);

        auditService.registrar(LegalAiAuditLog
                .iniciar(LegalAiAuditAction.DREAM_CRIADO, "Dream",
                        dreamId.value().toString(), "SYSTEM", agora)
                .comModelo(modelo));

        return dreamId;
    }

    @Transactional(readOnly = true)
    public Optional<Dream> consultar(DreamId dreamId) {
        return dreamRepository.buscarPorId(dreamId);
    }

    @Transactional
    public Dream cancelar(DreamId dreamId) {
        Dream dream = dreamRepository.buscarPorId(dreamId)
                .orElseThrow(() -> new IllegalArgumentException("Dream não encontrado: " + dreamId.value()));

        if (dream.status().isTerminal()) {
            return dream;
        }

        Instant agora = Instant.now(clock);
        Dream cancelado = dream.cancelado(agora);
        Dream salvo = dreamRepository.salvar(cancelado);

        auditService.registrar(LegalAiAuditLog
                .iniciar(LegalAiAuditAction.DREAM_CANCELADO, "Dream",
                        dreamId.value().toString(), "SYSTEM", agora));

        return salvo;
    }

    @Transactional(readOnly = true)
    public List<Dream> listarPorStatus(DreamStatus status) {
        return dreamRepository.listarPorStatus(status);
    }

    private void registrarOutbox(Dream dream, Instant agora) {
        try {
            Map<String, Object> payload = buildOutboxPayload(dream);
            String payloadJson = objectMapper.writeValueAsString(payload);
            dreamOutboxPort.registrar(dream.id().value(), payloadJson, agora);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar payload do outbox para dream " + dream.id(), e);
        }
    }

    private Map<String, Object> buildOutboxPayload(Dream dream) {
        if (dream.input() instanceof DreamInput.SessionsInput si) {
            return Map.of(
                    "dreamId", dream.id().value().toString(),
                    "inputStoreId", si.inputStoreId().value().toString(),
                    "sessionIds", si.sessionIds(),
                    "instrucoes", dream.instrucoes() != null ? dream.instrucoes() : "",
                    "modelo", dream.modelo()
            );
        }
        MemoryStoreId storeId = ((DreamInput.MemoryStoreInput) dream.input()).storeId();
        return Map.of(
                "dreamId", dream.id().value().toString(),
                "inputStoreId", storeId.value().toString(),
                "instrucoes", dream.instrucoes() != null ? dream.instrucoes() : "",
                "modelo", dream.modelo()
        );
    }
}
