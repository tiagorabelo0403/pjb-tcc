package com.tcc.pjb.backend.core.prazos.calendario;

import java.time.Instant;
import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pjb_calendario_forense", indexes = {
        @Index(name = "idx_cal_forense_uf", columnList = "uf, comarca, dia"),
        @Index(name = "idx_cal_forense_dia", columnList = "dia")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_cal_forense", columnNames = {"uf", "comarca", "dia", "tipo"})
})
public class CalendarioForenseEntry {

    public CalendarioForenseEntry() {
    }

    public CalendarioForenseEntry(LocalDate dia, String uf, String comarca, String tipo) {
        this.dia = dia;
        this.uf = uf;
        this.comarca = comarca;
        this.tipo = tipo;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final CalendarioForenseEntry target = new CalendarioForenseEntry();
        public Builder dia(LocalDate dia) { target.setDia(dia); return this; }
        public Builder uf(String uf) { target.setUf(uf); return this; }
        public Builder comarca(String comarca) { target.setComarca(comarca); return this; }
        public Builder tipo(String tipo) { target.setTipo(tipo); return this; }
        public Builder descricao(String descricao) { target.setDescricao(descricao); return this; }
        public CalendarioForenseEntry build() { return target; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @Column(name = "uf", length = 2)
    private String uf;

    
    @Column(name = "comarca", length = 120)
    private String comarca;

    @Column(name = "dia", nullable = false)
    private LocalDate dia;

    
    @Column(name = "tipo", nullable = false, length = 32)
    private String tipo;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
