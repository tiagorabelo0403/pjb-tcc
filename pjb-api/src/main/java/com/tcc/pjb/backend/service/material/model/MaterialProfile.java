package com.tcc.pjb.backend.service.material.model;

import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialProfile {
    private List<String> requiredDocuments;
    private List<String> proofChecklist;
    private List<String> legalBases;
    private List<String> warnings;
}
