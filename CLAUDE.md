# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## 프로젝트 개요
이 프로젝트는 다음 데이터 소스로부터 데이터를 수집하여 실시간 트렌드를 분석합니다:
- 뉴스 기사: RSS 크롤링
  - 언론사별 RSS 피드 수집
  - 기사 URL 추출
  - HTML 크롤링
- YouTube: YouTube Data API
  - 한국 인기 급상승 동영상 수집

## 목표 기능
- 서빙 모듈에서 최종적으로 오늘의 키워드 n개를 제공하는 API가 필요하다.

## Architecture
멀티 모듈 프로젝트

```
collection-module (Java)
    ↓ Kafka
processing-module (Java) ←gRPC→ ner-service (Python)
    ↓ Kafka
indexing-module (Java)
    ↓ Redis
serving-module (Java)
```

## 기술 스택
- Language: Java 21, Python 3.8+
- Framework: Spring Boot 3.2.5
- Build Tool: Gradle (Kotlin DSL)
- Message Queue: Apache Kafka
- Batch/Scheduling: Spring Batch
- Database: MongoDB
- Search Engine: Elasticsearch
- Cache: Redis (공유 캐시), Caffeine (로컬 캐시)
- Rate Limiting: Bucket4j
- RPC: gRPC (Java ↔ Python)
- NER: Hugging Face Transformers (KoBERT NER)
- Monitoring: Prometheus + Grafana + Micrometer
- Container: Docker, Docker Compose

## 성능 최적화
- 개발 초기에는 모든 설정값을 기본값을 사용한다.
- 4개의 모듈에 대한 전체 개발이 완료되면 테스트 및 모니터링을 수행한다.
- 성능을 점검한다.
- 설정값을 개선한다.

## 개발 규칙
- 코드는 알아보기 쉽게 작성해야 한다.
- 테스트 코드를 작성하기 쉽게 작성해야 한다.
- 유지보수가 용이하고, 객체지향적으로 작성해야 한다.
- 상황에 적합한 디자인 패턴이 있다면 사용한다.
- 주석은 한글만 사용하며, 최소한으로 작성한다.
- 로깅은 한글만 사용하며(이모지 사용 금지), 필요한 정보만 남긴다.

## 의존성 추가
- 모든 모듈에서 공유하는 경우: 최상위 `build.gradle.kts` 파일의 `subprojects` 블록에 추가
- 모듈별 의존성의 경우: 모듈별 `build.gradle.kts` 파일에 추가

## Collection Module 설계

### 역할
- 외부 데이터 소스로부터 데이터를 수집하여 MongoDB 저장 및 Kafka로 발행

### 데이터 수집 전략

#### 뉴스 수집
- 수집 주기: 20분마다
- 수집 방식: 언론사별 병렬 수집
- 수집 대상:
  - 경향신문: https://www.khan.co.kr/rss/rssdata/total_news.xml
  - 국민일보: https://www.kmib.co.kr/rss/data/kmibRssAll.xml
  - 전략 패턴으로 언론사 확장 가능하도록 설계
- 수집 데이터:
  - ID, URL, 제목, 본문, 발행시각, 언론사명, 카테고리, 태그, 수집시각
- 중복 체크: URL 기준

#### YouTube 수집
- 수집 주기: 10분마다
- 수집 대상: 대한민국 인기 급상승 동영상 50개
- API: YouTube Data API v3
- 일일 쿼터: 144 units / 10,000 units (1.44%)
- 수집 데이터:
  - videoId, 제목, 설명, 채널명, 발행시각, 카테고리ID, 태그, 조회수, 좋아요수, 댓글수, 수집시각
- 중복 체크: videoId 기준

### Spring Batch 구조

#### Chunk 기반 스트리밍 처리
- Chunk Size: 10~20개 (설정 가능)
- ItemReader → ItemProcessor → ItemWriter 패턴
- 메모리 효율적 처리, 부분 실패 시 재시작 가능

#### 뉴스 수집 Job (NewsCollectionJob)
- ItemReader: RSS 피드에서 URL 읽기
- ItemProcessor: HTML 크롤링 + 중복 체크
- ItemWriter: MongoDB 저장 + Kafka 발행 (트랜잭션)

#### YouTube 수집 Job (YoutubeCollectionJob)
- ItemReader: API 호출하여 동영상 하나씩 반환
- ItemProcessor: 데이터 변환 및 중복 체크
- ItemWriter: MongoDB 저장 + Kafka 발행 (트랜잭션)

