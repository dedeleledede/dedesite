package com.zavan.dedesite.model;

import com.zavan.dedesite.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return EncryptionService.encryptForJpa(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return EncryptionService.decryptForJpa(dbData);
    }
}
