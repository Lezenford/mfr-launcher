package ru.fullrest.mfr.server.model.dto;

import lombok.Data;

@Data
public class StatisticsCountDto {
    private final String version;
    private final long count;
}
