package com.tcc.pjb.backend.service.secretariat.ingest;

import org.springframework.stereotype.Service;

@Service
public class ProcessoExternoOrigemResolver {

    public SistemaProcessualOrigem resolver(String sistemaDeclarado, String formatoEnvelope) {
        if (sistemaDeclarado == null && formatoEnvelope == null) return SistemaProcessualOrigem.OUTRO;

        if (sistemaDeclarado != null) {
            String upper = sistemaDeclarado.toUpperCase();
            for (SistemaProcessualOrigem origem : SistemaProcessualOrigem.values()) {
                if (upper.contains(origem.name().replace("_", ""))) return origem;
            }
            if (upper.contains("PJE2") || upper.contains("PJE_2")) return SistemaProcessualOrigem.PJE_2X;
            if (upper.contains("PJE")) return SistemaProcessualOrigem.PJE;
            if (upper.contains("ESAJ") || upper.contains("TJSP")) return SistemaProcessualOrigem.ESAJ;
            if (upper.contains("EPROC") || upper.contains("TRF4") || upper.contains("TRF5")) return SistemaProcessualOrigem.EPROC;
        }

        if (formatoEnvelope != null) {
            String upper = formatoEnvelope.toUpperCase();
            if (upper.contains("MNI")) return SistemaProcessualOrigem.MNI;
            if (upper.contains("PDPJ")) return SistemaProcessualOrigem.PDPJ;
        }

        return SistemaProcessualOrigem.OUTRO;
    }
}
