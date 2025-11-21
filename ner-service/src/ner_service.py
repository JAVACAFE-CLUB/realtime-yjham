"""
한국어 개체명 인식(NER) 서비스
Hugging Face Transformers 기반
"""
import logging
from typing import List, Dict
from transformers import pipeline

logger = logging.getLogger(__name__)


class NERAnalyzer:
    """
    Hugging Face Transformers 기반 개체명 인식 분석기
    """

    # KLUE NER 태그를 우리 타입으로 매핑
    TAG_MAPPING = {
        'PS': 'PERSON',      # Person
        'LC': 'LOCATION',    # Location
        'OG': 'ORGANIZATION' # Organization
    }

    def __init__(self):
        """
        NER 모델 초기화
        """
        logger.info("NER 모델 초기화 시작")
        try:
            # KLUE RoBERTa 기반 NER 모델 사용
            self.ner = pipeline(
                "token-classification",
                model="klue/roberta-base",
                aggregation_strategy="simple"
            )
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
            # Hugging Face NER 수행
            ner_results = self.ner(text)

            # 결과 파싱 및 필터링
            entities = []
            for entity in ner_results:
                # entity_group에서 태그 추출 (예: "B-PS" -> "PS")
                entity_label = entity['entity_group']

                # B-, I- 접두사 제거
                if '-' in entity_label:
                    entity_label = entity_label.split('-')[-1]

                # 매핑된 타입 확인
                if entity_label in self.TAG_MAPPING:
                    entities.append({
                        'keyword': entity['word'].strip(),
                        'type': self.TAG_MAPPING[entity_label]
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
