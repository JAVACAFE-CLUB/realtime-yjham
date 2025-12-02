# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Realtime Korean trend analysis system that collects news (RSS) and YouTube data, extracts keywords using NER, and serves trending keywords via REST API.

## Build & Development Commands

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test                                    # All modules
./gradlew :collection-module:test                 # Single module
./gradlew test --tests "ClassName"                # Single test class
./gradlew test --tests "*Test.methodName"         # Single test method

# Run individual modules
./gradlew :collection-module:bootRun              # Port 8081
./gradlew :processing-module:bootRun              # Port 8082
./gradlew :indexing-module:bootRun                # Port 8083
./gradlew :serving-module:bootRun                 # Port 8080

# Infrastructure
docker compose -f docker-compose.infra.yml up -d              # Start infrastructure only
docker compose -f docker-compose.infra.yml down -v            # Stop and remove volumes
docker compose -f docker-compose.full.yml up -d               # Full system with apps
docker compose -f docker-compose.full.yml up -d --build       # Rebuild and start
docker compose -f docker-compose.full.yml up -d --build <service>  # Rebuild specific service
docker compose -f docker-compose.full.yml logs -f <service>   # Follow logs
```

### extraction-module (Python)

```bash
cd extraction-module
pip install -r requirements.txt           # Install dependencies
pytest                                     # Run all tests
pytest tests/test_analyzer.py             # Single test file
python src/main.py                         # Run gRPC server
./generate_proto.sh                        # Generate protobuf
```

### Kafka Debugging

```bash
# Check consumer group lag
docker exec realtime-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 --group processing-group --describe

# List topics
docker exec realtime-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## Architecture

### Data Flow

```
[News RSS / YouTube API]
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

### Module Responsibilities

| Module | Port | Role |
|--------|------|------|
| collection-module | 8081 | Spring Batch로 RSS/YouTube 수집 → MongoDB 저장 → Kafka 발행 |
| processing-module | 8082 | Kafka 소비 → gRPC로 NER 호출 → 키워드 추출 후 Kafka 발행 |
| indexing-module | 8083 | 처리된 데이터 Elasticsearch 인덱싱 + Redis 캐싱 |
| serving-module | 8080 | REST API 제공 (캐싱, Rate Limiting) |
| extraction-module | 50051 | Python gRPC 서비스, GLiNER-ko 한국어 NER |
| test-support | - | Testcontainers 기반 통합 테스트 유틸리티 |

### Key Abstractions

**collection-module**:
- `Publishable<T>` - Kafka 발행 가능한 엔티티 인터페이스 (상태 관리: PENDING → PUBLISHED/FAILED)
- `DataSource` - 데이터 소스 추상화 (NewsDataSource, YouTubeDataSource)
- `GenericItemWriter` - 범용 Spring Batch Writer

**processing-module**:
- `AbstractMessageConsumer` - Kafka 컨슈머 공통 로직 (에러 처리, DLQ)
- `NerClient` - NER 서비스 클라이언트 인터페이스

**indexing-module**:
- `AbstractIndexingConsumer` - 인덱싱 컨슈머 공통 로직

### Kafka Topics

- `raw-news`, `raw-youtube` - 수집된 원본 데이터
- `processed-news`, `processed-youtube` - 키워드 추출된 데이터
- `raw-news-dlq`, `raw-youtube-dlq` - Dead Letter Queue
- `aggregated-keywords` - 집계된 트렌드 키워드

## Tech Stack

- Java 21, Spring Boot 3.3.7
- MongoDB, Apache Kafka, Elasticsearch, Redis
- gRPC + Protobuf (extraction-module 통신)
- Python + GLiNER-ko (NER)

## Coding Style

- **Convention**: Naver Coding Convention v1.2 (tabs, 120 chars line length)
- **DTO**: Use `record` for immutable DTOs
- **Entity**: Use `@Getter`, `@Builder` (no `@Setter`), state changes via domain methods
- **Lombok**: Active use except `@Setter` on entities

## Git Commits

Format: Conventional Commits
- `feat:` new feature
- `fix:` bug fix
- `refactor:` code refactoring
- `test:` test additions
- `docs:` documentation

## Testing

**Style**: JUnit 5 + Given-When-Then pattern + `@DisplayName` for Korean descriptions

```java
@DisplayName("NewsItemProcessor 단위 테스트")
@ExtendWith(MockitoExtension.class)
class NewsItemProcessorTest {
    @Test
    @DisplayName("정상적인 RSS 아이템을 News 객체로 변환해야 한다")
    void process_withValidRssItem_shouldReturnNews() {
        // given
        // when
        // then
    }
}
```

**Integration Tests**: Extend base classes from `test-support` module:
- `AbstractMongoIntegrationTest` - MongoDB only
- `AbstractKafkaIntegrationTest` - MongoDB + Kafka
- `AbstractFullIntegrationTest` - Full infrastructure
- `AbstractElasticsearchIntegrationTest` - Elasticsearch only
- `AbstractRedisIntegrationTest` - Redis only
- `TestDataFactory` - Test data fixtures
- `KafkaTestSupport` - Kafka test utilities

Test profile: `application-test.yml`.

## Environment Variables

Required in `.env` file:
```
YOUTUBE_API_KEY=<your_youtube_api_key>
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=<secure_password>
```

## API Endpoints

**Collection Module (8081)**:
- `POST /api/jobs/news` - Trigger news collection
- `POST /api/jobs/youtube` - Trigger YouTube collection

**Serving Module (8080)**:
- `GET /api/keywords/today?limit=10&source=all&type=all` - Get trending keywords