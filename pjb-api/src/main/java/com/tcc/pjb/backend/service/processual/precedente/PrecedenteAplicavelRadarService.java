package com.tcc.pjb.backend.service.processual.precedente;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PrecedenteAplicavelRadarService {

    public record RadarPrecedenteInput(
            UUID processoId,
            RitoProcessual rito,
            String resumoPedido,
            String assuntoCNJ
    ) {}

    public record AlertaPrecedente(
            UUID processoId,
            String mensagem,
            String tipoAlerta
    ) {}

    private final PrecedenteDecisionSupportAssembler assembler;

    public PrecedenteAplicavelRadarService(PrecedenteDecisionSupportAssembler assembler) {
        this.assembler = assembler;
    }

    public List<AlertaPrecedente> radar(RadarPrecedenteInput input) {
        return List.of(
                new AlertaPrecedente(input.processoId(),
                        "Há precedentes possivelmente aplicáveis para o assunto '" + input.assuntoCNJ()
                                + "' — consultar base jurídica antes de decidir.",
                        "PRECEDENTE_DISPONIVEL"));
    }
}
