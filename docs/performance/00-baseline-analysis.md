# Baseline 성능 분석

## 1. 현재 상황 분석

### 1.1 테스트 환경
- **OS**: Windows 11
- **Java**: 21 (LTS)
- **Kafka**: Confluent Platform 7.5.0 (단일 브로커)
- **인프라**: Docker Compose 기반 로컬 환경

### 1.2 현재 Kafka 설정

#### Collection Module (Producer)
```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      # batch-size: 기본값 (16KB)
      # linger-ms: 기본값 (0)
      # compression-type: 미설정
```

**문제점:**
- `batch-size`: 기본값 16KB - 소규모 메시지에 적합하지 않을 수 있음
- `linger.ms`: 기본값 0 - 배치 없이 즉시 전송, 네트워크 오버헤드 증가
- `compression-type`: 미설정 - 대역폭 낭비

#### Processing Module (Consumer → Producer)
```yaml
spring:
  kafka:
    consumer:
      group-id: processing-group
      auto-offset-reset: earliest
      # max-poll-records: 기본값 (500)
      # fetch-min-size: 기본값 (1)
      # fetch-max-wait: 기본값 (500ms)
```

**문제점:**
- Consumer 동시성 설정 미정의 (기본값 1)
- 배치 리스너 미사용

#### Indexing Module (Consumer)
```yaml
spring:
  kafka:
    consumer:
      group-id: indexing-group
      auto-offset-reset: earliest
      # ack-mode: 기본값 (AUTO)
      # concurrency: 기본값 (1)
```

**문제점:**
- Auto-commit 사용으로 데이터 손실 가능성
- 단일 Consumer Thread

### 1.3 토픽 구성

| 토픽명 | 파티션 수 | 복제 계수 | 비고 |
|--------|----------|----------|------|
| raw-news | 1 (기본) | 1 | 병렬 처리 불가 |
| raw-youtube | 1 (기본) | 1 | 병렬 처리 불가 |
| processed-news | 3 | 1 | Processing에서 생성 |
| processed-youtube | 3 | 1 | Processing에서 생성 |
| raw-news-dlq | 1 | 1 | Dead Letter Queue |
| raw-youtube-dlq | 1 | 1 | Dead Letter Queue |

---

## 2. 왜 이 최적화가 필요한가?

### 2.1 현재 병목점 분석

#### 병목점 1: 동기 Producer 발행
- **현상**: 각 메시지 발행 후 결과를 대기 (최대 10초 타임아웃)
- **영향**: 처리량이 네트워크 지연에 비례하여 감소
- **예상 처리량**: ~10-50 TPS (최악의 경우)

#### 병목점 2: 배치 설정 미적용
- **현상**: `linger.ms=0`으로 인해 메시지마다 즉시 전송
- **영향**: 네트워크 호출 오버헤드 증가
- **예상 처리량**: 배치 적용 시 5-10배 개선 가능

#### 병목점 3: 파티션 부족 (raw-* 토픽)
- **현상**: 파티션 1개로 인해 Consumer 1개만 동시 처리 가능
- **영향**: Consumer 수평 확장 불가
- **예상 처리량**: 파티션 증가 시 선형 확장 가능

#### 병목점 4: gRPC NER 서비스
- **현상**: 외부 서비스 호출로 인한 지연
- **영향**: Processing 단계의 전체 처리량 제한
- **예상 처리량**: 10-100 TPS (CPU 기반, 핵심 병목)

### 2.2 목표 성능 (로컬 개발 환경)

| 지표 | 현재 (예상) | 목표 | 비고 |
|------|------------|------|------|
| Producer TPS | ~10-50 | **100-300** | 초당 메시지 발행량 |
| End-to-end 지연 | 수 초 | **< 3초** | Collection → Indexing 완료 |
| Consumer Lag | 미측정 | **< 300** | 1~3초 처리 지연 |

---

## 3. 측정 방법

### 3.1 메트릭 수집 환경

#### Kafka Broker 메트릭 (JMX Exporter)
- 위치: `kafka-jmx-exporter:9404`
- 설정 파일: `monitoring/kafka-jmx-exporter.yml`

#### 애플리케이션 메트릭 (Micrometer/Prometheus)
- Collection Module: `collection-module:8081/actuator/prometheus`
- Processing Module: `processing-module:8082/actuator/prometheus`
- Indexing Module: `indexing-module:8083/actuator/prometheus`

### 3.2 수집 메트릭

| 메트릭 | 설명 | 수집 위치 |
|--------|------|----------|
| `kafka_producer_record_send_rate` | 초당 발행 레코드 수 | Kafka JMX |
| `kafka_producer_request_latency_avg` | 평균 요청 지연 | Kafka JMX |
| `kafka_consumer_records_consumed_rate` | 초당 소비 레코드 수 | Kafka JMX |
| `kafka_consumer_fetch_latency_avg` | 평균 fetch 지연 | Kafka JMX |
| `collection.items.published` | 발행 성공 아이템 수 | Collection Module |
| `collection.publish.duration` | 발행 소요 시간 | Collection Module |
| `processing.messages.consumed` | 소비된 메시지 수 | Processing Module |
| `processing.process.duration` | 처리 소요 시간 | Processing Module |
| `indexing.messages.indexed` | 인덱싱 성공 메시지 수 | Indexing Module |
| `indexing.index.duration` | 인덱싱 소요 시간 | Indexing Module |

