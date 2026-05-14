package com.tcc.pjb.backend.service.audiencia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PautaJudicialService {

    public record SlotPauta(
            UUID slotId,
            LocalDateTime inicio,
            LocalDateTime fim,
            AudienciaTipo tipo,
            boolean disponivel,
            boolean virtual,
            String salaOuLink
    ) {}

    public record PautaConsulta(
            String varaId,
            LocalDate dataInicio,
            LocalDate dataFim,
            AudienciaTipo tipoFiltro
    ) {}

    public record PautaDisponivel(
            List<SlotPauta> slots,
            int totalDisponivel,
            int totalOcupado,
            LocalDate data
    ) {}

    public record DesignacaoRequest(
            UUID processoId,
            UUID slotId,
            AudienciaTipo tipo,
            List<String> participantes,
            boolean virtual
    ) {}

    public record DesignacaoResult(
            boolean sucesso,
            UUID audienciaId,
            LocalDateTime dataHora,
            String salaOuLink,
            String pendencia
    ) {}

    public PautaDisponivel consultarPauta(PautaConsulta consulta, List<SlotPauta> slots) {
        List<SlotPauta> filtrados = slots.stream()
                .filter(s -> !s.inicio().toLocalDate().isBefore(consulta.dataInicio()))
                .filter(s -> !s.inicio().toLocalDate().isAfter(consulta.dataFim()))
                .filter(s -> consulta.tipoFiltro() == null || s.tipo() == consulta.tipoFiltro())
                .toList();

        long disponiveis = filtrados.stream().filter(SlotPauta::disponivel).count();
        return new PautaDisponivel(
                filtrados,
                (int) disponiveis,
                filtrados.size() - (int) disponiveis,
                consulta.dataInicio());
    }

    public DesignacaoResult designar(DesignacaoRequest req, SlotPauta slot) {
        if (!slot.disponivel()) {
            return new DesignacaoResult(false, null, null, null, "Slot indisponível — já ocupado.");
        }
        if (req.virtual() && !slot.tipo().admiteVirtual()) {
            return new DesignacaoResult(false, null, null, null,
                    "Tipo de audiência não admite modalidade virtual.");
        }
        return new DesignacaoResult(
                true,
                UUID.randomUUID(),
                slot.inicio(),
                slot.salaOuLink(),
                null);
    }
}
