package com.tcc.pjb.backend.service.api;

import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MarketplaceRepresentacaoResolver {

    private final RepresentacaoProcessualPolicyService representacaoProcessualPolicyService;

    public MarketplaceRepresentacaoResolver(RepresentacaoProcessualPolicyService representacaoProcessualPolicyService) {
        this.representacaoProcessualPolicyService = Objects.requireNonNull(representacaoProcessualPolicyService);
    }

    public InstrumentoRepresentacaoProcessual resolve(RamoDireito ramo, RitoProcessual rito, String tribunal, String perfilAtorRaw) {
        TipoUsuario perfil = TipoUsuario.fromString(perfilAtorRaw);
        var policy = representacaoProcessualPolicyService.resolve(
                ramo == null ? null : ramo.name(),
                rito == null ? null : rito.name(),
                tribunal,
                perfil,
                null, null, null, false, false, null, null);
        if (!policy.regularidadeSuficiente()) {
            return null;
        }
        return InstrumentoRepresentacaoProcessual.fromString(policy.resolvedInstrument());
    }
}
