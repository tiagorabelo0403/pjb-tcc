package com.tcc.pjb.backend.mapper;

import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import com.tcc.pjb.backend.model.dto.UsuarioRequest;
import com.tcc.pjb.backend.model.dto.UsuarioResponse;
import com.tcc.pjb.backend.model.entity.Usuario;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UsuarioMapper {

    @Mapping(source = "perfil", target = "perfil")
    UsuarioResponse entidadeParaResponse(Usuario entidade);

    List<UsuarioResponse> entidadeParaResponseLista(List<Usuario> entidades);

    @BeanMapping(ignoreByDefault = false)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    Usuario requestParaEntidade(UsuarioRequest dto);

    @BeanMapping(
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    void atualizarEntidadeDoRequest(UsuarioRequest dto, @MappingTarget Usuario entidade);
}