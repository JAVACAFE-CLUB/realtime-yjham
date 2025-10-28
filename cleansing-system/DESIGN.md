# Cleansing System 설계 문서

## 1. 개요

### 1.1 목적
수집 시스템(collection-system)으로부터 전달받은 원본 데이터를 정제하여, 색인 시스템(indexing-system)에서 활용 가능한 깨끗한 데이터로 변환합니다.

### 1.2 시스템 위치
```
collection-system → **cleansing-system** → indexing-system → serving-system
```

### 1.3 기술 스택
- **언어**: Java 21
- **프레임워크**: Spring Boot 3.x.x, Spring Kafka
- **데이터베이스**:
  - MongoDB (원본 데이터 조회 + 정제 데이터 저장)
- **메시징**: Apache Kafka (Consumer + Producer)
- **텍스트 처리**:
  - HTML 정리
  - 위키 마크업 파싱(text/x-wiki)
  - 텍스트 정규화
  - Apache Tika, Apache Commons Text 등 
---

## 2. 데이터 정제 요구사항

### 2.1 뉴스 데이터 정제
**입력 (수집 시스템에서 전달, reference/news-article-document.txt 파일 참고)**
```json
{
  "_id": "68fe363619f58211bdb104d9",
  "source": "khan",
  "title": "현대건설, 국내 최초 미 원전 설계 수주",
  "text": "현대건설이 국내 기업 중 최초로...",
  "url": "https://www.khan.co.kr/article/...",
  "category": "경제",
  "createdDate": "2025-10-26T11:43:00.000Z",
  "collectedDate": "2025-10-26T14:54:45.066Z"
}
```

**정제 작업**
1. HTML 태그 제거 (이미 수집 시스템에서 처리됨)
2. 공백 정규화 (연속된 공백 → 단일 공백)
3. 줄바꿈 정규화 (연속된 줄바꿈 → 최대 2개)
4. 특수문자 정리 (제어 문자, 불필요한 유니코드 제거)
5. 앞뒤 공백 제거 (trim)

### 2.2 위키피디아 데이터 정제
**입력 (수집 시스템에서 전달, reference/wiki-page-document.txt 파일 참고)**
```json
{
  "_id": "68fe395e6a548b1299f860d3",
  "pageId": 34,
  "revisionId": 40628126,
  "title": "대한민국 제16대 대통령 선거",
  "text": "{{선거 정보\n| 선거명 = 대한민국 제16대...\n'''대한민국 제16대 대통령 선거'''는 [[대한민국의 대통령]]을...",
  "createdDate": "2025-01-15T10:30:00.000Z",
  "collectedDate": "2025-10-26T14:54:47.905Z"
}
```

**정제 작업**
1. 위키 마크업 제거
   - 템플릿: `{{템플릿명|...}}` → 제거
   - 내부 링크: `[[링크|표시명]]` → "표시명" 추출
   - 볼드: `'''텍스트'''` → "텍스트"
   - 이탤릭: `''텍스트''` → "텍스트"
   - 표: `{| ... |}` → 제거
   - 참조: `<ref>...</ref>` → 제거
2. 공백 정규화
3. 줄바꿈 정규화
4. 특수문자 정리
5. 앞뒤 공백 제거

### 2.3 유튜브 데이터 정제
**입력 (수집 시스템에서 전달, reference/youtube-video-document.txt 파일 참고)**
```json
{
  "_id": "68fe363719f58211bdb104e3",
  "videoId": "TvVtYaqCni8",
  "title": "LE SSERAFIM (르세라핌) 'SPAGHETTI (feat. j-hope of BTS)' OFFICIAL MV",
  "description": "LE SSERAFIM (르세라핌) 'SPAGHETTI...\n💿 https://le-sserafim.lnk.to/SPAGHETTI\n...",
  "tags": ["HYBE", "HYBE LABELS", ...],
  "channelTitle": "HYBE LABELS",
  "categoryId": "10",
  "createdDate": "2025-10-24T03:58:08.000Z",
  "collectedDate": "2025-10-26T14:54:47.905Z"
}
```

**정제 작업**
1. URL 제거 (description 내의 모든 URL)
2. 이모지 제거 또는 정규화
3. 공백 정규화
4. 줄바꿈 정규화
5. 특수문자 정리
6. 앞뒤 공백 제거

