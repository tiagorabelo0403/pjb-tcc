package com.tcc.pjb.backend.core.processo.polo.motor;

import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PoloCompositionPolicy {

    public List<PoloComposto> compor(Processo processo) {
        if (processo == null || processo.getRito() == null) {
            return List.of();
        }
        RitoProcessual rito = processo.getRito();
        List<ProceduralCatalogSupport.PartyRoleSpec> parties =
                ProceduralCatalogSupport.snapshot(rito).parties();

        List<PoloComposto> resultado = new ArrayList<>();
        for (ProceduralCatalogSupport.PartyRoleSpec spec : parties) {
            if (!spec.required() || !spec.external()) {
                continue;
            }
            PoloRoleMappingTable.lookup(rito, spec.code()).ifPresent(m -> {
                String nome = resolveFonte(m.tipoPolo(), processo);
                if (nome == null) {
                    return;
                }
                resultado.add(new PoloComposto(
                        m.tipoPolo(),
                        m.tipoParte(),
                        nome,
                        resolveCpf(m.tipoPolo(), processo),
                        resolveUf(m.tipoPolo(), processo),
                        resolveComarca(m.tipoPolo(), processo),
                        resolveMunicipio(m.tipoPolo(), processo)));
            });
        }
        return List.copyOf(resultado);
    }

    private String resolveFonte(TipoPolo tipoPolo, Processo processo) {
        if (tipoPolo == TipoPolo.ATIVO)   return processo.getParteAutoraNome();
        if (tipoPolo == TipoPolo.PASSIVO) return processo.getParteReuNome();
        return null;
    }

    private String resolveCpf(TipoPolo tipoPolo, Processo processo) {
        if (tipoPolo == TipoPolo.ATIVO)   return processo.getParteAutoraCpf();
        if (tipoPolo == TipoPolo.PASSIVO) return processo.getParteReuCpf();
        return null;
    }

    private String resolveUf(TipoPolo tipoPolo, Processo processo) {
        if (tipoPolo == TipoPolo.ATIVO)   return processo.getUfAutor();
        if (tipoPolo == TipoPolo.PASSIVO) return processo.getUfReu();
        return null;
    }

    private String resolveComarca(TipoPolo tipoPolo, Processo processo) {
        if (tipoPolo == TipoPolo.ATIVO)   return processo.getComarcaAutor();
        if (tipoPolo == TipoPolo.PASSIVO) return processo.getComarcaReu();
        return null;
    }

    private String resolveMunicipio(TipoPolo tipoPolo, Processo processo) {
        if (tipoPolo == TipoPolo.ATIVO)   return processo.getCidadeAutor();
        if (tipoPolo == TipoPolo.PASSIVO) return processo.getCidadeReu();
        return null;
    }
}
