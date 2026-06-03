package com.tcc.pjb.backend.core.protocolo.completude;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoCondicaoRequisito;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoDocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoParteProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoRepresentanteProcessual;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record ContextoValidacaoCompletude(
        RitoProcessual rito,
        TipoRepresentanteProcessual representante,
        TipoParteProcessual tipoPartePrincipal,
        Set<TipoCondicaoRequisito> condicoesAplicaveis,
        List<TipoDocumentoProcessual> documentosAnexados,
        LocalDate dataProtocolo
) {
    public boolean condicaoPresente(TipoCondicaoRequisito condicao) {
        if (condicao == TipoCondicaoRequisito.SEMPRE) return true;
        return condicoesAplicaveis != null && condicoesAplicaveis.contains(condicao);
    }

    public boolean possuiDocumento(TipoDocumentoProcessual tipo) {
        return documentosAnexados != null && documentosAnexados.contains(tipo);
    }
}
