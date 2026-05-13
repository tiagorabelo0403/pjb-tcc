package com.tcc.pjb.backend.service.rito.model;

import java.util.List;

public class RitoStage {
    private String fase;
    private List<String> allowedNext;
    private List<WorkTemplate> work;

    public RitoStage() {}

    public RitoStage(String fase, List<String> allowedNext, List<WorkTemplate> work) {
        this.fase = fase;
        this.allowedNext = allowedNext;
        this.work = work;
    }

    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }
    public List<String> getAllowedNext() { return allowedNext; }
    public void setAllowedNext(List<String> allowedNext) { this.allowedNext = allowedNext; }
    public List<WorkTemplate> getWork() { return work; }
    public void setWork(List<WorkTemplate> work) { this.work = work; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final RitoStage target = new RitoStage();
        public Builder fase(String fase) { target.setFase(fase); return this; }
        public Builder allowedNext(List<String> allowedNext) { target.setAllowedNext(allowedNext); return this; }
        public Builder work(List<WorkTemplate> work) { target.setWork(work); return this; }
        public RitoStage build() { return target; }
    }
}

