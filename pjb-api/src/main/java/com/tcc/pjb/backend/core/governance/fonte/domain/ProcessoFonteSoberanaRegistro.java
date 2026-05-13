package com.tcc.pjb.backend.core.governance.fonte.domain;

import java.time.Instant;
import java.util.Objects;

public record ProcessoFonteSoberanaRegistro(String chave,
                                            String dominio,
                                            String valor,
                                            String fonteOficial,
                                            String fonteDerivada,
                                            FonteSoberanaConfiabilidade confiabilidade,
                                            FonteSoberanaStatus status,
                                            Instant validoAte,
                                            String fallbackAplicado,
                                            String digest) {
    public ProcessoFonteSoberanaRegistro {
        chave = Objects.toString(chave, "").trim();
        dominio = Objects.toString(dominio, "").trim();
        valor = Objects.toString(valor, "").trim();
        fonteOficial = Objects.toString(fonteOficial, "").trim();
        fonteDerivada = Objects.toString(fonteDerivada, "").trim();
        confiabilidade = confiabilidade == null ? FonteSoberanaConfiabilidade.PROVISORIA : confiabilidade;
        status = status == null ? FonteSoberanaStatus.REVALIDAR : status;
        fallbackAplicado = Objects.toString(fallbackAplicado, "").trim();
        digest = Objects.toString(digest, "").trim();
    }

    public boolean expirada(Instant referencia) {
        return validoAte != null && validoAte.isBefore(referencia == null ? Instant.now() : referencia);
    }

    public boolean oficialOuVerificada() {
        return confiabilidade == FonteSoberanaConfiabilidade.OFICIAL || confiabilidade == FonteSoberanaConfiabilidade.DERIVADA_VERIFICADA;
    }
}
