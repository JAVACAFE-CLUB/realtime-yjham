"""
한국어 개체명 인식(NER) 서비스
GLiNER Korean 기반
"""
import logging
import re
from typing import List, Dict, Optional

from gliner import GLiNER

from config import NERConfig, get_ner_config

logger = logging.getLogger(__name__)


class NERAnalyzer:
    """
    GLiNER Korean 기반 개체명 인식 분석기
    """

    def __init__(self, config: Optional[NERConfig] = None):
        """
        NER 모델 초기화
        
        Args:
            config: NER 설정 (None이면 기본 설정 사용)
        """
        self.config = config or get_ner_config()
        
        logger.info("NER 모델 초기화 시작")
        try:
            # GLiNER Korean 모델 로드
            self.model = GLiNER.from_pretrained(self.config.model_name)
            logger.info(f"NER 모델 초기화 완료: {self.config.model_name}")
        except Exception as e:
            logger.error(f"NER 모델 초기화 실패: {e}")
            raise

    def _is_valid_keyword(self, keyword: str, entity_type: str) -> bool:
        """
        키워드 유효성 검사

        Args:
            keyword: 검사할 키워드
            entity_type: 개체 타입 (PERSON, LOCATION, ORGANIZATION)

        Returns:
            유효한 키워드인지 여부
        """
        # 빈 문자열 제외
        if not keyword or not keyword.strip():
            return False

        keyword = keyword.strip()

        # 최소 길이 검사
        if len(keyword) < self.config.min_keyword_length:
            return False

        # 단일 알파벳 제외 (A, B, C 등)
        if re.match(r'^[A-Za-z]$', keyword):
            return False

        # 숫자로만 구성된 키워드 제외
        if keyword.isdigit():
            return False

        # PERSON 타입일 때 단일 성씨만 있는 경우 제외
        if entity_type == 'PERSON' and keyword in self.config.single_surnames:
            return False

        # 특수문자로만 구성된 키워드 제외
        if re.match(r'^[^\w가-힣]+$', keyword):
            return False

        return True

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
            ner_results = self.model.predict_entities(text, self.config.entity_labels)

            # 결과 변환 및 필터링
            entities = []
            for entity in ner_results:
                entity_text = entity.get("text", "").strip()
                entity_label = entity.get("label", "")

                # 매핑된 타입 확인
                if entity_label in self.config.entity_labels:
                    mapped_type = entity_label

                    # 유효성 검사
                    if self._is_valid_keyword(entity_text, mapped_type):
                        entities.append({
                            'keyword': entity_text,
                            'type': mapped_type
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
