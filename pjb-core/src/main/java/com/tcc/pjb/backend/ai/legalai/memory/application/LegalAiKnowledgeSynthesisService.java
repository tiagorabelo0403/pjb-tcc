package com.tcc.pjb.backend.ai.legalai.memory.application;

import com.tcc.pjb.backend.ai.legalai.audit.application.LegalAiAuditService;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditAction;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditLog;
import com.tcc.pjb.backend.ai.legalai.memory.domain.LegalAiJudicialRiteCatalog;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryEntry;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryEntryRepository;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ciclo de revisão e síntese interna da IA jurídica.
 *
 * Varre os memory stores ativos, agrupa entradas por rito processual e produz
 * um relatório de síntese que alimenta o próximo ciclo de dreaming.
 * Processos sigilosos e críticos nunca participam desta síntese.
 */
@Service
public class LegalAiKnowledgeSynthesisService {

    private final MemoryStoreRepository memoryStoreRepository;
    private final MemoryEntryRepository memoryEntryRepository;
    private final LegalAiAuditService auditService;
    private final Clock clock;

    public LegalAiKnowledgeSynthesisService(
            MemoryStoreRepository memoryStoreRepository,
            MemoryEntryRepository memoryEntryRepository,
            LegalAiAuditService auditService,
            Clock clock) {
        this.memoryStoreRepository = memoryStoreRepository;
        this.memoryEntryRepository = memoryEntryRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SynthesisReport sintetizarConhecimento() {
        Instant agora = Instant.now(clock);
        List<MemoryStore> storesAtivos = memoryStoreRepository.listarAtivos().stream()
                .filter(s -> !s.isArquivado())
                .filter(s -> MemoryAccessPolicy.podeEnviarParaAnthropicApi(s.sigiloNivel()))
                .toList();

        Map<String, RiteSynthesis> porRito = new LinkedHashMap<>();
        int totalEntradas = 0;

        for (MemoryStore store : storesAtivos) {
            List<MemoryEntry> entradas = memoryEntryRepository.listarPorStore(store.id()).stream()
                    .filter(MemoryEntry::ativo)
                    .toList();

            for (MemoryEntry entry : entradas) {
                String rito = extrairRitoDaChave(entry.chave());
                porRito.computeIfAbsent(rito, k -> new RiteSynthesis(k, 0, List.of()))
                        .incrementar();
                totalEntradas++;
            }
        }

        registrarAuditoria(agora, storesAtivos.size(), totalEntradas);

        return new SynthesisReport(
                agora,
                storesAtivos.size(),
                totalEntradas,
                List.copyOf(porRito.values()),
                LegalAiJudicialRiteCatalog.ritos().size());
    }

    @Transactional(readOnly = true)
    public List<LegalAiJudicialRiteCatalog> listarRitosComCobertura(MemoryStoreId storeId) {
        List<MemoryEntry> entradas = memoryEntryRepository.listarPorStore(storeId).stream()
                .filter(MemoryEntry::ativo)
                .toList();

        return LegalAiJudicialRiteCatalog.ritos().stream()
                .filter(rito -> entradas.stream()
                        .anyMatch(e -> e.chave().startsWith(rito.codigo() + ":")))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LegalAiJudicialRiteCatalog> listarRitosSemCobertura(MemoryStoreId storeId) {
        List<LegalAiJudicialRiteCatalog> comCobertura = listarRitosComCobertura(storeId);
        return LegalAiJudicialRiteCatalog.ritos().stream()
                .filter(r -> !comCobertura.contains(r))
                .toList();
    }

    private String extrairRitoDaChave(String chave) {
        if (chave == null || !chave.contains(":")) {
            return "GERAL";
        }
        String prefixo = chave.substring(0, chave.indexOf(':'));
        try {
            return LegalAiJudicialRiteCatalog.porCodigo(prefixo).codigo();
        } catch (IllegalArgumentException ignored) {
            return prefixo;
        }
    }

    private void registrarAuditoria(Instant agora, int stores, int entradas) {
        auditService.registrar(LegalAiAuditLog
                .iniciar(LegalAiAuditAction.MEMORIA_PROMOVIDA, "KnowledgeSynthesis",
                        "synthesis-" + agora.toEpochMilli(), "SYSTEM", agora)
                .comDetalhes("{\"stores\":" + stores + ",\"entradas\":" + entradas + "}"));
    }

    public record SynthesisReport(
            Instant geradoEm,
            int totalStores,
            int totalEntradas,
            List<RiteSynthesis> porRito,
            int totalRitosCatalogados
    ) {
        public int coberturaPorcentagem() {
            if (totalRitosCatalogados == 0) return 0;
            long ritosComDados = porRito.stream().filter(r -> r.totalEntradas() > 0).count();
            return (int) (ritosComDados * 100L / totalRitosCatalogados);
        }
    }

    public static final class RiteSynthesis {
        private final String codigo;
        private int totalEntradas;
        private final List<String> amostras;

        public RiteSynthesis(String codigo, int totalEntradas, List<String> amostras) {
            this.codigo = codigo;
            this.totalEntradas = totalEntradas;
            this.amostras = amostras;
        }

        void incrementar() {
            this.totalEntradas++;
        }

        public String codigo() {
            return codigo;
        }

        public int totalEntradas() {
            return totalEntradas;
        }

        public List<String> amostras() {
            return amostras;
        }
    }
}
