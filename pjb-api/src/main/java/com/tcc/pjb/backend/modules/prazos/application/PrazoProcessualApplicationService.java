package com.tcc.pjb.backend.modules.prazos.application;

import com.tcc.pjb.backend.modules.prazos.api.PrazoDiaForenseCommand;
import com.tcc.pjb.backend.modules.prazos.api.PrazoDiaForenseResult;
import com.tcc.pjb.backend.modules.prazos.api.PrazoProcessualCalculoCommand;
import com.tcc.pjb.backend.modules.prazos.api.PrazoProcessualCalculoResult;
import com.tcc.pjb.backend.modules.prazos.api.PrazoProcessualPort;
import com.tcc.pjb.backend.modules.prazos.domain.PrazoProcessualBoundaryPolicy;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class PrazoProcessualApplicationService {

    private final PrazoProcessualPort port;
    private final PrazoProcessualBoundaryPolicy policy;

    @Autowired

    public PrazoProcessualApplicationService(PrazoProcessualPort port) {
        this(port, new PrazoProcessualBoundaryPolicy());
    }

    PrazoProcessualApplicationService(PrazoProcessualPort port,
                                      PrazoProcessualBoundaryPolicy policy) {
        this.port = Objects.requireNonNull(port);
        this.policy = Objects.requireNonNull(policy);
    }

    @Transactional(readOnly = true)
    public PrazoProcessualCalculoResult calcularPrazo(PrazoProcessualCalculoCommand command) {
        Objects.requireNonNull(command);
        var parametros = policy.validarCalculo(
                command.dataInicio(),
                command.tipoPrazo(),
                command.ramo(),
                command.grau(),
                command.tribunalCodigo(),
                command.uf(),
                command.comarca(),
                command.diasOverride()
        );
        var normalizado = new PrazoProcessualCalculoCommand(
                parametros.data(),
                parametros.tipoPrazo(),
                parametros.ramo(),
                parametros.grau(),
                parametros.tribunalCodigo(),
                parametros.uf(),
                parametros.comarca(),
                parametros.diasOverride()
        );
        var result = port.calcularPrazo(normalizado);
        boolean conferencia = policy.exigeConferenciaManual(parametros, result.marcoInicialDiaUtil(), result.advertencias());
        return result.comConferenciaManual(conferencia);
    }

    @Transactional(readOnly = true)
    public PrazoDiaForenseResult analisarDiaForense(PrazoDiaForenseCommand command) {
        Objects.requireNonNull(command);
        var parametros = policy.validarDiaForense(
                command.data(),
                command.tribunalCodigo(),
                command.uf(),
                command.comarca(),
                command.ramo(),
                command.grau()
        );
        var normalizado = new PrazoDiaForenseCommand(
                parametros.data(),
                parametros.tribunalCodigo(),
                parametros.uf(),
                parametros.comarca(),
                parametros.ramo(),
                parametros.grau()
        );
        var result = port.analisarDiaForense(normalizado);
        boolean conferencia = policy.exigeConferenciaManualDiaForense(parametros, result.diaUtil(), result.motivo());
        return result.comConferenciaManual(conferencia);
    }
}
