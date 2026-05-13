package com.tcc.pjb.backend.service.policy.impl;

import java.util.Locale;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.policy.SigiloPolicy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SigiloPolicyPadrao implements SigiloPolicy {

    @Override
    public NivelSigilo definirNivel(Processo processo) {
        if (processo == null) return NivelSigilo.PUBLICO;

        RamoDireito ramo = processo.getRamoDireito();
        String assunto = norm(processo.getAssunto());
        String classe = norm(processo.getClasseProcessual());

        
        if (ramo == RamoDireito.FAMILIA || containsAny(assunto, "FAMIL", "ALIMENT", "GUARDA", "DIVORC") || containsAny(classe, "FAMIL")) {
            return NivelSigilo.SEGREDO_JUSTICA;
        }

        
        if (containsAny(assunto, "INFANC", "JUVENTUDE", "ADOLESC", "ECA", "TUTELA", "GUARDIAO")
                || containsAny(assunto, "VIOLENCIA DOMESTICA", "MARIA DA PENHA", "MEDIDA PROTETIVA")) {
            return NivelSigilo.SEGREDO_JUSTICA;
        }

        
        if (containsAny(assunto, "INTERDICAO", "CURATELA", "SAUDE", "HOSPITAL", "TRATAMENTO")) {
            return NivelSigilo.RESTRITO;
        }

        
        if (ramo == RamoDireito.PENAL && containsAny(assunto, "QUEBRA DE SIGILO", "INTERCEPTACAO", "BUSCA E APREENSAO")) {
            return NivelSigilo.RESTRITO;
        }

        return NivelSigilo.PUBLICO;
    }

    private String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... needles) {
        String t = norm(text);
        for (String n : needles) {
            if (!n.isBlank() && t.contains(n)) return true;
        }
        return false;
    }
}
