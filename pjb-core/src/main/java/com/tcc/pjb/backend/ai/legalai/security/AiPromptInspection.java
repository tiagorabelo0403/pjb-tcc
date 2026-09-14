package com.tcc.pjb.backend.ai.legalai.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record AiPromptInspection(String prompt, boolean neutralizado, Set<String> sinais) {

    public AiPromptInspection {
        sinais = sinais == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(sinais));
    }

    public boolean suspeito() {
        return neutralizado || !sinais.isEmpty();
    }

    public String sinaisConcatenados() {
        return String.join(",", sinais);
    }
}
