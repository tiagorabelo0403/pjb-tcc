package com.tcc.pjb.backend.service.rito.model;

import java.util.List;

public class WorkTemplate {
    private String code;
    private String type;
    private String title;
    private String description;
    private String actorRole;
    private Integer priority;
    private Integer slaDays;
    private Boolean blocking;
    private List<String> legalBases;

    public WorkTemplate() {}

    public WorkTemplate(String code, String type, String title, String description, String actorRole, Integer priority, Integer slaDays, Boolean blocking, List<String> legalBases) {
        this.code = code;
        this.type = type;
        this.title = title;
        this.description = description;
        this.actorRole = actorRole;
        this.priority = priority;
        this.slaDays = slaDays;
        this.blocking = blocking;
        this.legalBases = legalBases;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getSlaDays() { return slaDays; }
    public void setSlaDays(Integer slaDays) { this.slaDays = slaDays; }
    public Boolean getBlocking() { return blocking; }
    public void setBlocking(Boolean blocking) { this.blocking = blocking; }
    public List<String> getLegalBases() { return legalBases; }
    public void setLegalBases(List<String> legalBases) { this.legalBases = legalBases; }
    public List<String> getChecklist() { return legalBases == null ? java.util.List.of() : legalBases; }
    public void setChecklist(List<String> checklist) { this.legalBases = checklist; }
}
