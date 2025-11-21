"""
한국어 개체명 인식(NER) 서비스
GLiNER Korean 기반
"""
import logging
from typing import List, Dict
from gliner import GLiNER

logger = logging.getLogger(__name__)


class NERAnalyzer:
    """
    GLiNER Korean 기반 개체명 인식 분석기
    """

    # GLiNER 레이블을 우리 타입으로 매핑
    ENTITY_LABELS = ["PERSON", "LOCATION", "ORGANIZATION"]

    # GLiNER 레이블 매핑 (필요시)
    LABEL_MAPPING = {
        'PERSON': 'PERSON',
        'LOCATION': 'LOCATION',
        'ORGANIZATION': 'ORGANIZATION'
    }

    def __init__(self):
        """
        NER 모델 초기화
        """
        logger.info("NER 모델 초기화 시작")
        try:
            # GLiNER Korean 모델 로드
            self.model = GLiNER.from_pretrained("taeminlee/gliner_ko")
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
            # GLiNER로 NER 수행
            ner_results = self.model.predict_entities(text, self.ENTITY_LABELS)

            # 결과 변환
            entities = []
            for entity in ner_results:
                entity_text = entity.get("text", "")
                entity_label = entity.get("label", "")

                # 매핑된 타입 확인
                if entity_label in self.LABEL_MAPPING:
                    entities.append({
                        'keyword': entity_text.strip(),
                        'type': self.LABEL_MAPPING[entity_label]
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
