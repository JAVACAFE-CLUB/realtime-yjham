"""
Prometheus 메트릭 모듈
"""
import logging
from typing import Optional

from prometheus_client import Counter, Histogram, Gauge, start_http_server

logger = logging.getLogger(__name__)


class NERMetrics:
    """
    NER 서비스 메트릭 관리
    """
    
    def __init__(self, namespace: str = "ner"):
        """
        메트릭 초기화
        
        Args:
            namespace: 메트릭 네임스페이스
        """
        # 요청 카운터
        self.requests_total = Counter(
            f"{namespace}_requests_total",
            "Total number of NER requests",
            ["method", "status"]
        )
        
        # 처리 시간 히스토그램
        self.request_duration_seconds = Histogram(
            f"{namespace}_request_duration_seconds",
            "Request duration in seconds",
            ["method"],
            buckets=(0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0)
        )
        
        # 추출된 개체 수
        self.entities_extracted_total = Counter(
            f"{namespace}_entities_extracted_total",
            "Total number of entities extracted",
            ["entity_type"]
        )
        
        # 배치 크기 히스토그램
        self.batch_size = Histogram(
            f"{namespace}_batch_size",
            "Batch request size",
            buckets=(1, 5, 10, 25, 50, 100, 250, 500)
        )
        
        # 현재 처리 중인 요청 수
        self.requests_in_progress = Gauge(
            f"{namespace}_requests_in_progress",
            "Number of requests currently being processed",
            ["method"]
        )
        
        # 모델 로드 상태
        self.model_loaded = Gauge(
            f"{namespace}_model_loaded",
            "Whether the NER model is loaded (1=loaded, 0=not loaded)"
        )
    
    def record_request(self, method: str, status: str = "success"):
        """요청 기록"""
        self.requests_total.labels(method=method, status=status).inc()
    
    def record_duration(self, method: str, duration: float):
        """처리 시간 기록"""
        self.request_duration_seconds.labels(method=method).observe(duration)
    
    def record_entities(self, entity_type: str, count: int = 1):
        """추출된 개체 기록"""
        self.entities_extracted_total.labels(entity_type=entity_type).inc(count)
    
    def record_batch_size(self, size: int):
        """배치 크기 기록"""
        self.batch_size.observe(size)
    
    def set_in_progress(self, method: str, count: int):
        """처리 중 요청 수 설정"""
        self.requests_in_progress.labels(method=method).set(count)
    
    def set_model_loaded(self, loaded: bool):
        """모델 로드 상태 설정"""
        self.model_loaded.set(1 if loaded else 0)


# 싱글톤 인스턴스
_metrics: Optional[NERMetrics] = None


def get_metrics() -> NERMetrics:
    """메트릭 인스턴스 반환"""
    global _metrics
    if _metrics is None:
        _metrics = NERMetrics()
    return _metrics


def start_metrics_server(port: int = 9090):
    """
    Prometheus 메트릭 HTTP 서버 시작
    
    Args:
        port: HTTP 서버 포트
    """
    start_http_server(port)
    logger.info(f"Prometheus 메트릭 서버 시작: 포트 {port}")
