package com.tcc.pjb.backend.modules.balcao.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.OWNER_ONLY)
@Entity
@Table(name = "balcao_virtual_atendimento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalcaoVirtualAtendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "protocolo", nullable = false, unique = true, length = 25)
    private String protocolo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 60)
    private TipoAtendimentoBalcao tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BalcaoAtendimentoStatus status;

    @Column(name = "solicitante_nome", nullable = false, length = 255)
    private String solicitanteNome;

    @Column(name = "solicitante_oab", length = 20)
    private String solicitanteOab;

    @Column(name = "solicitante_oab_uf", length = 2)
    private String solicitanteOabUf;

    @Column(name = "solicitante_cpf", length = 14)
    private String solicitanteCpf;

    @Column(name = "numero_processo", length = 25)
    private String numeroProcesso;

    @Column(name = "vara", length = 120)
    private String vara;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "atendido_por", length = 255)
    private String atendidoPor;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "chamado_em")
    private Instant chamadoEm;

    @Column(name = "iniciado_em")
    private Instant iniciadoEm;

    @Column(name = "encerrado_em")
    private Instant encerradoEm;
}
