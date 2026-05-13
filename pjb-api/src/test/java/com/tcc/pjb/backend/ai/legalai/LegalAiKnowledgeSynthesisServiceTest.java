package com.tcc.pjb.backend.ai.legalai;

import com.tcc.pjb.backend.ai.legalai.audit.application.LegalAiAuditService;
import com.tcc.pjb.backend.ai.legalai.memory.application.LegalAiKnowledgeSynthesisService;
import com.tcc.pjb.backend.ai.legalai.memory.application.LegalAiKnowledgeSynthesisService.SynthesisReport;
import com.tcc.pjb.backend.ai.legalai.memory.domain.LegalAiJudicialRiteCatalog;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryEntry;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryEntryRepository;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemorySigiloNivel;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegalAiKnowledgeSynthesisServiceTest {

    private static final Instant AGORA = Instant.parse("2026-05-13T02:00:00Z");

    private final MemoryStoreRepository storeRepository = mock(MemoryStoreRepository.class);
    private final MemoryEntryRepository entryRepository = mock(MemoryEntryRepository.class);
    private final LegalAiAuditService auditService = mock(LegalAiAuditService.class);
    private final Clock clock = Clock.fixed(AGORA, java.time.ZoneOffset.UTC);

    private LegalAiKnowledgeSynthesisService synthesisService;

    @BeforeEach
    void setUp() {
        synthesisService = new LegalAiKnowledgeSynthesisService(
                storeRepository, entryRepository, auditService, clock);
        doNothing().when(auditService).registrar(any());
    }

    @Test
    void sintetizarComStoresVazioDeveRetornarRelatorioZerado() {
        when(storeRepository.listarAtivos()).thenReturn(List.of());

        SynthesisReport report = synthesisService.sintetizarConhecimento();

        assertThat(report.totalStores()).isZero();
        assertThat(report.totalEntradas()).isZero();
        assertThat(report.porRito()).isEmpty();
    }

    @Test
    void sintetizarDeveExcluirStoresSigilosos() {
        MemoryStoreId storeId = MemoryStoreId.gerar();
        MemoryStore storeSigiloso = MemoryStore.criar(storeId, "s", "d", "m", MemorySigiloNivel.SIGILOSO, AGORA);
        when(storeRepository.listarAtivos()).thenReturn(List.of(storeSigiloso));

        SynthesisReport report = synthesisService.sintetizarConhecimento();

        assertThat(report.totalStores()).isZero();
        assertThat(report.totalEntradas()).isZero();
    }

    @Test
    void sintetizarDeveExcluirStoresCriticos() {
        MemoryStoreId storeId = MemoryStoreId.gerar();
        MemoryStore storeCritico = MemoryStore.criar(storeId, "s", "d", "m", MemorySigiloNivel.CRITICO, AGORA);
        when(storeRepository.listarAtivos()).thenReturn(List.of(storeCritico));

        SynthesisReport report = synthesisService.sintetizarConhecimento();

        assertThat(report.totalStores()).isZero();
    }

    @Test
    void sintetizarDeveContabilizarEntradasPorRito() {
        MemoryStoreId storeId = MemoryStoreId.gerar();
        MemoryStore storePublico = MemoryStore.criar(storeId, "pub", "d", "m", MemorySigiloNivel.PUBLIC, AGORA);
        when(storeRepository.listarAtivos()).thenReturn(List.of(storePublico));

        List<MemoryEntry> entradas = List.of(
                MemoryEntry.criar(storeId, "CRIM:padrao-denúncia", "conteúdo criminal", AGORA),
                MemoryEntry.criar(storeId, "CRIM:padrão-réu-ausente", "réu não encontrado", AGORA),
                MemoryEntry.criar(storeId, "TRB:audiência-conciliação", "tentativa de acordo", AGORA)
        );
        when(entryRepository.listarPorStore(storeId)).thenReturn(entradas);

        SynthesisReport report = synthesisService.sintetizarConhecimento();

        assertThat(report.totalEntradas()).isEqualTo(3);
        assertThat(report.porRito()).hasSize(2);

        var rito = report.porRito().stream()
                .filter(r -> r.codigo().equals("CRIM"))
                .findFirst();
        assertThat(rito).isPresent();
        assertThat(rito.get().totalEntradas()).isEqualTo(2);
    }

    @Test
    void coberturaPorcentagemDeveReflectirRitosComDados() {
        MemoryStoreId storeId = MemoryStoreId.gerar();
        MemoryStore store = MemoryStore.criar(storeId, "pub", "d", "m", MemorySigiloNivel.INSTITUCIONAL, AGORA);
        when(storeRepository.listarAtivos()).thenReturn(List.of(store));

        int totalRitos = LegalAiJudicialRiteCatalog.ritos().size();
        String primeiroCodigo = LegalAiJudicialRiteCatalog.ritos().get(0).codigo();
        List<MemoryEntry> entradas = List.of(
                MemoryEntry.criar(storeId, primeiroCodigo + ":entrada-1", "conteudo", AGORA)
        );
        when(entryRepository.listarPorStore(storeId)).thenReturn(entradas);

        SynthesisReport report = synthesisService.sintetizarConhecimento();

        assertThat(report.coberturaPorcentagem()).isGreaterThan(0);
        assertThat(report.coberturaPorcentagem()).isLessThanOrEqualTo(100);
        assertThat(report.totalRitosCatalogados()).isEqualTo(totalRitos);
    }

    @Test
    void catalogoDeveConterTodosOsRitosBrasileirosPrincipais() {
        List<LegalAiJudicialRiteCatalog> ritos = LegalAiJudicialRiteCatalog.ritos();
        assertThat(ritos).hasSizeGreaterThanOrEqualTo(20);

        List<String> codigos = ritos.stream().map(LegalAiJudicialRiteCatalog::codigo).toList();
        assertThat(codigos).contains("CIV", "CRIM", "TRB", "ELET", "JEC", "JEF", "FAL", "FAM", "EXEC");
    }

    @Test
    void listarRitosSemCoberturaDeveRetornarTodosQuandoStoreVazio() {
        MemoryStoreId storeId = MemoryStoreId.gerar();
        when(entryRepository.listarPorStore(storeId)).thenReturn(List.of());

        List<LegalAiJudicialRiteCatalog> semCobertura = synthesisService.listarRitosSemCobertura(storeId);

        assertThat(semCobertura).hasSize(LegalAiJudicialRiteCatalog.ritos().size());
    }

    @Test
    void listarRitosComCoberturaDeveReconhecerPrefixoNaChave() {
        MemoryStoreId storeId = MemoryStoreId.gerar();
        List<MemoryEntry> entradas = List.of(
                MemoryEntry.criar(storeId, "TRB:audiência-modelo", "conteudo trabalhista", AGORA)
        );
        when(entryRepository.listarPorStore(storeId)).thenReturn(entradas);

        List<LegalAiJudicialRiteCatalog> comCobertura = synthesisService.listarRitosComCobertura(storeId);

        assertThat(comCobertura).hasSize(1);
        assertThat(comCobertura.get(0).codigo()).isEqualTo("TRB");
    }

    @Test
    void sintetizarGeradoEmDeveCorresponderAoInstanteDoClock() {
        when(storeRepository.listarAtivos()).thenReturn(List.of());

        SynthesisReport report = synthesisService.sintetizarConhecimento();

        assertThat(report.geradoEm()).isEqualTo(AGORA);
    }
}
