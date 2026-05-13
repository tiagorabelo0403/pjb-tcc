package com.tcc.pjb.backend.modules.atendimento.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AtendimentoThreadMemberSettingsId implements Serializable {
  private Long threadId;
  private Long usuarioId;
}
