package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralLaianePayloadAssembler {

    LinkedHashMap<String, Object> assemble(LaianePeticaoAssistRequest request) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        if (request == null) {
            return payload;
        }
        payload.put("classe", request.getClasseTpu());
        payload.put("classeProcessual", request.getClasseTpu());
        payload.put("assunto", request.getAssuntoTpu());
        payload.put("resumo", request.getTextoFatosResumido());
        payload.put("objetoProcessual", ctxValue(request.getCtx(), "objetoProcessual"));
        payload.put("pedidoPrincipal", ctxValue(request.getCtx(), "pedidoPrincipal"));
        payload.put("pedidos", ctxValue(request.getCtx(), "pedidos"));
        payload.put("provas", NationalProceduralRoutingSupport.joinLines(request.getDocumentosAnexados()));
        payload.put("materia", NationalProceduralRoutingSupport.firstNonBlank(request.getMateriaPrincipal(), request.getRamoDireito()));
        payload.put("ramoDireito", request.getRamoDireito());
        payload.put("rito", request.getRitoSugerido());
        payload.put("valorCausa", request.getValorCausa());
        payload.put("parteAutoraCpf", request.getCpfCnpjAutor());
        payload.put("parteReuCpf", request.getCpfCnpjReu());
        payload.put("ufAutor", request.getUfAutor());
        payload.put("comarcaAutor", request.getComarcaAutor());
        payload.put("cidadeAutor", ctxValue(request.getCtx(), "cidadeAutor"));
        payload.put("foroAutor", ctxValue(request.getCtx(), "foroAutor"));
        payload.put("subsecaoJudiciariaAutor", ctxValue(request.getCtx(), "subsecaoJudiciariaAutor"));
        payload.put("ufReu", request.getUfReu());
        payload.put("comarcaReu", request.getComarcaReu());
        payload.put("cidadeReu", ctxValue(request.getCtx(), "cidadeReu"));
        payload.put("foroReu", ctxValue(request.getCtx(), "foroReu"));
        payload.put("subsecaoJudiciariaReu", ctxValue(request.getCtx(), "subsecaoJudiciariaReu"));
        payload.put("cidadeFato", ctxValue(request.getCtx(), "cidadeFato"));
        payload.put("municipioFato", ctxValue(request.getCtx(), "municipioFato"));
        payload.put("foro", ctxValue(request.getCtx(), "foroPretendido"));
        payload.put("secaoJudiciaria", ctxValue(request.getCtx(), "secaoJudiciariaPretendida"));
        payload.put("subsecaoJudiciaria", ctxValue(request.getCtx(), "subsecaoJudiciariaPretendida"));
        payload.put("circunscricao", ctxValue(request.getCtx(), "circunscricaoPretendida"));
        payload.put("tipoAcao", request.getKind());
        payload.put("tipoJustica", request.getTipoJustica());
        payload.put("requerJuizadoEspecial", request.getRequerJuizadoEspecial());
        payload.put("requerVaraEspecializada", request.getRequerVaraEspecializada());
        payload.put("casoUrgente", request.getCasoUrgente());
        payload.put("preferenciaDigital", request.getPreferenciaDigital());
        payload.put("tribunalCodigo", ctxValue(request.getCtx(), "tribunalCodigo"));
        if (request.getCtx() != null) {
            payload.putAll(request.getCtx());
        }
        return payload;
    }

    private static Object ctxValue(Map<String, Object> ctx, String key) {
        return ctx == null ? null : ctx.get(key);
    }
}