### 스케줄링
- 방식: Cron 기반 (@Scheduled)
- 뉴스: `0 */20 * * * *` (매시 0, 20, 40분)
- YouTube: `0 */10 * * * *` (매시 0, 10, 20, 30, 40, 50분)
- 환경: 단일 서버 (중복 실행 방지는 Spring Batch JobParameters 활용)

### 트랜잭션 처리
- 방식: Best Effort + 보상 트랜잭션
- MongoDB 저장 → Kafka 발행 순서
- publishStatus 필드: PENDING / PUBLISHED
- 별도 배치 Job (1시간마다): PENDING 상태 데이터를 Kafka로 재발행

### 에러 처리
- 전략: Item 재시도 + Skip 정책 (하이브리드)
- Retry: 3번 (지수 백오프: 1초, 2초, 4초)
- Skip Limit: Job 전체 100개까지
- Skip 대상: IOException, TimeoutException, HttpClientException
- 실패 Item: 로그만 남기고 재수집하지 않음

### MongoDB 스키마
- 컬렉션: `news`, `youtube` (분리 저장)
- 공통 필드: publishStatus (PENDING/PUBLISHED), publishedAt
- 인덱스:
  - `news`: url (unique), collectedAt, publishStatus
  - `youtube`: videoId (unique), collectedAt, publishStatus

### Kafka 토픽
- `raw-news`: 수집된 뉴스 데이터
- `raw-youtube`: 수집된 YouTube 데이터
- 메시지 형식: 전체 데이터 (JSON)

## Processing Module 설계

### 역할
- Raw 데이터를 전처리하여 색인 가능한 형태로 정제
- 개체명 인식(NER)을 통한 키워드 추출 및 분류
- 데이터 품질 검증 및 필터링

### 처리 방식
- 실시간 스트림 처리: Kafka Consumer
- 처리 흐름: Kafka(raw) → Processing → Python NER Service → Kafka(processed)

### 전처리 작업

#### 텍스트 정제
- HTML 태그 제거
- 특수문자 정규화
- 공백/줄바꿈 정규화
- URL/이메일 제거 또는 마스킹

#### 개체명 인식 (NER)
- Python NER Service와 gRPC 통신
- 장소(LOCATION), 인물(PERSON), 조직(ORGANIZATION) 분류
- 결과 형식: [{keyword: "삼성전자", type: "ORGANIZATION"}, ...]

#### 데이터 품질 검증
- 짧은 본문 필터링: 100자 미만 제외
- 스팸 키워드 필터링

### Kafka Consumer 설정
- 멀티 파티션: 각 토픽 3개 파티션
- Concurrency: 3 (파티션당 1개 Consumer)
- 파티션 키: 뉴스는 언론사명, YouTube는 videoId 해시

### 에러 처리
- 재시도: 3번 (간격: 1초, 2초, 4초)
- Dead Letter Queue: `raw-news-dlq`, `raw-youtube-dlq`
- 재시도 실패 시 DLQ로 전송

### 데이터 저장
- 전처리된 데이터는 MongoDB에 저장하지 않음
- Kafka를 통해 Indexing Module로 전달만 수행

### Kafka 토픽
- 입력: `raw-news` (3 파티션), `raw-youtube` (3 파티션)
- 출력: `processed-news` (3 파티션), `processed-youtube` (3 파티션)
- DLQ: `raw-news-dlq`, `raw-youtube-dlq`
- 메시지 형식: {원본 데이터, processedContent, keywords: [{keyword, type}]}

## Python NER Service 설계

### 역할
- 한국어 개체명 인식 (Named Entity Recognition)
- 장소, 인물, 조직 분류

### 기술 스택
- Language: Python 3.8+
- NER 라이브러리: Pororo (카카오브레인)
- 통신: gRPC Server

### gRPC API
```protobuf
service NERService {
  rpc Analyze(TextRequest) returns (NERResponse);
  rpc AnalyzeBatch(BatchTextRequest) returns (BatchNERResponse);
}
```

### 개체명 분류
- PERSON: 인물
- LOCATION: 장소
- ORGANIZATION: 조직
- 기타: 필터링

### 배포
- Docker 컨테이너로 독립 실행
- Processing Module과 gRPC로 통신
- 포트: 50051

## Indexing Module 설계

### 역할
- Elasticsearch 색인
- 주기적 키워드 집계
- Redis 캐시 저장

### 처리 방식
- Kafka Consumer: 실시간 색인
- 스케줄러: 5분마다 집계 수행

### Elasticsearch 인덱스

#### 인덱스 구조
- 인덱스명: `keywords`
- 단일 인덱스 (뉴스/YouTube 통합)
- 향후 필요시 시계열 인덱스로 확장 가능

