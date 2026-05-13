package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.federalismo.FederacaoLedgerEntry;

public interface FederacaoLedgerEntryRepository extends JpaRepository<FederacaoLedgerEntry, Long> {

    Optional<FederacaoLedgerEntry> findTopByOrderBySequenciaGlobalDesc();

    Optional<FederacaoLedgerEntry> findTopByTribunalCodigoOrderBySequenciaTribunalDesc(String tribunalCodigo);

    Optional<FederacaoLedgerEntry> findByIdempotencyKey(String idempotencyKey);

    List<FederacaoLedgerEntry> findAllByNupnOrderBySequenciaGlobalAsc(String nupn);

    List<FederacaoLedgerEntry> findTop200ByOrderBySequenciaGlobalAsc();
}
