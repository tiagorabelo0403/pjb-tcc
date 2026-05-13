package com.tcc.pjb.backend.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.tcc.pjb.backend.model.dto.ChatMensagemResponse;
import com.tcc.pjb.backend.model.entity.ChatMensagem;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE, 
        uses = {UsuarioMapper.class})
public interface ChatMensagemMapper {

    @Mapping(source = "processo.id", target = "processoId")
    @Mapping(source = "usuario.nome", target = "nomeUsuario")
    @Mapping(source = "usuario.perfil", target = "perfilUsuario")
    ChatMensagemResponse entidadeParaResponse(ChatMensagem entidade);

    List<ChatMensagemResponse> entidadeParaResponseLista(List<ChatMensagem> entidades);
}