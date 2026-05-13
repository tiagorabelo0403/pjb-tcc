package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpAuditEventDto {
    private UUID uuid;
    private String acao;
    private Long usuarioId;
    private String referenciaId;
    private String detalhes;
    private String justificativa;
    private LocalDateTime timestamp;
    private String nivelRisco;
    private String perfilComportamental;
    private String hashIntegridade;
}
