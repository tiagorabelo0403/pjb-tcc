package com.tcc.pjb.backend.modules.atendimento.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;




@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AtendimentoMessageReceiptId implements Serializable {
  private Long messageId;
  private Long usuarioId;
}
