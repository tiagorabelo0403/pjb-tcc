package com.tcc.pjb.backend.modules.advocacia.office.util;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class RamoDireitoSetConverter implements AttributeConverter<Set<RamoDireito>, String> {

    @Override
    public String convertToDatabaseColumn(Set<RamoDireito> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream()
                .map(RamoDireito::name)
                .sorted()
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<RamoDireito> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return EnumSet.noneOf(RamoDireito.class);
        }
        EnumSet<RamoDireito> out = EnumSet.noneOf(RamoDireito.class);
        Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(value -> {
                    try {
                        out.add(RamoDireito.valueOf(value));
                    } catch (IllegalArgumentException ignored) {
                    }
                });
        return out;
    }
}
