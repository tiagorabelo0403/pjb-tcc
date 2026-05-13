package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.pericia.CadeiaCustodiaDigitalLedgerEntry;

@Repository
public interface CadeiaCustodiaDigitalLedgerEntryRepository extends JpaRepository<CadeiaCustodiaDigitalLedgerEntry, Long> {

    List<CadeiaCustodiaDigitalLedgerEntry> findAllByDigestColecaoSha256OrderByOrdemLoteAsc(String digestColecaoSha256);

    Optional<CadeiaCustodiaDigitalLedgerEntry> findTopByOrderByIdDesc();

    Optional<CadeiaCustodiaDigitalLedgerEntry> findTopByDigestColecaoSha256OrderBySealedAtAsc(String digestColecaoSha256);

    List<CadeiaCustodiaDigitalLedgerEntry> findTop200ByChaveCustodiaOrderBySealedAtDesc(String chaveCustodia);
}
