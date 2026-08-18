package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import com.tcc.pjb.backend.modules.laiane.model.LaianeOficioStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Schema(description = "Ofício institucional do Ministério Público gerado e assinado pelo Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpOficioResponse {
    @Schema(type = "string", format = "uuid", example = "01963c1a-7e3f-7000-8000-000000000001",
            description = "Código de rastreamento único do ofício (UUID v7)",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID trackingCode;
    @Schema(description = "Tipo do ofício institucional",
            example = "OFICIO_REQUISITORIO",
            allowableValues = {"OFICIO_REQUISITORIO", "OFICIO_INFORMACAO", "OFICIO_NOTIFICACAO", "OFICIO_ENCAMINHAMENTO"})
    private String tipo;
    @Schema(description = "Status atual do ofício no fluxo de envio",
            example = "ENVIADO",
            allowableValues = {"CRIADO", "ENVIADO", "ENTREGUE", "CANCELADO"})
    private LaianeOficioStatus status;
    @Size(max = 100)
    @Schema(description = "Número de protocolo do ofício", example = "MP/CE-2026-12345")
    private String protocolo;
    @Size(max = 500)
    @Schema(description = "Assunto do ofício", example = "Requisição de informações sobre processo nº 0001234-56.2026.8.06.0001")
    private String assunto;
    @Size(max = 50_000)
    @Schema(description = "Conteúdo textual do ofício", example = "Exmo. Sr. Diretor, solicitamos...")
    private String conteudo;
    @Schema(description = "Documento formal assinado com envelope qualificado")
    private LaianeDocumentoFormalAssinadoResponse documentoFormalAssinado;
    @Schema(description = "Assinatura qualificada ICP-Brasil vinculada ao ofício")
    private LaianeAssinaturaQualificadaResponse assinaturaQualificada;
    @Schema(description = "Resultado da validação soberana da assinatura")
    private LaianeValidacaoSoberanaResponse validacaoSoberana;
    @Schema(description = "Data e hora de envio do ofício", example = "2026-05-31T15:00:00")
    private OffsetDateTime enviadoEm;
    @Schema(description = "Data e hora de entrega confirmada do ofício", example = "2026-05-31T15:05:00")
    private OffsetDateTime entregueEm;
    @Schema(description = "Data e hora de criação do registro", example = "2026-05-31T14:55:00")
    private OffsetDateTime createdAt;
    @Schema(description = "Data e hora da última atualização do registro", example = "2026-05-31T15:05:00")
    private OffsetDateTime updatedAt;
}
