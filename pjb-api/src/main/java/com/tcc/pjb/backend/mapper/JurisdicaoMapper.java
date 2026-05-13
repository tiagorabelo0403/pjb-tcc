package com.tcc.pjb.backend.mapper;

import java.util.List;
import org.mapstruct.*;
import com.tcc.pjb.backend.model.dto.JurisdicaoRequest;
import com.tcc.pjb.backend.model.dto.JurisdicaoResponse;
import com.tcc.pjb.backend.model.entity.Jurisdicao;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE 
)
public interface JurisdicaoMapper {

    
    
    

    
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", constant = "true") 
    @Mapping(target = "dataCriacao", ignore = true)     
    @Mapping(target = "dataAtualizacao", ignore = true) 

    
    
    Jurisdicao toEntity(JurisdicaoRequest dto);

    
    
    

    
    
    JurisdicaoResponse toResponse(Jurisdicao entidade);

    
    
    List<JurisdicaoResponse> toResponseList(List<Jurisdicao> entidades);

    
    
    

    
    
    @Mapping(target = "id", ignore = true)      
    @Mapping(target = "codigo", ignore = true)  
    @Mapping(target = "sigla", ignore = true)   
    @Mapping(target = "dataCriacao", ignore = true)
    void updateEntityFromDto(JurisdicaoRequest dto, @MappingTarget Jurisdicao entity);
}