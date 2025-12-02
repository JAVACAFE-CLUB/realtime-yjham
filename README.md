# 실시간 트렌드 분석 시스템

뉴스(RSS)와 YouTube 데이터를 수집하고, NER(개체명 인식)을 통해 키워드를 추출하여 실시간 트렌드 키워드를 제공하는 시스템입니다.

## 아키텍처

```
[뉴스 RSS / YouTube API]
         |
         v
  collection-module (MongoDB) ---> Kafka (raw-news, raw-youtube)
         |
         v
  processing-module <--gRPC--> extraction-module (GLiNER-ko)
         |
         v
  Kafka (processed-news, processed-youtube)
         |
         v
  indexing-module ---> Elasticsearch + Redis
         |
         v
  serving-module ---> REST API
```

## 모듈 구조

| 모듈 | 포트 | 설명 |
|------|------|------|
| serving-module | 8080 | 트렌드 키워드 조회 REST API (캐싱, Rate Limiting) |
| collection-module | 8081 | 뉴스(RSS), YouTube 데이터 수집 (Spring Batch) |
| processing-module | 8082 | Kafka 메시지 소비, NER gRPC 서비스로 키워드 추출 |
| indexing-module | 8083 | Elasticsearch 인덱싱, 트렌드 키워드 집계, Redis 캐싱 |
| extraction-module | 50051 | Python gRPC 서비스, GLiNER-ko 모델로 한국어 NER |
| test-support | - | 공유 테스트 유틸리티 (Testcontainers 기반) |

## 기술 스택

- **Backend**: Java 21, Spring Boot 3.3.7
- **데이터 저장**: MongoDB, Elasticsearch, Redis
- **메시지 브로커**: Apache Kafka
- **NER 서비스**: Python, GLiNER-ko, gRPC
- **모니터링**: Prometheus, Grafana, Micrometer

## 빠른 시작

### 1. 사전 요구사항

- Docker & Docker Compose
- Java 21
- Gradle

### 2. 환경 변수 설정

프로젝트 루트에 `.env` 파일 생성 (`.env.example` 참고):

```env
YOUTUBE_API_KEY=<your_youtube_api_key>
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=<secure_password>
```

### 3. 인프라 실행

```bash
# 인프라 서비스 시작 (MongoDB, Kafka, Elasticsearch, Redis, NER 등)
docker compose -f docker-compose.infra.yml up -d

# 인프라 중지
docker compose -f docker-compose.infra.yml down

# 인프라 중지 및 볼륨 삭제
docker compose -f docker-compose.infra.yml down -v
```

### 4. 애플리케이션 실행

#### 개별 모듈 실행 (개발 환경)

```bash
./gradlew :collection-module:bootRun     # 포트 8081
./gradlew :processing-module:bootRun     # 포트 8082
./gradlew :indexing-module:bootRun       # 포트 8083
./gradlew :serving-module:bootRun        # 포트 8080
```

#### 전체 시스템 실행 (Docker)

```bash
# 전체 시스템 시작
docker compose -f docker-compose.full.yml up -d

# 재빌드 후 시작
docker compose -f docker-compose.full.yml up -d --build

# 특정 서비스만 재빌드
docker compose -f docker-compose.full.yml up -d --build collection-module

# 로그 확인
docker compose -f docker-compose.full.yml logs -f <service-name>
```

## API 사용법

### 데이터 수집 트리거

```bash
# 뉴스 수집 시작
curl -X POST http://localhost:8081/api/jobs/news

# YouTube 수집 시작
curl -X POST http://localhost:8081/api/jobs/youtube
```

### 트렌드 키워드 조회

```bash
# 오늘의 트렌드 키워드 조회
curl "http://localhost:8080/api/keywords/today?limit=10&source=all&type=all"
```

### 헬스 체크

```bash
# 각 모듈 헬스 체크 (Actuator)
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

## 모니터링

### 대시보드 접속

| 서비스 | URL | 설명 |
|--------|-----|------|
| Grafana | http://localhost:3000 | 모니터링 대시보드 |
| Prometheus | http://localhost:9090 | 메트릭 수집/조회 |
| Kafka UI | http://localhost:9082 | Kafka 토픽/컨슈머 모니터링 |
| Kibana | http://localhost:9084 | Elasticsearch 데이터 조회 |
| Mongo Express | http://localhost:9081 | MongoDB 데이터 조회 |
| Redis Insight | http://localhost:9083 | Redis 데이터 조회 |

### Kafka 상태 확인

```bash
# Consumer Group Lag 확인
docker exec realtime-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 --group processing-group --describe

# 토픽 목록
docker exec realtime-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## 인프라 포트

| 서비스 | 포트 | 설명 |
|--------|------|------|
| MongoDB | 27017 | 문서 저장소 |
| Kafka | 9092 | 메시지 브로커 |
| Elasticsearch | 9200 | 검색/인덱싱 |
| Redis | 6379 | 캐싱 |
| Extraction Module | 50051 | NER gRPC 서비스 |

## 빌드 및 테스트

```bash
# 전체 빌드
./gradlew build

# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :collection-module:test

# 특정 테스트 클래스 실행
./gradlew test --tests "NewsItemProcessorTest"

# 특정 테스트 메서드 실행
./gradlew test --tests "*Test.methodName"
```

## Kafka 토픽

| 토픽 | 설명 |
|------|------|
| raw-news | 수집된 원본 뉴스 데이터 |
| raw-youtube | 수집된 원본 YouTube 데이터 |
| processed-news | 키워드가 추출된 뉴스 데이터 |
| processed-youtube | 키워드가 추출된 YouTube 데이터 |
| raw-news-dlq | 뉴스 처리 실패 데이터 (Dead Letter Queue) |
| raw-youtube-dlq | YouTube 처리 실패 데이터 (Dead Letter Queue) |
| aggregated-keywords | 집계된 트렌드 키워드 |

## 프로젝트 구조

```
realtime-trend-system/
├── collection-module/     # 데이터 수집 모듈
├── processing-module/     # 키워드 추출 모듈
├── indexing-module/       # 인덱싱/집계 모듈
├── serving-module/        # REST API 모듈
├── extraction-module/     # Python NER 서비스
├── test-support/          # 공유 테스트 유틸리티
├── monitoring/            # Prometheus, Grafana 설정
├── docs/                  # 문서
├── docker-compose.infra.yml   # 인프라 Docker 설정
└── docker-compose.full.yml    # 전체 시스템 Docker 설정
```