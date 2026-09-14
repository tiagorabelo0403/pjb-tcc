package com.tcc.pjb.backend.integration.mni.migration;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.integration.mni.application.MniRecepcaoService;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoRequest;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoResult;
import com.tcc.pjb.backend.model.repository.MniRecepcaoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drena a fila de {@link MniMigrationBatchItem} em lotes, submetendo cada payload a
 * {@link MniRecepcaoService#receberAutos(MniRecepcaoRequest)}. Cada item roda em sua própria
 * transação ({@code REQUIRES_NEW}): um XML malformado de um caso não derruba os demais nem exige
 * reprocessar o lote inteiro — fica marcado FALHOU com o erro registrado, o cursor avança e o
 * próximo item segue normalmente.
 */
@Service
public class MniMigrationBatchService {

    private final MniMigrationBatchItemRepository itemRepository;
    private final MniRecepcaoService recepcaoService;
    private final MniRecepcaoRepository mniRecepcaoRepository;

    public MniMigrationBatchService(MniMigrationBatchItemRepository itemRepository,
                                    MniRecepcaoService recepcaoService,
                                    MniRecepcaoRepository mniRecepcaoRepository) {
        this.itemRepository = Objects.requireNonNull(itemRepository);
        this.recepcaoService = Objects.requireNonNull(recepcaoService);
        this.mniRecepcaoRepository = Objects.requireNonNull(mniRecepcaoRepository);
    }

    public record ItemOutcome(boolean jaExistiaAntes, Long processoIdLocal) {
    }

    @Transactional
    public Long enfileirar(String tribunalOrigem, String motivo, String xml) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("xml obrigatório para enfileirar item de migração MNI");
        }
        MniMigrationBatchItem item = MniMigrationBatchItem.builder()
                .tribunalOrigem(tribunalOrigem)
                .motivo(motivo)
                .xml(xml)
                .status(MniMigrationItemStatus.PENDENTE)
                .build();
        return itemRepository.save(item).getId();
    }

    @Transactional(readOnly = true)
    public List<Long> findPendingIds(long afterId, Long untilId, int batchSize) {
        int size = Math.max(1, Math.min(batchSize, 500));
        return itemRepository.findPendingIds(afterId, untilId, MniMigrationItemStatus.PENDENTE, PageRequest.of(0, size));
    }

    @Transactional(readOnly = true)
    public long countPending(long afterId, Long untilId) {
        return itemRepository.countByStatusAfter(afterId, untilId, MniMigrationItemStatus.PENDENTE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ItemOutcome processarUmItem(Long itemId) {
        MniMigrationBatchItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item de migração MNI não encontrado: " + itemId));
        String hash = Hashes.sha256Hex(item.getXml() == null ? "" : item.getXml());
        boolean jaExistiaAntes = mniRecepcaoRepository.findByMniPayloadHash(hash).isPresent();

        MniRecepcaoResult resultado = recepcaoService.receberAutos(
                new MniRecepcaoRequest(item.getTribunalOrigem(), item.getMotivo(), item.getXml()));

        item.setStatus(MniMigrationItemStatus.PROCESSADO);
        item.setProcessoIdLocal(resultado.processoIdLocal());
        item.setProcessadoEm(Instant.now());
        itemRepository.save(item);
        return new ItemOutcome(jaExistiaAntes, resultado.processoIdLocal());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFalhou(Long itemId, String erro) {
        itemRepository.findById(itemId).ifPresent(item -> {
            item.setStatus(MniMigrationItemStatus.FALHOU);
            item.setErro(erro);
            item.setProcessadoEm(Instant.now());
            itemRepository.save(item);
        });
    }

    @Transactional(readOnly = true)
    public List<MniMigrationBatchItem> listarFalhas() {
        return itemRepository.findTop200ByStatusOrderByIdDesc(MniMigrationItemStatus.FALHOU);
    }
}
