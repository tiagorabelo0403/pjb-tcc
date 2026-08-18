package com.tcc.pjb.backend.model.dto.advogado;

import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoCondicaoRequisito;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoParteProcessual;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

@Schema(description = "Solicitação de protocolo de petição inicial a partir de rascunho Laiane")
public record LaianePeticaoInicialProtocolarRequest(
        @Schema(description = "Tipo de justiça para protocolo", example = "ESTADUAL")
        String tipoJustica,

        @Schema(description = "Tipo de parte do polo ativo (para gate de completude)", example = "PESSOA_FISICA")
        TipoParteProcessual tipoPartePrincipal,

        @Size(max = 10)
        @Schema(description = "Condições jurídicas aplicáveis ao protocolo (ex: PEDE_JUSTICA_GRATUITA)")
        Set<TipoCondicaoRequisito> condicoesAplicaveis,

        @Size(max = 50)
        @Schema(description = "Tipos de documentos que serão anexados ao protocolo")
        List<TipoDocumento> documentosAnexados
) {}
