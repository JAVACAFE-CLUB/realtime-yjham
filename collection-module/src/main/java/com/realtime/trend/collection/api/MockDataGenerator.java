package com.realtime.trend.collection.api;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 부하 테스트용 현실적인 한국어 Mock 데이터 생성기
 */
@Component
public class MockDataGenerator {

	// 뉴스 제목 템플릿
	private static final String[] NEWS_TITLE_TEMPLATES = {
		"%s, %s 발표... \"%s\"",
		"[속보] %s %s... 업계 \"충격\"",
		"%s \"%s\" %s 전망",
		"%s, %s 나서... %s 기대",
		"\"%s\" %s... %s 영향 분석",
		"%s %s, %s 확대",
		"[단독] %s %s 검토... %s 관측",
		"%s·%s %s... 시장 반응은?",
		"%s CEO \"%s %s할 것\"",
		"%s, %s 협력... %s 시너지"
	};

	// 유튜브 제목 템플릿
	private static final String[] YOUTUBE_TITLE_TEMPLATES = {
		"[%s] %s %s 리뷰 | %s",
		"%s vs %s 비교! 승자는? #%s",
		"\"%s\" %s이 말하는 %s의 비밀",
		"[긴급] %s %s 발생! %s 대응법",
		"%s %s 언박싱 | 솔직 후기",
		"프로가 알려주는 %s %s 꿀팁",
		"%s %s 먹방 | 진짜 맛있을까?",
		"[VLOG] %s에서 %s 체험기",
		"%s %s 커버 | %s 버전",
		"[%s 특집] %s %s 총정리"
	};

	// 키워드 풀
	private static final String[] TECH_KEYWORDS = {
		"삼성전자", "SK하이닉스", "LG전자", "현대자동차", "기아", "네이버", "카카오",
		"쿠팡", "배달의민족", "토스", "당근마켓", "야놀자", "마켓컬리", "무신사"
	};

	private static final String[] TECH_TOPICS = {
		"AI 반도체", "생성형 AI", "전기차 배터리", "자율주행", "메타버스",
		"블록체인", "클라우드", "5G", "6G", "양자컴퓨터", "로봇", "드론"
	};

	private static final String[] ENTERTAINMENT_KEYWORDS = {
		"BTS", "블랙핑크", "뉴진스", "에스파", "아이브", "르세라핌", "세븐틴",
		"스트레이키즈", "엔시티", "트와이스", "임영웅", "아이유", "태연"
	};

	private static final String[] ENTERTAINMENT_TOPICS = {
		"컴백", "신곡", "콘서트", "팬미팅", "앨범", "음원차트", "빌보드",
		"월드투어", "팬사인회", "뮤직비디오", "OST", "콜라보"
	};

	private static final String[] SPORTS_KEYWORDS = {
		"손흥민", "이강인", "김민재", "황희찬", "오타니", "류현진", "김하성",
		"토트넘", "파리생제르맹", "바이에른", "LA다저스", "맨체스터시티"
	};

	private static final String[] ECONOMY_KEYWORDS = {
		"코스피", "코스닥", "나스닥", "환율", "금리", "부동산", "아파트",
		"청약", "전세", "월세", "주식", "펀드", "ETF", "비트코인"
	};

	private static final String[] ACTIONS = {
		"출시", "발표", "공개", "도입", "확대", "투자", "인수", "합병",
		"협력", "제휴", "계약", "론칭", "업데이트", "개발", "연구"
	};

	private static final String[] ADJECTIVES = {
		"혁신적인", "획기적인", "새로운", "강력한", "대규모", "글로벌",
		"국내 최초", "세계 최초", "차세대", "초대형", "전격", "긴급"
	};

	private static final String[] PUBLISHERS = {
		"조선일보", "중앙일보", "동아일보", "한겨레", "경향신문", "한국경제",
		"매일경제", "서울경제", "아시아경제", "연합뉴스", "YTN", "SBS"
	};

	private static final String[] YOUTUBE_CHANNELS = {
		"빠더너스", "워크맨", "문명특급", "스브스뉴스", "14F", "MBCNEWS",
		"침착맨", "보겸", "쯔양", "먹어볼래", "잇섭", "테크몽", "ITSub"
	};

	private static final String[] CATEGORIES = {
		"정치", "경제", "사회", "IT/과학", "연예", "스포츠", "국제", "문화"
	};

	private final Random random = ThreadLocalRandom.current();

	/**
	 * 뉴스 메시지 생성
	 */
	public Map<String, Object> generateNewsMessage(int index) {
		String category = randomFrom(CATEGORIES);
		String[] keywords = getKeywordsForCategory(category);
		String keyword1 = randomFrom(keywords);
		String keyword2 = randomFrom(TECH_TOPICS);
		String action = randomFrom(ACTIONS);

		Map<String, Object> message = new HashMap<>();
		message.put("id", UUID.randomUUID().toString());
		message.put("url", "https://news.example.com/article/" + System.currentTimeMillis() + "/" + index);
		message.put("title", generateNewsTitle(keyword1, keyword2, action));
		message.put("content", generateNewsContent(keyword1, keyword2, action, category));
		message.put("publishedAt", LocalDateTime.now().minusHours(random.nextInt(24)));
		message.put("publisher", randomFrom(PUBLISHERS));
		message.put("author", generateReporterName());
		message.put("category", category);
		message.put("tags", generateTags(keyword1, keyword2, category));
		message.put("collectedAt", LocalDateTime.now());
		return message;
	}

