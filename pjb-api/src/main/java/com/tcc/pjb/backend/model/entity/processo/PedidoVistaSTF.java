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
@Table(name = "tb_pedido_vista_stf")
@Getter
public class PedidoVistaSTF {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pauta_id", nullable = false)
    private Long pautaId;

    @Column(name = "ministro_id", nullable = false)
    private Long ministroId;

    @Column(name = "data_pedido", nullable = false)
    private Instant dataPedido;

    @Column(name = "data_limite", nullable = false)
    private Instant dataLimite;

    @Column(name = "data_devolucao")
    private Instant dataDevolucao;

    @Column(name = "status", nullable = false)
    private String status;

    protected PedidoVistaSTF() {
    }

    public PedidoVistaSTF(Long pautaId, Long ministroId, Instant dataLimite) {
        this.pautaId = pautaId;
        this.ministroId = ministroId;
        this.dataLimite = dataLimite;
        this.dataPedido = Instant.now();
        this.status = "PENDENTE";
    }

    public void devolver() {
        this.status = "DEVOLVIDO";
        this.dataDevolucao = Instant.now();
    }
}
