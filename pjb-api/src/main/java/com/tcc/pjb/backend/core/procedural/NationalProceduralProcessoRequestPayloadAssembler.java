package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralProcessoRequestPayloadAssembler {

    LinkedHashMap<String, Object> assemble(ProcessoRequest request) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        if (request == null) {
            return payload;
        }
        payload.put("classe", request.getClasse());
        payload.put("classeProcessual", request.getClasse());
        payload.put("assunto", request.getAssunto());
        payload.put("resumo", request.getResumoFatico());
        payload.put("objetoProcessual", request.getObjetoProcessual());
        payload.put("pedidoPrincipal", request.getPedidoPrincipal());
        payload.put("pedidos", NationalProceduralRoutingSupport.joinLines(request.getPedidos()));
        payload.put("provas", NationalProceduralRoutingSupport.joinLines(request.getProvas()));
        payload.put("materia", request.getMateria());
        payload.put("ramoDireito", request.getMateria());
        payload.put("rito", request.getRito());
        payload.put("valorCausa", request.getValorCausa());
        payload.put("parteAutoraNome", request.getParteAutoraNome());
        payload.put("parteReuNome", request.getParteReuNome());
        payload.put("parteAutoraCpf", request.getParteAutoraCpf());
        payload.put("parteReuCpf", request.getParteReuCpf());
        payload.put("ufAutor", request.getUfAutor());
        payload.put("comarcaAutor", request.getComarcaAutor());
        payload.put("cidadeAutor", request.getCidadeAutor());
        payload.put("foroAutor", request.getForoAutor());
        payload.put("subsecaoJudiciariaAutor", request.getSubsecaoJudiciariaAutor());
        payload.put("ufReu", request.getUfReu());
        payload.put("comarcaReu", request.getComarcaReu());
        payload.put("cidadeReu", request.getCidadeReu());
        payload.put("foroReu", request.getForoReu());
        payload.put("subsecaoJudiciariaReu", request.getSubsecaoJudiciariaReu());
        payload.put("cidadeFato", request.getCidadeFato());
        payload.put("municipioFato", request.getMunicipioFato());
        payload.put("foro", request.getForoPretendido());
        payload.put("secaoJudiciaria", request.getSecaoJudiciariaPretendida());
        payload.put("subsecaoJudiciaria", request.getSubsecaoJudiciariaPretendida());
        payload.put("circunscricao", request.getCircunscricaoPretendida());
        payload.put("tipoAcao", request.getTipoAcao());
        payload.put("varaPretendida", request.getVaraPretendida());
        payload.put("tribunalCodigo", request.getTribunalPretendido());
        payload.put("tipoJustica", request.getTipoJusticaPretendida());
        payload.put("envolveMenor", request.isEnvolveMenor());
        payload.put("envolveSaude", request.isEnvolveSaude());
        return payload;
    }
}
