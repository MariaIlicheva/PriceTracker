package ru.pet.project.parsing;

import jakarta.persistence.Converter;
import ru.pet.project.common.DbEnumConverter;

@Converter
public class ParseJobStatusConverter extends DbEnumConverter<ParseJobStatus> {
    public ParseJobStatusConverter() {
        super(ParseJobStatus.class);
    }
}