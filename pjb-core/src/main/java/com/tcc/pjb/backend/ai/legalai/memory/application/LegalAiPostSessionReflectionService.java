package com.tcc.pjb.backend.ai.legalai.memory.application;

import com.tcc.pjb.backend.ai.legalai.audit.application.LegalAiAuditService;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditAction;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditLog;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidate;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReview;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReviewRepository;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateType;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryEntry;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryEntryRepository;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemorySigiloNivel;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreRepository;
import com.tcc.pjb.backend.ai.legalai.memory.domain.events.LegalAiMemoryCandidateDetectedEvent;
import com.tcc.pjb.backend.ai.legalai.memory.domain.events.LegalAiSessionClosedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class LegalAiPostSessionReflectionService {

    private final MemoryEntryRepository memoryEntryRepository;
    private final MemoryStoreRepository memoryStoreRepository;
    private final MemoryCandidateReviewRepository reviewRepository;
    private final MemoryContradictionDetector contradictionDetector;
    private final LegalAiAuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public LegalAiPostSessionReflectionService(
            MemoryEntryRepository memoryEntryRepository,
            MemoryStoreRepository memoryStoreRepository,
            MemoryCandidateReviewRepository reviewRepository,
            MemoryContradictionDetector contradictionDetector,
            LegalAiAuditService auditService,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.memoryEntryRepository = memoryEntryRepository;
        this.memoryStoreRepository = memoryStoreRepository;
        this.reviewRepository = reviewRepository;
        this.contradictionDetector = contradictionDetector;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public List<MemoryCandidate> processarSessaoEncerrada(LegalAiSessionClosedEvent evento) {
        MemorySigiloNivel nivel = MemorySigiloNivel.valueOf(evento.sigiloNivel());
        if (MemoryAccessPolicy.requerRevisaoHumana(nivel)) {
            return List.of();
        }

        Instant agora = Instant.now(clock);
        List<MemoryCandidate> candidatos = extrairCandidatos(evento, nivel, agora);

        for (MemoryCandidate candidato : candidatos) {
            String chave = candidato.tipo().name() + ":" + candidato.sessionId();

            if (candidato.podePromoverAutomaticamente()) {
                promoverAutomaticamente(candidato, chave, evento.moduloOrigem(), nivel, evento.userId(), agora);
            } else {
                encaminharParaRevisao(candidato, chave, evento.moduloOrigem(), nivel, evento.userId(), agora);
            }

            eventPublisher.publishEvent(new LegalAiMemoryCandidateDetectedEvent(
                    candidato.id(), candidato.sessionId(), candidato.tipo(),
                    candidato.podePromoverAutomaticamente(), agora));
        }

        return candidatos;
    }

    private void promoverAutomaticamente(
            MemoryCandidate candidato, String chave, String moduloOrigem,
            MemorySigiloNivel nivel, String userId, Instant agora) {
        MemoryStoreId storeId = resolverStoreId(moduloOrigem, nivel, agora);
        boolean temContradicao = contradictionDetector.detectarEPublicar(storeId, chave, candidato.id());

        if (temContradicao) {
            encaminharParaRevisao(candidato, chave, moduloOrigem, nivel, userId, agora);
            auditService.registrar(LegalAiAuditLog
                    .iniciar(LegalAiAuditAction.CONTRADICAO_DETECTADA, "MemoryEntry", chave, userId, agora)
                    .comDetalhes("{\"store\":\"" + storeId.value() + "\"}"));
            return;
        }

        MemoryEntry entry = MemoryEntry.criar(storeId, chave, candidato.conteudo(), agora);
        memoryEntryRepository.salvar(entry);
        auditService.registrar(LegalAiAuditLog
                .iniciar(LegalAiAuditAction.MEMORIA_PROMOVIDA, "MemoryEntry",
                        entry.id().toString(), userId, agora));
    }

    private void encaminharParaRevisao(
            MemoryCandidate candidato, String chave, String moduloOrigem,
            MemorySigiloNivel nivel, String userId, Instant agora) {
        MemoryCandidateReview review = MemoryCandidateReview.criar(
                candidato.id(), candidato.sessionId(), moduloOrigem,
                candidato.tipo(), chave, candidato.conteudo(),
                candidato.motivoExtracao(), nivel, agora);
        reviewRepository.salvar(review);
        auditService.registrar(LegalAiAuditLog
                .iniciar(LegalAiAuditAction.CANDIDATO_DETECTADO, "MemoryCandidateReview",
                        review.id().value().toString(), userId, agora));
    }

    private MemoryStoreId resolverStoreId(String moduloOrigem, MemorySigiloNivel nivel, Instant agora) {
        return memoryStoreRepository.listarPorModulo(moduloOrigem).stream()
                .filter(s -> !s.isArquivado() && s.sigiloNivel() == nivel)
                .findFirst()
                .map(MemoryStore::id)
                .orElseGet(() -> {
                    MemoryStore novo = MemoryStore.criar(
                            MemoryStoreId.gerar(),
                            moduloOrigem + "-reflexao",
                            "Store de reflexão para " + moduloOrigem,
                            moduloOrigem, nivel, agora);
                    return memoryStoreRepository.salvar(novo).id();
                });
    }

    private List<MemoryCandidate> extrairCandidatos(
            LegalAiSessionClosedEvent evento,
            MemorySigiloNivel nivel,
            Instant agora) {
        List<MemoryCandidate> candidatos = new ArrayList<>();

        if (evento.totalMessages() >= 3) {
            candidatos.add(MemoryCandidate.extrair(
                    evento.sessionId(),
                    MemoryCandidateType.PREFERENCIA_USUARIO,
                    "Sessão de " + evento.moduloOrigem() + " com " + evento.totalMessages() + " mensagens",
                    "Volume de interação indica padrão de uso",
                    nivel, agora));
        }

        if (evento.processoId() != null && !evento.processoId().isBlank()) {
            candidatos.add(MemoryCandidate.extrair(
                    evento.sessionId(),
                    MemoryCandidateType.PROCESSUAL,
                    "Sessão vinculada ao processo " + evento.processoId(),
                    "Contexto processual documentado",
                    nivel, agora));
        }

        return candidatos;
    }
}
