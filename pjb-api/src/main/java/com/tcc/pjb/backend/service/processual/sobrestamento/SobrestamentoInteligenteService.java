package com.tcc.pjb.backend.service.processual.sobrestamento;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SobrestamentoInteligenteService {

    public record SobrestamentoInput(
            UUID processoId,
            String numeroProcesso,
            String motivoSobrestamento,
            String processoReferenciaId,
            boolean motivoJaCessou,
            String temaRepetitivoNumero
    ) {}

    public record SobrestamentoStatus(
            UUID processoId,
            boolean sobrestado,
            boolean motivoCessou,
            String acaoSugerida
    ) {}

    public SobrestamentoStatus avaliar(SobrestamentoInput input) {
        if (input.motivoJaCessou()) {
            return new SobrestamentoStatus(input.processoId(), true, true,
                    "Motivo de sobrestamento cessado — reativar processo e verificar consequências.");
        }
        return new SobrestamentoStatus(input.processoId(), true, false,
                "Processo sobrestado — aguardar " + input.motivoSobrestamento() + ".");
    }

    public List<SobrestamentoStatus> varrer(List<SobrestamentoInput> processos) {
        return processos.stream().map(this::avaliar).toList();
    }
}
