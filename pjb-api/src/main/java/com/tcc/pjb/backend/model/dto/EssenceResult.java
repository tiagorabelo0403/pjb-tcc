package com.tcc.pjb.backend.model.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EssenceResult {

    private boolean essencePreserved;     
    private double similarityScore;       
    private double differenceScore;       
    private List<String> divergences;     
    private String suggestion;            

    public EssenceResult(boolean essencePreserved,
                         double similarityScore,
                         double differenceScore,
                         List<String> divergences,
                         String suggestion) {
        this.essencePreserved = essencePreserved;
        this.similarityScore = similarityScore;
        this.differenceScore = differenceScore;
        this.divergences = divergences;
        this.suggestion = suggestion;
    }

    
    public double getDifference() {
        return differenceScore;
    }

    
    public double getSimilarity() {
        return similarityScore;
    }
}