package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeLawyerProcuracaoResponse {
    private Long id;
    private Long clienteId;
    private Long processoId;
    private String status;
    private LocalDate inicioVigencia;
    private LocalDate fimVigencia;
    private String poderes;
    private String anexosJson;
    private Map<String, Object> representacaoPolicy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
