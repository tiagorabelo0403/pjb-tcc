package com.tcc.pjb.backend.modules.laiane.dto.legal;

import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianePlaybookItemDto {
    private String id;
    private String role;
    private String title;
    private String description;
    private List<String> tags;
    private String priority;
    private Integer score;
}
