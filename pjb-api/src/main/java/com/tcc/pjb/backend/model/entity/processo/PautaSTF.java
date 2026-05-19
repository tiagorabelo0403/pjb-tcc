package com.tcc.pjb.backend.model.entity.processo;

import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorSTF;
import com.tcc.pjb.backend.model.entity.enums.ResultadoVotacaoSTF;
import com.tcc.pjb.backend.model.entity.enums.TipoSessaoSTF;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

@Entity
@Table(name = "tb_pauta_stf")
@Getter
public class PautaSTF {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id", nullable = false)
    private Long processoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "orgao_julgador", nullable = false)
    private OrgaoJulgadorSTF orgaoJulgador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_sessao", nullable = false)
    private TipoSessaoSTF tipoSessao;

    @Column(name = "data_pauta", nullable = false)
    private LocalDate dataPauta;

    @Column(name = "relator_id", nullable = false)
    private Long relatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado")
    private ResultadoVotacaoSTF resultado;

    @Column(name = "votos_favoraveis")
    private int votosFavoraveis;

    @Column(name = "votos_contrarios")
    private int votosContrarios;

    @Column(name = "votos_vista")
    private int votosVista;

    @Column(name = "ministros_impedidos")
    private int ministrosImpedidos;

    @Column(name = "data_julgamento")
    private Instant dataJulgamento;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PautaSTF() {
    }

    public PautaSTF(Long processoId, OrgaoJulgadorSTF orgaoJulgador,
                    TipoSessaoSTF tipoSessao, LocalDate dataPauta, Long relatorId) {
        this.processoId = processoId;
        this.orgaoJulgador = orgaoJulgador;
        this.tipoSessao = tipoSessao;
        this.dataPauta = dataPauta;
        this.relatorId = relatorId;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void registrarVotacao(int votosFavoraveis, int votosContrarios,
                                  int votosVista, int ministrosImpedidos,
                                  ResultadoVotacaoSTF resultado) {
        this.votosFavoraveis = votosFavoraveis;
        this.votosContrarios = votosContrarios;
        this.votosVista = votosVista;
        this.ministrosImpedidos = ministrosImpedidos;
        this.resultado = resultado;
        this.dataJulgamento = Instant.now();
        this.updatedAt = Instant.now();
    }
}
