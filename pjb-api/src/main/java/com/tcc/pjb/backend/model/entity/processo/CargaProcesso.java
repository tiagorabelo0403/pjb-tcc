package com.tcc.pjb.backend.model.entity.processo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

@Entity
@Table(name = "tb_carga_processo")
@Getter
public class CargaProcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Column(name = "responsavel_id", nullable = false)
    private Long responsavelId;

    @Column(name = "unidade_id", nullable = false)
    private Long unidadeId;

    @Column(name = "tipo_carga", nullable = false)
    private String tipoCarga;

    @Column(name = "data_saida", nullable = false)
    private Instant dataSaida;

    @Column(name = "data_prevista")
    private Instant dataPrevista;

    @Column(name = "data_retorno")
    private Instant dataRetorno;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "volumes", nullable = false)
    private int volumes;

    @Column(name = "observacao")
    private String observacao;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CargaProcesso() {
    }

    public CargaProcesso(Long processoId, Long responsavelId, Long unidadeId,
                          String tipoCarga, Instant dataPrevista, int volumes, String observacao) {
        this.processoId = processoId;
        this.responsavelId = responsavelId;
        this.unidadeId = unidadeId;
        this.tipoCarga = tipoCarga;
        this.dataPrevista = dataPrevista;
        this.volumes = volumes;
        this.observacao = observacao;
        this.status = "ATIVA";
        Instant now = Instant.now();
        this.dataSaida = now;
        this.createdAt = now;
    }

    public void registrarRetorno() {
        this.status = "DEVOLVIDA";
        this.dataRetorno = Instant.now();
    }

    public void registrarExtravio(String observacaoExtravio) {
        this.status = "EXTRAVIADA";
        this.observacao = observacaoExtravio;
    }

    public boolean isAtiva() {
        return "ATIVA".equals(status);
    }
}
