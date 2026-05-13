package com.tcc.pjb.backend.ai.explainability;


public class ContrastiveExplanation {
    private String recommended;
    private String because;
    private String notRecommended;
    private String becauseNot;

    public ContrastiveExplanation() {
    }

    public ContrastiveExplanation(String recommended, String because, String notRecommended, String becauseNot) {
        this.recommended = recommended;
        this.because = because;
        this.notRecommended = notRecommended;
        this.becauseNot = becauseNot;
    }

    public String getRecommended() {
        return recommended;
    }

    public void setRecommended(String recommended) {
        this.recommended = recommended;
    }

    public String getBecause() {
        return because;
    }

    public void setBecause(String because) {
        this.because = because;
    }

    public String getNotRecommended() {
        return notRecommended;
    }

    public void setNotRecommended(String notRecommended) {
        this.notRecommended = notRecommended;
    }

    public String getBecauseNot() {
        return becauseNot;
    }

    public void setBecauseNot(String becauseNot) {
        this.becauseNot = becauseNot;
    }
}
