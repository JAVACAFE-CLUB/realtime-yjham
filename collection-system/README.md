# Collection System - 실시간 데이터 수집 시스템

실시간으로 뉴스 기사와 위키피디아 데이터를 수집하여 MinIO에 저장하고 Kafka로 이벤트를 발송하는 시스템입니다.

## 🏗️ 시스템 구조

```
Collection System
├── RSS 크롤링 시스템 (뉴스 기사 수집)
├── Wiki Dump 수집 시스템 (위키피디아 데이터 수집)
├── MinIO 스토리지 (데이터 저장) → 추후 S3로 변경 예정
└── Kafka 메시징 (이벤트 발송)
```

## 📋 주요 기능

### 1. RSS 크롤링 시스템
- **주기**: 매시간 실행
- **대상**: 언론사별 RSS 피드
- **처리 과정**: RSS 피드 → HTML 크롤링 → 기사 추출 → MinIO 저장 → Kafka 이벤트 발송
- **지원 언론사**: 경향신문, 조선일보, 중앙일보 등

### 2. Wiki Dump 수집 시스템
- **주기**: 매주 실행
- **대상**: 한국어 위키피디아 덤프 파일 (`kowiki-latest-pages-articles.xml.bz2`)
- **처리 과정**:
  1. **다운로드**: 위키미디어에서 최신 덤프 파일 다운로드 (중복 다운로드 방지)
  2. **압축 해제**: BZ2 압축 파일을 XML로 변환
  3. **파싱**: 대용량 XML을 SAX Parser로 스트리밍 처리
  4. **저장**: 페이지 단위로 MinIO에 JSON 형태로 저장
  5. **이벤트**: 각 페이지별 Kafka 이벤트 발송

### 3. API 수집 시스템 (미구현)
- **주기**: 매시간 실행
- **대상**: 구글 트랜드 API 혹은 유튜브 데이터 API
- **처리 과정**: 필요한 API 요청 → MinIO 저장 → Kafka 이벤트 발송
- **지원 언론사**: 경향신문, 조선일보, 중앙일보 등

## 🛠️ 기술 스택

- **Framework**: Spring Boot
- **언어**: Java 24
- **빌드 도구**: Gradle
- **메시징**: Apache Kafka
- **스토리지**: MinIO
- **XML 파싱**: SAX Parser
- **HTTP 클라이언트**: JSoup
- **압축 해제**: Apache Commons Compress
- **RSS 파싱**: ROME
- **직렬화**: Jackson

## 📦 프로젝트 구조

```
src/main/java/com/realtime/collectionsystem/
├── collection/
│   ├── collector/
│   │   ├── rss/              # RSS 크롤링 관련
│   │   └── wikidump/         # 위키 덤프 수집 관련
│   │       ├── WikiDumpDownloadService.java
│   │       ├── WikiDumpDecompressionService.java
│   │       └── WikiDumpParsingService.java
│   └── scheduler/            # 스케줄링 관련
│       ├── RssCrawlingScheduler.java
│       └── WikiDumpScheduler.java
├── domain/                   # 도메인 모델
│   ├── Article.java
│   └── WikiPage.java
├── storage/                  # 데이터 저장 관련
│   ├── repository/
│   └── service/
├── messaging/                # Kafka 메시징 관련
│   ├── dto/
│   ├── producer/
│   └── service/
└── config/                   # 설정 클래스
    ├── KafkaConfig.java
    └── JacksonConfig.java
```

## 🚀 실행 방법

### 1. 사전 준비
```bash
# Docker Compose로 Kafka와 MinIO 실행
docker-compose up -d
```

### 2. 애플리케이션 실행
```bash
# 개발 환경
./gradlew :collection-system:bootRun

# 프로덕션 빌드
./gradlew :collection-system:build
java -jar build/libs/collection-system.jar
```

### 3. 설정 옵션

#### application.yml
```yaml
# Wiki Dump 관련 설정
wiki:
  dump:
    storage:
      path: ./wiki-dumps              # 덤프 파일 저장 경로
    processing:
      batch-size: 1000                # 배치 처리 크기

# Kafka 설정
spring:
  kafka:
    bootstrap-servers: localhost:9092

# MinIO 설정 (환경변수로 설정 권장)
minio:
  endpoint: http://localhost:9000
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
```

