package com.tcc.pjb.backend.core.preflight;

import com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice.PjbDigitalCoreRitoKind;
import java.math.BigDecimal;
import java.util.List;

public record PjbZeroErrorTriageRequest(
        String classeProcessual,
        String assuntoPrincipal,
        PjbDigitalCoreRitoKind ritoKind,
        BigDecimal valorCausa,
        String cepParteAutora,
        String cepParteRe,
        String comarcaSelecionada,
        List<String> documentTypes,
        List<String> documentSignals,
        List<String> petitionSignals,
        boolean pedidoPericiaTecnicaComplexa,
        boolean pedidoTutelaUrgencia,
        boolean parteVulneravel,
        boolean primeiroGrau
) {

    public List<String> safeDocumentTypes() {
        return documentTypes == null ? List.of() : List.copyOf(documentTypes);
    }

    public List<String> safeDocumentSignals() {
        return documentSignals == null ? List.of() : List.copyOf(documentSignals);
    }

    public List<String> safePetitionSignals() {
        return petitionSignals == null ? List.of() : List.copyOf(petitionSignals);
    }
}
