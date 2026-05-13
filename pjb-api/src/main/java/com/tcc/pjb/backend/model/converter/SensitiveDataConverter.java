package com.tcc.pjb.backend.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.tcc.pjb.backend.core.infra.spring.SpringContext;
import com.tcc.pjb.backend.core.security.crypto.CryptoVaultService;

@Converter(autoApply = false)
public class SensitiveDataConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return SpringContext.getBean(CryptoVaultService.class).blindarDado(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return SpringContext.getBean(CryptoVaultService.class).lerDadoBlindado(dbData);
    }
}