	/**
	 * YouTube 메시지 생성
	 */
	public Map<String, Object> generateYoutubeMessage(int index) {
		String category = random.nextBoolean() ? "엔터테인먼트" : "테크";
		String[] keywords = category.equals("엔터테인먼트") ? ENTERTAINMENT_KEYWORDS : TECH_KEYWORDS;
		String keyword1 = randomFrom(keywords);
		String keyword2 = category.equals("엔터테인먼트") ?
			randomFrom(ENTERTAINMENT_TOPICS) : randomFrom(TECH_TOPICS);

		Map<String, Object> message = new HashMap<>();
		message.put("id", UUID.randomUUID().toString());
		message.put("videoId", "vid_" + System.currentTimeMillis() + "_" + index);
		message.put("title", generateYoutubeTitle(keyword1, keyword2));
		message.put("description", generateYoutubeDescription(keyword1, keyword2));
		message.put("channelTitle", randomFrom(YOUTUBE_CHANNELS));
		message.put("publishedAt", LocalDateTime.now().minusHours(random.nextInt(72)));
		message.put("categoryId", String.valueOf(random.nextInt(30) + 1));
		message.put("tags", generateTags(keyword1, keyword2, category));
		message.put("viewCount", (long) (Math.pow(10, random.nextInt(4) + 3) * random.nextDouble()));
		message.put("likeCount", (long) (Math.pow(10, random.nextInt(3) + 2) * random.nextDouble()));
		message.put("commentCount", (long) (Math.pow(10, random.nextInt(2) + 1) * random.nextDouble()));
		message.put("collectedAt", LocalDateTime.now());
		return message;
	}

	private String generateNewsTitle(String keyword1, String keyword2, String action) {
		String template = randomFrom(NEWS_TITLE_TEMPLATES);
		String adj = randomFrom(ADJECTIVES);
		return String.format(template, keyword1, keyword2 + " " + action, adj + " 변화");
	}

	private String generateNewsContent(String keyword1, String keyword2, String action, String category) {
		StringBuilder content = new StringBuilder();

		content.append(keyword1).append("이(가) ").append(keyword2).append(" 분야에서 ")
			.append(action).append("을(를) 단행했다. ");

		content.append("업계 관계자에 따르면 이번 ").append(action).append("은(는) ")
			.append(randomFrom(ADJECTIVES)).append(" 시도로 평가받고 있다. ");

		content.append("전문가들은 \"").append(keyword1).append("의 이번 결정이 ")
			.append(category).append(" 분야 전반에 큰 영향을 미칠 것\"이라고 분석했다. ");

		content.append("특히 ").append(keyword2).append(" 시장에서 ").append(keyword1)
			.append("의 입지가 더욱 강화될 것으로 전망된다. ");

		content.append("한편, 경쟁사들도 유사한 전략을 검토 중인 것으로 알려져 ")
			.append("향후 시장 경쟁이 치열해질 것으로 예상된다. ");

		content.append("이와 관련해 ").append(keyword1).append(" 관계자는 ")
			.append("\"").append(randomFrom(ADJECTIVES)).append(" 성과를 거둘 수 있도록 최선을 다하겠다\"고 밝혔다.");

		return content.toString();
	}

	private String generateYoutubeTitle(String keyword1, String keyword2) {
		String template = randomFrom(YOUTUBE_TITLE_TEMPLATES);
		String extra = randomFrom(new String[]{"실화임?", "대박", "충격", "감동", "레전드"});
		return String.format(template, keyword1, keyword2, extra, "2024");
	}

	private String generateYoutubeDescription(String keyword1, String keyword2) {
		StringBuilder desc = new StringBuilder();

		desc.append("안녕하세요! 오늘은 ").append(keyword1).append(" ")
			.append(keyword2).append(" 콘텐츠를 준비했습니다.\n\n");

		desc.append("이번 영상에서는:\n");
		desc.append("- ").append(keyword1).append(" 최신 소식\n");
		desc.append("- ").append(keyword2).append(" 상세 분석\n");
		desc.append("- 시청자 여러분을 위한 특별 정보\n\n");

		desc.append("#").append(keyword1.replace(" ", "")).append(" ");
		desc.append("#").append(keyword2.replace(" ", "")).append(" ");
		desc.append("#한국 #트렌드 #최신");

		return desc.toString();
	}

	private List<String> generateTags(String keyword1, String keyword2, String category) {
		List<String> tags = new ArrayList<>();
		tags.add(keyword1);
		tags.add(keyword2);
		tags.add(category);
		tags.add("트렌드");
		tags.add("2024");
		if (random.nextBoolean()) {
			tags.add("핫이슈");
		}
		return tags;
	}

	private String generateReporterName() {
		String[] surnames = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임"};
		String[] names = {"민수", "지현", "서연", "준호", "수진", "현우", "예진", "동현", "유진", "태현"};
		return randomFrom(surnames) + randomFrom(names) + " 기자";
	}

	private String[] getKeywordsForCategory(String category) {
		return switch (category) {
			case "IT/과학", "경제" -> concat(TECH_KEYWORDS, ECONOMY_KEYWORDS);
			case "연예" -> ENTERTAINMENT_KEYWORDS;
			case "스포츠" -> SPORTS_KEYWORDS;
			default -> TECH_KEYWORDS;
		};
	}

	private String[] concat(String[] a, String[] b) {
		String[] result = new String[a.length + b.length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	private String randomFrom(String[] array) {
		return array[random.nextInt(array.length)];
	}
}
