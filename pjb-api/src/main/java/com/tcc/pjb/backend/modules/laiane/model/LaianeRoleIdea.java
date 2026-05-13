package com.tcc.pjb.backend.modules.laiane.model;

import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeRoleIdea {
    private String id;
    private String role;
    private String title;
    private String description;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    private String priority;
}
