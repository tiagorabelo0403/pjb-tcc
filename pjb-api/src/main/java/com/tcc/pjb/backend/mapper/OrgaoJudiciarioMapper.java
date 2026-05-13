package com.tcc.pjb.backend.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import com.tcc.pjb.backend.model.dto.OrgaoJudiciarioRequest;
import com.tcc.pjb.backend.model.dto.OrgaoJudiciarioResponse;
import com.tcc.pjb.backend.model.entity.OrgaoJudiciario;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE) 
public interface OrgaoJudiciarioMapper {

    OrgaoJudiciario requestParaEntidade(OrgaoJudiciarioRequest dto);

    OrgaoJudiciarioResponse entidadeParaResponse(OrgaoJudiciario entidade);

    List<OrgaoJudiciarioResponse> entidadeParaResponseLista(List<OrgaoJudiciario> entidades);

    void atualizarEntidadeDoRequest(OrgaoJudiciarioRequest dto, @MappingTarget OrgaoJudiciario entidade);
}