package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import java.time.LocalDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpOficioResponse {
    @Schema(type = "string", format = "uuid", example = "01963c1a-7e3f-7000-8000-000000000001")
    private UUID trackingCode;
    private String tipo;
    private String status;
    private String protocolo;
    private String assunto;
    private String conteudo;
    private LaianeDocumentoFormalAssinadoResponse documentoFormalAssinado;
    private LaianeAssinaturaQualificadaResponse assinaturaQualificada;
    private LaianeValidacaoSoberanaResponse validacaoSoberana;
    private LocalDateTime enviadoEm;
    private LocalDateTime entregueEm;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
