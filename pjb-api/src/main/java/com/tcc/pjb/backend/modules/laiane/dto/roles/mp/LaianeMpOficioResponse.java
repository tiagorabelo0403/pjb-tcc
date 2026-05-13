package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpOficioResponse {
    private Long id;
    private UUID trackingCode;
    private Long origemId;
    private Long destinoId;
    private String tipo;
    private String status;
    private String protocolo;
    private String assunto;
    private String conteudo;
    private Map<String, Object> documentoFormalAssinado;
    private Map<String, Object> assinaturaQualificada;
    private Map<String, Object> validacaoSoberana;
    private LocalDateTime enviadoEm;
    private LocalDateTime entregueEm;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
