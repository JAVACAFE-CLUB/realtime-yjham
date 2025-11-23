"""
한국어 개체명 인식(NER) 분석기
GLiNER Korean 기반
"""
import logging
from typing import List, Optional, TYPE_CHECKING, Any

from config import NERConfig, get_ner_config
from domain import Entity, NERResult
from service.validator import KeywordValidator

if TYPE_CHECKING:
    from gliner import GLiNER

logger = logging.getLogger(__name__)


def _load_gliner_model(model_name: str) -> Any:
    """GLiNER 모델 로드 (lazy import)"""
    from gliner import GLiNER
    return GLiNER.from_pretrained(model_name)


class NERAnalyzer:
    """
    GLiNER Korean 기반 개체명 인식 분석기
    """
    
    def __init__(
        self, 
        config: Optional[NERConfig] = None,
        validator: Optional[KeywordValidator] = None,
        model: Optional["GLiNER"] = None,
    ):
        """
        NER 분석기 초기화
        
        Args:
            config: NER 설정 (None이면 기본 설정 사용)
            validator: 키워드 검증기 (None이면 새로 생성)
            model: GLiNER 모델 (None이면 새로 로드, 테스트용)
        """
        self.config = config or get_ner_config()
        self.validator = validator or KeywordValidator(self.config)
        
        if model is not None:
            self.model = model
            logger.info("외부 모델 주입됨")
        else:
            logger.info("NER 모델 초기화 시작")
            try:
                self.model = _load_gliner_model(self.config.model_name)
                logger.info(f"NER 모델 초기화 완료: {self.config.model_name}")
            except Exception as e:
                logger.error(f"NER 모델 초기화 실패: {e}")
                raise
    
    def analyze(self, text: str) -> NERResult:
        """
        텍스트에서 개체명 추출
        
        Args:
            text: 분석할 텍스트
        
        Returns:
            NERResult: 분석 결과
        """
        if not text or not text.strip():
            return NERResult(text=text, entities=[])
        
        try:
            # GLiNER로 NER 수행
            ner_results = self.model.predict_entities(text, self.config.entity_labels)
            
            # 결과 변환 및 필터링
            entities = []
            for result in ner_results:
                entity_text = result.get("text", "").strip()
                entity_label = result.get("label", "")
                
                # 매핑된 타입 확인 및 유효성 검사
                if entity_label in self.config.entity_labels:
                    if self.validator.is_valid(entity_text, entity_label):
                        entities.append(Entity(
                            keyword=entity_text,
                            type=entity_label,
                        ))
            
            logger.debug(f"텍스트 분석 완료: {len(entities)}개 개체명 추출")
            return NERResult(text=text, entities=entities)
        
        except Exception as e:
            logger.error(f"텍스트 분석 실패: {e}")
            return NERResult(text=text, entities=[])
    
    def analyze_batch(self, texts: List[str]) -> List[NERResult]:
        """
        여러 텍스트 배치 분석 (최적화 버전)
        
        Args:
            texts: 분석할 텍스트 리스트
        
        Returns:
            각 텍스트별 NERResult 리스트
        """
        if not texts:
            return []
        
        # 빈 텍스트 필터링 및 인덱스 추적
        valid_indices = []
        valid_texts = []
        for i, text in enumerate(texts):
            if text and text.strip():
                valid_indices.append(i)
                valid_texts.append(text)
        
        # 결과 초기화 (빈 텍스트는 빈 결과)
        results = [NERResult(text=t, entities=[]) for t in texts]
        
        if not valid_texts:
            return results
        
        try:
            # GLiNER 배치 예측 수행
            batch_predictions = self.model.batch_predict_entities(
                valid_texts, 
                self.config.entity_labels
            )
            
            # 각 텍스트별 결과 처리
            for idx, predictions in zip(valid_indices, batch_predictions):
                entities = []
                for result in predictions:
                    entity_text = result.get("text", "").strip()
                    entity_label = result.get("label", "")
                    
                    if entity_label in self.config.entity_labels:
                        if self.validator.is_valid(entity_text, entity_label):
                            entities.append(Entity(
                                keyword=entity_text,
                                type=entity_label,
                            ))
                
                results[idx] = NERResult(text=texts[idx], entities=entities)
            
            logger.info(f"배치 분석 완료: {len(texts)}개 텍스트")
            return results
        
        except Exception as e:
            logger.error(f"배치 분석 실패, 개별 분석으로 폴백: {e}")
            # 폴백: 개별 분석
            return [self.analyze(text) for text in texts]