---

## 3. 시스템 아키텍처

### 3.1 Kafka 토픽 구조

**입력 토픽 (collection-system에서 수신)**
- `news-collect-topic`: 뉴스 수집 이벤트
- `wiki-collect-topic`: 위키피디아 수집 이벤트
- `youtube-collect-topic`: 유튜브 수집 이벤트

**출력 토픽 (indexing-system으로 발행)**
- `news-cleanse-topic`: 뉴스 정제 완료 이벤트
- `wiki-cleanse-topic`: 위키피디아 정제 완료 이벤트
- `youtube-cleanse-topic`: 유튜브 정제 완료 이벤트

### 3.2 Kafka 메시지 구조

**입력 메시지 (collection-system에서 수신)**
```text
// NewsCollectionEvent
{
  "dataType": "NEWS",
  "url": "https://www.khan.co.kr/article/...",
  "source": "khan",
  "collectedDate": "2025-10-26T14:54:45.066"
}

// WikiCollectionEvent
{
  "dataType": "WIKI",
  "title": "대한민국 제16대 대통령 선거",
  "collectedDate": "2025-10-26T14:54:47.905"
}

// YoutubeCollectionEvent
{
  "dataType": "YOUTUBE",
  "videoId": "TvVtYaqCni8",
  "collectedDate": "2025-10-26T14:54:47.905"
}
```

**출력 메시지 (indexing-system으로 발행)**
```text
// NewsCleansingEvent
{
  "dataType": "NEWS",
  "id": "68fe363619f58211bdb104d9",  // MongoDB _id
  "cleansedDate": "2025-10-27T10:00:00.000"
}

// WikiCleansingEvent
{
  "dataType": "WIKI",
  "id": "68fe395e6a548b1299f860d3",
  "cleansedDate": "2025-10-27T10:00:00.000"
}

// YoutubeCleansingEvent
{
  "dataType": "YOUTUBE",
  "id": "68fe363719f58211bdb104e3",
  "cleansedDate": "2025-10-27T10:00:00.000"
}
```

### 3.3 MongoDB 컬렉션 구조

**원본 데이터 조회 (collection-system에서 저장)**
- `news_articles`: 뉴스 원본 데이터
- `wiki_pages`: 위키피디아 원본 데이터
- `youtube_videos`: 유튜브 원본 데이터

**정제 데이터 저장 (cleansing-system에서 생성)**
- `cleansed_news`: 정제된 뉴스 데이터
- `cleansed_wiki`: 정제된 위키피디아 데이터
- `cleansed_youtube`: 정제된 유튜브 데이터

**정제 데이터 스키마**
```text
// CleansedNews
{
  "_id": "68fe363619f58211bdb104d9",
  "source": "khan",
  "title": "현대건설, 국내 최초 미 원전 설계 수주",
  "cleanedText": "현대건설이 국내 기업 중 최초로...",
  "category": "경제",
  "createdDate": "2025-10-26T11:43:00.000Z",
  "collectedDate": "2025-10-26T14:54:45.066Z",
  "cleansedDate": "2025-10-27T10:00:00.000Z"
}

// CleansedWiki
{
  "_id": "68fe395e6a548b1299f860d3",
  "pageId": 34,
  "revisionId": 40628126,
  "title": "대한민국 제16대 대통령 선거",
  "cleanedText": "대한민국 제16대 대통령 선거는...",
  "createdDate": "2025-01-15T10:30:00.000Z",
  "collectedDate": "2025-10-26T14:54:47.905Z",
  "cleansedDate": "2025-10-27T10:00:00.000Z"
}

// CleansedYoutube
{
  "_id": "68fe363719f58211bdb104e3",
  "videoId": "TvVtYaqCni8",
  "title": "LE SSERAFIM (르세라핌) 'SPAGHETTI (feat. j-hope of BTS)' OFFICIAL MV",
  "cleanedDescription": "LE SSERAFIM (르세라핌) 'SPAGHETTI...",
  "tags": ["HYBE", "HYBE LABELS", ...],
  "channelTitle": "HYBE LABELS",
  "categoryId": "10",
  "createdDate": "2025-10-24T03:58:08.000Z",
  "collectedDate": "2025-10-26T14:54:47.905Z",
  "cleansedDate": "2025-10-27T10:00:00.000Z"
}
```

