package com.tcc.pjb.backend.model.dto.security;

import jakarta.validation.constraints.NotBlank;

public class RequestHashComputeRequest {

    @NotBlank
    private String method;

    @NotBlank
    private String path;

    private String query;

    private String bodyHash;

    private Long equipeId;

    private Long deviceId;

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getBodyHash() { return bodyHash; }
    public void setBodyHash(String bodyHash) { this.bodyHash = bodyHash; }

    public Long getEquipeId() { return equipeId; }
    public void setEquipeId(Long equipeId) { this.equipeId = equipeId; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
}
