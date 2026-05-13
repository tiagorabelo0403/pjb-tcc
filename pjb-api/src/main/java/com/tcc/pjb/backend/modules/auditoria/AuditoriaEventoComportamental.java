package com.tcc.pjb.backend.modules.auditoria;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "auditoria_eventos")
public class AuditoriaEventoComportamental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, length = 120)
    private String acao;

    @Column(nullable = false)
    private Long usuarioId;

    
    @Column(name = "referencia_id", length = 120)
    private String referenciaId;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    
    @Column(columnDefinition = "TEXT")
    private String justificativa;

    @Column(length = 128)
    private String hashIntegridade;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private double nivelRisco;

    @Column(length = 80)
    private String perfilComportamental;

    @PrePersist
    void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();
        if (timestamp == null) timestamp = LocalDateTime.now();
        
    }
}
