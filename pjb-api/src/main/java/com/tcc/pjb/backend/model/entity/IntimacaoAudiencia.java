package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;
import com.tcc.pjb.backend.model.entity.enums.StatusIntimacaoAudiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoDestinatarioIntimacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.OWNER_ONLY)
@Entity
@Table(name = "intimacao_audiencia")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntimacaoAudiencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audiencia_id", nullable = false)
    private Audiencia audiencia;

    @Column(name = "destinatario_nome", nullable = false, length = 255)
    private String destinatarioNome;

    @Enumerated(EnumType.STRING)
    @Column(name = "destinatario_tipo", nullable = false, length = 60)
    private TipoDestinatarioIntimacao destinatarioTipo;

    @Column(name = "destinatario_oab", length = 20)
    private String destinatarioOab;

    @Column(name = "destinatario_email", length = 255)
    private String destinatarioEmail;

    @Column(name = "canal", nullable = false, length = 50)
    private String canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StatusIntimacaoAudiencia status;

    @Column(name = "prazo_ciencia")
    private Instant prazoCiencia;

    @Column(name = "enviada_em")
    private Instant enviadaEm;

    @Column(name = "ciencia_em")
    private Instant cienciaEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
