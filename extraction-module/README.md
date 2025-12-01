# Extraction Module

한국어 개체명 인식(NER) gRPC 서비스. GLiNER Korean 모델을 사용하여 텍스트에서 인물, 장소, 조직명을 추출합니다.

## 패키지 구조

```
extraction-module/
├── proto/
│   └── ner.proto              # gRPC 프로토콜 정의
├── src/
│   ├── main.py                # 엔트리포인트
│   ├── config/                # 설정 모듈
│   │   ├── __init__.py
│   │   └── settings.py        # NERConfig, ServerConfig
│   ├── domain/                # 도메인 모듈
│   │   ├── __init__.py
│   │   └── entities.py        # Entity, NERResult
│   ├── service/               # 서비스 모듈
│   │   ├── __init__.py
│   │   ├── analyzer.py        # NERAnalyzer
│   │   └── validator.py       # KeywordValidator
│   ├── rpc/                  # gRPC 모듈
│   │   ├── __init__.py
│   │   ├── server.py          # gRPC 서버
│   │   ├── servicer.py        # NERServiceServicer
│   │   └── health.py          # Health Check 서비스
│   ├── infrastructure/        # 인프라 모듈
│   │   ├── __init__.py
│   │   └── metrics.py         # Prometheus 메트릭
│   └── generated/             # proto 생성 파일
└── tests/                     # 테스트
```

## 설치 및 실행

### 로컬 환경

```bash
# 의존성 설치
pip install -r requirements.txt

# proto 파일 컴파일
python -m grpc_tools.protoc \
    -I./proto \
    --python_out=./src/generated \
    --grpc_python_out=./src/generated \
    ./proto/ner.proto

# 서버 실행
python src/main.py
```

### Docker

```bash
# 이미지 빌드
docker build -t extraction-module .

# 컨테이너 실행
docker run -p 50051:50051 -p 9090:9090 extraction-module
```

## 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `GRPC_PORT` | 50051 | gRPC 서버 포트 |
| `GRPC_MAX_WORKERS` | 10 | 최대 워커 스레드 수 |
| `LOG_LEVEL` | INFO | 로그 레벨 |
| `NER_MODEL_NAME` | taeminlee/gliner_ko | GLiNER 모델명 |
| `NER_ENTITY_LABELS` | PERSON,LOCATION,ORGANIZATION | 추출할 개체 타입 |
| `NER_MIN_KEYWORD_LENGTH` | 2 | 최소 키워드 길이 |

## gRPC API

### Analyze

단일 텍스트에서 개체명 추출

```protobuf
rpc Analyze(TextRequest) returns (NERResponse);
```

### AnalyzeBatch

여러 텍스트 배치 분석 (최적화됨)

```protobuf
rpc AnalyzeBatch(BatchTextRequest) returns (BatchNERResponse);
```

## 테스트

```bash
# 전체 테스트
pytest

# 커버리지 리포트
pytest --cov=src --cov-report=html
```

## 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                     gRPC Server                          │
│  ┌─────────────────────────────────────────────────────┐│
│  │              NERServiceServicer                      ││
│  │  ┌─────────────────────────────────────────────────┐││
│  │  │                NERAnalyzer                       │││
│  │  │  ┌───────────────┐  ┌──────────────────────────┐│││
│  │  │  │ KeywordValidator │ │      GLiNER Model        ││││
│  │  │  └───────────────┘  └──────────────────────────┘│││
│  │  └─────────────────────────────────────────────────┘││
│  └─────────────────────────────────────────────────────┘│
│  ┌─────────────────────┐  ┌────────────────────────────┐│
│  │   Health Service     │  │    Prometheus Metrics     ││
│  └─────────────────────┘  └────────────────────────────┘│
└─────────────────────────────────────────────────────────┘
```

## 의존성 주입

테스트와 유연성을 위해 의존성 주입을 지원합니다:

```python
from service import NERAnalyzer, KeywordValidator
from config import NERConfig

# 커스텀 설정으로 분석기 생성
config = NERConfig(min_keyword_length=3)
validator = KeywordValidator(config=config)
analyzer = NERAnalyzer(config=config, validator=validator)

# 테스트용 모델 주입
from unittest.mock import Mock
mock_model = Mock()
analyzer = NERAnalyzer(model=mock_model)
```
