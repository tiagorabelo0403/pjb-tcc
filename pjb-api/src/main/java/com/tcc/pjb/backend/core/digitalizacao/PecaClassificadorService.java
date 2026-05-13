package com.tcc.pjb.backend.core.digitalizacao;

import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PecaClassificadorService {

    public String classificar(String texto) {
        String value = texto == null ? "" : texto.toLowerCase(Locale.ROOT);
        if (value.contains("sentença") || value.contains("sentenca")) {
            return "SENTENCA";
        }
        if (value.contains("despacho")) {
            return "DESPACHO";
        }
        if (value.contains("certidão") || value.contains("certidao")) {
            return "CERTIDAO";
        }
        if (value.contains("petição") || value.contains("peticao")) {
            return "PETICAO";
        }
        return "DOCUMENTO";
    }
}
