# Realtime Trend System

실시간 트렌드 분석 시스템 - 뉴스와 YouTube 데이터를 수집하여 오늘의 키워드를 제공하는 API

## 프로젝트 구조

```
realtime-trend-system/
├── collection-module/         # 데이터 수집 모듈 (Spring Batch)
├── processing-module/         # 데이터 전처리 모듈 (Kafka Consumer)
├── indexing-module/           # Elasticsearch 색인 모듈
├── serving-module/            # REST API 제공 모듈
├── docker-compose.infra.yml   # 인프라 구성 (개발용)
├── docker-compose.full.yml    # 전체 시스템 (향후 추가)
└── monitoring/                # Prometheus & Grafana 설정
```

## 시작하기

### 1. 요구사항

- Java 21
- Docker Desktop (Windows)
- Gradle 8.x

### 2. 인프라 시작 (PowerShell 또는 CMD에서 실행)

```powershell
# 프로젝트 루트 디렉토리에서 실행
docker compose -f docker-compose.infra.yml up -d
```

### 3. 인프라 상태 확인

```powershell
docker compose -f docker-compose.infra.yml ps
```

모든 서비스가 `healthy` 상태가 될 때까지 기다립니다 (약 1-2분 소요).

### 4. 인프라 서비스 접속 정보

| 서비스 | 접속 정보 | 계정 |
|--------|-----------|------|
| MongoDB | `localhost:27017` | admin / admin123 |
| Kafka | `localhost:9092` | - |
| Elasticsearch | `http://localhost:9200` | - |
| Redis | `localhost:6379` | - |
| Prometheus | `http://localhost:9090` | - |
| Grafana | `http://localhost:3000` | admin / admin123 |

### 5. 인프라 연결 테스트

#### MongoDB 테스트
```powershell
docker exec -it realtime-mongodb mongosh -u admin -p admin123
# MongoDB shell에서
> use realtime-trend
> show collections
> exit
```

#### Kafka 테스트
```powershell
# 토픽 목록 확인
docker exec -it realtime-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

#### Elasticsearch 테스트
```powershell
# 브라우저에서 http://localhost:9200 접속
# 또는
curl http://localhost:9200
```

#### Redis 테스트
```powershell
docker exec -it realtime-redis redis-cli ping
# PONG 응답이 오면 정상
```

### 6. 인프라 중지

```powershell
docker compose -f docker-compose.infra.yml down
```

### 7. 인프라 초기화 (데이터 삭제)

```powershell
docker compose -f docker-compose.infra.yml down -v
```

## 개발 가이드

### 프로젝트 빌드

```bash
./gradlew build
```

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :collection-module:test
```

### 모듈별 실행

인프라가 실행 중이어야 합니다.

#### Collection Module (포트: 8081)
```bash
./gradlew :collection-module:bootRun
```

#### Processing Module (포트: 8082)
```bash
./gradlew :processing-module:bootRun
```

#### Indexing Module (포트: 8083)
```bash
./gradlew :indexing-module:bootRun
```

#### Serving Module (포트: 8080)
```bash
./gradlew :serving-module:bootRun
```

## Phase 0 완료 항목

- [x] docker-compose.infra.yml 작성
  - MongoDB, Kafka, Elasticsearch, Redis, Prometheus, Grafana
- [x] 공통 의존성 설정 (build.gradle.kts)
  - Spring Boot, Spring Batch, Kafka, MongoDB, Redis, Elasticsearch
  - Testcontainers, Micrometer
- [x] gRPC 프로토콜 정의 (ner.proto)
  - NERService: Analyze, AnalyzeBatch
  - EntityType: PERSON, LOCATION, ORGANIZATION
- [x] 공통 도메인 모델 작성
  - News: 뉴스 데이터 모델
  - YouTubeVideo: YouTube 동영상 데이터 모델
  - PublishStatus: Kafka 발행 상태
- [x] 기본 설정 파일 작성
  - application.yml (Collection Module)

## 다음 단계

**Phase 1: Collection Module 개발**
- RSS 수집 구현
- HTML 크롤링 구현
- YouTube API 연동
- Spring Batch Job 구현
- MongoDB 저장 및 Kafka 발행
- 테스트 코드 작성

## 기술 스택

- **Language**: Java 21, Python 3.8+
- **Framework**: Spring Boot 3.2.5, Spring Batch
- **Build Tool**: Gradle 8.x (Kotlin DSL)
- **Message Queue**: Apache Kafka 7.5.0
- **Database**: MongoDB 7.0
- **Search Engine**: Elasticsearch 8.11.0
- **Cache**: Redis 7.2, Caffeine
- **RPC**: gRPC 1.59.0
- **Monitoring**: Prometheus 2.48.0, Grafana 10.2.0
- **Container**: Docker, Docker Compose

## 참고

- 자세한 설계 내용은 [CLAUDE.md](./CLAUDE.md) 참조
