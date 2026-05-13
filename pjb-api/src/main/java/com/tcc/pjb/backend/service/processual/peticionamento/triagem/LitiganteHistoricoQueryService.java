package com.tcc.pjb.backend.service.processual.peticionamento.triagem;

import org.springframework.stereotype.Service;

@Service
public class LitiganteHistoricoQueryService {

    public record HistoricoLitigante(
            String cpfCnpj,
            int totalProcessos,
            double taxaSucumbencia,
            int recursosProtelatoriasDetectados,
            int excecoesSistematicas
    ) {}

    public HistoricoLitigante consultar(String cpfCnpj) {
        return new HistoricoLitigante(cpfCnpj, 0, 0.0, 0, 0);
    }
}
