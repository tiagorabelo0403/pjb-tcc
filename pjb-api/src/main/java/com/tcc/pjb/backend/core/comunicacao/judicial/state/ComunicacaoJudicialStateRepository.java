package com.tcc.pjb.backend.core.comunicacao.judicial.state;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComunicacaoJudicialStateRepository extends JpaRepository<ComunicacaoJudicialStateEntry, Long> {

    Optional<ComunicacaoJudicialStateEntry> findByDomainNameAndStateKey(String domainName, String stateKey);

    List<ComunicacaoJudicialStateEntry> findByDomainNameOrderByUpdatedAtDesc(String domainName);

    List<ComunicacaoJudicialStateEntry> findByDomainNameAndSecondaryKeyOrderByUpdatedAtDesc(String domainName, String secondaryKey);

    List<ComunicacaoJudicialStateEntry> findByDomainNameAndProcessoIdOrderByUpdatedAtDesc(String domainName, Long processoId);

    List<ComunicacaoJudicialStateEntry> findByDomainNameAndStatusCodeOrderByUpdatedAtDesc(String domainName, String statusCode);

    List<ComunicacaoJudicialStateEntry> findByDomainNameAndStatusCodeInOrderByUpdatedAtDesc(String domainName, Collection<String> statusCodes);

    boolean existsByDomainNameAndStateKey(String domainName, String stateKey);

    void deleteByDomainNameAndStateKey(String domainName, String stateKey);
}
