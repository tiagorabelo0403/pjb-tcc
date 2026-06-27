package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralProcessoEntityPayloadAssembler {

    LinkedHashMap<String, Object> assemble(Processo processo, ProcessoRequest request) {
        return assemble(processo, request, List.of());
    }

    LinkedHashMap<String, Object> assemble(Processo processo, ProcessoRequest request, List<Attachment> anexos) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        if (processo != null) {
            payload.put("classe", processo.getClasseProcessual());
            payload.put("classeProcessual", processo.getClasseProcessual());
            payload.put("classeTpu", NationalProceduralRoutingSupport.firstNonBlank(processo.getClasseTpuCodigo(), processo.getClasseProcessual()));
            payload.put("classeTpuCodigo", processo.getClasseTpuCodigo());
            payload.put("classeTpuNome", processo.getClasseProcessual());
            payload.put("assunto", processo.getAssunto());
            payload.put("resumo", processo.getResumoIA());
            payload.put("objetoProcessual", processo.getObjetoProcessual());
            payload.put("pedidoPrincipal", processo.getPedidoPrincipal());
            payload.put("pedidos", processo.getPedidosConsolidados());
            payload.put("provas", processo.getMaterialProbatorioResumo());
            payload.put("materia", processo.getMateria() != null ? processo.getMateria().name() : processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
            payload.put("ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
            payload.put("rito", processo.getRito() != null ? processo.getRito().name() : null);
            payload.put("valorCausa", processo.getValorCausa());
            payload.put("parteAutoraNome", processo.getParteAutoraNome());
            payload.put("parteReuNome", processo.getParteReuNome());
            payload.put("parteAutoraCpf", processo.getParteAutoraCpf());
            payload.put("parteReuCpf", processo.getParteReuCpf());
            payload.put("ufAutor", NationalProceduralRoutingSupport.firstNonBlank(processo.getUf(), processo.getJurisdicao() != null ? processo.getJurisdicao().getEstado() : null));
            payload.put("comarcaAutor", NationalProceduralRoutingSupport.firstNonBlank(processo.getComarca(), processo.getJurisdicao() != null ? processo.getJurisdicao().getCidade() : null));
            payload.put("varaPretendida", NationalProceduralRoutingSupport.firstNonBlank(processo.getVara(), processo.getUnidadeJudiciariaCodigo()));
            payload.put("tribunalCodigo", NationalProceduralRoutingSupport.firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal(), processo.getJurisdicao() != null ? processo.getJurisdicao().getCodigo() : null));
            if (processo.getTipoJustica() != null) {
                payload.put("tipoJustica", processo.getTipoJustica().name());
            }
            if (processo.getJurisdicao() != null) {
                payload.put("tribunalCodigo", NationalProceduralRoutingSupport.firstNonBlank(NationalProceduralRoutingSupport.text(payload.get("tribunalCodigo")), processo.getJurisdicao().getCodigo()));
                payload.put("ufAutor", NationalProceduralRoutingSupport.firstNonBlank(NationalProceduralRoutingSupport.text(payload.get("ufAutor")), processo.getJurisdicao().getEstado()));
                payload.put("comarcaAutor", NationalProceduralRoutingSupport.firstNonBlank(NationalProceduralRoutingSupport.text(payload.get("comarcaAutor")), processo.getJurisdicao().getCidade()));
                payload.put("foro", processo.getJurisdicao().getNome());
                payload.put("__jurisdicaoEntity", processo.getJurisdicao());
            }
            if (processo.getDataCriacao() != null) {
                payload.put("__dataReferencia", processo.getDataCriacao().toLocalDate());
            }
        }
        if (request != null) {
            if (request.getUfAutor() != null) {
                payload.put("ufAutor", request.getUfAutor());
            }
            if (request.getComarcaAutor() != null) {
                payload.put("comarcaAutor", request.getComarcaAutor());
            }
            if (request.getUfReu() != null) {
                payload.put("ufReu", request.getUfReu());
            }
            if (request.getComarcaReu() != null) {
                payload.put("comarcaReu", request.getComarcaReu());
            }
            if (request.getTipoAcao() != null) {
                payload.put("tipoAcao", request.getTipoAcao());
            }
            if (request.getVaraPretendida() != null) {
                payload.put("varaPretendida", request.getVaraPretendida());
            }
            if (request.getTribunalPretendido() != null) {
                payload.put("tribunalCodigo", request.getTribunalPretendido());
            }
            if (request.getTipoJusticaPretendida() != null) {
                payload.put("tipoJustica", request.getTipoJusticaPretendida());
            }
        }
        List<Attachment> anexosSafe = anexos == null ? List.of() : anexos;
        List<String> tipados = anexosSafe.stream()
                .map(Attachment::getTipoDocumento)
                .filter(t -> t != null)
                .map(TipoDocumento::name)
                .toList();
        if (!tipados.isEmpty()) {
            payload.put("documentosTipados", tipados);
        }
        return payload;
    }
}
