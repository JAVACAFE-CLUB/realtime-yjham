# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Realtime Korean trend analysis system that collects news (RSS) and YouTube data, extracts keywords using NER, and serves trending keywords via REST API.

## Build & Development Commands

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test                           # All modules
./gradlew :collection-module:test        # Single module
./gradlew test --tests "ClassName"       # Single test class

# Run individual modules
./gradlew :collection-module:bootRun     # Port 8081
./gradlew :processing-module:bootRun     # Port 8082
./gradlew :indexing-module:bootRun       # Port 8083
./gradlew :serving-module:bootRun        # Port 8080

# Infrastructure
docker compose -f docker-compose.infra.yml up -d    # Start infrastructure
docker compose -f docker-compose.infra.yml down     # Stop infrastructure
docker compose -f docker-compose.infra.yml down -v  # Stop and remove volumes
docker compose -f docker-compose.full.yml up -d     # Full system with apps
```

### extraction-module (Python)

```bash
cd extraction-module
pip install -r requirements.txt          # Install dependencies
pytest                                    # Run all tests
pytest tests/test_analyzer.py            # Single test file
python src/main.py                        # Run gRPC server
./generate_proto.sh                       # Generate protobuf
```

## Architecture

### Module Structure

| Module | Port | Purpose |
|--------|------|---------|
| serving-module | 8080 | REST API for keyword queries with caching and rate limiting |
| collection-module | 8081 | Collects news (RSS) and YouTube data via Spring Batch, stores in MongoDB, publishes to Kafka |
| processing-module | 8082 | Consumes raw data from Kafka, extracts keywords via NER gRPC service |
| indexing-module | 8083 | Indexes keywords to Elasticsearch, aggregates trending keywords, caches in Redis |
| extraction-module | 50051 | Python gRPC service for Korean NER using GLiNER-ko model |
| test-support | - | Shared test utilities (Testcontainers base classes, fixtures) |

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

### Kafka Topics

- `raw-news`, `raw-youtube` - Raw collected data
- `processed-news`, `processed-youtube` - Data with extracted keywords
- `raw-news-dlq`, `raw-youtube-dlq` - Dead letter queues
- `aggregated-keywords` - Aggregated trending keywords

## Tech Stack

- Java 21, Spring Boot 3.3.7
- MongoDB (document storage)
- Apache Kafka (message broker)
- Elasticsearch (search/indexing)
- Redis (caching)
- gRPC + Protobuf (extraction-module communication)
- Python + GLiNER-ko (extraction-module)

## Package Conventions

Package structure: `com.realtime.trend.<module-name>.*`

- `batch/` - Spring Batch job configurations
- `config/` - Spring configurations
- `consumer/` - Kafka consumers
- `controller/` - REST controllers
- `domain/` - MongoDB documents
- `document/` - Elasticsearch documents
- `dto/` - Data Transfer Objects
- `repository/` - Data repositories
- `service/` - Business logic
- `grpc/` - gRPC client configurations
- `scheduler/` - Scheduled tasks

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

## Infrastructure Ports

| Service | Port |
|---------|------|
| MongoDB | 27017 |
| Kafka | 9092 |
| Elasticsearch | 9200 |
| Redis | 6379 |
| Extraction Module | 50051 |
| Mongo Express | 9081 |
| Kafka UI | 9082 |
| Redis Insight | 9083 |
| Kibana | 9084 |
| Prometheus | 9090 |
| Grafana | 3000 |

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
