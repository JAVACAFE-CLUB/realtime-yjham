package com.realtime.trend.serving.dto;

import java.util.Arrays;

public enum DataSource {
    ALL("all"),
    NEWS("news"),
    YOUTUBE("youtube");

    private final String value;

    DataSource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean isValid(String value) {
        return Arrays.stream(values())
                .anyMatch(source -> source.value.equals(value));
    }

    public static String validValues() {
        return String.join(", ", Arrays.stream(values())
                .map(DataSource::getValue)
                .toArray(String[]::new));
    }
}
