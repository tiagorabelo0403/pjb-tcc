package com.tcc.pjb.backend.integration.mni.adapter;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MniXmlToProcessoAdapter {

    private static final Pattern TAG_PATTERN = Pattern.compile("<([A-Za-z0-9_:-]+)>(.*?)</\\1>", Pattern.DOTALL);

    public Processo fromXml(String xml, String tribunalOrigem, String motivo) {
        Objects.requireNonNull(xml, "xml");
        Processo processo = new Processo();
        processo.setNumeroUnificado(firstNonBlank(tag(xml, "numeroUnificado"), tag(xml, "numero_processo"), tag(xml, "numero")));
        processo.setNumeroProcesso(firstNonBlank(processo.getNumeroUnificado(), tag(xml, "numeroProcesso")));
        processo.setTribunal(firstNonBlank(tag(xml, "tribunalDestino"), tribunalOrigem));
        processo.setUf(firstNonBlank(tag(xml, "uf"), inferUf(tribunalOrigem)));
        processo.setComarca(firstNonBlank(tag(xml, "comarca"), "MNI_RECEPCAO"));
        processo.setAssunto(firstNonBlank(tag(xml, "assunto"), tag(xml, "motivo"), motivo));
        processo.setClasseProcessual(firstNonBlank(tag(xml, "classeProcessual"), "MNI_RECEBIDO"));
        processo.setClasseTpuCodigo(firstNonBlank(tag(xml, "classeTpuCodigo"), tag(xml, "classe_tpu_codigo")));
        processo.setConnectorSystem("MNI");
        processo.setConnectorSubmissionStatus("RECEIVED");
        processo.setConnectorProtocolReference(firstNonBlank(tag(xml, "protocoloDestino"), tag(xml, "protocolo")));
        processo.setObjetoProcessual(firstNonBlank(tag(xml, "objetoProcessual"), tag(xml, "conteudo"), xml));
        processo.setRamoDireito(resolveRamo(tag(xml, "ramoDireito")));
        processo.setRito(resolveRito(tag(xml, "rito")));
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        return processo;
    }

    private String tag(String xml, String tag) {
        String normalizedTag = tag == null ? null : tag.trim();
        if (normalizedTag == null || normalizedTag.isBlank()) {
            return null;
        }
        Matcher matcher = TAG_PATTERN.matcher(xml);
        while (matcher.find()) {
            if (normalizedTag.equalsIgnoreCase(matcher.group(1))) {
                String out = matcher.group(2);
                return out == null ? null : out.trim();
            }
        }
        return null;
    }

    private RamoDireito resolveRamo(String raw) {
        RamoDireito ramo = RamoDireito.fromString(raw);
        return ramo == null ? RamoDireito.CIVIL : ramo;
    }

    private RitoProcessual resolveRito(String raw) {
        return RitoProcessual.tryParse(raw).orElse(RitoProcessual.COMUM_ORDINARIO);
    }

    private String inferUf(String tribunalOrigem) {
        if (tribunalOrigem == null || tribunalOrigem.isBlank()) {
            return null;
        }
        String normalized = tribunalOrigem.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() >= 2) {
            return normalized.substring(Math.max(0, normalized.length() - 2));
        }
        return normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
