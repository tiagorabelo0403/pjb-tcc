package com.tcc.pjb.backend.service.rito.model;

import java.util.List;

public class RitoDefinition {
    private String rito;
    private String title;
    private String ramoSugerido;
    private String definitionsVersion;
    private String definitionsHash;
    private java.util.Map<String, Object> metadata;
    private List<RitoStage> stages;

    public RitoDefinition() {}

    public RitoDefinition(String rito, String title, String ramoSugerido, List<RitoStage> stages) {
        this(rito, title, ramoSugerido, null, null, java.util.Map.of(), stages);
    }

    public RitoDefinition(String rito,
                          String title,
                          String ramoSugerido,
                          String definitionsVersion,
                          String definitionsHash,
                          java.util.Map<String, Object> metadata,
                          List<RitoStage> stages) {
        this.rito = rito;
        this.title = title;
        this.ramoSugerido = ramoSugerido;
        this.definitionsVersion = definitionsVersion;
        this.definitionsHash = definitionsHash;
        this.metadata = metadata;
        this.stages = stages;
    }

    public String getRito() { return rito; }
    public void setRito(String rito) { this.rito = rito; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRamoSugerido() { return ramoSugerido; }
    public void setRamoSugerido(String ramoSugerido) { this.ramoSugerido = ramoSugerido; }
    public String getDefinitionsVersion() { return definitionsVersion; }
    public void setDefinitionsVersion(String definitionsVersion) { this.definitionsVersion = definitionsVersion; }
    public String getDefinitionsHash() { return definitionsHash; }
    public void setDefinitionsHash(String definitionsHash) { this.definitionsHash = definitionsHash; }
    public java.util.Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(java.util.Map<String, Object> metadata) { this.metadata = metadata; }
    public List<RitoStage> getStages() { return stages; }
    public void setStages(List<RitoStage> stages) { this.stages = stages; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final RitoDefinition target = new RitoDefinition();
        public Builder rito(String rito) { target.setRito(rito); return this; }
        public Builder title(String title) { target.setTitle(title); return this; }
        public Builder ramoSugerido(String ramoSugerido) { target.setRamoSugerido(ramoSugerido); return this; }
        public Builder definitionsVersion(String definitionsVersion) { target.setDefinitionsVersion(definitionsVersion); return this; }
        public Builder definitionsHash(String definitionsHash) { target.setDefinitionsHash(definitionsHash); return this; }
        public Builder metadata(java.util.Map<String, Object> metadata) { target.setMetadata(metadata); return this; }
        public Builder stages(List<RitoStage> stages) { target.setStages(stages); return this; }
        public RitoDefinition build() { return target; }
    }
}

