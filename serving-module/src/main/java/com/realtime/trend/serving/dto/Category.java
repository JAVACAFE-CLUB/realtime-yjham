package com.realtime.trend.serving.dto;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 키워드 카테고리 enum
 */
public enum Category {
	POLITICS("정치"),
	ECONOMY("경제"),
	SOCIETY("사회"),
	INTERNATIONAL("국제"),
	SCIENCE("과학"),
	LIFESTYLE("라이프"),
	OPINION("오피니언"),
	LOCAL("지역"),
	PEOPLE("인물"),
	ENTERTAINMENT("엔터테인먼트"),
	MUSIC("음악"),
	SPORTS("스포츠"),
	GAMING("게임"),
	OTHER("기타"),
	ALL("전체");

	private final String displayName;

	private static final Set<String> VALID_VALUES = Arrays.stream(values())
		.map(Enum::name)
		.map(String::toLowerCase)
		.collect(Collectors.toSet());

	Category(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static boolean isValid(String value) {
		return value != null && VALID_VALUES.contains(value.toLowerCase());
	}

	public static String validValues() {
		return VALID_VALUES.toString();
	}
}
