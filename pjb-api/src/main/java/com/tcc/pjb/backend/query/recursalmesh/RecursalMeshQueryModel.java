package com.tcc.pjb.backend.query.recursalmesh;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = RecursalMeshQueryModel.INDEX_NAME)
@Setting(settingPath = "/elasticsearch/settings-pjb-recursal-mesh.json")
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class RecursalMeshQueryModel {

    public static final String INDEX_NAME = "pjb-recursal-mesh";

    @Id
    private String recursoId;

    @Field(type = FieldType.Long)
    private Long processoId;

    @Field(type = FieldType.Keyword)
    private String numeroProcesso;

    @Field(type = FieldType.Keyword)
    private String speciesCode;

    @Field(type = FieldType.Keyword)
    private String profileName;

    @Field(type = FieldType.Keyword)
    private String currentState;

    @Field(type = FieldType.Keyword)
    private String tribunalAtual;

    @Field(type = FieldType.Keyword)
    private String tribunalDetalhadoAtual;

    @Field(type = FieldType.Keyword)
    private String instanciaAtual;

    @Field(type = FieldType.Keyword)
    private String autoridadeAtual;

    @Field(type = FieldType.Keyword)
    private String lastEvent;

    @Field(type = FieldType.Integer)
    private Integer currentRevision;

    @Field(type = FieldType.Integer)
    private Integer totalTransitions;

    @Field(type = FieldType.Integer)
    private Integer iteracoesEmbargos;

    @Field(type = FieldType.Boolean)
    private Boolean transitadoEmJulgado;

    @Field(type = FieldType.Keyword)
    private String lastActor;

    @Field(type = FieldType.Keyword)
    private String tribunalProcesso;

    @Field(type = FieldType.Keyword)
    private String varaProcesso;

    @Field(type = FieldType.Keyword)
    private String comarcaProcesso;

    @Field(type = FieldType.Keyword)
    private String ufProcesso;

    @Field(type = FieldType.Keyword)
    private String sigiloProcesso;

    @Field(type = FieldType.Keyword)
    private String ramoProcesso;

    @Field(type = FieldType.Keyword)
    private String ritoProcesso;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String assuntoProcesso;

    @Field(type = FieldType.Boolean)
    private Boolean sobrestadoPrecedente;

    @Field(type = FieldType.Keyword)
    private String precedenteCodigo;

    @Field(type = FieldType.Keyword)
    private String precedenteTribunal;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String precedenteTema;

    @Field(type = FieldType.Boolean)
    private Boolean precedenteAplicado;

    @Field(type = FieldType.Boolean)
    private Boolean precedenteDistinguido;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String fundamentoDistincao;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate slaDataPrevistaSaida;

    @Field(type = FieldType.Integer)
    private Integer slaDiasUteisEsperados;

    @Field(type = FieldType.Boolean)
    private Boolean slaFatalParaPartes;

    @Field(type = FieldType.Boolean)
    private Boolean slaVencido;

    @Field(type = FieldType.Integer)
    private Integer slaDiasUteisExcedidos;

    @Field(type = FieldType.Keyword)
    private String slaSeveridade;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String slaFundamentoLegal;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String searchableText;

    @Field(type = FieldType.Keyword)
    @Builder.Default
    private Set<String> tags = new LinkedHashSet<>();

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private Instant lastTransitionAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private Instant createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private Instant updatedAt;

    public void addTag(String value) {
        if (value == null) {
            return;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return;
        }
        if (tags == null) {
            tags = new LinkedHashSet<>();
        }
        tags.add(normalized);
    }
}