---

## 4. 데이터 플로우

### 4.1 전체 처리 흐름
```
1. Kafka Consumer: 수집 이벤트 수신 (news-collect-topic, wiki-collect-topic, youtube-collect-topic)
   ↓
2. MongoDB Reader: 원본 데이터 조회 (news_articles, wiki_pages, youtube_videos)
   ↓
3. Text Cleaner: 텍스트 정제 처리
   - NewsTextCleaner: 뉴스 텍스트 정제
   - WikiTextCleaner: 위키 마크업 파싱 및 정제
   - YoutubeTextCleaner: URL/이모지 제거 및 정제
   ↓
4. MongoDB Writer: 정제 데이터 저장 (cleansed_news, cleansed_wiki, cleansed_youtube)
   ↓
5. Kafka Producer: 정제 완료 이벤트 발행 (news-cleanse-topic, wiki-cleanse-topic, youtube-cleanse-topic)
```

### 4.2 세부 처리 단계

**Step 1: Kafka 메시지 수신**
- Consumer가 토픽별로 메시지 수신
- 메시지에서 MongoDB 조회 키 추출 (url, title, videoId)

**Step 2: MongoDB 원본 조회**
- 뉴스: `url`로 조회
- 위키: `title`로 조회
- 유튜브: `videoId`로 조회

**Step 3: 텍스트 정제**
- 데이터 타입별 정제 로직 적용
- 공백/줄바꿈 정규화
- 특수문자 정리

**Step 4: 정제 데이터 저장**
- MongoDB 별도 컬렉션에 저장
- `_id`는 원본 데이터와 동일하게 유지

**Step 5: Kafka 메시지 발행**
- indexing-system이 조회할 수 있도록 `_id`만 전달
- 정제 완료 시간(`cleansedDate`) 포함

---

## 5. 컴포넌트 구조

### 5.1 패키지 구조 (예상)
```
com.realtime.cleansingsystem/
├── common/                  # 공통 유틸, 예외, 상수
│   ├── event/              # Kafka 이벤트 (Collection/Cleansing)
│   ├── exception/
│   └── util/
├── config/                  # 설정
│   ├── KafkaConfig         # Kafka Consumer/Producer 설정
│   └── MongoConfig         # MongoDB 설정
├── news/                    # 뉴스 정제
│   ├── domain/             # CleansedNews 엔티티
│   ├── repository/         # MongoRepository (원본 + 정제)
│   ├── service/            # NewsCleansingService
│   ├── cleaner/            # NewsTextCleaner
│   └── consumer/           # NewsConsumer
├── wiki/                    # 위키피디아 정제
│   ├── domain/
│   ├── repository/
│   ├── service/
│   ├── cleaner/            # WikiTextCleaner (마크업 파싱)
│   └── consumer/
└── youtube/                 # 유튜브 정제
    ├── domain/
    ├── repository/
    ├── service/
    ├── cleaner/            # YoutubeTextCleaner
    └── consumer/
```

### 5.2 주요 컴포넌트

**Consumer (Kafka 메시지 수신)**
- `NewsConsumer`: `news-collect-topic` 구독
- `WikiConsumer`: `wiki-collect-topic` 구독
- `YoutubeConsumer`: `youtube-collect-topic` 구독

**Service (비즈니스 로직)**
- `NewsCleansingService`: 뉴스 정제 오케스트레이션
- `WikiCleansingService`: 위키 정제 오케스트레이션
- `YoutubeCleansingService`: 유튜브 정제 오케스트레이션

**Cleaner (텍스트 정제)**
- `NewsTextCleaner`: 뉴스 텍스트 정제
- `WikiTextCleaner`: 위키 마크업 파싱 및 정제
- `YoutubeTextCleaner`: 유튜브 설명 정제

**Repository (MongoDB 접근)**
- 원본 조회: `NewsArticleRepository`, `WikiPageRepository`, `YoutubeVideoRepository`
- 정제 저장: `CleansedNewsRepository`, `CleansedWikiRepository`, `CleansedYoutubeRepository`

**Producer (Kafka 메시지 발행)**
- `KafkaTemplate`을 통해 정제 완료 이벤트 발행

---

## 참고 자료

- 수집 시스템 구현: `../collection-system/`
- MongoDB 샘플 데이터: `reference/`
