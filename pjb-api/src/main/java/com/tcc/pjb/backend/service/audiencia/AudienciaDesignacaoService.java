package com.tcc.pjb.backend.service.audiencia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AudienciaDesignacaoService {

    public record DesignacaoInput(
            UUID processoId,
            AudienciaTipo tipo,
            LocalDateTime dataHoraSolicitada,
            boolean partesSemAdvogado,
            boolean menorEnvolvido,
            boolean virtualSolicitada,
            boolean juizoLeigo,
            boolean composicaoColegiada,
            List<String> participantesRequeridos
    ) {}

    public record DesignacaoAnalise(
            boolean designavel,
            AudienciaTipo tipoDefinitivo,
            boolean deveSerVirtual,
            List<String> restricoes,
            String fundamentacao
    ) {}

    public DesignacaoAnalise analisar(DesignacaoInput input) {
        List<String> restricoes = new ArrayList<>();
        boolean virtual = input.virtualSolicitada();

        if (input.menorEnvolvido() && input.tipo().exigePresencaFisica()) {
            restricoes.add("Menor envolvido exige presença física — audiência não pode ser virtual.");
            virtual = false;
        }
        if (input.virtualSolicitada() && !input.tipo().admiteVirtual()) {
            restricoes.add("Tipo de audiência não admite modalidade virtual.");
            virtual = false;
        }
        if (input.partesSemAdvogado() && input.tipo() == AudienciaTipo.INSTRUCAO_E_JULGAMENTO) {
            restricoes.add("Partes sem advogado — verificar necessidade de curador especial (art. 72, CPC).");
        }
        if (input.tipo() == AudienciaTipo.JULGAMENTO_COLEGIADO && !input.composicaoColegiada()) {
            restricoes.add("Julgamento colegiado requer composição mínima do colegiado.");
        }
        if (input.participantesRequeridos().isEmpty()) {
            restricoes.add("Nenhum participante informado para a pauta.");
        }

        boolean bloqueante = restricoes.stream()
                .anyMatch(r -> r.contains("exige presença física") || r.contains("não admite modalidade virtual"));
        boolean designavel = !bloqueante;

        String fundamentacao = designavel
                ? input.tipo() + " — designável" + (virtual ? " em modalidade virtual." : " presencialmente.")
                : input.tipo() + " — impedimentos formais identificados.";

        return new DesignacaoAnalise(designavel, input.tipo(), virtual,
                List.copyOf(restricoes), fundamentacao);
    }
}
