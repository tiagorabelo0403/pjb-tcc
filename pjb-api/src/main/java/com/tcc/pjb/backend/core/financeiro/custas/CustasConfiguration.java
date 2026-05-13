package com.tcc.pjb.backend.core.financeiro.custas;

import com.tcc.pjb.backend.core.financeiro.custas.domain.GruResult;
import com.tcc.pjb.backend.core.financeiro.custas.domain.PixResult;
import com.tcc.pjb.backend.model.entity.Processo;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustasConfiguration {

    @Bean
    public GruCodigoBarrasGenerator gruCodigoBarrasGenerator() {
        return (tipoCusta, valor, uf) -> {
            String base = (uf == null ? "DF" : uf) + (tipoCusta == null ? "CUSTA" : tipoCusta.toUpperCase()) + valor.setScale(2, java.math.RoundingMode.HALF_UP);
            String digest = HexFormat.of().formatHex(base.getBytes(StandardCharsets.UTF_8));
            String numero = digest.substring(0, Math.min(20, digest.length())).toUpperCase();
            return new GruResult("18830", "23790.18830." + numero, "2379018830" + numero, numero);
        };
    }

    @Bean
    public PixPayloadGenerator pixPayloadGenerator() {
        return (valor, processoId, tipoCusta) -> {
            String txid = ("PJB" + processoId + (tipoCusta == null ? "CUSTA" : tipoCusta)).replaceAll("[^A-Za-z0-9]", "");
            return new PixResult("0002012636PJB." + txid + "." + valor.setScale(2, java.math.RoundingMode.HALF_UP), txid.substring(0, Math.min(35, txid.length())));
        };
    }

    @Bean
    public IsentoCustaPolicy isentoCustaPolicy() {
        return (processo, tipoCusta) -> isentoByRamo(processo);
    }

    private static com.tcc.pjb.backend.core.financeiro.custas.domain.IsencaoCustaResult isentoByRamo(Processo processo) {
        if (processo == null || processo.getRamoDireito() == null) {
            return com.tcc.pjb.backend.core.financeiro.custas.domain.IsencaoCustaResult.naoIsento();
        }
        return switch (processo.getRamoDireito()) {
            case INFANCIA_JUVENTUDE -> com.tcc.pjb.backend.core.financeiro.custas.domain.IsencaoCustaResult.isento("prioridade protetiva e gratuidade institucional");
            default -> com.tcc.pjb.backend.core.financeiro.custas.domain.IsencaoCustaResult.naoIsento();
        };
    }
}
