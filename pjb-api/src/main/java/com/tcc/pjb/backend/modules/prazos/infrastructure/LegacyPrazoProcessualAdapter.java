package com.tcc.pjb.backend.modules.prazos.infrastructure;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.modules.prazos.api.PrazoDiaForenseCommand;
import com.tcc.pjb.backend.modules.prazos.api.PrazoDiaForenseResult;
import com.tcc.pjb.backend.modules.prazos.api.PrazoProcessualCalculoCommand;
import com.tcc.pjb.backend.modules.prazos.api.PrazoProcessualCalculoResult;
import com.tcc.pjb.backend.modules.prazos.api.PrazoProcessualPort;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.service.processual.prazo.PrazoProcessualNacionalService;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class LegacyPrazoProcessualAdapter implements PrazoProcessualPort {

    private final PrazoProcessualNacionalService prazoService;

    public LegacyPrazoProcessualAdapter(PrazoProcessualNacionalService prazoService) {
        this.prazoService = Objects.requireNonNull(prazoService);
    }

    @Override
    public PrazoProcessualCalculoResult calcularPrazo(PrazoProcessualCalculoCommand command) {
        Objects.requireNonNull(command);
        var result = prazoService.calcular(new PrazoProcessualNacionalService.CalculoPrazoCommand(
                command.dataInicio(),
                enumValue(NationalPrazoEngine.TipoPrazo.class, command.tipoPrazo(), "Tipo de prazo invalido."),
                enumValue(RamoDireito.class, command.ramo(), "Ramo do direito invalido."),
                enumValue(GrauJurisdicao.class, command.grau(), "Grau de jurisdicao invalido."),
                command.tribunalCodigo(),
                command.uf(),
                command.comarca(),
                command.diasOverride()
        ));
        return new PrazoProcessualCalculoResult(
                result.dataInicio(),
                result.vencimentoNacional(),
                result.vencimentoForense(),
                result.diasCorridos(),
                result.diasUteisNacionais(),
                result.diasUteisForenses(),
                result.tipoPrazo() == null ? null : result.tipoPrazo().name(),
                result.ramo() == null ? null : result.ramo().name(),
                result.grau() == null ? null : result.grau().name(),
                result.tribunalCodigo(),
                result.uf(),
                result.comarca(),
                result.marcoInicialDiaUtil(),
                result.motivoMarcoInicial(),
                result.advertencias(),
                result.fundamentoNacional(),
                result.fundamentoForense(),
                false
        );
    }

    @Override
    public PrazoDiaForenseResult analisarDiaForense(PrazoDiaForenseCommand command) {
        Objects.requireNonNull(command);
        var result = prazoService.analisarDia(
                command.data(),
                command.tribunalCodigo(),
                command.uf(),
                command.comarca(),
                enumValue(RamoDireito.class, command.ramo(), "Ramo do direito invalido."),
                enumValue(GrauJurisdicao.class, command.grau(), "Grau de jurisdicao invalido.")
        );
        return new PrazoDiaForenseResult(
                result.data(),
                result.diaUtil(),
                result.motivo(),
                result.tipoEntrada(),
                false
        );
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(message, ex);
        }
    }
}
