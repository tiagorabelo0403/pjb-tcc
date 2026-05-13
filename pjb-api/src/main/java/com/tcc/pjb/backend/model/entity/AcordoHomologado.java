package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "acordos_homologados")
@EntityListeners(AuditingEntityListener.class)
public class AcordoHomologado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposta_acordo_id", nullable = false, unique = true)
    private PropostaAcordo proposta;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false, unique = true)
    private Processo processo;

    @Column(name = "url_pdf_homologado", nullable = false)
    private String urlPdfHomologado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "juiz_homologador_id", nullable = false)
    private Usuario juiz;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataHomologacao;

    @Lob
    @Column(columnDefinition = "TEXT")
private String hashAssinaturaJuiz;

    @Lob
    @Column(columnDefinition = "TEXT")
private String hashAssinaturaParte1;

    @Lob
    @Column(columnDefinition = "TEXT")
private String hashAssinaturaParte2;
}