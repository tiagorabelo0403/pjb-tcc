package com.tcc.pjb.backend.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.security.crypto.CryptoVaultService;

/**
 * Conversor gerenciado pelo Spring: o Boot configura {@code hibernate.resource.beans.container}
 * com o {@code SpringBeanContainer}, então o Hibernate pede esta instância ao contexto em vez de
 * criá-la por reflexão. É isso que garante que a chave mestra usada para cifrar seja a do contexto
 * dono do EntityManagerFactory.
 *
 * <p>{@link CryptoVaultService} entra por {@link ObjectProvider} e é resolvido só na primeira
 * conversão, não na construção do conversor — o conversor é instanciado durante o bootstrap do
 * EntityManagerFactory, quando nem todos os singletons existem.
 *
 * <p>Ver {@code D-springcontext-estatico-no-conversor-de-pii}: resolver por holder estático de
 * contexto fazia CPF e e-mail serem cifrados com a chave de outro contexto quando várias ITs
 * compartilham a mesma JVM.
 */
@Component
@Converter(autoApply = false)
public class SensitiveDataConverter implements AttributeConverter<String, String> {

    private final ObjectProvider<CryptoVaultService> cryptoVaultProvider;

    public SensitiveDataConverter(ObjectProvider<CryptoVaultService> cryptoVaultProvider) {
        this.cryptoVaultProvider = cryptoVaultProvider;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return cryptoVaultProvider.getObject().blindarDado(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return cryptoVaultProvider.getObject().lerDadoBlindado(dbData);
    }
}
