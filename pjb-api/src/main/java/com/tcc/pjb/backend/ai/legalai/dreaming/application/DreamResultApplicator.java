package com.tcc.pjb.backend.ai.legalai.dreaming.application;

import com.tcc.pjb.backend.ai.legalai.audit.application.LegalAiAuditService;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditAction;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditLog;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.Dream;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamRepository;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamResult;
import com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicDreamingClient;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemorySigiloNivel;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class DreamResultApplicator {

    private final MemoryStoreRepository memoryStoreRepository;
    private final DreamRepository dreamRepository;
    private final LegalAiAuditService auditService;
    private final Clock clock;

    public DreamResultApplicator(
            MemoryStoreRepository memoryStoreRepository,
            DreamRepository dreamRepository,
            LegalAiAuditService auditService,
            Clock clock) {
        this.memoryStoreRepository = memoryStoreRepository;
        this.dreamRepository = dreamRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public Dream aplicarResultado(Dream dream, AnthropicDreamingClient.AnthropicDreamStatusRef statusRef) {
        Instant agora = Instant.now(clock);

        if (statusRef.isCompleted() && statusRef.outputMemoryStoreId() != null) {
            MemoryStoreId outputId = MemoryStoreId.gerar();
            MemoryStore outputStore = MemoryStore.criar(
                    outputId,
                    "dream-output-" + dream.id().value(),
                    "Output gerado pelo dream " + dream.anthropicDreamId(),
                    dream.input().inputStoreId().value().toString(),
                    MemorySigiloNivel.INSTITUCIONAL,
                    agora
            ).comAnthropicStoreId(statusRef.outputMemoryStoreId());

            memoryStoreRepository.salvar(outputStore);

            DreamResult result = new DreamResult(
                    outputId,
                    statusRef.outputMemoryStoreId(),
                    statusRef.inputTokens(),
                    statusRef.outputTokens());

            Dream concluido = dream.completado(result, agora);
            Dream salvo = dreamRepository.salvar(concluido);

            auditService.registrar(LegalAiAuditLog
                    .iniciar(LegalAiAuditAction.DREAM_CONCLUIDO, "Dream",
                            dream.id().value().toString(), "SYSTEM", agora)
                    .comTokens((int) statusRef.inputTokens(), (int) statusRef.outputTokens())
                    .comModelo(dream.modelo()));

            return salvo;
        }

        if ("failed".equalsIgnoreCase(statusRef.status())) {
            Dream falhou = dream.falhou(statusRef.erro(), agora);
            Dream salvo = dreamRepository.salvar(falhou);

            auditService.registrar(LegalAiAuditLog
                    .iniciar(LegalAiAuditAction.DREAM_FALHOU, "Dream",
                            dream.id().value().toString(), "SYSTEM", agora)
                    .comDetalhes("{\"erro\":\"" + sanitizarErro(statusRef.erro()) + "\"}"));

            return salvo;
        }

        Dream cancelado = dream.cancelado(agora);
        Dream salvo = dreamRepository.salvar(cancelado);

        auditService.registrar(LegalAiAuditLog
                .iniciar(LegalAiAuditAction.DREAM_CANCELADO, "Dream",
                        dream.id().value().toString(), "SYSTEM", agora));

        return salvo;
    }

    @Transactional
    public Dream marcarFalha(Dream dream, String motivo) {
        Instant agora = Instant.now(clock);
        Dream falhou = dream.falhou(motivo, agora);
        Dream salvo = dreamRepository.salvar(falhou);

        auditService.registrar(LegalAiAuditLog
                .iniciar(LegalAiAuditAction.DREAM_FALHOU, "Dream",
                        dream.id().value().toString(), "SYSTEM", agora)
                .comDetalhes("{\"motivo\":\"" + sanitizarErro(motivo) + "\"}"));

        return salvo;
    }

    private String sanitizarErro(String erro) {
        if (erro == null) return "";
        return erro.replace("\"", "'").replace("\n", " ");
    }
}
