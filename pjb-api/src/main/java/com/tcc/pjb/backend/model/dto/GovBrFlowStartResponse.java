package com.tcc.pjb.backend.model.dto;

import lombok.Getter;

@Getter
public class GovBrFlowStartResponse {
    private String signedDocumentUrl;
    public GovBrFlowStartResponse(String signedDocumentUrl) { this.signedDocumentUrl = signedDocumentUrl; }
}