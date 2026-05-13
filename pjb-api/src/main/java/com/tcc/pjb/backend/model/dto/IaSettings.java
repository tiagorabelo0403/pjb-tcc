package com.tcc.pjb.backend.model.dto;

import java.time.Instant;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IaSettings {
    private boolean suggestionsEnabled = true;
    private boolean sendToProcessEnabled = false;
    private boolean sendToJudgeEnabled = true;
    private boolean preserveEssence = true;
    private Instant timeCutoff;

    @Converter(autoApply = true)
    public static class IaSettingsConverter implements AttributeConverter<IaSettings, String> {
        private final static ObjectMapper objectMapper = new ObjectMapper();
        @Override
        public String convertToDatabaseColumn(IaSettings attribute) {
            try {
                return objectMapper.writeValueAsString(attribute);
            } catch (Exception e) {
                return null;
            }
        }
        @Override
        public IaSettings convertToEntityAttribute(String dbData) {
            try {
                if (dbData == null || dbData.isBlank()) return new IaSettings();
                return objectMapper.readValue(dbData, IaSettings.class);
            } catch (Exception e) {
                return new IaSettings();
            }
        }
    }
}