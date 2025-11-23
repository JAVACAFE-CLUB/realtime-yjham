"""
키워드 유효성 검사 모듈
"""
import re
import logging
from typing import Set

from config import NERConfig, get_ner_config

logger = logging.getLogger(__name__)


class KeywordValidator:
    """
    추출된 키워드의 유효성을 검사하는 클래스
    """
    
    def __init__(self, config: NERConfig = None):
        """
        Args:
            config: NER 설정 (None이면 기본 설정 사용)
        """
        self.config = config or get_ner_config()
    
    def is_valid(self, keyword: str, entity_type: str) -> bool:
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
