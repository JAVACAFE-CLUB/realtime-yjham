# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **real-time data collection system** built with Spring Boot and Spring Batch. It collects data from three sources (News, Wikipedia, YouTube), stores it in MongoDB, and publishes to Kafka for downstream systems (cleansing → indexing → serving).

**Tech Stack:**
- Java 21 + Spring Boot 3.x
- Spring Batch for ETL workflows
- MySQL (batch metadata) + MongoDB (collected data)
- Apache Kafka (message streaming)
- Testcontainers for integration tests

## Build and Run Commands

### Build
```bash
.\gradlew build
```

### Run Tests
```bash
# Run all tests
.\gradlew test

# Run specific test class
.\gradlew test --tests DongaNewsCrawlerTest

# Run specific test method
.\gradlew test --tests DongaNewsCrawlerTest.동아일보_HTML_파싱_테스트
```

### Run Application
```bash
# Local environment (default)
.\gradlew bootRun --args='--spring.profiles.active=local'

# Production environment
.\gradlew bootRun --args='--spring.profiles.active=prod'
```

### Infrastructure (Docker)
Start required services (MySQL, MongoDB, Kafka, Zookeeper):
```bash
docker-compose up -d
```

Stop services:
```bash
docker-compose down
```

View Kafka topics:
```bash
docker exec collection-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

Access web UIs:
- MongoDB Express: http://localhost:8082
- Kafka UI: http://localhost:8081

## Architecture

### Two-Step Batch Pattern
All collection jobs follow this pattern:

1. **Step 1 - Collect**: Read from external source → Process/Validate → Write to MongoDB
2. **Step 2 - Publish**: Read unpublished records (publishedToKafka=false) → Write to Kafka → Update flag to true

### Module Structure
```
com.realtime.collectionsystem/
├── news/           # RSS feeds + HTML crawling (경향신문, 동아일보)
├── wiki/           # WikiDump XML parsing
├── youtube/        # YouTube Data API V3
├── kafka/          # Kafka producer configuration
├── data/           # Database configs (MySQL, MongoDB)
└── common/         # Shared utilities and exceptions
```

Each module contains:
- `batch/` - JobConfig, Reader, Processor, Writer
- `domain/` - Entity classes (with @Document for MongoDB)
- `repository/` - MongoRepository interfaces
- `scheduler/` - @Scheduled job triggers

### Scheduling
Jobs are triggered via Spring Events instead of direct scheduler calls:
- **News**: On startup + every hour (`@EventListener(ApplicationReadyEvent.class)` + `@Scheduled(cron = "0 0 * * * *")`)
- **Wikipedia**: On startup only (one-time initial load)
- **YouTube**: On startup + every hour

The scheduler publishes domain-specific events (`NewsCollectionEvent`, `WikiCollectionEvent`, `YoutubeCollectionEvent`) which are handled by job launchers in each module

### MongoDB Unique Indexes
- News: `url` field
- Wikipedia: `title` field
- YouTube: `videoId` field

## Code Conventions

### Naming Patterns
- Reader: `{Source}{Type}Reader` (e.g., `RssFeedReader`, `NewsKafkaReader`)
- Processor: `{Source}{Type}Processor` (e.g., `NewsArticleProcessor`)
- Writer: `{Source}{Type}Writer` (e.g., `NewsArticleWriter`, `NewsKafkaWriter`)
- Scheduler: `{Source}CollectionScheduler` (e.g., `NewsCollectionScheduler`)
- Job Bean: `{source}CollectionJob` (e.g., `newsCollectionJob`)
- Step Bean: `{source}{Action}Step` (e.g., `newsCollectStep`, `newsPublishStep`)

### Design Principles
- Single Responsibility Principle (SRP) - separate Reader/Processor/Writer
- Strategy Pattern for different news sources (경향신문, 동아일보)
- All entities have `publishedToKafka` (Boolean, default: false) and `collectedDate` (LocalDateTime)

### Logging and Comments
- Use Korean for log messages and comments
- Minimize logging to essential events (start, completion, errors)
- Prefer self-documenting code over comments

### Error Handling
- Log all exceptions but don't stop the batch flow
- On Kafka publish failure, do NOT update the `publishedToKafka` flag (allows retry)

## Configuration

### Profiles
- `application.yml` - base configuration
- `application-local.yml` - local development (localhost databases)
- `application-prod.yml` - production settings

### Environment Variables
Store sensitive data in `.env.local` or `.env.prod` files (not tracked in git).

Example from `application-local.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/collection_batch...
  data:
    mongodb:
      host: localhost
      port: 27017
  kafka:
    bootstrap-servers: localhost:9092
```

## Testing

Uses **Testcontainers** for integration tests with real MySQL, MongoDB, and Kafka containers.

Sample test data is available in the `reference/` directory for each data source.

## Important Implementation Notes

1. **Batch Metadata**: Spring Batch metadata is stored in MySQL (configured in `BatchConfig`), while collected data goes to MongoDB
2. **Chunk Size**: Default chunk size is 100 for all batch steps
3. **Transaction Management**: Uses platform transaction manager for each step's chunk processing
4. **Job Parameters**: Each scheduled execution uses `timestamp` parameter to create unique job instances
5. **News Sources**: The news module uses a strategy pattern to handle different RSS feed formats (Kyunghyang, Donga)
   - **DongaNewsCrawler**: Uses `h2.sub_tit` for titles, `section.news_view` for content, `meta[name=categoryname]` for categories
   - **KhanNewsCrawler**: Uses `section.art_cont h1` for titles, `#articleBody p.content_text` for content, `meta[property=article:section]` for categories
6. **WikiDump**: Uses StAX parser for efficient XML processing of large Wikipedia dump files
7. **Event-Driven Architecture**: Schedulers publish collection events to decouple scheduling from job execution (see `common/event/` package)
