package com.tcc.pjb.backend.modules.advocacia.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Enum que representa os estados de um cliente no sistema jurídico")
public enum StatusCliente {

    @Schema(description = "Cliente ativo e com cadastro regular")
    ATIVO,

    @Schema(description = "Cliente temporariamente inativo ou suspenso")
    INATIVO,

    @Schema(description = "Cliente suspenso devido a inconsistências ou bloqueio jurídico")
    SUSPENSO,

    @Schema(description = "Cliente aguardando verificação ou confirmação de dados")
    EM_ANALISE,

    @Schema(description = "Cliente arquivado ou removido logicamente do sistema")
    ARQUIVADO;
}
