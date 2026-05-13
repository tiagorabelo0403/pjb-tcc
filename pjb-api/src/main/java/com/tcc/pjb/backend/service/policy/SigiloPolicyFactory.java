package com.tcc.pjb.backend.service.policy;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.policy.impl.SigiloPolicyPadrao;
import com.tcc.pjb.backend.service.policy.impl.SigiloPolicyTJRJ;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SigiloPolicyFactory {

    private final SigiloPolicyPadrao sigiloPolicyPadrao;
    private final SigiloPolicyTJRJ sigiloPolicyTjrj;

    public SigiloPolicy obterPoliticaDeSigilo(Processo processo) {
        if (processo != null && processo.getJurisdicao() != null && processo.getJurisdicao().getNome() != null) {
            String nome = processo.getJurisdicao().getNome().toUpperCase();
            if (nome.contains("TJRJ")) {
                return sigiloPolicyTjrj;
            }
        }
        return sigiloPolicyPadrao;
    }
}
