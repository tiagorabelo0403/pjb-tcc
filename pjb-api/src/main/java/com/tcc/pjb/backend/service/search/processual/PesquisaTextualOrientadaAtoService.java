package com.tcc.pjb.backend.service.search.processual;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PesquisaTextualOrientadaAtoService {

    public record PesquisaInput(
            String consulta,
            RitoProcessual ritoFiltro,
            String operadorId,
            boolean temAcessoSigiloso
    ) {}

    public record ResultadoPesquisa(
            UUID processoId,
            String numeroProcesso,
            String resumo,
            double relevancia,
            boolean sigiloso
    ) {}

    public List<ResultadoPesquisa> pesquisar(PesquisaInput input) {
        if (input.consulta() == null || input.consulta().isBlank()) return List.of();
        return List.of();
    }

    public String normalizarConsulta(String consulta) {
        if (consulta == null) return "";
        return consulta.trim().toLowerCase()
                .replace("como", "")
                .replace("preciso", "")
                .replace("quero", "")
                .trim();
    }

    public boolean consultaEnvolveAtoJurisdicional(String consulta) {
        if (consulta == null) return false;
        String lower = consulta.toLowerCase();
        return lower.contains("sentença") || lower.contains("decisão")
                || lower.contains("despacho") || lower.contains("acórdão");
    }
}
