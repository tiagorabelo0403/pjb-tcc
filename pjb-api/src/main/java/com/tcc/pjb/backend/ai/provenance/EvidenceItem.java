package com.tcc.pjb.backend.ai.provenance;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class EvidenceItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String docId;
    private final EvidenceType tipo;
    private final String titulo;
    private final String tribunal;
    private final String orgaoJulgador;
    private final String fonteSistema; 
    private final String url;
    private final Instant dataPublicacao;
    private final Double score; 
    private final String trecho; 

    public enum EvidenceType {
        JURISPRUDENCIA,
        LEGISLACAO,
        DOUTRINA,
        PECA_PROCESSUAL,
        DIARIO_OFICIAL,

        
        GUIDELINE,
        CLINICAL_TRIAL,
        SYSTEMATIC_REVIEW,
        VETERINARY_TRANSLATIONAL,

        
        REGULATORY,
        MARKET_DATA,

        OUTRO
    }
}
