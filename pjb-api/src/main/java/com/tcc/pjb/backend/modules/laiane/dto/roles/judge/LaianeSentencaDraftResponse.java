package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import com.tcc.pjb.backend.model.dto.intelligence.JudgeDecisionConsistencyResponse;
import com.tcc.pjb.backend.model.dto.intelligence.QualifiedThemeAlertResponse;
import com.tcc.pjb.backend.model.dto.intelligence.StructuredProcessSummaryResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianePrecedenteLiteDto;
import com.tcc.pjb.backend.modules.laiane.model.LaianeSentencaStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Rascunho de sentença gerado e assistido pelo Laiane")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeSentencaDraftResponse {

    @Schema(description = "Identificador interno do rascunho", example = "88")
    private Long draftId;
    @Schema(description = "UUID do rascunho", example = "01963c1a-7e3f-7000-8000-000000000001")
    private String uuid;
    @Schema(description = "ID do processo sentenciado", example = "12345")
    private Long processoId;
    @Schema(description = "Status do rascunho",
            example = "DRAFT",
            allowableValues = {"DRAFT", "PUBLISHED", "ARCHIVED"})
    private LaianeSentencaStatus status;
    @Schema(description = "Hash do input processual que originou este rascunho", example = "sha256:abc...")
    private String inputHash;
    @Size(max = 500_000)
    @Schema(description = "Conteúdo do rascunho de sentença em markdown")
    private String draftMarkdown;
    @Schema(description = "Data e hora de criação do rascunho", example = "2026-05-31T10:00:00")
    private OffsetDateTime createdAt;
    @Schema(description = "Data e hora de publicação da sentença", example = "2026-05-31T16:00:00")
    private OffsetDateTime publishedAt;
    @Size(max = 5000)
    @Schema(description = "Resumo executivo do rascunho",
            example = "Sentença condenatória — réu incurso no art. 155 CP")
    private String resumoExecutivo;
    @Size(max = 30)
    @Schema(description = "Questões jurídicas a resolver antes da publicação (máx. 30)")
    private List<String> questoesASolver;
    @Size(max = 20)
    @Schema(description = "Contradições processuais identificadas (máx. 20)")
    private List<String> contradicoes;
    @Size(max = 10)
    @Schema(description = "Precedentes relacionados identificados pela IA (máx. 10)")
    private List<LaianePrecedenteLiteDto> precedentesRelacionados;
    @Schema(description = "Consultoria de decisão judicial assistida")
    private LaianeJudicialDecisionAdvisoryResponse decisaoAssistida;
    @Schema(description = "Relatório de consistência decisória")
    private JudgeDecisionConsistencyResponse consistenciaDecisoria;
    @Schema(description = "Resumo processual estruturado")
    private StructuredProcessSummaryResponse resumoProcessualEstruturado;
    @Schema(description = "Alertas de temas qualificados")
    private QualifiedThemeAlertResponse temasQualificados;
    @Schema(description = "Algoritmo PQC aplicado na assinatura", example = "CRYSTALS-Dilithium")
    private String pqcAlgorithm;
    @Schema(description = "Assinatura PQC em Base64")
    private String pqcSignatureB64;
    @Schema(description = "Chave pública PQC em Base64")
    private String pqcPublicKeyB64;
}
