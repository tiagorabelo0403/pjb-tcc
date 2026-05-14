package com.tcc.pjb.backend.service.recurso;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecursoDesertaoRadarService {

    public record DesertaoInput(
            UUID processoId,
            RecursoProcessualTipo tipo,
            boolean preparoLocalizado,
            BigDecimal valorPreparo,
            boolean isencaoGratuidade,
            boolean fazendaPublica,
            boolean ministerioPublico,
            boolean defensoriaPublica
    ) {}

    public record DesertaoAnalise(
            boolean emRiscoDeDesercao,
            boolean isento,
            String motivoIsencao,
            String diagnostico
    ) {}

    public DesertaoAnalise analisar(DesertaoInput input) {
        if (!input.tipo().exigePreparo()) {
            return new DesertaoAnalise(false, true,
                    "Tipo de recurso não exige preparo (art. 1.007, §1º, CPC).", "Isento por lei.");
        }
        if (input.fazendaPublica()) {
            return new DesertaoAnalise(false, true,
                    "Fazenda Pública isenta de preparo (art. 1.007, §1º, CPC).", "Isento.");
        }
        if (input.ministerioPublico() || input.defensoriaPublica()) {
            return new DesertaoAnalise(false, true,
                    "Ministério Público e Defensoria Pública isentos de preparo.", "Isento.");
        }
        if (input.isencaoGratuidade()) {
            return new DesertaoAnalise(false, true,
                    "Beneficiário de gratuidade da justiça — isento de preparo (art. 98, §1º, CPC).",
                    "Isento por gratuidade.");
        }
        if (input.preparoLocalizado()) {
            return new DesertaoAnalise(false, false, null, "Preparo localizado nos autos.");
        }
        return new DesertaoAnalise(true, false, null,
                "Recurso pode estar deserto: preparo não localizado nos autos (art. 1.007, CPC). "
                        + "Verificar comprovante de recolhimento ou complementação.");
    }
}
