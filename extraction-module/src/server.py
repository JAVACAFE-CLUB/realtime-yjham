"""
gRPC NER 서버
"""
import logging
import sys
from concurrent import futures

import grpc

# proto 파일에서 생성된 모듈 임포트
sys.path.append('./src/generated')
import ner_pb2
import ner_pb2_grpc

from ner_service import NERAnalyzer
from config import get_server_config, get_ner_config

# 서버 설정 로드
server_config = get_server_config()

logging.basicConfig(
    level=getattr(logging, server_config.log_level.upper(), logging.INFO),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class NERServiceServicer(ner_pb2_grpc.NERServiceServicer):
    """
    gRPC NER 서비스 구현
    """

    def __init__(self):
        """
        서비스 초기화
        """
        logger.info("NER 서비스 초기화 시작")
        self.analyzer = NERAnalyzer()
        logger.info("NER 서비스 초기화 완료")

    def Analyze(self, request, context):
        """
        단일 텍스트 분석

        Args:
            request: TextRequest
            context: gRPC context

        Returns:
            NERResponse
        """
        try:
            logger.info(f"분석 요청 수신: {len(request.text)} 문자")

            # NER 분석
            entities = self.analyzer.analyze(request.text)

            # 응답 생성
            response = ner_pb2.NERResponse()
            for entity in entities:
                entity_pb = response.entities.add()
                entity_pb.keyword = entity['keyword']
                entity_pb.type = entity['type']

            logger.info(f"분석 완료: {len(entities)}개 개체명")
            return response

        except Exception as e:
            logger.error(f"분석 중 오류 발생: {e}")
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f'분석 실패: {str(e)}')
            return ner_pb2.NERResponse()

    def AnalyzeBatch(self, request, context):
        """
        배치 텍스트 분석

        Args:
            request: BatchTextRequest
            context: gRPC context

        Returns:
            BatchNERResponse
        """
        try:
            logger.info(f"배치 분석 요청 수신: {len(request.texts)}개 텍스트")

            # 배치 NER 분석
            batch_results = self.analyzer.analyze_batch(list(request.texts))

            # 응답 생성
            batch_response = ner_pb2.BatchNERResponse()
            for entities in batch_results:
                response = batch_response.responses.add()
                for entity in entities:
                    entity_pb = response.entities.add()
                    entity_pb.keyword = entity['keyword']
                    entity_pb.type = entity['type']

            logger.info(f"배치 분석 완료: {len(request.texts)}개 텍스트")
            return batch_response

        except Exception as e:
            logger.error(f"배치 분석 중 오류 발생: {e}")
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f'배치 분석 실패: {str(e)}')
            return ner_pb2.BatchNERResponse()


def serve(port: int = None):
    """
    gRPC 서버 시작

    Args:
        port: 서버 포트 (None이면 설정에서 가져옴)
    """
    config = get_server_config()
    port = port or config.port
    
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=config.max_workers))
    ner_pb2_grpc.add_NERServiceServicer_to_server(NERServiceServicer(), server)
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


if __name__ == '__main__':
    serve()
