package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "notification_history", indexes = {
        @Index(name = "idx_notification_user_time", columnList = "usuarioId,enviadoEm"),
        @Index(name = "idx_notification_user_status_time", columnList = "usuarioId,status,enviadoEm"),
        @Index(name = "idx_notification_user_unread", columnList = "usuarioId,lidoEm,enviadoEm"),
        @Index(name = "idx_notification_process_title", columnList = "processoId,titulo"),
        @Index(name = "idx_notification_user_channel_title_time", columnList = "usuarioId,canal,titulo,enviadoEm"),
        @Index(name = "idx_notification_user_process_channel_time", columnList = "usuarioId,processoId,canal,enviadoEm"),
        @Index(name = "idx_notification_user_process_ciencia_time", columnList = "usuarioId,processoId,ciencia_confirmada_em,enviadoEm"),
        @Index(name = "idx_notification_tracking_token", columnList = "tracking_token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;
    private Long processoId;
    private String canal;
    private String titulo;
    private String mensagem;
    private LocalDateTime enviadoEm;
    private String status;

    @Column(name = "tracking_token", length = 120, unique = true)
    private String trackingToken;

    @Column(name = "tracking_hash", length = 128)
    private String trackingHash;

    @Column(name = "lido_em")
    private LocalDateTime lidoEm;

    @Column(name = "ciencia_confirmada_em")
    private LocalDateTime cienciaConfirmadaEm;
}
