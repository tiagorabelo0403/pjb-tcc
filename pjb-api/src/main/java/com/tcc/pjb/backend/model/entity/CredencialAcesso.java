package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.IDENTIDADE_SEGURANCA, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_credencial_acesso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredencialAcesso {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false)
    private String senhaHash;

    private LocalDateTime validade;

    private boolean ativa;

    @ManyToOne(optional = false)
    private Processo processo;
}
