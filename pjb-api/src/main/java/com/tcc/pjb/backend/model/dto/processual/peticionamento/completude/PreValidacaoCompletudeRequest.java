package com.tcc.pjb.backend.model.dto.processual.peticionamento.completude;

import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoCondicaoRequisito;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoParteProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

@Schema(description = "Solicitação de pré-validação de completude documental (modo rascunho, sem persistência)")
public record PreValidacaoCompletudeRequest(
        @NotNull
        @Schema(description = "Rito processual da petição", example = "COMUM_ORDINARIO")
        RitoProcessual rito,

        @NotNull
        @Schema(description = "Tipo de parte principal do polo ativo", example = "PESSOA_FISICA")
        TipoParteProcessual tipoPartePrincipal,

        @Size(max = 10)
        @Schema(description = "Condições jurídicas aplicáveis ao protocolo")
        Set<TipoCondicaoRequisito> condicoesAplicaveis,

        @Size(max = 50)
        @Schema(description = "Tipos de documentos que serão anexados ao protocolo")
        List<TipoDocumento> documentosAnexados
) {}
