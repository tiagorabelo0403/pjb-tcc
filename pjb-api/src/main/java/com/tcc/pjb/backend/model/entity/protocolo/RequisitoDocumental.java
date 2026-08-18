package com.tcc.pjb.backend.model.entity.protocolo;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.FonteNormativaTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.GrauExigibilidade;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.SeveridadeCompletude;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoCondicaoRequisito;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoParteProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoRepresentanteProcessual;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.AJUIZAMENTO, mode = PjbOwnershipMode.OWNER_ONLY)
@Entity
@Table(name = "tb_requisito_documental",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_requisito_documental_uuid", columnNames = {"uuid"}),
                @UniqueConstraint(name = "uq_requisito_documental_chave",
                        columnNames = {"rito_codigo", "tipo_documento", "vigente_a_partir_de",
                                "tipo_condicao", "aplica_a_tipo_parte"})
        },
        indexes = {
                @Index(name = "idx_requisito_rito", columnList = "rito_codigo"),
                @Index(name = "idx_requisito_vigencia", columnList = "rito_codigo,vigente_a_partir_de")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequisitoDocumental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "rito_codigo", nullable = false, length = 80)
    private RitoProcessual ritoCodigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 120)
    private TipoDocumento tipoDocumento;

    @Column(name = "obrigatorio", nullable = false)
    private boolean obrigatorio;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidade", nullable = false, length = 20)
    private SeveridadeCompletude severidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonte_normativa_tipo", nullable = false, length = 40)
    private FonteNormativaTipo fonteNormativaTipo;

    @Column(name = "fonte_normativa_identificador", length = 200)
    private String fonteNormativaIdentificador;

    @Column(name = "fundamento_resumido", length = 500)
    private String fundamentoResumido;

    @Enumerated(EnumType.STRING)
    @Column(name = "grau_exigibilidade", nullable = false, length = 40)
    private GrauExigibilidade grauExigibilidade;

    @Column(name = "autoridade_origem", length = 200)
    private String autoridadeOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "aplica_a_tipo_parte", length = 60)
    private TipoParteProcessual aplicaATipoParte;

    @Enumerated(EnumType.STRING)
    @Column(name = "aplica_a_representante", length = 60)
    private TipoRepresentanteProcessual aplicaARepresentante;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_condicao", nullable = false, length = 60)
    private TipoCondicaoRequisito tipoCondicao;

    @Column(name = "condicao_parametro", length = 120)
    private String condicaoParametro;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_sensibilidade", nullable = false, length = 20)
    private NivelSigilo nivelSensibilidade;

    @Column(name = "vigente_a_partir_de", nullable = false)
    private LocalDate vigenteAPartirDe;

    @Column(name = "vigente_ate")
    private LocalDate vigenteAte;

    @Column(name = "versao", nullable = false, length = 40)
    private String versao;

    @Column(name = "imutavel", nullable = false)
    private boolean imutavel;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "criado_por")
    private Long criadoPor;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) criadoEm = OffsetDateTime.now();
    }
}