#### 매핑 (Mapping)
```json
{
  "keyword": "keyword",        // 집계용
  "type": "keyword",           // PERSON, LOCATION, ORGANIZATION
  "source": "keyword",         // news, youtube
  "sourceId": "keyword",       // 원본 ID
  "collectedAt": "date",
  "originalText": "text"       // 검색용 (향후 확장)
}
```

#### 데이터 보관 정책
- 최근 30일 데이터만 유지
- 매일 자정 삭제 쿼리 실행 (30일 이전 데이터)

### 집계 전략

#### 집계 시점
- 주기: 5분마다 (Cron)
- 범위: 오늘 00:00 ~ 23:59 (일자 기준)

#### 집계 조합
- 전체 키워드 top 100
- 소스별 (뉴스, YouTube) top 100
- 타입별 (인물, 장소, 조직) top 100
- 소스 + 타입 조합 (총 6가지)

#### 결과 저장
- Redis 캐시 저장 (TTL: 10분)
- Key 패턴: `keywords:today:source:{source}:type:{type}`
- Kafka 발행: `aggregated-keywords` 토픽

### Kafka 토픽
- 입력: `processed-news`, `processed-youtube`
- 출력: `aggregated-keywords` (집계 결과)

## Serving Module 설계

### 역할
- 오늘의 키워드 API 제공
- Rate Limiting
- 2단계 캐싱 (Caffeine + Redis)

### API 설계

#### 엔드포인트
```
GET /api/keywords/today
```

#### Query Parameters
- `limit`: 반환 개수 (기본 10, 최대 100)
- `source`: 전체(all) / 뉴스(news) / YouTube(youtube)
- `type`: 전체(all) / 인물(PERSON) / 장소(LOCATION) / 조직(ORGANIZATION)

#### 응답 형식
```json
{
  "keywords": [
    {
      "keyword": "삼성전자",
      "type": "ORGANIZATION",
      "count": 1357
    },
    ...
  ],
  "metadata": {
    "totalCount": 150,
    "limit": 10,
    "source": "all",
    "type": "all",
    "lastUpdated": "2025-11-20T15:30:00Z"
  }
}
```

### 캐싱 전략

#### 2단계 캐싱
```
요청 → Caffeine (로컬, 100μs) → Redis (공유, 1ms) → Elasticsearch (100ms)
```

#### Caffeine 설정
- TTL: 5분
- 최대 크기: 100개 (조합별)
- Eviction: LRU

#### Redis 설정
- TTL: 10분 (Indexing Module이 5분마다 갱신)
- Key 패턴: `keywords:today:source:{source}:type:{type}`

#### 캐시 미스 시
- Elasticsearch 직접 쿼리
- Redis에 캐싱 (TTL: 5분)
- Caffeine에 캐싱

### Rate Limiting
- 방식: IP 기반 (Bucket4j)
- 제한: IP당 분당 100 요청, 시간당 1000 요청
- 초과 시: 429 Too Many Requests + Retry-After 헤더

## Docker Compose 구성

### 파일 구성
- `docker-compose.infra.yml`: 인프라만 (개발용)
- `docker-compose.full.yml`: 전체 시스템 (테스트/배포용)

### 인프라 서비스
- MongoDB
- Kafka + Zookeeper
- Elasticsearch
- Redis
- Prometheus
- Grafana

### 애플리케이션 서비스 (full.yml)
- collection-module
- processing-module
- ner-service (Python)
- indexing-module
- serving-module

### 네트워크
- 모든 서비스는 동일 Docker 네트워크에서 통신
- 외부 노출: Serving Module (8080), Grafana (3000)

## 모니터링 설계

### 메트릭 수집 (Micrometer + Prometheus)

#### Collection Module
- 수집된 뉴스/YouTube 개수
- 수집 성공/실패율
- 배치 Job 실행 시간

#### Processing Module
- 처리된 메시지 수
- NER 처리 시간
- DLQ 메시지 수

#### Indexing Module
- 색인된 문서 수
- 집계 실행 시간
- 캐시 히트율

#### Serving Module
- API 요청 수
- Rate Limiting 발동 횟수
- 응답 시간 (Caffeine/Redis/Elasticsearch 각각)

#### 기본 메트릭
- JVM (메모리, GC, 스레드)
- HTTP 요청 (요청 수, 응답 시간, 에러율)

### Grafana 대시보드
- 전체 파이프라인 상태
- 모듈별 처리량 및 지연시간
- 에러율 및 DLQ 메시지 수
- 캐시 히트율
- API 응답 시간

### 알림
- 초기에는 설정하지 않음
- 운영 안정화 후 추가 검토