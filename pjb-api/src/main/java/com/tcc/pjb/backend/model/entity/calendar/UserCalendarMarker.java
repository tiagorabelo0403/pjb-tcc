package com.tcc.pjb.backend.model.entity.calendar;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@PjbDataOwnership(module = PjbModuleId.PRAZOS_AGENDA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_user_calendar_marker")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCalendarMarker {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "event_type", nullable = false, length = 24)
  private String eventType;

  @Column(name = "event_id", nullable = false)
  private Long eventId;

  @Column(name = "color", nullable = false, length = 16)
  private String color;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
