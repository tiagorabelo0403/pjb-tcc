package com.tcc.pjb.backend.modules.advocacia.office.util;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;

@Converter
public class OfficeActionSetConverter implements AttributeConverter<Set<OfficeActionType>, String> {

    @Override
    public String convertToDatabaseColumn(Set<OfficeActionType> attribute) {
        if (attribute == null || attribute.isEmpty()) return "";
        return attribute.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    @Override
    public Set<OfficeActionType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return EnumSet.noneOf(OfficeActionType.class);
        String[] parts = dbData.split(",");
        EnumSet<OfficeActionType> out = EnumSet.noneOf(OfficeActionType.class);
        Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .forEach(s -> {
                    try {
                        out.add(OfficeActionType.valueOf(s));
                    } catch (Exception ignored) {
                    }
                });
        return out;
    }
}
