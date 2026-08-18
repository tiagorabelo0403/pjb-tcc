package com.tcc.pjb.backend.model.entity.comunicacao;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CanalCiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusCiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoCienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicaoCiencia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "pjb_ciencia_processual",
        indexes = {
                @Index(name = "idx_ciencia_processo_status",
                        columnList = "processo_id, status"),
                @Index(name = "idx_ciencia_usuario_pendente",
                        columnList = "usuario_id, data_expiracao")
        }
)
@Getter
@NoArgsConstructor
public class CienciaProcessual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ciencia", nullable = false, length = 50)
    private TipoCienciaProcessual tipoCiencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 30)
    private CanalCiencia canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private StatusCiencia status;

    @Enumerated(EnumType.STRING)
    @Column(name = "grau_jurisdicao", nullable = false, length = 15)
    private GrauJurisdicaoCiencia grauJurisdicao;

    @Column(name = "rito_processual", length = 50)
    private String ritoProcessual;

    @Column(name = "instancia_tribunal", length = 20)
    private String instanciaTribunal;

    @Column(name = "polo_processual", length = 15)
    private String poloProcessual;

    @Column(name = "recurso_origem_id")
    private Long recursoOrigemId;

    @Column(name = "ato_processual_id")
    private Long atoProcessualId;

    @Column(name = "numero_processo", nullable = false, length = 50)
    private String numeroProcesso;

    @Column(name = "numero_edicao_dje", length = 20)
    private String numeroEdicaoDje;

    @Column(name = "hash_conteudo", nullable = false, length = 64)
    private String hashConteudo;

    @Column(name = "comprovante_hash", length = 64)
    private String comprovanteHash;

    @Column(name = "data_disponibilizacao", nullable = false)
    private Instant dataDisponibilizacao;

    @Column(name = "data_publicacao_dje")
    private Instant dataPublicacaoDje;

    @Column(name = "dies_a_quo")
    private Instant diesAQuo;

    @Column(name = "data_ciencia")
    private Instant dataCiencia;

    @Column(name = "data_expiracao")
    private Instant dataExpiracao;

    @Column(name = "prazo_resposta_dias", nullable = false)
    private short prazoRespostaDias;

    @Column(name = "em_dias_uteis", nullable = false)
    private boolean emDiasUteis;

    @Column(name = "ip_confirmacao", length = 45)
    private String ipConfirmacao;

    @Column(name = "user_agent_hash", length = 64)
    private String userAgentHash;

    @Column(name = "session_token_hash", length = 64)
    private String sessionTokenHash;

    @Column(name = "audit_trail_id")
    private Long auditTrailId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CienciaProcessual(
            Processo processo,
            Usuario usuario,
            TipoCienciaProcessual tipoCiencia,
            CanalCiencia canal,
            GrauJurisdicaoCiencia grauJurisdicao,
            String ritoProcessual,
            String instanciaTribunal,
            String poloProcessual,
            Long recursoOrigemId,
            Long atoProcessualId,
            String numeroProcesso,
            String numeroEdicaoDje,
            String hashConteudo,
            Instant dataDisponibilizacao,
            Instant dataPublicacaoDje,
            Instant diesAQuo) {
        this.processo = processo;
        this.usuario = usuario;
        this.tipoCiencia = tipoCiencia;
        this.canal = canal;
        this.grauJurisdicao = grauJurisdicao;
        this.ritoProcessual = ritoProcessual;
        this.instanciaTribunal = instanciaTribunal;
        this.poloProcessual = poloProcessual;
        this.recursoOrigemId = recursoOrigemId;
        this.atoProcessualId = atoProcessualId;
        this.numeroProcesso = numeroProcesso;
        this.numeroEdicaoDje = numeroEdicaoDje;
        this.hashConteudo = hashConteudo;
        this.dataDisponibilizacao = dataDisponibilizacao;
        this.dataPublicacaoDje = dataPublicacaoDje;
        this.diesAQuo = diesAQuo;
        this.status = StatusCiencia.PENDENTE;
        this.prazoRespostaDias = (short) tipoCiencia.prazoRespostaDias();
        this.emDiasUteis = tipoCiencia.emDiasUteis();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

        if (tipoCiencia.isPrazoFictaAplicavel() && diesAQuo != null) {
            if (tipoCiencia.prazoEmHoras()) {
                this.dataExpiracao = diesAQuo.plus(tipoCiencia.prazoRespostaDias(), ChronoUnit.HOURS);
            } else {
                this.dataExpiracao = diesAQuo.plus(tipoCiencia.prazoRespostaDias(), ChronoUnit.DAYS);
            }
        }
    }

    public void confirmar(String ip, String userAgentHash,
                          String sessionTokenHash, Instant agora) {
        if (!isPendente()) {
            throw new IllegalStateException("Ciência não está pendente: " + status);
        }
        this.status = StatusCiencia.CONFIRMADA;
        this.dataCiencia = agora;
        this.ipConfirmacao = ip;
        this.userAgentHash = userAgentHash;
        this.sessionTokenHash = sessionTokenHash;
        this.comprovanteHash = Hashes.sha256Hex(
                processo.getId() + "#" + usuario.getId() + "#"
                        + tipoCiencia.name() + "#" + hashConteudo
                        + "#" + agora.toEpochMilli()
        );
        this.updatedAt = agora;
    }

    public void expirar(Instant agora) {
        if (!isPendente()) return;
        if (dataExpiracao == null || !agora.isAfter(dataExpiracao)) return;
        this.status = StatusCiencia.FICTA_CONFIRMADA;
        this.dataCiencia = dataExpiracao;
        this.updatedAt = agora;
    }

    public void cancelar() {
        if (!isPendente()) {
            throw new IllegalStateException("Ciência não está pendente: " + status);
        }
        this.status = StatusCiencia.CANCELADA;
        this.updatedAt = Instant.now();
    }

    public boolean isPendente() {
        return status == StatusCiencia.PENDENTE;
    }

    public boolean isConfirmada() {
        return status == StatusCiencia.CONFIRMADA;
    }

    public boolean isFicta() {
        return status == StatusCiencia.FICTA_CONFIRMADA;
    }

    public boolean isTerminal() {
        return status.isTerminal();
    }

    public long diasRestantes(Instant agora) {
        if (dataExpiracao == null) return 0L;
        if (agora.isAfter(dataExpiracao)) return 0L;
        return ChronoUnit.DAYS.between(agora, dataExpiracao);
    }
}
