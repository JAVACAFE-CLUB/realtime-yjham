package com.realtime.trend.indexing.domain;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 원본 카테고리를 표준 Category로 매핑하는 클래스
 */
@Component
public class CategoryMapper {

	private static final Map<String, Category> NEWS_CATEGORY_MAP = Map.ofEntries(
		Map.entry("정치", Category.POLITICS),
		Map.entry("경제", Category.ECONOMY),
		Map.entry("사회", Category.SOCIETY),
		Map.entry("국제", Category.INTERNATIONAL),
		Map.entry("과학", Category.SCIENCE),
		Map.entry("과학·환경", Category.SCIENCE),
		Map.entry("IT", Category.SCIENCE),
		Map.entry("라이프", Category.LIFESTYLE),
		Map.entry("생활", Category.LIFESTYLE),
		Map.entry("문화", Category.LIFESTYLE),
		Map.entry("오피니언", Category.OPINION),
		Map.entry("만평", Category.OPINION),
		Map.entry("사설", Category.OPINION),
		Map.entry("칼럼", Category.OPINION),
		Map.entry("지역", Category.LOCAL),
		Map.entry("사람들", Category.PEOPLE),
		Map.entry("인물", Category.PEOPLE),
		Map.entry("스포츠", Category.SPORTS),
		Map.entry("연예", Category.ENTERTAINMENT)
	);

	private static final Map<String, Category> YOUTUBE_CATEGORY_MAP = Map.ofEntries(
		Map.entry("1", Category.OTHER),           // Film & Animation
		Map.entry("2", Category.OTHER),           // Autos & Vehicles
		Map.entry("10", Category.MUSIC),          // Music
		Map.entry("15", Category.OTHER),          // Pets & Animals
		Map.entry("17", Category.SPORTS),         // Sports
		Map.entry("18", Category.OTHER),          // Short Movies
		Map.entry("19", Category.OTHER),          // Travel & Events
		Map.entry("20", Category.GAMING),         // Gaming
		Map.entry("21", Category.OTHER),          // Videoblogging
		Map.entry("22", Category.PEOPLE),         // People & Blogs
		Map.entry("23", Category.OTHER),          // Comedy
		Map.entry("24", Category.ENTERTAINMENT),  // Entertainment
		Map.entry("25", Category.OTHER),          // News & Politics
		Map.entry("26", Category.LIFESTYLE),      // Howto & Style
		Map.entry("27", Category.SCIENCE),        // Education
		Map.entry("28", Category.SCIENCE),        // Science & Technology
		Map.entry("29", Category.OTHER),          // Nonprofits & Activism
		Map.entry("30", Category.OTHER),          // Movies
		Map.entry("31", Category.OTHER),          // Anime/Animation
		Map.entry("32", Category.OTHER),          // Action/Adventure
		Map.entry("33", Category.OTHER),          // Classics
		Map.entry("34", Category.OTHER),          // Comedy
		Map.entry("35", Category.OTHER),          // Documentary
		Map.entry("36", Category.OTHER),          // Drama
		Map.entry("37", Category.OTHER),          // Family
		Map.entry("38", Category.OTHER),          // Foreign
		Map.entry("39", Category.OTHER),          // Horror
		Map.entry("40", Category.OTHER),          // Sci-Fi/Fantasy
		Map.entry("41", Category.OTHER),          // Thriller
		Map.entry("42", Category.OTHER),          // Shorts
		Map.entry("43", Category.OTHER),          // Shows
		Map.entry("44", Category.OTHER)           // Trailers
	);

	/**
	 * 뉴스 카테고리를 표준 Category로 매핑
	 *
	 * @param originalCategory 원본 카테고리명
	 * @return 매핑된 Category (매핑 실패 시 OTHER)
	 */
	public Category mapNewsCategory(String originalCategory) {
		if (originalCategory == null || originalCategory.isBlank()) {
			return Category.OTHER;
		}
		return NEWS_CATEGORY_MAP.getOrDefault(originalCategory.trim(), Category.OTHER);
	}

	/**
	 * YouTube 카테고리 ID를 표준 Category로 매핑
	 *
	 * @param categoryId YouTube 카테고리 ID
	 * @return 매핑된 Category (매핑 실패 시 OTHER)
	 */
	public Category mapYoutubeCategory(String categoryId) {
		if (categoryId == null || categoryId.isBlank()) {
			return Category.OTHER;
		}
		return YOUTUBE_CATEGORY_MAP.getOrDefault(categoryId.trim(), Category.OTHER);
	}

	/**
	 * 소스 타입에 따라 적절한 매핑 수행
	 *
	 * @param source 소스 타입 (news, youtube)
	 * @param originalCategory 원본 카테고리
	 * @return 매핑된 Category
	 */
	public Category map(String source, String originalCategory) {
		if ("youtube".equalsIgnoreCase(source)) {
			return mapYoutubeCategory(originalCategory);
		}
		return mapNewsCategory(originalCategory);
	}
}
