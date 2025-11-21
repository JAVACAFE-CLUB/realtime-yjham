# NER Service

한국어 개체명 인식(Named Entity Recognition) gRPC 서비스

## 기술 스택

- Python 3.9
- Pororo (카카오브레인 NLP 라이브러리)
- gRPC

## 기능

- 텍스트에서 개체명 추출
- 지원 개체명 타입:
  - PERSON (인물)
  - LOCATION (장소)
  - ORGANIZATION (조직)

## 설치 및 실행

### 로컬 실행

```bash
# 의존성 설치
pip install -r requirements.txt

# gRPC 코드 생성
bash generate_proto.sh

# 서버 실행
python src/server.py
```

### Docker 실행

```bash
# 이미지 빌드
docker build -t ner-service .

# 컨테이너 실행
docker run -p 50051:50051 ner-service
```

### Docker Compose 실행

```bash
# 프로젝트 루트에서 실행
docker compose -f docker-compose.infra.yml up -d ner-service
```

## gRPC API

### Analyze (단일 텍스트 분석)

```protobuf
rpc Analyze(TextRequest) returns (NERResponse);
```

**요청:**
```json
{
  "text": "삼성전자가 서울에서 신제품을 발표했다"
}
```

**응답:**
```json
{
  "entities": [
    {"keyword": "삼성전자", "type": "ORGANIZATION"},
    {"keyword": "서울", "type": "LOCATION"}
  ]
}
```

### AnalyzeBatch (배치 텍스트 분석)

```protobuf
rpc AnalyzeBatch(BatchTextRequest) returns (BatchNERResponse);
```

**요청:**
```json
{
  "texts": [
    "삼성전자가 서울에서 신제품을 발표했다",
    "이재용 회장이 미국을 방문했다"
  ]
}
```

**응답:**
```json
{
  "responses": [
    {
      "entities": [
        {"keyword": "삼성전자", "type": "ORGANIZATION"},
        {"keyword": "서울", "type": "LOCATION"}
      ]
    },
    {
      "entities": [
        {"keyword": "이재용", "type": "PERSON"},
        {"keyword": "미국", "type": "LOCATION"}
      ]
    }
  ]
}
```

## 포트

- gRPC: 50051

## 로그

서비스는 표준 출력으로 로그를 출력합니다:

```
2025-11-21 20:00:00 - __main__ - INFO - NER 모델 초기화 시작
2025-11-21 20:00:05 - __main__ - INFO - NER 모델 초기화 완료
2025-11-21 20:00:05 - __main__ - INFO - NER 서버 시작: 포트 50051
```

## 주의사항

- 첫 실행 시 Pororo 모델 다운로드로 인해 시작 시간이 오래 걸릴 수 있습니다
- GPU가 있는 환경에서는 자동으로 GPU를 사용합니다
- 메모리: 최소 2GB 권장
