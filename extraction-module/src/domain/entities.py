"""
도메인 엔티티 정의
"""
from dataclasses import dataclass, field
from typing import List
from enum import Enum


class EntityType(str, Enum):
    """개체명 타입"""
    PERSON = "PERSON"
    LOCATION = "LOCATION"
    ORGANIZATION = "ORGANIZATION"


@dataclass(frozen=True)
class Entity:
    """
    추출된 개체명
    
    Attributes:
        keyword: 추출된 키워드 텍스트
        type: 개체명 타입 (PERSON, LOCATION, ORGANIZATION)
    """
    keyword: str
    type: str
    
    def to_dict(self) -> dict:
        """딕셔너리로 변환"""
        return {
            "keyword": self.keyword,
            "type": self.type,
        }


@dataclass
class NERResult:
    """
    NER 분석 결과
    
    Attributes:
        text: 원본 텍스트
        entities: 추출된 개체명 리스트
    """
    text: str
    entities: List[Entity] = field(default_factory=list)
    
    @property
    def entity_count(self) -> int:
        """추출된 개체명 수"""
        return len(self.entities)
    
    def to_dict_list(self) -> List[dict]:
        """개체명 리스트를 딕셔너리 리스트로 변환"""
        return [entity.to_dict() for entity in self.entities]
