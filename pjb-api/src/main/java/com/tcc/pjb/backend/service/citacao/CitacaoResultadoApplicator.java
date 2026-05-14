package com.tcc.pjb.backend.service.citacao;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CitacaoResultadoApplicator {

    public sealed interface ResultadoCitacao permits
            ResultadoCitacao.Realizada,
            ResultadoCitacao.Negativa,
            ResultadoCitacao.FugaOcultacao,
            ResultadoCitacao.EnderecoNaoEncontrado,
            ResultadoCitacao.HoraCerta {

        record Realizada(UUID processoId, LocalDate dataCitacao,
                String nomeRecebedor, String qualificacao) implements ResultadoCitacao {}

        record Negativa(UUID processoId, LocalDate dataOcorrencia,
                String motivoRecusa) implements ResultadoCitacao {}

        record FugaOcultacao(UUID processoId, LocalDate dataOcorrencia,
                String descricaoOcorrencia) implements ResultadoCitacao {}

        record EnderecoNaoEncontrado(UUID processoId, LocalDate dataOcorrencia,
                String ultimoEnderecoTentado) implements ResultadoCitacao {}

        record HoraCerta(UUID processoId, LocalDate dataCitacao,
                String horario, String vizinhoOuPorteiro) implements ResultadoCitacao {}
    }

    public record AplicacaoResultado(
            UUID processoId,
            String tipoResultado,
            boolean citacaoRealizada,
            LocalDate inicioPrazoResposta,
            String proximaProvidencia,
            Instant registradoEm
    ) {}

    public AplicacaoResultado aplicar(ResultadoCitacao resultado,
            LocalDate dataDisponivelDJe) {
        return switch (resultado) {
            case ResultadoCitacao.Realizada r -> new AplicacaoResultado(
                    r.processoId(), "REALIZADA", true,
                    dataDisponivelDJe != null ? dataDisponivelDJe.plusDays(1) : r.dataCitacao().plusDays(1),
                    "Aguardar prazo de contestação.", Instant.now());
            case ResultadoCitacao.Negativa n -> new AplicacaoResultado(
                    n.processoId(), "NEGATIVA", false, null,
                    "Informar magistrado. Verificar citação por hora certa ou edital.", Instant.now());
            case ResultadoCitacao.FugaOcultacao f -> new AplicacaoResultado(
                    f.processoId(), "FUGA_OCULTACAO", false, null,
                    "Intimar partes. Proceder com citação por hora certa (CPC, art. 252).", Instant.now());
            case ResultadoCitacao.EnderecoNaoEncontrado e -> new AplicacaoResultado(
                    e.processoId(), "ENDERECO_NAO_ENCONTRADO", false, null,
                    "Solicitar diligências para localização. Considerar edital.", Instant.now());
            case ResultadoCitacao.HoraCerta h -> new AplicacaoResultado(
                    h.processoId(), "HORA_CERTA", true,
                    h.dataCitacao().plusDays(1),
                    "Citar por hora certa concluída. Aguardar contestação.", Instant.now());
        };
    }
}
