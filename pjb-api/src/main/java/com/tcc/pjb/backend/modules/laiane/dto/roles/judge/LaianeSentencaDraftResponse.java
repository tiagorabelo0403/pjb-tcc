package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import java.time.LocalDateTime;
import java.util.List;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianePrecedenteLiteDto;
import com.tcc.pjb.backend.model.dto.intelligence.JudgeDecisionConsistencyResponse;
import com.tcc.pjb.backend.model.dto.intelligence.QualifiedThemeAlertResponse;
import com.tcc.pjb.backend.model.dto.intelligence.StructuredProcessSummaryResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudicialDecisionAdvisoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeSentencaDraftResponse {

    private Long draftId;
    private String uuid;
    private Long processoId;
    private String status;
    private String inputHash;
    private String draftMarkdown;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private String resumoExecutivo;
    private List<String> questoesASolver;
    private List<String> contradicoes;
    private List<LaianePrecedenteLiteDto> precedentesRelacionados;
    private LaianeJudicialDecisionAdvisoryResponse decisaoAssistida;
    private JudgeDecisionConsistencyResponse consistenciaDecisoria;
    private StructuredProcessSummaryResponse resumoProcessualEstruturado;
    private QualifiedThemeAlertResponse temasQualificados;
    private String pqcAlgorithm;
    private String pqcSignatureB64;
    private String pqcPublicKeyB64;
}
