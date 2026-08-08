package com.tcc.pjb.backend.modules.suporte.entity;

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
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.OWNER_ONLY)
@Entity
@Table(name = "support_ticket")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aberto_por_id", nullable = false)
    private Long abertoPorId;

    @Column(name = "aberto_por_nome", nullable = false, length = 255)
    private String abertoPorNome;

    @Column(name = "aberto_por_tipo_usuario", length = 60)
    private String abertoPorTipoUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 60)
    private SupportTicketCategoria categoria;

    @Column(name = "assunto", nullable = false, length = 255)
    private String assunto;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SupportTicketStatus status;

    @Column(name = "atendido_por_id")
    private Long atendidoPorId;

    @Column(name = "atendido_por_nome", length = 255)
    private String atendidoPorNome;

    @Column(name = "resposta", columnDefinition = "TEXT")
    private String resposta;

    @Column(name = "viagem_uf_ou_pais_destino", length = 80)
    private String viagemUfOuPaisDestino;

    @Column(name = "viagem_data_inicio")
    private LocalDate viagemDataInicio;

    @Column(name = "viagem_data_fim")
    private LocalDate viagemDataFim;

    @Column(name = "viagem_justificativa", columnDefinition = "TEXT")
    private String viagemJustificativa;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atendido_em")
    private Instant atendidoEm;

    @Column(name = "resolvido_em")
    private Instant resolvidoEm;
}
