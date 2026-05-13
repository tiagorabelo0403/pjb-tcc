package com.tcc.pjb.backend.integration.mni.adapter;

import com.tcc.pjb.backend.model.entity.Processo;
import org.springframework.stereotype.Component;

@Component
public class MniProcessoPayloadAssembler {

    public String toXml(Processo processo) {
        String numero = escape(processo.getNumeroUnificado());
        String tribunal = escape(processo.getTribunal());
        String classe = escape(processo.getClasseProcessual());
        String assunto = escape(processo.getAssunto());
        return "<mniRemessa><processo><numeroUnificado>" + numero + "</numeroUnificado><tribunal>" + tribunal + "</tribunal><classe>" + classe + "</classe><assunto>" + assunto + "</assunto></processo></mniRemessa>";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
