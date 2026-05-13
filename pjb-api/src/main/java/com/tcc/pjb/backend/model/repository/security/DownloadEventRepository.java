package com.tcc.pjb.backend.model.repository.security;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.security.DownloadEvent;

public interface DownloadEventRepository extends JpaRepository<DownloadEvent, Long> {

    @Query("select count(e) from DownloadEvent e where e.usuario.id = :userId and e.createdAt >= :from")
    long countRecentByUser(@Param("userId") Long userId, @Param("from") LocalDateTime from);

    @Query("select count(e) from DownloadEvent e where e.usuario.id = :userId and e.deviceId = :deviceId and e.createdAt >= :from")
    long countRecentByUserAndDevice(@Param("userId") Long userId, @Param("deviceId") Long deviceId, @Param("from") LocalDateTime from);

    @Query("select coalesce(sum(e.bytes), 0) from DownloadEvent e where e.usuario.id = :userId and e.createdAt >= :from")
    long sumBytesRecentByUser(@Param("userId") Long userId, @Param("from") LocalDateTime from);

    @Query("select coalesce(sum(e.bytes), 0) from DownloadEvent e where e.usuario.id = :userId and e.deviceId = :deviceId and e.createdAt >= :from")
    long sumBytesRecentByUserAndDevice(@Param("userId") Long userId, @Param("deviceId") Long deviceId, @Param("from") LocalDateTime from);

    @Query("select min(e.createdAt) from DownloadEvent e where e.usuario.id = :userId and e.createdAt >= :from")
    LocalDateTime oldestRecentByUser(@Param("userId") Long userId, @Param("from") LocalDateTime from);

    @Query("select min(e.createdAt) from DownloadEvent e where e.usuario.id = :userId and e.deviceId = :deviceId and e.createdAt >= :from")
    LocalDateTime oldestRecentByUserAndDevice(@Param("userId") Long userId, @Param("deviceId") Long deviceId, @Param("from") LocalDateTime from);

    @Query("select count(e) from DownloadEvent e where e.usuario.id = :userId and e.processoId = :processoId and e.createdAt >= :from")
    long countRecentByUserAndProcess(@Param("userId") Long userId, @Param("processoId") Long processoId, @Param("from") LocalDateTime from);

    @Query("select coalesce(sum(e.bytes), 0) from DownloadEvent e where e.usuario.id = :userId and e.processoId = :processoId and e.createdAt >= :from")
    long sumBytesRecentByUserAndProcess(@Param("userId") Long userId, @Param("processoId") Long processoId, @Param("from") LocalDateTime from);

    @Query("select min(e.createdAt) from DownloadEvent e where e.usuario.id = :userId and e.processoId = :processoId and e.createdAt >= :from")
    LocalDateTime oldestRecentByUserAndProcess(@Param("userId") Long userId, @Param("processoId") Long processoId, @Param("from") LocalDateTime from);

    @Query("select count(e) from DownloadEvent e where e.usuario.id = :userId and e.documentoId = :documentoId and e.createdAt >= :from")
    long countRecentByUserAndDocumento(@Param("userId") Long userId, @Param("documentoId") String documentoId, @Param("from") LocalDateTime from);

    @Query("select coalesce(sum(e.bytes), 0) from DownloadEvent e where e.usuario.id = :userId and e.documentoId = :documentoId and e.createdAt >= :from")
    long sumBytesRecentByUserAndDocumento(@Param("userId") Long userId, @Param("documentoId") String documentoId, @Param("from") LocalDateTime from);

    @Query("select min(e.createdAt) from DownloadEvent e where e.usuario.id = :userId and e.documentoId = :documentoId and e.createdAt >= :from")
    LocalDateTime oldestRecentByUserAndDocumento(@Param("userId") Long userId, @Param("documentoId") String documentoId, @Param("from") LocalDateTime from);

}