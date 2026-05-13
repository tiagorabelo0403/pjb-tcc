package com.tcc.pjb.backend.service.processual.prazo;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;

@Service
public class PrazoProcessualNacionalService {

    private final NationalPrazoEngine nationalPrazoEngine;
    private final CalendarioForenseTribunalService calendarioForenseTribunalService;

    public PrazoProcessualNacionalService(NationalPrazoEngine nationalPrazoEngine,
                                          CalendarioForenseTribunalService calendarioForenseTribunalService) {
        this.nationalPrazoEngine = Objects.requireNonNull(nationalPrazoEngine);
        this.calendarioForenseTribunalService = Objects.requireNonNull(calendarioForenseTribunalService);
    }

    public PrazoProcessualResult calcular(CalculoPrazoCommand command) {
        Objects.requireNonNull(command);
        if (command.dataInicio() == null) {
            throw new IllegalArgumentException("Data inicial obrigatória.");
        }
        if (command.tipoPrazo() == null) {
            throw new IllegalArgumentException("Tipo de prazo obrigatório.");
        }
        var nacional = nationalPrazoEngine.calcular(
                command.dataInicio(),
                command.tipoPrazo(),
                command.ramo(),
                command.grau(),
                command.tribunalCodigo()
        );
        int diasForenses = command.diasOverride() == null ? Math.max(1, nacional.diasUteis()) : command.diasOverride();
        var forense = calendarioForenseTribunalService.calcularPrazo(
                command.dataInicio(),
                diasForenses,
                command.tribunalCodigo(),
                command.uf(),
                command.comarca()
        );
        var diaBase = calendarioForenseTribunalService.analisarDia(
                new CalendarioForenseTribunalService.ContextoCalendario(
                        command.tribunalCodigo(),
                        command.uf(),
                        command.comarca(),
                        command.ramo(),
                        command.grau(),
                        command.dataInicio()
                ),
                command.dataInicio()
        );
        LinkedHashSet<String> advertencias = new LinkedHashSet<>(nacional.advertencias());
        advertencias.add(forense.fundamentacao());
        if (!diaBase.diaUtil()) {
            advertencias.add("Marco inicial em dia não útil forense: " + diaBase.motivo());
        }
        return new PrazoProcessualResult(
                command.dataInicio(),
                nacional.vencimento(),
                forense.dataVencimento(),
                nacional.diasCorridos(),
                nacional.diasUteis(),
                forense.diasUteisContratados(),
                command.tipoPrazo(),
                command.ramo(),
                command.grau(),
                command.tribunalCodigo(),
                command.uf(),
                command.comarca(),
                diaBase.diaUtil(),
                diaBase.motivo(),
                List.copyOf(advertencias),
                nacional.fundamentoLegal(),
                forense.fundamentacao()
        );
    }

    public DiaForenseResult analisarDia(LocalDate data,
                                        String tribunalCodigo,
                                        String uf,
                                        String comarca,
                                        RamoDireito ramo,
                                        GrauJurisdicao grau) {
        var dia = calendarioForenseTribunalService.analisarDia(
                new CalendarioForenseTribunalService.ContextoCalendario(tribunalCodigo, uf, comarca, ramo, grau, data),
                data
        );
        return new DiaForenseResult(data, dia.diaUtil(), dia.motivo(), dia.tipoEntrada() == null ? null : dia.tipoEntrada().name());
    }

    public record CalculoPrazoCommand(
            LocalDate dataInicio,
            NationalPrazoEngine.TipoPrazo tipoPrazo,
            RamoDireito ramo,
            GrauJurisdicao grau,
            String tribunalCodigo,
            String uf,
            String comarca,
            Integer diasOverride) {
    }

    public record PrazoProcessualResult(
            LocalDate dataInicio,
            LocalDate vencimentoNacional,
            LocalDate vencimentoForense,
            int diasCorridos,
            int diasUteisNacionais,
            int diasUteisForenses,
            NationalPrazoEngine.TipoPrazo tipoPrazo,
            RamoDireito ramo,
            GrauJurisdicao grau,
            String tribunalCodigo,
            String uf,
            String comarca,
            boolean marcoInicialDiaUtil,
            String motivoMarcoInicial,
            List<String> advertencias,
            String fundamentoNacional,
            String fundamentoForense) {
    }

    public record DiaForenseResult(
            LocalDate data,
            boolean diaUtil,
            String motivo,
            String tipoEntrada) {
    }
}
