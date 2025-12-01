"""
gRPC 서버 모듈
"""
import logging
from concurrent import futures
from typing import Optional

import grpc

from generated import ner_pb2_grpc
from config import ServerConfig, get_server_config
from service import NERAnalyzer
from rpc.servicer import NERServiceServicer

logger = logging.getLogger(__name__)


def create_server(
    config: Optional[ServerConfig] = None,
    analyzer: Optional[NERAnalyzer] = None,
) -> grpc.Server:
    """
    gRPC 서버 인스턴스 생성
    
    Args:
        config: 서버 설정 (None이면 기본 설정 사용)
        analyzer: NER 분석기 (None이면 새로 생성)
    
    Returns:
        grpc.Server 인스턴스
    """
    config = config or get_server_config()
    
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=config.max_workers))
    
    # 서비스 등록
    servicer = NERServiceServicer(analyzer=analyzer)
    ner_pb2_grpc.add_NERServiceServicer_to_server(servicer, server)
    
    return server


def serve(
    port: Optional[int] = None,
    config: Optional[ServerConfig] = None,
    analyzer: Optional[NERAnalyzer] = None,
):
    """
    gRPC 서버 시작
    
    Args:
        port: 서버 포트 (None이면 설정에서 가져옴)
        config: 서버 설정 (None이면 기본 설정 사용)
        analyzer: NER 분석기 (None이면 새로 생성)
    """
    config = config or get_server_config()
    port = port or config.port
    
    server = create_server(config=config, analyzer=analyzer)
    server.add_insecure_port(f'[::]:{port}')
    
    logger.info(f"NER 서버 시작: 포트 {port}, 워커 수 {config.max_workers}")
    server.start()
    logger.info("NER 서버가 요청을 대기하고 있습니다...")
    
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        logger.info("NER 서버 종료 중...")
        server.stop(0)
        logger.info("NER 서버 종료 완료")
