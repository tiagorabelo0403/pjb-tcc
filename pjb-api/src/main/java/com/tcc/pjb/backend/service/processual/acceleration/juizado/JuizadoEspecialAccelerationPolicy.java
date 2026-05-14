package com.tcc.pjb.backend.service.processual.acceleration.juizado;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class JuizadoEspecialAccelerationPolicy {

    private JuizadoEspecialAccelerationPolicy() {}

    private static final BigDecimal LIMITE_JEC = new BigDecimal("40000.00");

    public record JuizadoInput(
            RitoProcessual rito,
            BigDecimal valorCausa,
            boolean audienciaUnaRealizada,
            boolean acordoTentado,
            boolean parteSemAdvogado,
            boolean cabeSentencaImediata
    ) {}

    public record AceleradorJuizado(
            String acao,
            String mensagem,
            boolean eAtoJurisdicional
    ) {}

    public List<AceleradorJuizado> sugerir(JuizadoInput input) {
        List<AceleradorJuizado> acoes = new ArrayList<>();
        if (input.valorCausa().compareTo(LIMITE_JEC) > 0) {
            acoes.add(new AceleradorJuizado(
                    "VERIFICAR_COMPETENCIA",
                    "Valor da causa excede R$40.000 — verificar competência do JEC.",
                    true));
        }
        if (!input.audienciaUnaRealizada()) {
            acoes.add(new AceleradorJuizado(
                    "DESIGNAR_AUDIENCIA_UNA",
                    "Designar audiência una de conciliação e instrução.",
                    true));
        } else if (!input.acordoTentado()) {
            acoes.add(new AceleradorJuizado(
                    "TENTAR_ACORDO",
                    "Audiência realizada sem tentativa de acordo — verificar.",
                    true));
        }
        if (input.cabeSentencaImediata()) {
            acoes.add(new AceleradorJuizado(
                    "MINUTAR_SENTENCA",
                    "Processo apto para sentença imediata.",
                    true));
        }
        return acoes;
    }
}
