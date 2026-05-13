package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeLawyerTeseResponse {
    private Long id;
    private String area;
    private String titulo;
    private String corpo;
    private String tagsJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
