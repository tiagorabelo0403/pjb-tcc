package com.tcc.pjb.backend.service.publicacao;

import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CienciaPublicacaoService {

    public record CienciaInput(
            UUID processoId,
            UUID publicacaoId,
            String parteId,
            String advogadoId,
            LocalDate dataDisponibilizacao,
            MeioPublicacao meio
    ) {}

    public record CienciaRegistrada(
            UUID processoId,
            UUID publicacaoId,
            String parteId,
            LocalDate dataDisponibilizacao,
            LocalDate inicioPrazo,
            Instant registradaEm,
            String observacao
    ) {}

    private final CalendarioUteisService calendarioService;

    public CienciaPublicacaoService(CalendarioUteisService calendarioService) {
        this.calendarioService = calendarioService;
    }

    public CienciaRegistrada registrar(CienciaInput input,
            CalendarioUteisService.CalendarioInput cal) {
        LocalDate inicio = calcularInicioPrazo(input, cal);
        String obs = "Ciência registrada via " + input.meio().name()
                + ". Prazo inicia em " + inicio + ".";
        return new CienciaRegistrada(
                input.processoId(), input.publicacaoId(),
                input.parteId(), input.dataDisponibilizacao(),
                inicio, Instant.now(), obs
        );
    }

    private LocalDate calcularInicioPrazo(CienciaInput input,
            CalendarioUteisService.CalendarioInput cal) {
        LocalDate base = input.dataDisponibilizacao().plusDays(1);
        return calendarioService.proximoDiaUtil(base, cal);
    }
}