## 📊 데이터 구조

### Article (뉴스 기사)
```json
{
  "url": "https://example.com/article/123",
  "title": "기사 제목",
  "content": "기사 내용",
  "author": "기자명",
  "source": "언론사명",
  "publishedDate": "2025-09-16T12:00:00",
  "collectedDate": "2025-09-16T12:05:00"
}
```

### WikiPage (위키피디아 페이지)
```json
{
  "pageId": 12345,
  "title": "페이지 제목",
  "content": "위키텍스트 내용",
  "namespace": 0,
  "namespaceName": "일반",
  "revisionId": 987654321,
  "lastModified": "2025-09-16T10:30:00",
  "contributor": "사용자명",
  "contributorId": 12345,
  "contentLength": 1500,
  "categories": ["분류1", "분류2"],
  "internalLinks": ["링크1", "링크2"],
  "externalLinks": ["https://example.com"],
  "collectedDate": "2025-09-16T12:00:00"
}
```

## 📂 저장 구조

### MinIO 버킷 구조
```
# 뉴스 기사
crawled-data/
  └── 2025/09/16/
      ├── 경향신문/
      ├── 조선일보/
      └── 중앙일보/

# 위키피디아 데이터
wiki-data/
  └── 2025/09/16/
      ├── namespace-main/      # 일반 문서 (namespace 0)
      ├── namespace-category/  # 분류 (namespace 14)
      └── namespace-template/  # 틀 (namespace 10)
```

## 📨 Kafka 토픽

- **article-collection-events**: 뉴스 기사 수집 이벤트
- **wikipage-collection-events**: 위키피디아 페이지 수집 이벤트

## ⚙️ 모니터링

### 로그 레벨
- **INFO**: 스케줄 실행, 배치 처리 완료
- **DEBUG**: 개별 아이템 처리 상세 정보
- **WARN**: 처리 실패 (계속 진행)
- **ERROR**: 시스템 오류 (중단)

### 주요 메트릭
- 처리된 기사/페이지 수
- 성공/실패 카운트
- 처리 소요 시간
- 네임스페이스별 분류 현황

## 🔧 개발 가이드

### 테스트 실행
```bash
# 전체 테스트
./gradlew :collection-system:test

# 특정 테스트
./gradlew :collection-system:test --tests "*WikiDump*"
```

### 새로운 언론사 추가
1. `RssCollector` 인터페이스 구현
2. `RssCollectorFactory`에 등록
3. 설정 파일에 RSS URL 추가

### 코드 스타일
- Java 24 기능 활용
- Lombok 어노테이션 사용
- 불변 객체 선호 (`@Builder`, `final` 필드)
- Jackson 직렬화 지원 (`@Jacksonized`)

## 🚨 주의사항

### Wiki Dump 처리
- **대용량 파일**: 압축 해제 후 10GB+ XML 파일 생성
- **메모리 관리**: SAX Parser + 배치 처리로 메모리 사용량 최적화
- **디스크 공간**: 압축 파일 + 해제 파일 = 약 12GB 필요
- **처리 시간**: 전체 처리에 수시간 소요 가능

### 성능 최적화
- 배치 크기 조정: `wiki.dump.processing.batch-size` (기본값: 1000)
- JVM 힙 메모리: `-Xmx4g` 이상 권장
- 디스크 I/O: SSD 권장

### 에러 복구
- 네트워크 오류: 자동 재시도 (3회)
- 파싱 오류: 개별 페이지 스킵 후 계속 진행
- 저장 오류: 배치 단위로 스킵 후 계속 진행

## 📈 확장 계획

- [ ] 다국어 위키피디아 지원
- [ ] 실시간 위키피디아 변경사항 추적
- [ ] 더 많은 언론사 RSS 피드 추가
- [ ] 데이터 품질 검증 로직 추가
- [ ] 분산 처리 지원 (Kafka Streams)

## 🤝 기여 방법

1. Fork 프로젝트
2. Feature 브랜치 생성
3. 변경사항 커밋
4. Pull Request 생성

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 있습니다.