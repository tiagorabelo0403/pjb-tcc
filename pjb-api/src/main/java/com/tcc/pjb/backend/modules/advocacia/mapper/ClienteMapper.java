package com.tcc.pjb.backend.modules.advocacia.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.mapstruct.*;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.dto.ClienteDTO;
import com.tcc.pjb.backend.modules.advocacia.entity.Cliente;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = {LocalDateTime.class, StatusCliente.class}
)
public interface ClienteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "advogado", source = "advogado")
    @Mapping(target = "cpfCriptografado", ignore = true)
    @Mapping(target = "cpfHash", ignore = true)
    @Mapping(target = "nome", source = "dto.nomeCompleto")
    @Mapping(target = "nomeCompleto", ignore = true)
    @Mapping(target = "emailCriptografado", ignore = true)
    @Mapping(target = "dataCriacao", expression = "java(LocalDateTime.now())")
    @Mapping(target = "dataAtualizacao", expression = "java(LocalDateTime.now())")
    @Mapping(target = "status", expression = "java(StatusCliente.ATIVO)")
    Cliente requestParaEntidade(ClienteDTO.ClienteRequest dto, Usuario advogado);

    @Mapping(target = "nomeCompleto", expression = "java(entidade.getNome())")
    @Mapping(source = "advogado.id", target = "advogadoId")
    @Mapping(source = "advogado.nome", target = "advogadoNome")
    @Mapping(source = "cpfCriptografado", target = "cpfCnpj")
    @Mapping(source = "emailCriptografado", target = "email")
    ClienteDTO.ClienteResponse entidadeParaResponse(Cliente entidade);

    List<ClienteDTO.ClienteResponse> entidadeParaResponseLista(List<Cliente> entidades);

    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "advogado", ignore = true) 
    @Mapping(target = "cpfCriptografado", ignore = true)
    @Mapping(target = "cpfHash", ignore = true)
    @Mapping(target = "nome", source = "dto.nomeCompleto")
    @Mapping(target = "nomeCompleto", ignore = true)
    @Mapping(target = "emailCriptografado", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "versao", ignore = true)
    @Mapping(target = "dataAtualizacao", expression = "java(LocalDateTime.now())")
    void atualizarEntidadeDoRequest(ClienteDTO.ClienteRequest dto, @MappingTarget Cliente entidade);
}