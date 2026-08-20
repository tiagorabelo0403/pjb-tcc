package com.tcc.pjb.backend.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "pjb-processos", createIndex = false)
@Setting(settingPath = "/elasticsearch/settings-pjb-processos.json")
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class ProcessoQueryModel {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String numeroUnificado;

    @Field(type = FieldType.Keyword)
    private String statusAtual;

    @Field(type = FieldType.Keyword)
    private String correlationId;

    @Field(type = FieldType.Long)
    private Long usuarioId;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String nomeUsuario;

    @Field(type = FieldType.Long)
    private Long jurisdicaoId;

    @Field(type = FieldType.Keyword)
    private String nomeJurisdicao;

    @Field(type = FieldType.Keyword)
    private String materia;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String assunto;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String objetoProcessual;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String pedidoPrincipal;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String pedidosConsolidados;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String materialProbatorioResumo;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String janelaAcordoResumo;

    @Field(type = FieldType.Keyword)
    private String tipoJusticaSugerida;

    @Field(type = FieldType.Keyword)
    private String ritoProcessual;

    @Field(type = FieldType.Keyword)
    private String regimeProcedimental;

    @Field(type = FieldType.Keyword)
    private String proceduralTrack;

    @Field(type = FieldType.Keyword)
    private String acaoNatureza;

    @Field(type = FieldType.Keyword)
    private String perfilProbatorio;

    @Field(type = FieldType.Keyword)
    private String faixaComplexidade;

    @Field(type = FieldType.Keyword)
    private String aderenciaEconomica;

    @Field(type = FieldType.Keyword)
    private String codigoDiagnosticoEconomico;

    @Field(type = FieldType.Keyword)
    private String varaSugerida;

    @Field(type = FieldType.Keyword)
    private String classeTpuCodigo;

    @Field(type = FieldType.Keyword)
    private String competenciaTerritorialModo;

    @Field(type = FieldType.Keyword)
    private String preventionMode;

    @Field(type = FieldType.Keyword)
    private String linkageMode;

    @Field(type = FieldType.Keyword)
    private String unidadeJudiciariaCodigo;

    @Field(type = FieldType.Keyword)
    private String sistemaConector;

    @Field(type = FieldType.Keyword)
    private String preProtocoloStatus;

    @Field(type = FieldType.Keyword)
    private String submissionBlueprintStatus;

    @Field(type = FieldType.Keyword)
    private String submissionDryRunStatus;

    @Field(type = FieldType.Keyword)
    private String localCorrelationMode;

    @Field(type = FieldType.Keyword)
    private String connectorExecutionMode;

    @Field(type = FieldType.Keyword)
    private String connectorSubmissionLane;

    @Field(type = FieldType.Keyword)
    private String connectorTargetKey;

    @Field(type = FieldType.Keyword)
    private String connectorSubmissionStatus;

    @Field(type = FieldType.Keyword)
    private String connectorProtocolReference;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String connectorSubmissionMessage;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime connectorSubmissionProcessedAt;

    @Field(type = FieldType.Integer)
    private Integer connectorSubmissionAttempts;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime connectorLastSubmissionAttemptAt;

    @Field(type = FieldType.Keyword)
    private String connectorSyncStatus;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String connectorSyncMessage;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime connectorSnapshotSyncedAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime connectorEventsSyncedAt;

    @Field(type = FieldType.Integer)
    private Integer connectorSyncAttempts;

    @Field(type = FieldType.Keyword)
    private String tribunalSugerido;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal confiancaRoteamento;

    @Field(type = FieldType.Integer)
    private Integer potencialAcordoScore;

    @Field(type = FieldType.Integer)
    private Integer materialProbatorioScore;

    @Field(type = FieldType.Keyword)
    private String classificacaoProbatoria;

    @Field(type = FieldType.Keyword)
    private String classificacaoNegocial;

    @Field(type = FieldType.Keyword)
    private String estrategiaContenciosa;

    @Field(type = FieldType.Keyword)
    private String prontidaoProtocolar;

    @Field(type = FieldType.Keyword)
    private String posturaNegocial;

    @Field(type = FieldType.Keyword)
    private String maturidadeProbatoria;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String gapsEstrategicos;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String planoEstrutural;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal valorCausa;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dataCriacao;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String poloAtivo;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String poloPassivo;

    @Field(type = FieldType.Keyword)
    @Builder.Default
    private Set<String> tags = new LinkedHashSet<>();

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String resumoPublico;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime atualizadoEm;

    private String analiseTriagemV1;
    private String peticaoInicialText;

    public void marcarAtualizacao() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public void addTag(String tag) {
        if (tag == null) {
            return;
        }
        String t = tag.trim();
        if (!t.isEmpty()) {
            if (this.tags == null) {
                this.tags = new LinkedHashSet<>();
            }
            this.tags.add(t);
        }
    }

    public String getAnaliseTriagemV1() {
        return analiseTriagemV1;
    }

    public void setAnaliseTriagemV1(String analiseTriagemV1) {
        this.analiseTriagemV1 = analiseTriagemV1;
    }

    public String getPeticaoInicialText() {
        return peticaoInicialText;
    }

    public void setPeticaoInicialText(String peticaoInicialText) {
        this.peticaoInicialText = peticaoInicialText;
    }
}
