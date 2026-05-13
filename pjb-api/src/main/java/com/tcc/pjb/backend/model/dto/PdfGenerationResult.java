package com.tcc.pjb.backend.model.dto;

import lombok.Getter;

@Getter
public class PdfGenerationResult {
    private String url;
    private String hash;
    public PdfGenerationResult(String url, String hash) { this.url = url; this.hash = hash; }
}