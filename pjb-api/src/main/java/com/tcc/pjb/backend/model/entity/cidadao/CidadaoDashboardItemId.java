package com.tcc.pjb.backend.model.entity.cidadao;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CidadaoDashboardItemId implements Serializable {
    private Long cidadaoUserId;
    private Long processoId;
}
