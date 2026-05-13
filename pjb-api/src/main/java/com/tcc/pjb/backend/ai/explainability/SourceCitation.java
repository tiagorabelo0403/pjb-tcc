package com.tcc.pjb.backend.ai.explainability;


public class SourceCitation {
    private String sourceId;
    private String title;
    private String location;
    private String snippet;

    public SourceCitation() {
    }

    public SourceCitation(String sourceId, String title, String location, String snippet) {
        this.sourceId = sourceId;
        this.title = title;
        this.location = location;
        this.snippet = snippet;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}
