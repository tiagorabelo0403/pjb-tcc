package com.tcc.pjb.backend.integration.mni.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.integration.mni.application.MniRecepcaoService;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoRequest;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoResult;
import com.tcc.pjb.backend.model.entity.judicial.MniRecepcao;
import com.tcc.pjb.backend.model.repository.MniRecepcaoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MniMigrationBatchServiceTest {

    private MniMigrationBatchItemRepository itemRepository;
    private MniRecepcaoService recepcaoService;
    private MniRecepcaoRepository mniRecepcaoRepository;
    private MniMigrationBatchService service;

    @BeforeEach
    void setUp() {
        itemRepository = mock(MniMigrationBatchItemRepository.class);
        recepcaoService = mock(MniRecepcaoService.class);
        mniRecepcaoRepository = mock(MniRecepcaoRepository.class);
        service = new MniMigrationBatchService(itemRepository, recepcaoService, mniRecepcaoRepository);
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void enfileirarRejeitaXmlAusente() {
        assertThatThrownBy(() -> service.enfileirar("TJCE", "CARTA_PRECATORIA", " "))
                .isInstanceOf(IllegalArgumentException.class);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void enfileirarSalvaItemPendenteComXml() {
        ArgumentCaptor<MniMigrationBatchItem> captor = ArgumentCaptor.forClass(MniMigrationBatchItem.class);
        when(itemRepository.save(captor.capture())).thenAnswer(inv -> {
            MniMigrationBatchItem saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        Long id = service.enfileirar("TJCE", "CARTA_PRECATORIA", "<mni/>");

        assertThat(id).isEqualTo(10L);
        assertThat(captor.getValue().getStatus()).isEqualTo(MniMigrationItemStatus.PENDENTE);
        assertThat(captor.getValue().getXml()).isEqualTo("<mni/>");
    }

    @Test
    void processarUmItemMarcaProcessadoQuandoPayloadEhNovo() {
        MniMigrationBatchItem item = MniMigrationBatchItem.builder()
                .id(5L).tribunalOrigem("TJCE").motivo("CARTA_PRECATORIA").xml("<mni/>")
                .status(MniMigrationItemStatus.PENDENTE).build();
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(mniRecepcaoRepository.findByMniPayloadHash(any())).thenReturn(Optional.empty());
        when(recepcaoService.receberAutos(any(MniRecepcaoRequest.class)))
                .thenReturn(new MniRecepcaoResult("TJCE", "0001-1.2026.8.06.0001", "CARTA_PRECATORIA", "hash1", 99L, "PROCESSED"));

        MniMigrationBatchService.ItemOutcome outcome = service.processarUmItem(5L);

        assertThat(outcome.jaExistiaAntes()).isFalse();
        assertThat(outcome.processoIdLocal()).isEqualTo(99L);
        assertThat(item.getStatus()).isEqualTo(MniMigrationItemStatus.PROCESSADO);
        assertThat(item.getProcessoIdLocal()).isEqualTo(99L);
        assertThat(item.getProcessadoEm()).isNotNull();
    }

    @Test
    void processarUmItemDetectaDuplicataPorHashJaExistente() {
        MniMigrationBatchItem item = MniMigrationBatchItem.builder()
                .id(6L).tribunalOrigem("TJCE").motivo("CARTA_PRECATORIA").xml("<mni/>")
                .status(MniMigrationItemStatus.PENDENTE).build();
        when(itemRepository.findById(6L)).thenReturn(Optional.of(item));
        when(mniRecepcaoRepository.findByMniPayloadHash(any())).thenReturn(Optional.of(MniRecepcao.builder().id(1L).build()));
        when(recepcaoService.receberAutos(any(MniRecepcaoRequest.class)))
                .thenReturn(new MniRecepcaoResult("TJCE", "0001-1.2026.8.06.0001", "CARTA_PRECATORIA", "hash1", 99L, "PROCESSED"));

        MniMigrationBatchService.ItemOutcome outcome = service.processarUmItem(6L);

        assertThat(outcome.jaExistiaAntes()).isTrue();
    }

    @Test
    void processarUmItemLancaExcecaoQuandoItemNaoExiste() {
        when(itemRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processarUmItem(404L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void marcarFalhouRegistraErroEStatus() {
        MniMigrationBatchItem item = MniMigrationBatchItem.builder()
                .id(7L).status(MniMigrationItemStatus.PENDENTE).build();
        when(itemRepository.findById(7L)).thenReturn(Optional.of(item));

        service.marcarFalhou(7L, "xml malformado");

        assertThat(item.getStatus()).isEqualTo(MniMigrationItemStatus.FALHOU);
        assertThat(item.getErro()).isEqualTo("xml malformado");
        assertThat(item.getProcessadoEm()).isNotNull();
    }

    @Test
    void marcarFalhouNaoLancaQuandoItemNaoExisteMais() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        service.marcarFalhou(999L, "erro");

        verify(itemRepository, never()).save(any());
    }

    @Test
    void findPendingIdsDelegaComStatusPendenteEBatchSizeClampeado() {
        when(itemRepository.findPendingIds(eq(0L), any(), eq(MniMigrationItemStatus.PENDENTE), any()))
                .thenReturn(List.of(1L, 2L, 3L));

        List<Long> ids = service.findPendingIds(0L, null, 10000);

        assertThat(ids).containsExactly(1L, 2L, 3L);
    }

    @Test
    void countPendingDelegaParaRepositorioComStatusPendente() {
        when(itemRepository.countByStatusAfter(0L, null, MniMigrationItemStatus.PENDENTE)).thenReturn(42L);

        assertThat(service.countPending(0L, null)).isEqualTo(42L);
    }
}
