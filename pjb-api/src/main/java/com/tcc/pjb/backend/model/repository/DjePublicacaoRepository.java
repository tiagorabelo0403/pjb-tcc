package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.judicial.DjePublicacao;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DjePublicacaoRepository extends JpaRepository<DjePublicacao, Long> {

    List<DjePublicacao> findTop100ByTribunalCodigoIgnoreCaseOrderByCreatedAtDesc(String tribunalCodigo);

    List<DjePublicacao> findTop100ByEdicaoDjeOrderByCreatedAtDesc(String edicaoDje);

    boolean existsByProcessoIdAndTipoAtoAndConteudoHash(Long processoId, String tipoAto, String conteudoHash);

    long countByStatus(String status);

    long countByTribunalCodigoIgnoreCase(String tribunalCodigo);

    long countByTribunalCodigoIgnoreCaseAndStatusIgnoreCase(String tribunalCodigo, String status);

    long countByEdicaoDje(String edicaoDje);

    long countByEdicaoDjeAndStatusIgnoreCase(String edicaoDje, String status);

    long countByEdicaoDjeAndPartesNotificadasFalse(String edicaoDje);

    List<DjePublicacao> findTop100ByProcessoIdOrderByCreatedAtDesc(Long processoId);

    List<DjePublicacao> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    List<DjePublicacao> findByStatusAndDataPublicacaoLessThanEqualOrderByDataPublicacaoAscIdAsc(String status,
                                                                                                 LocalDate dataPublicacao,
                                                                                                 Pageable pageable);

    List<DjePublicacao> findByStatusAndPrazoComecaEmLessThanEqualAndPartesNotificadasFalseOrderByPrazoComecaEmAscIdAsc(String status,
                                                                                                                         LocalDate prazoComecaEm,
                                                                                                                         Pageable pageable);
}
