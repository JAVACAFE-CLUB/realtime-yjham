package com.realtime.trend.serving.dto;

import java.util.Arrays;

public enum KeywordType {
    ALL("all"),
    PERSON("PERSON"),
    LOCATION("LOCATION"),
    ORGANIZATION("ORGANIZATION");

    private final String value;

    KeywordType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean isValid(String value) {
        return Arrays.stream(values())
                .anyMatch(type -> type.value.equals(value));
    }

    public static String validValues() {
        return String.join(", ", Arrays.stream(values())
                .map(KeywordType::getValue)
                .toArray(String[]::new));
    }
}
