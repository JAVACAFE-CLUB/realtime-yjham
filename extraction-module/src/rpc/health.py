"""
gRPC Health Check 서비스 구현
"""
import logging
from typing import Optional

import grpc
from grpc_health.v1 import health_pb2, health_pb2_grpc
from grpc_health.v1.health import HealthServicer

logger = logging.getLogger(__name__)


class NERHealthServicer(HealthServicer):
    """
    NER 서비스 Health Check 구현
    """
    
    def __init__(self):
        super().__init__()
        # 초기 상태를 SERVING으로 설정
        self.set_status("", health_pb2.HealthCheckResponse.SERVING)
        self.set_status("ner.NERService", health_pb2.HealthCheckResponse.SERVING)
    
    def set_serving(self):
        """서비스 상태를 SERVING으로 설정"""
        self.set_status("", health_pb2.HealthCheckResponse.SERVING)
        self.set_status("ner.NERService", health_pb2.HealthCheckResponse.SERVING)
        logger.info("Health status: SERVING")
    
    def set_not_serving(self):
        """서비스 상태를 NOT_SERVING으로 설정"""
        self.set_status("", health_pb2.HealthCheckResponse.NOT_SERVING)
        self.set_status("ner.NERService", health_pb2.HealthCheckResponse.NOT_SERVING)
        logger.info("Health status: NOT_SERVING")
    
    def set_status(self, service: str, status: int):
        """서비스 상태 설정"""
        self._server_status[service] = status


def add_health_servicer(server: grpc.Server, health_servicer: Optional[NERHealthServicer] = None):
    """
    서버에 Health Check 서비스 추가
    
    Args:
        server: gRPC 서버
        health_servicer: Health 서비스 (None이면 새로 생성)
    
    Returns:
        NERHealthServicer 인스턴스
    """
    health_servicer = health_servicer or NERHealthServicer()
    health_pb2_grpc.add_HealthServicer_to_server(health_servicer, server)
    logger.info("Health Check 서비스 등록 완료")
    return health_servicer
