package com.tcc.pjb.backend.model.dto.catalog;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdeaDto {

    
    private String role;

    
    @JsonProperty("id")
    @JsonAlias({"code"})
    private String id;

    
    private String title;

    
    private String description;

    
    private List<String> tags;

    
    private String priority;
}
