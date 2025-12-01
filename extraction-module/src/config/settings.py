"""
NER 서비스 설정
환경 변수로 설정 가능
"""
import os
from dataclasses import dataclass, field
from typing import Set, List


@dataclass(frozen=True)
class NERConfig:
    """
    NER 분석기 설정
    """
    # GLiNER 모델명
    model_name: str = field(
        default_factory=lambda: os.getenv("NER_MODEL_NAME", "taeminlee/gliner_ko")
    )
    
    # 추출할 개체 레이블
    entity_labels: List[str] = field(
        default_factory=lambda: os.getenv(
            "NER_ENTITY_LABELS", "PERSON,LOCATION,ORGANIZATION"
        ).split(",")
    )
    
    # 최소 키워드 길이
    min_keyword_length: int = field(
        default_factory=lambda: int(os.getenv("NER_MIN_KEYWORD_LENGTH", "2"))
    )
    
    # 필터링할 단일 성씨 목록
    single_surnames: Set[str] = field(
        default_factory=lambda: set(
            os.getenv(
                "NER_SINGLE_SURNAMES",
                "이,김,박,최,정,강,조,윤,장,임,한,오,서,신,권,황,안,송,류,전,홍,고,문,양,손,배,백,허,유,남"
            ).split(",")
        )
    )


@dataclass(frozen=True)
class ServerConfig:
    """
    gRPC 서버 설정
    """
    # 서버 포트
    port: int = field(
        default_factory=lambda: int(os.getenv("GRPC_PORT", "50051"))
    )
    
    # 최대 워커 수
    max_workers: int = field(
        default_factory=lambda: int(os.getenv("GRPC_MAX_WORKERS", "10"))
    )
    
    # 로그 레벨
    log_level: str = field(
        default_factory=lambda: os.getenv("LOG_LEVEL", "INFO")
    )


# 싱글톤 인스턴스
_ner_config: NERConfig = None
_server_config: ServerConfig = None


def get_ner_config() -> NERConfig:
    """NER 설정 인스턴스 반환"""
    global _ner_config
    if _ner_config is None:
        _ner_config = NERConfig()
    return _ner_config


def get_server_config() -> ServerConfig:
    """서버 설정 인스턴스 반환"""
    global _server_config
    if _server_config is None:
        _server_config = ServerConfig()
    return _server_config


def reset_config():
    """설정 초기화 (테스트용)"""
    global _ner_config, _server_config
    _ner_config = None
    _server_config = None
