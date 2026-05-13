package com.tcc.pjb.backend.modules.laiane.dto.roles.common;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianePrecedenteLiteDto {
    public String tema() { return getTema(); }
    private Long id;
    private String titulo;
    private String tribunal;
    private String tema;
    private String ementa;
}
