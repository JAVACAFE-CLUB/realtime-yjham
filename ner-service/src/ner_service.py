"""
한국어 개체명 인식(NER) 서비스
Pororo 라이브러리 기반
"""
import logging
from typing import List, Dict
from pororo import Pororo

logger = logging.getLogger(__name__)


class NERAnalyzer:
    """
    Pororo 기반 개체명 인식 분석기
    """

    # 추출할 개체명 타입
    VALID_TYPES = {'PERSON', 'LOCATION', 'ORGANIZATION'}

    def __init__(self):
        """
        NER 모델 초기화
        """
        logger.info("NER 모델 초기화 시작")
        try:
            self.ner = Pororo(task="ner", lang="ko")
            logger.info("NER 모델 초기화 완료")
        except Exception as e:
            logger.error(f"NER 모델 초기화 실패: {e}")
            raise

    def analyze(self, text: str) -> List[Dict[str, str]]:
        """
        텍스트에서 개체명 추출

        Args:
            text: 분석할 텍스트

        Returns:
            개체명 리스트 [{"keyword": "삼성전자", "type": "ORGANIZATION"}, ...]
        """
        if not text or not text.strip():
            return []

        try:
            # Pororo NER 수행
            ner_results = self.ner(text)

            # 결과 파싱 및 필터링
            entities = []
            for word, tag in ner_results:
                # B- 또는 I- 접두사 제거
                entity_type = tag.split('-')[-1] if '-' in tag else tag

                # 유효한 타입만 추출
                if entity_type in self.VALID_TYPES:
                    entities.append({
                        'keyword': word,
                        'type': entity_type
                    })

            logger.debug(f"텍스트 분석 완료: {len(entities)}개 개체명 추출")
            return entities

        except Exception as e:
            logger.error(f"텍스트 분석 실패: {e}")
            return []

    def analyze_batch(self, texts: List[str]) -> List[List[Dict[str, str]]]:
        """
        여러 텍스트 배치 분석

        Args:
            texts: 분석할 텍스트 리스트

        Returns:
            각 텍스트별 개체명 리스트
        """
        results = []
        for text in texts:
            results.append(self.analyze(text))

        logger.info(f"배치 분석 완료: {len(texts)}개 텍스트")
        return results
