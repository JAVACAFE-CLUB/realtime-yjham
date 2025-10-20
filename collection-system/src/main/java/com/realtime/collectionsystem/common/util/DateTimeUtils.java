package com.realtime.collectionsystem.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    public static LocalDateTime now() {
        return LocalDateTime.now(SEOUL_ZONE);
    }

    public static LocalDateTime parseToLocalDateTime(String dateString, DateTimeFormatter formatter) {
        return ZonedDateTime.parse(dateString, formatter)
                .withZoneSameInstant(SEOUL_ZONE)
                .toLocalDateTime();
    }
}
