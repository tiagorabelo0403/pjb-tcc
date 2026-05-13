package com.tcc.pjb.backend.ai.explainability;

import java.util.ArrayList;
import java.util.List;

public class ExplanationBundle {

    
    @Deprecated(forRemoval = true)
    public java.util.List<SourceCitation> getSources() {
        return getCitations();
    }

    
    @Deprecated(forRemoval = true)
    public java.util.List<ContrastiveExplanation> getContrastiveExplanations() {
        return getContrastive();
    }


    private List<String> reasoning = new ArrayList<>();
    private List<SourceCitation> citations = new ArrayList<>();
    private List<ContrastiveExplanation> contrastive = new ArrayList<>();

    public List<String> getReasoning() {
        return reasoning;
    }

    public void setReasoning(List<String> reasoning) {
        this.reasoning = reasoning;
    }

    public List<SourceCitation> getCitations() {
        return citations;
    }

    public void setCitations(List<SourceCitation> citations) {
        this.citations = citations;
    }

    public List<ContrastiveExplanation> getContrastive() {
        return contrastive;
    }

    public void setContrastive(List<ContrastiveExplanation> contrastive) {
        this.contrastive = contrastive;
    }
}
