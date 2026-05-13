package com.tcc.pjb.backend.model.dto;

import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessoResponse {

    private Long id;
    private String numeroProcesso;
    private String classe;
    private String materia;
    private String rito;
    private String assunto;
    private String objetoProcessual;
    private String pedidoPrincipal;
    private String pedidosConsolidados;
    private String materialProbatorioResumo;
    private Integer materialProbatorioScore;
    private Integer potencialAcordoScore;
    private String janelaAcordoResumo;
    private String classificacaoProbatoria;
    private String classificacaoNegocial;
    private String estrategiaContenciosa;
    private String prontidaoProtocolar;
    private String posturaNegocial;
    private String maturidadeProbatoria;
    private ProceduralRoutingReport proceduralRouting;
    private ProceduralSubmissionBlueprintReport submissionBlueprint;
    private ProceduralConnectorExecutionReport connectorExecution;
    private String connectorSubmissionStatus;
    private String connectorProtocolReference;
    private String connectorSubmissionMessage;
    private LocalDateTime connectorSubmissionProcessedAt;
    private Integer connectorSubmissionAttempts;
    private LocalDateTime connectorLastSubmissionAttemptAt;
    private String connectorSyncStatus;
    private String connectorSyncMessage;
    private LocalDateTime connectorSnapshotSyncedAt;
    private LocalDateTime connectorEventsSyncedAt;
    private Integer connectorSyncAttempts;
    private List<String> gapsEstrategicos;
    private List<String> planoEstrutural;
    private BigDecimal valorCausa;
    private LocalDateTime dataDistribuicao;
}
