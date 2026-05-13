package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@PjbDataOwnership(module = PjbModuleId.COMUNICACOES, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_chat_mensagem",
        indexes = {
                @Index(name = "idx_chat_usuario", columnList = "usuario_id"),
                @Index(name = "idx_chat_processo", columnList = "processo_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"usuario", "processo"})
@Access(AccessType.FIELD)
@EntityListeners(ChatMensagemListener.class) 
public class ChatMensagem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Conteúdo da mensagem não pode ser vazio")
    @Size(max = 5000, message = "Conteúdo excede o limite permitido")
    @Column(name = "conteudo", columnDefinition = "TEXT", nullable = false, length = 5000)
    private String conteudo;

    @CreationTimestamp
    @Column(name = "data_envio", updatable = false, nullable = false)
    private LocalDateTime dataEnvio;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_usuario"))
    private Usuario usuario;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_processo"))
    private Processo processo;

    @PreUpdate
    public void preUpdate() {
        
    }
}