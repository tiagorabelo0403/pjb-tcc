package com.tcc.pjb.backend.modules.atendimento.entity;

import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AtendimentoReadStateId implements Serializable {

  private Long threadId;
  private Long usuarioId;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AtendimentoReadStateId that)) return false;
    return Objects.equals(threadId, that.threadId) && Objects.equals(usuarioId, that.usuarioId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(threadId, usuarioId);
  }
}
