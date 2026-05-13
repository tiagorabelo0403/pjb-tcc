package com.tcc.pjb.backend.model.entity.calendar;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@PjbDataOwnership(module = PjbModuleId.PRAZOS_AGENDA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_user_calendar_system_event", indexes = {
        @Index(name = "idx_ucse_user_at", columnList = "usuario_id, at"),
        @Index(name = "idx_ucse_user_proc", columnList = "usuario_id, processo_id"),
        @Index(name = "idx_ucse_user_domain", columnList = "usuario_id, domain_key", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCalendarSystemEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "domain_key", nullable = false, length = 180)
    private String domainKey;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "body", length = 4000)
    private String body;

    @Column(name = "at", nullable = false)
    private LocalDateTime at;

    @Column(name = "color", nullable = false, length = 16)
    private String color;

    @Column(name = "details_url", length = 255)
    private String detailsUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
