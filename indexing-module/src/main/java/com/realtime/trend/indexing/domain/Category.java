package com.realtime.trend.indexing.domain;

/**
 * 표준화된 카테고리 enum
 * 다양한 소스(뉴스, YouTube)의 카테고리를 통합된 형태로 관리
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
	OTHER("기타");

	private final String displayName;

	Category(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
