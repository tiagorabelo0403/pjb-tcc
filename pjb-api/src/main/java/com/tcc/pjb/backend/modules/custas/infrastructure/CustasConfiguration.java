package com.tcc.pjb.backend.modules.custas.infrastructure;

import com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator;
import com.tcc.pjb.backend.modules.custas.domain.CustaIsencaoPolicy;
import com.tcc.pjb.backend.modules.custas.domain.CustaIsencaoPorRitoPolicy;
import com.tcc.pjb.backend.modules.custas.domain.GruResult;
import com.tcc.pjb.backend.modules.custas.domain.PixPayloadGenerator;
import com.tcc.pjb.backend.modules.custas.domain.PixResult;
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
    public CustaIsencaoPolicy custaIsencaoPolicy() {
        return new CustaIsencaoPorRitoPolicy();
    }
}
