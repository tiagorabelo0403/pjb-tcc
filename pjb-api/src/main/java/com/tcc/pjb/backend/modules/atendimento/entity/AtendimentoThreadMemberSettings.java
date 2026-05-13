package com.tcc.pjb.backend.modules.atendimento.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tb_atendimento_thread_member_settings",
    indexes = {
        @Index(name = "idx_atendimento_tms_user", columnList = "usuario_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(AtendimentoThreadMemberSettingsId.class)
public class AtendimentoThreadMemberSettings {

  @Id
  @Column(name = "thread_id", nullable = false)
  private Long threadId;

  @Id
  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "muted_until")
  private Instant mutedUntil;

  @Column(name = "quiet_hours_start_min")
  private Short quietHoursStartMin;

  @Column(name = "quiet_hours_end_min")
  private Short quietHoursEndMin;

  @Column(name = "quiet_days_mask")
  private Integer quietDaysMask;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
