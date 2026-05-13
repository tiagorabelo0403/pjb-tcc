package com.tcc.pjb.backend.model.entity.intelligence;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
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
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoFundamento;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.INTELIGENCIA_APLICADA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_pessoa_localizacao_consulta", indexes = {
        @Index(name = "idx_pessoa_localizacao_executor_created", columnList = "executor_user_id, created_at"),
        @Index(name = "idx_pessoa_localizacao_canal_created", columnList = "canal_consulta, created_at"),
        @Index(name = "idx_pessoa_localizacao_ref_proc", columnList = "referencia_procedimental"),
        @Index(name = "idx_pessoa_localizacao_correlation", columnList = "correlation_id", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PessoaLocalizacaoConsultaGovernada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 80)
    private String correlationId;

    @Column(name = "executor_user_id", nullable = false)
    private Long executorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "executor_tipo_usuario", nullable = false, length = 80)
    private TipoUsuario executorTipoUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_consulta", nullable = false, length = 40)
    private PessoaLocalizacaoCanalConsulta canalConsulta;

    @Enumerated(EnumType.STRING)
    @Column(name = "fundamento", nullable = false, length = 80)
    private PessoaLocalizacaoFundamento fundamento;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "mandado_id")
    private Long mandadoId;

    @Column(name = "referencia_procedimental", nullable = false, length = 160)
    private String referenciaProcedimental;

    @Column(name = "finalidade", nullable = false, length = 500)
    private String finalidade;

    @Column(name = "justificativa_operacional", nullable = false, length = 1000)
    private String justificativaOperacional;

    @Column(name = "cpf_hash", nullable = false, length = 128)
    private String cpfHash;

    @Column(name = "cpf_mascarado", nullable = false, length = 32)
    private String cpfMascarado;

    @Column(name = "possui_contexto_formal", nullable = false)
    private boolean possuiContextoFormal;

    @Column(name = "consulta_sem_processo_autorizada", nullable = false)
    private boolean consultaSemProcessoAutorizada;

    @Column(name = "endereco_estrito_solicitado", nullable = false)
    private boolean enderecoEstritoSolicitado;

    @Column(name = "endereco_estrito_liberado", nullable = false)
    private boolean enderecoEstritoLiberado;

    @Column(name = "nivel_exposicao", nullable = false, length = 32)
    private String nivelExposicao;

    @Column(name = "postura_nivel", nullable = false, length = 24)
    private String posturaNivel;

    @Column(name = "postura_score", nullable = false)
    private int posturaScore;

    @Column(name = "requer_revisao", nullable = false)
    private boolean requerRevisao;

    @Column(name = "modo_liberacao", nullable = false, length = 60)
    private String modoLiberacao;

    @Column(name = "step_up_required", nullable = false)
    private boolean stepUpRequired;

    @Column(name = "step_up_satisfied", nullable = false)
    private boolean stepUpSatisfied;

    @Column(name = "challenge_hint", length = 180)
    private String challengeHint;

    @Column(name = "fontes_consultadas", nullable = false)
    private int fontesConsultadas;

    @Column(name = "enderecos_encontrados", nullable = false)
    private int enderecosEncontrados;

    @Column(name = "restricoes_encontradas", nullable = false)
    private int restricoesEncontradas;

    @Column(name = "vinculos_encontrados", nullable = false)
    private int vinculosEncontrados;

    @Column(name = "alertas_count", nullable = false)
    private int alertasCount;

    @Column(name = "sinais_postura", nullable = false, length = 1500)
    private String sinaisPostura;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }
}