### 3.3 부하 테스트 방법

```bash
# 동기 부하 테스트 (목표 TPS 지정)
curl -X POST "http://localhost:8081/api/load-test/run?messageCount=100&targetTps=50&source=both"

# 비동기 부하 테스트 (높은 TPS 달성)
curl -X POST "http://localhost:8081/api/load-test/run-async?messageCount=1000&threads=4&source=both"
```

---

## 4. Baseline 측정 결과

> **측정일**: 2025-12-02
> **환경**: Docker Compose 기반 전체 시스템 (docker-compose.full.yml)

### 4.1 부하 테스트 결과

#### 동기 테스트 (100 메시지, 목표 50 TPS)
| 항목 | 값 | 비고 |
|------|---|------|
| 테스트 메시지 수 | 100 | news + youtube 혼합 |
| 총 소요 시간 | 2,455 ms | |
| 성공 메시지 수 | 100 | |
| 실패 메시지 수 | 0 | |
| 실제 TPS | **40.7** | 목표 대비 81% |
| 평균 발행 지연 | 4.57 ms | |

#### 비동기 테스트 (500 메시지, 4 스레드)
| 항목 | 값 | 비고 |
|------|---|------|
| 테스트 메시지 수 | 500 | news + youtube 혼합 |
| 총 소요 시간 | 30 ms | |
| 성공 메시지 수 | 500 | |
| 실패 메시지 수 | 0 | |
| 실제 TPS (Producer) | **16,667** | 발행 속도만 측정 |

### 4.2 Consumer Lag 확인

#### Processing Group (raw-* 토픽 소비)
| 토픽 | 현재 오프셋 | 로그 끝 오프셋 | Lag | 처리 속도 |
|------|-----------|--------------|-----|---------|
| raw-news | 245 | 380 | **135** | ~3.3 TPS |
| raw-youtube | 167 | 350 | **183** | ~3.4 TPS |

> **핵심 병목점**: Processing 단계에서 NER 서비스 호출로 인해 **~6.7 TPS**로 제한됨

#### Indexing Group (processed-* 토픽 소비)
| 토픽 | 파티션 | Lag | 상태 |
|------|--------|-----|------|
| processed-news | 0, 1, 2 | 0 | ✅ 빠른 처리 |
| processed-youtube | 0, 1, 2 | 0 | ✅ 빠른 처리 |

### 4.3 Processing 단계 성능 분석

| 지표 | News | YouTube |
|------|------|---------|
| 처리된 메시지 수 | 284 | 206 |
| 총 처리 시간 | 163.2초 | 100.8초 |
| **평균 처리 시간** | **575ms** | **489ms** |
| 최대 처리 시간 | 767ms | 742ms |

### 4.4 End-to-End 지연시간

| 단계 | 예상 지연 | 측정값 |
|------|---------|--------|
| Collection → Kafka | ~100ms | **~5ms** (Producer) |
| Kafka → Processing | ~500ms | ~instant |
| Processing (NER 포함) | ~500-2000ms | **~530ms** |
| Processing → Kafka | ~100ms | ~5ms |
| Kafka → Indexing | ~500ms | ~instant |
| Indexing (ES 저장) | ~200ms | ~20ms |
| **총 End-to-End** | **~2-4초** | **~1초** (Lag 제외) |

> **주의**: 실제 End-to-End 시간은 Consumer Lag에 따라 크게 증가할 수 있음

---

## 5. 추가 고려사항

### 5.1 측정 시 주의사항
- 첫 번째 측정은 JVM 워밍업으로 인해 수치가 낮을 수 있음
- Kafka 브로커 및 NER 서비스가 완전히 시작된 후 측정
- 여러 번 측정하여 평균값 사용

### 5.2 다음 단계
1. 인프라 실행 (`docker compose -f docker-compose.infra.yml up -d`)
2. 각 모듈 실행
3. 부하 테스트 실행 및 결과 기록
4. Grafana 대시보드에서 메트릭 시각화
5. 병목점 식별 및 Phase 2 진행

---

## 6. 측정 절차

### 6.1 인프라 시작
```bash
# 인프라 시작
docker compose -f docker-compose.infra.yml up -d

# 상태 확인
docker compose -f docker-compose.infra.yml ps
```

### 6.2 애플리케이션 시작
```bash
# 각 모듈 시작 (별도 터미널)
./gradlew :collection-module:bootRun
./gradlew :processing-module:bootRun
./gradlew :indexing-module:bootRun
./gradlew :serving-module:bootRun
```

### 6.3 부하 테스트 실행
```bash
# 1차 테스트: 100개 메시지, 50 TPS 목표
curl -X POST "http://localhost:8081/api/load-test/run?messageCount=100&targetTps=50&source=both"

# 2차 테스트: 500개 메시지, 비동기 4스레드
curl -X POST "http://localhost:8081/api/load-test/run-async?messageCount=500&threads=4&source=both"
```

### 6.4 결과 확인
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Kafka UI: http://localhost:9082

---

*작성일: 2025-12-01*
*Phase 1: 성능 측정 환경 구축 완료*
