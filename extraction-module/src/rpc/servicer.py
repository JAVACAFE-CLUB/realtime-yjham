"""
gRPC NER 서비스 구현
"""
import logging
from typing import Optional

import grpc

from generated import ner_pb2, ner_pb2_grpc
from service import NERAnalyzer

logger = logging.getLogger(__name__)


class NERServiceServicer(ner_pb2_grpc.NERServiceServicer):
    """
    gRPC NER 서비스 구현
    """
    
    def __init__(self, analyzer: Optional[NERAnalyzer] = None):
        """
        서비스 초기화
        
        Args:
            analyzer: NER 분석기 (None이면 새로 생성)
        """
        logger.info("NER 서비스 초기화 시작")
        self.analyzer = analyzer or NERAnalyzer()
        logger.info("NER 서비스 초기화 완료")
    
    def Analyze(self, request, context) -> ner_pb2.NERResponse:
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
            result = self.analyzer.analyze(request.text)
            
            # 응답 생성
            response = ner_pb2.NERResponse()
            for entity in result.entities:
                entity_pb = response.entities.add()
                entity_pb.keyword = entity.keyword
                entity_pb.type = entity.type
            
            logger.info(f"분석 완료: {result.entity_count}개 개체명")
            return response
        
        except Exception as e:
            logger.error(f"분석 중 오류 발생: {e}")
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f'분석 실패: {str(e)}')
            return ner_pb2.NERResponse()
    
    def AnalyzeBatch(self, request, context) -> ner_pb2.BatchNERResponse:
        """
        배치 텍스트 분석
        
        Args:
            request: BatchTextRequest
            context: gRPC context
        
        Returns:
            BatchNERResponse
        """
        try:
            texts = list(request.texts)
            logger.info(f"배치 분석 요청 수신: {len(texts)}개 텍스트")
            
            # 배치 NER 분석
            results = self.analyzer.analyze_batch(texts)
            
            # 응답 생성
            batch_response = ner_pb2.BatchNERResponse()
            for result in results:
                response = batch_response.responses.add()
                for entity in result.entities:
                    entity_pb = response.entities.add()
                    entity_pb.keyword = entity.keyword
                    entity_pb.type = entity.type
            
            logger.info(f"배치 분석 완료: {len(texts)}개 텍스트")
            return batch_response
        
        except Exception as e:
            logger.error(f"배치 분석 중 오류 발생: {e}")
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f'배치 분석 실패: {str(e)}')
            return ner_pb2.BatchNERResponse()
