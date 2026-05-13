package com.tcc.pjb.backend.service.secretariat.rules;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

@Service
public class SecretariatRulePackFactory {

    private static final Map<RamoDireito, SecretariatRulePack> CATALOG = Map.ofEntries(
            Map.entry(RamoDireito.PENAL, new SecretariatRulePack(
                    RamoDireito.PENAL.name(),
                    Duration.ofHours(24),
                    "DESPACHO_CRIMINAL",
                    List.of("DESPACHO_CRIMINAL", "MANDADO_CITACAO", "CERTIDAO_CUMPRIMENTO"),
                    true,
                    true,
                    false,
                    true
            )),
            Map.entry(RamoDireito.CIVIL, new SecretariatRulePack(
                    RamoDireito.CIVIL.name(),
                    Duration.ofDays(5),
                    "DESPACHO_CIVEL",
                    List.of("DESPACHO_CIVEL", "INTIMACAO_PADRAO", "CERTIDAO_CARTORARIA"),
                    false,
                    false,
                    true,
                    false
            )),
            Map.entry(RamoDireito.FAMILIA, new SecretariatRulePack(
                    RamoDireito.FAMILIA.name(),
                    Duration.ofDays(2),
                    "DESPACHO_FAMILIA",
                    List.of("DESPACHO_FAMILIA", "INTIMACAO_MP", "CERTIDAO_SIGILO"),
                    false,
                    true,
                    true,
                    true
            )),
            Map.entry(RamoDireito.ELEITORAL, new SecretariatRulePack(
                    RamoDireito.ELEITORAL.name(),
                    Duration.ofHours(48),
                    "DESPACHO_ELEITORAL",
                    List.of("DESPACHO_ELEITORAL", "INTIMACAO_URGENTE", "CERTIDAO_CARTORARIA"),
                    true,
                    true,
                    false,
                    false
            )),
            Map.entry(RamoDireito.TRABALHISTA, new SecretariatRulePack(
                    RamoDireito.TRABALHISTA.name(),
                    Duration.ofDays(2),
                    "DESPACHO_TRABALHISTA",
                    List.of("DESPACHO_TRABALHISTA", "NOTIFICACAO_TRABALHISTA", "CERTIDAO_AUDIENCIA"),
                    false,
                    false,
                    true,
                    false
            )),
            Map.entry(RamoDireito.TRIBUTARIO, new SecretariatRulePack(
                    RamoDireito.TRIBUTARIO.name(),
                    Duration.ofDays(10),
                    "DESPACHO_TRIBUTARIO",
                    List.of("DESPACHO_TRIBUTARIO", "INTIMACAO_FAZENDA", "CERTIDAO_CARGA"),
                    false,
                    false,
                    false,
                    false
            )),
            Map.entry(RamoDireito.ADMINISTRATIVO, new SecretariatRulePack(
                    RamoDireito.ADMINISTRATIVO.name(),
                    Duration.ofDays(10),
                    "DESPACHO_FAZENDA",
                    List.of("DESPACHO_FAZENDA", "INTIMACAO_ENTE_PUBLICO", "CERTIDAO_RETORNO"),
                    false,
                    false,
                    false,
                    false
            ))
    );

    private static final SecretariatRulePack DEFAULT = new SecretariatRulePack(
            "PADRAO",
            Duration.ofDays(5),
            "DESPACHO_PADRAO",
            List.of("DESPACHO_PADRAO", "INTIMACAO_PADRAO", "CERTIDAO_CARTORARIA"),
            false,
            false,
            false,
            false
    );

    public SecretariatRulePack resolve(RamoDireito ramoDireito) {
        return ramoDireito == null ? DEFAULT : CATALOG.getOrDefault(ramoDireito, DEFAULT);
    }

    public SecretariatRulePack resolve(String ramoDireito) {
        if (ramoDireito == null || ramoDireito.isBlank()) {
            return DEFAULT;
        }
        RamoDireito ramo = RamoDireito.fromString(ramoDireito.trim().toUpperCase(Locale.ROOT));
        return resolve(ramo);
    }
}
