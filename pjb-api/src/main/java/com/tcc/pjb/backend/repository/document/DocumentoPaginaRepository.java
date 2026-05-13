package com.tcc.pjb.backend.repository.document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;

public interface DocumentoPaginaRepository extends JpaRepository<DocumentoPagina, UUID> {

    interface ProcessStats {
        long getTotalPages();
        long getPagesWithText();
    }

    interface DocumentStats {
        UUID getDocumentoId();
        long getTotalPages();
        long getPagesWithText();
    }

    Optional<DocumentoPagina> findByPageId(String pageId);

    @Query("select p from DocumentoPagina p where p.documento.id = :documentoId order by p.pageNumber asc")
    List<DocumentoPagina> findByDocumentoId(@Param("documentoId") UUID documentoId);

    @Query("select p from DocumentoPagina p where p.documento.id in :documentoIds order by p.documento.criadoEm desc, p.pageNumber asc")
    List<DocumentoPagina> findByDocumentoIds(@Param("documentoIds") List<UUID> documentoIds);

    @Query("select count(p) from DocumentoPagina p where p.documento.id = :documentoId")
    long countByDocumentoId(@Param("documentoId") UUID documentoId);

    @Query("select p from DocumentoPagina p where p.documento.processo.id = :processoId order by p.documento.criadoEm desc, p.pageNumber asc")
    List<DocumentoPagina> findByProcessoId(@Param("processoId") Long processoId);


    @Query(value = """
        select count(*) as totalPages,
               coalesce(sum(case when p.texto_extraido is not null and btrim(p.texto_extraido) <> '' then 1 else 0 end), 0) as pagesWithText
          from tb_documento_pagina p
          join tb_documento_processual d on d.id = p.documento_id
         where d.processo_id = :processoId
        """, nativeQuery = true)
    ProcessStats findProcessStatsByProcessoId(@Param("processoId") Long processoId);

    @Query(value = """
        select p.documento_id as documentoId,
               count(*) as totalPages,
               coalesce(sum(case when p.texto_extraido is not null and btrim(p.texto_extraido) <> '' then 1 else 0 end), 0) as pagesWithText
          from tb_documento_pagina p
         where p.documento_id in (:documentoIds)
         group by p.documento_id
        """, nativeQuery = true)
    List<DocumentStats> findDocumentStatsByDocumentoIds(@Param("documentoIds") List<UUID> documentoIds);

    @Query(value = """
        select p.id, p.documento_id, p.page_number, p.page_id, p.fingerprint, p.texto_extraido, p.criado_em
          from tb_documento_pagina p
          join tb_documento_processual d on d.id = p.documento_id
         where d.processo_id = :processoId
         order by d.criado_em desc, p.page_number asc
         limit :limit
        """, nativeQuery = true)
    List<DocumentoPagina> findTopNavigationPagesByProcessoId(@Param("processoId") Long processoId,
                                                             @Param("limit") int limit);

    @Query(value = """
        select p.id, p.documento_id, p.page_number, p.page_id, p.fingerprint, p.texto_extraido, p.criado_em
          from tb_documento_pagina p
          join tb_documento_processual d on d.id = p.documento_id
         where d.processo_id = :processoId
           and (
                (p.texto_tsv @@ plainto_tsquery('portuguese', :q))
                or (p.texto_extraido ilike ('%' || :q || '%'))
           )
         order by d.criado_em desc, p.page_number asc
         limit :limit
        """, nativeQuery = true)
    List<DocumentoPagina> searchInProcess(@Param("processoId") Long processoId,
                                          @Param("q") String q,
                                          @Param("limit") int limit);


    default List<DocumentoPagina> findByDocumentoId(Long documentoId) {
        return findByDocumentoId(documentoId == null ? null : new UUID(0L, documentoId));
    }

    default long countByDocumentoId(Long documentoId) {
        return countByDocumentoId(documentoId == null ? null : new UUID(0L, documentoId));
    }
}
